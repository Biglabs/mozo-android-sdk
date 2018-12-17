package com.biglabs.mozo.sdk

import android.content.Context
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.contact.AddressBookActivity
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.CryptoUtils
import com.biglabs.mozo.sdk.utils.PreferenceUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import java.math.BigDecimal
import java.security.SecureRandom

@Suppress("unused")
class MozoWallet private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }

    private var forSaveSeed: String? = null
    private var forSaveAddress: String? = null
    private var forSavePrivateKey: String? = null

    private var mProfile: Models.Profile? = null

    init {
        MozoSDK.getInstance().profileViewModel.profileLiveData.observeForever {
            this.mProfile = it
        }
    }

    fun getAddress() = mProfile?.walletInfo?.offchainAddress

    /**
     * Returns the balance of current wallet.
     *
     * @since  2018-12-12
     * @param  fromCache    If true the balance will be returns from cache immediately if it
     * available, otherwise the value will be reloaded from network before returns. Default is true
     * @param  callback     The listener to receive balance value
     * @return              The balance of current wallet
     */
    fun getBalance(fromCache: Boolean = true, callback: (balance: BigDecimal) -> Unit) {
        if (fromCache && MozoSDK.getInstance().profileViewModel.getBalance() != null) {
            callback.invoke(MozoSDK.getInstance().profileViewModel.getBalance()!!.balanceNonDecimal())
            return
        }

        MozoSDK.getInstance().profileViewModel.fetchBalance(MozoSDK.getInstance().context) {
            callback.invoke(it?.balanceNonDecimal() ?: BigDecimal.ZERO)
        }
    }

    fun openAddressBook() {
        AddressBookActivity.start(MozoSDK.getInstance().context)
    }

    internal fun initWallet(context: Context) = GlobalScope.async {
        mProfile?.run {
            if (walletInfo == null || walletInfo!!.encryptSeedPhrase.isNullOrEmpty()) {
                /* Server wallet is NOT existing, create a new one at local */
                clearVariables()
                val mnemonic = MnemonicUtils.generateMnemonic(
                        SecureRandom().generateSeed(16)
                )
                this@MozoWallet.forSaveSeed = mnemonic
                this@MozoWallet.forSavePrivateKey = CryptoUtils.getFirstAddressPrivateKey(mnemonic)

                val credentials = Credentials.create(forSavePrivateKey)
                this@MozoWallet.forSaveAddress = credentials.address

                /* Required input new PIN */
                return@async SecurityActivity.KEY_CREATE_PIN
            } else {
                if (PreferenceUtils.getInstance(context).getFlag(PreferenceUtils.FLAG_SYNC_WALLET_INFO)) {
                    syncWalletInfo(walletInfo!!, context) {}
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

    internal fun executeSaveWallet(pin: String, context: Context, retry: () -> Unit) = GlobalScope.async {
        mProfile ?: return@async false

        val wallet = Models.WalletInfo().apply {
            encryptSeedPhrase = CryptoUtils.encrypt(this@MozoWallet.forSaveSeed!!, pin)
            offchainAddress = this@MozoWallet.forSaveAddress!!
            privateKey = CryptoUtils.encrypt(this@MozoWallet.forSavePrivateKey!!, pin)
        }
        mProfile!!.apply { walletInfo = wallet }

        /* save wallet info to server */
        val isSuccess = syncWalletInfo(wallet, context, retry).await()

        if (!isSuccess) {
            return@async false
        }

        /* save wallet info to local */
        mozoDB.profile().save(mProfile!!)
        MozoSDK.getInstance().profileViewModel.updateProfile(mProfile!!)

        clearVariables()
        return@async true
    }

    private fun syncWalletInfo(walletInfo: Models.WalletInfo, context: Context, retry: () -> Unit) = GlobalScope.async {
        val response = MozoService.getInstance(context).saveWallet(walletInfo, retry).await()
        val success = response != null
        PreferenceUtils.getInstance(context).setFlag(
                PreferenceUtils.FLAG_SYNC_WALLET_INFO,
                !success
        )

        return@async success
    }

    @Suppress("UNUSED_VALUE")
    internal fun validatePin(pin: String) = GlobalScope.async {
        mProfile?.walletInfo?.run {
            if (encryptSeedPhrase.isNullOrEmpty() || pin.isEmpty()) return@async false
            else return@async try {
                var decrypted = CryptoUtils.decrypt(encryptSeedPhrase!!, pin)
                val isCorrect = !decrypted.isNullOrEmpty() && MnemonicUtils.validateMnemonic(decrypted)
                if (isCorrect) {
                    privateKey = CryptoUtils.encrypt(
                            CryptoUtils.getFirstAddressPrivateKey(decrypted!!),
                            pin
                    )
                    mozoDB.profile().save(mProfile!!)
                }
                decrypted = null

                isCorrect
            } catch (ex: Exception) {
                false
            }
        }

        return@async false
    }

    @Subscribe
    internal fun onReceivePin(event: MessageEvent.Pin) {
        EventBus.getDefault().unregister(this@MozoWallet)
        /* load data to variables */
        MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
    }

    internal fun isHasWallet() = MozoSDK.getInstance().profileViewModel.hasWallet()

    internal fun getSeed() = this.forSaveSeed

    internal fun getPrivateKeyEncrypted() = mProfile?.walletInfo?.privateKey ?: ""

    internal fun clear() {
        clearVariables()
        mProfile = null
    }

    private fun clearVariables() {
        this@MozoWallet.forSaveSeed = null
        this@MozoWallet.forSaveAddress = null
        this@MozoWallet.forSavePrivateKey = null
    }

    companion object {
        @Volatile
        private var instance: MozoWallet? = null

        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = MozoWallet()
            instance
        }!!
    }
}