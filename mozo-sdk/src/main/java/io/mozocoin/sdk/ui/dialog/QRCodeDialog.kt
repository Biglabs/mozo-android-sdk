package io.mozocoin.sdk.ui.dialog

import android.content.Context
import android.os.Bundle
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import kotlinx.android.synthetic.main.dialog_qr_code.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class QRCodeDialog(context: Context, val value: String) : BaseDialog(context) {

    private var generateQRJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (value.isEmpty()) dismiss()

        window?.setBackgroundDrawableResource(R.drawable.mozo_bg_dialog)
        setContentView(R.layout.dialog_qr_code)

        generateQRJob = GlobalScope.launch {
            val size = context.resources.getDimensionPixelSize(R.dimen.mozo_qr_large_size)
            val qrImage = Support.generateQRCode(value, size)
            launch(Dispatchers.Main) {
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