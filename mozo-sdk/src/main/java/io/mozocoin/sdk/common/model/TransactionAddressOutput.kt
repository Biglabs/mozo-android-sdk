package io.mozocoin.sdk.common.model

import java.math.BigDecimal

class TransactionAddressOutput(
        val addresses: ArrayList<String> = arrayListOf(),
        val value: BigDecimal
)