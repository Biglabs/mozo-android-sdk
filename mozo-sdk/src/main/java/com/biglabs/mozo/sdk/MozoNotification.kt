package com.biglabs.mozo.sdk

import android.content.Context
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.model.Notification
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.censor
import com.biglabs.mozo.sdk.utils.displayString
import com.biglabs.mozo.sdk.utils.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("unused")
class MozoNotification {
    companion object {

        internal fun prepareNotification(context: Context, message: Models.BroadcastDataContent): Notification {
            val isSendType = message.from.equals(
                    MozoWallet.getInstance().getAddress() ?: "",
                    ignoreCase = true
            )

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
                    val address = if (isSendType) message.to else message.from
                    val contact = MozoSDK.getInstance().contactViewModel.findByAddress(address)
                    content = context.getString(
                            if (isSendType) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from,
                            contact?.name ?: address
                    )
                }
            }
            return Notification(icon = largeIcon, title = title, content = content, type = message.event, time = message.time)
        }

        internal fun save(data: Models.BroadcastDataContent) = GlobalScope.launch {
            MozoDatabase.getInstance(MozoSDK.getInstance().context)
                    .notifications()
                    .save(
                            prepareNotification(MozoSDK.getInstance().context, data)
                    )
        }

        @JvmStatic
        fun getAll(callback: (notifications: List<Notification>) -> Unit) {
            GlobalScope.launch {
                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context).notifications().getAll()
                launch(Dispatchers.Main) { callback.invoke(result) }
            }
        }
    }
}