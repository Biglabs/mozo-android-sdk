package com.biglabs.mozo.sdk.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.set
import com.biglabs.mozo.sdk.BuildConfig
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.authentication.AuthStateManager
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.utils.*
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

    private var myAddress: String? = null

    private val notificationManager: NotificationManager by lazy { MozoSDK.context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    init {
        GlobalScope.launch {
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
                        toString().logAsError("Socket message")

                        if (event.equals(Constant.NOTIFY_EVENT_BALANCE_CHANGED, ignoreCase = true)) {
                            MozoSDK.getInstance().profileViewModel.fetchData(MozoSDK.context!!)
                        }
                        showNotification(this)
                    }
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        "close: $code, reason: $reason, remote: $remote".logAsError("web socket")
        instance = null
    }

    override fun onError(e: Exception?) {
        "error: ${e?.message}".logAsError("web socket")
        disconnect()
    }

    private fun showNotification(message: Models.BroadcastDataContent) = GlobalScope.launch {
        val context = MozoSDK.context?.applicationContext ?: return@launch

        val isSendType = message.from.equals(myAddress, ignoreCase = true)

        var title = if (message.amount != null) context.getString(
                if (isSendType) R.string.mozo_notify_title_sent else R.string.mozo_notify_title_received,
                Support.calculateAmountDecimal(message.amount, message.decimal).displayString()
        ) else ""
        var content = ""
        var largeIcon = R.drawable.im_notification_received_sent

        when (message.event) {
            Constant.NOTIFY_EVENT_AIRDROPPED -> {
                content = context.getString(R.string.mozo_notify_content_from, message.storeName)
                largeIcon = R.drawable.im_notification_airdrop
            }
            Constant.NOTIFY_EVENT_CUSTOMER_CAME -> {
                title = context.string(if (message.comeIn) R.string.mozo_notify_title_come_in else R.string.mozo_notify_title_just_left)
                message.phoneNo?.let {
                    content = it.censor(3, 4)
                }
                largeIcon = R.drawable.im_notification_customer_came
            }
            else -> {
                content = buildNotificationContent(context, isSendType, if (isSendType) message.to else message.from).await()
            }
        }

        val contentTitle = SpannableString(title).apply {
            set(0, length, StyleSpan(Typeface.BOLD))
            if (!Constant.NOTIFY_EVENT_CUSTOMER_CAME.equals(message.event, ignoreCase = true))
                set(indexOf(" "), length, ForegroundColorSpan(context.color(R.color.mozo_color_primary)))
        }
        val contentText = SpannableString(content).apply {
            set(0, length, StyleSpan(Typeface.ITALIC))
            set(0, length, ForegroundColorSpan(context.color(R.color.mozo_color_section_text)))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !notificationChannelExists(message.event)) {
            createChannel(message.event).await()
        }
        val resultIntent = Intent(context, MozoSDK.notifyActivityClass)
        val requestID = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(context, requestID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, message.event)
                .setSmallIcon(R.drawable.ic_mozo_notification)
                .setLargeIcon(context.bitmap(largeIcon))
                .setColor(context.color(R.color.mozo_color_primary))
                .setContentTitle(contentTitle)
                .setContentText(contentText)
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

    private fun buildNotificationContent(context: Context, isSent: Boolean, address: String) = GlobalScope.async {
        val contact = MozoSDK.getInstance().contactViewModel.findByAddress(address)

        return@async context.getString(
                if (isSent) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from,
                contact?.name ?: address
        )
    }

    companion object {
        @Volatile
        private var instance: MozoSocketClient? = null

        fun connect(context: Context) = synchronized(this) {
            if (instance == null) {
                val accessToken = AuthStateManager.getInstance(context).current.accessToken ?: ""
                instance = MozoSocketClient(
                        URI("ws://${BuildConfig.DOMAIN_SOCKET}/websocket/user/" + UUID.randomUUID().toString()),
                        mutableMapOf(
                                "Authorization" to "bearer $accessToken",
                                "Content-Type" to "application/json",
                                "X-atmo-protocol" to "true",
                                "X-Atmosphere-Framework" to "2.3.3-javascript",
                                "X-Atmosphere-tracking-id" to "0",
                                "X-Atmosphere-Transport" to "websocket"
                        )
                ).apply {
                    connect()
                }
            }
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