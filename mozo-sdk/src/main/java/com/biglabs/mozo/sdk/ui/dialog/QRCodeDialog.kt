package com.biglabs.mozo.sdk.ui.dialog

import android.content.Context
import android.os.Bundle
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.synthetic.main.dialog_qr_code.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

internal class QRCodeDialog(context: Context, val value: String) : BaseDialog(context) {

    private var generateQRJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (value.isEmpty()) dismiss()

        window?.setBackgroundDrawableResource(R.drawable.mozo_bg_dialog)
        setContentView(R.layout.dialog_qr_code)

        generateQRJob = launch {
            val size = context.resources.getDimensionPixelSize(R.dimen.mozo_qr_large_size)
            val qrImage = Support.generateQRCode(value, size)
            launch(UI) {
                image_qr_code?.setImageBitmap(qrImage)
            }
        }

        button_close.click { dismiss() }
    }

    override fun onStop() {
        super.onStop()
        generateQRJob?.cancel()
    }

    companion object {
        fun show(context: Context, rawValue: String) {
            QRCodeDialog(context, rawValue).show()
        }
    }
}