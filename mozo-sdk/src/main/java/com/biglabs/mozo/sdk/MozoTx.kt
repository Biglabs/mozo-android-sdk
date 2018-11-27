package com.biglabs.mozo.sdk

import androidx.lifecycle.Observer
import android.content.Context
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.transaction.TransactionFormActivity
import com.biglabs.mozo.sdk.transaction.TransactionHistoryActivity
import com.biglabs.mozo.sdk.utils.CryptoUtils
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal

class MozoTx private constructor() {

    private var decimal = 0.0
    private var exchangeRate = BigDecimal.ZERO

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            this@MozoTx.decimal = decimal.toDouble()
            exchangeRate = rate
        }
    }

    fun transfer() {
        MozoSDK.context?.run {
            TransactionFormActivity.start(this)
            return
        }
    }

    fun openTransactionHistory() {
        TransactionHistoryActivity.start(MozoSDK.context!!)
    }

    internal fun createTransaction(context: Context, output: String, amount: String, pin: String, retryCallback: (request: Models.TransactionResponse?) -> Unit) = GlobalScope.async {
        val myAddress = MozoWallet.getInstance().getAddress().await() ?: return@async null
        val response = MozoService
                .getInstance(context)
                .createTransaction(
                        prepareRequest(myAddress, output, amount)
                ) {
                    retryCallback(null)
                }
                .await()
        if (response != null) {
            val privateKeyEncrypted = MozoWallet.getInstance().getPrivateKeyEncrypted().await()
            val privateKey = CryptoUtils.decrypt(privateKeyEncrypted, pin)
            privateKey?.logAsError("raw privateKey")

            val toSign = response.toSign[0]
            val credentials = Credentials.create(privateKey)
            val signatureData = Sign.signMessage(Numeric.hexStringToByteArray(toSign), credentials.ecKeyPair, false)

            val signature = CryptoUtils.serializeSignature(signatureData)
            signature.logAsError("signature")
            val pubKey = Numeric.toHexStringWithPrefixSafe(credentials.ecKeyPair.publicKey)
            pubKey.logAsError("pubKey")
            response.signatures = arrayListOf(signature)
            response.publicKeys = arrayListOf(pubKey)
            return@async sendTransaction(context, response) { retryCallback(response) }.await()
        } else {
            "create Tx failed".logAsError()
            return@async null
        }
    }

    private fun sendTransaction(context: Context, request: Models.TransactionResponse, callback: () -> Unit) = GlobalScope.async {
        return@async MozoService
                .getInstance(context)
                .sendTransaction(request, callback)
                .await()
    }

    private fun prepareRequest(inAdd: String, outAdd: String, amount: String): Models.TransactionRequest {
        val finalAmount = amountWithDecimal(amount)
        return Models.TransactionRequest(
                arrayListOf(Models.TransactionAddress(arrayListOf(inAdd))),
                arrayListOf(Models.TransactionAddressOutput(arrayListOf(outAdd), finalAmount))
        )
    }

    internal fun getTransactionStatus(context: Context, txHash: String, retry: () -> Unit) = MozoService.getInstance(context).getTransactionStatus(txHash, retry)

    internal fun amountWithDecimal(amount: String) = amountWithDecimal(amount.toBigDecimal())

    internal fun amountWithDecimal(amount: BigDecimal) = amount.multiply(Math.pow(10.0, decimal).toBigDecimal())

    companion object {
        @Volatile
        private var instance: MozoTx? = null

        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) {
                instance = MozoTx()
                MozoSDK.getInstance().profileViewModel.run {
                    balanceAndRateLiveData.observeForever(instance!!.balanceAndRateObserver)
                }
            }
            instance
        }!!
    }
}