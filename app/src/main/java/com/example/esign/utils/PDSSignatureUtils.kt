package com.example.esign.utils

import android.content.Context
import android.view.View
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.benzveen.pdfdigitalsignature.Signature.SignatureView
import com.example.esign.R
import java.io.File

object PDSSignatureUtils {
    private val sSignaturePopUpMenu: PopupWindow? = null
    private var mSignatureLayout: View? = null
    fun showFreeHandView(mCtx: Context, file: File): SignatureView {
        val createFreeHandView: SignatureView = SignatureUtils.createFreeHandView(
            mCtx.resources.getDimension(R.dimen.sign_menu_width)
                .toInt() - mCtx.resources.getDimension(R.dimen.sign_left_offset)
                .toInt() - mCtx.resources.getDimension(R.dimen.sign_right_offset).toInt() * 3,
            mCtx.resources.getDimension(R.dimen.sign_button_height)
                .toInt() - mCtx.resources.getDimension(R.dimen.sign_top_offset).toInt(),
            file,
            mCtx
        )!!
        val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(-2, -2)
        layoutParams.addRule(9)
        layoutParams.setMargins(
            mCtx.resources.getDimension(R.dimen.sign_left_offset).toInt(),
            mCtx.resources.getDimension(R.dimen.sign_top_offset).toInt(),
            0,
            0
        )
        createFreeHandView.setLayoutParams(layoutParams)
        return createFreeHandView

        /* createFreeHandView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FASSignatureUtils.addSignElement(z);
            }
        });*/
    }

    val isSignatureMenuOpen: Boolean
        get() = sSignaturePopUpMenu != null && sSignaturePopUpMenu.isShowing()

    fun dismissSignatureMenu() {
        if (sSignaturePopUpMenu != null && sSignaturePopUpMenu.isShowing()) {
            sSignaturePopUpMenu.dismiss()
            mSignatureLayout = null
        }
    }
}