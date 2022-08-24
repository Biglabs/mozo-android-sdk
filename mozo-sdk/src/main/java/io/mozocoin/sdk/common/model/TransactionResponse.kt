package io.mozocoin.sdk.common.model

import com.google.gson.annotations.SerializedName

@Suppress("SpellCheckingInspection")
data class TransactionResponse(
    val tx: TransactionResponseData,
    @SerializedName("tosign")
    val toSign: ArrayList<String>,
    var signatures: ArrayList<String>,
    @SerializedName("pubkeys")
    var publicKeys: ArrayList<String>,
    val nonce: Long,
    var additionalData: String?
)