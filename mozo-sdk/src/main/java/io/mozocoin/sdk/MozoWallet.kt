package io.mozocoin.sdk

import android.content.Context
import com.google.gson.Gson
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
import io.mozocoin.sdk.utils.logAsError
import io.mozocoin.sdk.utils.string
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.math.BigDecimal

@Suppress("unused")
class MozoWallet private constructor() {
    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }

    private var mProfile: Profile? = null
    private var mWallet: WalletHelper? = null
    private var mInitWalletCallback: ((isSuccess: Boolean) -> Unit)? = null

    init {
        MozoSDK.getInstance().profileViewModel.profileLiveData.observeForever {
            this.mProfile = it
            mWallet = WalletHelper.initWithWalletInfo(it?.walletInfo)
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

    internal fun initWallet(context: Context, profile: Profile, callback: ((success: Boolean) -> Unit)? = null) {
        "MozoWallet initWallet: $profile".logAsError("vu")

        val flag = if (profile.walletInfo == null || profile.walletInfo!!.encryptSeedPhrase.isNullOrEmpty()) {
            /* Server wallet is NOT existing, create a new one at local */
            mWallet = WalletHelper.create()

            "MozoWallet initWallet: create new wallet: ${Gson().toJson(mWallet)} ".logAsError("vu")

            /* Required input new PIN */
            SecurityActivity.KEY_CREATE_PIN
        } else {

            mWallet = WalletHelper.initWithWalletInfo(profile.walletInfo)
            "MozoWallet initWallet: recover wallet: ${Gson().toJson(mWallet)} ".logAsError("vu")


            if (mWallet?.isUnlocked() == false) {
                /* Local wallet is existing but no private Key */
                /* Required input previous PIN */
                SecurityActivity.KEY_ENTER_PIN
            } else 0
        }

        "MozoWallet initWallet: flag: $flag".logAsError("vu")

        if (flag == 0) {
            callback?.invoke(true)
        } else {
            mProfile = profile
            mInitWalletCallback = callback
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
            SecurityActivity.start(context, flag)
            /**
             * Handle after enter PIN at MozoWallet.onReceivePin
             */
        }
    }

    internal fun executeSaveWallet(context: Context, pin: String, callback: ((isSuccess: Boolean) -> Unit)? = null) {
        "MozoWallet executeSaveWallet: $mProfile".logAsError("vu")
        "MozoWallet executeSaveWallet: wallet: ${Gson().toJson(mWallet)} ".logAsError("vu")
        if (mProfile == null) {
            callback?.invoke(false)
            return
        }

        val wallet = getWallet().encrypt(pin).buildWalletInfo()
        "MozoWallet executeSaveWallet: wallet: ${Gson().toJson(wallet)} ".logAsError("vu")
        mProfile!!.apply { walletInfo = wallet }

        /* save wallet info to server */
        syncWalletInfo(wallet, context, callback)
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
            SharedPrefsUtils.setNeedSyncWallet(!isSuccess)
            callback?.invoke(isSuccess)
        }
    }

    @Suppress("UNUSED_VALUE")
    internal fun validatePinAsync(pin: String) = GlobalScope.async {
        if (mWallet == null) {
            mWallet = WalletHelper.initWithWalletInfo(mProfile?.walletInfo)
        }

        val verifyPin = mWallet?.verifyPin(pin)
        return@async !pin.isEmpty() && verifyPin == true
    }

    @Subscribe
    internal fun onUserCancelErrorDialog(event: MessageEvent.UserCancelErrorDialog) {
        checkNotNull(event)
        EventBus.getDefault().unregister(this@MozoWallet)

        MozoAuth.getInstance().signOut()
    }

    @Subscribe
    internal fun onReceivePin(event: MessageEvent.Pin) {
        checkNotNull(event)
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
    internal fun onCancelPin(event: MessageEvent.UserCancel) {
        checkNotNull(event)
        EventBus.getDefault().unregister(this@MozoWallet)

        mInitWalletCallback?.invoke(false)
        EventBus.getDefault().post(MessageEvent.Auth(false, UserCancelException()))
    }

    internal fun getWallet(): WalletHelper {
        if (mWallet == null) {
            mWallet = WalletHelper.create()
        }
        return mWallet!!
    }

    internal fun clear() {
        mWallet = null
        mProfile = null
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