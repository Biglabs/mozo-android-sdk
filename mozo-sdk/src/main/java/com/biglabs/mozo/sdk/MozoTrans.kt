package com.biglabs.mozo.sdk

import android.arch.lifecycle.Observer
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.core.WalletService
import com.biglabs.mozo.sdk.transaction.TransactionFormActivity
import com.biglabs.mozo.sdk.transaction.TransactionHistoryActivity
import com.biglabs.mozo.sdk.utils.CryptoUtils
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.experimental.async
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal

class MozoTrans private constructor() {

    private val mozoService: MozoService by lazy { MozoService.getInstance(MozoSDK.context!!) }

    private var decimal = 0.0
    private var exchangeRate = BigDecimal.ZERO

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            this@MozoTrans.decimal = decimal.toDouble()
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

    internal fun createTransaction(output: String, amount: String, pin: String, retryCallback: (request: Models.TransactionResponse?) -> Unit) = async {
        val myAddress = WalletService.getInstance().getAddress().await() ?: return@async null
        val response = MozoService
                .getInstance(MozoSDK.context!!)
                .createTransaction(
                        prepareRequest(myAddress, output, amount)
                ) {
                    retryCallback(null)
                }
                .await()
        if (response != null) {
            val privateKeyEncrypted = WalletService.getInstance().getPrivateKeyEncrypted().await()
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
            return@async sendTransaction(response) { retryCallback(response) }.await()
        } else {
            "create Tx failed".logAsError()
            return@async null
        }
    }

    internal fun sendTransaction(request: Models.TransactionResponse, callback: () -> Unit) = async {
        return@async MozoService
                .getInstance(MozoSDK.context!!)
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

    internal fun getTransactionStatus(txHash: String, retry: () -> Unit) = mozoService.getTransactionStatus(txHash, retry)

    internal fun amountWithDecimal(amount: String) = amountWithDecimal(amount.toBigDecimal())

    internal fun amountWithDecimal(amount: BigDecimal) = amount.multiply(Math.pow(10.0, decimal).toBigDecimal())

    companion object {
        @Volatile
        private var instance: MozoTrans? = null

        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) {
                instance = MozoTrans()
                MozoSDK.profileViewModel?.run {
                    balanceAndRateLiveData.observeForever(instance!!.balanceAndRateObserver)
                }
            }
            instance
        }!!
    }
}