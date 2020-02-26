package io.mozocoin.sdk.utils.customtabs

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.*


internal class CustomTabsHelper {

    private var customTabsSession: CustomTabsSession? = null
    private var client: CustomTabsClient? = null
    private var connection: CustomTabsServiceConnection? = null
    private var connectionCallback: ConnectionCallback? = null

    /**
     * Creates or retrieves an exiting CustomTabsSession
     *
     * @return a CustomTabsSession
     */
    val session: CustomTabsSession?
        get() {
            if (client == null) {
                customTabsSession = null
            } else {
                customTabsSession = client!!.newSession(null)
            }
            return customTabsSession
        }

    /**
     * Unbinds the Activity from the Custom Tabs Service
     *
     * @param activity the activity that is connected to the service
     */
    fun unbindCustomTabsService(activity: Activity) {
        activity.unbindService(connection ?: return)
        client = null
        customTabsSession = null
    }

    /**
     * Register a Callback to be called when connected or disconnected from the Custom Tabs Service
     */
    fun setConnectionCallback(connectionCallback: ConnectionCallback) {
        this.connectionCallback = connectionCallback
    }

    /**
     * Binds the Activity to the Custom Tabs Service
     *
     * @param activity the activity to be bound to the service
     */
    fun bindCustomTabsService(activity: Activity) {
        if (client != null) {
            return
        }

        val packageName = CustomTabsPackageHelper.getPackageNameToUse(activity) ?: return
        connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                this@CustomTabsHelper.client = client
                this@CustomTabsHelper.client!!.warmup(0L)
                if (connectionCallback != null) {
                    connectionCallback!!.onCustomTabsConnected()
                }
                //Initialize a session as soon as possible.
                session
            }

            override fun onServiceDisconnected(name: ComponentName) {
                client = null
                if (connectionCallback != null) {
                    connectionCallback!!.onCustomTabsDisconnected()
                }
            }

            override fun onBindingDied(name: ComponentName) {
                client = null
                if (connectionCallback != null) {
                    connectionCallback!!.onCustomTabsDisconnected()
                }
            }
        }
        CustomTabsClient.bindCustomTabsService(activity, packageName, connection!!)
    }

    fun mayLaunchUrl(uri: Uri, extras: Bundle, otherLikelyBundles: List<Bundle>): Boolean {
        if (client == null) {
            return false
        }
        val session = session
        return session != null && session.mayLaunchUrl(uri, extras, otherLikelyBundles)
    }

    /**
     * A Callback for when the service is connected or disconnected. Use those callbacks to
     * handle UI changes when the service is connected or disconnected
     */
    interface ConnectionCallback {
        /**
         * Called when the service is connected
         */
        fun onCustomTabsConnected()

        /**
         * Called when the service is disconnected
         */
        fun onCustomTabsDisconnected()
    }

    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available
     */
    interface CustomTabFallback {
        /**
         * @param context The Activity that wants to open the Uri
         * @param uri     The uri to be opened by the fallback
         */
        fun openUri(context: Context, uri: Uri)
    }

    companion object {
        private const val EXTRA_CUSTOM_TABS_KEEP_ALIVE = "android.support.customtabs.extra.KEEP_ALIVE"

        /**
         * Opens the URL on a Custom Tab if possible. Otherwise fallsback to opening it on a WebView
         *
         * @param context          The host activity
         * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
         * @param uri              the Uri to be opened
         * @param fallback         a CustomTabFallback to be used if Custom Tabs is not available
         */
        fun openCustomTab(context: Context,
                          customTabsIntent: CustomTabsIntent,
                          uri: Uri,
                          fallback: CustomTabFallback?) {
            val packageName = CustomTabsPackageHelper.getPackageNameToUse(context)

            //If we cant find a package name, it means there's no browser that supports
            //Chrome Custom Tabs installed. So, we fallback to the web-view
            if (packageName == null) {
                fallback?.openUri(context, uri)
            } else {
                customTabsIntent.intent
                        .putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
                        .setPackage(packageName)
                customTabsIntent.launchUrl(context, uri)
            }
        }

        fun addKeepAliveExtra(context: Context, intent: Intent) {
            @Suppress("SpellCheckingInspection")
            val keepAliveIntent = Intent().setClassName(
                    context.packageName,
                    KeepAliveService::class.java.canonicalName
                            ?: "io.mozocoin.sdk.utils.customtabs.KeepAliveService"
            )
            intent.putExtra(EXTRA_CUSTOM_TABS_KEEP_ALIVE, keepAliveIntent)
        }
    }
}