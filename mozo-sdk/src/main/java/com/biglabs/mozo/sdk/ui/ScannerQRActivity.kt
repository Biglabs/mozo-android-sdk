package com.biglabs.mozo.sdk.ui

import android.os.Bundle
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.click
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.android.synthetic.main.view_qr_code_scanner.*

class ScannerQRActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.view_qr_code_scanner)
        super.onCreate(savedInstanceState)

        button_back.click {
            onBackPressed()
        }
    }

    override fun initializeContent(): DecoratedBarcodeView {
        return findViewById(R.id.barcode_scanner_view)
    }
}