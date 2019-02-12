package io.mozocoin.sdk.common.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Ignore
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.displayString
import java.math.BigDecimal

data class TransactionHistory(
        var txHash: String?,
        var blockHeight: Long,
        var action: String?,
        var fees: Double,
        var amount: BigDecimal,
        var addressFrom: String?,
        var addressTo: String?,
        var contractAddress: String?,
        var symbol: String?,
        var contractAction: String?,
        var decimal: Int,
        var time: Long,
        var txStatus: String?
) : Parcelable {

    @Ignore
    var contactName: String? = null

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readDouble(),
            (parcel.readString() ?: "0").toBigDecimal(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readLong(),
            parcel.readString())

    fun amountInDecimal(): BigDecimal = amount.divide(Math.pow(10.0, decimal.toDouble()).toBigDecimal())

    fun amountDisplay(): String = amountInDecimal().displayString(12)

    fun type(address: String?): Boolean = addressFrom.equals(MY_ADDRESS, ignoreCase = true) || addressFrom.equals(address, ignoreCase = true)

    fun isSuccess() = txStatus.equals(Constant.STATUS_SUCCESS, ignoreCase = true)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(txHash)
        parcel.writeLong(blockHeight)
        parcel.writeString(action)
        parcel.writeDouble(fees)
        parcel.writeString(amount.toString())
        parcel.writeString(addressFrom)
        parcel.writeString(addressTo)
        parcel.writeString(contractAddress)
        parcel.writeString(symbol)
        parcel.writeString(contractAction)
        parcel.writeInt(decimal)
        parcel.writeLong(time)
        parcel.writeString(txStatus)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionHistory> {
        internal const val MY_ADDRESS = "MY_ADDRESS"

        override fun createFromParcel(parcel: Parcel): TransactionHistory {
            return TransactionHistory(parcel)
        }

        override fun newArray(size: Int): Array<TransactionHistory?> {
            return arrayOfNulls(size)
        }
    }
}