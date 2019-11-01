package io.mozocoin.sdk.common.model

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import com.google.gson.Gson
import io.mozocoin.sdk.MozoNotification
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.displayString
import io.mozocoin.sdk.utils.safe
import java.math.BigDecimal

enum class NotificationGroup(val id: Int) {
    BALANCE_SENT(100),
    BALANCE_RECEIVE(101),
    CUSTOMER_COME_IN(102),
    CUSTOMER_COME_OUT(103),
    AIRDROP(104),
    AIRDROP_SIGN_UP(105),
    AIRDROP_FOUNDER(106),
    AIRDROP_TOP_1K(107),
    INVITE(108),
    PROMO(109),
    GROUP_BROADCAST(110);

    companion object {
        private fun getCurrentlyGroupExtras(notificationManager: NotificationManager, groupKey: String): List<Bundle>? = synchronized(this) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                notificationManager.activeNotifications?.mapNotNull {
                    if (it.groupKey.contains(groupKey, true)) it.notification.extras
                    else null
                }
            else null
        }

        fun getItems(context: Context, notificationManager: NotificationManager, message: BroadcastDataContent): List<CharSequence>? {
            val gSon = Gson()
            return getCurrentlyGroupExtras(notificationManager, getKey(message).name)?.mapNotNull {
                it.getString(MozoNotification.EXTRAS_ITEM_DATA)?.let { data ->
                    try {
                        gSon.fromJson(data, BroadcastDataContent::class.java)
                    } catch (ex: Exception) {
                        null
                    }
                }?.also { content ->
                    val notify = MozoNotification.prepareNotification(context, content)
                    return@mapNotNull TextUtils.concat(notify.titleDisplay(), " ", notify.contentDisplay())
                }

                return@mapNotNull null
            }
        }

        fun getContentText(context: Context, notificationManager: NotificationManager, notificationGroup: NotificationGroup): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                return null

            val extras = getCurrentlyGroupExtras(notificationManager, notificationGroup.name)

            var totalAmount = BigDecimal.ZERO
            extras?.forEach { bundle ->
                bundle.getString(MozoNotification.EXTRAS_ITEM_AMOUNT)?.let {
                    totalAmount = totalAmount.plus(it.toBigDecimalOrNull().safe())
                }
            }
            totalAmount = MozoTx.getInstance().amountNonDecimal(totalAmount)

            val totalNotice = extras?.size ?: 0
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
                PROMO -> ""
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
            Constant.NOTIFY_EVENT_CUSTOMER_CAME,
            Constant.NOTIFY_EVENT_PROMO_PURCHASED -> R.drawable.im_notification_customer_came_group
            Constant.NOTIFY_EVENT_PROMO_USED -> R.drawable.im_notification_promo_used
            Constant.NOTIFY_EVENT_AIRDROP_INVITE -> R.drawable.im_notification_airdrop_invite_group
            Constant.NOTIFY_EVENT_GROUP_BROADCAST -> R.drawable.ic_mozo_offchain
            else -> R.drawable.im_notification_airdrop_group
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
            Constant.NOTIFY_EVENT_AIRDROP_SIGN_UP -> AIRDROP_SIGN_UP
            Constant.NOTIFY_EVENT_AIRDROP_FOUNDER -> AIRDROP_FOUNDER
            Constant.NOTIFY_EVENT_AIRDROP_TOP_RETAILER -> AIRDROP_TOP_1K

            Constant.NOTIFY_EVENT_PROMO_PURCHASED -> PROMO
            Constant.NOTIFY_EVENT_PROMO_USED -> PROMO

            Constant.NOTIFY_EVENT_GROUP_BROADCAST -> GROUP_BROADCAST

            else -> AIRDROP
        }
    }
}