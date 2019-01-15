package com.biglabs.mozo.sdk.core

import android.content.Context
import com.biglabs.mozo.sdk.BuildConfig
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.authentication.MozoAuthActivity
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.MozoAPIs
import com.biglabs.mozo.sdk.common.model.*
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.ui.dialog.ErrorDialog
import com.biglabs.mozo.sdk.utils.Support
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MozoService private constructor() {

    private val mozoAPIs: MozoAPIs by lazy { createService() }

    fun fetchProfile(context: Context, callback: ((data: Profile?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.fetchProfile(), callback)
    }

    /**
     * Wallet Information APIs
     */
    fun saveWallet(context: Context, walletInfo: WalletInfo, callback: ((data: Profile?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.saveWallet(walletInfo), callback)
    }

    fun getBalance(context: Context, address: String, callback: ((data: BalanceInfo?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getBalance(address), callback)
    }

    fun getExchangeRate(context: Context, currency: String, symbol: String = Constant.SYMBOL_MOZO, callback: ((data: ExchangeRate?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getExchangeRate(currency, symbol), callback)
    }

    /**
     * Transaction APIs
     */
    fun createTx(context: Context, request: TransactionRequest, callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.createTx(request), callback)
    }

    fun sendTransaction(context: Context, request: TransactionResponse, callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.sendTx(request), callback)
    }

    fun getTransactionHistory(context: Context, address: String, page: Int = Constant.PAGING_START_INDEX, size: Int = Constant.PAGING_SIZE, callback: ((data: BaseData<TransactionHistory>?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getTransactionHistory(address, page, size), callback)
    }

    fun getTxStatus(context: Context, txHash: String, callback: ((data: TransactionStatus?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getTxStatus(txHash), callback)
    }

    /**
     * Address Book APIs
     */
    fun getContacts(context: Context, callback: ((data: BaseData<Contact>?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getContacts(), callback)
    }

    fun saveContact(context: Context, contact: Contact, callback: ((data: Contact?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.saveContact(contact), callback, handleError = false)
    }

    fun getStoreBook(context: Context, callback: ((data: BaseData<Contact>?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getStoreBook(), callback)
    }

    /**
     * Payment Request APIs
     */
    fun getPaymentRequests(context: Context, page: Int = Constant.PAGING_START_INDEX, size: Int = Constant.PAGING_SIZE, callback: ((data: BaseData<PaymentRequest>?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.getPaymentRequests(page, size), callback)
    }

    fun sendPaymentRequest(context: Context, toAddress: String, request: PaymentRequest, callback: ((data: PaymentRequest?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.sendPaymentRequest(toAddress, request), callback)
    }

    fun deletePaymentRequest(context: Context, id: Long, callback: ((data: Any?, errorCode: String?) -> Unit)? = null) {
        execute(context, mozoAPIs.deletePaymentRequest(id), callback)
    }

    private fun <V, T : Base<V>> execute(
            context: Context,
            call: Call<T>, callback: ((data: V?, errorCode: String?) -> Unit)?,
            handleError: Boolean = true
    ) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful /*200..<300*/) {

                    val body = response.body() as? Base<V>
                    if (body == null) {
                        ErrorDialog.generalError(context, null)
                        return
                    }

                    if (!body.isSuccess && handleError) {
                        // TODO show error with body.errorCode

                        callback?.invoke(null, body.errorCode)
                    } else {
                        callback?.invoke(body.data, body.errorCode)
                    }

                } else /*300..500*/ {

                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (context is BaseActivity || context is MozoAuthActivity) {
                    if (t is IOException) {
                        ErrorDialog.networkError(context, null)
                    } else {
                        ErrorDialog.generalError(context, null)
                    }
                }
            }
        })
    }

    private fun createService(): MozoAPIs {
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
                            .method(original.method(), original.body())
                            .build()
                    it.proceed(request)
                }
                .addInterceptor(HttpLoggingInterceptor().setLevel(
                        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
                ))

        return Retrofit.Builder()
                .baseUrl("https://${Support.domainAPI()}/solomon/api/app/")
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MozoAPIs::class.java)
    }

    companion object {

        @Volatile
        private var instance: MozoService? = null

        fun getInstance() = synchronized(this) {
            if (instance == null) {
                instance = MozoService()
            }
            return@synchronized instance!!
        }
    }
}