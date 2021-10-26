package io.mozocoin.sdk.common

import androidx.annotation.StringRes
import io.mozocoin.sdk.R

enum class ErrorCode(val key: String, @StringRes val message: Int) {
    ERROR_FATAL("INTERNAL_ERROR", R.string.error_fatal),
    ERROR_INVALID_REQUEST("INVALID_REQUEST", R.string.error_fatal),
    ERROR_REQUIRED_LOGIN("INVALID_USER_TOKEN", R.string.error_required_login),

    ERROR_TX_NONCE_TOO_LOW("TRANSACTION_ERROR_NONCE_TOO_LOW", R.string.error_common),
    ERROR_TX_INVALID_ADDRESS("TRANSACTION_ERROR_INVALID_ADDRESS", R.string.error_tx_invalid_address),

    ERROR_WALLET_ADDRESS_NOT_EXIST("SOLOMON_PAYMENT_REQUEST_INVALID_NON_EXIST_WALLET_ADDRESS", R.string.error_wallet_not_found),

    ERROR_WALLET_ADDRESS_IN_USED("SOLOMON_USER_PROFILE_WALLET_ADDRESS_IN_USED", R.string.error_wallet_different),
    ERROR_WALLET_ADDRESS_EXISTING("SOLOMON_USER_PROFILE_WALLET_INVALID_UPDATE_EXISTING_WALLET_ADDRESS", R.string.error_wallet_different),
    ERROR_WALLET_DIFFERENT("SOLOMON_FATAL_USE_DIFFERENT_OFFCHAIN_ADDRESS", R.string.error_wallet_different),

    ERROR_CONVERT_STATUS_PENDING("STORE_ADDRESS_SIGN_TRANSACTION_STATUS_PENDING", R.string.error_convert_status_pending),
    ERROR_CONVERT_REACH_LIMIT("TRANSACTION_CONVERT_REACH_LIMIT", R.string.error_convert_reach_limit),

    ERROR_DUPLICATE_ADDRESS("SOLOMON_USER_ADDRESS_BOOK_DUPLICATE_OFFCHAIN_ADDRESS", R.string.error_common),

    ERROR_MAINTAINING("MAINTAINING", R.string.error_fatal),

    ERROR_USER_DEACTIVATED("USER_DEACTIVATED", R.string.error_account_deactivated),
    ERROR_UPDATE_VERSION_REQUIREMENT("UPDATE_VERSION_REQUIREMENT", R.string.error_fatal),
    ERROR_TEMPORARILY_SUSPENDED("TEMPORARILY_SUSPENDED", R.string.error_temporary_suspend);

    companion object {
        fun findByKey(key: String?) = values().find { it.key.equals(key, ignoreCase = true) }
    }
}