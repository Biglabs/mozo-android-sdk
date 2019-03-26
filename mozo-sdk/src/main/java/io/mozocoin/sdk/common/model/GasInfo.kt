package io.mozocoin.sdk.common.model

import java.math.BigDecimal

data class GasInfo(
        val low: Int,
        val average: Int,
        val fast: Int,
        val gasLimit: BigDecimal
)