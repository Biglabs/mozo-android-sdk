package io.mozocoin.sdk.common.service

import android.app.Activity
import android.content.Context
import io.mozocoin.sdk.BuildConfig
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.authentication.MozoAuthActivity
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.model.*
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.MozoSnackbar
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.equalsIgnoreCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun checkSystemStatus(
        context: Context,
        callback: ((data: Status?, errorCode: String?) -> Unit)?
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.checkSystemStatus(), callback, null, false)
        }
    }

    fun checkNetworkStatus(
        context: Context,
        callback: ((data: Status?, errorCode: String?) -> Unit)?,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.checkSystemStatus(), callback, retry)
        }
    }

    fun getProfile(
        context: Context,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getProfile(), callback, retry)
        }
    }

    fun updateProfile(
        context: Context,
        profile: Profile,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.updateProfile(profile), callback, retry)
        }
    }

    /**
     * Off-Chain Wallet Information APIs
     */
    fun saveWallet(
        context: Context,
        walletInfo: WalletInfo,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.saveWallet(walletInfo), callback, retry)
        }
    }

    fun saveWalletAutoToPin(
        context: Context,
        walletInfo: WalletInfo,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.updateWalletAutoToPin(walletInfo), callback, retry)
        }
    }

    fun updateWalletAfterReset(
        context: Context,
        walletInfo: WalletInfo,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.updateWalletAfterReset(walletInfo), callback, retry)
        }
    }

    fun resetWallet(
        context: Context,
        walletInfo: WalletInfo,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.resetWallet(walletInfo), callback, null, false)
        }
    }

    fun getBalance(
        context: Context,
        address: String,
        callback: ((data: BalanceInfo?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getBalance(address), callback, retry)
        }
    }

    fun getExchangeRate(
        context: Context,
        locale: String,
        callback: ((data: ExchangeRateData?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getExchangeRate(locale), callback, retry, false)
        }
    }

    /**
     * Off-Chain Wallet with On-Chain inside Information APIs
     */
    fun getOnChainBalanceInOffChain(
        context: Context,
        address: String,
        callback: ((data: BalanceTokensData?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getOnChainBalanceInOffChain(address), callback, retry)
        }
    }

    fun getEthBalanceInOffChain(
        context: Context,
        address: String,
        callback: ((data: BalanceEthData?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getEthBalanceInOffChain(address), callback, retry)
        }
    }

    /**
     * On-Chain Wallet Information APIs
     */
    fun saveOnChainWallet(
        context: Context,
        walletInfo: WalletInfo,
        callback: ((data: Profile?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.saveOnChainWallet(walletInfo), callback, retry)
        }
    }

    fun getOnChainBalance(
        context: Context,
        address: String,
        callback: ((data: BalanceData?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getOnChainBalance(address), callback, retry)
        }
    }

    fun getGasInfo(
        context: Context,
        callback: ((data: GasInfo?, errorCode: String?) -> Unit)?,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getGasInfo(), callback, retry)
        }
    }

    fun prepareConvertRequest(
        context: Context,
        request: ConvertRequest,
        callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)?,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.prepareConvertRequest(request), callback, retry)
        }
    }

    fun signConvertRequest(
        context: Context,
        data: TransactionResponse,
        callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)?,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.signConvertRequest(data), callback, retry)
        }
    }

    fun getConvertStatus(
        context: Context,
        txHash: String,
        callback: ((data: TransactionStatus?, errorCode: String?) -> Unit)?,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getConvertStatus(txHash), callback, retry)
        }
    }

    /**
     * Transaction APIs
     */
    fun createTx(
        context: Context,
        request: TransactionRequest,
        callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.createTx(request), callback, retry)
        }
    }

    fun sendTransaction(
        context: Context,
        request: TransactionResponse,
        callback: ((data: TransactionResponse?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.sendTx(request), callback, retry)
        }
    }

    fun getTransactionHistory(
        context: Context,
        address: String,
        isReceive: Boolean? = null,
        page: Int = Constant.PAGING_START_INDEX,
        size: Int = Constant.PAGING_SIZE,
        callback: ((data: BaseData<TransactionHistory>?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(
                context,
                mozoAPIs.getTransactionHistory(address, isReceive, page, size),
                callback,
                retry
            )
        }
    }

    fun getTxStatus(
        context: Context,
        txHash: String,
        callback: ((data: TransactionStatus?, errorCode: String?) -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getTxStatus(txHash), callback)
        }
    }

    /**
     * Address Book APIs
     */
    fun getContactUsers(
        context: Context,
        callback: ((data: BaseData<Contact>?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getContacts(), callback, retry)
        }
    }

    fun saveContact(
        context: Context, contact:
        Contact, callback: ((data: Contact?, errorCode: String?) -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.saveContact(contact), callback, handleError = false)
        }
    }

    fun getContactStores(
        context: Context,
        callback: ((data: BaseData<Contact>?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getStoreBook(), callback, retry)
        }
    }

    fun findContact(
        context: Context,
        phone: String,
        callback: ((data: Contact?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.findContact(phone), callback, retry)
        }
    }

    /**
     * Payment Request APIs
     */
    fun getPaymentRequests(
        context: Context,
        page: Int = Constant.PAGING_START_INDEX,
        size: Int = Constant.PAGING_SIZE,
        callback: ((data: BaseData<PaymentRequest>?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getPaymentRequests(page, size), callback, retry)
        }
    }

    fun sendPaymentRequest(
        context: Context,
        toAddress: String,
        request: PaymentRequest,
        callback: ((data: PaymentRequest?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(
                context,
                mozoAPIs.sendPaymentRequest(toAddress, request),
                callback,
                retry,
                handleError = false
            )
        }
    }

    fun deletePaymentRequest(
        context: Context,
        id: Long,
        callback: ((data: Any?, errorCode: String?) -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.deletePaymentRequest(id), callback)
        }
    }

    /**
     * To-do list APIs
     */
    fun getTodoList4Shopper(
        context: Context,
        isBluetoothOff: Boolean,
        lat: Double,
        lng: Double,
        callback: ((data: BaseData<Todo>?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(
                context,
                mozoAPIs.getTodoList4Shopper(isBluetoothOff, lat, lng),
                callback,
                retry
            )
        }
    }

    fun getTodoSettings(
        context: Context,
        callback: ((data: TodoSettings?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getTodoSettings(), callback, retry)
        }
    }

    fun importContacts(
        context: Context,
        dto: ImportContactRequestDTO,
        retry: (() -> Unit)? = null,
        callback: ((data: ImportedContactDTO?, errorCode: String?) -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.importContacts(dto), callback, retry)
        }
    }

    fun checkingProcess(
        context: Context,
        retry: (() -> Unit)? = null,
        callback: ((data: ImportContactStatus?, errorCode: String?) -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.checkingProcess(), callback, retry)
        }
    }

    /**
     */
    fun getCountries(
        context: Context,
        callback: ((BaseData<CountryCode>?, errorCode: String?) -> Unit)? = null,
        retry: (() -> Unit)? = null
    ) {
        MainScope().launch {
            execute(context, mozoAPIs.getCountries(), callback, retry)
        }
    }

    private suspend fun <V, T : Base<V>> execute(
        context: Context,
        call: Call<T>,
        callback: ((data: V?, errorCode: String?) -> Unit)?,
        retry: (() -> Unit)? = null,
        handleError: Boolean = true
    ) = withContext(Dispatchers.IO) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                MozoSnackbar.dismiss()
                val body = response.body() as? Base<V>
                if (response.isSuccessful /*200..<300*/ && body != null) {
                    if (!body.isSuccess && handleError) {

                        if (context is Activity && !context.isFinishing && !context.isDestroyed) {
                            when {
                                ErrorCode.ERROR_REQUIRED_LOGIN.key.equalsIgnoreCase(body.errorCode) -> {
                                    callback?.invoke(null, body.errorCode)
                                    MozoAuth.getInstance().signOut()
                                    return
                                }
                                ErrorCode.ERROR_MAINTAINING.key.equalsIgnoreCase(body.errorCode) -> {
                                    if (shouldHandleMaintenance(call))
                                        MozoSDK.startMaintenanceMode(context)
                                }
                                ErrorCode.ERROR_UPDATE_VERSION_REQUIREMENT.key.equalsIgnoreCase(body.errorCode) -> {
                                    MozoSDK.startUpdateRequired(context)
                                }
                                else -> ErrorCode.findByKey(body.errorCode)?.let {
                                    when (it.message) {
                                        R.string.error_fatal -> ErrorDialog.withContactError(context)
                                        R.string.error_account_deactivated -> ErrorDialog.deactivatedError(
                                            context
                                        )
                                        else -> MessageDialog(
                                            context,
                                            context.getString(it.message)
                                        )
                                            .setAction(R.string.mozo_button_retry, retry)
                                            .show()
                                    }
                                }
                            }
                        }

                        callback?.invoke(null, body.errorCode)
                    } else {
                        callback?.invoke(body.data, body.errorCode)
                    }

                } else /*300..500*/ {
                    val error = response.errorBody()?.string()
                    if (error != null && error.contains("invalid_token", ignoreCase = true)) {
                        callback?.invoke(null, ErrorCode.ERROR_REQUIRED_LOGIN.key)
                        if (handleError) MozoAuth.getInstance().signOut()
                        return
                    }
                    response.errorBody()?.close()
                    onFailure(call, Throwable())
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback?.invoke(null, null)

                if (!handleError) return
                if (context is BaseActivity || context is MozoAuthActivity || shouldHandleException(
                        call
                    )
                ) {
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

    private fun <T> shouldHandleException(call: Call<T>): Boolean {
        val path = call.request().url.encodedPath
        return path.endsWith("/user-profile", true)
    }

    private fun <T> shouldHandleMaintenance(call: Call<T>): Boolean {
        val path = call.request().url.encodedPath
        return when {
            path.endsWith("system-status", true) -> false
            else -> true
        }
    }

    private fun createService(): MozoAPIs {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor {
                val accessToken = "Bearer ${MozoAuth.getInstance().getAccessToken() ?: ""}"
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
                    when {
                        MozoSDK.isEnableDebugLogging -> HttpLoggingInterceptor.Level.HEADERS
                        BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
                        else -> HttpLoggingInterceptor.Level.NONE
                    }
                )
            )

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
        const val APIS_SCAN = "mozoscan/api/app"

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