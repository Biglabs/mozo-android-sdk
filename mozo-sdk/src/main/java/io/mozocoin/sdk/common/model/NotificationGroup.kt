package io.mozocoin.sdk.common.model

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import java.math.BigDecimal
import java.text.DecimalFormat

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

        private fun getCurrentlyGroupExtras(context: Context, groupKey: String, key: String): Bundle? = synchronized(this) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager?.activeNotifications
                        ?.firstOrNull {
                            it.groupKey.contains(groupKey, true)
                                    && it.notification.extras.containsKey(key)
                        }?.notification?.extras
            } else null
        }

        fun getItems(context: Context, message: BroadcastDataContent, groupExtras: Bundle? = null, newItem: String? = null): List<String>? {
            var items = getCurrentlyGroupExtras(context, getKey(message).name, NOTIFY_ITEM)
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

        fun getContentText(context: Context, message: BroadcastDataContent, groupExtras: Bundle): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return null

            val notificationGroup = getKey(message)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            var totalNotice = manager?.activeNotifications
                    ?.filter { it.groupKey.contains(notificationGroup.name, true) }
                    ?.size ?: 0

            totalNotice = Math.max(totalNotice - 1, 1)

            var totalAmount = getCurrentlyGroupExtras(context, notificationGroup.name, TOTAL_AMOUNT)
                    ?.getDouble(TOTAL_AMOUNT, 0.0) ?: 0.0

            totalAmount += MozoTx.getInstance()
                    .amountNonDecimal(message.amount ?: BigDecimal.ZERO)
                    .toDouble()

            totalAmount = DecimalFormat("#.##").format(totalAmount).toDouble()

            groupExtras.putDouble(TOTAL_AMOUNT, totalAmount)

            return when (notificationGroup) {
                BALANCE_SENT      -> context.getString(
                        R.string.mozo_notify_content_sent_group,
                        totalNotice,
                        totalAmount.toString()
                )
                BALANCE_RECEIVE   -> context.getString(
                        R.string.mozo_notify_content_received_group,
                        totalNotice,
                        totalAmount.toString()
                )
                CUSTOMER_COME_IN  -> context.getString(
                        R.string.mozo_notify_content_customer_join_group,
                        totalNotice,
                        totalNotice
                )
                CUSTOMER_COME_OUT -> context.getString(
                        R.string.mozo_notify_content_customer_left_group,
                        totalNotice,
                        totalNotice
                )
                INVITE            -> context.getString(
                        R.string.mozo_notify_content_invited_group,
                        totalNotice,
                        totalNotice
                )
                else              -> context.getString(
                        R.string.mozo_notify_content_airdrop_group,
                        totalNotice,
                        totalAmount.toString()
                )
            }
        }

        fun getContentTitle(context: Context, message: BroadcastDataContent) = when (getKey(message)) {
            CUSTOMER_COME_IN  -> context.getString(R.string.mozo_notify_title_come_in_group)

            CUSTOMER_COME_OUT -> context.getString(R.string.mozo_notify_title_just_left_group)

            INVITE            -> context.getString(R.string.mozo_notify_content_invited_group)

            else              -> null
        }

        fun getIcon(type: String?) = when (type) {
            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> R.drawable.ic_notification_balance_changed
            Constant.NOTIFY_EVENT_CUSTOMER_CAME   -> R.drawable.ic_customer_came_grouped
            Constant.NOTIFY_EVENT_AIRDROPPED      -> R.drawable.ic_notification_airdrops_grouped
            else                                  -> R.drawable.ic_notification_invite_group
        }

        fun getKey(message: BroadcastDataContent) = when (message.event) {
            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                if (message.from.equals(MozoWallet.getInstance().getAddress(), true))
                    BALANCE_SENT
                else BALANCE_RECEIVE
            }
            Constant.NOTIFY_EVENT_CUSTOMER_CAME   -> {
                if (message.isComeIn) CUSTOMER_COME_IN
                else CUSTOMER_COME_OUT
            }
            Constant.NOTIFY_EVENT_AIRDROP_INVITE  -> INVITE

            else                                  -> AIRDROP
        }
    }
}