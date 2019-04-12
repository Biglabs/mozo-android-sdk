package io.mozocoin.sdk.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.TransferSpeed
import io.mozocoin.sdk.common.model.ConvertRequest
import io.mozocoin.sdk.common.model.TransactionResponse
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.activity_convert_broadcast.*
import kotlinx.android.synthetic.main.activity_convert_broadcast_confirm.*
import kotlinx.android.synthetic.main.activity_convert_broadcast_result.*
import kotlinx.android.synthetic.main.activity_convert_broadcast_submit.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

internal class ConvertBroadcastActivity : BaseActivity() {

    private var convertRequest: ConvertRequest? = null
    private var checkStatusJob: Job? = null
    private var isOnChain = true
    private var isCanBack = true

    private var lastTxHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent?.hasExtra(KEY_DATA) == true -> {
                convertRequest = intent.getParcelableExtra(KEY_DATA)
                isOnChain = intent.getBooleanExtra(KEY_CONVERT_ON_CHAIN, isOnChain)
            }
            intent?.hasExtra(KEY_DATA_TX_HASH) == true -> lastTxHash = intent.getStringExtra(KEY_DATA_TX_HASH)
            else -> {
                finish()
                return
            }
        }

        setContentView(R.layout.activity_convert_broadcast)
        updateUI()

        button_confirm.click {
            sendConvertRequest()
        }

        button_hide.click {
            EventBus.getDefault().post(MessageEvent.CloseActivities())
        }

        button_back_to_wallet.click {
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
            val amountInDecimal = SharedPrefsUtils.getLastAmountConvertOnChainInOffChain()?.toBigDecimal().safe()
            val amount = MozoTx.getInstance().amountNonDecimal(amountInDecimal)
            convert_broadcast_result_amount?.text = amount.displayString()
            convert_broadcast_result_amount_rate?.text = MozoWallet.getInstance().amountInCurrency(amount)

            checkConvertStatus(lastTxHash)
            return
        }
        convertRequest ?: return

        updateContainerUI(FLOW_STEP_CONFIRM)
        val amount = MozoTx.getInstance().amountNonDecimal(convertRequest!!.value)
        val amountDisplay = amount.displayString()
        val amountCurrency = MozoWallet.getInstance().amountInCurrency(amount)
        convert_amount_on_chain?.text = amountDisplay
        convert_amount_on_chain_rate.text = amountCurrency
        convert_amount_off_chain?.text = amountDisplay
        convert_amount_off_chain_rate.text = amountCurrency

        convert_broadcast_result_amount?.text = amountDisplay
        convert_broadcast_result_amount_rate?.text = amountCurrency

        convert_gas_limit?.text = convertRequest!!.gasLimit.displayString()
        convert_gas_price?.text = convertRequest!!.gasPrice.toGwei().displayString()
        convert_gas_price_speed?.setText(TransferSpeed.calculate(convertRequest!!.gasPriceProgress).display)
    }

    private fun showLoading(show: Boolean) {
        convert_loading_container?.isVisible = show
    }

    private fun sendConvertRequest() {
        showLoading(true)
        MozoAPIsService.getInstance().prepareConvertRequest(
                this,
                convertRequest ?: return,
                { data, _ ->
                    showLoading(data != null)
                    data ?: return@prepareConvertRequest
                    signConvertRequest(data)
                },
                this::sendConvertRequest
        )
    }

    private fun signConvertRequest(data: TransactionResponse) {
        val onReceiveMessage: (String, String, String) -> Unit = { _, signature, publicKey ->
            data.signatures = arrayListOf(signature)
            data.publicKeys = arrayListOf(publicKey)

            submitRequest(data)
        }

        if (isOnChain) {
            MozoTx.getInstance().signOnChainMessage(this, data.toSign.first(), onReceiveMessage)
        } else {
            MozoTx.getInstance().signMessage(this, data.toSign.first(), onReceiveMessage)
        }
    }

    private fun submitRequest(response: TransactionResponse) {
        showLoading(false)
        updateContainerUI(FLOW_STEP_WAITING)
        convert_broadcast_submit_title?.setText(R.string.mozo_convert_submit_broadcasting_title)
        checkStatusJob?.cancel()
        isCanBack = false

        MozoAPIsService.getInstance().signConvertRequest(this, response, { data, _ ->
            if (data == null) {
                updateContainerUI(FLOW_STEP_CONFIRM)
                isCanBack = true
                return@signConvertRequest
            }

            if (!isOnChain) {
                SharedPrefsUtils.setLastInfoConvertOnChainInOffChain(data.tx.hash, convertRequest?.value.toString())
            }
            convert_broadcast_submit_title?.setText(R.string.mozo_convert_submit_broadcast_title)
            checkConvertStatus(data.tx.hash)
        }, {
            submitRequest(response)
        })
    }

    private fun checkConvertStatus(hash: String?) {
        checkStatusJob?.cancel()

        if (hash.isNullOrEmpty()) {
            updateResultUI(false, hash)
            return
        }
        updateContainerUI(FLOW_STEP_WAITING)
        checkStatusJob = GlobalScope.launch(Dispatchers.Main) {
            delay(2000)

            convert_broadcast_submit_title?.setText(R.string.mozo_convert_submit_waiting_title)

            MozoAPIsService.getInstance().getConvertStatus(this@ConvertBroadcastActivity, hash, { data, _ ->
                data ?: return@getConvertStatus
                if (data.isSuccess() || data.isFailed()) {
                    checkStatusJob?.cancel()
                    checkStatusJob = null
                    updateResultUI(data.isSuccess(), data.txHash)

                } else /* PENDING */ {
                    checkConvertStatus(hash)
                }
            }, {
                checkConvertStatus(hash)
            })
        }
    }

    private fun updateResultUI(success: Boolean, hash: String?) {
        if (!isOnChain) {
            SharedPrefsUtils.setLastInfoConvertOnChainInOffChain(null, null)
        }
        updateContainerUI(FLOW_STEP_RESULT)

        if (success) {
            convert_broadcast_result_icon?.setImageResource(R.drawable.ic_send_complete)
            convert_broadcast_result_title?.setText(R.string.mozo_convert_submit_success_title)
            convert_broadcast_result_content?.setText(R.string.mozo_convert_submit_success_content)
            convert_broadcast_result_hash?.text = hash
            convert_broadcast_result_hash?.click {
                openTab(StringBuilder("https://")
                        .append(Support.domainEhterscan())
                        .append("/tx/")
                        .append(it.text)
                        .toString()
                )
            }
            convert_broadcast_result_amount_group?.visible()
        } else {
            convert_broadcast_result_icon?.setImageResource(R.drawable.ic_send_failed)
            convert_broadcast_result_title?.setText(R.string.mozo_convert_submit_fail_title)
            convert_broadcast_result_content?.setText(R.string.mozo_convert_submit_fail_content)
            convert_broadcast_result_amount_group?.visibility = View.INVISIBLE
        }
    }

    private fun updateContainerUI(step: Int) {
        convert_broadcast_flipper?.displayedChild = step
        convert_broadcast_toolbar?.showBackButton(step == FLOW_STEP_CONFIRM)
        convert_broadcast_toolbar?.showCloseButton(step == FLOW_STEP_CONFIRM)
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