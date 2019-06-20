package io.mozocoin.sdk

import android.content.Context
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.WalletHelper
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.WalletInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.contact.AddressBookActivity
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.SharedPrefsUtils
import io.mozocoin.sdk.utils.UserCancelException
import io.mozocoin.sdk.utils.logAsInfo
import io.mozocoin.sdk.utils.string
import io.mozocoin.sdk.wallet.create.CreateWalletActivity
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.math.BigDecimal

@Suppress("unused")
class MozoWallet private constructor() {
    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }

    private var mProfile: Profile? = null
    private var mWallet: WalletHelper? = null
    private var mInitWalletCallback: ((isSuccess: Boolean) -> Unit)? = null
    private var mReady4WalletCheckingJob: Job? = null
    private var mReady4WalletCheckingDelayed = 0L

    internal var isDuringResetPinProcess = false

    init {
        MozoSDK.getInstance().profileViewModel.profileLiveData.observeForever {
            this.mProfile = it
            GlobalScope.launch {
                mWallet = WalletHelper.initWithWalletInfo(it?.walletInfo, mWallet)
            }
        }
    }

    fun getAddress() = mProfile?.walletInfo?.offchainAddress ?: mWallet?.offChainAddress

    /**
     * Returns the balance of current wallet.
     *
     * @since  2018-12-12
     * @param  fromCache    If true the balance will be returns from cache immediately if it
     * available, otherwise the value will be reloaded from network before returns. Default is true
     * @param  callback     The listener to receive balance value
     * @return              The balance of current wallet
     */
    fun getBalance(fromCache: Boolean = true, callback: (balance: BigDecimal, displayInCurrency: String?) -> Unit) {
        if (fromCache && MozoSDK.getInstance().profileViewModel.getBalance() != null) {
            callback.invoke(
                    MozoSDK.getInstance().profileViewModel.getBalance()!!.balanceNonDecimal(),
                    MozoSDK.getInstance().profileViewModel.getBalanceInCurrencyDisplay()
            )
            return
        }

        MozoSDK.getInstance().profileViewModel.fetchBalance(MozoSDK.getInstance().context) {
            callback.invoke(
                    it?.balanceNonDecimal() ?: BigDecimal(-1),
                    MozoSDK.getInstance().profileViewModel.getBalanceInCurrencyDisplay()
            )
        }
    }

    fun getDecimal() = MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.value?.decimal
            ?: Constant.DEFAULT_DECIMAL

    fun amountInCurrency(amount: BigDecimal) = MozoSDK.getInstance().profileViewModel.calculateAmountInCurrency(amount)

    fun openAddressBook() {
        AddressBookActivity.start(MozoSDK.getInstance().context)
    }

    internal fun initWallet(context: Context, profile: Profile /* server */, walletHelper: WalletHelper? = null, callback: ((success: Boolean) -> Unit)? = null) {
        if (profile.walletInfo?.encryptSeedPhrase.isNullOrEmpty()) {
            /* Server wallet is NOT existing, create a new one at local */
            mWallet = WalletHelper.create()

            /* Required input new PIN */
            //SecurityActivity.KEY_CREATE_PIN
            mProfile = profile
            mInitWalletCallback = callback
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
            CreateWalletActivity.start(context)
            /**
             * Handle after enter PIN at MozoWallet.onCreateWalletDone
             */
            return
        }

        GlobalScope.launch {
            mWallet = walletHelper ?: WalletHelper.initWithWalletInfo(profile.walletInfo)
            val flag = if (profile.walletInfo?.onchainAddress.isNullOrEmpty() || mWallet?.isUnlocked() == false) {
                /* Local wallet is existing but no private Key */
                /* Required input previous PIN */
                SecurityActivity.KEY_ENTER_PIN
            } else 0

            withContext(Dispatchers.Main) {
                when {
                    flag == 0 -> callback?.invoke(true)
                    isDuringResetPinProcess -> callback?.invoke(false)
                    else -> {
                        mProfile = profile
                        mInitWalletCallback = callback
                        if (!EventBus.getDefault().isRegistered(this@MozoWallet)) {
                            EventBus.getDefault().register(this@MozoWallet)
                        }
                        SecurityActivity.start(context, flag)
                        /**
                         * Handle after enter PIN at MozoWallet.onReceivePin
                         */
                    }
                }
            }
        }
    }

    internal fun executeSaveWallet(context: Context, pin: String?, callback: ((isSuccess: Boolean) -> Unit)? = null) {
        mReady4WalletCheckingJob?.cancel()

        if (!MozoSDK.isReadyForWallet && mReady4WalletCheckingDelayed < MAX_DELAY_TIME) {
            mReady4WalletCheckingJob = GlobalScope.launch {
                delay(1000)

                mReady4WalletCheckingDelayed += 1000
                executeSaveWallet(context, pin, callback)
            }
            return
        }
        MozoSDK.readyForWallet(true)
        mReady4WalletCheckingDelayed = 0L

        if (mProfile == null) {
            callback?.invoke(false)
            return
        }

        /**
         * Only encrypt wallet if pin is not null during create wallet manual process
         */
        if (pin != null) getWallet()?.encrypt(pin)

        val wallet = getWallet()?.buildWalletInfo()
        mProfile!!.apply { walletInfo = wallet }

        if (wallet == null) {
            callback?.invoke(false)
            return
        }

        /* save wallet info to server */
        syncWalletInfo(wallet, context, callback)
    }

    private fun syncWalletInfo(walletInfo: WalletInfo, context: Context, callback: ((isSuccess: Boolean) -> Unit)? = null) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        MozoAPIsService.getInstance().saveWallet(context, walletInfo, { data, errorCode ->
            when (errorCode) {
                ErrorCode.ERROR_WALLET_ADDRESS_IN_USED.key,
                ErrorCode.ERROR_WALLET_ADDRESS_EXISTING.key,
                ErrorCode.ERROR_WALLET_DIFFERENT.key -> {
                    MessageDialog(context, context.string(ErrorCode.ERROR_WALLET_DIFFERENT.message))
                            .setAction(R.string.mozo_button_restore) {
                                EventBus.getDefault().post(MessageEvent.CloseActivities())
                                MozoAuth.getInstance().signOut()
                            }
                            .cancelable(false)
                            .show()
                }
            }

            val isSuccess = data != null
            SharedPrefsUtils.setNeedSyncWallet(!isSuccess)
            callback?.invoke(isSuccess)
        }, {
            syncWalletInfo(walletInfo, context, callback)
        })
    }

    internal fun syncOnChainWallet(context: Context, pin: String, callback: ((isSuccess: Boolean) -> Unit)? = null) {
        if (mProfile == null) {
            callback?.invoke(false)
            return
        }
        if (mProfile!!.walletInfo?.onchainAddress.isNullOrEmpty()) {
            val wallet = getWallet()?.decrypt(pin)?.buildWalletInfo()
            MozoAPIsService.getInstance().saveOnChainWallet(context, wallet ?: return, { p, _ ->
                callback?.invoke(p != null)
            }, {
                syncOnChainWallet(context, pin, callback)
            })
        } else
            callback?.invoke(true)
    }

    internal fun validatePinAsync(pin: String) = GlobalScope.async {
        if (mWallet == null) {
            mWallet = WalletHelper.initWithWalletInfo(mProfile?.walletInfo)
        }

        val verifyPin = mWallet?.verifyPin(pin)
        return@async pin.isNotEmpty() && verifyPin == true
    }

    @Subscribe
    internal fun onCreateWalletDone(event: MessageEvent.CreateWalletAutomatic) {
        event.toString().logAsInfo()
        EventBus.getDefault().unregister(this@MozoWallet)

        if (mInitWalletCallback != null) {
            mInitWalletCallback?.invoke(mWallet?.isUnlocked() == true)
            mInitWalletCallback = null
        } else {
            /* load data to variables */
            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
        }
    }

    @Subscribe
    internal fun onReceivePin(event: MessageEvent.Pin) {
        EventBus.getDefault().unregister(this@MozoWallet)

        if (mInitWalletCallback != null) {
            if (mWallet == null) mInitWalletCallback?.invoke(false)
            else {
                mWallet!!.decrypt(event.pin)
                mInitWalletCallback?.invoke(mWallet!!.isUnlocked())
            }

            mInitWalletCallback = null
        } else {
            /* load data to variables */
            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
        }
    }

    @Subscribe
    internal fun onUserCancelErrorDialog(@Suppress("UNUSED_PARAMETER") event: MessageEvent.UserCancelErrorDialog) {
        EventBus.getDefault().unregister(this@MozoWallet)

        MozoAuth.getInstance().signOut()
    }

    @Subscribe
    internal fun onCancelPin(@Suppress("UNUSED_PARAMETER") event: MessageEvent.UserCancel) {
        EventBus.getDefault().unregister(this@MozoWallet)

        mInitWalletCallback?.invoke(false)
        mReady4WalletCheckingJob?.cancel()
        mReady4WalletCheckingJob = null
        mReady4WalletCheckingDelayed = 0L

        EventBus.getDefault().post(MessageEvent.Auth(UserCancelException()))
    }

    internal fun getWallet(createNewIfNeed: Boolean = false): WalletHelper? {
        if (createNewIfNeed && mWallet == null) {
            mWallet = WalletHelper.create()
        }
        return mWallet
    }

    internal fun clear() {
        mWallet = null
        mProfile = null
    }

    companion object {
        private const val MAX_DELAY_TIME = 270000L /* 90s x 3 times */

        @Volatile
        private var instance: MozoWallet? = null

        @JvmStatic
        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = MozoWallet()
            instance
        }!!
    }
}