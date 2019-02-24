package io.mozocoin.sdk.common

import android.content.Context
import androidx.annotation.StringRes
import io.mozocoin.sdk.R

enum class Gender(val key: String, @StringRes val display: Int) {
    NONE("None", R.string.mozo_text_gender_none),
    MALE("Male", R.string.mozo_text_gender_male),
    FEMALE("Female", R.string.mozo_text_gender_female);

    fun display(context: Context) = context.getString(display)

    companion object {
        fun find(key: String?) = values().find { it.key.equals(key, ignoreCase = true) } ?: NONE
    }
}