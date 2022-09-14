package io.mozocoin.sdk.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.ActivityScannerBinding
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.visible
import java.util.concurrent.Executors

class ScannerActivity : LocalizationBaseActivity() {
    private lateinit var binding: ActivityScannerBinding
    private var isBindCamera = false
    private var cameraInfoArr = listOf<CameraInfo>()
    private var cameraIndex = 0
    private var cameraProvider: ProcessCameraProvider? = null
    private var lastCamera: CameraInfo? = null
    private var lastCameraControl: CameraControl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.click {
            finish()
        }
        binding.scannerPermissionProblem.text = getString(
            R.string.error_camera_permission, getString(R.string.error_camera_action)
        )
        binding.scannerPermissionAction.click {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${this@ScannerActivity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
            }
        }
        binding.actionFlipCamera.click {
            if (cameraIndex >= cameraInfoArr.size - 1) {
                cameraIndex = 0
            } else {
                cameraIndex++
            }

            lastCamera = cameraInfoArr.getOrNull(cameraIndex) ?: cameraInfoArr.firstOrNull()
            this.startCamera()
        }
        binding.actionToggleFlash.click {
            val current = it.isSelected
            it.isSelected = !current
            lastCameraControl?.enableTorch(!current)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBindCamera) return

        when (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                binding.scannerMask.visible()
                binding.scannerPermissionProblem.gone()
                binding.scannerPermissionAction.gone()
                bindCameraUseCases()
            }
            PackageManager.PERMISSION_DENIED -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    binding.scannerMask.gone()
                    binding.scannerPermissionProblem.visible()
                    binding.scannerPermissionAction.visible()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        lastCamera = null
        lastCameraControl = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // user granted permissions - we can set up our scanner
            bindCameraUseCases()
        } else {
            forceClose()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCases() {
        isBindCamera = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraInfoArr = cameraProvider!!.availableCameraInfos
            when {
                cameraInfoArr.size > 1 -> {
                    binding.actionFlipCamera.visible()
                }
                cameraInfoArr.isEmpty() -> {
                    forceClose()
                    return@addListener
                }
                else -> {
                    binding.actionFlipCamera.gone()
                }
            }
            lastCamera = cameraInfoArr.first()
            this.startCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera() {
        cameraProvider ?: return
        lastCamera ?: return

        binding.actionToggleFlash.apply {
            isVisible = lastCamera?.hasFlashUnit() == true
            isSelected = (lastCamera?.torchState ?: 0) == TorchState.ON
        }

        // setting up the preview use case
        val previewUseCase = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.cameraView.surfaceProvider)
            }

        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417
        ).build()

        // getClient() creates a new instance of the MLKit barcode scanner with the specified options
        val scanner = BarcodeScanning.getClient(options)

        // setting up the analysis use case
        val analysisUseCase = ImageAnalysis.Builder()
            .build()

        // define the actual functionality of our analysis use case
        analysisUseCase.setAnalyzer(
            Executors.newSingleThreadExecutor()
        ) { imageProxy ->
            processImageProxy(scanner, imageProxy)
        }

        try {
            // configure to use the back camera
            cameraProvider!!.unbindAll()
            lastCameraControl = cameraProvider!!.bindToLifecycle(
                this,
                lastCamera!!.cameraSelector,
                previewUseCase,
                analysisUseCase
            ).cameraControl
        } catch (e: Exception) {
            Support.onScanSuccess("")
            scanner.close()
            finish()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    val barcode = barcodeList.getOrNull(0)
                    val result = barcode?.rawValue ?: return@addOnSuccessListener
                    Support.onScanSuccess(result)
                    barcodeScanner.close()
                    finish()
                }
                .addOnCompleteListener {
                    // When the image is from CameraX analysis use case, must
                    // call image.close() on received images when finished
                    // using them. Otherwise, new images may not be received
                    // or the camera may stall.

                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    private fun forceClose() {
        Toast.makeText(
            this,
            R.string.error_camera,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }
}