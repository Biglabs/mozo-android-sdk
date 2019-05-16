package io.mozocoin.sdk.common.model

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.displayString
import io.mozocoin.sdk.utils.safe

enum class NotificationGroup(val id: Int) {
    BALANCE_SENT(100),
    BALANCE_RECEIVE(101),
    CUSTOMER_COME_IN(102),
    CUSTOMER_COME_OUT(103),
    AIRDROP(104),
    INVITE(105);

    companion object {
        private const val NOTIFY_ITEM = "notify_item"
        private const val TOTAL_AMOUNT = "total_amount"

        private fun getCurrentlyGroupExtras(notificationManager: NotificationManager, groupKey: String, key: String): Bundle? = synchronized(this) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications?.firstOrNull {
                    it.groupKey.contains(groupKey, true) && it.notification.extras.containsKey(key)
                }?.notification?.extras
            } else null
        }

        fun getItems(notificationManager: NotificationManager, message: BroadcastDataContent, groupExtras: Bundle? = null, newItem: String? = null): List<String>? {
            var items = getCurrentlyGroupExtras(notificationManager, getKey(message).name, NOTIFY_ITEM)
                    ?.getString(NOTIFY_ITEM)

            groupExtras?.apply {
                items = if (items.isNullOrBlank())
                    newItem
                else {
                    if (newItem != null)
                        "$newItem|$items"
                    else "$items"
                }
                putString(NOTIFY_ITEM, items)
            }

            return items?.split("|")
        }

        fun getContentText(context: Context, notificationManager: NotificationManager, message: BroadcastDataContent, groupExtras: Bundle): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return null

            val notificationGroup = getKey(message)
            val totalNotice = notificationManager.activeNotifications?.filter {
                it.groupKey.contains(notificationGroup.name, true)
            }?.size ?: 0

            var totalAmount = MozoTx.getInstance().amountNonDecimal(message.amount.safe())
            getCurrentlyGroupExtras(notificationManager, notificationGroup.name, TOTAL_AMOUNT)
                    ?.getString(TOTAL_AMOUNT)
                    ?.let { totalString ->
                        totalString.toBigDecimalOrNull()?.let {
                            totalAmount = totalAmount.plus(it)
                        }
                    }

            groupExtras.putString(TOTAL_AMOUNT, totalAmount.toString())

            return when (notificationGroup) {
                BALANCE_SENT -> context.getString(
                        R.string.mozo_notify_content_sent_group,
                        totalAmount.displayString()
                )
                BALANCE_RECEIVE -> context.getString(
                        R.string.mozo_notify_content_received_group,
                        totalAmount.displayString()
                )
                CUSTOMER_COME_IN -> context.getString(
                        R.string.mozo_notify_content_customer_join_group,
                        totalNotice
                )
                CUSTOMER_COME_OUT -> context.getString(
                        R.string.mozo_notify_content_customer_left_group,
                        totalNotice
                )
                INVITE -> context.getString(
                        R.string.mozo_notify_content_invited_group,
                        totalNotice
                )
                else -> context.getString(
                        R.string.mozo_notify_content_received_group,
                        totalAmount.displayString()
                )
            }
        }

        fun getContentTitle(context: Context, message: BroadcastDataContent, count: Int) = when (getKey(message)) {
            CUSTOMER_COME_IN -> context.resources.getQuantityString(R.plurals.mozo_notify_title_come_in, count)
            CUSTOMER_COME_OUT -> context.resources.getQuantityString(R.plurals.mozo_notify_title_leave, count)
            INVITE -> context.getString(R.string.mozo_notify_content_invited_group)
            else -> null
        }

        fun getIcon(type: String?) = when (type) {
            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> R.drawable.im_notification_balance_changed_group
            Constant.NOTIFY_EVENT_CUSTOMER_CAME -> R.drawable.im_notification_customer_came_group
            Constant.NOTIFY_EVENT_AIRDROPPED -> R.drawable.im_notification_airdrop_group
            else -> R.drawable.im_notification_airdrop_invite_group
        }

        fun getKey(message: BroadcastDataContent) = when (message.event) {
            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                if (message.from.equals(MozoWallet.getInstance().getAddress(), true))
                    BALANCE_SENT
                else BALANCE_RECEIVE
            }
            Constant.NOTIFY_EVENT_CUSTOMER_CAME -> {
                if (message.isComeIn) CUSTOMER_COME_IN
                else CUSTOMER_COME_OUT
            }
            Constant.NOTIFY_EVENT_AIRDROP_INVITE -> INVITE

            else -> AIRDROP
        }
    }
}