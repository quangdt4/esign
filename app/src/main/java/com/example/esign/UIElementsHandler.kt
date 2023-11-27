package com.example.esign

import android.os.Handler
import android.os.Message
import java.lang.ref.WeakReference

class UIElementsHandler(fASDocumentViewer: DocumentActivity?) : Handler() {
    private val mActivity: WeakReference<DocumentActivity?>

    init {
        mActivity = WeakReference(fASDocumentViewer)
    }

    override fun handleMessage(message: Message) {
        val fASDocumentViewer = mActivity.get()
        if (fASDocumentViewer != null && message.what == 1) {
            fASDocumentViewer.fadePageNumberOverlay()
        }
        super.handleMessage(message)
    }
}