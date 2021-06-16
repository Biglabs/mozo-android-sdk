package io.mozocoin.sdk.ui.dialog

import android.content.Context
import android.os.Bundle
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.DialogQrCodeBinding
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.dimen
import kotlinx.coroutines.*

internal class QRCodeDialog(context: Context, val value: String) : BaseDialog(context) {

    private lateinit var binding: DialogQrCodeBinding
    private var generateQRJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (value.isEmpty()) dismiss()

        window?.setBackgroundDrawableResource(R.drawable.mozo_bg_dialog)

        binding = DialogQrCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        generateQRJob = MozoSDK.scope.launch {
            val size = context.dimen(R.dimen.mozo_qr_large_size)
            val qrImage = Support.generateQRCode(value, size)
            withContext(Dispatchers.Main) {
                binding.imageQrCode.setImageBitmap(qrImage)
            }
        }

        binding.buttonClose.click { dismiss() }
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