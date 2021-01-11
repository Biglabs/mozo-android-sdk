package io.mozocoin.sdk.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.ExchangeRateData
import io.mozocoin.sdk.common.model.ExchangeRateInfo
import io.mozocoin.sdk.ui.ScannerQRActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class Support {
    companion object {

        @JvmStatic
        fun scanQRCode(activity: Activity) {
            IntentIntegrator(activity)
                    .setCaptureActivity(ScannerQRActivity::class.java)
                    .setBeepEnabled(true)
                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    .setPrompt("")
                    .initiateScan()
        }

        @JvmStatic
        fun scanQRCode(fragment: Fragment) {
            IntentIntegrator.forSupportFragment(fragment)
                    .setCaptureActivity(ScannerQRActivity::class.java)
                    .setBeepEnabled(true)
                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    .setPrompt("")
                    .initiateScan()
        }

        @JvmStatic
        fun generateQRCode(str: String, size: Int): Bitmap = BarcodeEncoder().encodeBitmap(
                str,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hashMapOf(
                        EncodeHintType.CHARACTER_SET to "UTF-8",
                        EncodeHintType.MARGIN to 0
                )
        )

        @JvmStatic
        fun toAmountNonDecimal(amount: BigDecimal, decimal: Int): BigDecimal = toAmountNonDecimal(amount, decimal.toDouble())

        @JvmStatic
        fun toAmountNonDecimal(amount: BigDecimal, decimal: Double): BigDecimal = amount.divide(10.0.pow(decimal).toBigDecimal())

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
                    .joinToString(separator = "\n") { "${it.className}.${it.methodName}" }.logPublic("signOut")
        }

        @JvmStatic
        fun writeLog(content: String?) = GlobalScope.launch(Dispatchers.IO) {
            content ?: return@launch

            if (ActivityCompat.checkSelfPermission(MozoSDK.getInstance().context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                val today = SimpleDateFormat("yyMMMd", Locale.US).format(Date())
                val file = File("sdcard/Mozo/${today}-${MozoSDK.getInstance().context.packageName}.log")
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

        internal fun domainAPI() = when (MozoSDK.serviceEnvironment) {
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

        internal fun domainImage() = when (MozoSDK.serviceEnvironment) {
            MozoSDK.ENVIRONMENT_DEVELOP -> Constant.DOMAIN_IMAGE_DEV
            MozoSDK.ENVIRONMENT_STAGING -> Constant.DOMAIN_IMAGE_STAGING
            else -> Constant.DOMAIN_IMAGE_PRODUCTION
        }

        internal fun getDefaultCurrency() = ExchangeRateData(
                ExchangeRateInfo(Constant.DEFAULT_CURRENCY, Constant.DEFAULT_CURRENCY_SYMBOL, SharedPrefsUtils.getDefaultCurrencyRate()),
                ExchangeRateInfo(Constant.DEFAULT_CURRENCY, Constant.DEFAULT_CURRENCY_SYMBOL, BigDecimal.ZERO)
        )

        internal fun formatSpendableText(view: TextView?, balanceDisplay: String, isOnchain: Boolean = false) {
            view ?: return
            view.text = SpannableString(view.context.getString(R.string.mozo_transfer_spendable, balanceDisplay) + (if (isOnchain) " Onchain" else "")).apply {
                set(indexOfFirst { it.isDigit() }..length, ForegroundColorSpan(view.context.color(R.color.mozo_color_primary)))
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
    }
}