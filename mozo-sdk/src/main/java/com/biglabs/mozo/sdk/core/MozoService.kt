package com.biglabs.mozo.sdk.core

import android.content.Context
import com.biglabs.mozo.sdk.BuildConfig
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.MozoAPIs
import com.biglabs.mozo.sdk.ui.dialog.ErrorDialog
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MozoService private constructor() {

    private fun handleError(ex: Exception? = null, onTryAgain: (() -> Unit)? = null) = async(UI) {
        if (ex != null && ex is IOException) {
            ErrorDialog.networkError(onTryAgain)
        } else {
            ErrorDialog.generalError(onTryAgain)
        }
    }

    fun getContacts(onTryAgain: (() -> Unit)? = null) = async {
        return@async try {
            val response = mAPIs?.getContacts()?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            emptyList<Models.Contact>()
        }
    }

    fun saveContact(contact: Models.Contact, errorCodeForSkipHandle: Array<Int> = emptyArray(), onTryAgain: () -> Unit) = async {
        return@async try {
            val response = mAPIs?.saveContact(contact)?.await()
            if (response?.body() == null &&
                    !errorCodeForSkipHandle.contains(response?.code() ?: 0)) {
                handleError(onTryAgain = onTryAgain)
            }
            response
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            null
        }
    }

    fun fetchProfile(onTryAgain: () -> Unit) = async {
        return@async try {
            val response = mAPIs?.fetchProfile()?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            null
        }
    }

//    fun saveExchangeInfo(exchangeInfo: Models.Profile): Deferred<Response<Models.Profile>> {
//    }
//
//    fun saveSettings(notificationThreshold: Int): Deferred<Response<Models.Profile>> {
//    }

    fun saveWallet(walletInfo: Models.WalletInfo, onTryAgain: () -> Unit) = async {
        return@async try {
            val response = mAPIs?.saveWallet(walletInfo)?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            null
        }
    }

    fun getBalance(address: String) = async {
        return@async try {
            val response = mAPIs?.getBalance(address)?.await()
            if (response?.body() == null) {
                handleError()
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    fun createTransaction(request: Models.TransactionRequest, onTryAgain: (() -> Unit)?) = async {
        return@async try {
            val response = mAPIs?.createTransaction(request)?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            null
        }
    }

    fun sendTransaction(request: Models.TransactionResponse, onTryAgain: (() -> Unit)?) = async {
        try {
            val response = mAPIs?.sendTransaction(request)?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            null
        }
    }

    fun getTransactionHistory(address: String, page: Int = Constant.PAGING_START_INDEX, size: Int = Constant.PAGING_SIZE, onTryAgain: (() -> Unit)?) = async {
        return@async try {
            val response = mAPIs?.getTransactionHistory(address, page, size)?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            emptyList<Models.TransactionHistory>()
        }
    }

    fun getTransactionStatus(txHash: String, onTryAgain: (() -> Unit)?) = async {
        return@async try {
            val response = mAPIs?.getTransactionStatus(txHash)?.await()
            if (response?.body() == null) {
                handleError(onTryAgain = onTryAgain)
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e, onTryAgain)
            null
        }
    }

    fun getExchangeRate(currency: String, symbol: String = Constant.SYMBOL_SOLO) = async {
        return@async try {
            val response = mAPIs?.getExchangeRate(currency, symbol)?.await()
            if (response?.body() == null) {
                handleError()
            }
            response?.body()
        } catch (e: Exception) {
            handleError(e)
            Models.ExchangeRate(0.0)
        }
    }

    companion object {

        private var mAPIs: MozoAPIs? = null

        @Volatile
        private var instance: MozoService? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            if (instance == null) {
                mAPIs = createService(context)
                instance = MozoService()
            }
            instance
        }!!

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