package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.safe
import java.math.BigDecimal

data class BalanceEthData(
        val balanceOfETH: BalanceInfo?,
        val feeTransferERC20: BigDecimal?
) {
    fun estimateFeeInEth() = Support.toAmountNonDecimal(
            feeTransferERC20.safe(),
            balanceOfETH?.decimals ?: 0
    )
}