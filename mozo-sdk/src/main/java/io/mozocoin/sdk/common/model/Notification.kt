package io.mozocoin.sdk.common.model

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.mozocoin.sdk.MozoNotification
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.color
import kotlin.math.max

@Entity(tableName = "notifications")
class Notification(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L,
        var read: Boolean = false,
        val isSend: Boolean = false,
        val title: String,
        val content: String,
        val type: String,
        val time: Long
) {
    var raw: String? = null

    fun titleDisplay() = SpannableString(title).apply {
        set(0, length, StyleSpan(Typeface.BOLD))
        if (
                !Constant.NOTIFY_EVENT_CUSTOMER_CAME.equals(type, ignoreCase = true) &&
                !Constant.NOTIFY_EVENT_PROMO_USED.equals(type, ignoreCase = true) &&
                !Constant.NOTIFY_EVENT_PROMO_PURCHASED.equals(type, ignoreCase = true)
        )
            set(
                    max(indexOfFirst { it.isDigit() }, 0),
                    length,
                    ForegroundColorSpan(MozoSDK.getInstance().context.color(R.color.mozo_color_primary))
            )
    }

    fun contentDisplay() = SpannableString(content).apply {
        set(0, length, StyleSpan(Typeface.ITALIC))
        set(0, length, ForegroundColorSpan(MozoSDK.getInstance().context.color(R.color.mozo_color_section_text)))
    }

    fun icon() = MozoNotification.getNotificationIcon(type)
}