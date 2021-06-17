package io.mozocoin.sdk.common.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    @Transient
    val id: Long,
    @SerializedName(value = "name", alternate = ["storeName"]) val name: String? = null,
    @SerializedName("storePhysicalAddress") val physicalAddress: String? = null,
    @SerializedName(
        value = "soloAddress",
        alternate = ["storeOffchainAddress"]
    ) val soloAddress: String? = null,
    val phoneNo: String? = null,
    val storeId: Long? = null,
    @Transient
    var isStore: Boolean = false
) : Parcelable