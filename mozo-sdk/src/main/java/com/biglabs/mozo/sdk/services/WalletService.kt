package com.biglabs.mozo.sdk.services

import android.content.Context
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.CryptoUtils
import com.biglabs.mozo.sdk.utils.PreferenceUtils
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.experimental.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import java.security.SecureRandom

@Suppress("unused")
internal class WalletService private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.context!!) }

    private var seed: String? = null
    private var address: String? = null
    private var privateKey: String? = null
    private var privateKeyEncrypted: String? = null

    private var profile: Models.Profile? = null

    init {
        getAddress()
    }

    fun initWallet() = async {
        getAddress().await()
        profile?.run {
            if (walletInfo == null || walletInfo!!.encryptSeedPhrase.isNullOrEmpty()) {
                /* Server wallet is NOT existing, create a new one at local */
                clearVariables()
                val mnemonic = MnemonicUtils.generateMnemonic(
                        SecureRandom().generateSeed(16)
                )
                this@WalletService.seed = mnemonic
                this@WalletService.privateKey = CryptoUtils.getFirstAddressPrivateKey(mnemonic)

                val credentials = Credentials.create(privateKey)
                this@WalletService.address = credentials.address

                /* Required input new PIN */
                return@async SecurityActivity.KEY_CREATE_PIN
            } else {
                if (PreferenceUtils.getInstance(MozoSDK.context!!).getFlag(PreferenceUtils.FLAG_SYNC_WALLET_INFO)) {
                    syncWalletInfo(walletInfo!!, MozoSDK.context!!)
                }
                if (walletInfo?.privateKey.isNullOrEmpty()) {
                    /* Local wallet is existing but no private Key */
                    /* Required input previous PIN */
                    return@async SecurityActivity.KEY_ENTER_PIN
                }
            }
        }
        return@async 0
    }

    internal fun executeSaveWallet(pin: String, context: Context) = async {
        getAddress().await()
        profile?.let {
            it.walletInfo = Models.WalletInfo(
                    CryptoUtils.encrypt(this@WalletService.seed!!, pin),
                    this@WalletService.address!!,
                    CryptoUtils.encrypt(this@WalletService.privateKey!!, pin)
            )

            /* save wallet info to server */
            val isSuccess = syncWalletInfo(it.walletInfo!!, context).await()

            if (!isSuccess) {
                return@async false
            }

            /* save wallet info to local */
            mozoDB.profile().save(it)
        }
        clearVariables()
        return@async true
    }

    private fun syncWalletInfo(walletInfo: Models.WalletInfo, context: Context) = async {
        val response = MozoService.getInstance(context).saveWallet(walletInfo).await()
        val success = response != null
        PreferenceUtils.getInstance(context).setFlag(
                PreferenceUtils.FLAG_SYNC_WALLET_INFO,
                !success
        )

        return@async success
    }

    fun validatePin(pin: String) = async {
        getAddress().await()
        profile?.walletInfo?.run {
            if (encryptSeedPhrase.isNullOrEmpty() || pin.isEmpty()) return@async false
            else return@async try {
                var decrypted = CryptoUtils.decrypt(encryptSeedPhrase!!, pin)
                val isCorrect = !decrypted.isNullOrEmpty() && MnemonicUtils.validateMnemonic(decrypted)
                if (isCorrect) {
                    privateKey = CryptoUtils.encrypt(
                            CryptoUtils.getFirstAddressPrivateKey(decrypted!!),
                            pin
                    )
                    mozoDB.profile().save(profile!!)
                }
                decrypted?.logAsError("mnemonic")
                @Suppress("UNUSED_VALUE")
                decrypted = null
                isCorrect
            } catch (ex: Exception) {
                false
            }
        }

        return@async false
    }

    fun isHasWallet() = profile?.walletInfo != null

    @Subscribe
    fun onReceivePin(event: MessageEvent.Pin) {
        EventBus.getDefault().unregister(this@WalletService)
        /* load data to variables */
        getAddress()
        address?.logAsError("address after synchronize")
    }

    @Synchronized
    fun getAddress() = async {
        if (profile == null) {
            profile = mozoDB.profile().getCurrentUserProfile()
            address = profile?.walletInfo?.offchainAddress
            privateKeyEncrypted = profile?.walletInfo?.privateKey
        }
        return@async address
    }

    internal fun getSeed() = this.seed

    internal fun getPrivateKeyEncrypted() = async {
        if (privateKeyEncrypted == null) {
            getAddress().await()
            privateKeyEncrypted = profile?.walletInfo?.privateKey
        }
        return@async privateKeyEncrypted ?: ""
    }

    internal fun clear() {
        clearVariables()
        profile = null
    }

    private fun clearVariables() {
        this@WalletService.seed = null
        this@WalletService.privateKey = null
    }

    companion object {
        @Volatile
        private var instance: WalletService? = null

        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = WalletService()
            instance
        }!!
    }
}