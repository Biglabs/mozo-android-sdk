package io.mozocoin.sdk.common.service

import android.app.Activity
import android.content.Context
import com.google.gson.JsonObject
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.TokenInfo
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.utils.SharedPrefsUtils
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.logAsInfo
import io.mozocoin.sdk.utils.logPublic
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

internal class MozoTokenService private constructor() {
    private val mAPIs: KeyCloakAPIs by lazy { createService() }
    private val clientId: String by lazy {
        MozoSDK.getInstance().context.getString(
            when {
                MozoSDK.isInternalApps -> R.string.auth_client_id_operation
                MozoSDK.isRetailerApp -> R.string.auth_client_id_retailer
                else -> R.string.auth_client_id_shopper
            }
        )
    }

    var tokenInfo: TokenInfo? = null
        get() {
            if (field == null) tokenInfo = SharedPrefsUtils.tokenInfo
            return field
        }
        private set

    fun isAuthorized(): Boolean {
        if (!tokenInfo?.access_token.isNullOrEmpty()) {
            tokenInfo!!.initialize()
            return tokenInfo!!.accessExpireAt > (System.currentTimeMillis() / 1000)
        }

        return false
    }

    fun shouldRefreshToken(): Boolean {
        tokenInfo ?: return false
        val secondOffset = 5/*minute*/ * 60
        if (tokenInfo!!.accessExpireAt - secondOffset <= (System.currentTimeMillis() / 1000)) {
            return true
        }

        val expirationTime = Calendar.getInstance()
        expirationTime.timeInMillis = (tokenInfo?.refreshExpireAt ?: 0) * 1000

        val expireAt = expirationTime.time
        expirationTime.add(Calendar.DAY_OF_MONTH, -2)

        "Access Token expire at: $expireAt\nWill be refresh at: ${expirationTime.time}".logPublic()
        return Calendar.getInstance().after(expirationTime)
    }

    fun requestToken(
        code: String,
        redirectUri: String,
        codeVerifier: String,
        callback: ((token: TokenInfo?, error: Exception?) -> Unit)?
    ) {
        mAPIs.requestToken(
            clientId,
            grantType = "authorization_code",
            code = code,
            redirectUri = redirectUri,
            codeVerifier = codeVerifier
        ).enqueue(object : Callback<TokenInfo> {
            override fun onResponse(call: Call<TokenInfo>, response: Response<TokenInfo>) {
                tokenInfo = response.body()?.initialize()
                SharedPrefsUtils.tokenInfo = tokenInfo
                callback?.invoke(tokenInfo, null)
                reportToken()
            }

            override fun onFailure(call: Call<TokenInfo>, t: Throwable) {
                callback?.invoke(null, Exception(t))
            }
        })
    }

    fun reportToken() {
        val token = tokenInfo?.access_token ?: return
        if (token.isEmpty()) return
        "Report token $token".logAsInfo()
        MainScope().launch {
            val obj = JsonObject()
            obj.addProperty("token", token)
            mAPIs.reportToken(obj).enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    "Report token isSuccessful: ${response.isSuccessful}".logAsInfo()
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    "Report token onFailure: ${t.localizedMessage}".logAsInfo()
                }
            })
        }
    }

    fun refreshToken(callback: ((token: TokenInfo?) -> Unit)?) {
        val refreshToken = tokenInfo?.refresh_token ?: return
        mAPIs.requestToken(
            clientId,
            grantType = "refresh_token",
            refreshToken = refreshToken
        ).enqueue(object : Callback<TokenInfo> {
            override fun onResponse(call: Call<TokenInfo>, response: Response<TokenInfo>) {
                tokenInfo = response.body()?.initialize()
                SharedPrefsUtils.tokenInfo = tokenInfo
                callback?.invoke(tokenInfo)
                reportToken()
            }

            override fun onFailure(call: Call<TokenInfo>, t: Throwable) {
                callback?.invoke(null)
            }
        })
    }

    fun checkSession(
        context: Context,
        callback: ((isExpired: Boolean) -> Unit)?,
        retry: (() -> Unit)? = null
    ) {
        mAPIs.getUserInfo().enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                callback?.invoke(!response.isSuccessful)
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                if (context is Activity && !context.isFinishing && !context.isDestroyed) {
                    if (t is IOException) {
                        ErrorDialog.networkError(context, onTryAgain = retry)
                    } else {
                        ErrorDialog.generalError(context, onTryAgain = retry)
                    }
                    ErrorDialog.setCancelable(false)
                }
            }
        })
    }

    private fun createService(): KeyCloakAPIs {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor {
                val accessToken = "Bearer ${tokenInfo?.access_token ?: ""}"
                val original = it.request()
                val request = original.newBuilder()
                    .header("Authorization", accessToken)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", Support.userAgent())
                    .method(original.method, original.body)
                    .build()
                it.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
                )
            )

        return Retrofit.Builder()
            .baseUrl("https://${Support.domainAuth()}/")
            .client(client.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KeyCloakAPIs::class.java)
    }

    private interface KeyCloakAPIs {
        @FormUrlEncoded
        @POST("auth/realms/mozo/protocol/openid-connect/token")
        fun requestToken(
            @Field("client_id") clientId: String,
            @Field("grant_type") grantType: String,
            @Field("code") code: String? = null,
            @Field("redirect_uri") redirectUri: String? = null,
            @Field("code_verifier") codeVerifier: String? = null,
            @Field("refresh_token") refreshToken: String? = null
        ): Call<TokenInfo>

        /**
         * Report Token
         */
        @POST
        fun reportToken(
            @Body info: JsonObject,
            @Url url: String = "https://${Support.domainAPI()}/store/api/public/tokenHistory/addTokenHistory"
        ): Call<Any>

        @GET("auth/realms/mozo/protocol/openid-connect/userinfo")
        fun getUserInfo(): Call<Any>
    }

    companion object {
        @Volatile
        private var instance: MozoTokenService? = null

        @JvmStatic
        fun instance(): MozoTokenService =
            instance ?: synchronized(this) {
                instance = MozoTokenService()
                instance!!
            }

        fun clear() {
            instance().tokenInfo = null
            SharedPrefsUtils.tokenInfo = null
        }
    }
}