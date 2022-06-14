package io.mozocoin.sdk.authentication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.FragmentActivity
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.service.MozoTokenService
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.UserCancelException
import io.mozocoin.sdk.utils.logAsError
import io.mozocoin.sdk.utils.setMatchParent
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.openid.appauth.*
import net.openid.appauth.browser.BrowserBlacklist
import net.openid.appauth.browser.Browsers
import net.openid.appauth.browser.VersionRange
import net.openid.appauth.browser.VersionedBrowserMatcher
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

internal class MozoAuthActivity : FragmentActivity() {

    private var mAuthService: AuthorizationService? = null
    private val mAuthStateManager: AuthStateManager by lazy {
        AuthStateManager.getInstance(applicationContext)
    }

    private val mAuthRequest = AtomicReference<AuthorizationRequest>()
    private val mAuthIntent = AtomicReference<CustomTabsIntent>()
    private var mAuthIntentLatch = CountDownLatch(1)

    private var modeSignIn = true
    private var isSignOutBeforeIn = false
    private var isSignOutWhenError = false

    private var handleJob: Job? = null
    private var isAuthInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_loading)
        setMatchParent()

        modeSignIn = intent.getBooleanExtra(FLAG_MODE_SIGN_IN, modeSignIn)

        if (modeSignIn && mAuthStateManager.current.isAuthorized) {
            handleResult()
            return
        }

        initializeAppAuth()
    }

    override fun onDestroy() {
        mAuthService?.dispose()
        mAuthService = null
        authenticationInProgress = false
        super.onDestroy()

        if (isAuthInProgress) {
            isAuthInProgress = false
            EventBus.getDefault().post(MessageEvent.Auth(UserCancelException()))
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_anim, R.anim.fade_out_short)
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    private fun initializeAppAuth() = MozoSDK.scope.launch {
        mAuthRequest.set(null)
        mAuthIntent.set(null)

        val signInEndPoint =
            getString(R.string.auth_end_point_authorization, Support.domainAuth()).toUri()
        val tokenEndpoint = getString(R.string.auth_end_point_token, Support.domainAuth()).toUri()
        mAuthStateManager.replace(
            AuthState(
                AuthorizationServiceConfiguration(
                    signInEndPoint,
                    tokenEndpoint
                )
            )
        )

        initializeAuthRequest()
    }

    private fun initializeAuthRequest() {
        val appScheme = getString(
            R.string.auth_redirect_uri,
            "com.biglabs.mozosdk.${applicationInfo.packageName}"
        )
        val clientId = getString(
            when {
                MozoSDK.isInternalApps -> R.string.auth_client_id_operation
                MozoSDK.isRetailerApp -> R.string.auth_client_id_retailer
                else -> R.string.auth_client_id_shopper
            }
        )

        if (mAuthStateManager.current.authorizationServiceConfiguration == null) {
            cancelAuth()
            return
        }

        val locale = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.ENGLISH
        val authRequestBuilder = if (modeSignIn) {
            AuthorizationRequest.Builder(
                mAuthStateManager.current.authorizationServiceConfiguration!!,
                clientId,
                ResponseTypeValues.CODE,
                Uri.parse(appScheme)
            )
                .setPrompt("consent")
                .setScope("openid profile phone")
                .setAdditionalParameters(
                    mutableMapOf(
                        "kc_locale" to locale.toLanguageTag(),
                        "application_type" to "native"
                    )
                )

        } else /* SIGN OUT */ {
            val signInRequest = AuthorizationRequest.Builder(
                mAuthStateManager.current.authorizationServiceConfiguration!!,
                clientId,
                ResponseTypeValues.CODE,
                Uri.parse(appScheme)
            )
                .setPrompt("consent")
                .setScope("openid profile phone")
                .setAdditionalParameters(
                    mutableMapOf(
                        "kc_locale" to locale.toLanguageTag(),
                        "application_type" to "native"
                    )
                )
                .build()

            val signOutEndpoint = getString(R.string.auth_logout_uri, Support.domainAuth()).toUri()
            val tokenEndpoint =
                getString(R.string.auth_end_point_token, Support.domainAuth()).toUri()
            AuthorizationRequest.Builder(
                AuthorizationServiceConfiguration(
                    signOutEndpoint,
                    tokenEndpoint
                ),
                clientId,
                ResponseTypeValues.CODE,
                signInRequest.toUri()
            )
                .setState(signInRequest.state)
                .setCodeVerifier(
                    signInRequest.codeVerifier,
                    signInRequest.codeVerifierChallenge,
                    signInRequest.codeVerifierChallengeMethod
                )
                .setNonce(signInRequest.nonce)
                .setPrompt("consent")
                .setScope("openid profile phone")
                .setAdditionalParameters(
                    mutableMapOf(
                        "kc_locale" to locale.toLanguageTag(),
                        "application_type" to "native"
                    )
                )
        }

        mAuthRequest.set(authRequestBuilder.build())

        warmUpBrowser()
        doAuth()
    }

    private fun getAuthService(): AuthorizationService {
        return if (mAuthService == null || mAuthService?.isDisposed == true) {
            val appAuthConfig = AppAuthConfiguration.Builder()
                .setBrowserMatcher(
                    BrowserBlacklist(
                        VersionedBrowserMatcher(
                            Browsers.SBrowser.PACKAGE_NAME,
                            Browsers.SBrowser.SIGNATURE_SET,
                            true, // when this browser is used via a custom tab
                            VersionRange.atMost("5.3")
                        ),
                        VersionedBrowserMatcher(
                            Browsers.Chrome.PACKAGE_NAME,
                            Browsers.Chrome.SIGNATURE_SET,
                            true, // when this browser is used via a custom tab
                            VersionRange.atMost("53.0.2785.124")
                        )
                    )
                )
                .build()
            AuthorizationService(MozoSDK.getInstance().context, appAuthConfig)

        } else mAuthService!!
    }

    private fun warmUpBrowser() {
        mAuthIntentLatch = CountDownLatch(1)
        val customTabs = getAuthService().createCustomTabsIntentBuilder(mAuthRequest.get().toUri())
            .setShowTitle(true)
            .setInstantAppsEnabled(false)
            .build()

        val extras = Bundle()
        extras.putBinder(CustomTabsIntent.EXTRA_SESSION, null)
        extras.putInt(CustomTabsIntent.EXTRA_SHARE_STATE, CustomTabsIntent.SHARE_STATE_OFF)
        extras.putParcelableArrayList(CustomTabsIntent.EXTRA_MENU_ITEMS, null)
        customTabs.intent.putExtras(extras)

        mAuthIntent.set(customTabs)
        mAuthIntentLatch.countDown()
    }

    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    private fun doAuth() = MainScope().launch {
        try {
            mAuthIntentLatch.await()
        } catch (ex: Exception) {
            finishAuth(ex)
        }

        isAuthInProgress = true
        val intent = getAuthService().getAuthorizationRequestIntent(
            mAuthRequest.get(),
            mAuthIntent.get()
        )
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        startActivityForResult(intent, KEY_DO_AUTHENTICATION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        isAuthInProgress = isAuthInProgress && resultCode == RESULT_CANCELED
        when {
            requestCode == KEY_DO_AUTHENTICATION -> {
                if (isSignOutWhenError) {
                    mAuthStateManager.clearSession()
                    finishAuth()
                    return
                }
                if (isSignOutBeforeIn) {
                    isSignOutBeforeIn = false
                    initializeAuthRequest()
                    return
                }
                if (data == null) return
                val dataJson = data.getStringExtra(AuthorizationResponse.EXTRA_RESPONSE)

                if (!modeSignIn && !dataJson.isNullOrEmpty()) {
                    val appScheme = getString(
                        R.string.auth_redirect_uri,
                        "com.biglabs.mozosdk.${applicationInfo.packageName}"
                    )

                    val dataJsonObj = JSONObject(dataJson)
                    (dataJsonObj.get("request") as JSONObject).put("redirectUri", appScheme)
                    data.putExtra(AuthorizationResponse.EXTRA_RESPONSE, dataJsonObj.toString())
                }

                val response = AuthorizationResponse.fromIntent(data)
                val ex = AuthorizationException.fromIntent(data)

                if (response != null || ex != null) {
                    mAuthStateManager.updateAfterAuthorization(response, ex)
                }

                when {
                    response?.authorizationCode != null -> {
                        // authorization code exchange is required
                        mAuthStateManager.updateAfterAuthorization(response, ex)
                        exchangeAuthorizationCode(response)
                    }
                    ex != null -> handleResult(ex)
                    resultCode == RESULT_CANCELED -> cancelAuth()
                    else -> {
                        finish()
                    }
                }
            }
            !modeSignIn -> {
                handleResult()
            }
            else -> handleResult(Exception("No Result"))
        }
    }

    private fun exchangeAuthorizationCode(response: AuthorizationResponse) {
        val clientAuthentication: ClientAuthentication
        try {
            clientAuthentication = mAuthStateManager.current.clientAuthentication
        } catch (ex: ClientAuthentication.UnsupportedAuthenticationMethod) {
            handleResult(exception = ex)
            return
        }

        getAuthService().performTokenRequest(
            response.createTokenExchangeRequest(),
            clientAuthentication
        ) { tokenResponse, authException ->
            mAuthStateManager.updateAfterTokenResponse(tokenResponse, authException)
            MozoTokenService.instance().reportToken()
            handleResult(exception = authException)
        }
    }

    private fun handleResult(exception: Exception? = null) {
        handleJob?.cancel()
        handleJob = MozoSDK.scope.launch {
            if (exception == null) {
                MozoAuth.getInstance().syncProfile(this@MozoAuthActivity) {
                    if (it) finishAuth(null)
                    else finish()
                }
                return@launch
            }

            exception.message?.logAsError("authentication")
            finishAuth(exception)
        }
    }

    private fun finishAuth(exception: Exception? = null) = MainScope().launch {
        if (
            exception is AuthorizationException
            && exception.code == AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR.code
            && (exception.cause?.message?.contains("Issued at time", ignoreCase = true) == true
                    || exception.cause?.message?.contains(
                "ID Token expired",
                ignoreCase = true
            ) == true)
        ) {
            AlertDialog.Builder(this@MozoAuthActivity, R.style.PermissionTheme)
                .setTitle(R.string.mozo_dialog_error_system_time_msg)
                .setMessage(R.string.mozo_dialog_error_system_time_msg_sub)
                .setPositiveButton(R.string.mozo_settings_title) { dialog, _ ->
                    dialog.dismiss()
                    this@MozoAuthActivity.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                }
                .setOnDismissListener {
                    cancelAuth()
                }
                .create()
                .show()
        } else {
            EventBus.getDefault().post(MessageEvent.Auth(exception))
            finish()
            authenticationInProgress = false
        }
    }

    private fun cancelAuth() {
        finishAuth(UserCancelException())
    }

    companion object {
        private const val FLAG_MODE_SIGN_IN = "FLAG_MODE_SIGN_IN"
        private const val KEY_DO_AUTHENTICATION = 100

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

        fun signOut(context: Context) {
            authenticationInProgress = false
            start(context, signIn = false)
        }
    }
}
