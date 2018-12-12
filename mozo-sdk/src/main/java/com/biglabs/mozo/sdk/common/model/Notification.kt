package com.biglabs.mozo.sdk.common.model

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.text.set
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.utils.color

@Entity(tableName = "notifications")
class Notification(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L,
        var read: Boolean = false,
        val isSend: Boolean = false,
        val icon: Int,
        val title: String,
        val content: String,
        val type: String,
        val time: Long
) {
    fun titleDisplay() = SpannableString(title).apply {
        set(0, length, StyleSpan(Typeface.BOLD))
        if (!Constant.NOTIFY_EVENT_CUSTOMER_CAME.equals(type, ignoreCase = true))
            set(
                    indexOf(" "),
                    length,
                    ForegroundColorSpan(MozoSDK.getInstance().context.color(R.color.mozo_color_primary))
            )
    }

    fun contentDisplay() = SpannableString(content).apply {
        set(0, length, StyleSpan(Typeface.ITALIC))
        set(0, length, ForegroundColorSpan(MozoSDK.getInstance().context.color(R.color.mozo_color_section_text)))
    }
}