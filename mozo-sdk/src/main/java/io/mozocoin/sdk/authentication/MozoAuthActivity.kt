package io.mozocoin.sdk.authentication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.service.MozoTokenService
import io.mozocoin.sdk.databinding.ActivityAuthBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import net.openid.appauth.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.concurrent.atomic.AtomicReference

internal class MozoAuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var mAuthService: AuthorizationService? = null
    private val mAuthStateManager: AuthStateManager by lazy {
        AuthStateManager.getInstance(applicationContext)
    }
    private val mAppScheme: String by lazy {
        getString(
            R.string.auth_redirect_uri,
            "com.biglabs.mozosdk.${applicationInfo.packageName}"
        )
    }
    private val mClientId: String by lazy {
        getString(
            when {
                MozoSDK.isInternalApps -> R.string.auth_client_id_operation
                MozoSDK.isRetailerApp -> R.string.auth_client_id_retailer
                else -> R.string.auth_client_id_shopper
            }
        )
    }

    private val mAuthRequest = AtomicReference<AuthorizationRequest>()
    private val mAuthIntent = AtomicReference<CustomTabsIntent>()

    private var modeSignIn = true

    private var handleJob: Job? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonClose.click { cancelAuth() }
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportZoom(false)
            settings.userAgentString = Support.userAgent()
        }
        binding.buttonRefresh.click {
            binding.webView.reload()
        }

        modeSignIn = intent.getBooleanExtra(FLAG_MODE_SIGN_IN, modeSignIn)

        if (modeSignIn && MozoTokenService.instance().isAuthorized()) {
            handleResult()
            return
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onLoadResource(view: WebView?, url: String?) {
                if (url?.startsWith(mAppScheme) == true) {
                    handleAuthResult(url)
                    return
                }
                super.onLoadResource(view, url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.errorContainer.gone()
                binding.webView.visible()
                binding.progressIndicator.visible()
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.progressIndicator.gone()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                binding.webView.gone()

                if (request?.url.toString().startsWith(mAppScheme)) {
                    binding.progressIndicator.visible()
                    return
                }
                binding.errorContainer.visible()
            }
        }

        initializeAppAuth()
    }

    override fun onDestroy() {
        mAuthService?.dispose()
        mAuthService = null
        if (authenticationInProgress) {
            authenticationInProgress = false
            EventBus.getDefault().post(MessageEvent.Auth(UserCancelException()))
        }
        super.onDestroy()
    }

    override fun onBackPressed() = cancelAuth()

    override fun finish() {
        overridePendingTransition(R.anim.no_anim, R.anim.fade_out_short)
        super.finish()
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

        if (mAuthStateManager.current.authorizationServiceConfiguration == null) {
            cancelAuth()
            return@launch
        }

        val locale = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.ENGLISH
        val authRequestBuilder = if (modeSignIn) {
            AuthorizationRequest.Builder(
                mAuthStateManager.current.authorizationServiceConfiguration!!,
                mClientId,
                ResponseTypeValues.CODE,
                Uri.parse(mAppScheme)
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
                mClientId,
                ResponseTypeValues.CODE,
                Uri.parse(mAppScheme)
            )
                .setState(null)
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
                mClientId,
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

        val authRequest = authRequestBuilder.build()
        mAuthRequest.set(authRequest)

        withContext(Dispatchers.Main) {
            binding.webView.loadUrl(authRequest.toUri().toString())
        }
    }

    private fun handleAuthResult(data: String) {
        when {
            data.isNotEmpty() -> {
                val uri = Uri.parse(data)
                val code = uri.getQueryParameter("code")
                if (code.isNullOrEmpty()) {
                    handleResult(Exception("No Result"))
                    return
                }

                MozoTokenService.instance().requestToken(
                    code,
                    codeVerifier = mAuthRequest.get().codeVerifier ?: "",
                    redirectUri = mAppScheme
                ) { t, e ->
                    if (t == null) finishAuth(e)
                    else handleResult()
                }
            }
            !modeSignIn -> {
                handleResult()
            }
            else -> handleResult(Exception("No Result"))
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
            authenticationInProgress = false
            finish()
        }
    }

    private fun cancelAuth() {
        authenticationInProgress = false
        finishAuth(UserCancelException())
    }

    companion object {
        private const val FLAG_MODE_SIGN_IN = "FLAG_MODE_SIGN_IN"

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
