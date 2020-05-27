package io.mozocoin.sdk.utils

import android.content.Context
import android.util.Log
import android.util.Patterns
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoSDK
import java.security.MessageDigest
import java.util.*

internal fun String?.logAsError(prefix: String? = null) {
    if (BuildConfig.DEBUG || MozoSDK.isEnableDebugLogging) {
        Log.e("MozoSDK", (if (prefix != null) "$prefix: " else "") + this)
        Log.e("MozoSDK", " ")
    }
}

internal fun String?.logAsInfo(prefix: String? = null) {
    if (BuildConfig.DEBUG || MozoSDK.isEnableDebugLogging) {
        Log.i("MozoSDK", (if (prefix != null) "$prefix: " else "") + this)
        Log.i("MozoSDK", " ")
    }
}

fun String.censor(paddingStart: Int, paddingEnd: Int, mask: Char = '*'): String = toCharArray().mapIndexed { i, c ->
    if (i >= paddingStart && i < length - paddingEnd && !c.isWhitespace()) mask else c
}.joinToString(separator = "")

fun String.md5() = encrypt(this, "MD5")

fun String.sha1() = encrypt(this, "SHA-1")

fun String.isIdcard(): Boolean {
    val p18 = "^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]\$".toRegex()
    val p15 = "^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{2}[0-9Xx]\$".toRegex()
    return matches(p18) || matches(p15)
}

fun String.isPhone(): Boolean = !isNullOrEmpty() && Patterns.PHONE.matcher(this).matches()

fun String.isValidPhone(context: Context): Boolean {
    if (!isPhone()) return false

    return try {
        val phoneUtil = PhoneNumberUtil.createInstance(context)
        val phoneNumber = phoneUtil.parse(this, Locale.getDefault().language)

        phoneNumber != null && phoneUtil.isValidNumber(phoneNumber)
    } catch (e: Exception) {
        false
    }
}

fun String.isEmail(): Boolean = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isNumeric(): Boolean {
    val p = "^[0-9]+$".toRegex()
    return matches(p)
}

fun String.equalsIgnoreCase(other: String?) = this.toLowerCase(Locale.getDefault()).contentEquals(other.safe().toLowerCase(Locale.getDefault()))

fun String?.safe() = this ?: ""

private fun encrypt(string: String?, type: String): String {
    val bytes = MessageDigest.getInstance(type).digest(string!!.toByteArray())
    return bytes2Hex(bytes)
}

internal fun bytes2Hex(bts: ByteArray): String {
    var des = ""
    var tmp: String
    for (i in bts.indices) {
        tmp = Integer.toHexString(bts[i].toInt() and 0xFF)
        if (tmp.length == 1) {
            des += "0"
        }
        des += tmp
    }
    return des
}
