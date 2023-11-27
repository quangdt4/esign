package com.benzveen.pdfdigitalsignature.Signature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import com.example.esign.signature.FreeHandActivity

class SignatureView : RelativeLayout {
    var image: Bitmap?
        private set
    private var mBitmapPaint: Paint?
    private var mCanvas: Canvas?
    private var mGestureInkList: ArrayList<Float>?
    var mInkList: ArrayList<ArrayList<Float>>?
    private var mIsEditable: Boolean
    private var mIsFirstBoundingRect: Boolean
    var mLayoutHeight: Int
        private set
    var mLayoutWidth: Int
        private set
    private var mPath: Path?
    private var mQuadEndPointX: Float
    private var mQuadEndPointY: Float
    var mRectBottom: Float
    var mRectLeft: Float
    var mRectRight: Float
    var mRectTop: Float
    var mRedoInkList: ArrayList<ArrayList<Float>>?
    private var mSignatureCreationMode: Boolean
    var mStrokeColor: Int
    var actualColor = 0
    var mStrokeWidthInDocSpace: Float
    private var mTouchDownPointX: Float
    private var mTouchDownPointY: Float
    private var mX: Float
    private var mY: Float

    constructor(context: Context?) : super(context) {
        mX = 0.0f
        mY = 0.0f
        mQuadEndPointX = 0.0f
        mQuadEndPointY = 0.0f
        mTouchDownPointX = 0.0f
        mTouchDownPointY = 0.0f
        mStrokeWidthInDocSpace = 0.0f
        mStrokeColor = 0
        mIsFirstBoundingRect = true
        mRectLeft = 0.0f
        mRectTop = 0.0f
        mRectRight = 0.0f
        mRectBottom = 0.0f
        image = null
        mCanvas = null
        mPath = null
        mBitmapPaint = null
        mGestureInkList = null
        mInkList = null
        mRedoInkList = null
        mSignatureCreationMode = true
        mIsEditable = false
        mLayoutHeight = -1
        mLayoutWidth = -1
        initializeOverlayView()
    }

    constructor(context: Context?, i: Int, i2: Int) : super(context) {
        mX = 0.0f
        mY = 0.0f
        mQuadEndPointX = 0.0f
        mQuadEndPointY = 0.0f
        mTouchDownPointX = 0.0f
        mTouchDownPointY = 0.0f
        mStrokeWidthInDocSpace = 0.0f
        mStrokeColor = 0
        mIsFirstBoundingRect = true
        mRectLeft = 0.0f
        mRectTop = 0.0f
        mRectRight = 0.0f
        mRectBottom = 0.0f
        image = null
        mCanvas = null
        mPath = null
        mBitmapPaint = null
        mGestureInkList = null
        mInkList = null
        mRedoInkList = null
        mSignatureCreationMode = true
        mIsEditable = false
        mLayoutHeight = -1
        mLayoutWidth = -1
        mSignatureCreationMode = false
        mLayoutHeight = i2
        mLayoutWidth = i
        initializeOverlayView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        mX = 0.0f
        mY = 0.0f
        mQuadEndPointX = 0.0f
        mQuadEndPointY = 0.0f
        mTouchDownPointX = 0.0f
        mTouchDownPointY = 0.0f
        mStrokeWidthInDocSpace = 0.0f
        mStrokeColor = 0
        mIsFirstBoundingRect = true
        mRectLeft = 0.0f
        mRectTop = 0.0f
        mRectRight = 0.0f
        mRectBottom = 0.0f
        image = null
        mCanvas = null
        mPath = null
        mBitmapPaint = null
        mGestureInkList = null
        mInkList = null
        mRedoInkList = null
        mSignatureCreationMode = true
        mIsEditable = false
        mLayoutHeight = -1
        mLayoutWidth = -1
        initializeOverlayView()
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        mX = 0.0f
        mY = 0.0f
        mQuadEndPointX = 0.0f
        mQuadEndPointY = 0.0f
        mTouchDownPointX = 0.0f
        mTouchDownPointY = 0.0f
        mStrokeWidthInDocSpace = 0.0f
        mStrokeColor = 0
        mIsFirstBoundingRect = true
        mRectLeft = 0.0f
        mRectTop = 0.0f
        mRectRight = 0.0f
        mRectBottom = 0.0f
        image = null
        mCanvas = null
        mPath = null
        mBitmapPaint = null
        mGestureInkList = null
        mInkList = null
        mRedoInkList = null
        mSignatureCreationMode = true
        mIsEditable = false
        mLayoutHeight = -1
        mLayoutWidth = -1
        initializeOverlayView()
    }

    fun getmLayoutWidth(): Int {
        return this.mLayoutWidth
    }

    fun getmLayoutHeight(): Int {
        return this.mLayoutHeight
    }
    fun initializeOverlayView() {
        setWillNotDraw(false)
        mStrokeWidthInDocSpace = 3.0f
        mStrokeColor = ViewCompat.MEASURED_STATE_MASK
        mPath = Path()
        mBitmapPaint = Paint()
        mBitmapPaint!!.isAntiAlias = true
        mBitmapPaint!!.isDither = true
        mBitmapPaint!!.color = mStrokeColor
        mBitmapPaint!!.style = Paint.Style.STROKE
        mBitmapPaint!!.strokeJoin = Paint.Join.ROUND
        mBitmapPaint!!.strokeCap = Cap.ROUND
        mBitmapPaint!!.strokeWidth = mStrokeWidthInDocSpace
        mInkList = ArrayList<ArrayList<Float>>()
        mRedoInkList = ArrayList<ArrayList<Float>>()
        mX = 0.0f
        mY = 0.0f
        mTouchDownPointX = 0.0f
        mTouchDownPointY = 0.0f
        mIsFirstBoundingRect = true
        mRectLeft = 0.0f
        mRectTop = 0.0f
        mRectRight = 0.0f
        mRectBottom = 0.0f
        mIsEditable = false
    }

    fun initializeInkList(arrayList: ArrayList<ArrayList<Float>>) {
        mInkList = arrayList
    }

    fun scaleAndTranslatePath(
        arrayList: ArrayList<ArrayList<Float>>,
        rectF: RectF,
        f: Float,
        f2: Float,
        f3: Float,
        f4: Float
    ) {
        val size = arrayList!!.size
        for (i in 0 until size) {
            val arrayList2 = arrayList[i] as ArrayList<Any>?
            var i2 = 0
            while (i2 < arrayList2!!.size) {
                arrayList2.set(
                    i2,
                    java.lang.Float.valueOf((arrayList2[i2] as Number).toFloat() * f - f3)
                )
                val i3 = i2 + 1
                arrayList2.set(
                    i3,
                    java.lang.Float.valueOf((arrayList2[i3] as Number).toFloat() * f2 - f4)
                )
                i2 += 2
            }
        }
        mRectLeft = rectF.left * f
        mRectTop = rectF.top * f2
        mRectRight = rectF.right * f
        mRectBottom = rectF.bottom * f2
    }

    fun redrawPath() {
        if (mCanvas != null) {
            redrawPath(mCanvas)
        }
    }

    fun drawTransparent() {
        if (mCanvas != null) {
            mCanvas!!.drawColor(0, PorterDuff.Mode.CLEAR)
        }
    }

    fun fillColor() {
        if (mCanvas != null) {
            mCanvas!!.drawColor(-16776961, PorterDuff.Mode.DARKEN)
        }
    }

    fun setStrokeColor(i: Int) {
        mStrokeColor = i
        mBitmapPaint!!.color = i
        redrawPath()
    }

    fun setmActualColor(i: Int) {
        actualColor = i
    }

    var strokeWidth: Float
        get() = mStrokeWidthInDocSpace
        set(f) {
            var f = f
            if (f <= 0.0f) {
                f = 0.5f
            }
            mStrokeWidthInDocSpace = f
            mBitmapPaint!!.strokeWidth = mStrokeWidthInDocSpace
            invalidate()
            drawTransparent()
            redrawPath()
            invalidate()
        }

    fun setLayoutParams(i: Int, i2: Int) {
        image = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(image!!)
        strokeWidth = mStrokeWidthInDocSpace
        layoutParams = LayoutParams(i, i2)
        mLayoutHeight = i2
        mLayoutWidth = i
        val arrayList: ArrayList<ArrayList<Float>> = mInkList!!
        val boundingBox = boundingBox
        clear()
        initializeInkList(arrayList)
        scaleAndTranslatePath(arrayList, boundingBox, 1.0f, 1.0f, 0.0f, 0.0f)
    }

    val boundingBox: RectF
        get() = RectF(mRectLeft, mRectTop, mRectRight, mRectBottom)

    fun getInkList(): java.util.ArrayList<java.util.ArrayList<Float>>? {
        return mInkList
    }


    fun setEditable(z: Boolean) {
        mIsEditable = z xor true
    }

    private fun redrawPath(canvas: Canvas?) {
        val size = mInkList!!.size
        for (i in 0 until size) {
            val arrayList = mInkList!![i] as ArrayList<*>?
            touch_start(arrayList!![0] as Float, arrayList[1] as Float)
            var i2 = 2
            while (i2 < arrayList.size) {
                touch_move(arrayList[i2] as Float, arrayList[i2 + 1] as Float)
                i2 += 2
            }
            mPath!!.lineTo(mX, mY)
            canvas!!.drawPath(mPath!!, mBitmapPaint!!)
            mPath!!.reset()
        }
    }

    /* Access modifiers changed, original: protected */
    public override fun onDraw(canvas: Canvas) {
        if (!mSignatureCreationMode) {
            drawTransparent()
            redrawPath(canvas)
        } else if (image != null) {
            canvas.drawBitmap(image!!, 0.0f, 0.0f, null)
        }
    }

    private fun setBoundingRect(f: Float, f2: Float) {
        if (f < mRectLeft) {
            mRectLeft = f
        } else if (f > mRectRight) {
            mRectRight = f
        }
        if (f2 < mRectTop) {
            mRectTop = f2
        } else if (f2 > mRectBottom) {
            mRectBottom = f2
        }
    }

    private fun touch_start(f: Float, f2: Float) {
        var f = f
        mPath!!.reset()
        mPath!!.moveTo(f, f2)
        mQuadEndPointX = f
        mQuadEndPointY = f2
        mX = f
        mY = f2
        mTouchDownPointX = f
        mTouchDownPointY = f2
        mGestureInkList = ArrayList<Float>()
        mGestureInkList!!.add(java.lang.Float.valueOf(mX))
        mGestureInkList!!.add(java.lang.Float.valueOf(mY))
        if (mIsFirstBoundingRect) {
            f = mX
            mRectRight = f
            mRectLeft = f
            f = mY
            mRectBottom = f
            mRectTop = f
            mIsFirstBoundingRect = false
            return
        }
        setBoundingRect(mX, mY)
    }

    private fun touch_move(f: Float, f2: Float) {
        val abs = Math.abs(f - mX)
        val abs2 = Math.abs(f2 - mY)
        if (abs >= TOUCH_TOLERANCE || abs2 >= TOUCH_TOLERANCE) {
            mQuadEndPointX = (mX + f) / 2.0f
            mQuadEndPointY = (mY + f2) / 2.0f
            mPath!!.quadTo(mX, mY, mQuadEndPointX, mQuadEndPointY)
            mX = f
            mY = f2
        }
        mGestureInkList!!.add(java.lang.Float.valueOf(mX))
        mGestureInkList!!.add(java.lang.Float.valueOf(mY))
        setBoundingRect(mX, mY)
    }

    private fun touch_up(f: Float, f2: Float) {
        drawPointIfRequired(f, f2)
        mPath!!.lineTo(mX, mY)
        if (mCanvas != null) {
            mCanvas!!.drawPath(mPath!!, mBitmapPaint!!)
        }
        mPath!!.reset()
        mInkList!!.add(mGestureInkList!!)
        if (mRedoInkList!!.size != 0) {
            mRedoInkList!!.clear()
        }
    }

    private fun drawPointIfRequired(f: Float, f2: Float) {
        val abs = Math.abs(f - mTouchDownPointX)
        val abs2 = Math.abs(f2 - mTouchDownPointY)
        if (abs < TOUCH_TOLERANCE && abs2 < TOUCH_TOLERANCE) {
            mX = f
            mY = f2
            if (compareDoubleValues(abs.toDouble(), 0.0) && compareDoubleValues(
                    abs2.toDouble(),
                    0.0
                )
            ) {
                mY = f2 - 1.0f
            }
            mGestureInkList!!.add(java.lang.Float.valueOf(mX))
            mGestureInkList!!.add(java.lang.Float.valueOf(mY))
            setBoundingRect(mX, mY)
        }
    }

    private fun compareDoubleValues(d: Double, d2: Double): Boolean {
        return Math.abs(d - d2) < 0.001
    }

    val statusBarHeight: Int
        get() = try {
            val identifier = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (identifier > 0) {
                resources.getDimensionPixelSize(identifier)
            } else 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }

    /* Access modifiers changed, original: protected */
    public override fun onSizeChanged(i: Int, i2: Int, i3: Int, i4: Int) {
        super.onSizeChanged(i, i2, i3, i4)
        try {
            image = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(image!!)
            scaleAndTranslatePath(
                mInkList!!,
                RectF(mRectLeft, mRectTop, mRectRight, mRectBottom),
                if (i3 != 0) i.toFloat() / i3.toFloat() else 1.0f,
                if (i4 != 0) i2.toFloat() / i4.toFloat() else 1.0f,
                0.0f,
                0.0f
            )
            redrawPath()
        } catch (unused: IllegalArgumentException) {
        } catch (unused: OutOfMemoryError) {
        }
    }

    /* Access modifiers changed, original: protected */
    public override fun onMeasure(i: Int, i2: Int) {
        var i = i
        var i2 = i2
        if (!(mSignatureCreationMode || mLayoutWidth == -1 || mLayoutHeight == -1)) {
            i = MeasureSpec.makeMeasureSpec(mLayoutWidth, MeasureSpec.EXACTLY)
            i2 = MeasureSpec.makeMeasureSpec(mLayoutHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(i, i2)
    }

    /* Access modifiers changed, original: protected */
    public override fun onVisibilityChanged(view: View, i: Int) {
        super.onVisibilityChanged(view, i)
        if (i == 0) {
            redrawPath()
            invalidate()
        }
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        if (mIsEditable) {
            super.onTouchEvent(motionEvent)
            return true
        }
        val x = motionEvent.x
        val y = motionEvent.y
        enableToolBarButton()
        when (motionEvent.action) {
            0 -> {
                touch_start(x, y)
                invalidate()
            }

            1 -> {
                touch_up(x, y)
                drawTransparent()
                redrawPath()
                invalidate()
            }

            2 -> {
                val historySize = motionEvent.historySize
                var i = 0
                while (i < historySize) {
                    touch_move(motionEvent.getHistoricalX(i), motionEvent.getHistoricalY(i))
                    i++
                }
                touch_move(x, y)
                if (mCanvas != null) {
                    mCanvas!!.drawPath(mPath!!, mBitmapPaint!!)
                    mPath!!.reset()
                    mPath!!.moveTo(mQuadEndPointX, mQuadEndPointY)
                }
                invalidate()
            }
        }
        return true
    }

    fun enableToolBarButton() {
        if (context != null) {
            (context as FreeHandActivity).enableClear(true)
            (context as FreeHandActivity).enableSave(true)
        }
    }

    fun clear() {
        mX = 0.0f
        mY = 0.0f
        mRectLeft = 0.0f
        mRectTop = 0.0f
        mRectRight = 0.0f
        mRectBottom = 0.0f
        mIsFirstBoundingRect = true
        drawTransparent()
        mPath!!.reset()
        mInkList = ArrayList<ArrayList<Float>>()
        invalidate()
    }

    companion object {
        const val DEFAULT_STROKE_WIDTH = 3.0f
        private const val EPSILON_FOR_DOUBLE_COMPARISON = 0.001
        const val STROKE_WIDTH_MINIMUM = 2.0f
        private const val TOUCH_TOLERANCE = 0.1f
    }
}