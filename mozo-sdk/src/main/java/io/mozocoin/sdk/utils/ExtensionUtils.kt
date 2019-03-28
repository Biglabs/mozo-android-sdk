package io.mozocoin.sdk.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.customtabs.CustomTabsHelper
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

fun Activity.setMatchParent() {
    val attrs = window.attributes
    attrs.width = ViewGroup.LayoutParams.MATCH_PARENT
    attrs.height = ViewGroup.LayoutParams.MATCH_PARENT
    window.attributes = attrs
}

fun FragmentActivity.replace(@IdRes id: Int, fragment: Fragment, backStackName: String? = null) {
    supportFragmentManager.beginTransaction().replace(id, fragment).apply {
        if (backStackName != null) addToBackStack(backStackName)
    }.commit()
}

fun Fragment.replace(@IdRes id: Int, fragment: Fragment, backStackName: String? = null) {
    childFragmentManager.beginTransaction().replace(id, fragment).apply {
        if (backStackName != null) addToBackStack(backStackName)
    }.commit()
}

internal fun Context.clipboard(): ClipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

internal fun Context.copyText(text: String?) {
    if (!text.isNullOrEmpty()) {
        clipboard().primaryClip = ClipData.newPlainText("mozo_wallet_text", text)
    }
}

internal fun Context.copyWithToast(text: String?) {
    if (!text.isNullOrEmpty()) {
        clipboard().primaryClip = ClipData.newPlainText("mozo_wallet_text", text)
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
    val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(color(R.color.mozo_color_primary))
            .build()
    CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)
    CustomTabsHelper.openCustomTab(this, customTabsIntent, Uri.parse(url), null)
}

fun visible(views: Array<View>) {
    views.map {
        it.visible()
    }
}

fun gone(views: Array<View>) {
    views.map {
        it.gone()
    }
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
fun <T : View> T.click(block: (T) -> Unit) = setOnClickListener { block(it as T) }

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
            block(s)
        }
    })
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
    return this.multiply(Math.pow(10.0, 9.0).toBigDecimal())
}

fun BigDecimal.toGwei(): BigDecimal {
    return this.divide(Math.pow(10.0, 9.0).toBigDecimal())
}

fun BigDecimal?.safe(): BigDecimal = this ?: BigDecimal.ZERO