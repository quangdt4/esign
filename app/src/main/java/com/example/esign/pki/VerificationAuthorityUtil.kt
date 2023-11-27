package com.example.esign.pki

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PdfReader
import java.io.File
import java.io.InputStream
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

object VerificationAuthorityUtil {

    fun verifySignature(
        applicationContext: Context, signedPdfFilePath: File, contentResolver: ContentResolver
    ) {
        try {
            /** Thêm libs vào danh sách các nhà cung cấp bảo mật. BouncyCastleProvider là module
             *  cung cấp thuật toán mã hóa, quản lý khóa, xác thực, và các dịch vụ khác
             */
            Security.addProvider(BouncyCastleProvider())
            /**
             * Lấy Uri của tệp PDF đã ký từ FileProvider
             */
            val document = FileProvider.getUriForFile(
                applicationContext, applicationContext.packageName + ".provider", signedPdfFilePath
            )
            val inputStream: InputStream? = contentResolver.openInputStream(document!!)
            val reader = PdfReader(inputStream)

            val acroFields: AcroFields = reader.acroFields
            Security.addProvider(BouncyCastleProvider())

            /** kiểm tra tính hợp lệ */
            for (signatureName in acroFields.signatureNames) {
                val pkcs7 = acroFields.verifySignature(signatureName)
                val valid = pkcs7.verify()
                Log.w(
                    "quangdo",
                    "verifySignature: $valid " + "\n certificate information = ${pkcs7.certificates.first()}"
                )
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}