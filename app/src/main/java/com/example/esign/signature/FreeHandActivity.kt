package com.example.esign.signature

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.benzveen.pdfdigitalsignature.Signature.SignatureView
import com.example.esign.R

class FreeHandActivity : AppCompatActivity() {
    private var isFreeHandCreated = false
    private var signatureView: SignatureView? = null
    private var inkWidth: SeekBar? = null
    private var menu: Menu? = null
    private var saveItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_hand)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        signatureView = findViewById(R.id.inkSignatureOverlayView)
        inkWidth = findViewById(R.id.seekBar)
        inkWidth!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                signatureView!!.strokeWidth = (progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<View>(R.id.action_clear).setOnClickListener {
            clearSignature()
            enableClear(false)
            enableSave(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.freehandmenu, menu)
        this.menu = menu
        saveItem = menu.findItem(R.id.signature_save)
        saveItem!!.isEnabled = false
        saveItem!!.icon!!.alpha = 130
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.signature_save) {
            saveFreeHand()
            val data = Intent()
            val text = "Result OK"
            data.action = text
            setResult(Activity.RESULT_OK, data)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun onRadioButtonClicked(view: View) {
        val checked: Boolean = (view as RadioButton).isChecked
        when (view.id) {
            R.id.radioBlack -> if (checked) {
                signatureView!!.setStrokeColor(
                    ContextCompat.getColor(
                        this@FreeHandActivity,
                        R.color.inkblack
                    )
                )
            }

            R.id.radioRed -> if (checked) signatureView!!.setStrokeColor(
                ContextCompat.getColor(
                    this@FreeHandActivity,
                    R.color.inkred
                )
            )

            R.id.radioBlue -> if (checked) signatureView!!.setStrokeColor(
                ContextCompat.getColor(
                    this@FreeHandActivity,
                    R.color.inkblue
                )
            )

            R.id.radiogreen -> if (checked) signatureView!!.setStrokeColor(
                ContextCompat.getColor(
                    this@FreeHandActivity,
                    R.color.inkgreen
                )
            )
        }
    }

    private fun clearSignature() {
        signatureView!!.clear()
        signatureView!!.setEditable(true)
    }

    fun enableClear(z: Boolean) {
        val button: ImageButton = findViewById<ImageButton>(R.id.action_clear)
        button.isEnabled = z
        if (z) {
            button.alpha = 1.0f
        } else {
            button.alpha = 0.5f
        }
    }

    fun enableSave(z: Boolean) {
        if (z) {
            saveItem!!.icon!!.alpha = 255
        } else {
            saveItem!!.icon!!.alpha = 130
        }
        saveItem!!.isEnabled = z
    }

    private fun saveFreeHand() {
        val localSignatureView: SignatureView =
            findViewById(R.id.inkSignatureOverlayView)
        val localArrayList: ArrayList<*>? = localSignatureView.mInkList
        if (localArrayList != null && localArrayList.size > 0) {
            isFreeHandCreated = true
        }
        SignatureUtils.saveSignature(applicationContext, localSignatureView)
    }
}