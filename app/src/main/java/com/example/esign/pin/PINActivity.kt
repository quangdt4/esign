package com.example.esign.pin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.esign.utils.CommonUtils.setPIN
import androidx.appcompat.app.AppCompatActivity
import com.example.esign.R
import com.example.esign.utils.CommonUtils.getPIN
import com.example.esign.utils.CommonUtils.isPinSetup
import com.example.esign.utils.CommonUtils.showToast
import com.google.android.material.textfield.TextInputLayout

class PINActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pin)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUp()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setUp() {
        val pin: EditText = findViewById(R.id.edtPassword)
        val confirmPin: EditText = findViewById(R.id.edtConfirmPassword)
        val button = findViewById<Button>(R.id.btCreatePin)
        val buttonDelete = findViewById<Button>(R.id.btDeletePin)
        val error = findViewById<TextView>(R.id.tvError)
        val layout = findViewById<TextInputLayout>(R.id.layoutTextInput)

        if (isPinSetup(this)) {
            supportActionBar?.title = getString(R.string.delete_pin)
            layout.hint = getString(R.string.old_pin)
            button.visibility = View.GONE
            buttonDelete.visibility = View.VISIBLE
            buttonDelete.setOnClickListener {
                error.visibility = View.GONE
                val pinFirst = pin.text.toString()
                val pinConfirm = confirmPin.text.toString()
                val pinSaved = getPIN(this)
                when {
                    pinFirst.isBlank() || pinConfirm.isBlank() -> showToast("Field is blank!", this)

                    pinFirst == pinConfirm && pinFirst == pinSaved && pinConfirm == pinSaved -> {
                        setPIN(this, "")
                        showToast("PIN has deleted!", this)
                        finish()
                    }

                    else -> error.visibility = View.VISIBLE
                }
            }
        } else {
            supportActionBar?.title = getString(R.string.create_pin)
            layout.hint = getString(R.string.new_pin)
            button.visibility = View.VISIBLE
            buttonDelete.visibility = View.GONE
            button.setOnClickListener {
                when {
                    pin.text.toString().isBlank() || confirmPin.text.toString()
                        .isBlank() -> showToast("Field is blank!", this)

                    pin.text.toString() == confirmPin.text.toString() -> {
                        setPIN(this, pin.text.toString())
                        showToast("Create PIN success!", this)
                        finish()
                    }

                    else -> error.visibility = View.VISIBLE
                }
            }
        }
    }
}