package com.biglabs.mozo.sdk.auth

import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.Models.AnonymousUserInfo
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.services.WalletService
import com.biglabs.mozo.sdk.utils.AuthStateManager
import com.biglabs.mozo.sdk.utils.logAsError
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*

@Suppress("RedundantSuspendModifier", "unused")
class MozoAuth private constructor() {

    private val mozoDB: MozoDatabase by lazy { MozoDatabase.getInstance(MozoSDK.context!!) }
    private val mozoService: MozoService by lazy { MozoService.getInstance(MozoSDK.context!!) }
    private val walletService: WalletService by lazy { WalletService.getInstance() }

    private val authStateManager: AuthStateManager by lazy { AuthStateManager.getInstance(MozoSDK.context!!) }
    private val mAuthService: AuthorizationService by lazy { AuthorizationService(MozoSDK.context!!) }

    private var mAuthListener: AuthenticationListener? = null

    init {
        launch {
            val isSignedIn = isSignedIn()
            if (!isSignedIn) {
                val anonymousUser = initAnonymousUser()
                anonymousUser.toString().logAsError()
                // TODO authentication with anonymousUser
            } else {
                val response = mozoService.fetchProfile().await()
//                if (response.code() == 401) {
//                    signOut()
//                    return@launch
//                } else {
                response?.run {
                    mozoDB.profile().save(this)
                }
                if (authStateManager.current.needsTokenRefresh) {
                    doRefreshToken()
                }
//                }
            }

            launch(UI) {
                mAuthListener?.onChanged(isSignedIn)
            }
        }
    }

    fun signIn() {
        walletService.clear()
        MozoSDK.context?.run {
            if (!EventBus.getDefault().isRegistered(this@MozoAuth)) {
                EventBus.getDefault().register(this@MozoAuth)
            }
            MozoAuthActivity.signIn(this)
            return
        }
    }

    fun isSignedIn() = authStateManager.current.isAuthorized

    fun isSignUpCompleted() = authStateManager.current.isAuthorized && walletService.isHasWallet()

    fun signOut() {
        walletService.clear()
        MozoSDK.context?.let {
            MozoAuthActivity.signOut(it) {

                mAuthListener?.onChanged(false)

                launch {
                    mozoDB.userInfo().delete()

                    val currentState = authStateManager.current
                    val clearedState = AuthState(currentState.authorizationServiceConfiguration!!)
                    if (currentState.lastRegistrationResponse != null) {
                        clearedState.update(currentState.lastRegistrationResponse)
                    }
                    authStateManager.replace(clearedState)
                }
            }
        }
    }

    fun setAuthenticationListener(listener: AuthenticationListener) {
        this.mAuthListener = listener
    }

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

        /* notify for caller */
        mAuthListener?.onChanged(auth.isSignedIn)
    }

    internal fun syncProfile() = async {
        val response = mozoService.fetchProfile().await()
        if (response != null) {

            /* save User info first */
            mozoDB.userInfo().save(Models.UserInfo(
                    userId = response.userId
            ))

            /* update local profile to match with server profile */
            mozoDB.profile().save(response)
        } else {
            // TODO handle fetch profile error
        }
        "syncProfile OK".logAsError()
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