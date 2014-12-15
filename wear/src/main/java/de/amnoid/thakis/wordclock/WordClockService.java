package de.amnoid.thakis.wordclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.TimeZone;

public class WordClockService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static int IT = 1 << 0;
    private static int IS = 1 << 1;
    private static int HALF = 1 << 2;
    private static int TEN_H = 1 << 3;
    private static int QUARTER = 1 << 4;
    private static int TWENTY_H = 1 << 5;
    private static int FIVE_H = 1 << 6;
    private static int MINUTES = 1 << 7;
    private static int TO = 1 << 8;
    private static int PAST = 1 << 9;
    private static int ONE = 1 << 10;
    private static int THREE = 1 << 11;
    private static int TWO = 1 << 12;
    private static int FOUR = 1 << 13;
    private static int FIVE = 1 << 14;
    private static int SIX = 1 << 15;
    private static int SEVEN = 1 << 16;
    private static int EIGHT = 1 << 17;
    private static int NINE = 1 << 18;
    private static int TEN = 1 << 19;
    private static int ELEVEN = 1 << 2;
    private static int TWELVE = 1 << 21;
    private static int AM = 1 << 22;
    private static int PM = 1 << 23;

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        private Time mTime;
        private boolean mLowBitAmbient;

        boolean mRegisteredTimeZoneReceiver = false;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private Paint mLightPaint;
        private Paint mDarkPaint;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mTime = new Time();

            mLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mLightPaint.setColor(Color.WHITE);
            mLightPaint.setTextSize(36f);
            mLightPaint.setTypeface(Typeface.DEFAULT_BOLD);

            for (int i = 1; i < words.length; ++i) {
                if (yCoords[i] != yCoords[i - 1])
                    continue;  // First word on line.

                int index = lines[yCoords[i]].indexOf(words[i]);
                xCoords[i] = Math.round(
                        mLightPaint.measureText(lines[yCoords[i]], 0, index));
            }

            mDarkPaint = new Paint(mLightPaint);
            mDarkPaint.setColor(Color.DKGRAY);

            // XXX this is ignored? at least in the emulator
            setWatchFaceStyle(new WatchFaceStyle.Builder(WordClockService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setShowSystemUiTime(true)
                    .build());
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            // "For devices that use low-bit ambient mode, the screen supports fewer bits for each
            // color in ambient mode, so you should disable anti-aliasing."
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            // XXX
            // "For devices that require burn-in protection, avoid using large blocks of
            // white pixels in ambient mode and do not place content within 10 pixels of the
            // edge of the screen, since the system shifts the content periodically
            // to avoid pixel burn-in."
            //mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
            //        false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            // XXX enough to do this every 5 min
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            boolean wasInAmbientMode = isInAmbientMode();
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode != wasInAmbientMode) {
                if (mLowBitAmbient) {
                    boolean antiAlias = !inAmbientMode;
                    mLightPaint.setAntiAlias(antiAlias);
                    mDarkPaint.setAntiAlias(antiAlias);
                }
                invalidate();
            }
        }

        String[] words = new String[] {
                "it", "is", "half", "ten",
                "quarter", "twenty",
                "five", "minutes", "to",
                "past", "one", "three",
                "two", "four", "five",
                "six", "seven", "eight",
                "nine", "ten", "eleven",
                "twelve", "am", "pm"};
        int[] xCoords = new int[] {  // Filled in by onCreate().
                0, 0, 0, 0,
                0, 0,
                0, 0, 0,
                0, 0, 0,  // "past"...
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
                0, 0, 0};
        int[] yCoords = new int[] {
                0, 0, 0, 0,
                1, 1,
                2, 2, 2,
                3, 3, 3,  // "past"...
                4, 4, 4,
                5, 5, 5,
                6, 6, 6,
                7, 7, 7};
        String[] lines = new String[] {
                "it is half ten",
                "quarter twenty",
                "five minutes to",
                "past one three",
                "two four five",
                "six seven eight",
                "nine ten eleven",
                "twelve am pm",
        };

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // XXX: early return if nothing changed; a real paint is only needed every 5 minutes.
            mTime.setToNow();

            // After this, mTime.minute will be 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, or 55.
            // 0-4 is mapped to 4, etc. That seems better than rounding, because if you're
            // wondering if something that starts 10:00 has already started, then "10:00" will
            // mean that it's at least ten. With rounding (57.5-2.5 -> 0) this wouldn't work.
            mTime.minute -= mTime.minute % 5;

            int mask = IT | IS;

            if (mTime.minute > 30) {
                mTime.minute = 60 - mTime.minute;
                mask |= TO;
                mTime.hour++;
            } else if (mTime.minute != 0)
                mask |= PAST;
            switch (mTime.minute) {
                case 5: mask |= FIVE_H; break;
                case 10: mask |= TEN_H; break;
                case 15: mask |= QUARTER; break;
                case 20: mask |= TWENTY_H; break;
                case 25: mask |= TWENTY_H | FIVE_H; break;
                case 30: mask |= HALF; break;
            }

            mask |= mTime.hour < 12 ? AM : PM;
            mTime.hour %= 12;
            if (mTime.hour == 0)
                mask |= TWELVE;
            else if (mTime.hour == 2)
                mask |= TWO;
            else if (mTime.hour == 3)
                mask |= THREE;
            else
                mask |= ONE << (mTime.hour - 1);

            // Draw the background first, and then the active time on top of it.
            int x = 20;
            int y = 80;
            if (!isInAmbientMode()) {
                for (int i = 0; i < words.length; i++) {
                    if ((mask & (1 << i)) != 0) continue;
                    canvas.drawText(
                            words[i],
                            x + xCoords[i],
                            (int) (y + yCoords[i] * 0.5 * mLightPaint.getFontSpacing()),
                            mDarkPaint);
                }
            }

            for (int i = 0; i < words.length; i++) {
                if ((mask & (1 << i)) == 0) continue;

                canvas.drawText(
                        words[i],
                        x + xCoords[i],
                        (int) (y + yCoords[i] * 0.5 * mLightPaint.getFontSpacing()),
                        mLightPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WordClockService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WordClockService.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }
}