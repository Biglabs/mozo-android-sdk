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

enum class TransferSpeed(@StringRes val display: Int) {
    SLOW(R.string.mozo_convert_slow),
    NORMAL(R.string.mozo_convert_normal),
    FAST(R.string.mozo_convert_fast);

    companion object {
        fun calculate(progress: Int) = when (progress) {
            in 0..20 -> SLOW
            in 21..60 -> NORMAL
            else -> FAST
        }
    }
}

enum class TodoType(@StringRes val title: Int, @StringRes val action: Int) {
    AIRDROP_NEARBY(R.string.mozo_notify_to_do_airdrop_nearby, R.string.mozo_notify_to_do_airdrop_nearby_action),
    AIRDROP_EXPIRED(R.string.mozo_notify_to_do_airdrop_expire, R.string.mozo_notify_to_do_airdrop_expire_action),
    AIRDROP_EMPTY(R.string.mozo_notify_to_do_airdrop_empty, R.string.mozo_notify_to_do_airdrop_empty_action),
    AIRDROP_OUT_OF_MOZOX(R.string.mozo_notify_to_do_airdrop_out_of_mozo, R.string.mozo_notify_to_do_airdrop_out_of_mozo_action),
    LOCATION_SERVICE_OFF(R.string.mozo_notify_to_do_location, R.string.mozo_notify_to_do_location_action),
    BLUETOOTH_OFF(R.string.mozo_notify_to_do_bluetooth, R.string.mozo_notify_to_do_bluetooth_action),
    PROMOTION_NEARBY(R.string.mozo_notify_to_do_promo_nearby, R.string.mozo_notify_to_do_promo_nearby_action),
    PROMOTION_EXPIRED(R.string.mozo_notify_to_do_promo_expire, R.string.mozo_notify_to_do_promo_expire_action),
    PROMOTION_EMPTY(R.string.mozo_notify_to_do_promo_empty, R.string.mozo_notify_to_do_promo_empty_action),
    VOUCHER_NEARBY(R.string.mozo_notify_to_do_voucher_nearby, R.string.mozo_notify_to_do_voucher_nearby_action),
    VOUCHER_EXPIRED(R.string.mozo_notify_to_do_voucher_nearby, R.string.mozo_notify_to_do_voucher_nearby_action),
    LOW_MOZOX_RETAILER(R.string.mozo_notify_to_do_low_mozo_retailer, R.string.mozo_notify_to_do_low_mozo_retailer_action),
    LOW_MOZOX_SHOPPER(R.string.mozo_notify_to_do_low_mozo_shopper, R.string.mozo_notify_to_do_low_mozo_shopper_action),
    UNSECURE_WALLET(R.string.mozo_notify_to_do_unsecured, R.string.mozo_notify_to_do_unsecured_action);

    companion object {
        fun find(key: String?) = values().find { it.name.equals(key, ignoreCase = true) }
    }
}