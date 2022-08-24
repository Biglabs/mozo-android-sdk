package io.mozocoin.sdk.common.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentRequest(
    val id: Long = 0L,
    val content: String?,
    val timeInSec: Long = 0L
) : Parcelable