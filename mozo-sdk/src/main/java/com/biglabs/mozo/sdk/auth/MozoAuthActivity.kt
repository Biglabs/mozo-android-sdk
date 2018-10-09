package com.biglabs.mozo.sdk.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.FragmentActivity
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.services.WalletService
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.AuthStateManager
import com.biglabs.mozo.sdk.utils.logAsError
import com.biglabs.mozo.sdk.utils.setMatchParent
import com.biglabs.mozo.sdk.utils.string
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import net.openid.appauth.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

internal class MozoAuthActivity : FragmentActivity() {

    private var mAuthService: AuthorizationService? = null
    private var mAuthStateManager: AuthStateManager? = null

    private val mAuthRequest = AtomicReference<AuthorizationRequest>()
    private val mAuthIntent = AtomicReference<CustomTabsIntent>()
    private var mAuthIntentLatch = CountDownLatch(1)

    private var modeSignIn = true
    private var signOutConfiguration: AuthorizationServiceConfiguration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_loading)
        setMatchParent()

        modeSignIn = intent.getBooleanExtra(FLAG_MODE_SIGN_IN, modeSignIn)

        mAuthStateManager = AuthStateManager.getInstance(this)
        if (modeSignIn && mAuthStateManager!!.current.isAuthorized) {
            handleResult()
            return
        }

        initializeAppAuth()
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuthService?.dispose()
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    private fun initializeAppAuth() = async {
        mAuthService?.dispose()
        mAuthService = AuthorizationService(this@MozoAuthActivity)
        mAuthRequest.set(null)
        mAuthIntent.set(null)

        if (!modeSignIn) {
            signOutConfiguration = AuthorizationServiceConfiguration(
                    Uri.parse(string(R.string.auth_logout_uri)),
                    Uri.parse(string(R.string.auth_logout_uri)),
                    null
            )
            initializeAuthRequest()
            return@async
        }

        if (mAuthStateManager!!.current.authorizationServiceConfiguration != null) {
            initializeAuthRequest()
            return@async
        }

        mAuthStateManager!!.replace(AuthState(
                AuthorizationServiceConfiguration(
                        Uri.parse(string(R.string.auth_end_point_authorization)),
                        Uri.parse(string(R.string.auth_end_point_token))
                )
        ))
        initializeAuthRequest()
    }

    private fun initializeAuthRequest() {
        createAuthRequest()
        warmUpBrowser()
        doAuth()
    }

    private fun createAuthRequest() {
        val redirectUrl = string(R.string.auth_redirect_uri, R.string.auth_redirect_scheme)
        val authRequestBuilder = AuthorizationRequest.Builder(
                if (modeSignIn)
                    mAuthStateManager!!.current.authorizationServiceConfiguration!!
                else
                    signOutConfiguration!!,
                string(R.string.auth_client_id),
                ResponseTypeValues.CODE,
                Uri.parse(redirectUrl))
                .setPrompt("login")
                .setScope("openid profile phone")

        mAuthRequest.set(authRequestBuilder.build())
    }

    private fun warmUpBrowser() {
        mAuthIntentLatch = CountDownLatch(1)
        val intentBuilder = mAuthService!!.createCustomTabsIntentBuilder(mAuthRequest.get().toUri())
        val customTabs = intentBuilder
                .setShowTitle(true)
                .setInstantAppsEnabled(false)
                .build()

        val extras = Bundle()
        extras.putBinder(CustomTabsIntent.EXTRA_SESSION, null)
        extras.putBoolean(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false)
        extras.putParcelableArrayList(CustomTabsIntent.EXTRA_MENU_ITEMS, null)
        customTabs.intent.putExtras(extras)

        mAuthIntent.set(customTabs)
        mAuthIntentLatch.countDown()
    }

    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    private fun doAuth() {
        try {
            mAuthIntentLatch.await()
        } catch (ex: InterruptedException) {
        }

        val intent = mAuthService!!.getAuthorizationRequestIntent(mAuthRequest.get(), mAuthIntent.get())
        launch(UI) {
            startActivityForResult(intent, KEY_DO_AUTHENTICATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == KEY_DO_AUTHENTICATION && modeSignIn -> {
                if (data == null) return
                val response = AuthorizationResponse.fromIntent(data)
                val ex = AuthorizationException.fromIntent(data)

                if (response != null || ex != null) {
                    mAuthStateManager!!.updateAfterAuthorization(response, ex)
                }

                when {
                    response?.authorizationCode != null -> {
                        // authorization code exchange is required
                        mAuthStateManager!!.updateAfterAuthorization(response, ex)
                        exchangeAuthorizationCode(response)
                    }
                    else -> {
                        finishAndRemoveTask()
                    }
                }
            }
            requestCode == KEY_DO_ENTER_PIN -> {
                EventBus.getDefault().post(MessageEvent.Auth(modeSignIn))
                finishAndRemoveTask()
            }
            !modeSignIn -> {
                handleResult()
            }
            else -> handleResult(Exception("No Result"))
        }
    }

    private fun exchangeAuthorizationCode(response: AuthorizationResponse) {
        performTokenRequest(response.createTokenExchangeRequest(), AuthorizationService.TokenResponseCallback { tokenResponse, authException ->
            mAuthStateManager!!.updateAfterTokenResponse(tokenResponse, authException)
            handleResult(exception = authException)
        })
    }

    private fun performTokenRequest(request: TokenRequest, callback: AuthorizationService.TokenResponseCallback) {
        val clientAuthentication: ClientAuthentication
        try {
            clientAuthentication = mAuthStateManager!!.current.clientAuthentication
        } catch (ex: ClientAuthentication.UnsupportedAuthenticationMethod) {
            handleResult(exception = ex)
            return
        }

        mAuthService!!.performTokenRequest(request, clientAuthentication, callback)
    }

    private fun handleResult(exception: Exception? = null) = async {
        if (modeSignIn) {
            if (exception == null) {
                MozoAuth.getInstance().syncProfile().await()
                // TODO handle sync profile failed
                val flag = WalletService.getInstance().initWallet().await()
                SecurityActivity.start(this@MozoAuthActivity, flag, KEY_DO_ENTER_PIN)
            } else {
                //TODO handle authentication error
                exception.message?.logAsError("authentication")
            }
            return@async
        } else
            signOutCallBack?.invoke()

        launch(UI) {
            EventBus.getDefault().post(MessageEvent.Auth(modeSignIn, exception))
            finishAndRemoveTask()
        }
    }

    companion object {
        private const val FLAG_MODE_SIGN_IN = "FLAG_MODE_SIGN_IN"
        private const val KEY_DO_AUTHENTICATION = 100
        private const val KEY_DO_ENTER_PIN = 200

        private var signOutCallBack: (() -> Unit)? = null

        private fun start(context: Context, signIn: Boolean = true) {
            Intent(context, MozoAuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(FLAG_MODE_SIGN_IN, signIn)
                context.startActivity(this)
            }
        }

        fun signIn(context: Context) {
            start(context)
        }

        fun signOut(context: Context, callback: (() -> Unit)? = null) {
            signOutCallBack = callback
            start(context, signIn = false)
        }
    }
}
