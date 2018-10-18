package com.biglabs.mozo.sdk.services

import android.content.Context
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.utils.AuthStateManager
import com.biglabs.mozo.sdk.utils.logAsError
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

class MozoSocketClient(uri: URI, header: Map<String, String>) : WebSocketClient(uri, header) {

    override fun onOpen(serverHandshake: ServerHandshake?) {
        "open: ${serverHandshake?.httpStatus}, ${serverHandshake?.httpStatusMessage}".logAsError("web socket")
    }

    override fun onMessage(s: String?) {
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
                        event.logAsError("message event")
                        amount.toString().logAsError("message amount")
                        decimal.toString().logAsError("decimal")
                        from.logAsError("from")
                        to.logAsError("to")
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