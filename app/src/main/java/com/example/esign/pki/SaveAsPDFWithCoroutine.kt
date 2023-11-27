package com.example.esign.pki

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log
import android.view.View
import com.example.esign.DocumentActivity
import com.example.esign.pdf.PDSPDFPage
import com.example.esign.pds.model.PDSElement
import com.example.esign.utils.CommonUtils.showToast
import com.example.esign.utils.ViewUtils
import com.itextpdf.text.Image
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfSignatureAppearance
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.pdf.security.BouncyCastleDigest
import com.itextpdf.text.pdf.security.DigestAlgorithms
import com.itextpdf.text.pdf.security.ExternalDigest
import com.itextpdf.text.pdf.security.ExternalSignature
import com.itextpdf.text.pdf.security.MakeSignature
import com.itextpdf.text.pdf.security.PrivateKeySignature
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveAsPDFWithCoroutine(private val activity: DocumentActivity, private val fileName: String) {

    private var signer: PdfStamper? = null
    private var mResult: Boolean = false

    suspend fun saveAsPDF() {
        withContext(Dispatchers.IO) {
            try {
                val document = activity.document ?: return@withContext
                val file = createFile()

                FileOutputStream(file).use { os ->
                    val reader = PdfReader(document.stream!!)
                    PdfReader.unethicalreading = true

                    for (i in 0 until document.numPages) {
                        val mediaBox: Rectangle = reader.getPageSize(i + 1)

                        for (j in 0 until document.getPage(i)!!.numElements) {
                            val page: PDSPDFPage = document.getPage(i)!!
                            val element: PDSElement = page.getElement(j)
                            val bounds: RectF = element.rect!!
                            val createBitmap = getBitmapFromElementType(element)
                            val byteArray = bitmapToByteArray(createBitmap)
                            val signImage = Image.getInstance(byteArray)

                            processSignature(
                                activity, os, reader,
                                mediaBox, bounds, signImage, i, j
                            )
                        }
                    }
                    signer?.close()
                    reader.close()
                    os.close()
                    mResult = true
                }
            } catch (e: Exception) {
                handleException(e)
                mResult = true
            }
        }
    }

    /**
     * - 'KeyStore' lưu trữ public và private, chứng chỉ (chứng thư số), và thông tin liên quan đến
     *   bảo mật như các mảng tin cậy (truststores) chứa chứng chỉ CA.
     *
     * - 'alias' (tên định danh) tương ứng với một cặp khóa trong keystore.
     *
     * - 'password' là mật khẩu khi set up chứng thư số
     *
     * - 'chain' là một chuỗi nhiều chứng chỉ. Mỗi chứng chỉ trong chuỗi là chứng chỉ của một thực
     *   thể (entity), và chuỗi này thường được sắp xếp theo thứ tự từ chứng chỉ của thực thể tới
     *   chứng chỉ CA (Certificate Authority) cao nhất.
     *
     */
    private fun processSignature(
        activity: DocumentActivity,
        os: FileOutputStream,
        reader: PdfReader,
        mediaBox: Rectangle,
        bounds: RectF,
        signImage: Image,
        i: Int,
        j: Int
    ) {
        if (activity.alias != null || activity.keyStore != null ||
            activity.mDigitalIDPassword != null
        ) {
            Log.w("quangdo", "======== process Signature ======== \n \n", )

            val keyStore: KeyStore = activity.keyStore!!
            val alias: String = activity.alias!!
            val password: CharArray = activity.mDigitalIDPassword!!.toCharArray()

            val privateKey = keyStore.getKey(alias, password) as PrivateKey
            val publicKey = keyStore.getCertificate(alias).publicKey
            Log.e("quangdo", "PRIVATE KEY <-------\n$privateKey \n")
            Log.d("quangdo", "PUBLIC KEY <------- $publicKey")

            /** lấy chứng chỉ của cặp khóa đc định danh bởi alias */
            val chain = keyStore.getCertificateChain(alias)

            /** tạo và thêm chữ ký số vào tài liệu PDF */
            if (signer == null) signer = PdfStamper.createSignature(reader, os, '\u0000')

            /**
             * Sử dụng private key để ký số tài liệu PDF, sử dụng thuật toán SHA-256 và chuẩn CMS.
             * Chữ ký sau đó được đính vào tài liệu PDF tại vị trí đính.
             */
            val digest: ExternalDigest = BouncyCastleDigest()
            val signature: ExternalSignature =
                PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, null)

            /**
             * Tạo và đính chữ ký số vào tài liệu, bao gồm:
             * - Ảnh chữ ký số
             * - Hàm băm tạo bởi thuật toán SHA-256
             * - Certificate
             */
            MakeSignature.signDetached(
                addSignatureImageToDocument(mediaBox, bounds, signImage, i, j),
                digest,
                signature,
                chain,
                null,
                null,
                null,
                0,
                MakeSignature.CryptoStandard.CADES
            )
        } else {
            signer = PdfStamper(reader, os, '\u0000')
            val contentByte: PdfContentByte = signer!!.getOverContent(i + 1)
            signImage.alignment = Image.ALIGN_UNDEFINED
            signImage.scaleToFit(bounds.width(), bounds.height())
            signImage.setAbsolutePosition(
                bounds.left - (signImage.scaledWidth - bounds.width()) / 2,
                mediaBox.height - (bounds.top + bounds.height())
            )
            contentByte.addImage(signImage)
        }
    }

    private fun addSignatureImageToDocument(
        mediaBox: Rectangle,
        bounds: RectF,
        signImage: Image,
        i: Int,
        j: Int
    ): PdfSignatureAppearance {
        /** Thêm ảnh chữ ký số đính vào tài liệu PDF */
        val appearance: PdfSignatureAppearance = signer!!.signatureAppearance
        val top: Float = mediaBox.height - (bounds.top + bounds.height())
        appearance.setVisibleSignature(
            Rectangle(
                bounds.left,
                top,
                bounds.left + bounds.width(),
                top + bounds.height()
            ),
            i + 1, "sig$j"
        )
        appearance.renderingMode = PdfSignatureAppearance.RenderingMode.GRAPHIC
        appearance.signatureGraphic = signImage
        return appearance
    }

    private fun createFile(): File {
        val root = activity.filesDir
        val myDir = File("$root/DigitalSignature").apply { mkdirs() }
        return File(myDir, fileName).apply { delete() }
    }

    private fun getBitmapFromElementType(element: PDSElement?): Bitmap? {
        return when (element?.type) {
            PDSElement.PDSElementType.PDSElementTypeSignature -> {
                element.mElementViewer?.let { viewer ->
                    val dummy: View = viewer.elementView ?: return null
                    val view: View? = ViewUtils.createSignatureView(
                        activity, element, viewer.pageViewer?.toViewCoordinatesMatrix
                    )
                    Bitmap.createBitmap(dummy.width, dummy.height, Bitmap.Config.ARGB_8888)
                        .also { view?.draw(Canvas(it)) }
                }
            }
            else -> element?.bitmap
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap?): ByteArray? {
        bitmap?.let {
            val byteArrayOutputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            it.recycle()
            return byteArrayOutputStream.toByteArray()
        }
        return null
    }

    private fun handleException(e: Exception) {
        e.printStackTrace()
        val root = activity.filesDir
        val file = File("$root/DigitalSignature", fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    fun onPostExecute() {
        activity.runOnUiThread {
            activity.runPostExecution()
            val msg = if (!mResult) {
                "Something went wrong while signing the PDF document. Please try again."
            } else {
                "PDF document saved successfully"
            }
            showToast(msg, activity.applicationContext)
        }
    }
}
