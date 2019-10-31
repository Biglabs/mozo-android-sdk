package io.mozocoin.sdk.common.model

import android.os.Parcelable
import androidx.room.Ignore
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.displayString
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import kotlin.math.pow

@Parcelize
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
    @IgnoredOnParcel
    var contactName: String? = null

    fun amountInDecimal(): BigDecimal = amount.divide(10.0.pow(decimal.toDouble()).toBigDecimal())

    fun amountDisplay(): String = amountInDecimal().displayString(12)

    fun type(address: String?): Boolean = addressFrom.equals(MY_ADDRESS, ignoreCase = true) || addressFrom.equals(address, ignoreCase = true)

    fun isSuccess() = txStatus.equals(Constant.STATUS_SUCCESS, ignoreCase = true)

    companion object {
        internal const val MY_ADDRESS = "MY_ADDRESS"
    }
}