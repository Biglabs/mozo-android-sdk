package io.mozocoin.sdk.common.model

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Ignore
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.utils.displayString
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
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
    var txStatus: String? = null,
    var filter: Int = 0
) : Parcelable {

    @Ignore
    @IgnoredOnParcel
    var contactName: String? = null

    fun amountInDecimal(): BigDecimal = amount.divide(10.0.pow(decimal.toDouble()).toBigDecimal())

    fun amountDisplay(): String = amountInDecimal().displayString()

    fun type(address: String?): Boolean =
        addressFrom.equals(MY_ADDRESS, ignoreCase = true) || addressFrom.equals(
            address,
            ignoreCase = true
        )

    fun isSuccess() = txStatus.equals(Constant.STATUS_SUCCESS, ignoreCase = true)

    class DiffCallback(
        private val oldList: MutableList<TransactionHistory>,
        private val newList: MutableList<TransactionHistory>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].txHash == newList[newItemPosition].txHash

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].amount == newList[newItemPosition].amount
                    && oldList[oldItemPosition].addressFrom == newList[newItemPosition].addressFrom
                    && oldList[oldItemPosition].addressTo == newList[newItemPosition].addressTo
                    && oldList[oldItemPosition].time == newList[newItemPosition].time
                    && oldList[oldItemPosition].txStatus == newList[newItemPosition].txStatus
                    && oldList[oldItemPosition].filter == newList[newItemPosition].filter

        override fun getChangePayload(
            oldItemPosition: Int,
            newItemPosition: Int
        ): TransactionHistory {
            return newList[newItemPosition]
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size
    }

    companion object {
        internal const val MY_ADDRESS = "MY_ADDRESS"
    }
}