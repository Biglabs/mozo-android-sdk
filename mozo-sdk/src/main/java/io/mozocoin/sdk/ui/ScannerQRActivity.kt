package io.mozocoin.sdk.ui

import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import io.mozocoin.sdk.databinding.ViewQrCodeScannerBinding
import io.mozocoin.sdk.utils.click

class ScannerQRActivity : CaptureActivity() {

    private lateinit var binding: ViewQrCodeScannerBinding

    override fun initializeContent(): DecoratedBarcodeView {
        binding = ViewQrCodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.click {
            onBackPressed()
        }

        return binding.barcodeScannerView
    }
}