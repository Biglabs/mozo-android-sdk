package com.biglabs.mozo.sdk.common

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.*
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.displayString
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

object Models {

    /* Database models */
    @Entity
    data class AnonymousUserInfo(
            @NonNull @PrimaryKey var id: Long = 0L,
            val userId: String,
            val balance: Long = 0L,
            val accessToken: String? = null,
            val refreshToken: String? = null
    )

    @Entity
    data class UserInfo(
            @NonNull @PrimaryKey var id: Long = 0L,
            val userId: String,
            @ColumnInfo(name = "phoneNumber") val phoneNumber: String? = null,
            @ColumnInfo(name = "fullName") val fullName: String? = null
    )

    @Entity(indices = [Index(value = ["id", "userId"], unique = true)])
    data class Profile(
            @PrimaryKey
            var id: Long = 0L,
            val userId: String,
            val status: String? = null,
            @Embedded
            var exchangeInfo: ExchangeInfo? = null,
            @Embedded
            val settings: Settings? = null,
            @Embedded
            var walletInfo: WalletInfo? = null
    )

    data class ExchangeInfo(
            val apiKey: String,
            val depositAddress: String? = null,
            val exchangeId: String? = null,
            val exchangePlatform: String? = null,
            var exchangeSecret: String? = null
    )

    data class Settings(
            val notificationThreshold: Int
    )

    @Suppress("SpellCheckingInspection")
    class WalletInfo {
        var encryptSeedPhrase: String? = null
        var offchainAddress: String? = null
        var privateKey: String? = null
    }

    /* API services models */
    data class Contact(
            val id: Long,
            val name: String?,
            val soloAddress: String?
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readLong(),
                parcel.readString(),
                parcel.readString())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(id)
            parcel.writeString(name)
            parcel.writeString(soloAddress)
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

    data class BalanceInfo(
            val balance: BigDecimal,
            val symbol: String?,
            val decimals: Int,
            val contractAddress: String?
    ) {
        fun balanceNonDecimal(): BigDecimal = Support.toAmountNonDecimal(balance, decimals)
    }

    class TransactionAddress(
            val addresses: ArrayList<String> = arrayListOf()
    )

    class TransactionAddressOutput(
            val addresses: ArrayList<String> = arrayListOf(),
            val value: BigDecimal
    )

    data class TransactionRequest(
            val inputs: ArrayList<TransactionAddress> = arrayListOf(),
            val outputs: ArrayList<TransactionAddressOutput> = arrayListOf()
    )

    data class TransactionResponseData(
            val hash: String? = null,
            val fees: Double,
            val inputs: ArrayList<TransactionAddress>,
            val outputs: ArrayList<TransactionAddressOutput>,
            val data: String,
            @SerializedName("double_spend")
            val doubleSpend: Boolean,
            @SerializedName("gas_price")
            val gasPrice: Double,
            @SerializedName("gas_limit")
            val gasLimit: Double
    )

    @Suppress("SpellCheckingInspection")
    data class TransactionResponse(
            val tx: TransactionResponseData,
            @SerializedName("tosign")
            val toSign: ArrayList<String>,
            var signatures: ArrayList<String>,
            @SerializedName("pubkeys")
            var publicKeys: ArrayList<String>,
            val nonce: Long
    )

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

    data class TransactionStatus(
            val txHash: String,
            val status: String
    ) {
        fun isSuccess() = Constant.STATUS_SUCCESS.equals(status, ignoreCase = true)
        fun isFailed() = Constant.STATUS_FAILED.equals(status, ignoreCase = true)
    }

    data class ExchangeRate(
            val rate: Double
    )

    data class BroadcastData(
            val time: Long,
            val content: String
    ) {
        fun getData() = try {
            Gson().fromJson(content, BroadcastDataContent::class.java)?.apply {
                time = this@BroadcastData.time
            }
        } catch (e: Exception) {
            null
        }
    }

    data class BroadcastDataContent(
            val event: String,
            val from: String,
            val to: String,
            val amount: BigDecimal?,
            val decimal: Int,
            val symbol: String,
            var time: Long,
            val phoneNo: String?,
            val isComeIn: Boolean,
            val storeName: String
    ) {
        override fun toString(): String =
                "{event=$event, from=$from, to=$to, amount=$amount, decimal=$decimal, symbol=$symbol, time=$time, phoneNo=$phoneNo, comeIn=$isComeIn, storeName=$storeName}"
    }

    data class PaymentRequest(
            val date: Long = 0L,
            val toAddress: String?,
            val content: String?
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readLong(),
                parcel.readString(),
                parcel.readString())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(date)
            parcel.writeString(toAddress)
            parcel.writeString(content)
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
}