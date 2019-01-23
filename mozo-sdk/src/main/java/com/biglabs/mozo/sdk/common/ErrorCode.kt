package com.biglabs.mozo.sdk.common

import androidx.annotation.StringRes
import com.biglabs.mozo.sdk.R

enum class ErrorCode(val key: String, @StringRes val message: Int) {
    ERROR_FATAL("INTERNAL_ERROR", R.string.error_fatal),
    ERROR_REQUIRED_LOGIN("INVALID_USER_TOKEN", R.string.error_required_login),

    ERROR_WALLET_DIFFERENT("SOLOMON_USER_PROFILE_WALLET_INVALID_UPDATE_EXISTING_WALLET_ADDRESS", R.string.error_wallet_different)
}