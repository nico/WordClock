package de.amnoid.thakis.wordclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.TimeZone;

public class WordClockService extends CanvasWatchFaceService {
    public WordClockService() {
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        Time mTime;
        boolean mLowBitAmbient;

        boolean mRegisteredTimeZoneReceiver = false;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        Paint textPaint;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mTime = new Time();

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            /*
            XXX play with this
            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
            .setBackgroundVisibility(WatchFaceStyle
                                    .BACKGROUND_VISIBILITY_INTERRUPTIVE)
            .setShowSystemUiTime(false)
            .build());
             */
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
            // XXX setStatusBarGravity() ?
            // XXX setHotwordIndicatorGravity() ?
            // XXX setViewProtection() ?
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

            // XXX
            if (inAmbientMode != wasInAmbientMode) {
                if (mLowBitAmbient) {
                    boolean antiAlias = !inAmbientMode;
                    //mHourPaint.setAntiAlias(antiAlias);
                    //mMinutePaint.setAntiAlias(antiAlias);
                    //mSecondPaint.setAntiAlias(antiAlias);
                    //mTickPaint.setAntiAlias(antiAlias);
                }
                invalidate();
                //updateTimer();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            canvas.drawText("hello, wear!", 10, 100, textPaint);
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