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
import android.os.Handler;
import android.os.Message;
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
        private static final int MSG_UPDATE_TIME = 1;

        private Time mTime;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        // onTimeTick() is only called in ambient mode, so it can't be used to drive the watch.
        // Install a timer that fires every 5 minutes and use it to drive the watch in interactive
        // mode.
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what != MSG_UPDATE_TIME) return;

                invalidate();
                if (shouldTimerBeRunning()) {
                    // Set next timer to the next round multiple of 5 min, and then some.
                    Time time = new Time();
                    time.setToNow();
                    long nowMs = time.toMillis(false);
                    time.minute += 5 - time.minute % 5;
                    time.second = 1;
                    long delayMs = time.toMillis(false) - nowMs;
                    mUpdateTimeHandler
                            .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                }
            }
        };

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
        private int mTextWidth, mTextHeight;
        private double mLineHeight;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mTime = new Time();

            // Set up paints.
            mLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mLightPaint.setColor(Color.WHITE);
            mLightPaint.setTextSize(36f);
            mLightPaint.setTypeface(Typeface.DEFAULT_BOLD);

            mDarkPaint = new Paint(mLightPaint);
            mDarkPaint.setColor(Color.DKGRAY);

            // Figure out x and y coords of each word.
            for (int i = 1; i < words.length; ++i) {
                if (yCoords[i] != yCoords[i - 1])
                    continue;  // First word on line.

                int index = lines[yCoords[i]].indexOf(words[i]);
                xCoords[i] = Math.round(
                        mLightPaint.measureText(lines[yCoords[i]], 0, index));
            }
            mLineHeight = 0.5 * mLightPaint.getFontSpacing();
            for (int i = 0; i < words.length; i++) {
                yCoords[i] = (int)Math.round(yCoords[i] * mLineHeight);
            }

            // Measure size of the whole text block.
            for (int i = 0; i < lines.length; ++i)
                mTextWidth = Math.max(mTextWidth, Math.round(mLightPaint.measureText(lines[i])));
            mTextHeight = (int)Math.round(lines.length * mLineHeight);

            // Set up system UI stuff.
            // This seems to be ignored in the emulator, but seems to mostly works on the device.
            setWatchFaceStyle(new WatchFaceStyle.Builder(WordClockService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.END | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.END | Gravity.BOTTOM)
                    .setShowSystemUiTime(false)
                    .build());
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            // "For devices that use low-bit ambient mode, the screen supports fewer bits for each
            // color in ambient mode, so you should disable anti-aliasing."
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            // "For devices that require burn-in protection, avoid using large blocks of
            // white pixels in ambient mode and do not place content within 10 pixels of the
            // edge of the screen, since the system shifts the content periodically
            // to avoid pixel burn-in."
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            // NOTE: This is only called in ambient mode.
            super.onTimeTick();

            // TODO: onTimeTick() is called every minute, but the watch face only changes
            // appearance every 5 minutes. Only call this when the face would actually draw
            // something new.
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mLightPaint.setAntiAlias(antiAlias);
                mDarkPaint.setAntiAlias(antiAlias);
            }
            if (mBurnInProtection) {
                boolean fill = !inAmbientMode;
                mLightPaint.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
                mDarkPaint.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
            }
            invalidate();
            updateTimer();
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
                case 5: mask |= FIVE_H | MINUTES; break;
                case 10: mask |= TEN_H | MINUTES; break;
                case 15: mask |= QUARTER; break;
                case 20: mask |= TWENTY_H | MINUTES; break;
                case 25: mask |= TWENTY_H | FIVE_H | MINUTES; break;
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

            // Clear background.
            canvas.drawColor(Color.BLACK);

            // Draw the background first, and then the active time on top of it.
            int x = (canvas.getWidth() - mTextWidth) / 2;
            int y = (canvas.getHeight() - mTextHeight) / 2
                    + (int)Math.round(mLineHeight);
            if (!isInAmbientMode()) {
                for (int i = 0; i < words.length; i++) {
                    if ((mask & (1 << i)) != 0) continue;

                    canvas.drawText(words[i], x + xCoords[i], y + yCoords[i], mDarkPaint);
                }
            }

            for (int i = 0; i < words.length; i++) {
                if ((mask & (1 << i)) == 0) continue;

                canvas.drawText(words[i], x + xCoords[i], y + yCoords[i], mLightPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            updateTimer();
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