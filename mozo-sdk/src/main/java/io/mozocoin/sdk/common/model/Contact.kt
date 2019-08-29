package io.mozocoin.sdk.common.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Contact(
        @Transient
        val id: Long,
        @SerializedName(value = "name", alternate = ["storeName"]) val name: String?,
        @SerializedName("storePhysicalAddress") val physicalAddress: String?,
        @SerializedName(value = "soloAddress", alternate = ["storeOffchainAddress"]) val soloAddress: String?,
        val phoneNo: String?,
        @Transient
        var isStore: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(physicalAddress)
        parcel.writeString(soloAddress)
        parcel.writeString(phoneNo)
        parcel.writeByte(if (isStore) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Contact> {
        override fun createFromParcel(parcel: Parcel): Contact {
            return Contact(parcel)
        }

        override fun newArray(size: Int): Array<Contact?> {
            return arrayOfNulls(size)
        }
    }
}