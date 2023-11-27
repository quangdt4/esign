package com.example.esign.utils

import android.content.Context
import android.widget.Toast

object CommonUtils {
    fun showToast(text: String, context: Context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}