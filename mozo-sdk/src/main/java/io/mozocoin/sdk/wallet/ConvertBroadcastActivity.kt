package io.mozocoin.sdk.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.TransferSpeed
import io.mozocoin.sdk.common.model.ConvertRequest
import io.mozocoin.sdk.common.model.TransactionResponse
import io.mozocoin.sdk.common.model.TransactionStatus
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivityConvertBroadcastBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

internal class ConvertBroadcastActivity : BaseActivity() {
    private lateinit var binding: ActivityConvertBroadcastBinding
    private var convertRequest: ConvertRequest? = null
    private var checkStatusJob: Job? = null
    private var isOnChain = true
    private var isCanBack = true
    private var isConvertingOn2Off = true

    private var lastTxHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent?.hasExtra(KEY_DATA) == true -> {
                convertRequest = intent.getParcelableExtra(KEY_DATA)
                isOnChain = intent.getBooleanExtra(KEY_CONVERT_ON_CHAIN, isOnChain)
            }
            intent?.hasExtra(KEY_DATA_TX_HASH) == true -> lastTxHash =
                intent.getStringExtra(KEY_DATA_TX_HASH)
            else -> {
                finish()
                return
            }
        }
        isConvertingOn2Off = (convertRequest == null || convertRequest!!.on2Off)

        binding = ActivityConvertBroadcastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.flipPageConfirm.convertAmountOnChainRate.isVisible =
            Constant.SHOW_MOZO_EQUIVALENT_CURRENCY
        binding.flipPageConfirm.convertAmountOffChainRate.isVisible =
            Constant.SHOW_MOZO_EQUIVALENT_CURRENCY
        binding.flipPageResult.convertBroadcastResultAmountRate.isVisible =
            Constant.SHOW_MOZO_EQUIVALENT_CURRENCY

        updateUI()

        binding.flipPageConfirm.buttonConfirm.click {
            sendConvertRequest()
        }

        binding.flipPageSubmit.buttonHide.click {
            EventBus.getDefault().post(MessageEvent.CloseActivities())
        }

        binding.flipPageResult.buttonBackToWallet.click {
            EventBus.getDefault().post(MessageEvent.CloseActivities())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkStatusJob?.cancel()
        checkStatusJob = null
    }

    override fun onBackPressed() {
        if (isCanBack) {
            super.onBackPressed()
        }
    }

    private fun updateUI() {
        if (!lastTxHash.isNullOrEmpty()) {
            val amountInDecimal =
                SharedPrefsUtils.getLastAmountConvertOnChainInOffChain()?.toBigDecimal().safe()
            val amount = MozoTx.instance().amountNonDecimal(amountInDecimal)
            binding.flipPageResult.convertBroadcastResultAmount.text = amount.displayString()
            binding.flipPageResult.convertBroadcastResultAmountRate.text =
                MozoWallet.getInstance().amountInCurrency(amount)

            checkConvertStatus(lastTxHash)
            return
        }
        convertRequest ?: return

        val convertingTitle = if (isConvertingOn2Off) R.string.mozo_button_convert_2_off
        else R.string.mozo_button_convert_2_on
        binding.convertBroadcastToolbar.setTitle(convertingTitle)

        updateContainerUI(FLOW_STEP_CONFIRM)
        binding.flipPageConfirm.convertHeadLine.setText(convertingTitle)
        binding.flipPageConfirm.convertFromLabel.setText(
            if (isConvertingOn2Off) R.string.mozo_convert_from_on_chain
            else R.string.mozo_convert_from_off_chain
        )
        binding.flipPageConfirm.convertAmountOnChain.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isConvertingOn2Off) R.drawable.ic_mozo_onchain else R.drawable.ic_mozo_offchain,
            0,
            0,
            0
        )
        binding.flipPageConfirm.convertToLabel.setText(
            if (isConvertingOn2Off) R.string.mozo_convert_to_off_chain
            else R.string.mozo_convert_to_on_chain
        )
        binding.flipPageConfirm.convertAmountOffChain.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isConvertingOn2Off) R.drawable.ic_mozo_offchain else R.drawable.ic_mozo_onchain,
            0,
            0,
            0
        )
        binding.flipPageConfirm.gasGroup.isVisible = isConvertingOn2Off

        val amount = MozoTx.instance().amountNonDecimal(convertRequest!!.value)
        val amountDisplay = amount.displayString()
        val amountCurrency = MozoWallet.getInstance().amountInCurrency(amount)
        binding.flipPageConfirm.convertAmountOnChain.text = amountDisplay
        binding.flipPageConfirm.convertAmountOnChainRate.text = amountCurrency
        binding.flipPageConfirm.convertAmountOffChain.text = amountDisplay
        binding.flipPageConfirm.convertAmountOffChainRate.text = amountCurrency

        binding.flipPageResult.convertBroadcastResultAmount.text = amountDisplay
        binding.flipPageResult.convertBroadcastResultAmountRate.text = amountCurrency
        binding.flipPageResult.convertBroadcastResultAmount.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isConvertingOn2Off) R.drawable.ic_mozo_offchain else R.drawable.ic_mozo_onchain,
            0, 0, 0
        )

        binding.flipPageConfirm.convertGasLimit.text = convertRequest!!.gasLimit.displayString()
        binding.flipPageConfirm.convertGasPrice.text =
            convertRequest!!.gasPrice.toGwei().displayString()
        binding.flipPageConfirm.convertGasPriceSpeed.setText(TransferSpeed.calculate(convertRequest!!.gasPriceProgress).display)
    }

    private fun showLoading(show: Boolean) {
        binding.convertLoadingContainer.isVisible = show
    }

    private fun sendConvertRequest() {
        convertRequest ?: return
        showLoading(true)

        val callback: (data: TransactionResponse?, errorCode: String?) -> Unit = { data, _ ->
            showLoading(data != null)
            if (data != null) signConvertRequest(data)
        }

        if (convertRequest!!.on2Off) {
            MozoAPIsService.getInstance().prepareConvertRequest(
                this,
                convertRequest!!,
                callback,
                this::sendConvertRequest
            )
        } else {
            MozoAPIsService.getInstance().prepareConvertOff2On(
                this,
                convertRequest!!.value,
                convertRequest!!.toAddress,
                callback,
                this::sendConvertRequest
            )
        }
    }

    private fun signConvertRequest(data: TransactionResponse) {
        val onReceiveMessage: (String, String, String) -> Unit = { _, signature, publicKey ->
            data.signatures = arrayListOf(signature)
            data.publicKeys = arrayListOf(publicKey)

            submitRequest(data)
        }

        if (isOnChain) {
            MozoTx.instance().signOnChainMessage(this, data.toSign.first(), onReceiveMessage)
        } else {
            MozoTx.instance().signMessage(this, data.toSign.first(), onReceiveMessage)
        }
    }

    private fun submitRequest(response: TransactionResponse) {
        showLoading(false)
        updateContainerUI(FLOW_STEP_WAITING)
        binding.flipPageSubmit.convertBroadcastSubmitTitle.setText(R.string.mozo_convert_submit_broadcasting_title)
        checkStatusJob?.cancel()
        isCanBack = false

        val callback: (data: TransactionResponse?, errorCode: String?) -> Unit = { data, _ ->
            if (data == null) {
                updateContainerUI(FLOW_STEP_CONFIRM)
                isCanBack = true
            } else {

                if (!isOnChain) {
                    SharedPrefsUtils.setLastInfoConvertOnChainInOffChain(
                        data.tx.hash,
                        convertRequest?.value.toString()
                    )
                }
                binding.flipPageSubmit.convertBroadcastSubmitTitle.setText(R.string.mozo_convert_submit_broadcast_title)
                checkConvertStatus(data.tx.hash)
            }
        }

        if (convertRequest?.on2Off == true) {
            MozoAPIsService.getInstance().signConvertRequest(this, response, callback, {
                submitRequest(response)
            })
        } else {
            MozoAPIsService.getInstance().sendTransaction(this, response, callback, {
                submitRequest(response)
            })
        }
    }

    private fun checkConvertStatus(hash: String?) {
        checkStatusJob?.cancel()

        if (hash.isNullOrEmpty()) {
            updateResultUI(false, hash)
            return
        }
        updateContainerUI(FLOW_STEP_WAITING)

        val callback: (data: TransactionStatus?, errorCode: String?) -> Unit = { data, _ ->
            if (data != null && (data.isSuccess() || data.isFailed())) {
                checkStatusJob?.cancel()
                checkStatusJob = null
                updateResultUI(data.isSuccess(), data.txHash)

            } else /* PENDING */ {
                checkConvertStatus(hash)
            }
        }
        checkStatusJob = MainScope().launch {
            delay(2000)

            binding.flipPageSubmit.convertBroadcastSubmitTitle.setText(R.string.mozo_convert_submit_waiting_title)
            if (convertRequest?.on2Off == true) {
                MozoAPIsService.getInstance()
                    .getConvertStatus(this@ConvertBroadcastActivity, hash, callback, {
                        checkConvertStatus(hash)
                    })
            } else {
                MozoAPIsService.getInstance()
                    .getTxStatus(this@ConvertBroadcastActivity, hash, callback)
            }
        }
    }

    private fun updateResultUI(success: Boolean, hash: String?) {
        if (!isOnChain) {
            SharedPrefsUtils.setLastInfoConvertOnChainInOffChain(null, null)
        }
        updateContainerUI(FLOW_STEP_RESULT)

        if (success) {
            binding.flipPageResult.convertBroadcastResultIcon.setImageResource(R.drawable.ic_send_complete)
            binding.flipPageResult.convertBroadcastResultTitle.setText(R.string.mozo_convert_submit_success_title)
            binding.flipPageResult.convertBroadcastResultContent.setText(
                if (isConvertingOn2Off) R.string.mozo_convert_submit_success_on_2_off
                else R.string.mozo_convert_submit_success_off_2_on
            )
            binding.flipPageResult.convertBroadcastResultHash.text = hash
            binding.flipPageResult.convertBroadcastResultHash.click {
                openTab(
                    StringBuilder("https://")
                        .append(Support.domainEhterscan())
                        .append("/tx/")
                        .append(it.text)
                        .toString()
                )
            }
            binding.flipPageResult.convertBroadcastResultAmountGroup.visible()
            binding.flipPageResult.convertBroadcastResultAmountRate.isVisible =
                Constant.SHOW_MOZO_EQUIVALENT_CURRENCY
            binding.flipPageResult.convertBroadcastResultHashLabel.isVisible = isConvertingOn2Off
            binding.flipPageResult.convertBroadcastResultHash.isVisible = isConvertingOn2Off
        } else {
            binding.flipPageResult.convertBroadcastResultIcon.setImageResource(R.drawable.ic_send_failed)
            binding.flipPageResult.convertBroadcastResultTitle.setText(R.string.mozo_convert_submit_fail_title)
            binding.flipPageResult.convertBroadcastResultContent.setText(
                if (isConvertingOn2Off) R.string.mozo_convert_submit_fail_on_2_off
                else R.string.mozo_convert_submit_success_off_2_on
            )
            binding.flipPageResult.convertBroadcastResultAmountGroup.visibility = View.INVISIBLE
        }
    }

    private fun updateContainerUI(step: Int) {
        binding.convertBroadcastFlipper.displayedChild = step
        binding.convertBroadcastToolbar.showBackButton(step == FLOW_STEP_CONFIRM)
        binding.convertBroadcastToolbar.showCloseButton(step == FLOW_STEP_CONFIRM)
    }

    companion object {
        private const val FLOW_STEP_CONFIRM = 0
        private const val FLOW_STEP_WAITING = 1
        private const val FLOW_STEP_RESULT = 2

        private const val KEY_DATA = "KEY_DATA"
        private const val KEY_DATA_TX_HASH = "KEY_DATA_TX_HASH"
        private const val KEY_CONVERT_ON_CHAIN = "KEY_CONVERT_ON_CHAIN"

        fun start(context: Context, request: ConvertRequest, isOnChain: Boolean = true) {
            context.startActivity(
                Intent(context, ConvertBroadcastActivity::class.java)
                    .putExtra(KEY_DATA, request)
                    .putExtra(KEY_CONVERT_ON_CHAIN, isOnChain)
            )
        }

        fun start(context: Context, txHash: String) {
            context.startActivity(
                Intent(context, ConvertBroadcastActivity::class.java)
                    .putExtra(KEY_DATA_TX_HASH, txHash)
            )
        }
    }
}