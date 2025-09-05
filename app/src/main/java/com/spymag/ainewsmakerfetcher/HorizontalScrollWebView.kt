package com.spymag.ainewsmakerfetcher

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import kotlin.math.abs

/**
 * Custom WebView that handles horizontal scrolling properly, especially for tables.
 * Prevents parent scroll containers from interfering with horizontal scrolling gestures.
 */
class HorizontalScrollWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    
    private var startX = 0f
    private var startY = 0f
    private var isHorizontalScrolling = false
    private var isDragging = false
    
    companion object {
        private const val TOUCH_SLOP = 50f // Threshold for determining scroll direction
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isHorizontalScrolling = false
                isDragging = false
                // Allow parent to potentially intercept
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(event.x - startX)
                val deltaY = abs(event.y - startY)
                
                if (!isDragging && (deltaX > TOUCH_SLOP || deltaY > TOUCH_SLOP)) {
                    isDragging = true
                    
                    // Determine if this is primarily horizontal scrolling
                    isHorizontalScrolling = deltaX > deltaY && deltaX > TOUCH_SLOP
                    
                    if (isHorizontalScrolling) {
                        // Request that parent doesn't intercept touch events
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
                
                // If we're in horizontal scrolling mode, maintain the lock
                if (isHorizontalScrolling) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // Reset state and allow parent to intercept future touches
                isHorizontalScrolling = false
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        
        return super.onTouchEvent(event)
    }
}