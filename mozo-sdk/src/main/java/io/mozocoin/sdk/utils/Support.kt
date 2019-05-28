package io.mozocoin.sdk.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.ExchangeRateData
import io.mozocoin.sdk.common.model.ExchangeRateInfo
import io.mozocoin.sdk.ui.ScannerQRActivity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

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
                hashMapOf(EncodeHintType.MARGIN to 0)
        )

        @JvmStatic
        fun toAmountNonDecimal(amount: BigDecimal, decimal: Int): BigDecimal = toAmountNonDecimal(amount, decimal.toDouble())

        @JvmStatic
        fun toAmountNonDecimal(amount: BigDecimal, decimal: Double): BigDecimal = amount.divide(Math.pow(10.0, decimal).toBigDecimal())

        @JvmStatic
        fun parsePaymentRequest(content: String): Array<String> {
            // mozox:0xbc049e92d22a6e544d1032b243310ac167ac2f9a?amount=1028
            if (content.startsWith("mozox:")) {
                val r = content.trimStart(*"mozox:".toCharArray()).split("?amount=").toTypedArray()
                r.lastOrNull()?.let {
                    r[r.lastIndex] = BigDecimal(it).setScale(MozoWallet.getInstance().getDecimal(), RoundingMode.HALF_EVEN).toString()
                }
                return r
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
    }
}