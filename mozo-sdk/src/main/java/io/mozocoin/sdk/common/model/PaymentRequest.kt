package io.mozocoin.sdk.common.model

import android.os.Parcel
import android.os.Parcelable

data class PaymentRequest(
        val id: Long = 0L,
        val content: String?,
        val timeInSec: Long = 0L
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(content)
        parcel.writeLong(timeInSec)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PaymentRequest> {
        override fun createFromParcel(parcel: Parcel): PaymentRequest {
            return PaymentRequest(parcel)
        }

        override fun newArray(size: Int): Array<PaymentRequest?> {
            return arrayOfNulls(size)
        }
    }
}