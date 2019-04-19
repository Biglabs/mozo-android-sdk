package io.mozocoin.sdk.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.TransferSpeed
import io.mozocoin.sdk.common.model.BalanceInfo
import io.mozocoin.sdk.common.model.ConvertRequest
import io.mozocoin.sdk.common.model.GasInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.activity_convert_on_in_off.*
import java.math.BigDecimal

internal class ConvertOnInOffActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private var mGasInfo: GasInfo? = null
    private var mGasPrice: BigDecimal = BigDecimal.ZERO

    private var mBalanceOfEthInWei: BigDecimal = BigDecimal.ZERO
    private var mBalanceOfOnchain: BalanceInfo? = null
    private var mAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert_on_in_off)

        if (!intent.hasExtra(KEY_CONVERT_ADDRESS) || !intent.hasExtra(KEY_CONVERT_BALANCE)) {
            finish()
            return
        }

        mAddress = intent.getStringExtra(KEY_CONVERT_ADDRESS)
        mBalanceOfOnchain = intent.getParcelableExtra(KEY_CONVERT_BALANCE)

        convert_on_chain_swipe_refresh?.apply {
            mozoSetup()
            setOnRefreshListener(this@ConvertOnInOffActivity)
        }
        convert_on_chain_amount.text = mBalanceOfOnchain?.balanceNonDecimal().displayString()
        convert_on_chain_amount_rate?.text = MozoWallet.getInstance().amountInCurrency(mBalanceOfOnchain?.balance.safe())

        convert_gas_price_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mGasInfo ?: return
                mGasPrice = BigDecimal((progress / 100.0) * (mGasInfo!!.fast - mGasInfo!!.low) + mGasInfo!!.low)
                        .setScale(0, BigDecimal.ROUND_HALF_UP)
                updateGasPriceUI()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        button_continue.click {
            ConvertBroadcastActivity.start(
                    this,
                    prepareRequestData() ?: return@click,
                    isOnChain = false
            )
        }

        button_read_more.click {
            //openTab("https://kb.myetherwallet.com/gas/what-is-gas-ethereum.html")
        }

        fetchData()
    }

    override fun onRefresh() {
        fetchData()
    }

    private fun fetchData() {
        showLoading(true)
        MozoAPIsService.getInstance().getGasInfo(this, { data, _ ->
            if (data == null) {
                showLoading(false)
                return@getGasInfo
            }

            mGasInfo = data
            convert_gas_limit?.text = data.gasLimit.displayString()

            val normalPercent = (data.average.toFloat() - data.low) / (data.fast.toFloat() - data.low) * 100

            val params = convert_gas_price_seek_normal?.layoutParams as ConstraintLayout.LayoutParams
            params.horizontalBias = normalPercent / 100
            convert_gas_price_seek_normal?.layoutParams = params
            convert_gas_price_seek?.progress = 100

            mGasPrice = BigDecimal(data.fast)
            fetchBalanceData()
            updateGasPriceUI()

        }, this::fetchData)
    }

    private fun fetchBalanceData() {
        showLoading(true)
        MozoAPIsService.getInstance().getEthBalanceInOffChain(this, mAddress ?: return, { data, _ ->
            showLoading(false)
            data ?: return@getEthBalanceInOffChain

            mBalanceOfEthInWei = data.balanceOfETH?.balance ?: return@getEthBalanceInOffChain
            convert_on_chain_eth_balance?.apply {
                if (mBalanceOfEthInWei <= BigDecimal.ZERO) {
                    setBackgroundResource(R.drawable.mozo_dr_hint_error)
                    isSelected = true
                    text = HtmlCompat.fromHtml(getString(
                            R.string.mozo_convert_on_in_off_warning,
                            data.balanceOfETH.balanceNonDecimal().displayString(),
                            data.estimateFeeInEth().displayString()
                    ), HtmlCompat.FROM_HTML_MODE_LEGACY)

                    button_continue?.isEnabled = false
                } else {
                    setBackgroundResource(R.drawable.mozo_dr_btn_detected_on_chain)
                    isSelected = false
                    text = HtmlCompat.fromHtml(getString(
                            R.string.mozo_convert_on_in_off_eth_balance,
                            data.balanceOfETH.balanceNonDecimal().displayString()
                    ), HtmlCompat.FROM_HTML_MODE_LEGACY)

                    button_continue?.isEnabled = true
                }
                isVisible = true
            }

        }, this::fetchData)
    }

    private fun updateGasPriceUI() {
        convert_gas_price?.text = mGasPrice.displayString()

        convert_gas_price_seek_slow.highlight(false)
        convert_gas_price_seek_normal.highlight(false)
        convert_gas_price_seek_fast.highlight(false)
        when (TransferSpeed.calculate(convert_gas_price_seek?.progress ?: 0)) {
            TransferSpeed.SLOW -> {
                convert_gas_price_seek_slow.highlight(true)
            }
            TransferSpeed.NORMAL -> {
                convert_gas_price_seek_normal.highlight(true)
            }
            TransferSpeed.FAST -> {
                convert_gas_price_seek_fast.highlight(true)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        convert_on_chain_swipe_refresh?.isRefreshing = show
    }

    private fun prepareRequestData(): ConvertRequest? {
        val gasPrice = mGasPrice.toWei()
        when {
            gasPrice > mBalanceOfEthInWei -> {
                MessageDialog.show(this, R.string.mozo_transfer_amount_eth_not_enough)
                return null
            }
        }

        val address = mAddress ?: return null
        val amount = mBalanceOfOnchain?.balance ?: return null

        val gasLimit = mGasInfo?.gasLimit.safe()
        return ConvertRequest(
                address,
                gasLimit,
                gasPrice,
                address,
                amount,
                convert_gas_price_seek?.progress ?: 0
        )
    }

    companion object {

        private const val KEY_CONVERT_ADDRESS = "KEY_CONVERT_ADDRESS"
        private const val KEY_CONVERT_BALANCE = "KEY_CONVERT_BALANCE"

        fun start(context: Context, address: String, balance: BalanceInfo) {
            context.startActivity(
                    Intent(context, ConvertOnInOffActivity::class.java)
                            .putExtra(KEY_CONVERT_ADDRESS, address)
                            .putExtra(KEY_CONVERT_BALANCE, balance)
            )
        }
    }
}