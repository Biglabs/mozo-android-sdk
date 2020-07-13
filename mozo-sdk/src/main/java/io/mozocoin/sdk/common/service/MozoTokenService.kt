package io.mozocoin.sdk.common.service

import android.app.Activity
import android.content.Context
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.authentication.AuthStateManager
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.logAsError
import io.mozocoin.sdk.utils.logAsInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MozoTokenService private constructor() {

    private val authStateManager: AuthStateManager by lazy { AuthStateManager.getInstance(MozoSDK.getInstance().context) }
    private val mAuthService: AuthorizationService by lazy { AuthorizationService(MozoSDK.getInstance().context) }

    private val mAPIs: KeyCloakAPIs by lazy { createService() }

    fun refreshToken(callback: ((token: String?) -> Unit)?) {
        try {
            mAuthService.performTokenRequest(
                    authStateManager.current.createTokenRefreshRequest(),
                    authStateManager.current.clientAuthentication
            ) { response, ex ->
                authStateManager.updateAfterTokenResponse(response, ex)

                response?.run {
                    "Refresh token successful: $accessToken".logAsInfo()
                }
                ex?.run {
                    "Refresh token failed: $message".logAsInfo()
                }

                callback?.invoke(response?.accessToken)
            }
        } catch (ex: Exception) {
            "Fail to refresh token: ${ex.message}".logAsError()
            callback?.invoke(null)
        }
    }

    fun checkSession(
            context: Context,
            callback: ((isExpired: Boolean) -> Unit)?,
            retry: (() -> Unit)? = null
    ) {
        GlobalScope.launch(Dispatchers.Main) {
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
    }

    private fun createService(): KeyCloakAPIs {
        val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor {
                    val accessToken = MozoAuth.getInstance().getAccessToken()
                    val original = it.request()
                    val request = original.newBuilder()
                            .header("Authorization", "Bearer $accessToken")
                            .header("Content-Type", "application/json")
                            .method(original.method, original.body)
                            .build()
                    it.proceed(request)
                }
                .addInterceptor(HttpLoggingInterceptor().setLevel(
                        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
                ))

        return Retrofit.Builder()
                .baseUrl("https://${Support.domainAuth()}/")
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(KeyCloakAPIs::class.java)
    }

    private interface KeyCloakAPIs {
        @GET("auth/realms/mozo/protocol/openid-connect/userinfo")
        fun getUserInfo(): Call<Any>
    }

    companion object {
        fun newInstance() = synchronized(this) {
            return@synchronized MozoTokenService()
        }
    }
}