package com.biglabs.mozo.sdk.utils

import android.app.Activity
import android.graphics.Bitmap
import com.biglabs.mozo.sdk.ui.ScannerQRActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.math.BigDecimal

class Support {
    companion object {

        fun scanQRCode(activity: Activity) {
            IntentIntegrator(activity)
                    .setCaptureActivity(ScannerQRActivity::class.java)
                    .setBeepEnabled(true)
                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    .setPrompt("")
                    .initiateScan()
        }

        fun generateQRCode(str: String, size: Int): Bitmap = BarcodeEncoder().encodeBitmap(
                str,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hashMapOf(EncodeHintType.MARGIN to 0)
        )

        fun calculateAmountDecimal(amount: BigDecimal, decimal: Int): BigDecimal = calculateAmountDecimal(amount, decimal.toDouble())
        fun calculateAmountDecimal(amount: BigDecimal, decimal: Double): BigDecimal = amount.divide(Math.pow(10.0, decimal).toBigDecimal())
    }
}