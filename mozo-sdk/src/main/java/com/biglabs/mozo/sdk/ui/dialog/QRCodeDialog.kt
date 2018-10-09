package com.biglabs.mozo.sdk.ui.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.synthetic.main.dialog_qr_code.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class QRCodeDialog : DialogFragment() {

    private var rawValue: String? = null

    private var generateQRJob: Job? = null
    private var dimAmount = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            rawValue = getString(KEY_RAW_VALUE)
            if (rawValue.isNullOrEmpty()) dismiss()
        }

        val value = TypedValue()
        resources.getValue(R.dimen.mozo_background_dim_amount, value, true)
        dimAmount = value.float
    }

    override fun onStop() {
        super.onStop()
        generateQRJob?.cancel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dialog_qr_code, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog.window.setBackgroundDrawableResource(R.drawable.mozo_bg_dialog)
        dialog.window.setDimAmount(dimAmount)
        rawValue?.let {
            generateQRJob = launch {
                val size = resources.getDimensionPixelSize(R.dimen.mozo_qr_large_size)
                val qrImage = Support.generateQRCode(it, size)
                launch(UI) {
                    image_qr_code?.setImageBitmap(qrImage)
                }
            }
        }

        button_close.click { dismiss() }
    }

    companion object {
        private const val KEY_RAW_VALUE = "KEY_RAW_VALUE"

        fun show(rawValue: String, fragmentManager: FragmentManager) {
            val bundle = Bundle()
            bundle.putString(KEY_RAW_VALUE, rawValue)

            QRCodeDialog().apply {
                arguments = bundle
                show(fragmentManager, QRCodeDialog::class.java.simpleName)
            }
        }
    }
}