package com.example.esign.pki

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PdfReader
import java.io.InputStream
import java.security.Security
import java.security.cert.X509Certificate
import org.bouncycastle.jce.provider.BouncyCastleProvider

object VerificationAuthorityUtil {

    fun verifySignature(
        contentResolver: ContentResolver,
        signedFileUri: Uri,
        onVerified: (Boolean, X509Certificate?) -> Unit
    ) {
        try {
            /** Thêm libs vào danh sách các nhà cung cấp bảo mật. BouncyCastleProvider là module
             *  cung cấp thuật toán mã hóa, quản lý khóa, xác thực, và các dịch vụ khác
             */
            Security.addProvider(BouncyCastleProvider())

            /**
             * Lấy Uri của tệp PDF đã ký từ FileProvider
             */
            val inputStream: InputStream? = contentResolver.openInputStream(signedFileUri)
            val reader = PdfReader(inputStream)

            val acroFields: AcroFields = reader.acroFields
            Security.addProvider(BouncyCastleProvider())

            /** kiểm tra tính hợp lệ của chữ ký */
            val signatureName = acroFields.signatureNames[0]

            Log.i("quangdo", "verifySignature: ")
            val pkcs7 = acroFields.verifySignature(signatureName)
            val valid = pkcs7.verify()
            val cer: X509Certificate = pkcs7.signingCertificate

            Log.w(
                "quangdo",
                "verifySignature: $valid \n certificate information = $cer"
            )

            onVerified.invoke(valid, cer)
            reader.close()
        } catch (e: Exception) {
            onVerified.invoke(false, null)
            e.printStackTrace()
        }
    }
}