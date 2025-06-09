package ru.sevostyanov.aiscemetery.views

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var matrix = Matrix()
    private var mode = NONE
    
    // Масштабирование
    private var minScale = 1f
    private var maxScale = 3f
    private var saveScale = 1f
    
    // Перетаскивание
    private var start = PointF()
    private var last = PointF()
    private var width = 0f
    private var height = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var bmWidth = 0f
    private var bmHeight = 0f
    
    private var scaleDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    
    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val CLICK = 3
    }
    
    init {
        super.setClickable(true)
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
        
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }
    
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null) {
            bmWidth = drawable.intrinsicWidth.toFloat()
            bmHeight = drawable.intrinsicHeight.toFloat()
            fitImageToView()
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        
        // Если изображение уже загружено, подгоняем его под view
        if (drawable != null && viewWidth > 0 && viewHeight > 0) {
            fitImageToView()
        }
    }
    
    private fun fitImageToView() {
        if (bmWidth == 0f || bmHeight == 0f || viewWidth == 0 || viewHeight == 0) return
        
        val scaleX = viewWidth.toFloat() / bmWidth
        val scaleY = viewHeight.toFloat() / bmHeight
        val scale = min(scaleX, scaleY)
        
        matrix.reset()
        
        // Центрируем изображение
        val redundantYSpace = viewHeight - (scale * bmHeight)
        val redundantXSpace = viewWidth - (scale * bmWidth)
        
        matrix.postScale(scale, scale)
        matrix.postTranslate(redundantXSpace / 2f, redundantYSpace / 2f)
        
        width = viewWidth.toFloat()
        height = viewHeight.toFloat()
        saveScale = scale
        minScale = scale
        maxScale = scale * 3f
        
        imageMatrix = matrix
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        val curr = PointF(event.x, event.y)
        
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                last.set(curr)
                start.set(last)
                mode = DRAG
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                last.set(curr)
                start.set(last)
                mode = ZOOM
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    val fixTransX = getFixDragTrans(deltaX, width, bmWidth * saveScale)
                    val fixTransY = getFixDragTrans(deltaY, height, bmHeight * saveScale)
                    matrix.postTranslate(fixTransX, fixTransY)
                    fixTrans()
                    last.set(curr.x, curr.y)
                }
            }
            
            MotionEvent.ACTION_UP -> {
                mode = NONE
                val xDiff = abs(curr.x - start.x)
                val yDiff = abs(curr.y - start.y)
                if (xDiff < CLICK && yDiff < CLICK) {
                    performClick()
                }
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }
        
        imageMatrix = matrix
        invalidate()
        return true
    }
    
    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else {
            delta
        }
    }
    
    private fun fixTrans() {
        val transX = getMatrixTransX()
        val transY = getMatrixTransY()
        
        val fixTransX = getFixTrans(transX, width, bmWidth * saveScale)
        val fixTransY = getFixTrans(transY, height, bmHeight * saveScale)
        
        if (fixTransX != 0f || fixTransY != 0f) {
            matrix.postTranslate(fixTransX, fixTransY)
        }
    }
    
    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        
        return when {
            trans < minTrans -> -trans + minTrans
            trans > maxTrans -> -trans + maxTrans
            else -> 0f
        }
    }
    
    private fun getMatrixTransX(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MTRANS_X]
    }
    
    private fun getMatrixTransY(): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MTRANS_Y]
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }
            
            if (bmWidth * saveScale <= width || bmHeight * saveScale <= height) {
                matrix.postScale(
                    mScaleFactor, mScaleFactor,
                    width / 2, height / 2
                )
            } else {
                matrix.postScale(
                    mScaleFactor, mScaleFactor,
                    detector.focusX, detector.focusY
                )
            }
            
            fixTrans()
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Двойной тап для сброса масштаба
            if (saveScale > minScale + 0.1f) {
                // Возвращаем к минимальному масштабу
                val scale = minScale / saveScale
                saveScale = minScale
                matrix.postScale(scale, scale, width / 2, height / 2)
                fixTrans()
                imageMatrix = matrix
                invalidate()
            } else {
                // Увеличиваем
                val scale = 2f / saveScale
                saveScale = min(2f, maxScale)
                matrix.postScale(scale, scale, e.x, e.y)
                fixTrans()
                imageMatrix = matrix
                invalidate()
            }
            return true
        }
    }
} 