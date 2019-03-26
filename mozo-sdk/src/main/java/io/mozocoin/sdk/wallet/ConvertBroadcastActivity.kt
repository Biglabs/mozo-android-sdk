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
    private var isCanBack = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.hasExtra(KEY_DATA) == true) {
            convertRequest = intent.getParcelableExtra(KEY_DATA)
        } else {
            finish()
            return
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
        convertRequest ?: return

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
        MozoTx.getInstance().signOnChainMessage(this, data.toSign.first()) { _, signature, publicKey ->
            data.signatures = arrayListOf(signature)
            data.publicKeys = arrayListOf(publicKey)

            submitRequest(data)
        }
    }

    private fun submitRequest(response: TransactionResponse) {
        showLoading(false)
        convert_broadcast_flipper?.showNext()
        convert_broadcast_toolbar?.showBackButton(false)
        convert_broadcast_toolbar?.showCloseButton(false)
        checkStatusJob?.cancel()
        isCanBack = false

        MozoAPIsService.getInstance().signConvertRequest(this, response, { data, _ ->
            convert_broadcast_submit_title?.setText(R.string.mozo_convert_submit_broadcast_title)

            checkConvertStatus(data?.tx?.hash)
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
        convert_broadcast_flipper?.showNext()
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

    companion object {
        private const val KEY_DATA = "KEY_DATA"

        fun start(context: Context, request: ConvertRequest) {
            context.startActivity(
                    Intent(context, ConvertBroadcastActivity::class.java)
                            .putExtra(KEY_DATA, request)
            )
        }
    }
}