package com.biglabs.mozo.sdk

import android.content.Context
import com.biglabs.mozo.sdk.common.ErrorCode
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.common.model.Profile
import com.biglabs.mozo.sdk.common.model.WalletInfo
import com.biglabs.mozo.sdk.contact.AddressBookActivity
import com.biglabs.mozo.sdk.common.service.MozoDatabase
import com.biglabs.mozo.sdk.common.service.MozoAPIsService
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.ui.dialog.MessageDialog
import com.biglabs.mozo.sdk.utils.CryptoUtils
import com.biglabs.mozo.sdk.utils.PreferenceUtils
import com.biglabs.mozo.sdk.utils.string
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

    private var mProfile: Profile? = null

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
            callback.invoke(it?.balanceNonDecimal() ?: BigDecimal(-1))
        }
    }

    fun openAddressBook() {
        AddressBookActivity.start(MozoSDK.getInstance().context)
    }

    internal fun initWallet(context: Context) = GlobalScope.async {
        mProfile = MozoSDK.getInstance().profileViewModel.getProfile()
        mProfile ?: return@async 0
        if (mProfile!!.walletInfo == null || mProfile!!.walletInfo!!.encryptSeedPhrase.isNullOrEmpty()) {
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
                syncWalletInfo(mProfile!!.walletInfo!!, context) {

                }
            }
            if (mProfile!!.walletInfo?.privateKey.isNullOrEmpty()) {
                /* Local wallet is existing but no private Key */
                /* Required input previous PIN */
                return@async SecurityActivity.KEY_ENTER_PIN
            } else return@async 0
        }
    }

    internal fun executeSaveWallet(context: Context, pin: String, callback: ((isSuccess: Boolean) -> Unit)? = null) {
        if (mProfile == null) {
            callback?.invoke(false)
            return
        }

        val wallet = WalletInfo().apply {
            encryptSeedPhrase = CryptoUtils.encrypt(this@MozoWallet.forSaveSeed!!, pin)
            offchainAddress = this@MozoWallet.forSaveAddress!!
            privateKey = CryptoUtils.encrypt(this@MozoWallet.forSavePrivateKey!!, pin)
        }
        mProfile!!.apply { walletInfo = wallet }

        /* save wallet info to server */
        syncWalletInfo(wallet, context) {
            if (it) {
                GlobalScope.launch {
                    /* save wallet info to local */
                    mozoDB.profile().save(mProfile!!)
                }
                MozoSDK.getInstance().profileViewModel.updateProfile(context, mProfile!!)
                clearVariables()
            }
            callback?.invoke(it)
        }
    }

    private fun syncWalletInfo(walletInfo: WalletInfo, context: Context, callback: ((isSuccess: Boolean) -> Unit)? = null) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        MozoAPIsService.getInstance().saveWallet(context, walletInfo) { _, errorCode ->
            when (errorCode) {
                ErrorCode.ERROR_WALLET_ADDRESS_IN_USED.key,
                ErrorCode.ERROR_WALLET_ADDRESS_EXISTING.key,
                ErrorCode.ERROR_WALLET_DIFFERENT.key -> {
                    MessageDialog(context, context.string(ErrorCode.ERROR_WALLET_DIFFERENT.message))
                            .setAction(R.string.mozo_button_restore) {
                                EventBus.getDefault().post(MessageEvent.CloseActivities())
                                MozoAuth.getInstance().signOut(true)
                            }
                            .cancelable(false)
                            .show()
                }
            }

            val isSuccess = errorCode.isNullOrEmpty()
            PreferenceUtils.getInstance(context).setFlag(
                    PreferenceUtils.FLAG_SYNC_WALLET_INFO,
                    !isSuccess
            )
            callback?.invoke(isSuccess)
        }
    }

    @Subscribe
    internal fun onUserCancelErrorDialog(event: MessageEvent.UserCancelErrorDialog) {
        checkNotNull(event)
        EventBus.getDefault().unregister(this@MozoWallet)

        MozoAuth.getInstance().signOut()
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
        checkNotNull(event)
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