package com.example.esign.pds.model

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.esign.pds.PDSElementViewer
import java.io.File

class PDSElement {
    var horizontalPadding = 0.0f
    var letterSpace = 0.0f
    var maxWidth = 0.0f
    var minWidth = 0.0f
    private var mRect: RectF? = null
    var size = 0.0f
    var strokeWidth = 0.0f
    var type = PDSElementType.PDSElementTypeSignature
        private set
    var mElementViewer: PDSElementViewer? = null
    var file: File? = null
        private set
    var bitmap: Bitmap? = null
        private set
    var verticalPadding = 0.0f
    var alises: String? = null

    enum class PDSElementType {
        PDSElementTypeImage, PDSElementTypeSignature
    }

    constructor(fASElementType: PDSElementType, file: File?) {
        type = fASElementType
        this.file = file
    }

    constructor(fASElementType: PDSElementType, file: Bitmap?) {
        type = fASElementType
        bitmap = file
    }

    var rect: RectF?
        get() = mRect
        set(rectF) {
            mRect = rectF
        }
}