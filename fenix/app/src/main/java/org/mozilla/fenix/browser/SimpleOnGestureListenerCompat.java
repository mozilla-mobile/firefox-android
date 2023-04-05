/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser;

import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

/**
 * Defines a class extending {@link android.view.GestureDetector.SimpleOnGestureListener} that allows nullable MotionEvents parameters
 */
public class SimpleOnGestureListenerCompat extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(@Nullable MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(@Nullable MotionEvent e) {
    }

    @Override
    public boolean onScroll(@Nullable MotionEvent e1, @Nullable MotionEvent e2,
                            float distanceX, float distanceY) {
        return false;
    }

    @Override
    public boolean onFling(@Nullable MotionEvent e1, @Nullable MotionEvent e2, float velocityX,
                           float velocityY) {
        return false;
    }

    @Override
    public void onShowPress(@Nullable MotionEvent e) {
    }

    @Override
    public boolean onDown(@Nullable MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(@Nullable MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(@Nullable MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(@Nullable MotionEvent e) {
        return false;
    }

    @Override
    public boolean onContextClick(@Nullable MotionEvent e) {
        return false;
    }
}
