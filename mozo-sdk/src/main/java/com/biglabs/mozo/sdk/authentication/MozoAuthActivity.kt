package com.biglabs.mozo.sdk.authentication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.FragmentActivity
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.openid.appauth.*
import org.greenrobot.eventbus.EventBus
import java.util.*
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
    private var isSignOutBeforeIn = false
    private var isSignOutWhenError = false

    private var handleJob: Job? = null
    private var isAuthInProgress = false

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
        authenticationInProgress = false

        if (isAuthInProgress) {
            isAuthInProgress = false
            EventBus.getDefault().post(MessageEvent.Auth(modeSignIn, UserCancelException()))
        }
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    private fun initializeAppAuth() = GlobalScope.launch {
        mAuthService?.dispose()
        mAuthService = AuthorizationService(this@MozoAuthActivity)
        mAuthRequest.set(null)
        mAuthIntent.set(null)

        val logoutUrl = getString(R.string.auth_logout_uri, Support.domainAuth())
        signOutConfiguration = AuthorizationServiceConfiguration(
                logoutUrl.toUri(),
                logoutUrl.toUri(),
                null
        )
        mAuthStateManager!!.replace(AuthState(
                AuthorizationServiceConfiguration(
                        getString(R.string.auth_end_point_authorization, Support.domainAuth()).toUri(),
                        getString(R.string.auth_end_point_token, Support.domainAuth()).toUri()
                )
        ))

//        if (modeSignIn) {
//            doSignOutFirst()
//            return@launch
//        }
        initializeAuthRequest()
    }

    private fun initializeAuthRequest() {
        createAuthRequest()
        warmUpBrowser()
        doAuth()
    }

    private fun createAuthRequest() {
        val redirectUrl = getString(R.string.auth_redirect_uri, String.format(Locale.US, "com.biglabs.mozosdk.%s", applicationInfo.packageName))
        val authRequestBuilder = AuthorizationRequest.Builder(
                if (modeSignIn)
                    mAuthStateManager!!.current.authorizationServiceConfiguration!!
                else
                    signOutConfiguration!!,
                string(R.string.auth_client_id),
                ResponseTypeValues.CODE,
                Uri.parse(redirectUrl))
                .setPrompt("consent")
                .setScope("openid profile phone")

        val locale = ConfigurationCompat.getLocales(resources.configuration)[0]
        authRequestBuilder.setAdditionalParameters(
                mutableMapOf(
                        "kc_locale" to locale.language,
                        "application_type" to "native"
                )
        )
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
    private fun doAuth() = GlobalScope.launch(Dispatchers.Main) {
        try {
            mAuthIntentLatch.await()
        } catch (ex: InterruptedException) {
        }

        isAuthInProgress = true
        startActivityForResult(
                mAuthService!!.getAuthorizationRequestIntent(mAuthRequest.get(), mAuthIntent.get()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                },
                KEY_DO_AUTHENTICATION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        isAuthInProgress = false
        when {
            requestCode == KEY_DO_AUTHENTICATION && modeSignIn -> {
                if (isSignOutWhenError) {
                    mAuthStateManager?.clearSession()
                    finishAuth()
                    return
                }
                if (isSignOutBeforeIn) {
                    isSignOutBeforeIn = false
                    initializeAuthRequest()
                    return
                }
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
                    resultCode == RESULT_CANCELED -> finishAuth(UserCancelException())
                    else -> {
                        finish()
                    }
                }
            }
            requestCode == KEY_DO_ENTER_PIN -> {
                if (resultCode == RESULT_OK) {
                    finishAuth()
                } else {
                    modeSignIn = false
                    initializeAppAuth()
                }
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

    private fun handleResult(exception: Exception? = null) {
        handleJob?.cancel()
        handleJob = GlobalScope.launch {
            if (modeSignIn) {
                if (exception == null) {
                    MozoAuth.getInstance().syncProfile(this@MozoAuthActivity) {
                        if (it <= 0) {
                            // This user already logged in before, so bypass confirm PIN
                            finishAuth(exception)
                        } else
                            SecurityActivity.start(this@MozoAuthActivity, it, KEY_DO_ENTER_PIN)
                    }
                } else {
                    //TODO handle authentication error
                    exception.message?.logAsError("authentication")
                }
                return@launch
            } else
                signOutCallBack?.invoke()

            finishAuth(exception)
        }
    }

    private fun finishAuth(exception: Exception? = null) = GlobalScope.launch(Dispatchers.Main) {
        EventBus.getDefault().post(MessageEvent.Auth(modeSignIn, exception))
        finish()
    }

    companion object {
        private const val FLAG_MODE_SIGN_IN = "FLAG_MODE_SIGN_IN"
        private const val KEY_DO_AUTHENTICATION = 100
        private const val KEY_DO_ENTER_PIN = 200

        private var signOutCallBack: (() -> Unit)? = null
        @Volatile
        private var authenticationInProgress = false

        private fun start(context: Context, signIn: Boolean = true) {
            if (authenticationInProgress) return
            Intent(context, MozoAuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(FLAG_MODE_SIGN_IN, signIn)
                context.startActivity(this)
            }
            authenticationInProgress = true
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
