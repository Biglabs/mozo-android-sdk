package com.biglabs.mozo.sdk

import android.content.Context
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.authentication.AuthenticationListener
import com.biglabs.mozo.sdk.authentication.MozoAuthActivity
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.common.model.UserInfo
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.core.MozoSocketClient
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.*
import net.openid.appauth.AuthorizationService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@Suppress("RedundantSuspendModifier", "unused")
class MozoAuth private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.getInstance().context) }
    private val walletService: MozoWallet by lazy { MozoWallet.getInstance() }

    private val authStateManager: AuthStateManager by lazy { AuthStateManager.getInstance(MozoSDK.getInstance().context) }
    private val mAuthService: AuthorizationService by lazy { AuthorizationService(MozoSDK.getInstance().context) }

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

    @Subscribe
    internal fun onAuthorizeChanged(auth: MessageEvent.Auth) {
        EventBus.getDefault().unregister(this@MozoAuth)

        if (auth.isSignedIn) {
            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context) {
                mAuthListener?.onChanged(true)
            }
            MozoSDK.getInstance().contactViewModel.fetchData(MozoSDK.getInstance().context)
            MozoSocketClient.connect(MozoSDK.getInstance().context)
        } else {
            MozoSDK.getInstance().profileViewModel.clear()
            MozoSocketClient.disconnect()
            /* notify for caller */
            mAuthListener?.onChanged(false)
        }
    }

    internal fun syncProfile(context: Context, callback: ((flag: Int) -> Unit)? = null) {
        MozoService.getInstance().fetchProfile(context) { data, _ ->
            data ?: return@fetchProfile

            GlobalScope.launch {
                /* save User info first */
                mozoDB.userInfo().save(UserInfo(
                        userId = data.userId
                ))
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

    private fun doRefreshToken() {
        try {
            mAuthService.performTokenRequest(
                    authStateManager.current.createTokenRefreshRequest(),
                    authStateManager.current.clientAuthentication
            ) { response, ex ->
                authStateManager.updateAfterTokenResponse(response, ex)
                response?.run {
                    "Refresh token successful: $accessToken".logAsError()
                }
                ex?.run {
                    "Refresh token failed: $message".logAsError()
                }
            }
        } catch (ex: Exception) {
            "Fail to refresh token: ${ex.message}".logAsError("MozoAuth")
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