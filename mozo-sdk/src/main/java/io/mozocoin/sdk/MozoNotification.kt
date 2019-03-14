package io.mozocoin.sdk

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.OnNotificationReceiveListener
import io.mozocoin.sdk.common.model.BroadcastDataContent
import io.mozocoin.sdk.common.model.Notification
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.transaction.TransactionDetails
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.censor
import io.mozocoin.sdk.utils.displayString
import io.mozocoin.sdk.utils.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Suppress("unused")
class MozoNotification {
    companion object {

        const val REQUEST_CODE = 0x3020
        const val KEY_DATA = "mozo_notification_data"

        internal fun shouldShowNotification(event: String?) = (if (MozoSDK.isRetailerApp)
            arrayOf(
                    Constant.NOTIFY_EVENT_AIRDROPPED,
                    Constant.NOTIFY_EVENT_BALANCE_CHANGED,
                    Constant.NOTIFY_EVENT_CUSTOMER_CAME
            ) else
            arrayOf(
                    Constant.NOTIFY_EVENT_AIRDROPPED,
                    Constant.NOTIFY_EVENT_BALANCE_CHANGED
            )).contains(event?.toLowerCase())

        @Synchronized
        internal fun prepareDataIntent(message: Notification): Intent = Intent(
                MozoSDK.getInstance().context,
                MozoSDK.getInstance().notifyActivityClass
        ).apply {
            putExtra(KEY_DATA, Gson().toJson(message))
        }

        internal fun prepareNotification(context: Context, message: BroadcastDataContent): Notification {
            val isSendType = message.from.equals(
                    MozoWallet.getInstance().getAddress() ?: "",
                    ignoreCase = true
            )

            var title = if (message.amount != null) context.getString(
                    if (isSendType) R.string.mozo_notify_title_sent else R.string.mozo_notify_title_received,
                    Support.toAmountNonDecimal(message.amount, message.decimal).displayString()
            ) else ""
            var content = ""

            when (message.event ?: "") {
                Constant.NOTIFY_EVENT_AIRDROPPED -> {
                    content = context.getString(R.string.mozo_notify_content_from, message.storeName)
                }
                Constant.NOTIFY_EVENT_CUSTOMER_CAME -> {
                    title = context.string(if (message.isComeIn) R.string.mozo_notify_title_come_in else R.string.mozo_notify_title_just_left)
                    message.phoneNo?.let {
                        content = it.censor(3, 4)
                    }
                }
                Constant.NOTIFY_EVENT_STORE_BOOK_ADDED -> {
                }
                else -> {
                    val address = if (isSendType) message.to else message.from
                    val contact = MozoSDK.getInstance().contactViewModel.findByAddress(address)
                    content = context.getString(
                            if (isSendType) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from,
                            contact?.name ?: address
                    )
                }
            }
            return Notification(isSend = isSendType, title = title, content = content, type = message.event
                    ?: "", time = message.time).apply {
                raw = Gson().toJson(message)
            }
        }

        internal fun getNotificationIcon(type: String?) = when (type ?: "") {
            Constant.NOTIFY_EVENT_AIRDROPPED -> R.drawable.im_notification_airdrop
            Constant.NOTIFY_EVENT_CUSTOMER_CAME -> R.drawable.im_notification_customer_came
            else -> R.drawable.im_notification_received_sent
        }

        @Synchronized
        internal fun save(data: BroadcastDataContent) {
            if (!shouldShowNotification(data.event)) {
                return
            }
            GlobalScope.launch {
                val itemId = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .save(prepareNotification(MozoSDK.getInstance().context, data))

                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .get(itemId)

                launch(Dispatchers.Main) {
                    MozoSDK.getInstance().onNotificationReceiveListener?.onReceived(result)
                }
            }
        }

        @Synchronized
        internal fun openDetails(context: Context, data: String) {
            try {
                Gson().run {
                    if (data.contains("raw", ignoreCase = true)) {
                        fromJson(fromJson(data, Notification::class.java).raw, BroadcastDataContent::class.java)
                    } else {
                        fromJson(data, BroadcastDataContent::class.java)
                    }
                }
            } catch (e: Exception) {
                null
            }?.let {
                when (it.event) {
                    Constant.NOTIFY_EVENT_AIRDROPPED,
                    Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                        val txHistory = TransactionHistory(
                                null,
                                0L,
                                null,
                                0.0,
                                it.amount ?: BigDecimal.ZERO,
                                it.from,
                                it.to,
                                null,
                                it.symbol,
                                null,
                                it.decimal,
                                it.time / 1000L,
                                Constant.STATUS_SUCCESS
                        )
                        TransactionDetails.start(context, txHistory)
                    }
                }
            }
        }

        @JvmStatic
        fun getAll(callback: (notifications: List<Notification>) -> Unit) {
            GlobalScope.launch {
                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context).notifications().getAll()
                launch(Dispatchers.Main) { callback.invoke(result) }
            }
        }

        @JvmStatic
        fun setNotificationReceiveListener(listener: OnNotificationReceiveListener) {
            MozoSDK.getInstance().onNotificationReceiveListener = listener
        }

        @Synchronized
        @JvmStatic
        fun markAsRead(intent: Intent, callback: (notification: Notification) -> Unit) {
            intent.getStringExtra(KEY_DATA)?.let { data ->
                try {
                    Gson().fromJson(data, Notification::class.java)
                } catch (ignore: Exception) {
                    null
                }?.let {
                    markAsRead(it, callback)
                }
            }
        }

        @Synchronized
        @JvmStatic
        fun markAsRead(notification: Notification, callback: (notification: Notification) -> Unit) {
            GlobalScope.launch {
                notification.read = true
                MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .updateRead(notification)
                launch(Dispatchers.Main) { callback.invoke(notification) }
            }
        }

        @Synchronized
        @JvmStatic
        fun markAllAsRead(callback: (notifications: List<Notification>) -> Unit) {
            GlobalScope.launch {
                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .getAll()
                        .map {
                            it.apply { read = true }
                        }
                MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .updateRead(*result.toTypedArray())
                launch(Dispatchers.Main) { callback.invoke(result) }
            }
        }

        @JvmStatic
        fun openDetails(context: Context, intent: Intent) {
            intent.getStringExtra(KEY_DATA)?.let {
                openDetails(context, it)
            }
        }

        @JvmStatic
        fun openDetails(context: Context, notification: Notification) {
            notification.raw ?: return
            openDetails(context, notification.raw!!)
        }
    }
}