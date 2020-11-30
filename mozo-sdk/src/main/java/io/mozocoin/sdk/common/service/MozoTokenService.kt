package io.mozocoin.sdk.common.service

import android.app.Activity
import android.content.Context
import com.google.gson.JsonObject
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.authentication.AuthStateManager
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.logAsError
import io.mozocoin.sdk.utils.logAsInfo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MozoTokenService private constructor() {

    private val authStateManager: AuthStateManager by lazy { AuthStateManager.getInstance(MozoSDK.getInstance().context) }
    private val mAuthService: AuthorizationService by lazy { AuthorizationService(MozoSDK.getInstance().context) }

    private val mAPIs: KeyCloakAPIs by lazy { createService() }

    private fun token() = authStateManager.current.accessToken

    fun reportToken() {
        val token = token() ?: return
        if (token.isEmpty()) return
        MainScope().launch {
            val obj = JsonObject()
            obj.addProperty("token", token)
            mAPIs.reportToken(obj).enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                }
            })
        }
    }

    fun refreshToken(callback: ((token: String?) -> Unit)?) {
        try {
            mAuthService.performTokenRequest(
                    authStateManager.current.createTokenRefreshRequest(),
                    authStateManager.current.clientAuthentication
            ) { response, ex ->
                authStateManager.updateAfterTokenResponse(response, ex)
                reportToken()

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
        MainScope().launch {
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
                    val accessToken = token()
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
    }
}