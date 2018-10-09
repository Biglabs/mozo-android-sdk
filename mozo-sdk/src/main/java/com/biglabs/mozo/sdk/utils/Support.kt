package com.biglabs.mozo.sdk.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class Support {
    companion object {

        fun generateQRCode(str: String, size: Int): Bitmap = BarcodeEncoder().encodeBitmap(str, BarcodeFormat.QR_CODE, size, size)
    }
}