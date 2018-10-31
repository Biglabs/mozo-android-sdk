package com.biglabs.mozo.sdk.core

import android.content.Context
import com.biglabs.mozo.sdk.BuildConfig
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.authentication.MozoAuthActivity
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.MozoAPIs
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.ui.dialog.ErrorDialog
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MozoService private constructor(val context: Context) {

    private fun <T> handleError(response: Response<T>? = null, exception: Exception? = null, onTryAgain: (() -> Unit)? = null, skipCodes: Array<Int> = emptyArray()) = launch(UI) {
        if (response?.body() != null || skipCodes.contains(response?.code() ?: 0)) return@launch

        if (response?.code() == 401 /* The access token has expired */) {
            MozoAuth.getInstance().signOut()
            return@launch
        }

        if (context is BaseActivity || context is MozoAuthActivity) {
            if (exception != null && exception is IOException) {
                ErrorDialog.networkError(context, onTryAgain)
            } else {
                ErrorDialog.generalError(context, onTryAgain)
            }
        }
    }

    fun getContacts(onTryAgain: (() -> Unit)? = null) = async {
        var response: Response<List<Models.Contact>>? = null
        val ex = try {
            response = mAPIs?.getContacts()?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

    fun saveContact(contact: Models.Contact, errorCodeForSkipHandle: Array<Int> = emptyArray(), onTryAgain: () -> Unit) = async {
        var response: Response<Models.Contact>? = null
        val ex = try {
            response = mAPIs?.saveContact(contact)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain, errorCodeForSkipHandle)
        return@async response
    }

    fun fetchProfile(onTryAgain: () -> Unit) = async {
        var response: Response<Models.Profile>? = null
        val ex = try {
            response = mAPIs?.fetchProfile()?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

//    fun saveExchangeInfo(exchangeInfo: Models.Profile): Deferred<Response<Models.Profile>> {
//    }
//
//    fun saveSettings(notificationThreshold: Int): Deferred<Response<Models.Profile>> {
//    }

    fun saveWallet(walletInfo: Models.WalletInfo, onTryAgain: () -> Unit) = async {
        var response: Response<Models.Profile>? = null
        val ex = try {
            response = mAPIs?.saveWallet(walletInfo)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

    fun getBalance(address: String, onTryAgain: (() -> Unit)?) = async {
        var response: Response<Models.BalanceInfo>? = null
        val ex = try {
            response = mAPIs?.getBalance(address)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

    fun getExchangeRate(currency: String, symbol: String = Constant.SYMBOL_SOLO, onTryAgain: (() -> Unit)?) = async {
        var response: Response<Models.ExchangeRate>? = null
        val ex = try {
            response = mAPIs?.getExchangeRate(currency, symbol)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body() ?: Models.ExchangeRate(0.0)
    }

    fun createTransaction(request: Models.TransactionRequest, onTryAgain: (() -> Unit)?) = async {
        var response: Response<Models.TransactionResponse>? = null
        val ex = try {
            response = mAPIs?.createTransaction(request)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

    fun sendTransaction(request: Models.TransactionResponse, onTryAgain: (() -> Unit)?) = async {
        var response: Response<Models.TransactionResponse>? = null
        val ex = try {
            response = mAPIs?.sendTransaction(request)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

    fun getTransactionHistory(address: String, page: Int = Constant.PAGING_START_INDEX, size: Int = Constant.PAGING_SIZE, onTryAgain: (() -> Unit)?) = async {
        var response: Response<List<Models.TransactionHistory>>? = null
        val ex = try {
            response = mAPIs?.getTransactionHistory(address, page, size)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body() ?: emptyList()
    }

    fun getTransactionStatus(txHash: String, onTryAgain: (() -> Unit)?) = async {
        var response: Response<Models.TransactionStatus>? = null
        val ex = try {
            response = mAPIs?.getTransactionStatus(txHash)?.await()
            null
        } catch (e: Exception) {
            e
        }
        handleError(response, ex, onTryAgain)
        return@async response?.body()
    }

    companion object {

        @Volatile
        private var mAPIs: MozoAPIs? = null

        fun getInstance(context: Context) = synchronized(this) {
            if (mAPIs == null) {
                mAPIs = createService(context.applicationContext)
            }
            MozoService(context)
        }

        private fun createService(context: Context): MozoAPIs {
            val client = OkHttpClient.Builder()
                    .addInterceptor {
                        val accessToken = AuthStateManager.getInstance(context).current.accessToken
                        val original = it.request()
                        val request = original.newBuilder()
                                .header("Authorization", "Bearer $accessToken")
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build()
                        it.proceed(request)
                    }
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)

            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY)
                client.addInterceptor(logging)
            }

            return Retrofit.Builder()
                    .baseUrl(Constant.BASE_API_URL)
                    .client(client.build())
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(MozoAPIs::class.java)
        }
    }
}