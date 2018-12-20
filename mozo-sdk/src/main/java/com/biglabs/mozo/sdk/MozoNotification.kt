package com.biglabs.mozo.sdk

import android.content.Context
import android.content.Intent
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.OnNotificationReceiveListener
import com.biglabs.mozo.sdk.common.model.Notification
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.transaction.TransactionDetails
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.censor
import com.biglabs.mozo.sdk.utils.displayString
import com.biglabs.mozo.sdk.utils.string
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Suppress("unused")
class MozoNotification {
    companion object {

        const val REQUEST_CODE = 0x3020
        private const val KEY_DATA = "mozo_notification_data"

        @Synchronized
        internal fun prepareDataIntent(message: Models.BroadcastDataContent): Intent = Intent(
                MozoSDK.getInstance().context,
                MozoSDK.getInstance().notifyActivityClass
        ).apply {
            putExtra(KEY_DATA, Gson().toJson(message))
        }

        internal fun prepareNotification(context: Context, message: Models.BroadcastDataContent): Notification {
            val isSendType = message.from.equals(
                    MozoWallet.getInstance().getAddress() ?: "",
                    ignoreCase = true
            )

            var title = if (message.amount != null) context.getString(
                    if (isSendType) R.string.mozo_notify_title_sent else R.string.mozo_notify_title_received,
                    Support.toAmountNonDecimal(message.amount, message.decimal).displayString()
            ) else ""
            var content = ""
            var largeIcon = R.drawable.im_notification_received_sent

            when (message.event) {
                Constant.NOTIFY_EVENT_AIRDROPPED -> {
                    content = context.getString(R.string.mozo_notify_content_from, message.storeName)
                    largeIcon = R.drawable.im_notification_airdrop
                }
                Constant.NOTIFY_EVENT_CUSTOMER_CAME -> {
                    title = context.string(if (message.isComeIn) R.string.mozo_notify_title_come_in else R.string.mozo_notify_title_just_left)
                    message.phoneNo?.let {
                        content = it.censor(3, 4)
                    }
                    largeIcon = R.drawable.im_notification_customer_came
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
            return Notification(isSend = isSendType, icon = largeIcon, title = title, content = content, type = message.event, time = message.time).apply {
                raw = Gson().toJson(message)
            }
        }

        @Synchronized
        internal fun save(data: Models.BroadcastDataContent) {
            GlobalScope.launch {
                val itemId = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .save(prepareNotification(MozoSDK.getInstance().context, data))

                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                        .notifications()
                        .get(itemId)

                launch(Dispatchers.Main) {
                    MozoSDK.getInstance().onNotificationReceiveListener?.onReveiced(result)
                }
            }
        }

        @Synchronized
        internal fun openDetails(context: Context, data: String) {
            try {
                Gson().fromJson(data, Models.BroadcastDataContent::class.java)
            } catch (e: Exception) {
                null
            }?.let {
                when (it.event) {
                    Constant.NOTIFY_EVENT_AIRDROPPED,
                    Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                        val txHistory = Models.TransactionHistory(
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