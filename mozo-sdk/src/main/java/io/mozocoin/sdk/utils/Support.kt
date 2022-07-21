package io.mozocoin.sdk.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.text.set
import androidx.core.view.isVisible
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.ExchangeRateData
import io.mozocoin.sdk.common.model.ExchangeRateInfo
import io.mozocoin.sdk.ui.ScannerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

object Support {
    private var scannerCallback: ((String) -> Unit)? = null
    internal fun onScanSuccess(result: String) {
        scannerCallback?.invoke(result)
        scannerCallback = null
    }

    @JvmStatic
    fun scanQRCode(context: Context, callback: ((String) -> Unit)) {
        this.scannerCallback = callback
        context.startActivity(Intent(context, ScannerActivity::class.java))
    }

    @JvmStatic
    suspend fun createQRCode(str: String, size: Int, logo: Drawable? = null): Bitmap? =
        withContext(Dispatchers.Unconfined) {
            try {
                val bitmap = BarcodeEncoder().encodeBitmap(
                    str,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    hashMapOf(
                        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                        EncodeHintType.CHARACTER_SET to "UTF-8",
                        EncodeHintType.MARGIN to 0
                    )
                )
                if (logo == null) bitmap
                else {
                    mergeBitmaps(
                        getBitmapFromDrawable((0.4 * size).toInt(), logo),
                        bitmap
                    )
                }
            } catch (ignored: Exception) {
                null
            }
        }

    @Suppress("unused")
    @JvmStatic
    suspend fun createBarcode(data: String, size: Int): Bitmap? =
        withContext(Dispatchers.Unconfined) {
            try {
                BarcodeEncoder().encodeBitmap(
                    data,
                    BarcodeFormat.CODE_128,
                    size,
                    (size * 0.4).toInt(),
                    hashMapOf(
                        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                        EncodeHintType.CHARACTER_SET to "UTF-8",
                        EncodeHintType.MARGIN to 0
                    )
                )
            } catch (ignored: Exception) {
                null
            }
        }

    @JvmStatic
    fun toAmountNonDecimal(amount: BigDecimal, decimal: Int): BigDecimal =
        toAmountNonDecimal(amount, decimal.toDouble())

    @JvmStatic
    fun toAmountNonDecimal(amount: BigDecimal, decimal: Double): BigDecimal =
        amount.divide(10.0.pow(decimal).toBigDecimal())

    @JvmStatic
    fun parsePaymentRequest(content: String): Array<String> {
        /**
         * mozox:0xbc049e92d22a6e544d1032b243310ac167ac2f9a?amount=1028
         * */
        if (content.startsWith("mozox:")) {
            return content.trimStart(*"mozox:".toCharArray()).split("?amount=").toTypedArray()
        }
        return emptyArray()
    }

    @JvmStatic
    fun getDisplayDate(context: Context, time: Long, pattern: String): String = if (time <= 0)
        context.getString(R.string.mozo_view_text_just_now)
    else
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time))

    @JvmStatic
    fun homePage() = "https://${domainHomePage()}"

    @JvmStatic
    fun logStackTrace() {
        Thread.currentThread()
            .stackTrace
            .mapNotNull {
                if (
                    it.className.startsWith("io.mozocoin") ||
                    it.className.startsWith("com.google")
                ) it else null
            }
            .joinToString(separator = "\n") { "${it.className}.${it.methodName}" }
            .logPublic("signOut")
    }

    @JvmStatic
    fun writeLog(content: String?) = MozoSDK.scope.launch(Dispatchers.IO) {
        content ?: return@launch

        if (ActivityCompat.checkSelfPermission(
                MozoSDK.getInstance().context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val today = SimpleDateFormat("yyMMMd", Locale.US).format(Date())
            val file =
                File("sdcard/Mozo/${today}-${MozoSDK.getInstance().context.packageName}.log")
            if (!file.exists()) {
                try {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                val buf = BufferedWriter(FileWriter(file, true))
                buf.append(Date().toString().plus(": $content"))
                buf.newLine()
                buf.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun domainAPI() = when (MozoSDK.serviceEnvironment) {
        MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_API_DEV
        MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_API_STAGING
        else -> Constant.DOMAIN_API_PRODUCTION
    }

    internal fun domainAuth() = when (MozoSDK.serviceEnvironment) {
        MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_AUTH_DEV
        MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_AUTH_STAGING
        else -> Constant.DOMAIN_AUTH_PRODUCTION
    }

    internal fun domainSocket() = when (MozoSDK.serviceEnvironment) {
        MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_SOCKET_DEV
        MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_SOCKET_STAGING
        else -> Constant.DOMAIN_SOCKET_PRODUCTION
    }

    internal fun domainEhterscan() = when (MozoSDK.serviceEnvironment) {
        MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_ETHER_SCAN_DEV
        MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_ETHER_SCAN_STAGING
        else -> Constant.DOMAIN_ETHER_SCAN_PRODUCTION
    }

    internal fun domainHomePage() = when (MozoSDK.serviceEnvironment) {
        MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_LANDING_PAGE_DEV
        MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_LANDING_PAGE_STAGING
        else -> Constant.DOMAIN_LANDING_PAGE_PRODUCTION
    }

    internal fun userAgent() = "MozoSDK/${BuildConfig.SDK_VERSION} (Android ${Build.VERSION.SDK_INT}) ${MozoSDK.hostName}"

    fun domainImage() = when (MozoSDK.serviceEnvironment) {
        MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_IMAGE_DEV
        MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_IMAGE_STAGING
        else -> Constant.DOMAIN_IMAGE_PRODUCTION
    }

    internal fun getDefaultCurrency() = ExchangeRateData(
        ExchangeRateInfo(
            Constant.DEFAULT_CURRENCY,
            Constant.DEFAULT_CURRENCY_SYMBOL,
            SharedPrefsUtils.getDefaultCurrencyRate()
        ),
        ExchangeRateInfo(
            Constant.DEFAULT_CURRENCY,
            Constant.DEFAULT_CURRENCY_SYMBOL,
            BigDecimal.ZERO
        )
    )

    internal fun formatSpendableText(
        view: TextView?,
        balanceDisplay: String,
        isOnchain: Boolean = false
    ) {
        view ?: return
        view.text = SpannableString(
            view.context.getString(
                R.string.mozo_transfer_spendable,
                balanceDisplay
            ) + (if (isOnchain) " Onchain" else "")
        ).apply {
            set(
                indexOfFirst { it.isDigit() }..length,
                ForegroundColorSpan(view.context.color(R.color.mozo_color_primary))
            )
        }
        view.isVisible = true
    }

    internal fun isInternalApps(context: Context): Boolean {
        val packageName = context.applicationContext.packageName
        return when {
            packageName.startsWith("io.mozocoin.tools.operation") -> true
            else -> false
        }
    }

    private fun getBitmapFromDrawable(size: Int, logo: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        logo.setBounds(0, 0, canvas.width, canvas.height)
        logo.draw(canvas)

        return bitmap
    }

    private fun mergeBitmaps(overlay: Bitmap, bitmap: Bitmap): Bitmap {
        val canvas = Canvas(bitmap)
        val canvasWidth = canvas.width
        val canvasHeight = canvas.height

        canvas.drawBitmap(bitmap, Matrix(), null)

        val centreX = (canvasWidth - overlay.width) / 2f
        val centreY = (canvasHeight - overlay.height) / 2f

        canvas.drawBitmap(overlay, centreX, centreY, null)

        return bitmap
    }

    fun fileSize(dir: File): Long {
        var dirSize: Long = 0
        if (dir.isDirectory) {
            for (f in Objects.requireNonNull(dir.listFiles())) {
                dirSize += if (f.isFile) {
                    f.length()
                } else fileSize(f)
            }
        } else if (dir.isFile) {
            dirSize += dir.length()
        }
        return dirSize
    }

    fun cleanDir(dir: File) {
        if (!dir.exists()) return
        if (dir.isDirectory) {
            for (f in Objects.requireNonNull(dir.listFiles())) {
                if (f.isDirectory) cleanDir(f) else f.delete()
            }
        } else {
            dir.delete()
        }
    }
}