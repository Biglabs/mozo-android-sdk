package io.mozocoin.sdk

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import io.mozocoin.sdk.authentication.AuthStateListener
import io.mozocoin.sdk.authentication.MozoAuthActivity
import io.mozocoin.sdk.authentication.ProfileChangeListener
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.Gender
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.WalletHelper
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.UserInfo
import io.mozocoin.sdk.common.service.*
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.UserCancelException
import io.mozocoin.sdk.utils.logAsInfo
import io.mozocoin.sdk.utils.logPublic
import kotlinx.coroutines.*
import net.openid.appauth.AuthorizationException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

@Suppress("RedundantSuspendModifier", "unused")
class MozoAuth private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }
    private val mozoAPIs: MozoAPIsService by lazy { MozoAPIsService.getInstance() }
    private val walletService: MozoWallet by lazy { MozoWallet.getInstance() }

    private var mAuthListeners: MutableList<AuthStateListener> = mutableListOf()
    private var mProfileChangeListeners: MutableList<ProfileChangeListener>? = null

    internal var isInitialized = false
    private val mComponentCallbacks: ComponentCallbacks by lazy {
        object : ComponentCallbacks {
            override fun onLowMemory() {}
            override fun onConfigurationChanged(newConfig: Configuration) {
                val symbol = MozoSDK.getInstance().profileViewModel
                    .exchangeRateLiveData.value?.token?.currencySymbol
                    ?: Constant.DEFAULT_CURRENCY_SYMBOL
                if (symbol != Currency.getInstance(Locale.getDefault()).symbol) {
                    MozoSDK.getInstance().profileViewModel.fetchExchangeRate(MozoSDK.getInstance().context)
                }
            }
        }
    }

    init {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    @Subscribe
    internal fun onAuthorizeChanged(auth: MessageEvent.Auth) {
        if (auth.exception is UserCancelException) {
            MozoSDK.getInstance().profileViewModel.clear()
            mAuthListeners.forEach { it.onAuthCanceled() }
            return
        }

        when {
            isSignedIn() -> {
                MozoSDK.getInstance().contactViewModel.fetchData(MozoSDK.getInstance().context)
                MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context) {
                    if (it == null || it.walletInfo?.onchainAddress.isNullOrEmpty()) {
                        syncProfile(MozoSDK.getInstance().context) { isSuccess ->
                            mAuthListeners.forEach { l -> l.onAuthStateChanged(isSuccess) }
                            if (isSuccess) {
                                MozoSocketClient.connect()
                                MozoSDK.getInstance().context.registerComponentCallbacks(
                                    mComponentCallbacks
                                )
                            }
                        }
                    } else {
                        mAuthListeners.forEach { l -> l.onAuthStateChanged(true) }
                        MozoSocketClient.connect()
                        MozoSDK.getInstance().context.registerComponentCallbacks(mComponentCallbacks)
                    }
                }
            }
            auth.exception is AuthorizationException
                    && auth.exception.code == AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR.code -> {
                MainScope().launch {
                    delay(1000)
                    signIn()
                }
            }
            else -> {
                MozoSDK.getInstance().profileViewModel.clear()
                /* notify for caller */
                mAuthListeners.forEach { l -> l.onAuthStateChanged(false) }
                MozoSDK.getInstance().context.unregisterComponentCallbacks(mComponentCallbacks)
            }
        }
    }

    private fun initialize() {
        if (isInitialized) return
        if (isSignedIn() && MozoTokenService.instance().shouldRefreshToken()) {
            MozoTokenService.instance().refreshToken {
                onAuthorizeChanged(MessageEvent.Auth())
            }
        } else {
            onAuthorizeChanged(MessageEvent.Auth())
            MozoTokenService.instance().reportToken()
        }

        isInitialized = true
    }

    private var signedInCallbackJob: Job? = null
    private fun onSignedInBeforeWallet() {
        signedInCallbackJob?.cancel()
        signedInCallbackJob = MainScope().launch {
            delay(2000) // 2s
            mAuthListeners.forEach { l -> l.onSignedIn() }
            signedInCallbackJob = null
        }
    }

    fun signIn() {
        if (ConnectionService.isNetworkAvailable) {
            initialize()
            walletService.clear()
            MozoAuthActivity.signIn(MozoSDK.getInstance().context)
        } else {
            ErrorDialog.networkError(
                MozoSDK.getInstance().remindAnchorView?.context
                    ?: MozoSDK.getInstance().context,
                ::signIn
            )
        }
    }

    fun signOut() = signOut(false)

    internal fun signOut(silent: Boolean = false) {
        Support.logStackTrace()

        MessageDialog.dismiss()
        MozoTokenService.clear()

        walletService.clear()
        mozoDB.clear()

        MozoSocketClient.disconnect()
        onAuthorizeChanged(MessageEvent.Auth())
        MozoAuthActivity.signOut(MozoSDK.getInstance().context, silent = silent)
    }

    fun isSignedIn() = MozoTokenService.instance().isAuthorized()

    fun isSignUpCompleted(context: Context, callback: (isCompleted: Boolean) -> Unit) {
        MozoSDK.getInstance().profileViewModel.fetchData(context) {
            if (it == null) {
                syncProfile(context) { isSuccess ->
                    callback.invoke(isSignedIn() && isSuccess)
                }
            } else callback.invoke(isSignedIn() && MozoSDK.getInstance().profileViewModel.hasWallet())
        }
    }

    fun getAccessToken(): String? {
        val token = MozoTokenService.instance().tokenInfo?.access_token
        if (token.isNullOrEmpty()) {
            "Access token is NULL or empty".logPublic()
        }
        return token
    }

    /**
     *  Get current user information
     * @param  fromCache    If true the result will be returns from cache immediately if it
     * available, otherwise the value will be reloaded from network before returns. Default is true
     */
    fun getUserInfo(
        context: Context,
        fromCache: Boolean = true,
        callback: (userInfo: UserInfo?) -> Unit
    ) {
        if (fromCache) {
            callback.invoke(MozoSDK.getInstance().profileViewModel.userInfoLiveData.value)
            return
        }
        syncProfile(context) {
            callback.invoke(MozoSDK.getInstance().profileViewModel.userInfoLiveData.value)
        }
    }

    fun updateUserInfo(
        context: Context,
        avatar: String? = null,
        fullName: String? = null,
        birthday: Long? = null,
        email: String? = null,
        gender: Gender? = null,
        callback: (userInfo: UserInfo?) -> Unit
    ) {
        val finalEmail = if (email.isNullOrEmpty()) null else email
        mozoAPIs.updateProfile(
            context,
            Profile(
                avatarUrl = avatar,
                fullName = fullName,
                birthday = birthday,
                email = finalEmail,
                gender = gender?.key
            ),
            { data, _ ->

                if (data == null) {
                    callback.invoke(null)
                    return@updateProfile
                }

                val userInfo = UserInfo(
                    userId = data.userId ?: "",
                    avatarUrl = data.avatarUrl,
                    fullName = data.fullName,
                    phoneNumber = data.phoneNumber,
                    birthday = data.birthday ?: 0L,
                    email = data.email,
                    gender = data.gender
                )
                callback.invoke(userInfo)

                MozoSDK.scope.launch {
                    /* save User info first */
                    mozoDB.userInfo().deleteAll()
                    mozoDB.userInfo().save(userInfo)
                    MozoSDK.getInstance().profileViewModel.updateUserInfo(userInfo)
                }
            },
            {
                updateUserInfo(context, avatar, fullName, birthday, email, gender, callback)
            })
    }

    fun checkSession(context: Context, callback: (isExpired: Boolean) -> Unit) {
        val tokenService = MozoTokenService.instance()
        "Check token via Keycloak: Start".logAsInfo()
        tokenService.checkSession(context, { isExpired ->
            "Check token via Keycloak: Done, isExpired: $isExpired".logAsInfo()

            if (isExpired) {
                /* Token has expired, try to request the new token */
                tokenService.refreshToken { token ->
                    callback.invoke(token == null)
                }
            } else /* Token is working */
                callback.invoke(false)
        }, {
            "Check token via Keycloak: from Retry action".logAsInfo()
            checkSession(context, callback)
        })
    }

    @Synchronized
    internal fun syncProfile(context: Context, callback: ((success: Boolean) -> Unit)? = null) {
        if (!isSignedIn() || !ConnectionService.isNetworkAvailable) {
            callback?.invoke(false)
            return
        }
        mozoAPIs.getProfile(context, { data, _ ->
            if (data == null) {
                callback?.invoke(false)
                mAuthListeners.forEach { l -> l.onAuthFailed() }
                return@getProfile
            }

            /* Invoke Sign In event for subscriber */
            onSignedInBeforeWallet()

            if (data.walletInfo?.encryptSeedPhrase.isNullOrEmpty()) {
                saveUserInfo(context, data, callback = callback)
                return@getProfile
            }



            MozoSDK.scope.launch {
                doSaveUserInfoAsync(data)
                MozoSDK.getInstance().profileViewModel.fetchData(context, data.userId) {
                    if (
                        it?.walletInfo?.encryptSeedPhrase == data.walletInfo?.encryptSeedPhrase &&
                        it?.walletInfo?.offchainAddress?.equals(
                            data.walletInfo?.offchainAddress,
                            ignoreCase = true
                        ) == true &&
                        it.walletInfo?.onchainAddress?.equals(
                            data.walletInfo?.onchainAddress,
                            ignoreCase = true
                        ) == true
                    ) {
                        callback?.invoke(true) // No need recover wallet
                    } else {
                        saveUserInfo(context, data, callback = callback)
                    }
                }
            }
        }, {
            syncProfile(context, callback)
        })
    }

    internal fun saveUserInfo(
        context: Context,
        profile: Profile,
        walletHelper: WalletHelper? = null,
        callback: ((success: Boolean) -> Unit)? = null
    ) {
        if (MozoSDK.isInternalApps) {
            /**
             * Skip initialize Wallet for Internal apps
             */
            MozoSDK.scope.launch {
                mozoDB.profile().save(profile)
                /* save User info first */
                doSaveUserInfoAsync(profile)

                MozoSDK.getInstance().profileViewModel.updateProfile(context, profile)

                withContext(Dispatchers.Main) {
                    callback?.invoke(true)
                }
            }
            return
        }
        MozoWallet.getInstance().initWallet(context, profile, walletHelper) {
            MozoSDK.scope.launch {
                if (it) {
                    /* update local profile to match with server profile */
                    profile.apply {
                        walletInfo = MozoWallet.getInstance().getWallet()?.buildWalletInfo()
                    }
                    mozoDB.profile().save(profile)
                    /* save User info first */
                    doSaveUserInfoAsync(profile)

                    MozoSDK.getInstance().profileViewModel.updateProfile(context, profile)
                }
                withContext(Dispatchers.Main) {
                    callback?.invoke(it)
                }
            }
        }
    }

    private suspend fun doSaveUserInfoAsync(profile: Profile): UserInfo {
        mozoDB.userInfo().deleteAll()

        val userInfo = UserInfo(
            userId = profile.userId ?: "",
            avatarUrl = profile.avatarUrl,
            fullName = profile.fullName,
            phoneNumber = profile.phoneNumber,
            birthday = profile.birthday ?: 0L,
            email = profile.email,
            gender = profile.gender
        )
        mozoDB.userInfo().save(userInfo)
        MozoSDK.getInstance().profileViewModel.updateUserInfo(userInfo)

        return userInfo
    }


    fun addAuthStateListener(listener: AuthStateListener) {
        this.mAuthListeners.add(listener)
        initialize()
    }

    fun removeAuthStateListener(listener: AuthStateListener) {
        this.mAuthListeners.remove(listener)
    }

    fun addProfileChangeListener(listener: ProfileChangeListener) {
        if (mProfileChangeListeners == null) {
            mProfileChangeListeners = mutableListOf()
        }
        mProfileChangeListeners?.add(listener)
    }

    fun removeProfileChangeListener(listener: ProfileChangeListener) {
        this.mProfileChangeListeners?.remove(listener)
    }

    companion object {
        @Volatile
        private var instance: MozoAuth? = null

        @JvmStatic
        fun getInstance(): MozoAuth =
            instance ?: synchronized(this) {
                instance = MozoAuth()
                instance!!
            }

        internal fun invokeProfileChangeListener(userInfo: UserInfo) {
            instance?.mProfileChangeListeners?.forEach { it.onProfileChanged(userInfo) }
        }
    }
}