package com.biglabs.mozo.sdk.core

import android.content.Context
import com.biglabs.mozo.sdk.BuildConfig
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.utils.AuthStateManager
import com.biglabs.mozo.sdk.utils.logAsError
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MozoService private constructor() {
    fun getContacts() = async {
        return@async try {
            mAPIs?.getContacts()?.await()?.body()
        } catch (e: Exception) {
            emptyList<Models.Contact>()
        }
    }

    fun saveContact(contact: Models.Contact) = async {
        return@async try {
            mAPIs?.saveContact(contact)?.await()?.body()
        } catch (ex: Exception) {
            null
        }
    }

    fun fetchProfile() = async {
        return@async try {
            mAPIs?.fetchProfile()?.await()?.body()
        } catch (e: Exception) {
            "fetchProfile exception".logAsError()
            null
        }
    }

    //
//    fun saveExchangeInfo(exchangeInfo: Models.Profile): Deferred<Response<Models.Profile>> {
//    }
//
//    fun saveSettings(notificationThreshold: Int): Deferred<Response<Models.Profile>> {
//    }

    fun saveWallet(walletInfo: Models.WalletInfo) = async {
        return@async try {
            mAPIs?.saveWallet(walletInfo)?.await()?.body()
        } catch (ex: Exception) {
            null
        }
    }

    fun getBalance(address: String) = async {
        return@async try {
            mAPIs?.getBalance(address)?.await()?.body()
        } catch (e: Exception) {
            null
        }
    }

    fun createTransaction(request: Models.TransactionRequest) = async {
        return@async try {
            mAPIs?.createTransaction(request)?.await()?.body()
        } catch (ex: Exception) {
            null
        }
    }

    fun sendTransaction(request: Models.TransactionResponse) = async {
        return@async try {
            mAPIs?.sendTransaction(request)?.await()?.body()
        } catch (ex: Exception) {
            null
        }
    }

    fun getTransactionHistory(address: String, page: Int = Constant.PAGING_START_INDEX, size: Int = Constant.PAGING_SIZE) = async {
        return@async try {
            mAPIs?.getTransactionHistory(address, page, size)?.await()?.body()
        } catch (ex: Exception) {
            emptyList<Models.TransactionHistory>()
        }
    }

    fun getExchangeRate(currency: String, symbol: String = Constant.SYMBOL_SOLO) = async {
        return@async try {
            mAPIs?.getExchangeRate(currency, symbol)?.await()?.body()
        } catch (ex: Exception) {
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