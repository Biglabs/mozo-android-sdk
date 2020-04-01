package io.mozocoin.sdk.wallet

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.BalanceInfo
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.transaction.TransactionDetails
import io.mozocoin.sdk.transaction.TransactionHistoryRecyclerAdapter
import io.mozocoin.sdk.transaction.payment.PaymentRequestActivity
import io.mozocoin.sdk.ui.dialog.QRCodeDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_mozo_wallet_off.*
import kotlinx.coroutines.*

internal class OffChainWalletFragment : Fragment(R.layout.fragment_mozo_wallet_off), SwipeRefreshLayout.OnRefreshListener {
    private val histories = arrayListOf<TransactionHistory>()
    private val onItemClick = { history: TransactionHistory ->
        if (context != null) {
            TransactionDetails.start(context!!, history)
        }
    }

    private var buttonPaymentRequest = true
    private var buttonSend = true

    private var historyAdapter = TransactionHistoryRecyclerAdapter(histories, onItemClick, null)
    private var currentAddress: String? = null
    private var fetchDataJob: Job? = null
    private var fetchDataJobHandler: Job? = null
    private var generateQRJob: Job? = null

    private var mOnChainBalanceInfo: BalanceInfo? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wallet_fragment_off_swipe?.apply {
            mozoSetup()
            setOnRefreshListener(this@OffChainWalletFragment)
        }
        wallet_fragment_btn_payment_request?.apply {
            visibility = if (buttonPaymentRequest) View.VISIBLE else View.GONE
            click {
                if (context != null) PaymentRequestActivity.start(context!!)
            }
        }
        wallet_fragment_btn_send?.apply {
            visibility = if (buttonSend) View.VISIBLE else View.GONE
            click {
                MozoTx.getInstance().transfer()
            }
        }
        wallet_fragment_btn_view_all?.click {
            MozoTx.getInstance().openTransactionHistory(it.context)
        }
        wallet_fragment_qr_image?.click {
            QRCodeDialog.show(context ?: return@click, currentAddress ?: return@click)
        }
        wallet_fragment_address?.click {
            it.copyWithToast()
        }

        wallet_info_detected_on_chain?.click {
            val lastTxHash = SharedPrefsUtils.getLastTxConvertOnChainInOffChain()
            if (lastTxHash.isNullOrEmpty()) {
                ConvertOnInOffActivity.start(
                        it.context,
                        currentAddress ?: return@click,
                        mOnChainBalanceInfo ?: return@click
                )
            } else {
                ConvertBroadcastActivity.start(it.context, lastTxHash)
            }
        }

        wallet_fragment_history_recycler?.apply {
            setHasFixedSize(false)
            adapter = historyAdapter
        }
    }

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        val typedArray = resources.obtainAttributes(attrs, R.styleable.MozoWalletFragment)
        buttonPaymentRequest = typedArray.getBoolean(R.styleable.MozoWalletFragment_buttonPaymentRequest, buttonPaymentRequest)
        buttonSend = typedArray.getBoolean(R.styleable.MozoWalletFragment_buttonSend, buttonSend)
        typedArray.recycle()
    }

    override fun onResume() {
        super.onResume()
        wallet_info_detected_on_chain?.gone()
        if (MozoAuth.getInstance().isSignedIn()) {
            view?.postDelayed(250) {
                MozoSDK.getInstance().profileViewModel.run {
                    profileLiveData.observe(this@OffChainWalletFragment, profileObserver)
                    balanceAndRateLiveData.observeForever(balanceAndRateObserver)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.removeObserver(profileObserver)
            balanceAndRateLiveData.removeObserver(balanceAndRateObserver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchDataJob?.cancel()
        fetchDataJobHandler?.cancel()
        generateQRJob?.cancel()
    }

    override fun onRefresh() {
        MozoSDK.getInstance().profileViewModel.fetchBalance(context!!)
        fetchData()
    }

    private val profileObserver = Observer<Profile?> {
        if (it?.walletInfo != null) {
            currentAddress = it.walletInfo!!.offchainAddress
            historyAdapter.address = currentAddress

            wallet_fragment_address?.text = currentAddress

            fetchData()

            generateQRJob?.cancel()
            generateQRJob = generateQRImage()
        } else {

            /* Clear last information */
            view?.find<TextView>(R.id.wallet_fragment_balance_value)?.text = null
            view?.find<TextView>(R.id.wallet_fragment_currency_value)?.text = null

            currentAddress = null
            histories.clear()
            historyAdapter.address = null
            historyAdapter.notifyData()

            wallet_fragment_address?.text = null
            generateQRJob?.cancel()
            wallet_fragment_qr_image?.setImageDrawable(null)
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            view?.find<TextView>(R.id.wallet_fragment_balance_value)?.apply {
                text = if (realValues) balanceNonDecimal.displayString() else null
            }
            view?.find<TextView>(R.id.wallet_fragment_currency_value)?.apply {
                text = if (realValues) balanceNonDecimalInCurrencyDisplay else null
            }
        }
    }

    @Synchronized
    private fun fetchData() {
        fetchDataJob?.cancel()
        fetchDataJobHandler?.cancel()
        fetchDataJobHandler = GlobalScope.launch {
            delay(1000)
            if (!isAdded || activity == null) return@launch
            if (context == null || currentAddress == null) return@launch

            MozoAPIsService.getInstance().getTransactionHistory(context!!, currentAddress!!,
                    page = Constant.PAGING_START_INDEX,
                    size = 10,
                    callback = { data, _ ->
                        wallet_fragment_off_swipe?.isRefreshing = false
                        historyAdapter.mEmptyView = wallet_fragment_history_empty_view

                        if (data?.items == null) {
                            historyAdapter.setCanLoadMore(false)
                            historyAdapter.notifyData()
                            return@getTransactionHistory
                        }

                        fetchDataJob = GlobalScope.launch {
                            histories.clear()
                            histories.addAll(data.items!!.map {
                                it.apply {
                                    contactName = MozoSDK.getInstance().contactViewModel.findByAddress(
                                            if (it.type(currentAddress)) it.addressTo else it.addressFrom
                                    )?.name
                                }
                            })
                            withContext(Dispatchers.Main) {
                                fetchDataJob = null
                                historyAdapter.setCanLoadMore(false)
                                historyAdapter.notifyData()
                                wallet_fragment_history_recycler?.scheduleLayoutAnimation()
                            }
                        }
                    },
                    retry = this@OffChainWalletFragment::fetchData)

            /**
             * Detect Onchain MozoX inside Offchain Wallet Address
             * */
            MozoAPIsService.getInstance().getOnChainBalanceInOffChain(context!!, currentAddress!!, { data, _ ->
                data ?: return@getOnChainBalanceInOffChain

                wallet_info_detected_on_chain?.isVisible = data.detectedOnchain || !data.convertToMozoXOnchain
                mOnChainBalanceInfo = data.balanceOfTokenOnchain

                when {
                    !data.convertToMozoXOnchain -> {
                        wallet_info_detected_on_chain?.text = HtmlCompat.fromHtml(
                                getString(R.string.mozo_convert_on_in_off_converting),
                                FROM_HTML_MODE_LEGACY
                        )
                    }
                    data.detectedOnchain -> {
                        wallet_info_detected_on_chain?.text = HtmlCompat.fromHtml(getString(
                                R.string.mozo_convert_on_in_off_detected,
                                data.balanceOfTokenOnchain?.balanceNonDecimal()?.displayString()
                        ), FROM_HTML_MODE_LEGACY)
                    }
                }

                if (data.convertToMozoXOnchain) {
                    SharedPrefsUtils.setLastInfoConvertOnChainInOffChain(null, null)
                }

            }, this@OffChainWalletFragment::fetchData)
        }
    }

    private fun generateQRImage() = GlobalScope.launch {
        val qrImage = Support.generateQRCode(
                currentAddress ?: return@launch,
                resources.dp2Px(128f).toInt()
        )
        withContext(Dispatchers.Main) {
            wallet_fragment_qr_image?.setImageBitmap(qrImage)
        }
        generateQRJob = null
    }

    @Suppress("unused")
    fun showPaymentRequestButton(display: Boolean) {
        buttonPaymentRequest = display
        wallet_fragment_btn_payment_request?.visibility = if (buttonPaymentRequest) View.VISIBLE else View.GONE
    }

    @Suppress("unused")
    fun showSendButton(display: Boolean) {
        buttonSend = display
        wallet_fragment_btn_send?.visibility = if (buttonSend) View.VISIBLE else View.GONE
    }

    companion object {
        fun getInstance() = OffChainWalletFragment()
    }
}