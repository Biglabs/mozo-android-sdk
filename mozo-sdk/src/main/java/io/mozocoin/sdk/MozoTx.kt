package io.mozocoin.sdk

import android.content.Context
import androidx.lifecycle.Observer
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.*
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.transaction.TransactionFormActivity
import io.mozocoin.sdk.transaction.TransactionHistoryActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal

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

    internal fun createTransaction(context: Context, output: String, amount: String, pin: String, callback: (response: TransactionResponse?, doRetry: Boolean) -> Unit) {
        val myAddress = MozoWallet.getInstance().getAddress()
        if (myAddress == null) {
            callback.invoke(null, false)
            return
        }
        MozoAPIsService.getInstance().createTx(context, prepareRequest(myAddress, output, amount)) { data, _ ->
            data ?: return@createTx

            val wallet = MozoWallet.getInstance().getWallet()?.decrypt(pin)
            val credentials = wallet?.buildOffChainCredentials()
            if (wallet != null && credentials != null) {

                val toSign = data.toSign.firstOrNull()
                if (toSign == null) {
                    callback.invoke(null, true)
                    return@createTx
                }
                val signatureData = Sign.signMessage(Numeric.hexStringToByteArray(toSign), credentials.ecKeyPair, false)

                val signature = CryptoUtils.serializeSignature(signatureData)
                val pubKey = Numeric.toHexStringWithPrefixSafe(credentials.ecKeyPair.publicKey)
                data.signatures = arrayListOf(signature)
                data.publicKeys = arrayListOf(pubKey)

                MozoAPIsService.getInstance().sendTransaction(context, data, { txResponse, _ ->
                    callback.invoke(txResponse, false)
                }, {
                    callback.invoke(null, true)
                })
            } else {
                callback.invoke(null, false)
            }
        }
    }

    internal fun getTransactionStatus(context: Context, txHash: String, callback: (status: TransactionStatus) -> Unit) {
        MozoAPIsService.getInstance().getTxStatus(context, txHash) { data, _ ->
            data ?: return@getTxStatus
            callback.invoke(data)
        }
    }

    @Suppress("unused")
    @Subscribe
    internal fun onUserCancel(event: MessageEvent.UserCancel) {
        checkNotNull(event)
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

        if (messagesToSign!!.isNotEmpty() && callbackToSign != null && event.requestCode == SecurityActivity.KEY_VERIFY_PIN) {
            GlobalScope.launch {
                val wallet = MozoWallet.getInstance().getWallet()?.decrypt(event.pin)
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
    fun amountWithDecimal(amount: BigDecimal): BigDecimal = amount.multiply(Math.pow(10.0, decimal).toBigDecimal())

    fun amountNonDecimal(amount: String): BigDecimal = amountNonDecimal(amount.toBigDecimal())
    fun amountNonDecimal(amount: BigDecimal): BigDecimal = amount.divide(Math.pow(10.0, decimal).toBigDecimal())

    fun openTransactionHistory() {
        TransactionHistoryActivity.start(MozoSDK.getInstance().context)
    }

    internal fun signOnChainMessage(context: Context, message: String, callback: (message: String, signature: String, publicKey: String) -> Unit) {
        isOnChainMessageSigning = true
        signMessages(context, message) {
            it.firstOrNull()?.run {
                callback.invoke(first, second, third)
            }
        }
    }

    fun signMessage(context: Context, message: String, callback: (message: String, signature: String, publicKey: String) -> Unit) {
        signMessages(context, message) {
            it.firstOrNull()?.run {
                callback.invoke(first, second, third)
            }
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

    companion object {
        private const val TAG = "Transaction"

        @Volatile
        private var instance: MozoTx? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance = MozoTx()
            return@synchronized instance!!
        }
    }
}