package com.biglabs.mozo.sdk.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.biglabs.mozo.sdk.MozoNotification
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.bitmap
import com.biglabs.mozo.sdk.utils.color
import com.biglabs.mozo.sdk.utils.logAsInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
                        Gson().fromJson(messages[1], Models.BroadcastData::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    message?.getData()?.run {
                        /* Save notification to local storage */
                        MozoNotification.save(this)

                        /* Reload balance */
                        if (event.equals(Constant.NOTIFY_EVENT_BALANCE_CHANGED, ignoreCase = true)) {
                            @Suppress("DeferredResultUnused")
                            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.getInstance().context)
                        }

                        /* Do show notification on system tray */
                        showNotification(this)
                    }
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        "close [code: $code, reason: $reason]".logAsInfo(TAG)
        instance = null
    }

    override fun onError(e: Exception?) {
        "error [message: ${e?.message}]".logAsInfo(TAG)
        disconnect()
    }

    private fun showNotification(message: Models.BroadcastDataContent) = GlobalScope.launch {
        MozoSDK.getInstance().notifyActivityClass ?: return@launch

        val context = MozoSDK.getInstance().context
        val notification = MozoNotification.prepareNotification(context, message)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !notificationChannelExists(message.event)) {
            createChannel(message.event).await()
        }
        val resultIntent = Intent(context, MozoSDK.getInstance().notifyActivityClass)
        // TODO put message data to intent
        val requestID = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(context, requestID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, message.event)
                .setSmallIcon(R.drawable.ic_mozo_notification)
                .setLargeIcon(context.bitmap(notification.icon))
                .setColor(context.color(R.color.mozo_color_primary))
                .setContentTitle(notification.titleDisplay())
                .setContentText(notification.contentDisplay())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
        launch(Dispatchers.Main) {
            NotificationManagerCompat
                    .from(context)
                    .notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(channel: String) = GlobalScope.async(Dispatchers.Main) {
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

        @Synchronized
        fun connect(context: Context): MozoSocketClient {
            if (instance == null) {
                val accessToken = AuthStateManager.getInstance(context).current.accessToken ?: ""
                val channel = if (MozoSDK.isRetailerApp) Constant.SOCKET_CHANNEL_RETAILER else Constant.SOCKET_CHANNEL_SHOPPER
                instance = MozoSocketClient(
                        URI("ws://${Support.domainSocket()}/websocket/user/${UUID.randomUUID()}/$channel"),
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
                instance?.closeConnection(1000, "disconnect socket")
            } finally {
                instance = null
            }
        }
    }
}