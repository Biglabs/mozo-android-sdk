package io.mozocoin.sdk.utils

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnNextLayout
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.widget.PinEntryEditText
import io.mozocoin.sdk.utils.customtabs.CustomTabsHelper
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

fun FragmentActivity.replace(@IdRes id: Int, fragment: Fragment, backStackName: String? = null) {
    if (isFinishing || isDestroyed) return
    supportFragmentManager.beginTransaction().replace(id, fragment).apply {
        if (backStackName != null) addToBackStack(backStackName)
    }.commit()
}

fun Fragment.replace(@IdRes id: Int, fragment: Fragment, backStackName: String? = null) {
    if (!isAdded || isRemoving) return
    childFragmentManager.beginTransaction().run {
        childFragmentManager.fragments.forEach {
            hide(it)
        }

        if (fragment.isAdded) show(fragment)
        else add(id, fragment)
    }.apply {
        if (backStackName != null) addToBackStack(backStackName)
    }.commit()
}

fun Context?.isLocationPermissionGranted() = this != null && ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED

fun Context?.isStoragePermissionGranted() = this != null && ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
) == PackageManager.PERMISSION_GRANTED

internal fun Context.clipboard(): ClipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

internal fun Context.copyText(text: String?) {
    if (!text.isNullOrEmpty()) {
        clipboard().setPrimaryClip(ClipData.newPlainText("mozo_wallet_text", text))
    }
}

internal fun Context.copyWithToast(text: String?) {
    if (!text.isNullOrEmpty()) {
        clipboard().setPrimaryClip(ClipData.newPlainText("mozo_wallet_text", text))
        Toast.makeText(this, R.string.mozo_dialog_copied_msg, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension method to Get Integer resource for Context.
 */
fun Context.getInteger(@IntegerRes id: Int) = resources.getInteger(id)

fun Context.string(@StringRes id: Int, @StringRes idRef: Int = 0): String {
    return if (idRef != 0) getString(id, string(idRef)) else getString(id)
}

fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Context.dimen(@DimenRes id: Int): Int = resources.getDimensionPixelSize(id)

fun Context.bitmap(@DrawableRes icon: Int) = AppCompatResources.getDrawable(this, icon)?.toBitmap()

fun Context.openTab(url: String) {
    var finalUrl = url
    if (finalUrl.contains(Support.domainHomePage(), ignoreCase = true)) {
        val langParam = "language"

        val uri = Uri.parse(finalUrl)
        if (uri.getQueryParameter(langParam).isNullOrEmpty()) {
            finalUrl = uri.buildUpon().appendQueryParameter(langParam, Locale.getDefault().toLanguageTag()).build().toString()
        }
    }

    if (finalUrl.startsWith(Support.domainHomePage(), ignoreCase = true)) {
        finalUrl = "https://".plus(finalUrl)
    }

    val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(color(R.color.mozo_color_primary))
            .build()

    //to be used if Custom Tabs is not available
    val fallback = object : CustomTabsHelper.CustomTabFallback {
        override fun openUri(context: Context, uri: Uri) {
            if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                return
            }

            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(context.packageManager) != null)
                context.startActivity(intent)
            else "No activity found".logAsError()
        }
    }

    val targetUri = Uri.parse(finalUrl)
    try {
        CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)
        CustomTabsHelper.openCustomTab(this, customTabsIntent, targetUri, fallback)
    } catch (e: Exception) {
        if (e !is ActivityNotFoundException) {
            /**
             * Try to open link by external browser
             */
            fallback.openUri(this, targetUri)
        }
        e.printStackTrace()
    }
}

fun Context.openAppInStore() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=$packageName")
    }

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        openTab("https://play.google.com/store/apps/details?id=$packageName")
    }
}

fun visibility(visible: Boolean, vararg views: View?) {
    if (visible) visible(*views)
    else gone(*views)
}

fun visible(vararg views: View?) {
    views.forEach { it?.visibility = View.VISIBLE }
}

fun gone(vararg views: View?) {
    views.forEach { it?.visibility = View.GONE }
}

fun Resources.dp2Px(value: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics)
}

/**
 * Extension method to show a keyboard for View.
 */
fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    this.requestFocus()
    imm.showSoftInput(this, 0)
}

/**
 * Try to hide the keyboard and returns whether it worked
 * https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
 */
fun View.hideKeyboard(): Boolean {
    try {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    } catch (ignored: RuntimeException) {
    }
    return false
}

/**
 * Set an onclick listener
 */
@Suppress("UNCHECKED_CAST")
fun <T : View> T.click(block: (T) -> Unit) = setOnClickListener {
    isClickable = false
    block(it as T)

    doOnNextLayout {
        isClickable = true
    }

    postDelayed(1200) {
        isClickable = true
    }
}

inline fun <reified T : View> View.find(@IdRes id: Int): T? = findViewById(id) as? T

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

internal fun TextView.copyText() = apply { context.copyText(text.toString()) }
internal fun TextView.copyWithToast() = apply { context.copyWithToast(text.toString()) }
internal fun TextView.highlight(doIt: Boolean) {
    typeface = if (doIt) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    setTextColor(context.color(if (doIt) R.color.mozo_color_primary else R.color.mozo_color_section_text))
}

fun EditText.onTextChanged(block: (s: CharSequence?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            removeTextChangedListener(this)
            block(s)
            addTextChangedListener(this)
        }
    })
}

fun EditText.onAmountInputChanged(textChanged: ((String?) -> Unit)? = null, amountChanged: (BigDecimal) -> Unit) {
    inputType = InputType.TYPE_CLASS_NUMBER
    keyListener = DigitsKeyListener.getInstance("0123456789.,")
    filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, MozoTx.getInstance().mozoDecimal().toInt()))

    onTextChanged {
        textChanged?.invoke(it?.toString())

        if (it.isNullOrEmpty()) {
            amountChanged(BigDecimal.ZERO)
            return@onTextChanged
        }
        if (it.startsWith(DecimalFormatSymbols.getInstance().decimalSeparator)) {
            setText(String.format(Locale.US, "0%s", it))
            setSelection(it.length + 1)
            return@onTextChanged
        }

        var separatorIndex = it.lastIndexOf(DecimalFormatSymbols.getInstance().decimalSeparator) + 1
        if (separatorIndex > 0) {
            separatorIndex = it.length - separatorIndex
        }

        var rawAmount = it.replace(Regex("\\D+"), "")
        if (separatorIndex > 0) {
            rawAmount = StringBuilder(rawAmount).insert(rawAmount.length - separatorIndex, ".").toString()
        }

        val amount = BigDecimal(rawAmount)
        if (
                !it.endsWith(DecimalFormatSymbols.getInstance().decimalSeparator) &&
                !it.endsWith("${DecimalFormatSymbols.getInstance().decimalSeparator}0")
        ) {
            val amountDisplay = NumberFormat.getNumberInstance().format(amount)
            setText(amountDisplay)
            setSelection(amountDisplay.length)
        }

        amountChanged(amount)
    }
}

fun SwipeRefreshLayout.mozoSetup() {
//    val offset = resources.getDimensionPixelSize(R.dimen.mozo_refresh_progress_offset)
//    setProgressViewOffset(true, progressViewStartOffset + offset, progressViewEndOffset + offset / 3)
    setColorSchemeResources(R.color.mozo_color_primary)
}

fun BigDecimal.trailingZeros(scale: Int): BigDecimal {
    return setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
}

fun BigDecimal?.displayString(scale: Int = 6): String {
    return NumberFormat.getNumberInstance().format(this?.trailingZeros(scale).safe())
}

fun BigDecimal.toWei(): BigDecimal {
    return this.multiply(10.0.pow(9.0).toBigDecimal())
}

fun BigDecimal.toGwei(): BigDecimal {
    return this.divide(10.0.pow(9.0).toBigDecimal())
}

fun BigDecimal?.safe(): BigDecimal = this ?: BigDecimal.ZERO

internal fun PinEntryEditText.onBackPress(action: suspend () -> Unit) = setOnBackPress(action)