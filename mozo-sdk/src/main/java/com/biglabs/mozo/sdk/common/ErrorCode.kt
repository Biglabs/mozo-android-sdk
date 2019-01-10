package com.biglabs.mozo.sdk.common

import androidx.annotation.StringRes
import com.biglabs.mozo.sdk.R

enum class ErrorCode(@StringRes message: Int) {
    INTERNAL_ERROR(R.string.error_internal),
    INVALID_USER_TOKEN(R.string.error_invalid_token)
}