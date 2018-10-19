package com.biglabs.mozo.sdk.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.auth.AuthStateManager
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.core.Models
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
            if (equals("1|X", ignoreCase = false)) {
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
                            MozoSDK.profileViewModel?.fetchData()
                        }
                        event.logAsError("message event")
                        amount.toString().logAsError("message amount")
                        decimal.toString().logAsError("decimal")
                        from.logAsError("from")
                        to.logAsError("to")

                        val isSendType = from.equals(myAddress, ignoreCase = true)

                        val resultIntent = Intent(MozoSDK.context, MozoSDK.notifyAciivityClass)
                        val requestID = System.currentTimeMillis().toInt()
                        val pendingIntent = PendingIntent.getActivity(MozoSDK.context, requestID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                        val notifyTitle = String.format(Locale.US, "You %s %s Mozo", (if (isSendType) "sent" else "received"), Support.calculateAmountDecimal(amount, decimal).displayString())
                        val notifyContent = String.format(Locale.US, "%s Mozo wallet address @%s…%s", (if (isSendType) "To" else "From"), from.substring(0..5), from.substring(from.length - 5 until from.length))

                        val builder = NotificationCompat.Builder(MozoSDK.context!!, "event")
                                .setSmallIcon(R.drawable.ic_mozo_offchain)
                                .setColor(Color.parseColor("#4e94f3"))
                                .setContentTitle(notifyTitle)
                                .setContentText(notifyContent)
                                .setAutoCancel(true)
                                .setDefaults(Notification.DEFAULT_ALL)
                                .setContentIntent(pendingIntent)
                        NotificationManagerCompat.from(MozoSDK.context!!).notify(System.currentTimeMillis().toInt(), builder.build())
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