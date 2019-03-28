package io.mozocoin.sdk

import android.content.Context
import io.mozocoin.sdk.authentication.AuthStateListener
import io.mozocoin.sdk.authentication.AuthStateManager
import io.mozocoin.sdk.authentication.MozoAuthActivity
import io.mozocoin.sdk.common.Gender
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.UserInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.common.service.MozoSocketClient
import io.mozocoin.sdk.common.service.MozoTokenService
import io.mozocoin.sdk.utils.UserCancelException
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

@Suppress("RedundantSuspendModifier", "unused")
class MozoAuth private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }
    private val walletService: MozoWallet by lazy { MozoWallet.getInstance() }

    private val authStateManager: AuthStateManager by lazy { AuthStateManager.getInstance(MozoSDK.getInstance().context) }
    private var mAuthListeners: MutableList<AuthStateListener> = mutableListOf()

    internal var isInitialized = false

    @Subscribe
    internal fun onAuthorizeChanged(auth: MessageEvent.Auth) {
        EventBus.getDefault().unregister(this@MozoAuth)

        if (auth.exception is UserCancelException) {
            mAuthListeners.forEach { it.onAuthCanceled() }
            return
        }

        if (isSignedIn()) {
            MozoSDK.getInstance().contactViewModel.fetchData(MozoSDK.getInstance().context)
            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context) {
                if (it == null || it.walletInfo?.onchainAddress.isNullOrEmpty()) {
                    syncProfile(MozoSDK.getInstance().context) { isSuccess ->
                        mAuthListeners.forEach { l -> l.onAuthStateChanged(isSuccess) }
                        if (isSuccess) MozoSocketClient.connect()
                    }
                } else {
                    mAuthListeners.forEach { l -> l.onAuthStateChanged(true) }
                    MozoSocketClient.connect()
                }
            }
        } else {
            MozoSDK.getInstance().profileViewModel.clear()
            MozoSocketClient.disconnect()
            /* notify for caller */
            mAuthListeners.forEach { l -> l.onAuthStateChanged(false) }
        }
    }

    private fun initialize() {
        if (isInitialized) return
        if (isSignedIn() && authStateManager.current.accessTokenExpirationTime ?: 0 > 0) {
            val expirationTime = Calendar.getInstance()
            expirationTime.timeInMillis = authStateManager.current.accessTokenExpirationTime ?: 0
            expirationTime.add(Calendar.DAY_OF_MONTH, -2)

            if (Calendar.getInstance().after(expirationTime)) {
                MozoTokenService.newInstance().refreshToken {
                    onAuthorizeChanged(MessageEvent.Auth(isSignedIn()))
                }
            } else onAuthorizeChanged(MessageEvent.Auth(isSignedIn()))
        } else
            onAuthorizeChanged(MessageEvent.Auth(isSignedIn()))

        isInitialized = true
    }

    fun signIn() {
        initialize()
        walletService.clear()
        if (!EventBus.getDefault().isRegistered(this@MozoAuth)) {
            EventBus.getDefault().register(this@MozoAuth)
        }
        MozoAuthActivity.signIn(MozoSDK.getInstance().context)
    }

    fun signOut() {
        signOut(false)
    }

    fun signOut(reSignIn: Boolean = false) {
        walletService.clear()
        MozoAuthActivity.signOut(MozoSDK.getInstance().context) {

            onAuthorizeChanged(MessageEvent.Auth(false))

            GlobalScope.launch {
                mozoDB.clear()
                authStateManager.clearSession()

                if (reSignIn) {
                    delay(1000)
                    signIn()
                }
            }
        }
    }

    fun isSignedIn() = authStateManager.current.isAuthorized

    fun isSignUpCompleted(callback: (isCompleted: Boolean) -> Unit) {
        MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context) {
            if (it == null) {
                syncProfile(MozoSDK.getInstance().context) { isSuccess ->
                    callback.invoke(authStateManager.current.isAuthorized && isSuccess)
                }
            } else callback.invoke(authStateManager.current.isAuthorized && MozoSDK.getInstance().profileViewModel.hasWallet())
        }
    }

    fun addAuthStateListener(listener: AuthStateListener) {
        this.mAuthListeners.add(listener)
        initialize()
    }

    fun removeAuthStateListener(listener: AuthStateListener) {
        this.mAuthListeners.remove(listener)
    }

    fun getAccessToken() = authStateManager.current.accessToken

    /**
     *  Get current user information
     * @param  fromCache    If true the result will be returns from cache immediately if it
     * available, otherwise the value will be reloaded from network before returns. Default is true
     */
    fun getUserInfo(context: Context, fromCache: Boolean = true, callback: (userInfo: UserInfo?) -> Unit) {
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
        MozoAPIsService.getInstance().updateProfile(
                context,
                Profile(avatarUrl = avatar, fullName = fullName, birthday = birthday, email = finalEmail, gender = gender?.key)
        ) { data, _ ->

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

            GlobalScope.launch {
                /* save User info first */
                mozoDB.userInfo().deleteAll()
                mozoDB.userInfo().save(userInfo)
                MozoSDK.getInstance().profileViewModel.updateUserInfo(userInfo)
            }
        }
    }

    fun checkSession(context: Context, callback: (isExpired: Boolean) -> Unit) {
        val tokenService = MozoTokenService.newInstance()
        tokenService.checkSession(context, { isExpired ->

            if (isExpired) {
                /* Token has expired, try to request the new token */
                tokenService.refreshToken { token ->
                    callback.invoke(token.isNullOrEmpty())
                }
            } else /* Token is working */
                callback.invoke(false)
        }, {
            checkSession(context, callback)
        })
    }

    internal fun syncProfile(context: Context, callback: ((success: Boolean) -> Unit)? = null) {
        MozoAPIsService.getInstance().getProfile(context, { data, _ ->
            callback?.invoke(false)
            data ?: return@getProfile

            GlobalScope.launch {
                if (data.walletInfo?.encryptSeedPhrase.isNullOrEmpty()) {
                    saveUserInfo(context, data, callback)
                    return@launch
                }

                doSaveUserInfoAsync(data).await()

                MozoSDK.getInstance().profileViewModel.fetchData(context, data.userId) {
                    if (
                            it?.walletInfo?.encryptSeedPhrase == data.walletInfo?.encryptSeedPhrase &&
                            it?.walletInfo?.offchainAddress == data.walletInfo?.offchainAddress &&
                            it?.walletInfo?.onchainAddress == data.walletInfo?.onchainAddress
                    ) {
                        callback?.invoke(true) // No need recover wallet
                    } else {
                        saveUserInfo(context, data, callback)
                    }
                }
            }
        }, {
            syncProfile(context, callback)
        })
    }

    private fun saveUserInfo(context: Context, profile: Profile, callback: ((success: Boolean) -> Unit)? = null) {
        MozoWallet.getInstance().initWallet(context, profile) {
            GlobalScope.launch {
                if (it) {


                    /* update local profile to match with server profile */
                    profile.apply { walletInfo = MozoWallet.getInstance().getWallet().buildWalletInfo() }
                    mozoDB.profile().save(profile)
                    /* save User info first */
                    doSaveUserInfoAsync(profile).await()

                    MozoSDK.getInstance().profileViewModel.updateProfile(context, profile)
                }
                withContext(Dispatchers.Main) {
                    callback?.invoke(it)
                }
            }
        }
    }

    private fun doSaveUserInfoAsync(profile: Profile) = GlobalScope.async {
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
        return@async userInfo
    }

    companion object {
        @Volatile
        private var instance: MozoAuth? = null

        fun getInstance(): MozoAuth =
                instance ?: synchronized(this) {
                    instance = MozoAuth()
                    instance!!
                }
    }
}