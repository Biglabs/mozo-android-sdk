package io.mozocoin.sdk.common.service

import android.app.Activity
import android.content.Context
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R
import io.mozocoin.sdk.authentication.MozoAuthActivity
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.model.*
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.Support
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class MozoAPIsService private constructor() {

    private val mozoAPIs: MozoAPIs by lazy { createService() }

    fun getProfile(context: Context, callback: ((data: Profile?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getProfile(), callback, retry)
        }
    }

    fun updateProfile(context: Context, profile: Profile, callback: ((data: Profile?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.updateProfile(profile), callback)
        }
    }

    /**
     * Off-Chain Wallet Information APIs
     */
    fun saveWallet(context: Context, walletInfo: WalletInfo, callback: ((data: Profile?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.saveWallet(walletInfo), callback)
        }
    }

    fun getBalance(context: Context, address: String, callback: ((data: BalanceInfo?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getBalance(address), callback)
        }
    }

    fun getExchangeRate(context: Context, locale: String, callback: ((data: ExchangeRateData?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getExchangeRate(locale), callback)
        }
    }

    /**
     * Off-Chain Wallet with On-Chain inside Information APIs
     */
    fun getOnChainBalanceInOffChain(context: Context, address: String, callback: ((data: BalanceTokensData?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getOnChainBalanceInOffChain(address), callback, retry)
        }
    }

    fun getEthBalanceInOffChain(context: Context, address: String, callback: ((data: BalanceEthData?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getEthBalanceInOffChain(address), callback, retry)
        }
    }

    /**
     * On-Chain Wallet Information APIs
     */
    fun saveOnChainWallet(context: Context, walletInfo: WalletInfo, callback: ((data: Profile?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.saveOnChainWallet(walletInfo), callback, retry)
        }
    }

    fun getOnChainBalance(context: Context, address: String, callback: ((data: BalanceData?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getOnChainBalance(address), callback, retry)
        }
    }

    fun getGasInfo(context: Context, callback: ((data: GasInfo?, errorCode: String?) -> Unit)?, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getGasInfo(), callback, retry)
        }
    }

    fun prepareConvertRequest(context: Context, request: ConvertRequest, callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)?, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.prepareConvertRequest(request), callback, retry)
        }
    }

    fun signConvertRequest(context: Context, data: TransactionResponse, callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)?, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.signConvertRequest(data), callback, retry)
        }
    }

    fun getConvertStatus(context: Context, txHash: String, callback: ((data: TransactionStatus?, errorCode: String?) -> Unit)?, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getConvertStatus(txHash), callback, retry)
        }
    }

    /**
     * Transaction APIs
     */
    fun createTx(context: Context, request: TransactionRequest, callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.createTx(request), callback)
        }
    }

    fun sendTransaction(context: Context, request: TransactionResponse, callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)? = null, retry: (() -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.sendTx(request), callback, retry)
        }
    }

    fun getTransactionHistory(
            context: Context,
            address: String,
            page: Int = Constant.PAGING_START_INDEX,
            size: Int = Constant.PAGING_SIZE,
            callback: ((data: BaseData<TransactionHistory>?, errorCode: String?) -> Unit)? = null,
            retry: (() -> Unit)? = null
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getTransactionHistory(address, page, size), callback, retry)
        }
    }

    fun getTxStatus(context: Context, txHash: String, callback: ((data: TransactionStatus?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getTxStatus(txHash), callback)
        }
    }

    /**
     * Address Book APIs
     */
    fun getContactUsers(context: Context, callback: ((data: BaseData<Contact>?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getContacts(), callback)
        }
    }

    fun saveContact(context: Context, contact: Contact, callback: ((data: Contact?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.saveContact(contact), callback, handleError = false)
        }
    }

    fun getContactStores(context: Context, callback: ((data: BaseData<Contact>?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getStoreBook(), callback)
        }
    }

    /**
     * Payment Request APIs
     */
    fun getPaymentRequests(context: Context, page: Int = Constant.PAGING_START_INDEX, size: Int = Constant.PAGING_SIZE, callback: ((data: BaseData<PaymentRequest>?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.getPaymentRequests(page, size), callback)
        }
    }

    fun sendPaymentRequest(context: Context, toAddress: String, request: PaymentRequest, callback: ((data: PaymentRequest?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.sendPaymentRequest(toAddress, request), callback, handleError = false)
        }
    }

    fun deletePaymentRequest(context: Context, id: Long, callback: ((data: Any?, errorCode: String?) -> Unit)? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            execute(context, mozoAPIs.deletePaymentRequest(id), callback)
        }
    }

    private fun <V, T : Base<V>> execute(
            context: Context,
            call: Call<T>,
            callback: ((data: V?, errorCode: String?) -> Unit)?,
            retry: (() -> Unit)? = null,
            handleError: Boolean = true
    ) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body() as? Base<V>
                if (response.isSuccessful /*200..<300*/ && body != null) {
                    if (!body.isSuccess && handleError) {

                        if (context is Activity && !context.isFinishing && !context.isDestroyed) {
                            ErrorCode.findByKey(body.errorCode)?.let {
                                if (it.shouldShowContactMessage()) {
                                    ErrorDialog.withContactError(context)

                                } else
                                    MessageDialog(context, context.getString(it.message))
                                            .setAction(R.string.mozo_button_retry, retry)
                                            .show()
                            }
                        }

                        callback?.invoke(null, body.errorCode)
                    } else {
                        callback?.invoke(body.data, body.errorCode)
                    }

                } else /*300..500*/ {
                    val error = response.errorBody()?.string()
                    if (error != null && error.contains("invalid_token", ignoreCase = true)) {
                        MozoAuth.getInstance().signOut()
                        return
                    }
                    onFailure(call, Throwable())
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback?.invoke(null, null)

                if (context is BaseActivity || context is MozoAuthActivity) {
                    if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                        return
                    }
                    if (t is IOException) {
                        ErrorDialog.networkError(context, onTryAgain = retry)
                    } else {
                        ErrorDialog.generalError(context, onTryAgain = retry)
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
                .baseUrl("https://${Support.domainAPI()}/")
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MozoAPIs::class.java)
    }

    companion object {

        const val APIS_SOLOMON = "solomon/api/app"
        const val APIS_STORE = "store/api/app"

        @Volatile
        private var instance: MozoAPIsService? = null

        fun getInstance() = synchronized(this) {
            if (instance == null) {
                instance = MozoAPIsService()
            }
            return@synchronized instance!!
        }
    }
}