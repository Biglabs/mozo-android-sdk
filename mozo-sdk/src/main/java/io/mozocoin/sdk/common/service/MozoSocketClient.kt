package io.mozocoin.sdk.common.service

import android.os.Build
import com.google.gson.Gson
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoNotification
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.model.BroadcastData
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.logAsInfo
import io.mozocoin.sdk.utils.md5
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*
import javax.net.ssl.SSLParameters
import kotlin.math.pow

internal class MozoSocketClient(uri: URI, header: Map<String, String>) : WebSocketClient(uri, header) {

    override fun onOpen(serverHandshake: ServerHandshake?) {
        "open [status: ${serverHandshake?.httpStatus}, url: $uri]".logAsInfo(TAG)
        stopRetryConnect()
    }

    override fun onMessage(s: String?) {
        "message $s".logAsInfo(TAG)


        s?.run {
            if (equals("1|X", ignoreCase = true)) {
                sendPing()
            } else {
                val messages = split("|")
                if (messages.size > 1) {
                    val message = try {
                        Gson().fromJson(messages[1], BroadcastData::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    message?.getData()?.let { broadcast ->
                        broadcast.event ?: return@let

                        /* Save notification to local storage */
                        // MozoNotification.save(broadcast)

                        when (broadcast.event.toLowerCase(Locale.getDefault())) {
                            /* Reload balance */
                            Constant.NOTIFY_EVENT_AIRDROP_INVITE -> {
                                MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
                            }
                            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                                MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
                                MozoWallet.getInstance().invokeBalanceChanged()
                            }
                            Constant.NOTIFY_EVENT_ADDRESS_BOOK_CHANGED -> {
                                MozoSDK.getInstance().contactViewModel.fetchUser(MozoSDK.getInstance().context)
                            }
                            Constant.NOTIFY_EVENT_STORE_BOOK_ADDED -> {
                                MozoSDK.getInstance().contactViewModel.appendStoreContact(broadcast.contact)
                            }
                            Constant.NOTIFY_EVENT_CONVERT -> {
                                EventBus.getDefault().post(MessageEvent.ConvertOnChain())
                            }
                            Constant.NOTIFY_EVENT_PROFILE_CHANGED -> {
                                MozoAuth.getInstance().syncProfile(MozoSDK.getInstance().context)
                            }
                        }

                        /* Do show notification on system tray */
                        if (MozoNotification.shouldShowNotification(broadcast.event)) {
                            MozoNotification.getInstance().showNotification(broadcast)
                        } else if (!MozoSDK.shouldShowNotification) {
                            "Notification received but not be shown".logAsInfo(TAG)
                        }
                    }
                }
            }
        }

        if (instance == null) {
            try {
                closeBlocking()
                closeConnection(-1, "proactive disconnect")
            } finally {
                doRetryConnect()
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        "close [code: $code, reason: $reason]".logAsInfo(TAG)
        if (!isDoDisconnect) {
            disconnect()
            doRetryConnect()
        }
        isDoDisconnect = false
    }

    override fun onError(e: Exception?) {
        "error [message: ${e?.message}]".logAsInfo(TAG)
        disconnect()
        doRetryConnect()
    }

    override fun onSetSSLParameters(sslParameters: SSLParameters?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            super.onSetSSLParameters(sslParameters)
        }
    }

    companion object {
        private const val TAG = "Socket"

        @Volatile
        private var instance: MozoSocketClient? = null

        private val sessionUUID = UUID.randomUUID()

        @Volatile
        private var initializeJob: Job? = null

        @Volatile
        private var retryConnectJob: Job? = null

        @Volatile
        private var retryConnectTime = Constant.SOCKET_RETRY_START_TIME

        private var isDoDisconnect = false

        @Synchronized
        fun connect() {
            MozoTokenService.newInstance().checkSession(MozoSDK.getInstance().context, { isExpired ->
                "Check session result: isExpired = $isExpired".logAsInfo(TAG)
                val accessToken = MozoAuth.getInstance().getAccessToken()
                if (isExpired || accessToken.isNullOrEmpty()) {
                    disconnect()
                    return@checkSession
                }

                initializeJob?.cancel()
                initializeJob = GlobalScope.launch {
                    delay(500)

                    disconnect().join()

                    val channel = if (MozoSDK.isRetailerApp) Constant.SOCKET_CHANNEL_RETAILER else Constant.SOCKET_CHANNEL_SHOPPER
                    val userId = MozoSDK.getInstance().profileViewModel.getProfile()?.userId
                    val uuid = StringBuilder()
                            .append(sessionUUID)
                            .append("-")
                            .append(userId)
                            .append("-")
                            .append(channel).toString().md5()

                    instance = MozoSocketClient(
                            URI("wss://${Support.domainSocket()}/websocket/user/$uuid/$channel"),
                            mutableMapOf(
                                    "Authorization" to "bearer $accessToken",
                                    "Content-Type" to "application/json",
                                    "X-atmo-protocol" to "true",
                                    "X-Atmosphere-Framework" to "2.3.3-javascript",
                                    "X-Atmosphere-tracking-id" to "0",
                                    "X-Atmosphere-Transport" to "websocket"
                            )
                    )
                    try {
                        if (instance?.isOpen == false)
                            instance?.connectBlocking()
                    } catch (ignore: Exception) {
                        doRetryConnect()
                    }
                }
            })
        }

        @JvmStatic
        fun disconnect() = GlobalScope.launch {
            try {
                isDoDisconnect = true
                instance?.closeBlocking()
                instance?.closeConnection(-1, "proactive disconnect")
            } finally {
                instance = null
            }
        }

        private fun doRetryConnect() {
            "start retry connect".logAsInfo(TAG)
            retryConnectJob?.cancel()
            retryConnectJob = GlobalScope.launch {
                delay(retryConnectTime)
                "retry connect time: $retryConnectTime".logAsInfo(TAG)
                retryConnectTime *= 2
                if (retryConnectTime > Constant.SOCKET_RETRY_START_TIME * 2.0.pow(8.0)) {
                    retryConnectTime = Constant.SOCKET_RETRY_START_TIME
                }

                if (ConnectionService.isNetworkAvailable) connect()
                else doRetryConnect()
            }
        }

        private fun stopRetryConnect() {
            retryConnectTime = Constant.SOCKET_RETRY_START_TIME
            retryConnectJob?.cancel()
            retryConnectJob = null
            "stop retry connect".logAsInfo(TAG)
        }
    }

}