package io.mozocoin.sdk.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.contact.AddressAddActivity
import io.mozocoin.sdk.databinding.ViewTransactionDetailsBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import java.math.BigDecimal

internal class TransactionDetailsActivity : BaseActivity() {

    private lateinit var binding: ViewTransactionDetailsBinding
    private var mHistory: TransactionHistory? = null
    private var mPaymentRequest: PaymentRequest? = null

    private var findContactJob: Job? = null
    private var currentBalance = BigDecimal.ZERO
    private var targetAddress: String? = null
    private var mAmount = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mHistory = intent?.getParcelableExtra(KEY_DATA)
        mPaymentRequest = intent?.getParcelableExtra(KEY_DATA_PAYMENT)

        if (mHistory == null && mPaymentRequest == null) {
            finishAndRemoveTask()
            return
        }

        binding = ViewTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.textDetailAmountRateSide.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY
        when {
            mHistory != null -> {
                mAmount = mHistory!!.amountInDecimal()
                setTitle(R.string.mozo_transaction_detail_title)
            }
            mPaymentRequest != null -> {
                mPaymentRequest!!.content ?: return
                val data = Support.parsePaymentRequest(mPaymentRequest!!.content!!)
                targetAddress = data.firstOrNull()
                data.lastOrNull()?.let {
                    mAmount = it.toBigDecimal()
                }

                setTitle(R.string.mozo_payment_request_title)
                binding.buttonPay.apply {
                    visible()
                    click(onPayClicked)
                }
            }
        }

        bindData(MozoWallet.getInstance().getAddress() ?: "")

        MozoSDK.getInstance().contactViewModel.usersLiveData.observe(this) {
            it?.run {
                displayContact()
            }
        }
        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(this) {
            currentBalance = it.balanceNonDecimal
            it?.rate?.run {
                binding.textDetailAmountRateSide.text = MozoSDK.getInstance().profileViewModel
                        .formatCurrencyDisplay(mAmount.multiply(this), true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        findContactJob?.cancel()
        findContactJob = null
    }

    private fun bindData(myAddress: String) {
        var sendType = true
        var detailTime = 0L
        var amountDisplay = ""
        when {
            mHistory != null -> {
                sendType = mHistory!!.type(myAddress)
                binding.textDetailStatus.setText(if (sendType) R.string.mozo_view_text_tx_sent else R.string.mozo_view_text_tx_received)
                targetAddress = if (sendType) mHistory!!.addressTo else mHistory!!.addressFrom

                detailTime = mHistory!!.time * 1000L
                amountDisplay = mHistory!!.amountDisplay()
            }
            mPaymentRequest != null -> {
                mPaymentRequest!!.content ?: return
                val data = Support.parsePaymentRequest(mPaymentRequest!!.content!!)
                targetAddress = data.firstOrNull()

                binding.textDetailStatus.setText(R.string.mozo_button_send_mozo)
                data.lastOrNull()?.let {
                    amountDisplay = it.toBigDecimal().displayString()
                }
            }
        }
        if (sendType) {
            binding.textDetailReceiver.setText(R.string.mozo_view_text_to)
            binding.imageTxType.setBackgroundResource(R.drawable.mozo_bg_icon_send)

        } else {
            binding.textDetailReceiver.setText(R.string.mozo_view_text_from)
            binding.imageTxType.setBackgroundResource(R.drawable.mozo_bg_icon_received)
            binding.imageTxType.rotation = 180f
        }

        binding.textDetailReceiverWalletAddress.text = targetAddress

        if (detailTime > 0) {
            binding.textDetailTime.text = Support.getDisplayDate(
                    this,
                    detailTime,
                    string(R.string.mozo_format_date_time)
            )
            binding.textDetailTime.isVisible = true
        } else binding.textDetailTime.isVisible = false

        binding.textDetailAmountValue.text = amountDisplay

        displayContact()
    }

    private fun displayContact() {
        findContactJob?.cancel()
        findContactJob = GlobalScope.launch {
            val contact = MozoSDK.getInstance().contactViewModel.findByAddress(targetAddress)
            withContext(Dispatchers.Main) {
                val isContact = contact != null
                val isStore = contact?.isStore == true

                binding.buttonSaveAddress.isGone = isContact && !contact?.name.isNullOrEmpty()
                binding.buttonSaveAddressTopLine.isGone = isContact
                binding.imageDetailReceiver.isVisible = isContact

                binding.textDetailReceiverName.isVisible = isContact
                binding.textDetailReceiverPhone.isVisible =
                        isContact && !isStore && !contact?.phoneNo.isNullOrEmpty()
                binding.textDetailStoreAddress.isVisible = isContact && isStore

                if (contact != null) {
                    binding.imageDetailReceiver.setImageResource(
                            if (isStore) R.drawable.ic_store
                            else R.drawable.ic_receiver
                    )
                    binding.textDetailReceiverName.text = contact.name
                    binding.textDetailStoreAddress.text = contact.physicalAddress
                    binding.textDetailReceiverPhone.text = contact.phoneNo

                } else binding.buttonSaveAddress.click {
                    AddressAddActivity.start(this@TransactionDetailsActivity, targetAddress)
                }
            }
            findContactJob = null
        }
    }

    private val onPayClicked: (Button) -> Unit = {
        when {
            mAmount > currentBalance -> {
                MessageDialog.show(this, R.string.mozo_dialog_error_not_enough_msg)
            }
            targetAddress == MozoWallet.getInstance().getAddress() -> {
                MessageDialog.show(it.context, R.string.mozo_transfer_err_send_to_own_wallet)
            }
            else ->
                TransactionFormActivity.start(this, targetAddress, mAmount.toString())
        }
    }

    companion object {
        private const val KEY_DATA = "KEY_DATA"
        private const val KEY_DATA_PAYMENT = "KEY_DATA_PAYMENT"

        fun start(context: Context, history: TransactionHistory) {
            Intent(context, TransactionDetailsActivity::class.java).apply {
                putExtra(KEY_DATA, history)
                context.startActivity(this)
            }
        }

        fun start(context: Context, paymentRequest: PaymentRequest) {
            Intent(context, TransactionDetailsActivity::class.java).apply {
                putExtra(KEY_DATA_PAYMENT, paymentRequest)
                context.startActivity(this)
            }
        }
    }
}