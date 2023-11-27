package com.example.esign.pdf

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class PDSPDFDocument(context: Context, document: Uri?) {
    var numPages: Int
        private set
    private var mPages: HashMap<Int?, PDSPDFPage?>? = null

    @Transient
    var renderer: PDDocument? = null
        private set
    var documentUri: Uri? = null
    var stream: InputStream?
    var context: Context? = null

    init {
        numPages = -1
        mPages = HashMap()
        documentUri = document
        this.context = context
        stream = context.contentResolver.openInputStream(document!!)
    }

    fun open() {
        val open = context!!.contentResolver.openFileDescriptor(documentUri!!, "r")
        val inputStream = context!!.contentResolver.openInputStream(documentUri!!) ?: return
        if (isValidPDF(open)) {
            synchronized(lockObject) {
                try {
                    renderer = PDDocument.load(inputStream)
                    numPages = renderer!!.numberOfPages
                } catch (unused: Exception) {
                    open?.close()
                    throw IOException()
                } catch (_: Throwable) {
                }
            }
            return
        }
        open!!.close()
        throw IOException()
    }

    fun close() {
        if (renderer != null) {
            renderer!!.close()
            renderer = null
        }
    }

    fun getPage(i: Int): PDSPDFPage? {
        if (i >= numPages || i < 0) {
            return null
        }
        val fASPDFPage = mPages!![Integer.valueOf(i)]
        if (fASPDFPage != null) {
            return fASPDFPage
        }
        val fASPDFPage2 = PDSPDFPage(i, this)
        mPages!![Integer.valueOf(i)] = fASPDFPage2
        return fASPDFPage2
    }

    private fun isValidPDF(parcelFileDescriptor: ParcelFileDescriptor?): Boolean {
        return try {
            val bArr = ByteArray(4)
            FileInputStream(parcelFileDescriptor!!.fileDescriptor).read(bArr) == 4 && bArr[0] == 37.toByte() && bArr[1] == 80.toByte() && bArr[2] == 68.toByte() && bArr[3] == 70.toByte()
        } catch (unused: IOException) {
            false
        }
    }

    companion object {
        @Transient
        val lockObject = Any()
    }
}