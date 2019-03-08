package io.mozocoin.sdk.utils

import android.util.Log
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoSDK
import java.security.MessageDigest

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

fun String.isPhone(): Boolean {
    val p = "^1([34578])\\d{9}\$".toRegex()
    return matches(p)
}

fun String.isEmail(): Boolean {
    val p = "^(\\w)+(\\.\\w+)*@(\\w)+((\\.\\w+)+)\$".toRegex()
    return matches(p)
}

fun String.isNumeric(): Boolean {
    val p = "^[0-9]+$".toRegex()
    return matches(p)
}

fun String.equalsIgnoreCase(other: String) = this.toLowerCase().contentEquals(other.toLowerCase())

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
