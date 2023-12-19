package com.example.esign.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast

object CommonUtils {

    private const val E_SIGN_PREF = "E_SIGN_PREF"
    private const val PIN = "PIN"

    fun showToast(text: String, context: Context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun setPIN(context: Context,  value: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(E_SIGN_PREF, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(PIN, value)
        editor.apply()
        Log.i("quangdo", "get check pin ${getPIN(context)}")
    }

    fun isPinSetup(context: Context) = getPIN(context = context).isBlank().not()

    fun getPIN(context: Context): String {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(E_SIGN_PREF, Context.MODE_PRIVATE)
        return sharedPreferences.getString(PIN, "") ?: ""
    }
}