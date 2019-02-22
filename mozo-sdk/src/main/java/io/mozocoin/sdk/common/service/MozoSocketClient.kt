package io.mozocoin.sdk.common.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import io.mozocoin.sdk.MozoNotification
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.authentication.AuthStateManager
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.BroadcastData
import io.mozocoin.sdk.common.model.BroadcastDataContent
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

internal class MozoSocketClient(uri: URI, header: Map<String, String>) : WebSocketClient(uri, header) {

    private val notificationManager: NotificationManager by lazy {
        MozoSDK.getInstance().context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        connect()
    }

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
                        MozoNotification.save(broadcast)

                        when (broadcast.event.toLowerCase()) {
                            /* Reload balance */
                            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                                MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
                            }
                            Constant.NOTIFY_EVENT_STORE_BOOK_ADDED -> {
                                MozoSDK.getInstance().contactViewModel.fetchStore(MozoSDK.getInstance().context)
                            }
                        }

                        /* Do show notification on system tray */
                        if (MozoNotification.shouldShowNotification(broadcast.event)) {
                            showNotification(broadcast)
                        }
                    }
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        "close [code: $code, reason: $reason]".logAsInfo(TAG)
        if (code != -1) {
            disconnect()
            doRetryConnect()
        }
    }

    override fun onError(e: Exception?) {
        "error [message: ${e?.message}]".logAsInfo(TAG)
        disconnect()
        doRetryConnect()
    }

    private fun showNotification(message: BroadcastDataContent) = GlobalScope.launch {
        MozoSDK.getInstance().notifyActivityClass ?: return@launch

        val context = MozoSDK.getInstance().context
        val notification = MozoNotification.prepareNotification(context, message)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !notificationChannelExists(message.event!!)) {
            createChannel(message.event).join()
        }

        val pendingIntent = PendingIntent.getActivity(
                context,
                MozoNotification.REQUEST_CODE,
                MozoNotification.prepareDataIntent(notification),
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, message.event!!)
                .setSmallIcon(R.drawable.ic_mozo_notification)
                .setLargeIcon(context.bitmap(notification.icon))
                .setColor(context.color(R.color.mozo_color_primary))
                .setContentTitle(notification.titleDisplay())
                .setContentText(notification.contentDisplay())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
        withContext(Dispatchers.Main) {
            NotificationManagerCompat
                    .from(context)
                    .notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(channel: String) = GlobalScope.launch(Dispatchers.Main) {
        var channelName = channel.split("_")[0]
        channelName = channelName.substring(0, 1).toUpperCase() + channelName.substring(1)

        val notificationChannel = NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.setShowBadge(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannelExists(channelId: String): Boolean = notificationManager.getNotificationChannel(channelId) != null

    companion object {
        private const val TAG = "Socket"

        @Volatile
        private var instance: MozoSocketClient? = null

        @Volatile
        private var retryConnectJob: Job? = null

        @Volatile
        private var retryConnectTime = Constant.SOCKET_RETRY_START_TIME

        @Synchronized
        fun connect(): MozoSocketClient {
            if (instance?.isOpen == true) {
                disconnect()
            }
            if (instance == null) {
                val accessToken = AuthStateManager.getInstance(MozoSDK.getInstance().context)
                        .current.accessToken ?: ""
                val channel = if (MozoSDK.isRetailerApp) Constant.SOCKET_CHANNEL_RETAILER else Constant.SOCKET_CHANNEL_SHOPPER
                val userId = MozoSDK.getInstance().profileViewModel.getProfile()?.userId
                val uuid = StringBuilder()
                        .append(UUID.randomUUID())
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
            }
            return instance!!
        }

        fun disconnect() {
            try {
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
                connect()
                retryConnectTime *= 2
                if (retryConnectTime > Constant.SOCKET_RETRY_START_TIME * Math.pow(2.0, 8.0)) {
                    retryConnectTime = Constant.SOCKET_RETRY_START_TIME
                }
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