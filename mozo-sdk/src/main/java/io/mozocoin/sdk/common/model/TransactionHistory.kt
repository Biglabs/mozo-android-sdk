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
        var action: String? = null,
        var amount: BigDecimal,
        var addressFrom: String? = null,
        var addressTo: String? = null,
        var blockHeight: Long,
        var contractAddress: String? = null,
        var contractAction: String? = null,
        var decimal: Int = Constant.DEFAULT_DECIMAL,
        var symbol: String? = null,
        var fees: Double,
        var time: Long,
        var topUpReason: String? = null,
        var txHash: String? = null,
        var txStatus: String? = null
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