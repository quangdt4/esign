package com.example.esign.pds.page

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.SizeF
import com.example.esign.pdf.PDSPDFPage

class PDSRenderPageAsyncTask internal constructor(
    context: Context?,
    fASPage: PDSPDFPage,
    sizeF: SizeF?,
    f: Float,
    z: Boolean,
    z2: Boolean,
    onPostExecuteListener: OnPostExecuteListener?
) : AsyncTask<Void?, Void?, Bitmap?>() {
    private var mBitmapSize: SizeF? = null
    private var mContext: Context? = null
    private var mForPrint = false
    private var mImageViewSize: SizeF? = null
    private var mIncludePageElements = false
    private var mListener: OnPostExecuteListener? = null
    private val mPage: PDSPDFPage
    var scale = 1.0f

    interface OnPostExecuteListener {
        fun onPostExecute(fASRenderPageAsyncTask: PDSRenderPageAsyncTask, bitmap: Bitmap?)
    }

    init {
        mContext = context
        mPage = fASPage
        mImageViewSize = sizeF
        scale = f
        mIncludePageElements = z
        mForPrint = z2
        mListener = onPostExecuteListener
    }

    /* Access modifiers changed, original: protected|varargs */
    override fun doInBackground(vararg p0: Void?): Bitmap? {
        if (isCancelled || mImageViewSize!!.width <= 0.0f) {
            return null
        }
        mBitmapSize = computePageBitmapSize()
        if (isCancelled) {
            return null
        }
        var width: Float = mBitmapSize!!.width * scale - 500.0f
        var height: Float = mBitmapSize!!.height * scale - 500.0f
        val f = width / height
        if (width > 2000.0f && width > height) {
            height = 2000.0f / f
            width = 2000.0f
        } else if (height > 2000.0f && height > width) {
            width = f * 2000.0f
            height = 2000.0f
        }
        if (width > height) {
            var temp = width
            width = height
            height = temp
        }

        return try {
            var createBitmap =
                Bitmap.createBitmap(Math.round(width), Math.round(height), Bitmap.Config.ARGB_8888)
            if (isCancelled) {
                return null
            }
            createBitmap.setHasAlpha(false)
            if (isCancelled) {
                createBitmap.recycle()
                return null
            }
            mPage.renderPage(createBitmap, mIncludePageElements, mForPrint)
            if (!isCancelled) {
                return createBitmap
            }
            createBitmap.recycle()
            null
        } catch (unused: OutOfMemoryError) {
            null
        }
    }

    /* Access modifiers changed, original: protected */
    public override fun onPostExecute(bitmap: Bitmap?) {
        if (mListener != null) {
            mListener!!.onPostExecute(this, bitmap)
        }
    }

    /* Access modifiers changed, original: protected */
    public override fun onCancelled(bitmap: Bitmap?) {
        if (mListener != null) {
            mListener!!.onPostExecute(this, null)
        }
    }

    val bitmapSize: SizeF?
        get() = mBitmapSize
    val page: PDSPDFPage
        get() = mPage

    private fun computePageBitmapSize(): SizeF {
        var width: Float
        val pageSize: SizeF = mPage.pageSize!!
        var width2: Float = pageSize.width / pageSize.height
        if (width2 > mImageViewSize!!.width / mImageViewSize!!.height) {
            width = mImageViewSize!!.width
            if (width > 2000.0f) {
                width = 2000.0f
            }
            width2 = Math.round(width / width2).toFloat()
        } else {
            width = mImageViewSize!!.height
            if (width > 2000.0f) {
                width = 2000.0f
            }
            val f = width2 * width
            width2 = width
            width = f
        }
        return SizeF(width, width2)
    }

    companion object {
        private const val MAX_BITMAP_SIZE = 3072
    }
}