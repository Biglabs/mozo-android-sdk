package com.biglabs.mozo.sdk.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.math.BigDecimal

class Support {
    companion object {

        fun generateQRCode(str: String, size: Int): Bitmap = BarcodeEncoder().encodeBitmap(str, BarcodeFormat.QR_CODE, size, size)

        fun calculateAmountDecimal(amount: BigDecimal, decimal: Int): BigDecimal = calculateAmountDecimal(amount, decimal.toDouble())
        fun calculateAmountDecimal(amount: BigDecimal, decimal: Double): BigDecimal = amount.divide(Math.pow(10.0, decimal).toBigDecimal())
    }
}