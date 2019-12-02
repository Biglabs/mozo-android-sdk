package io.mozocoin.sdk.common

import java.math.BigDecimal

interface OnBalanceChangedListener {
    fun onBalanceChanged(balance: BigDecimal?)
}