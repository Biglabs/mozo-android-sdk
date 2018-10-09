package com.biglabs.mozo.sdk.trans

import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.MozoApiService
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.services.WalletService
import com.biglabs.mozo.sdk.utils.CryptoUtils
import com.biglabs.mozo.sdk.utils.PreferenceUtils
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.experimental.async
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal

class MozoTrans private constructor() {

    private val mPreferenceUtils: PreferenceUtils by lazy { PreferenceUtils.getInstance(MozoSDK.context!!) }

    private val decimalRate: Double

    init {
        val decimal = mPreferenceUtils.getDecimal()
        decimalRate = Math.pow(10.0, decimal.toDouble())
    }

    fun getBalance() = async {
        val address = WalletService.getInstance().getAddress().await() ?: return@async null
        val balanceInfo = MozoService
                .getInstance(MozoSDK.context!!)
                .getBalance(address)
                .await()
        mPreferenceUtils.setDecimal(balanceInfo?.decimals ?: -1)
        return@async balanceInfo?.balanceDisplay()
    }

    fun transfer() {
        MozoSDK.context?.run {
            //            if (!EventBus.getDefault().isRegistered(this@MozoTrans)) {
//                EventBus.getDefault().register(this@MozoTrans)
//            }
            TransactionFormActivity.start(this)
            return
        }
    }

    fun openTransactionHistory() {
        TransactionHistoryActivity.start(MozoSDK.context!!)
    }

    internal fun createTransaction(output: String, amount: String, pin: String) = async {
        val myAddress = WalletService.getInstance().getAddress().await() ?: return@async null
        val response = MozoService
                .getInstance(MozoSDK.context!!)
                .createTransaction(
                        prepareRequest(myAddress, output, amount)
                )
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

            return@async sendTransaction(response).await()
        } else {
            "create Tx failed".logAsError()
            return@async null
        }
    }

    private fun sendTransaction(request: Models.TransactionResponse) = async {
        return@async MozoService
                .getInstance(MozoSDK.context!!)
                .sendTransaction(request)
                .await()
    }

    private fun prepareRequest(inAdd: String, outAdd: String, amount: String): Models.TransactionRequest {
        val finalAmount = amountWithDecimal(amount)
        return Models.TransactionRequest(
                arrayListOf(Models.TransactionAddress(arrayListOf(inAdd))),
                arrayListOf(Models.TransactionAddressOutput(arrayListOf(outAdd), finalAmount))
        )
    }

    internal fun amountWithDecimal(amount: String) = amount.toBigDecimal().multiply(BigDecimal.valueOf(decimalRate))

    companion object {
        @Volatile
        private var instance: MozoTrans? = null

        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = MozoTrans()
            instance
        }!!
    }
}