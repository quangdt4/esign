package com.example.esign.pds

import android.content.ClipData
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import com.benzveen.pdfdigitalsignature.Signature.SignatureView
import com.example.esign.pds.model.PDSElement
import com.example.esign.R
import com.example.esign.pds.page.PDSPageViewer
import com.example.esign.utils.ViewUtils

class PDSElementViewer(context: Context?, fASPageViewer: PDSPageViewer?, fASElement: PDSElement) {
    var isBorderShown = false
        private set
    private var mContainerView: RelativeLayout? = null
    private var mContext: Context? = null
    private var mElement: PDSElement? = null
    var elementView: View? = null
        private set
    private var mHasDragStarted = false
    private var mImageButton: ImageButton? = null
    private var mLastMotionX = 0.0f
    private var mLastMotionY = 0.0f
    private var mLongPress = false
    var pageViewer: PDSPageViewer? = null
    private var mResizeInitialPos = 0.0f

    internal inner class CustomDragShadowBuilder(view: View?, var mX: Int, var mY: Int) :
        View.DragShadowBuilder(view) {
        override fun onDrawShadow(canvas: Canvas) {}
        override fun onProvideShadowMetrics(point: Point, point2: Point) {
            super.onProvideShadowMetrics(point, point2)
            point2[(mX.toFloat() * pageViewer!!.scaleFactor).toInt()] =
                (mY.toFloat() * pageViewer!!.scaleFactor).toInt()
            point[(getView().getWidth()
                .toFloat() * pageViewer!!.scaleFactor).toInt()] = (getView().getHeight()
                .toFloat() * pageViewer!!.scaleFactor).toInt()
        }
    }

    internal inner class DragEventData(var viewer: PDSElementViewer, var x: Float, var y: Float)

    init {
        mContext = context
        pageViewer = fASPageViewer
        mElement = fASElement
        fASElement.mElementViewer = this
        createElement(fASElement)
    }

    val element: PDSElement?
        get() = mElement
    val containerView: RelativeLayout?
        get() = mContainerView
    val imageButton: ImageButton?
        get() = mImageButton!!

    private fun createElement(fASElement: PDSElement) {
        elementView = createElementView(fASElement)
        pageViewer!!.pageView!!.addView(elementView)
        elementView!!.tag = fASElement
        if (!isElementInModel) {
            addElementInModel(fASElement)
        }
        setListeners()
    }

    fun removeElement() {
        if (elementView!!.parent != null) {
            pageViewer!!.page.removeElement(elementView!!.tag as PDSElement)
            pageViewer!!.hideElementPropMenu()
            pageViewer!!.pageView!!.removeView(elementView)
        }
    }

    private fun createElementView(fASElement: PDSElement): View? {
        return when (fASElement.type) {
            PDSElement.PDSElementType.PDSElementTypeSignature -> {
                val createSignatureView: SignatureView = ViewUtils.createSignatureView(
                    mContext,
                    fASElement,
                    pageViewer!!.toViewCoordinatesMatrix
                )!!
                fASElement.rect = (
                    RectF(
                        fASElement.rect!!.left,
                        fASElement.rect!!.top,
                        fASElement.rect!!.left + pageViewer!!.mapLengthToPDFCoordinates(
                            createSignatureView.mLayoutWidth.toFloat()
                        ),
                        fASElement.rect!!.bottom
                    )
                )
                fASElement.strokeWidth = (
                    pageViewer!!.mapLengthToPDFCoordinates(
                        createSignatureView.strokeWidth
                    )
                )
                createSignatureView.setFocusable(true)
                createSignatureView.setFocusableInTouchMode(true)
                createSignatureView.setClickable(true)
                createSignatureView.setLongClickable(true)
                createResizeButton(createSignatureView)
                createSignatureView
            }

            PDSElement.PDSElementType.PDSElementTypeImage -> {
                val imageView: ImageView = ViewUtils.createImageView(
                    mContext,
                    fASElement,
                    pageViewer!!.toViewCoordinatesMatrix
                )!!
                imageView.setImageBitmap(fASElement.bitmap)
                fASElement.rect = (
                    RectF(
                        fASElement.rect!!.left,
                        fASElement.rect!!.top,
                        fASElement.rect!!.left + pageViewer!!.mapLengthToPDFCoordinates(
                            imageView.width.toFloat()
                        ),
                        fASElement.rect!!.bottom
                    )
                )
                imageView.isFocusable = true
                imageView.isFocusableInTouchMode = true
                imageView.isClickable = true
                imageView.isLongClickable = true
                imageView.invalidate()
                createResizeButton(imageView)
                imageView
            }

            else -> null
        }
    }

    private fun addElementInModel(fASElement: PDSElement) {
        pageViewer!!.page.addElement(fASElement)
    }

    private val isElementInModel: Boolean
        private get() {
            for (i in 0 until pageViewer!!.page.numElements) {
                if (pageViewer!!.page.getElement(i) === elementView!!.tag) {
                    return true
                }
            }
            return false
        }

    fun setListeners() {
        setTouchListener()
        setFocusListener()
    }

    private fun setTouchListener() {
        elementView!!.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(view: View): Boolean {
                view.requestFocus()
                mLongPress = true
                return true
            }
        })
        elementView!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                var action: Int = motionEvent.getAction()
                when (action and 255) {
                    0 -> {
                        mHasDragStarted = false
                        mLongPress = false
                        mLastMotionX = motionEvent.getX()
                        mLastMotionY = motionEvent.getY()
                    }

                    1 -> {
                        mHasDragStarted = false
                        pageViewer!!.setElementAlreadyPresentOnTap(true)
                        if (view !is SignatureView) {
                            view.visibility = View.VISIBLE
                        }
                        mContainerView!!.setVisibility(View.VISIBLE)
                    }

                    2 -> if (!mHasDragStarted) {
                        action = Math.abs((motionEvent.getX() - mLastMotionX).toInt())
                        val abs: Int = Math.abs((motionEvent.getY() - mLastMotionY).toInt())
                        val `access$700`: Int
                        `access$700` = if (mLongPress) {
                            MOTION_THRESHOLD_LONG_PRESS
                        } else {
                            MOTION_THRESHOLD
                        }
                        if (motionEvent.getX() >= 0.0f && motionEvent.getY() >= 0.0f && isBorderShown && (action > `access$700` || abs > `access$700`)) {
                            val x: Float = motionEvent.getX()
                            val y: Float = motionEvent.getY()
                            view.startDrag(
                                ClipData.newPlainText(
                                    "pos", String.format(
                                        "%d %d", *arrayOf<Any>(
                                            Integer.valueOf(Math.round(x)),
                                            Integer.valueOf(Math.round(y))
                                        )
                                    )
                                ),
                                CustomDragShadowBuilder(view, Math.round(x), Math.round(y)),
                                DragEventData(this@PDSElementViewer, x, y),
                                0
                            )
                            mHasDragStarted = true
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setFocusListener() {
        elementView!!.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(view: View, z: Boolean) {
                if (z) {
                    assignFocus()
                }
            }
        })
    }

    fun assignFocus() {
        pageViewer!!.showElementPropMenu(this)
    }

    fun relayoutContainer() {
        elementView!!.measure(Math.round(-2.0f), Math.round(-2.0f))
        elementView!!.layout(0, 0, elementView!!.measuredWidth, elementView!!.measuredHeight)
        mImageButton!!.measure(Math.round(-2.0f), Math.round(-2.0f))
        mImageButton!!.layout(0, 0, mImageButton!!.getMeasuredWidth(), mImageButton!!.getMeasuredHeight())
        val measuredWidth: Int = elementView!!.measuredWidth + mImageButton!!.getMeasuredHeight() / 2
        val measuredHeight = elementView!!.measuredHeight
        mImageButton!!.setVisibility(View.VISIBLE)
        mContainerView!!.setLayoutParams(ViewGroup.LayoutParams(measuredWidth, measuredHeight))
    }

    fun setResizeButtonImage() {
        mImageButton!!.setImageResource(R.drawable.ic_resize)
    }

    private fun createResizeButton(view: View) {
        mImageButton = ImageButton(mContext)
        mImageButton!!!!.setImageResource(R.drawable.ic_resize)
        mImageButton!!.setBackgroundColor(0)
        mImageButton!!.setPadding(0, 0, 0, 0)
        val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(-2, -2)
        layoutParams.addRule(11)
        layoutParams.addRule(15)
        mImageButton!!.setLayoutParams(layoutParams)
        mImageButton!!.measure(Math.round(-2.0f), Math.round(-2.0f))
        mImageButton!!.layout(0, 0, mImageButton!!.getMeasuredWidth(), mImageButton!!.getMeasuredHeight())
        mContainerView = RelativeLayout(mContext)
        mContainerView!!.setFocusable(false)
        mContainerView!!.setFocusableInTouchMode(false)
        mImageButton!!.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.getAction()) {
                    0 -> {
                        mResizeInitialPos = motionEvent.getRawX()
                        pageViewer!!.resizeInOperation = (true)
                    }

                    1, 3 -> pageViewer!!.resizeInOperation = (false)
                    2 -> if (pageViewer!!.resizeInOperation) {
                        val rawX: Float = (motionEvent.getRawX() - mResizeInitialPos) / 2.0f
                        if ((rawX <= -mContext!!.resources.getDimension(R.dimen.sign_field_step_size) || rawX >= mContext!!.resources.getDimension(
                                R.dimen.sign_field_step_size
                            )) && elementView!!.height.toFloat() + rawX >= mContext!!.resources.getDimension(
                                R.dimen.sign_field_min_height
                            ) && elementView!!.height.toFloat() + rawX <= mContext!!.resources.getDimension(
                                R.dimen.sign_field_max_height
                            )
                        ) {
                            mResizeInitialPos = motionEvent.getRawX()
                            pageViewer!!.modifyElementSignatureSize(
                                elementView!!.tag as PDSElement,
                                elementView!!,
                                mContainerView!!,
                                (elementView!!.width.toFloat() * rawX / elementView!!.height.toFloat()).toInt(),
                                rawX.toInt()
                            )
                        }
                    }
                }
                return true
            }
        })
    }

    fun showBorder() {
        changeColor(true)
        if (mContainerView!!.getParent() == null) {
            val signatureViewWidth: Int
            val signatureViewHeight: Int
            if (elementView!!.parent === pageViewer!!.pageView) {
                elementView!!.onFocusChangeListener = null
                pageViewer!!.pageView!!.removeView(elementView)
                mContainerView!!.addView(elementView)
            }
            mContainerView!!.addView(mImageButton!!)
            mContainerView!!.setX(elementView!!.x)
            mContainerView!!.setY(elementView!!.y)
            elementView!!.x = 0.0f
            elementView!!.y = 0.0f
            if (elementView is SignatureView) {
                signatureViewWidth =
                    (elementView as SignatureView?)!!.mLayoutWidth + mImageButton!!.getMeasuredWidth() / 2
                signatureViewHeight = (elementView as SignatureView?)!!.mLayoutHeight
            } else {
                // this.mElementView.measure(Math.round(-2.0f), Math.round(-2.0f));
                // this.mElementView.layout(0, 0, this.mElementView.getMeasuredWidth(), this.mElementView.getMeasuredHeight());
                signatureViewWidth =
                    elementView!!.layoutParams.width + mImageButton!!.getMeasuredWidth() / 2
                signatureViewHeight = elementView!!.layoutParams.height
            }
            mContainerView!!.setLayoutParams(
                RelativeLayout.LayoutParams(
                    signatureViewWidth,
                    signatureViewHeight
                )
            )
            pageViewer!!.pageView!!.addView(mContainerView)
        }
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setStroke(2, mContext!!.resources.getColor(R.color.colorAccent))
        elementView!!.background = gradientDrawable
        isBorderShown = true
    }

    fun hideBorder() {
        changeColor(false)
        if (mContainerView!!.getParent() === pageViewer!!.pageView) {
            elementView!!.x = mContainerView!!.getX()
            elementView!!.y = mContainerView!!.getY()
            pageViewer!!.pageView!!.removeView(mContainerView)
            mContainerView!!.removeView(mImageButton!!)
            if (elementView!!.parent === mContainerView) {
                mContainerView!!.removeView(elementView)
                pageViewer!!.pageView!!.addView(elementView)
                setFocusListener()
            }
        }
        elementView!!.background = null
        isBorderShown = false
    }

    fun changeColor(z: Boolean) {
        var color =
            if (z) mContext!!.resources.getColor(R.color.colorAccent) else ViewCompat.MEASURED_STATE_MASK
        if (elementView is SignatureView) {
            color = (elementView as SignatureView?)!!.actualColor
            (elementView as SignatureView?)!!.setStrokeColor(color)
        } else if (elementView is ImageView) {
            //((ImageView) this.mElementView).setColorFilter(color);
        }
    }

    companion object {
        private const val MOTION_THRESHOLD = 3
        private const val MOTION_THRESHOLD_LONG_PRESS = 12
    }
}