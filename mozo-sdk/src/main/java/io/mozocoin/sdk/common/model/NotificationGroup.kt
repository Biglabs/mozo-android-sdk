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
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.activeNotifications
                        .firstOrNull {
                            it.groupKey.contains(groupKey, true)
                                    && it.notification.extras.containsKey(key)
                        }?.notification?.extras
            } else null
        }

        fun getItems(context: Context, message: BroadcastDataContent, groupExtras: Bundle, newItem: String): List<String>? {
            var items = getCurrentlyGroupExtras(context, getKey(message).name, NOTIFY_ITEM)
                    ?.getString(NOTIFY_ITEM)
            items = if (items.isNullOrBlank()) newItem
            else "$newItem|$items"

            groupExtras.putString(NOTIFY_ITEM, items)
            return items.split("|")
        }

        fun getContentText(context: Context, message: BroadcastDataContent): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return null

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val count = manager.activeNotifications.filter {
                it.groupKey.contains(getKey(message).name, true)
            }.size - 1

            return when (message.event) {
                Constant.NOTIFY_EVENT_BALANCE_CHANGED -> "From $count transaction"
                else -> ""
            }
        }

        fun getContentTitle(context: Context, message: BroadcastDataContent, groupExtras: Bundle): String {
            var count = getCurrentlyGroupExtras(context, getKey(message).name, TOTAL_AMOUNT)
                    ?.getDouble(TOTAL_AMOUNT, 0.0) ?: 0.0

            count += MozoTx.getInstance().amountNonDecimal(message.amount
                    ?: BigDecimal.ZERO).toDouble()

            count = DecimalFormat("#.##").format(count).toDouble()

            groupExtras.putDouble(TOTAL_AMOUNT, count)

            return when (message.event) {
                Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                    if (message.from.equals(MozoWallet.getInstance().getAddress() ?: "", true))
                        context.getString(R.string.mozo_notify_title_sent)
                    else
                        context.getString(R.string.mozo_notify_title_received)
                }
                else -> "$count Customers arrived"
            }
        }

        fun getIcon(type: String?) = when (type) {
            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> R.drawable.ic_airdrops_grouped
            else -> R.drawable.ic_customer_arrived_grouped
        }

        /*return group key of Notifications*/
        fun getKey(message: BroadcastDataContent) = when (message.event) {
            Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                if (message.from.equals(MozoWallet.getInstance().getAddress(), true))
                    BALANCE_SENT
                else BALANCE_RECEIVE
            }

            Constant.NOTIFY_EVENT_CUSTOMER_CAME -> {
                if (message.isComeIn)
                    CUSTOMER_COME_IN
                else CUSTOMER_COME_OUT
            }

            Constant.NOTIFY_EVENT_AIRDROP_INVITE -> INVITE

            else -> AIRDROP
        }
    }
}