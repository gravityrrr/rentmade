package com.example.made.util

import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

private const val SWIPE_MIN_DISTANCE = 72
private const val SWIPE_MIN_VELOCITY = 110

fun AppCompatActivity.navigateTabInstant(target: Class<*>) {
    if (this::class.java == target) return
    startActivity(Intent(this, target))
    overridePendingTransition(0, 0)
    finish()
}

fun attachTabSwipeNavigation(
    activity: AppCompatActivity,
    touchSurface: View,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
) {
    val detector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val start = e1 ?: return false
            val deltaX = e2.x - start.x
            val deltaY = e2.y - start.y
            val absDeltaX = abs(deltaX)
            val absDeltaY = abs(deltaY)
            val absVelocityX = abs(velocityX)
            if (absDeltaX < SWIPE_MIN_DISTANCE || absVelocityX < SWIPE_MIN_VELOCITY) return false
            if (absDeltaX < absDeltaY) return false

            if (deltaX < 0) {
                onSwipeLeft?.invoke()
            } else {
                onSwipeRight?.invoke()
            }
            return true
        }
    })

    touchSurface.setOnTouchListener { _, event ->
        detector.onTouchEvent(event)
        false
    }

    activity.window.decorView.setOnTouchListener { _, event ->
        detector.onTouchEvent(event)
        false
    }
}
