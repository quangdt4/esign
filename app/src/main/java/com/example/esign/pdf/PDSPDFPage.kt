package com.example.esign.pdf

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.SizeF
import com.example.esign.pds.model.PDSElement
import com.example.esign.pds.page.PDSPageViewer
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.rendering.PDFRenderer

class PDSPDFPage(val number: Int, val document: PDSPDFDocument) {
    private val elements: ArrayList<PDSElement> = arrayListOf()
    private var mPageSize: SizeF? = null
    var pageViewer: PDSPageViewer? = null


    val pageSize: SizeF?
        get() {
            if (mPageSize == null) {
                synchronized(PDSPDFDocument.lockObject) {
                    synchronized(document) {
                        val page = document.renderer!!.getPage(number)
                        if (page.mediaBox.width > page.mediaBox.height) {
                            mPageSize = SizeF(page.mediaBox.height, page.mediaBox.width)
                        } else if (page.mediaBox.width < page.mediaBox.height) {
                            mPageSize = SizeF(page.mediaBox.width, page.mediaBox.height)
                        }
                    }
                }
            }
            return mPageSize
        }

    fun renderPage(bitmap: Bitmap?, z: Boolean, z2: Boolean) {
        val i = if (z2) 2 else 1
        synchronized(PDSPDFDocument.lockObject) {
            synchronized(document) {
                val openPage: PDPage =
                    document.renderer!!.getPage(number)
                if (openPage.mediaBox.width > openPage.mediaBox.height) {
                    mPageSize = SizeF(openPage.mediaBox.height, openPage.mediaBox.width)
                } else if (openPage.mediaBox.width < openPage.mediaBox.height) {
                    mPageSize = SizeF(openPage.mediaBox.width, openPage.mediaBox.height)
                }


                val render = PDFRenderer(document.renderer)
                val canvas = Canvas(bitmap!!)
                val paint = Paint()
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL

                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                val screenHeight = Resources.getSystem().displayMetrics.heightPixels

                canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
                render.renderPageToGraphics(number, paint, canvas)

            }
        }
    }

    fun removeElement(fASElement: PDSElement?) {
        elements.remove(fASElement)
    }

    fun addElement(fASElement: PDSElement) {
        elements.add(fASElement)
    }

    val numElements: Int
        get() = elements.size

    fun getElement(i: Int): PDSElement {
        return elements[i]
    }

    fun updateElement(
        fASElement: PDSElement,
        rectF: RectF?,
        f: Float,
        f2: Float,
        f3: Float,
        f4: Float
    ) {
        if (rectF != fASElement.rect) {
            fASElement.rect = rectF
        }
        if (!(f == 0.0f || f == fASElement.size)) {
            fASElement.size = f
        }
        if (!(f2 == 0.0f || f2 == fASElement.maxWidth)) {
            fASElement.maxWidth = f2
        }
        if (!(f3 == 0.0f || f3 == fASElement.strokeWidth)) {
            fASElement.strokeWidth = f3
        }
        if (!(f4 == 0.0f || f4 == fASElement.letterSpace)) {
            fASElement.letterSpace = f4
        }
    }

}