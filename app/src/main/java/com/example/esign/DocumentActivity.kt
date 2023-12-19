package com.example.esign

import SignatureUtils
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.esign.pdf.PDSPDFDocument
import com.example.esign.pds.PDSElementViewer
import com.example.esign.pds.adapter.PDSPageAdapter
import com.example.esign.pds.model.PDSElement
import com.example.esign.pds.page.PDSPageViewer
import com.example.esign.pds.page.PDSViewPager
import com.example.esign.pki.SaveAsPDFWithCoroutine
import com.example.esign.pki.VerificationAuthorityUtil.verifySignature
import com.example.esign.signature.SignatureActivity
import com.example.esign.utils.CommonUtils.showToast
import com.github.clans.fab.FloatingActionButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider

class DocumentActivity : AppCompatActivity() {

    private var pdfData: Uri? = null
    private var digitalID: Uri? = null
    var newPdfData: Uri? = null

    private var mViewPager: PDSViewPager? = null
    private var imageAdapter: PDSPageAdapter? = null
    private var savingProgress: ProgressBar? = null
    private var menu: Menu? = null
    private var passwordAlertDialog: AlertDialog? = null
    private var signatureOptionDialog: AlertDialog? = null

    private var mDocument: PDSPDFDocument? = null

    private val uiElementsHandler = UIElementsHandler(this)

    private var isSigned = false
    private var mVisibleWindowHt = 0

    var isFirstTap = true
    var keyStore: KeyStore? = null
    var alias: String? = null
    var mDigitalIDPassword: String? = null

    companion object {
        private const val ACTION = "ActivityAction"
        private const val FILE = "FileSearch"
        private const val PDF_OPEN = "PDFOpen"
        private const val PKCS_12 = "pkcs12"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_digital_signature)
        setUp()
    }

    private fun setUp() {
        mViewPager = findViewById(R.id.viewpager)
        savingProgress = findViewById(R.id.savingProgress)

        val fabVerify: ExtendedFloatingActionButton = findViewById(R.id.fabVerify)
        fabVerify.setOnClickListener {
            verify()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val message: String? = intent.getStringExtra(ACTION)

        if (message == FILE) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            val mimetypes = arrayOf("application/pdf")
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            startImportDocumentForResult.launch(intent)
        } else if (message == PDF_OPEN) {
            val imageUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra("PDFOpen")
            if (imageUris != null) {
                for (i in imageUris.indices) {
                    val imageUri = imageUris[i]
                    openPDF(imageUri)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        this.menu = menu
        val saveItem = this.menu!!.findItem(R.id.action_save)
        saveItem.icon!!.alpha = 130
        val signItem = this.menu!!.findItem(R.id.action_sign)
        signItem.icon!!.alpha = 255
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign -> showSignatureOptionsDialog()
            R.id.action_save -> savePDFDocument()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun verify() {
        if (pdfData != null) {
            verifySignature(contentResolver, pdfData!!, onVerified = { verify, cert ->
                if (verify && cert != null) {
                    showVerifyDialog(cert)
                } else {
                    showToast("Verify fail!", this)
                }
            })
        } else if (newPdfData != null) {
            verifySignature(contentResolver, newPdfData!!, onVerified = { verify, cert ->
                if (verify && cert != null) {
                    showVerifyDialog(cert)
                } else {
                    showToast("Verify fail!", this)
                }
            })
        }
    }

    private fun showVerifyDialog(cer: X509Certificate) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Verify signature")
        alertDialog.setCancelable(true)
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL, "Close"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.setMessage(
            "Serial Number: ${cer.serialNumber}\n" + "IssuerDN: ${cer.issuerDN}]\n" + "Signature: ${cer.signature}"
        )

        alertDialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isSigned) {
            showSaveChangesDialog()
        } else {
            finish()
        }
    }

    private fun openPDF(pdfData: Uri?) {
        try {
            newPdfData = pdfData
            val document = PDSPDFDocument(this, pdfData)
            document.open()
            mDocument = document
            imageAdapter = PDSPageAdapter(supportFragmentManager, document)
            updatePageNumber(1)
            mViewPager!!.adapter = imageAdapter
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(
                "Cannot open PDF, either PDF is corrupted or password protected",
                this@DocumentActivity
            )
            finish()
        }
    }

    private fun computeVisibleWindowHtForNonFullScreenMode(): Int {
        return findViewById<View>(R.id.docviewer).height
    }

    val visibleWindowHeight: Int
        get() {
            if (mVisibleWindowHt == 0) {
                mVisibleWindowHt = computeVisibleWindowHtForNonFullScreenMode()
            }
            return mVisibleWindowHt
        }
    val document: PDSPDFDocument?
        get() = mDocument

    private fun getPassword() {
        val dialogView = layoutInflater.inflate(R.layout.passworddialog, null)
        passwordAlertDialog = AlertDialog.Builder(this).setView(dialogView).apply {
                val password: EditText = dialogView.findViewById(R.id.passwordText)
                val submit = dialogView.findViewById<Button>(R.id.passwordSubmit)
                submit.setOnClickListener {
                    Log.i("quangdo", "...on Submit getPassword")
                    handlePasswordSubmission(password)
                }
            }.create()
        passwordAlertDialog?.show()
    }

    /**
     * Xử ly mật khẩu để xác thực chứng thư số (certificate)
     */
    private fun handlePasswordSubmission(password: EditText) {
        if (password.length() == 0) {
            showToast("Password can't be blank", this)
        } else {
            mDigitalIDPassword = password.text.toString()
            Log.i("quangdo", "password = ******* ")
            val provider = BouncyCastleProvider()
            Security.addProvider(provider)
            try {
                val inputStream: InputStream = contentResolver.openInputStream(digitalID!!)!!

                /**
                 * PKCS_12: định dạng file, sd lưu trữ  các key, chứng chỉ số và thông tin cá nhân
                 */
                keyStore = KeyStore.getInstance(PKCS_12, provider.name)
                if (keyStore != null) {
                    keyStore!!.load(inputStream, mDigitalIDPassword!!.toCharArray())
                    alias = keyStore!!.aliases().nextElement()
                    passwordAlertDialog!!.dismiss()
                }
                showToast("Digital certificate is added with Signature", this)
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    private fun handleException(exception: Exception) {
        when {
            exception.message!!.contains("wrong password") -> {
                showToast("Password is incorrect or certificate is corrupted", this)
            }

            else -> {
                showToast("Something went wrong while adding Digital certificate", this)
                passwordAlertDialog!!.dismiss()
            }
        }
        exception.printStackTrace()
    }

    fun invokeMenuButton(disableButtonFlag: Boolean) {
        val saveItem = menu!!.findItem(R.id.action_save)
        saveItem.isEnabled = disableButtonFlag
        val signPDF = menu!!.findItem(R.id.action_sign)
        //signPDF.setEnabled(!disableButtonFlag);
        isSigned = disableButtonFlag
        if (disableButtonFlag) {
            //signPDF.getIcon().setAlpha(130);
            saveItem.icon!!.alpha = 255
        } else {
            //signPDF.getIcon().setAlpha(255);
            saveItem.icon!!.alpha = 130
        }
    }

    fun updatePageNumber(i: Int) {
        val textView = findViewById<View>(R.id.pageNumberTxt) as TextView
        findViewById<View>(R.id.pageNumberOverlay).visibility = View.VISIBLE
        val stringBuilder = StringBuilder()
        stringBuilder.append(i)
        stringBuilder.append("/")
        stringBuilder.append(mDocument!!.numPages)
        textView.text = stringBuilder.toString()

        uiElementsHandler.removeMessages(1)
        val message = Message()
        message.what = 1
        uiElementsHandler.sendMessageDelayed(message, 1000.toLong())
    }

    fun fadePageNumberOverlay() {
        val loadAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val findViewById: View = findViewById(R.id.pageNumberOverlay)
        if (findViewById.visibility == View.VISIBLE) {
            findViewById.startAnimation(loadAnimation)
            findViewById.visibility = View.INVISIBLE
        }
    }

    fun runPostExecution() {
        savingProgress!!.visibility = View.INVISIBLE
        val i = Intent()
        setResult(Activity.RESULT_OK, i)
        finish()
    }

    /**
     * --------- SAVE DATA ---------
     */
    private fun savePDFDocument() {
        val dialog = Dialog(this@DocumentActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val alertView: View = layoutInflater.inflate(R.layout.file_alert_dialog, null)
        val edittext: EditText = alertView.findViewById(R.id.editText2)
        dialog.setContentView(alertView)
        dialog.setCancelable(true)
        val lp: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
            copyFrom(dialog.window!!.attributes)
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        dialog.show()
        dialog.window!!.attributes = lp
        dialog.findViewById<View>(R.id.bt_close).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.bt_save).setOnClickListener {
            val fileName: String = edittext.text.toString()
            if (fileName.isBlank()) {
                Toast.makeText(
                    this@DocumentActivity, "File name should not be empty", Toast.LENGTH_LONG
                ).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        val task = SaveAsPDFWithCoroutine(this@DocumentActivity, "$fileName.pdf")
                        task.saveAsPDF()
                        task.onPostExecute()
                    }
                    dialog.dismiss()
                }
            }
        }
    }

    /**
     * Handle dialog
     */
    private fun showSaveChangesDialog() {
        AlertDialog.Builder(this).setTitle("Save Document")
            .setMessage("Want to save your changes to the PDF document?")
            .setPositiveButton("Save") { _, _ ->
                savePDFDocument()
            }.setNegativeButton("Exit") { _, _ ->
                finish()
            }.show()
    }

    private fun showSignatureOptionsDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.optiondialog, null)
        dialogBuilder.setView(dialogView)

        val signature = dialogView.findViewById<Button>(R.id.fromCollection)
        signature.setOnClickListener {
            startSignatureForResult.launch(
                Intent(
                    applicationContext, SignatureActivity::class.java
                )
            )
            signatureOptionDialog?.dismiss()
        }

        val image = dialogView.findViewById<Button>(R.id.fromImage)
        image.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            startImageForResult.launch(intent)
            signatureOptionDialog?.dismiss()
        }

        signatureOptionDialog = dialogBuilder.create()
        signatureOptionDialog?.show()
    }

    /**
     * Add Element
     */
    private fun addElementFile(
        fASElementType: PDSElement.PDSElementType, file: File?, f: Float, f2: Float
    ) {
        val focusedChild: View = mViewPager!!.focusedChild
        val fASPageViewer: PDSPageViewer =
            (focusedChild as ViewGroup).getChildAt(0) as PDSPageViewer
        val visibleRect: RectF = fASPageViewer.visibleRect
        val width: Float = visibleRect.left + visibleRect.width() / 2.0f - f / 2.0f
        val height: Float = visibleRect.top + visibleRect.height() / 2.0f - f2 / 2.0f
        val fASElementType2: PDSElement.PDSElementType = fASElementType
        val element: PDSElement =
            fASPageViewer.createElement(fASElementType2, file, width, height, f, f2)
        if (!isSigned) {
            val dialog: AlertDialog
            val builder = AlertDialog.Builder(this@DocumentActivity)
            builder.setMessage("Do you want to add digital certificate with this Signature?")
                .setPositiveButton(
                    "Yes"
                ) { _, _ ->
                    Log.i("quangdo", "addElementFile: need file .p12, type x-pkcs12 (CERTIFICATE)")

                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.type = "application/keychain_access"
                    val mimetypes = arrayOf("application/x-pkcs12")
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                    startDigitalIdForResult.launch(intent)
                }.setNegativeButton(
                    "No"
                ) { dialog, _ -> dialog.dismiss() }
            dialog = builder.create()
            dialog.show()
        }
        invokeMenuButton(true)
    }

    private fun addElementBitmap(
        fASElementType: PDSElement.PDSElementType, bitmap: Bitmap?, f: Float, f2: Float
    ) {
        val focusedChild: View = mViewPager!!.focusedChild
        if (bitmap != null) {
            val fASPageViewer: PDSPageViewer =
                (focusedChild as ViewGroup).getChildAt(0) as PDSPageViewer
            val visibleRect: RectF = fASPageViewer.visibleRect
            val width: Float = visibleRect.left + visibleRect.width() / 2.0f - f / 2.0f
            val height: Float = visibleRect.top + visibleRect.height() / 2.0f - f2 / 2.0f
            val lastFocusedElementViewer: PDSElementViewer =
                fASPageViewer.lastFocusedElementViewer!!
            val fASElementType2: PDSElement.PDSElementType = fASElementType
            val element: PDSElement =
                fASPageViewer.createElement(fASElementType2, bitmap, width, height, f, f2)
            if (!isSigned) {
                val dialog: AlertDialog
                val builder = AlertDialog.Builder(this@DocumentActivity)
                builder.setMessage("Do you want to add digital certificate with this Signature?")
                    .setPositiveButton(
                        "Yes"
                    ) { _, _ ->
                        Log.i("quangdo", "addElementBitmap: need file .p12, type x-pkcs12")

                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        intent.type = "application/keychain_access"
                        val mimetypes = arrayOf("application/x-pkcs12")
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                        startDigitalIdForResult.launch(intent)
                    }.setNegativeButton(
                        "No"
                    ) { dialog, _ -> dialog.dismiss() }
                dialog = builder.create()
                dialog.show()
            }
            invokeMenuButton(true)
        }
    }

    /**
     * on Result
     */
    private val startImportDocumentForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result != null) {
                pdfData = result.data?.data
                openPDF(pdfData)
            }
        } else {
            finish()
        }
    }

    private val startSignatureForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returnValue: String = result!!.data?.getStringExtra("FileName") ?: ""
            val fi = File(returnValue)
            this.addElementFile(
                PDSElement.PDSElementType.PDSElementTypeSignature,
                fi,
                SignatureUtils.getSignatureWidth(
                    resources.getDimension(R.dimen.sign_field_default_height).toInt(),
                    fi,
                    applicationContext
                ).toFloat(),
                resources.getDimension(R.dimen.sign_field_default_height)
            )
        }
    }

    private val startImageForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result != null) {
                val imageData: Uri? = result.data?.data
                val bitmap: Bitmap?
                try {
                    val input: InputStream = this.contentResolver.openInputStream(imageData!!)!!
                    bitmap = BitmapFactory.decodeStream(input)
                    input.close()
                    if (bitmap != null) this.addElementBitmap(
                        PDSElement.PDSElementType.PDSElementTypeImage,
                        bitmap,
                        resources.getDimension(R.dimen.sign_field_default_height),
                        resources.getDimension(R.dimen.sign_field_default_height)
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val startDigitalIdForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result != null) {
                digitalID = result.data?.data
                Log.d("quangdo", "Add certification success \n")
                getPassword()
            }
        } else {
            showToast("Digital certificate is not added with Signature", this@DocumentActivity)
        }
    }
}