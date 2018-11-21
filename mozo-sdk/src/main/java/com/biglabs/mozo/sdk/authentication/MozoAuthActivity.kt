package com.biglabs.mozo.sdk.authentication

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.biglabs.mozo.sdk.BuildConfig
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.MessageEvent
import com.biglabs.mozo.sdk.core.WalletService
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.ui.dialog.ErrorDialog
import com.biglabs.mozo.sdk.utils.logAsError
import com.biglabs.mozo.sdk.utils.setMatchParent
import com.biglabs.mozo.sdk.utils.string
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
    private fun initializeAppAuth() = GlobalScope.launch {
        mAuthService?.dispose()
        mAuthService = AuthorizationService(this@MozoAuthActivity)
        mAuthRequest.set(null)
        mAuthIntent.set(null)

        val logoutUrl = getString(R.string.auth_logout_uri, BuildConfig.DOMAIN_AUTH)
        signOutConfiguration = AuthorizationServiceConfiguration(
                logoutUrl.toUri(),
                logoutUrl.toUri(),
                null
        )
        if (mAuthStateManager!!.current.authorizationServiceConfiguration == null) {
            mAuthStateManager!!.replace(AuthState(
                    AuthorizationServiceConfiguration(
                            getString(R.string.auth_end_point_authorization, BuildConfig.DOMAIN_AUTH).toUri(),
                            getString(R.string.auth_end_point_token, BuildConfig.DOMAIN_AUTH).toUri()
                    )
            ))
        }

        if (modeSignIn) {
            doSignOutFirst()
            return@launch
        }
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
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        GlobalScope.launch(Dispatchers.Main) {
            startActivityForResult(intent, KEY_DO_AUTHENTICATION)
        }
    }

    private fun doSignOutFirst() {
        isSignOutBeforeIn = true
        val redirectUrl = getString(R.string.auth_redirect_uri, String.format(Locale.US, "com.biglabs.mozosdk.%s", applicationInfo.packageName))
        val authRequestBuilder = AuthorizationRequest.Builder(signOutConfiguration!!, string(R.string.auth_client_id), ResponseTypeValues.CODE, Uri.parse(redirectUrl))

        val authRequest = authRequestBuilder.build()
        val intentBuilder = mAuthService!!.createCustomTabsIntentBuilder(authRequest.toUri())
        val customTabs = intentBuilder.setShowTitle(true).setInstantAppsEnabled(false).build()

        val extras = Bundle()
        extras.putBinder(CustomTabsIntent.EXTRA_SESSION, null)
        extras.putBoolean(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false)
        extras.putParcelableArrayList(CustomTabsIntent.EXTRA_MENU_ITEMS, null)
        customTabs.intent.putExtras(extras)

        val intent = mAuthService!!.getAuthorizationRequestIntent(authRequest, customTabs)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        startActivityForResult(intent, KEY_DO_AUTHENTICATION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == KEY_DO_AUTHENTICATION && modeSignIn -> {
                if (isSignOutWhenError) {
                    mAuthStateManager?.clearSession()
                    EventBus.getDefault().post(MessageEvent.Auth(modeSignIn))
                    finish()
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
                    else -> {
                        finish()
                    }
                }
            }
            requestCode == KEY_DO_ENTER_PIN -> {
                EventBus.getDefault().post(MessageEvent.Auth(modeSignIn))
                finish()
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
                    val response = MozoAuth.getInstance()
                            .syncProfile(this@MozoAuthActivity) {
                                handleResult()
                            }
                            .await()
                    if (response == null) {
                        ErrorDialog.onCancel(DialogInterface.OnCancelListener {
                            isSignOutWhenError = true
                            doSignOutFirst()
                        })
                        return@launch
                    }

                    val flag = WalletService.getInstance().initWallet(this@MozoAuthActivity).await()
                    SecurityActivity.start(this@MozoAuthActivity, flag, KEY_DO_ENTER_PIN)
                } else {
                    //TODO handle authentication error
                    exception.message?.logAsError("authentication")
                }
                return@launch
            } else
                signOutCallBack?.invoke()

            launch(Dispatchers.Main) {
                EventBus.getDefault().post(MessageEvent.Auth(modeSignIn, exception))
                finish()
            }
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
