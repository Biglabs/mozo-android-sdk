package io.mozocoin.sdk.common.model

import androidx.room.Ignore

open class ProfileLocale(
    @Ignore open val locale: String? = null,
    @Ignore open val language: String? = null,
    @Ignore open val region: String? = null,
)
