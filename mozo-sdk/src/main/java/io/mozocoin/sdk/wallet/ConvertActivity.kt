package io.mozocoin.sdk.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.TransferSpeed
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.ConvertRequest
import io.mozocoin.sdk.common.model.GasInfo
import io.mozocoin.sdk.common.model.WalletInfo
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivityConvertBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import java.math.BigDecimal
import java.util.*

internal class ConvertActivity : BaseActivity() {

    private lateinit var binding: ActivityConvertBinding
    private val wallet: WalletInfo? by lazy {
        MozoWallet.getInstance().getWallet()?.buildWalletInfo()
    }
    private var isConvertOn2Off = true
    private var mGasInfo: GasInfo? = null
    private var mGasPrice: BigDecimal = BigDecimal.ZERO

    private var mBalanceOfEthInWei: BigDecimal = BigDecimal.ZERO
    private var mCurrentBalance: BigDecimal = BigDecimal.ZERO

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> { bar ->
        binding.inputConvertAmount.filters = arrayOf<InputFilter>(
            DecimalDigitsInputFilter(
                12,
                bar?.decimal ?: Constant.DEFAULT_DECIMAL
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isConvertOn2Off = intent.getBooleanExtra(MODE_ON_2_OFF, isConvertOn2Off)
        binding = ActivityConvertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.convertToolbar.setTitle(
            if (isConvertOn2Off) R.string.mozo_button_convert_2_off
            else R.string.mozo_button_convert_2_on
        )

        if (!isConvertOn2Off) {
            binding.scrollContainer.children.forEach { it.gone() }
            binding.inputGroup.visible()
        }

        binding.inputConvertAmount.onTextChanged {
            when {
                it.isNullOrEmpty() -> {
                    binding.inputConvertAmountRate.text = ""
                    binding.buttonContinue.isEnabled = false

                }
                it.startsWith(".") -> {
                    binding.inputConvertAmount.setText(String.format(Locale.US, "0%s", it))
                    binding.inputConvertAmount.setSelection(it.length + 1)

                }
                else -> {
                    val amount = BigDecimal(it.toString())
                    binding.inputConvertAmountRate.text =
                        MozoWallet.getInstance().amountInCurrency(amount)
                    binding.buttonContinue.isEnabled = true
                }
            }
        }
        binding.inputConvertAmountRate.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY

        binding.convertGasPriceSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mGasInfo ?: return
                mGasPrice =
                    BigDecimal((progress / 100.0) * (mGasInfo!!.fast - mGasInfo!!.low) + mGasInfo!!.low)
                        .setScale(0, BigDecimal.ROUND_HALF_UP)
                updateGasPriceUI()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        binding.convertGasPriceSeekSlow.click {
            binding.convertGasPriceSeek.progress = 0
        }
        binding.convertGasPriceSeekNormal.click {
            binding.convertGasPriceSeek.progress = 30
        }
        binding.convertGasPriceSeekFast.click {
            binding.convertGasPriceSeek.progress = 100
        }
        binding.buttonContinue.click {
            ConvertBroadcastActivity.start(
                this,
                prepareRequestData() ?: return@click,
                isOnChain = false
            )
        }

        binding.buttonReadMore.click {
            //openTab("https://kb.myetherwallet.com/gas/what-is-gas-ethereum.html")
        }

        fetchData()
    }

    override fun onResume() {
        super.onResume()
        MozoSDK.getInstance().profileViewModel
            .balanceAndRateLiveData
            .observe(this, balanceAndRateObserver)
    }

    override fun onPause() {
        super.onPause()
        MozoSDK.getInstance().profileViewModel
            .balanceAndRateLiveData
            .removeObservers(this)
    }

    private fun updateGasPriceUI() {
        binding.convertGasPrice.text = mGasPrice.displayString()

        binding.convertGasPriceSeekSlow.highlight(false)
        binding.convertGasPriceSeekNormal.highlight(false)
        binding.convertGasPriceSeekFast.highlight(false)
        when (TransferSpeed.calculate(binding.convertGasPriceSeek.progress)) {
            TransferSpeed.SLOW -> {
                binding.convertGasPriceSeekSlow.highlight(true)
            }
            TransferSpeed.NORMAL -> {
                binding.convertGasPriceSeekNormal.highlight(true)
            }
            TransferSpeed.FAST -> {
                binding.convertGasPriceSeekFast.highlight(true)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.convertLoadingContainer.isVisible = show
    }

    private fun fetchData() {
        showLoading(true)
        MozoAPIsService.getInstance().getGasInfo(this, { data, _ ->
            showLoading(false)
            data ?: return@getGasInfo

            mGasInfo = data
            binding.convertGasLimit.text = data.gasLimit.displayString()

            val normalPercent =
                (data.average.toFloat() - data.low) / (data.fast.toFloat() - data.low) * 100
            binding.convertGasPriceSeek.progress = normalPercent.toInt()

            val params =
                binding.convertGasPriceSeekNormal.layoutParams as ConstraintLayout.LayoutParams
            params.horizontalBias = normalPercent / 100
            binding.convertGasPriceSeekNormal.layoutParams = params

            mGasPrice = BigDecimal(data.average)
            updateGasPriceUI()

        }, this::fetchData)

        if (isConvertOn2Off) {
            MozoAPIsService.getInstance().getOnChainBalance(
                this,
                wallet?.onchainAddress ?: return,
                { data, _ ->
                    showLoading(false)
                    data ?: return@getOnChainBalance

                    mBalanceOfEthInWei = data.balanceOfETH?.balance.safe()
                    mCurrentBalance = data.balanceOfToken?.balanceNonDecimal().safe()

                    Support.formatSpendableText(
                        binding.outputAmountSpendable,
                        mCurrentBalance.displayString(),
                        true
                    )

                    binding.inputConvertAmount.showKeyboard()
                }, this::fetchData
            )
        } else {
            MozoWallet.getInstance().getBalance { balance, _ ->
                mCurrentBalance = balance
                Support.formatSpendableText(
                    binding.outputAmountSpendable,
                    balance.displayString(),
                    false
                )
                binding.inputConvertAmount.showKeyboard()
            }
        }
    }

    private fun prepareRequestData(): ConvertRequest? {
        val amount = binding.inputConvertAmount.text?.toString()?.toBigDecimal().safe()
        val gasPrice = mGasPrice.toWei()
        when {
            amount <= BigDecimal.ZERO -> {
                MessageDialog.show(this, R.string.mozo_transfer_amount_error_too_low)
                return null
            }
            amount > mCurrentBalance -> {
                MessageDialog.show(this, R.string.mozo_transfer_amount_error_not_enough)
                return null
            }
            !isConvertOn2Off && amount > MAX_CONVERT_OFF_2_ON -> {
                MessageDialog.show(
                    this,
                    getString(R.string.mozo_convert_reach_limit, MAX_CONVERT_OFF_2_ON.displayString())
                )
                return null
            }
            isConvertOn2Off && gasPrice > mBalanceOfEthInWei -> {
                MessageDialog.show(this, R.string.mozo_transfer_amount_eth_not_enough)
                return null
            }
        }

        if (
            wallet?.offchainAddress != null &&
            wallet?.onchainAddress != null
        ) {
            val gasLimit = mGasInfo?.gasLimit.safe()
            val finalAmount = MozoTx.instance().amountWithDecimal(amount)
            return ConvertRequest(
                wallet?.onchainAddress!!,
                gasLimit,
                gasPrice,
                wallet?.offchainAddress!!,
                finalAmount,
                binding.convertGasPriceSeek.progress,
                on2Off = isConvertOn2Off
            )
        }
        return null
    }

    companion object {
        private const val MODE_ON_2_OFF = "MODE_ON_2_OFF"
        private val MAX_CONVERT_OFF_2_ON = BigDecimal("50000000")

        private fun start(context: Context, isOn2Off: Boolean) =
            Intent(context, ConvertActivity::class.java).apply {
                putExtra(MODE_ON_2_OFF, isOn2Off)
            }.let {
                context.startActivity(it)
            }

        fun startForOn2Off(context: Context) = start(context, true)

        fun startForOff2On(context: Context) = start(context, false)
    }
}