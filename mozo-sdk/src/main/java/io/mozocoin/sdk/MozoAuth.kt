package io.mozocoin.sdk

import android.content.Context
import io.mozocoin.sdk.authentication.AuthStateManager
import io.mozocoin.sdk.authentication.AuthenticationListener
import io.mozocoin.sdk.authentication.MozoAuthActivity
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.model.UserInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.common.service.MozoSocketClient
import io.mozocoin.sdk.common.service.MozoTokenService
import io.mozocoin.sdk.ui.SecurityActivity
import io.mozocoin.sdk.utils.UserCancelException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@Suppress("RedundantSuspendModifier", "unused")
class MozoAuth private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }
    private val walletService: MozoWallet by lazy { MozoWallet.getInstance() }

    private val authStateManager: AuthStateManager by lazy { AuthStateManager.getInstance(MozoSDK.getInstance().context) }
    private var mAuthListener: AuthenticationListener? = null

    init {
        GlobalScope.launch(Dispatchers.Main) {
            onAuthorizeChanged(MessageEvent.Auth(isSignedIn()))
        }
    }

    fun isSignedIn() = authStateManager.current.isAuthorized

    fun isSignUpCompleted() = authStateManager.current.isAuthorized && walletService.isHasWallet()

    fun signIn() {
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

    fun setAuthenticationListener(listener: AuthenticationListener) {
        this.mAuthListener = listener
    }

    fun getAccessToken() = authStateManager.current.accessToken

    fun getUserInfo(context: Context, callback: (userInfo: UserInfo?) -> Unit) {
        MozoAPIsService.getInstance().fetchProfile(context) { data, _ ->
            if (data == null) {
                callback.invoke(MozoSDK.getInstance().profileViewModel.userInfoLiveData.value)
                return@fetchProfile
            }

            val userInfo = UserInfo(
                    userId = data.userId ?: "",
                    avatarUrl = data.avatarUrl,
                    fullName = data.fullName,
                    phoneNumber = data.phoneNumber,
                    birthday = data.birthday,
                    email = data.email,
                    gender = data.gender
            )
            callback.invoke(userInfo)

            GlobalScope.launch {
                /* save User info first */
                mozoDB.userInfo().save(userInfo)
                MozoSDK.getInstance().profileViewModel.updateUserInfo(userInfo)
            }
        }
    }

    fun checkSession(context: Context, callback: (isExpired: Boolean) -> Unit) {
        val tokenService = MozoTokenService.newInstance()
        tokenService.checkSession(context, {

            if (it) {
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

    @Subscribe
    internal fun onAuthorizeChanged(auth: MessageEvent.Auth) {
        EventBus.getDefault().unregister(this@MozoAuth)

        if (auth.exception is UserCancelException) {
            mAuthListener?.onCanceled()
            return
        }

        if (auth.isSignedIn) {
            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context) {
                mAuthListener?.onChanged(true)
                MozoSocketClient.connect()
            }
            MozoSDK.getInstance().contactViewModel.fetchData(MozoSDK.getInstance().context)
        } else {
            MozoSDK.getInstance().profileViewModel.clear()
            MozoSocketClient.disconnect()
            /* notify for caller */
            mAuthListener?.onChanged(false)
        }
    }

    internal fun syncProfile(context: Context, callback: ((flag: Int) -> Unit)? = null) {
        MozoAPIsService.getInstance().fetchProfile(context) { data, _ ->
            data ?: return@fetchProfile

            GlobalScope.launch {
                val userInfo = UserInfo(
                        userId = data.userId ?: "",
                        avatarUrl = data.avatarUrl,
                        fullName = data.fullName,
                        phoneNumber = data.phoneNumber,
                        birthday = data.birthday,
                        email = data.email,
                        gender = data.gender
                )
                /* save User info first */
                mozoDB.userInfo().save(userInfo)
                MozoSDK.getInstance().profileViewModel.updateUserInfo(userInfo)
            }

            if (data.walletInfo?.offchainAddress.isNullOrEmpty() || data.walletInfo?.encryptSeedPhrase.isNullOrEmpty()) {
                GlobalScope.launch {
                    MozoSDK.getInstance().profileViewModel.updateProfile(context, data)
                    delay(100)
                    MozoWallet.getInstance().initWallet(context).await()
                    callback?.invoke(SecurityActivity.KEY_CREATE_PIN)
                }
                return@fetchProfile
            }

            MozoSDK.getInstance().profileViewModel.fetchData(context, data.userId) {
                if (
                        it?.walletInfo?.encryptSeedPhrase == data.walletInfo?.encryptSeedPhrase &&
                        it?.walletInfo?.offchainAddress == data.walletInfo?.offchainAddress
                ) {
                    callback?.invoke(-1) // No need recover wallet
                } else {
                    GlobalScope.launch {
                        /* update local profile to match with server profile */
                        mozoDB.profile().save(data)

                        MozoSDK.getInstance().profileViewModel.updateProfile(context, data)

                        val flag = MozoWallet.getInstance().initWallet(context).await()
                        callback?.invoke(flag)
                    }
                }
            }
        }
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