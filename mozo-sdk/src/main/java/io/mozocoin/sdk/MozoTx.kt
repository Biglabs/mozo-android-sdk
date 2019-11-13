package io.mozocoin.sdk

import android.content.Context
import androidx.lifecycle.Observer
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.*
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.transaction.TransactionFormActivity
import io.mozocoin.sdk.transaction.TransactionHistoryActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.utils.CryptoUtils
import io.mozocoin.sdk.utils.logAsInfo
import io.mozocoin.sdk.utils.safe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal
import kotlin.math.pow

class MozoTx private constructor() {

    private var decimal = Constant.DEFAULT_DECIMAL.toDouble()
    private var exchangeRate = Constant.DEFAULT_CURRENCY_RATE.toBigDecimal()

    private var messagesToSign: Array<out String>? = null
    private var callbackToSign: ((result: List<Triple<String, String, String>>) -> Unit)? = null
    private var isOnChainMessageSigning = false

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            this@MozoTx.decimal = decimal.toDouble()
            exchangeRate = rate
        }
    }

    init {
        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.observeForever(balanceAndRateObserver)
        }
    }

    private fun prepareRequest(inAdd: String, outAdd: String, amount: String): TransactionRequest {
        val finalAmount = amountWithDecimal(amount)
        return TransactionRequest(
                arrayListOf(TransactionAddress(arrayListOf(inAdd))),
                arrayListOf(TransactionAddressOutput(arrayListOf(outAdd), finalAmount))
        )
    }

    internal fun verifyAddress(context: Context, output: String, callback: (isValid: Boolean) -> Unit) {
        MozoAPIsService.getInstance().createTx(
                context,
                prepareRequest(MozoWallet.getInstance().getAddress().safe(), output, "0"),
                { _, errorCode ->
                    callback.invoke(ErrorCode.ERROR_TX_INVALID_ADDRESS.key != errorCode)
                }
                ,
                {
                    verifyAddress(context, output, callback)
                }
        )
    }

    internal fun createTransaction(context: Context, output: String, amount: String, callback: (response: TransactionResponse?, doRetry: Boolean) -> Unit) {
        val myAddress = MozoWallet.getInstance().getAddress()
        if (myAddress == null) {
            callback.invoke(null, false)
            return
        }
        MozoAPIsService.getInstance().createTx(context, prepareRequest(myAddress, output, amount), { data, errorCode ->
            if (data == null) {
                callback.invoke(null, false)

                if (ErrorCode.findByKey(errorCode) == null) {
                    /**
                     * Handle otherwise errors
                     * */
                    ErrorDialog.generalError(context) {
                        callback.invoke(null, true)
                    }
                }
                return@createTx
            }

            val toSign = data.toSign.firstOrNull()
            if (toSign == null) {
                callback.invoke(null, true)
                return@createTx
            }
            signMessage(context, toSign) { _, signature, publicKey ->
                if (signature.isEmpty() || publicKey.isEmpty()) {
                    callback.invoke(null, false)
                    return@signMessage
                }

                data.signatures = arrayListOf(signature)
                data.publicKeys = arrayListOf(publicKey)

                MozoAPIsService.getInstance().sendTransaction(context, data, { txResponse, _ ->
                    callback.invoke(txResponse, false)
                }, {
                    callback.invoke(null, true)
                })
            }
        }, {
            createTransaction(context, output, amount, callback)
        })
    }

    internal fun getTransactionStatus(context: Context, txHash: String, callback: (status: TransactionStatus) -> Unit) {
        MozoAPIsService.getInstance().getTxStatus(context, txHash) { data, _ ->
            data ?: return@getTxStatus
            callback.invoke(data)
        }
    }

    @Suppress("unused")
    @Subscribe
    internal fun onUserCancel(@Suppress("UNUSED_PARAMETER") event: MessageEvent.UserCancel) {
        EventBus.getDefault().unregister(this)
        messagesToSign = null
        callbackToSign?.invoke(emptyList())
        callbackToSign = null
    }

    @Suppress("unused")
    @Subscribe
    internal fun onReceivePin(event: MessageEvent.Pin) {
        EventBus.getDefault().unregister(this)

        messagesToSign ?: return

        if (
                messagesToSign!!.isNotEmpty()
                && callbackToSign != null
                && (event.requestCode == SecurityActivity.KEY_VERIFY_PIN
                        || event.requestCode == SecurityActivity.KEY_VERIFY_PIN_FOR_SEND)
        ) {
            GlobalScope.launch {
                val wallet = MozoWallet.getInstance().getWallet()?.decrypt(event.pin)
                "My Wallet: ${wallet.toString()}".logAsInfo(TAG)
                val credentials = if (isOnChainMessageSigning) wallet?.buildOnChainCredentials() else wallet?.buildOffChainCredentials()
                val result = if (credentials != null) {
                    val publicKey = Numeric.toHexStringWithPrefixSafe(credentials.ecKeyPair.publicKey)
                    messagesToSign!!.map {
                        val signature = CryptoUtils.serializeSignature(
                                Sign.signMessage(
                                        Numeric.hexStringToByteArray(it),
                                        credentials.ecKeyPair,
                                        false
                                )
                        )
                        return@map Triple(it, signature, publicKey)
                    }
                } else {
                    emptyList()
                }

                wallet?.lock()
                isOnChainMessageSigning = false
                withContext(Dispatchers.Main) {
                    callbackToSign?.invoke(result)
                    messagesToSign = null
                    callbackToSign = null
                }
            }
        } else {
            messagesToSign = null
            callbackToSign = null
        }
    }

    fun amountWithDecimal(amount: String): BigDecimal = amountWithDecimal(amount.toBigDecimal())
    fun amountWithDecimal(amount: BigDecimal): BigDecimal = amount.multiply(10.0.pow(decimal).toBigDecimal())

    fun amountNonDecimal(amount: String): BigDecimal = amountNonDecimal(amount.toBigDecimal())
    fun amountNonDecimal(amount: BigDecimal): BigDecimal = amount.divide(10.0.pow(decimal).toBigDecimal())

    fun openTransactionHistory(context: Context) {
        TransactionHistoryActivity.start(context)
    }

    internal fun signOnChainMessage(context: Context, message: String, callback: (message: String, signature: String, publicKey: String) -> Unit) {
        isOnChainMessageSigning = true
        signMessages(context, message) {
            val trip = it.firstOrNull()
            callback.invoke(trip?.first ?: "", trip?.second ?: "", trip?.third ?: "")
        }
    }

    fun signMessage(context: Context, message: String, callback: (message: String, signature: String, publicKey: String) -> Unit) {
        signMessages(context, message) {
            val trip = it.firstOrNull()
            callback.invoke(trip?.first ?: "", trip?.second ?: "", trip?.third ?: "")
        }
    }

    fun signMessages(context: Context, vararg messages: String, callback: (result: List<Triple<String, String, String>>) -> Unit) {
        if (callbackToSign != null) return

        this.messagesToSign = messages
        this.callbackToSign = callback

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        SecurityActivity.startVerify(context)
    }

    fun transfer() {
        TransactionFormActivity.start(MozoSDK.getInstance().context)
    }

    fun mozoDecimal() = decimal

    companion object {
        private const val TAG = "Transaction"

        @Volatile
        private var instance: MozoTx? = null

        @JvmStatic
        fun getInstance() = instance ?: synchronized(this) {
            instance = MozoTx()
            return@synchronized instance!!
        }
    }
}