package io.mozocoin.sdk.common.model

import android.util.Base64
import com.google.gson.Gson
import org.json.JSONObject
import java.nio.charset.Charset

internal data class TokenInfo(
    val access_token: String?,
    val refresh_token: String?,
    val expires_in: Long?,
    val refresh_expires_in: Long?,
) {
    @Transient
    var pinSecret: String? = null

    @Transient
    var accessExpireAt: Long = 0

    @Transient
    var refreshExpireAt: Long = 0

    internal fun initialize(): TokenInfo {
        try {
            access_token?.split(".")?.getOrNull(1)?.let { payload ->
                val json = JSONObject(
                    String(Base64.decode(payload, Base64.URL_SAFE), Charset.forName("utf-8"))
                )
                /**
                 * Get pin_secret from token
                 */
                if (json.has(paramPinSecret)) {
                    pinSecret = json.getString(paramPinSecret)
                }
                if (json.has(paramIat)) {
                    val issueTime = json.getLong(paramIat)
                    accessExpireAt = issueTime + (expires_in ?: 0)
                    refreshExpireAt = issueTime + (refresh_expires_in ?: 0)
                }
            }
        } catch (ignored: Exception) {
        }
        return this
    }

    override fun toString(): String = Gson().toJson(this)

    companion object {
        const val paramPinSecret = "pin_secret"
        const val paramIat = "iat"

        fun fromJson(str: String?): TokenInfo? {
            if (str.isNullOrEmpty()) return null
            return Gson().fromJson(str, TokenInfo::class.java)?.initialize()
        }
    }
}