package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.safe
import java.math.BigDecimal

data class BalanceEthData(
    val balanceOfETH: BalanceInfo?,
    val feeTransferERC20: BigDecimal?
) {
    fun getMissingEthAmount(): BigDecimal {
        val balance = balanceOfETH?.balance.safe()
        val missing = feeTransferERC20.safe() - balance
        return Support.toAmountNonDecimal(
            missing,
            balanceOfETH?.decimals ?: 18 //WEI -> ETH
        )
    }
}