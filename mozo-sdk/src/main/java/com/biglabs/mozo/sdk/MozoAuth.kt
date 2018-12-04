package com.biglabs.mozo.sdk

import android.content.Context
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.authentication.AuthenticationListener
import com.biglabs.mozo.sdk.authentication.MozoAuthActivity
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.Models.AnonymousUserInfo
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.core.MozoSocketClient
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

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

    fun signIn() {
        walletService.clear()
        if (!EventBus.getDefault().isRegistered(this@MozoAuth)) {
            EventBus.getDefault().register(this@MozoAuth)
        }
        MozoAuthActivity.signIn(MozoSDK.getInstance().context)
        return
    }

    fun isSignedIn() = authStateManager.current.isAuthorized

    fun isSignUpCompleted() = authStateManager.current.isAuthorized && walletService.isHasWallet()

    fun signOut() {
        walletService.clear()
        MozoAuthActivity.signOut(MozoSDK.getInstance().context) {

            onAuthorizeChanged(MessageEvent.Auth(false))

            GlobalScope.launch {
                mozoDB.userInfo().delete()

                authStateManager.clearSession()
            }
        }
    }

    fun setAuthenticationListener(listener: AuthenticationListener) {
        this.mAuthListener = listener
    }

    fun getAccessToken() = authStateManager.current.accessToken

    private suspend fun initAnonymousUser(): AnonymousUserInfo {
        var anonymousUser = mozoDB.anonymousUserInfo().get()
        if (anonymousUser == null) {
            val userId = UUID.randomUUID().toString()
            anonymousUser = AnonymousUserInfo(userId = userId)

            mozoDB.anonymousUserInfo().save(anonymousUser)
        }

        return anonymousUser
    }

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

    internal fun syncProfile(context: Context, retryCallback: () -> Unit) = GlobalScope.async {
        val response = MozoService.getInstance(context).fetchProfile(retryCallback).await()
        if (response != null) {

            /* save User info first */
            mozoDB.userInfo().save(Models.UserInfo(
                    userId = response.userId
            ))

            /* update local profile to match with server profile */
            mozoDB.profile().save(response)
            launch(Dispatchers.Main) {
                MozoSDK.getInstance().profileViewModel.updateProfile(response)
            }
        } else {
            // TODO handle fetch profile error
        }
        return@async response
    }

    private fun doRefreshToken() {
        try {
            mAuthService.performTokenRequest(
                    authStateManager.current.createTokenRefreshRequest(),
                    authStateManager.current.clientAuthentication) { response, ex ->
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