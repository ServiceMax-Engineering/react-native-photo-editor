package com.ahmedadeltito.photoeditorsdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.*

open class CustomBrushDrawingView(context: Context, attributeSet: AttributeSet?, defStyle: Int) :
    BrushDrawingView(context, attributeSet, defStyle) {
    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0)
    constructor(context: Context) : this(context, null, 0)


    private var drawPath = Path()
    private var drawPaint = Paint()
    private var canvasPaint = Paint()

    private var drawCanvas: Canvas? = null
    private var canvasBitMap: Bitmap? = null

    enum class DrawingMode {
        Brush,
        Arrow
    }

    private var drawingMode: DrawingMode = DrawingMode.Brush

    private var initialPoint: PointF? = null
    private var isDrawing = false
    private var photoEditorSDKListener: OnPhotoEditorSDKListener? = null

    override fun setupBrushDrawing() {
        drawPaint = Paint()
        drawPath = Path()
        refreshArrowDrawing()
        canvasPaint = Paint(Paint.DITHER_FLAG)
        super.setupBrushDrawing()
    }

    private fun refreshArrowDrawing() {
        val paintObject = drawPaint
        paintObject.isAntiAlias = true
        paintObject.isDither = true
        paintObject.style = Paint.Style.STROKE
        paintObject.strokeJoin = Paint.Join.ROUND
        paintObject.strokeCap = Paint.Cap.ROUND
        paintObject.strokeWidth = brushSize
        paintObject.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        initialPoint = null
    }

    override fun setBrushColor(color: Int) {
        super.setBrushColor(color)
        refreshArrowDrawing()
        drawPaint.color = brushColor
    }

    override fun setBrushSize(size: Float) {
        super.setBrushSize(size)
        refreshArrowDrawing()
    }

    fun setDrawingMode(drawingMode: DrawingMode) {
        this.drawingMode = drawingMode
        when (drawingMode) {
            DrawingMode.Brush -> {
                drawPaint.style = Paint.Style.STROKE
            }
            DrawingMode.Arrow -> {
                drawPaint.style = Paint.Style.FILL_AND_STROKE
            }
        }
    }

    override fun setBrushDrawingMode(isDrawing: Boolean) {
        this.isDrawing = isDrawing
        if (isDrawing) {
            visibility = VISIBLE
            refreshArrowDrawing()
        } else {
            setDrawingMode(DrawingMode.Brush)
        }
    }

    override fun clearAll() {
        val c = drawCanvas ?: return
        c.drawColor(0, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        drawBitmapInCanvas(canvas, canvasBitMap, canvasPaint)
        drawDrawPathInCanvas(canvas, drawPath, drawPaint)
    }

    override fun setOnPhotoEditorSDKListener(onPhotoEditorSDKListener: OnPhotoEditorSDKListener?) {
        this.photoEditorSDKListener = onPhotoEditorSDKListener
    }


    private fun drawBitmapInCanvas(
        canvas: Canvas?,
        bitmap: Bitmap?,
        paint: Paint?
    ) {
        val c = canvas ?: return
        val b = bitmap ?: return
        val p = paint ?: return
        c.drawBitmap(b, 0f, 0f, p)
    }

    private fun drawDrawPathInCanvas(
        canvas: Canvas?,
        path: Path,
        paint: Paint
    ) {
        canvas ?: return
        canvas.drawPath(path, paint)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvasBitMap = bitmap
        drawCanvas = Canvas(bitmap)
    }

    private fun drawArrowInPath(
        path: Path,
        startPoint: PointF?,
        endPoint: PointF?,
        headLength: Float = 20.0f
    ) {
        startPoint ?: return
        endPoint ?: return
        val startX = startPoint.x
        val startY = startPoint.y
        val endX = endPoint.x
        val endY = endPoint.y
        val angle = atan2(endY - startY, endX - startX)
        path.reset()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val angleOffset = Math.PI / 6
        path.lineTo(
            (endX - (headLength * cos(angle + angleOffset))).toFloat(),
            (endY - (headLength * sin(angle + angleOffset))).toFloat()
        )
        path.lineTo(
            (endX - (headLength * cos(angle - angleOffset))).toFloat(),
            (endY - (headLength * sin(angle - angleOffset))).toFloat()
        )
        path.lineTo(endX, endY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawing) {
            return false
        }
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (drawingMode) {
                    DrawingMode.Brush -> {
                        drawPath.moveTo(x, y)
                    }
                    DrawingMode.Arrow -> {
                        initialPoint = PointF(x, y)
                    }
                }
                photoEditorSDKListener?.onStartViewChangeListener(ViewType.BRUSH_DRAWING)
            }
            MotionEvent.ACTION_MOVE -> {
                when (drawingMode) {
                    DrawingMode.Brush -> {
                        drawPath.lineTo(x, y)
                    }
                    DrawingMode.Arrow -> {
                        drawArrowInPath(drawPath, initialPoint, PointF(x, y))
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                when (drawingMode) {
                    DrawingMode.Brush -> {
                        drawDrawPathInCanvas(drawCanvas, drawPath, drawPaint)
                    }
                    DrawingMode.Arrow -> {
                        drawDrawPathInCanvas(drawCanvas, drawPath, drawPaint)
                    }
                }
                drawPath.reset()
                photoEditorSDKListener?.onStartViewChangeListener(ViewType.BRUSH_DRAWING)
                invalidate()
            }
        }
        return true
    }
}

