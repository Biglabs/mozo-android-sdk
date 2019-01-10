package com.biglabs.mozo.sdk

import android.content.Context
import androidx.lifecycle.Observer
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.common.model.*
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.transaction.TransactionFormActivity
import com.biglabs.mozo.sdk.transaction.TransactionHistoryActivity
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal

class MozoTx private constructor() {

    private var decimal = 0.0
    private var exchangeRate = BigDecimal.ZERO

    private var messagesToSign: Array<out String>? = null
    private var callbackToSign: ((result: List<Triple<String, String, String>>) -> Unit)? = null

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

    internal fun createTransaction(context: Context, output: String, amount: String, pin: String, callback: (request: TransactionResponse?) -> Unit) {
        val myAddress = MozoWallet.getInstance().getAddress()
        if (myAddress == null) {
            callback.invoke(null)
            return
        }
        MozoService.getInstance().createTx(context, prepareRequest(myAddress, output, amount)) { data, _ ->
            data ?: return@createTx
            val privateKeyEncrypted = MozoWallet.getInstance().getPrivateKeyEncrypted()
            val privateKey = CryptoUtils.decrypt(privateKeyEncrypted, pin)

            val toSign = data.toSign[0]
            val credentials = Credentials.create(privateKey)
            val signatureData = Sign.signMessage(Numeric.hexStringToByteArray(toSign), credentials.ecKeyPair, false)

            val signature = CryptoUtils.serializeSignature(signatureData)
            val pubKey = Numeric.toHexStringWithPrefixSafe(credentials.ecKeyPair.publicKey)
            data.signatures = arrayListOf(signature)
            data.publicKeys = arrayListOf(pubKey)

            MozoService.getInstance().sendTransaction(context, data) { txResponse, _ ->
                callback.invoke(txResponse)
            }
        }
    }

    internal fun getTransactionStatus(context: Context, txHash: String, callback: (status: TransactionStatus) -> Unit) {
        MozoService.getInstance().getTxStatus(context, txHash) { data, _ ->
            data ?: return@getTxStatus
            callback.invoke(data)
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    internal fun onUserCancel(event: MessageEvent.UserCancel) {
        EventBus.getDefault().unregister(this)
        messagesToSign = null
        callbackToSign = null
    }

    @SuppressWarnings("unused")
    @Subscribe
    internal fun onReceivePin(event: MessageEvent.Pin) {
        EventBus.getDefault().unregister(this)

        messagesToSign ?: return

        if (messagesToSign!!.isNotEmpty() && callbackToSign != null && event.requestCode == SecurityActivity.KEY_VERIFY_PIN) {
            GlobalScope.launch {
                val privateKeyEncrypted = MozoWallet.getInstance().getPrivateKeyEncrypted()
                val privateKey = CryptoUtils.decrypt(privateKeyEncrypted, event.pin)
                val credentials = Credentials.create(privateKey)
                val publicKey = Numeric.toHexStringWithPrefixSafe(credentials.ecKeyPair.publicKey)

                val result = messagesToSign!!.map {
                    val signature = CryptoUtils.serializeSignature(
                            Sign.signMessage(
                                    Numeric.hexStringToByteArray(it),
                                    credentials.ecKeyPair,
                                    false
                            )
                    )
                    return@map Triple(it, signature, publicKey)
                }

                launch(Dispatchers.Main) {
                    callbackToSign!!.invoke(result)
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