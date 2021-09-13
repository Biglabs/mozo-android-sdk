package io.mozocoin.sdk

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.google.gson.Gson
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.OnNotificationReceiveListener
import io.mozocoin.sdk.common.model.BroadcastDataContent
import io.mozocoin.sdk.common.model.Notification
import io.mozocoin.sdk.common.model.NotificationGroup
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.transaction.TransactionDetailsActivity
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Suppress("unused")
class MozoNotification private constructor() {

    private val notificationManager: NotificationManager by lazy {
        MozoSDK.getInstance().context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    internal var notifyActivityClass: Class<out Activity>? = null
    private var onNotificationReceiveListener: OnNotificationReceiveListener? = null

    private var mShowGroupNotifyJob: Job? = null

    internal fun showNotification(message: BroadcastDataContent) = MainScope().launch {
        notifyActivityClass ?: return@launch

        val context = MozoSDK.getInstance().context
        val notification = prepareNotification(context, message)
        val notificationGroup = NotificationGroup.getKey(message)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !notificationChannelExists(message.event ?: return@launch)
        ) {
            createChannel(message.event)
        }

        val intentUniqueId = (System.currentTimeMillis() and 0xfffffff).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            intentUniqueId,
            prepareDataIntent(notification, message),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val singleNotify =
            NotificationCompat.Builder(context, message.event ?: return@launch).apply {
                color = context.color(R.color.mozo_color_primary)
                setSmallIcon(R.drawable.ic_mozo_notification)
                setContentTitle(notification.titleDisplay())
                setContentText(notification.contentDisplay())
                setAutoCancel(true)
                setDefaults(android.app.Notification.DEFAULT_ALL)
                setContentIntent(pendingIntent)

                if (notification.type != Constant.NOTIFY_EVENT_GROUP_BROADCAST) {
                    setLargeIcon(context.bitmap(notification.icon()))
                    setGroup(notificationGroup.name)
                    extras.putString(EXTRAS_ITEM_AMOUNT, message.amount?.toString())
                    extras.putString(EXTRAS_ITEM_DATA, notification.raw)
                }
            }

        val id = atomicInteger.incrementAndGet()
        NotificationManagerCompat.from(context).notify(id, singleNotify.build())

        if (notification.type != Constant.NOTIFY_EVENT_GROUP_BROADCAST) {
            return@launch doGroupNotificationDelayed(
                context,
                message,
                notification,
                notificationGroup,
                pendingIntent
            )
        }

        message.imageId?.let {
            getImage(context, it) { drawable ->
                drawable?.toBitmap()?.let { bm ->
                    singleNotify.setLargeIcon(bm)
                    singleNotify.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bm)
                            .bigLargeIcon(null)
                    )
                }
                NotificationManagerCompat.from(context).notify(id, singleNotify.build())
            }
        }
    }

    private fun doGroupNotificationDelayed(
        context: Context,
        message: BroadcastDataContent,
        notify: Notification,
        notifyGroup: NotificationGroup,
        pendingIntent: PendingIntent
    ) {

        mShowGroupNotifyJob?.cancel()
        mShowGroupNotifyJob = MainScope().launch {
            delay(2000)

            val group = NotificationCompat.Builder(context, message.event ?: return@launch)
                .apply {
                    val line = TextUtils.concat(notify.titleDisplay(), " ", notify.contentDisplay())
                    val items = NotificationGroup.getItems(context, notificationManager, message)
                    val title = NotificationGroup.getContentTitle(
                        context,
                        message,
                        count = items?.size ?: 0
                    ) ?: line
                    val totalText = NotificationGroup.getContentText(
                        context,
                        notificationManager,
                        notifyGroup
                    ) ?: line

                    setStyle(NotificationCompat.InboxStyle().run {
                        items?.forEach { addLine(it) }
                            ?: addLine(line)
                        setBigContentTitle(title)
                    })

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                        setContentIntent(pendingIntent)

                    color = context.color(R.color.mozo_color_primary)
                    setAutoCancel(true)
                    setContentTitle(title)
                    setDefaults(android.app.Notification.DEFAULT_ALL)
                    setGroup(notifyGroup.name)
                    setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                    setGroupSummary(true)
                    setLargeIcon(context.bitmap(NotificationGroup.getIcon(message.event)))
                    setNumber(items?.size ?: 0)
                    setSmallIcon(R.drawable.ic_mozo_notification)
                    setSubText(totalText)
                }
            NotificationManagerCompat.from(context).notify(notifyGroup.id, group.build())
        }
    }

    private fun getImage(
        context: Context,
        imageID: String,
        completion: ((Drawable?) -> Unit)? = null
    ) {
        val url = "https://${Support.domainImage()}/api/public/$imageID"
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(
                onSuccess = { result ->
                    // Handle the successful result.
                    completion?.invoke(result)
                },
                onError = {
                    completion?.invoke(null)
                }
            )
            .build()
        context.imageLoader.enqueue(request)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createChannel(channel: String) = coroutineScope {
        var channelName = channel.split("_")[0]
        channelName =
            channelName.substring(0, 1).uppercase(Locale.getDefault()) + channelName.substring(1)

        val notificationChannel =
            NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.setShowBadge(true)
        notificationChannel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannelExists(channelId: String): Boolean =
        notificationManager.getNotificationChannel(
            channelId
        ) != null


    companion object {

        const val KEY_DATA = "mozo_notification_data"
        const val KEY_TYPE = "mozo_notification_type"

        internal const val EXTRAS_ITEM_AMOUNT = "EXTRAS_ITEM_AMOUNT"
        internal const val EXTRAS_ITEM_DATA = "EXTRAS_ITEM_DATA"

        private val ourInstance = MozoNotification()

        private val atomicInteger = AtomicInteger(0)

        @JvmStatic
        internal fun getInstance(): MozoNotification = ourInstance

        internal fun shouldShowNotification(event: String?) = (if (MozoSDK.isRetailerApp)
            arrayOf(
                Constant.NOTIFY_EVENT_AIRDROPPED,
                Constant.NOTIFY_EVENT_AIRDROP_TOKEN_BACK,
                Constant.NOTIFY_EVENT_AIRDROP_INVITE,
                Constant.NOTIFY_EVENT_AIRDROP_FOUNDER,
                Constant.NOTIFY_EVENT_AIRDROP_SIGN_UP,
                Constant.NOTIFY_EVENT_AIRDROP_TOP_RETAILER,
                Constant.NOTIFY_EVENT_BALANCE_CHANGED,
                Constant.NOTIFY_EVENT_CUSTOMER_CAME,
                Constant.NOTIFY_EVENT_PROMO_PURCHASED,
                Constant.NOTIFY_EVENT_GROUP_BROADCAST
            ) else
            arrayOf(
                Constant.NOTIFY_EVENT_AIRDROPPED,
                Constant.NOTIFY_EVENT_AIRDROP_TOKEN_BACK,
                Constant.NOTIFY_EVENT_AIRDROP_INVITE,
                Constant.NOTIFY_EVENT_AIRDROP_SIGN_UP,
                Constant.NOTIFY_EVENT_BALANCE_CHANGED,
                Constant.NOTIFY_EVENT_PROMO_USED,
                Constant.NOTIFY_EVENT_GROUP_BROADCAST,
                Constant.NOTIFY_EVENT_WARNING_COVID,
                Constant.NOTIFY_EVENT_LUCKY_DRAW_AWARD
            )).contains(event?.lowercase(Locale.ROOT)) && MozoSDK.shouldShowNotification

        @Synchronized
        internal fun prepareDataIntent(
            message: Notification,
            broadcastContent: BroadcastDataContent
        ): Intent = Intent(
            MozoSDK.getInstance().context,
            getInstance().notifyActivityClass
        ).apply {
            putExtra(KEY_DATA, Gson().toJson(message))
            if (message.type == Constant.NOTIFY_EVENT_LUCKY_DRAW_AWARD) {
                putExtra(message.type, broadcastContent.messageId)
            } else {
                putExtra(message.type, message.content)
            }
        }

        internal fun getNotificationIcon(type: String?) = when (type ?: "") {
            Constant.NOTIFY_EVENT_AIRDROPPED,
            Constant.NOTIFY_EVENT_AIRDROP_TOKEN_BACK -> R.drawable.im_notification_airdrop
            Constant.NOTIFY_EVENT_AIRDROP_INVITE -> R.drawable.im_notification_airdrop_invite
            Constant.NOTIFY_EVENT_AIRDROP_FOUNDER,
            Constant.NOTIFY_EVENT_AIRDROP_SIGN_UP,
            Constant.NOTIFY_EVENT_AIRDROP_TOP_RETAILER -> R.drawable.im_notification_airdrop_bonus
            Constant.NOTIFY_EVENT_CUSTOMER_CAME,
            Constant.NOTIFY_EVENT_PROMO_PURCHASED -> R.drawable.im_notification_customer_came
            Constant.NOTIFY_EVENT_PROMO_USED -> R.drawable.im_notification_promo_used
            Constant.NOTIFY_EVENT_GROUP_BROADCAST -> R.drawable.im_notification_group_broadcast
            Constant.NOTIFY_EVENT_WARNING_COVID -> R.drawable.im_notification_covid_warning
            Constant.NOTIFY_EVENT_LUCKY_DRAW_AWARD -> R.drawable.im_notification_lucky_draw
            else -> R.drawable.im_notification_balance_changed
        }

        @JvmStatic
        fun prepareNotification(context: Context, message: BroadcastDataContent): Notification {
            val isSendType = message.from.equals(
                MozoSDK.getInstance().profileViewModel.getProfile()?.walletInfo?.offchainAddress,
                ignoreCase = true
            )

            var title = ""
            if (message.event == Constant.NOTIFY_EVENT_GROUP_BROADCAST) {
                title = message.title ?: ""

            } else if (message.amount != null) {
                title = context.getString(
                    when (message.event ?: "") {
                        Constant.NOTIFY_EVENT_AIRDROP_FOUNDER,
                        Constant.NOTIFY_EVENT_AIRDROP_SIGN_UP,
                        Constant.NOTIFY_EVENT_AIRDROP_TOP_RETAILER -> R.string.mozo_notify_title_airdrop_bonus
                        Constant.NOTIFY_EVENT_AIRDROP_TOKEN_BACK -> R.string.mozo_notify_title_airdrop_back
                        else -> if (isSendType) R.string.mozo_notify_title_sent else R.string.mozo_notify_title_received
                    },
                    Support.toAmountNonDecimal(message.amount, message.decimal).displayString()
                )
            }

            var content = ""

            when (message.event ?: "") {
                Constant.NOTIFY_EVENT_AIRDROPPED -> {
                    content =
                        context.getString(R.string.mozo_notify_content_from, message.storeName)
                }
                Constant.NOTIFY_EVENT_AIRDROP_TOKEN_BACK -> {
                    content =
                        context.getString(
                            R.string.mozo_notify_content_token_back,
                            message.storeName
                        )
                }
                Constant.NOTIFY_EVENT_AIRDROP_INVITE -> {
                    val phone = message.phoneNo?.censor(3, 4) ?: ""
                    content = context.getString(R.string.mozo_notify_content_invited, phone)
                }
                Constant.NOTIFY_EVENT_AIRDROP_FOUNDER -> {
                    content = context.getString(R.string.mozo_notify_content_airdrop_founder)
                }
                Constant.NOTIFY_EVENT_AIRDROP_SIGN_UP -> {
                    content = context.getString(R.string.mozo_notify_content_airdrop_sign_up)
                }
                Constant.NOTIFY_EVENT_AIRDROP_TOP_RETAILER -> {
                    content = context.getString(R.string.mozo_notify_content_airdrop_1k_retailer)
                }
                Constant.NOTIFY_EVENT_CUSTOMER_CAME -> {
                    title = context.resources.getQuantityString(
                        if (message.isComeIn) R.plurals.mozo_notify_title_come_in
                        else R.plurals.mozo_notify_title_leave,
                        1
                    )
                    message.phoneNo?.let {
                        content = it.censor(3, 4)
                    }
                }
                Constant.NOTIFY_EVENT_PROMO_USED -> {
                    title = context.getString(R.string.mozo_notify_title_promo_used)
                    content = context.getString(
                        R.string.mozo_notify_title_promo_used_content,
                        message.storeName
                    )
                }
                Constant.NOTIFY_EVENT_PROMO_PURCHASED -> {
                    title = context.getString(R.string.mozo_notify_title_promo_purchased)
                    content = message.promoName ?: ""
                }
                Constant.NOTIFY_EVENT_GROUP_BROADCAST -> {
                    title = message.title ?: ""
                    content = message.body ?: ""
                }
                Constant.NOTIFY_EVENT_WARNING_COVID -> {
                    title = context.getString(R.string.mozo_notify_title_covid_warning)
                    content = context.getString(
                        R.string.mozo_notify_content_covid_warning,
                        message.numNewWarningZone
                    )
                }
                Constant.NOTIFY_EVENT_LUCKY_DRAW_AWARD -> {
                    title = context.getString(R.string.mozo_notify_title_lucky_draw)
                    content = context.getString(R.string.mozo_notify_content_lucky_draw)
                }
                else -> {
                    val address = if (isSendType) message.to else message.from
                    val contact = MozoWallet.getInstance().findContact(address)
                    content = context.getString(
                        if (isSendType) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from,
                        contact?.name ?: address
                    )
                }
            }
            return Notification(
                isSend = isSendType,
                title = title,
                content = content,
                type = message.event
                    ?: "",
                time = message.time
            ).apply {
                raw = Gson().toJson(message)
            }
        }

        @Synchronized
        internal fun save(data: BroadcastDataContent) {
            if (!shouldShowNotification(data.event)) {
                return
            }
            MozoSDK.scope.launch {
                val itemId = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                    .notifications()
                    .save(prepareNotification(MozoSDK.getInstance().context, data))

                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                    .notifications()
                    .get(itemId)

                withContext(Dispatchers.Main) {
                    getInstance().onNotificationReceiveListener?.onReceived(result)
                }
            }
        }

        @Synchronized
        internal fun handleAction(context: Context, data: String): Boolean {
            return try {
                val rawData = Gson().run {
                    if (data.contains("raw", ignoreCase = true)) {
                        fromJson(
                            fromJson(data, Notification::class.java).raw,
                            BroadcastDataContent::class.java
                        )
                    } else {
                        fromJson(data, BroadcastDataContent::class.java)
                    }
                }
                when (rawData.event) {
                    Constant.NOTIFY_EVENT_AIRDROPPED,
                    Constant.NOTIFY_EVENT_AIRDROP_TOKEN_BACK,
                    Constant.NOTIFY_EVENT_BALANCE_CHANGED -> {
                        val txHistory = TransactionHistory(
                            blockHeight = 0L,
                            fees = 0.0,
                            amount = rawData.amount.safe(),
                            addressFrom = rawData.from,
                            addressTo = rawData.to,
                            symbol = rawData.symbol,
                            decimal = rawData.decimal,
                            time = rawData.time / 1000L,
                            txStatus = Constant.STATUS_SUCCESS
                        )
                        TransactionDetailsActivity.start(context, txHistory)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun getAll(callback: (notifications: List<Notification>) -> Unit) {
            MozoSDK.scope.launch {
                val result =
                    MozoDatabase.getInstance(MozoSDK.getInstance().context).notifications().getAll()
                withContext(Dispatchers.Main) { callback.invoke(result) }
            }
        }

        @JvmStatic
        fun setNotificationReceiveListener(listener: OnNotificationReceiveListener?) {
            getInstance().onNotificationReceiveListener = listener
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
            MozoSDK.scope.launch {
                notification.read = true
                MozoDatabase.getInstance(MozoSDK.getInstance().context)
                    .notifications()
                    .updateRead(notification)
                withContext(Dispatchers.Main) { callback.invoke(notification) }
            }
        }

        @Synchronized
        @JvmStatic
        fun markAllAsRead(callback: (notifications: List<Notification>) -> Unit) {
            MozoSDK.scope.launch {
                val result = MozoDatabase.getInstance(MozoSDK.getInstance().context)
                    .notifications()
                    .getAll()
                    .map {
                        it.apply { read = true }
                    }
                MozoDatabase.getInstance(MozoSDK.getInstance().context)
                    .notifications()
                    .updateRead(*result.toTypedArray())
                withContext(Dispatchers.Main) { callback.invoke(result) }
            }
        }

        @JvmStatic
        fun handleAction(context: Context, intent: Intent): Boolean {
            intent.getStringExtra(KEY_DATA)?.let {
                return handleAction(context, it)
            }
            return false
        }

        @JvmStatic
        fun handleAction(context: Context, notification: Notification): Boolean {
            notification.raw ?: return false
            return handleAction(context, notification.raw!!)
        }
    }
}