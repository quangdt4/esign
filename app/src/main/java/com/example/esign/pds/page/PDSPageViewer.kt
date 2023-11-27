package com.example.esign.pds.page

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.AsyncTask
import android.os.Build
import android.os.SystemClock
import android.util.SizeF
import android.view.DragEvent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.OverScroller
import android.widget.RelativeLayout
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
import com.benzveen.pdfdigitalsignature.Signature.SignatureView
import com.example.esign.DocumentActivity
import com.example.esign.pdf.PDSPDFPage
import com.example.esign.pds.model.PDSElement
import com.example.esign.R
import com.example.esign.pds.PDSElementViewer
import com.example.esign.utils.PDSSignatureUtils
import com.example.esign.utils.ViewUtils
import java.io.File
import java.util.Observable
import java.util.Observer

class PDSPageViewer(
    private val mContext: Context,
    activity: DocumentActivity?,
    pdfPage: PDSPDFPage
) : FrameLayout(
    mContext
), Observer {
    private val mImageView: ImageView
    private val mInflater: LayoutInflater
    private val mProgressView: LinearLayout
    private val mScaleGestureListener: ScaleGestureListener
    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    var mInitialRenderingTask: PDSRenderPageAsyncTask? = null
    private var mRenderPageTask: PDSRenderPageAsyncTask? = null
    var scaleFactor = 1.0f
        private set
    private var mScroll: PointF = PointF(0.0f, 0.0f)
    private var mFocus: PointF = PointF(0.0f, 0.0f)
    private var mStartScaleFactor = 1.0f
    private var maxScrollX = 0
    private var mMaxScrollY = 0
    private var mPageView: RelativeLayout? = null
    private var mScrollView: RelativeLayout? = null
    private var mScroller: OverScroller? = null
    private var mLastZoomTime: Long = 0
    private var mIsFirstScrollAfterIntercept = false
    private var mIsInterceptedScrolling = false
    private val mKeyboardHeight = 0
    private val mKeyboardShown = false
    var resizeInOperation = false
    private var mRenderPageTaskPending = false
    private var mBitmapScale = 1.0f
    private val mPage: PDSPDFPage
    var mInitialImageSize: SizeF? = null
    private var mImage: Bitmap? = null
    private var mImageContentRect: RectF? = null
    private var mToPDFCoordinatesMatrix: Matrix? = null
    private var mRenderingComplete = false
    var toViewCoordinatesMatrix: Matrix? = null
        private set
    private var mInterceptedDownX = 0.0f
    private var mInterceptedDownY = 0.0f
    private var mTouchSlop = 0.0f
    private var mLastDragPointX = -1.0f
    private var mLastDragPointY = -1.0f
    private var mElementPropMenu: View? = null
    var lastFocusedElementViewer: PDSElementViewer? = null
        private set
    private var mElementAlreadyPresentOnTap = false
    private var mElementCreationMenu: View? = null
    private var mTouchX = 0.0f
    private var mTouchY = 0.0f
    private var mDragShadowView: ImageView? = null
    var activity: DocumentActivity? = null

    init {
        this.activity = activity
        mPage = pdfPage
        mPage.pageViewer = (this)
        mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inflate: View = mInflater.inflate(R.layout.pdfviewer, null, false)
        addView(inflate)
        mScrollView = inflate.findViewById<RelativeLayout>(R.id.scrollview)
        mPageView = inflate.findViewById<RelativeLayout>(R.id.pageview)
        mImageView = inflate.findViewById<ImageView>(R.id.imageview)
        setHorizontalScrollBarEnabled(true)
        setVerticalScrollBarEnabled(true)
        setScrollbarFadingEnabled(true)
        mProgressView = findViewById<LinearLayout>(R.id.linlaProgress)
        mProgressView.setVisibility(View.VISIBLE)
        mScroller = OverScroller(getContext())
        mScaleGestureListener = ScaleGestureListener(this)
        mScaleGestureDetector = ScaleGestureDetector(mContext, mScaleGestureListener)
        mGestureDetector = GestureDetector(mContext, GestureListener(this))
        mGestureDetector!!.setIsLongpressEnabled(true)
        requestFocus()
    }

    override fun update(o: Observable, arg: Any) {}
    private fun attachListeners() {
        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                val z =
                    mGestureDetector!!.onTouchEvent(motionEvent) || mScaleGestureDetector!!.onTouchEvent(
                        motionEvent
                    )
                if (!(z || motionEvent.getAction() == 1)) {
                    motionEvent.getAction()
                }
                return z
            }
        })
        mPageView!!.setOnDragListener(object : OnDragListener {
            override fun onDrag(view: View, dragEvent: DragEvent): Boolean {
                when (dragEvent.getAction()) {
                    1 -> {
                        mLastDragPointX = -1.0f
                        mLastDragPointY = -1.0f
                    }

                    2 -> handleDragMove(dragEvent)
                    3 -> {
                        mLastDragPointX = dragEvent.getX()
                        mLastDragPointY = dragEvent.getY()
                    }

                    4 -> handleDragEnd(dragEvent)
                    5 -> {
                        mLastDragPointX = -1.0f
                        mLastDragPointY = -1.0f
                    }
                }
                return true
            }
        })
    }

    private inner class GestureListener private constructor() : GestureDetector.SimpleOnGestureListener() {
        internal constructor(fASPageViewer: PDSPageViewer?) : this()

        override fun onDown(motionEvent: MotionEvent): Boolean {
            mScroller!!!!.forceFinished(true)
            ViewCompat.postInvalidateOnAnimation(this@PDSPageViewer)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            motionEvent: MotionEvent,
            f: Float,
            f2: Float
        ): Boolean {
            if (motionEvent.getPointerCount() > 1 || SystemClock.elapsedRealtime() - mLastZoomTime < 200L) {
                return false
            }
            mScroller!!.abortAnimation()
            mScroller!!.fling(
                mScrollView!!.getScrollX(),
                mScrollView!!.getScrollY(),
                (-f).toInt() * 2,
                (-f2).toInt() * 2,
                0,
                maxScrollX,
                0,
                maxScrollY
            )
            ViewCompat.postInvalidateOnAnimation(this@PDSPageViewer)
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            motionEvent: MotionEvent,
            f: Float,
            f2: Float
        ): Boolean {
            return if (mIsInterceptedScrolling && mIsFirstScrollAfterIntercept) {
                mIsFirstScrollAfterIntercept = false
                false
            } else if (motionEvent.getPointerCount() > 1 || SystemClock.elapsedRealtime() - mLastZoomTime < 200L) {
                false
            } else {
                applyScroll(
                    mScrollView!!.getScrollX() + Math.round(
                        f
                    ), mScrollView!!.getScrollY() + Math.round(f2)
                )
                true
            }
        }

        override fun onLongPress(motionEvent: MotionEvent) {
            super.onLongPress(motionEvent)
            onTap(motionEvent, true)
        }

        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            super.onSingleTapUp(motionEvent)
            mElementAlreadyPresentOnTap = false
            onTap(motionEvent, false)
            return true
        }
    }

    private inner class ScaleGestureListener private constructor() :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {
        internal constructor(fASPageViewer: PDSPageViewer?) : this()

        override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
            return scaleBegin(scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY())
        }

        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            return scale(scaleGestureDetector.getScaleFactor())
        }

        override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {
            scaleEnd()
        }

        fun resetScale() {
            mFocus = PointF(0.0f, 0.0f)
            mScroll = PointF(0.0f, 0.0f)
            mStartScaleFactor = 1.0f
            scaleFactor = 1.0f
            maxScrollX = 0
            mMaxScrollY = 0
            mPageView!!.setScaleX(scaleFactor)
            mPageView!!.setScaleY(scaleFactor)
            mScrollView!!.scrollTo(0, 0)
            updateImageFoScale()
        }
    }

    fun resetScale() {
        mScaleGestureListener.resetScale()
    }

    private fun scaleBegin(f: Float, f2: Float): Boolean {
        mPageView!!.setPivotX(0.0f)
        mPageView!!.setPivotY(0.0f)
        mFocus.set(f + mScrollView!!.getScrollX().toFloat(), f2 + mScrollView!!.getScrollY().toFloat())
        mScroll.set(mScrollView!!.getScrollX().toFloat(), mScrollView!!.getScrollY().toFloat())
        mStartScaleFactor = scaleFactor
        if (lastFocusedElementViewer != null) {
            if (mElementPropMenu != null) {
                mElementPropMenu!!.visibility = View.INVISIBLE
            } else if (mElementCreationMenu != null) {
                mElementCreationMenu!!.visibility = View.INVISIBLE
            }
            lastFocusedElementViewer!!.hideBorder()
        } else if (mElementCreationMenu != null) {
            mElementCreationMenu!!.visibility = View.INVISIBLE
        }
        return true
    }

    private fun scale(f: Float): Boolean {
        scaleFactor *= f / 10000.0f * 10000.0f
        scaleFactor = scaleFactor / 10000.0f * 10000.0f
        scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f))
        maxScrollX = Math.round(mScrollView!!.getWidth().toFloat() * (scaleFactor - 1.0f))
            .toInt()
        mMaxScrollY = Math.round(mScrollView!!.getHeight().toFloat() * (scaleFactor - 1.0f))
            .toInt()
        var round = Math.round(mFocus.x * (scaleFactor / mStartScaleFactor - 1.0f) + mScroll.x)
            .toInt()
        var round2 = Math.round(mFocus.y * (scaleFactor / mStartScaleFactor - 1.0f) + mScroll.y)
            .toInt()
        round = Math.max(0, Math.min(round, maxScrollX))
        round2 = Math.max(0, Math.min(round2, maxScrollY))
        mPageView!!.setScaleX(scaleFactor)
        mPageView!!.setScaleY(scaleFactor)
        mScrollView!!.scrollTo(round, round2)
        invalidate()
        return true
    }

    private fun scaleEnd() {
        mLastZoomTime = SystemClock.elapsedRealtime()
        updateImageFoScale()
        if (lastFocusedElementViewer == null) {
            return
        }
        if (mElementPropMenu != null) {
            showElementPropMenu(lastFocusedElementViewer)
        } else if (mElementCreationMenu != null) {
            //  showElementCreationMenu(this.mLastFocusedElementViewer);
        }
    }

    private fun applyScroll(i: Int, i2: Int) {
        mScrollView!!.scrollTo(
            Math.max(0, Math.min(i, maxScrollX)),
            Math.max(0, Math.min(i2, maxScrollY))
        )
    }

    private val maxScrollY: Int
        private get() {
            val i = mMaxScrollY
            return if (mKeyboardShown) i + mKeyboardHeight else i
        }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        val actionMasked = MotionEventCompat.getActionMasked(motionEvent)
        if (!resizeInOperation || scaleFactor == 1.0f) {
            return false
        }
        if (lastFocusedElementViewer != null) {
            val rect = Rect()
            lastFocusedElementViewer!!.containerView!!.getHitRect(rect)
            if (rect.contains(
                    ((motionEvent.getX() + mScrollView!!.getScrollX()
                        .toFloat()) / scaleFactor).toInt(),
                    ((motionEvent.getY() + mScrollView!!.getScrollY()
                        .toFloat()) / scaleFactor).toInt()
                )
            ) {
                return false
            }
        }
        var z = true
        when (actionMasked) {
            1, 3 -> mIsInterceptedScrolling = false
            2 -> if (!mIsInterceptedScrolling) {
                val abs: Float = Math.abs(motionEvent.getX() - mInterceptedDownX)
                val abs2: Float = Math.abs(motionEvent.getY() - mInterceptedDownY)
                if (abs > mTouchSlop || abs2 > mTouchSlop) {
                    mIsInterceptedScrolling = true
                    mIsFirstScrollAfterIntercept = true
                }
            }

            0 -> {
                mIsInterceptedScrolling = false
                mInterceptedDownX = motionEvent.getX()
                mInterceptedDownY = motionEvent.getY()
                mTouchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop.toFloat()
            }
        }
        z = false
        return z
    }

    private fun onTap(motionEvent: MotionEvent, z: Boolean) {
        manualScale(motionEvent.getX(), motionEvent.getY())
        //getDocumentViewer().hideTrainingViewIfVisible();
        val x: Float = (motionEvent.getX() + mScrollView!!.getScrollX().toFloat()) / scaleFactor
        val y: Float = (motionEvent.getY() + mScrollView!!.getScrollY().toFloat()) / scaleFactor
        if (!imageContentRect!!.contains(
                RectF(
                    x,
                    y,
                    getResources().getDimension(R.dimen.element_min_width) + x + getResources().getDimension(
                        R.dimen.element_horizontal_padding
                    ) * 2.0f,
                    getResources().getDimension(R.dimen.element_min_width) + y + getResources().getDimension(
                        R.dimen.element_vertical_padding
                    ) * 2.0f
                )
            )
        ) {
            if (PDSSignatureUtils.isSignatureMenuOpen) {
                PDSSignatureUtils.dismissSignatureMenu()
            }
            removeFocus()
        } else {
            mTouchX = x
            mTouchY = y
            if (PDSSignatureUtils.isSignatureMenuOpen) {
                PDSSignatureUtils.dismissSignatureMenu()
            } else if (z) {
                onLongTap(x, y)
            } else {
                onSingleTap(x, y)
            }
        }
    }

    private fun onLongTap(f: Float, f2: Float) {
        if (lastFocusedElementViewer != null) {
            removeFocus()
        }
    }

    private fun onSingleTap(f: Float, f2: Float) {
        if (lastFocusedElementViewer != null) {
            removeFocus()
            hideElementCreationMenu()
        }
    }

    fun manualScale(f: Float, f2: Float) {
        if (scaleFactor == 1.0f && documentViewer.isFirstTap) {
            documentViewer.isFirstTap = (false)
            scaleBegin(f, f2)
            scale(1.15f)
            scale(1.3f)
            scale(1.45f)
            scaleEnd()
        }
    }

    val documentViewer: DocumentActivity
        get() = mContext as DocumentActivity

    private fun setImageBitmap(bitmap: Bitmap) {
        if (mImage != null) {
            mImage!!.recycle()
        }
        mImage = bitmap
        mImageView.setImageBitmap(bitmap)
    }

    val imageContentRect: RectF?
        get() = mImageContentRect

    override fun onLayout(z: Boolean, i: Int, i2: Int, i3: Int, i4: Int) {
        super.onLayout(z, i, i2, i3, i4)
        if (z && mImageView.width > 0) {
            initRenderingAsync()
        }
    }

    override fun onSizeChanged(i: Int, i2: Int, i3: Int, i4: Int) {
        if (mImageView.width > 0) {
            initRenderingAsync()
        }
        /* if (this.mImage != null && i3 == i) {
            i = i4 - i2;
            if (Math.abs(i) >= 200) {
                this.mKeyboardHeight = Math.abs(i) + ((int) getResources().getDimension(R.dimen.suggestion_bar_height));
                if (i2 < i4 && this.mLastFocusedElementViewer != null && !this.mKeyboardShown) {
                    this.mKeyboardShown = true;
                    float f = getVisibleRect().bottom - (((float) this.mKeyboardHeight) / this.mScaleFactor);
                    View elementView = this.mLastFocusedElementViewer.getElementView();
                    if (this.mLastFocusedElementViewer.getElementView() instanceof FASCombFieldView) {
                        elementView = this.mLastFocusedElementViewer.getContainerView();
                    }
                    float y = elementView.getY() + ((float) elementView.getHeight());
                    if (y > f) {
                        this.mPanY = Math.round(y - f);
                        applyScroll(this.mScrollView!!.getScrollX(), this.mScrollView!!.getScrollY() + Math.round(((float) this.mPanY) * this.mScaleFactor));
                    }
                    this.mSuggestionsBarView.setVisibility(0);
                    this.mSuggestionsBarView.invalidate();
                } else if (i2 > i4 && this.mKeyboardShown) {
                    this.mKeyboardShown = false;
                    applyScroll(this.mScrollView!!.getScrollX(), this.mScrollView!!.getScrollY() - Math.round(((float) this.mPanY) * this.mScaleFactor));
                    this.mPanY = 0;
                    this.mSuggestionsBarView.setVisibility(4);
                }
            }
        }*/
    }

    private fun initRenderingAsync() {
        if (mImage == null && mImageView.width > 0 && (mInitialRenderingTask == null || mInitialRenderingTask!!.status == AsyncTask.Status.FINISHED)) {
            mInitialRenderingTask = PDSRenderPageAsyncTask(
                mContext,
                mPage,
                SizeF(mImageView.width.toFloat(), mImageView.height.toFloat()),
                1.0f,
                false,
                false,
                object : PDSRenderPageAsyncTask.OnPostExecuteListener {
                    override fun onPostExecute(
                        fASRenderPageAsyncTask: PDSRenderPageAsyncTask,
                        bitmap: Bitmap?
                    ) {
                        if (bitmap != null && scaleFactor == 1.0f && fASRenderPageAsyncTask.page === mPage) {
                            val visibleWindowHeight: Int = documentViewer.visibleWindowHeight
                            if (visibleWindowHeight > 0) {
                                mScrollView!!.setLayoutParams(
                                    LayoutParams(
                                        -1,
                                        visibleWindowHeight
                                    )
                                )
                            }
                            mInitialImageSize = fASRenderPageAsyncTask.bitmapSize
                            computeImageContentRect()
                            computeCoordinateConversionMatrices()
                            /* if (PDSPageViewer.this.mPage.getNumber() == 0) {
                            FASDocStore instance = FASDocStore.getInstance(FASPageViewer.this.mContext);
                            String uuid = FASPageViewer.this.getDocumentViewer().getDocument().getUuid();
                            if (!instance.thumbnailExists(uuid)) {
                                instance.updateThumbnail(uuid);
                            }
                        }*/setImageBitmap(bitmap)
                            renderElements()
                            mProgressView.setVisibility(INVISIBLE)
                            attachListeners()
                        } else bitmap?.recycle()
                        mRenderingComplete = true
                    }
                })
            mInitialRenderingTask!!.execute(*arrayOfNulls(0))
        }
    }

    private fun renderElements() {
        for (i in 0 until mPage.numElements) {
            addElement(mPage.getElement(i))
        }
    }

    private fun computeImageContentRect() {
        var width: Float
        val f: Float
        var width2: Float = mInitialImageSize!!.getWidth() / mInitialImageSize!!.getHeight()
        var f2 = 0.0f
        if (width2 >= mImageView.width.toFloat() / mImageView.height.toFloat()) {
            width = mImageView.width.toFloat()
            width2 = width / width2
            f2 = (mImageView.height.toFloat() - width2) / 2.0f
            f = 0.0f
            val f3 = width
            width = width2
            width2 = f3
        } else {
            width = mImageView.height.toFloat()
            width2 *= width
            f = (mImageView.width.toFloat() - width2) / 2.0f
        }
        mImageContentRect = RectF(f, f2, width2 + f, width + f2)
    }

    private fun computeCoordinateConversionMatrices() {
        val pageSize: SizeF = mPage.pageSize!!
        val imageContentRect: RectF? = imageContentRect
        mToPDFCoordinatesMatrix = Matrix()
        mToPDFCoordinatesMatrix!!.postTranslate(
            0.0f - imageContentRect!!.left,
            0.0f - imageContentRect!!.top
        )
        mToPDFCoordinatesMatrix!!.postScale(
            pageSize.getWidth() / imageContentRect!!.width(),
            pageSize.getHeight() / imageContentRect!!.height()
        )
        toViewCoordinatesMatrix = Matrix()
        toViewCoordinatesMatrix!!.postScale(
            imageContentRect!!.width() / pageSize.getWidth(),
            imageContentRect!!.height() / pageSize.getHeight()
        )
        toViewCoordinatesMatrix!!.postTranslate(imageContentRect!!.left, imageContentRect!!.top)
    }

    @Synchronized
    private fun updateImageFoScale() {
        if (scaleFactor != mBitmapScale) {
            if (mRenderPageTask != null) {
                mRenderPageTask!!.cancel(false)
                if (mRenderPageTask!!.status == AsyncTask.Status.RUNNING) {
                    mRenderPageTaskPending = true
                    return
                }
            }
            mRenderPageTask = PDSRenderPageAsyncTask(
                mContext,
                mPage,
                SizeF(mImageView.width.toFloat(), mImageView.height.toFloat()),
                scaleFactor,
                false,
                false,
                object : PDSRenderPageAsyncTask.OnPostExecuteListener {
                    override fun onPostExecute(
                        fASRenderPageAsyncTask: PDSRenderPageAsyncTask,
                        bitmap: Bitmap?
                    ) {
                        if (bitmap != null && scaleFactor == fASRenderPageAsyncTask.scale) {
                            setImageBitmap(bitmap)
                            mBitmapScale = scaleFactor
                        } else bitmap?.recycle()
                        if (mRenderPageTaskPending) {
                            mRenderPageTask = null
                            mRenderPageTaskPending = false
                            updateImageFoScale()
                        }
                    }
                })
            mRenderPageTask!!.execute(*arrayOfNulls(0))
        }
    }

    val visibleRect: RectF
        get() = RectF(
            mScrollView!!.getScrollX().toFloat() / scaleFactor,
            mScrollView!!.getScrollY().toFloat() / scaleFactor,
            (mScrollView!!.getScrollX() + mPageView!!.getWidth()).toFloat() / scaleFactor,
            (mScrollView!!.getScrollY() + mPageView!!.getHeight()).toFloat() / scaleFactor
        )

    fun cancelRendering() {
        if (mInitialRenderingTask != null) {
            mInitialRenderingTask!!.cancel(false)
        }
    }

    override fun computeScroll() {
        if (!mScroller!!.isFinished()) {
            mScroller!!.computeScrollOffset()
            applyScroll(mScroller!!.getCurrX(), mScroller!!.getCurrY())
        }
    }

    override fun computeHorizontalScrollRange(): Int {
        return (Math.round(mScrollView!!.getWidth().toFloat() * scaleFactor) - 1).toInt()
    }

    override fun computeVerticalScrollRange(): Int {
        return (Math.round(mScrollView!!.getHeight().toFloat() * scaleFactor) - 1).toInt()
    }

    override fun computeHorizontalScrollOffset(): Int {
        return mScrollView!!.getScrollX()
    }

    override fun computeVerticalScrollOffset(): Int {
        return mScrollView!!.getScrollY()
    }

    val pageView: RelativeLayout?
        get() = mPageView
    val page: PDSPDFPage
        get() = mPage

    fun hideElementPropMenu() {
        if (mElementPropMenu != null) {
            mScrollView!!.removeView(mElementPropMenu)
            mElementPropMenu = null
        }
        if (lastFocusedElementViewer != null) {
            lastFocusedElementViewer!!.hideBorder()
            lastFocusedElementViewer = null
        }
    }

    fun createElement(
        fASElementType: PDSElement.PDSElementType,
        file: File?,
        f: Float,
        f2: Float,
        f3: Float,
        f4: Float
    ): PDSElement {
        val fASElement = PDSElement(fASElementType, file)
        //fASElement.setContent(fASElementContent);
        fASElement.rect = (mapRectToPDFCoordinates(RectF(f, f2, f + f3, f2 + f4)))
        val addElement = addElement(fASElement)
        if (fASElementType === PDSElement.PDSElementType.PDSElementTypeSignature) {
            addElement.elementView!!.requestFocus()
        }
        return fASElement
    }

    fun createElement(
        fASElementType: PDSElement.PDSElementType?,
        bitmap: Bitmap?,
        f: Float,
        f2: Float,
        f3: Float,
        f4: Float
    ): PDSElement {
        val fASElement = PDSElement(fASElementType!!, bitmap)
        //fASElement.setContent(fASElementContent);
        fASElement.rect = (mapRectToPDFCoordinates(RectF(f, f2, f + f3, f2 + f4)))
        val addElement = addElement(fASElement)
        addElement.elementView!!.requestFocus()
        return fASElement
    }

    private fun addElement(fASElement: PDSElement): PDSElementViewer {
        return PDSElementViewer(mContext, this, fASElement)
    }

    fun mapRectToPDFCoordinates(rectF: RectF?): RectF? {
        mToPDFCoordinatesMatrix!!.mapRect(rectF)
        return rectF
    }

    fun mapLengthToPDFCoordinates(f: Float): Float {
        return mToPDFCoordinatesMatrix!!.mapRadius(f)
    }

    fun setElementAlreadyPresentOnTap(z: Boolean) {
        mElementAlreadyPresentOnTap = z
    }

    fun showElementPropMenu(fASElementViewer: PDSElementViewer?) {
        hideElementPropMenu()
        hideElementCreationMenu()
        fASElementViewer!!.showBorder()
        lastFocusedElementViewer = fASElementViewer
        val inflate: View = mInflater.inflate(R.layout.element_prop_menu_layout, null, false)
        inflate.tag = fASElementViewer
        mScrollView!!.addView(inflate)
        mElementPropMenu = inflate
        (inflate.findViewById<View>(R.id.delButton) as ImageButton).setOnClickListener(
            View.OnClickListener {
                fASElementViewer.removeElement()
                activity!!.invokeMenuButton(false)
            })
        setMenuPosition(fASElementViewer.containerView, inflate)
    }

    fun hideElementCreationMenu() {
        if (mElementCreationMenu != null) {
            mScrollView!!.removeView(mElementCreationMenu)
            mElementCreationMenu = null
        }
    }

    private fun setMenuPosition(f: Float, f2: Float, view: View?, z: Boolean) {
        view!!.measure(0, 0)
        var f3 = scaleFactor * f
        if (z) {
            f3 -= (view.measuredWidth / 2).toFloat()
        }
        var dimension: Float =
            scaleFactor * f2 - getResources().getDimension(R.dimen.menu_offset_y).toInt()
                .toFloat()
        val rectF = RectF(
            f3,
            dimension,
            view.measuredWidth.toFloat() + f3,
            view.measuredHeight.toFloat() + dimension
        )
        val visibleRect: RectF = visibleRect
        visibleRect.intersect(imageContentRect!!)
        if (!visibleRect.contains(rectF)) {
            if (f3 > visibleRect.right * scaleFactor - view.measuredWidth.toFloat()) {
                f3 = visibleRect.right * scaleFactor - view.measuredWidth.toFloat()
            } else if (f3 < visibleRect.left * scaleFactor && z) {
                f3 = visibleRect.left * scaleFactor
            }
            if (dimension < visibleRect.top * scaleFactor) {
                if (z) {
                    dimension =
                        f2 * scaleFactor + getResources().getDimension(R.dimen.menu_offset_x)
                            .toInt()
                            .toFloat()
                } else if (f3 > visibleRect.left * scaleFactor + view.measuredWidth.toFloat() + getResources().getDimension(
                        R.dimen.menu_offset_x
                    ).toInt()
                        .toFloat()
                ) {
                    f3 =
                        f * scaleFactor - view.measuredWidth.toFloat() - getResources().getDimension(
                            R.dimen.menu_offset_x
                        ).toInt()
                            .toFloat()
                    dimension = f2 * scaleFactor
                }
            }
        }
        view.x = f3
        view.y = dimension
    }

    private fun setMenuPosition(view: View?, view2: View?) {
        setMenuPosition(view!!.x, view.y, view2, false)
    }

    fun mapLengthToViewCoordinates(f: Float): Float {
        return toViewCoordinatesMatrix!!.mapRadius(f)
    }

    fun modifyElementSignatureSize(
        fASElement: PDSElement,
        view: View,
        relativeLayout: RelativeLayout,
        i: Int,
        i2: Int
    ) {
        val f = i2.toFloat()
        if (imageContentRect!!.contains(
                RectF(
                    relativeLayout.x,
                    relativeLayout.y - f,
                    relativeLayout.x + relativeLayout.width.toFloat() + i.toFloat(),
                    relativeLayout.y + relativeLayout.height.toFloat()
                )
            )
        ) {
            if (view is SignatureView) {
                val signatureView = view
                signatureView.setLayoutParams(view.getWidth() + i, view.getHeight() + i2)
                relativeLayout.layoutParams =
                    RelativeLayout.LayoutParams(
                        relativeLayout.width + i,
                        relativeLayout.height + i2
                    )
                relativeLayout.y = relativeLayout.y - f
                mPage.updateElement(
                    fASElement, mapRectToPDFCoordinates(
                        RectF(
                            relativeLayout.x.toInt().toFloat(),
                            relativeLayout.y.toInt().toFloat(),
                            (relativeLayout.x + view.getWidth().toFloat()).toInt()
                                .toFloat(),
                            (relativeLayout.y + view.getHeight().toFloat()).toInt().toFloat()
                        )
                    ), 0.0f, 0.0f, signatureView.strokeWidth, 0.0f
                )
                setMenuPosition(relativeLayout, mElementPropMenu)
            } else if (view is ImageView) {
                view.layoutParams =
                    RelativeLayout.LayoutParams(view.getWidth() + i, view.getHeight() + i2)
                relativeLayout.layoutParams =
                    RelativeLayout.LayoutParams(
                        relativeLayout.width + i,
                        relativeLayout.height + i2
                    )
                relativeLayout.y = relativeLayout.y - f
                mPage.updateElement(
                    fASElement, mapRectToPDFCoordinates(
                        RectF(
                            relativeLayout.x.toInt().toFloat(),
                            relativeLayout.y.toInt().toFloat(),
                            (relativeLayout.x + view.getWidth().toFloat()).toInt()
                                .toFloat(),
                            (relativeLayout.y + view.getHeight().toFloat()).toInt().toFloat()
                        )
                    ), 0.0f, 0.0f, 0.0f, 0.0f
                )
                setMenuPosition(relativeLayout, mElementPropMenu)
            }
        }
    }

    fun removeFocus() {
        if (Build.VERSION.SDK_INT < 28) {
            clearFocus()
        }
        hideElementPropMenu()
        hideElementCreationMenu()
        if (Build.VERSION.SDK_INT >= 28) {
            mImageView.requestFocus()
        }
    }

    private fun handleDragMove(dragEvent: DragEvent) {
        val dragEventData: PDSElementViewer.DragEventData = dragEvent.getLocalState() as PDSElementViewer.DragEventData
        val fASElementViewer: PDSElementViewer = dragEventData.viewer
        val elementView = fASElementViewer.elementView
        mLastDragPointX = dragEvent.getX()
        mLastDragPointY = dragEvent.getY()
        if (mDragShadowView == null) {
            hideDragElement(fASElementViewer)
            fASElementViewer.element
            initDragShadow(elementView!!)
        }
        val rectF = RectF(
            mLastDragPointX - dragEventData.x,
            mLastDragPointY - dragEventData.y,
            elementView!!.width.toFloat() + mLastDragPointX - dragEventData.x,
            elementView!!.height.toFloat() + mLastDragPointY - dragEventData.y
        )
        ViewUtils.constrainRectXY(rectF, imageContentRect!!)
        mLastDragPointX = rectF.left + dragEventData.x
        mLastDragPointY = rectF.top + dragEventData.y
        updateDragShadow(mLastDragPointX, mLastDragPointY, dragEventData.x, dragEventData.y)
    }

    private fun updateDragShadow(f: Float, f2: Float, f3: Float, f4: Float) {
        if (mDragShadowView != null) {
            mDragShadowView!!.x = f - f3
            mDragShadowView!!.y = f2 - f4
        }
    }

    private fun handleDragEnd(dragEvent: DragEvent) {
        if (mLastDragPointX != -1.0f || mLastDragPointY != -1.0f) {
            val f: Float
            val dragEventData: PDSElementViewer.DragEventData = dragEvent.getLocalState() as PDSElementViewer.DragEventData
            val fASElementViewer: PDSElementViewer = dragEventData.viewer
            val elementView = fASElementViewer.elementView
            var f2: Float = mLastDragPointX - dragEventData.x
            var f3: Float = mLastDragPointY - dragEventData.y
            var width = elementView!!.width
            var height = elementView.height
            width = fASElementViewer.containerView!!.width
            height = fASElementViewer.containerView!!.height
            val f4 = width.toFloat()
            val f5 = height.toFloat()
            val rectF = RectF(f2, f3, f2 + f4, f3 + f5)
            var imageContentRect: RectF? = imageContentRect
            if (!imageContentRect!!.contains(rectF)) {
                if (f2 < imageContentRect!!.left) {
                    f2 = imageContentRect!!.left
                } else if (f2 > imageContentRect!!.right - f4) {
                    f2 = imageContentRect!!.right - f4
                }
                if (f3 < imageContentRect!!.top) {
                    f3 = imageContentRect!!.top
                } else if (f3 > imageContentRect!!.bottom - f5) {
                    f3 = imageContentRect!!.bottom - f5
                }
            }
            val containerView: RelativeLayout? = fASElementViewer.containerView
            containerView!!.setX(f2)
            containerView!!.setY(f3)
            containerView!!.setVisibility(View.VISIBLE)
            /* else {
                elementView.setX(f2);
                elementView.setY(f3);
            }*/elementView.visibility = View.VISIBLE
            f = 0.0f
            imageContentRect = RectF(
                f2.toInt().toFloat(),
                f3.toInt().toFloat(),
                (f2 + elementView.width.toFloat()).toInt()
                    .toFloat(),
                (f3 + elementView.height.toFloat()).toInt().toFloat()
            )
            mapRectToPDFCoordinates(imageContentRect)
            mPage.updateElement(
                elementView.tag as PDSElement,
                imageContentRect,
                0.0f,
                f,
                0.0f,
                0.0f
            )
            showElementPropMenu(fASElementViewer)
            releaseDragShadow()
        }
    }

    private fun hideDragElement(fASElementViewer: PDSElementViewer) {
        removeFocus()
        fASElementViewer.showBorder()
        fASElementViewer.containerView!!.visibility = View.INVISIBLE
        fASElementViewer.elementView!!.visibility = View.INVISIBLE
    }

    private fun initDragShadow(view: View) {
        if (mDragShadowView == null) {
            val createBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(createBitmap))
            mDragShadowView = ImageView(mContext)
            mDragShadowView!!.setImageBitmap(createBitmap)
            mDragShadowView!!.imageAlpha = DRAG_SHADOW_OPACITY
            mDragShadowView!!.layoutParams = RelativeLayout.LayoutParams(view.width, view.height)
            mPageView!!.addView(mDragShadowView)
        }
    }

    private fun releaseDragShadow() {
        if (mDragShadowView != null) {
            mDragShadowView!!.visibility = View.INVISIBLE
            mPageView!!.removeView(mDragShadowView)
            mDragShadowView = null
        }
    }

    companion object {
        private const val DRAG_SHADOW_OPACITY = 180
    }
}