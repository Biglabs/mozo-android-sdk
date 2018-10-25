package com.biglabs.mozo.sdk.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.displayString
import com.biglabs.mozo.sdk.utils.logAsError
import com.google.gson.Gson
import kotlinx.coroutines.experimental.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

class MozoSocketClient(uri: URI, header: Map<String, String>) : WebSocketClient(uri, header) {

    private var myAddress: String? = null

    val notificationManager: NotificationManager by lazy { MozoSDK.context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    init {
        launch {
            myAddress = WalletService.getInstance().getAddress().await()
        }
    }

    override fun onOpen(serverHandshake: ServerHandshake?) {
        "open: ${serverHandshake?.httpStatus}, ${serverHandshake?.httpStatusMessage}".logAsError("web socket")
    }

    override fun onMessage(s: String?) {
        "message: $s".logAsError("web socket")
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
                        if (event.equals(Constant.NOTIFY_EVENT_BALANCE_CHANGED, ignoreCase = true)) {
                            MozoSDK.getInstance().profileViewModel.fetchData()
                        }
                        event.logAsError("message event")
                        amount.toString().logAsError("message amount")
                        decimal.toString().logAsError("decimal")
                        from.logAsError("from")
                        to.logAsError("to")

                        showNotification(this)
                    }
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        "close: $code, reason: $reason, remote: $remote".logAsError("web socket")
    }

    override fun onError(e: Exception?) {
        "error: ${e?.message}".logAsError("web socket")
    }

    private fun showNotification(message: Models.BroadcastDataContent) {
        MozoSDK.context?.applicationContext?.run {

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                if (!notificationChannelExists(message.event)) {
                    createChannel(message.event)
                }
            }
            val isSendType = message.from.equals(myAddress, ignoreCase = true)

            val resultIntent = Intent(MozoSDK.context, MozoSDK.notifyActivityClass)
            val requestID = System.currentTimeMillis().toInt()
            val pendingIntent = PendingIntent.getActivity(MozoSDK.context, requestID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notifyTitle = String.format(Locale.US, "You %s %s Mozo", (if (isSendType) "sent" else "received"), Support.calculateAmountDecimal(message.amount, message.decimal).displayString())
            val notifyContent = if (isSendType) buildNotificationContent("To", message.to) else buildNotificationContent("From", message.from)

            val builder = NotificationCompat.Builder(MozoSDK.context!!, message.event)
                    .setSmallIcon(R.drawable.ic_mozo_notification)
                    .setColor(ContextCompat.getColor(this, R.color.mozo_color_primary))
                    .setContentTitle(notifyTitle)
                    .setContentText(notifyContent)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(channel: String) {
        val notificationChannel = NotificationChannel(channel, channel, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.setShowBadge(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannelExists(channelId: String): Boolean = notificationManager.getNotificationChannel(channelId) != null

    private fun buildNotificationContent(prefix: String, address: String) = String.format(
            Locale.US,
            "%s Mozo wallet address @%s…%s",
            prefix,
            address.substring(0..5),
            address.substring(address.length - 5 until address.length)
    )

    companion object {
        @Volatile
        private var instance: MozoSocketClient? = null

        fun connect(context: Context) = synchronized(this) {
            if (instance == null) {
                val accessToken = AuthStateManager.getInstance(context).current.accessToken ?: ""
                instance = MozoSocketClient(
                        URI(Constant.BASE_SOCKET_URL + UUID.randomUUID().toString()),
                        mutableMapOf(
                                "Authorization" to "bearer $accessToken",
                                "Content-Type" to "application/json",
                                "X-atmo-protocol" to "true",
                                "X-Atmosphere-Framework" to "2.3.3-javascript",
                                "X-Atmosphere-tracking-id" to "0",
                                "X-Atmosphere-Transport" to "websocket"
                        )
                )
                instance?.getURI().toString().logAsError("socket uri")
            }
            instance?.connect()
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