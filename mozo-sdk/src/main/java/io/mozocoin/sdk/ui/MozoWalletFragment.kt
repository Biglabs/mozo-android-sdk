package io.mozocoin.sdk.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.transaction.TransactionDetails
import io.mozocoin.sdk.transaction.TransactionHistoryActivity
import io.mozocoin.sdk.transaction.TransactionHistoryRecyclerAdapter
import io.mozocoin.sdk.transaction.payment.PaymentRequestActivity
import io.mozocoin.sdk.ui.dialog.QRCodeDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_mozo_wallet.*
import kotlinx.android.synthetic.main.view_wallet_state_not_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MozoWalletFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
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
    private var generateQRJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_mozo_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wallet_fragment_btn_refresh_balance?.click {
            if (context != null) MozoSDK.getInstance().profileViewModel.fetchBalance(context!!)
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
            if (context != null) TransactionHistoryActivity.start(context!!)
        }
        wallet_fragment_qr_image?.click {
            if (context != null && currentAddress != null)
                QRCodeDialog.show(context!!, currentAddress!!)
        }
        wallet_fragment_btn_show?.click {
            if (context != null && currentAddress != null)
                QRCodeDialog.show(context!!, currentAddress!!)
        }

        wallet_fragment_refresh_layout?.apply {
            mozoSetup()
            setOnRefreshListener(this@MozoWalletFragment)
        }

        historyAdapter.setEmptyView(wallet_fragment_history_empty_view)
        wallet_fragment_history_recycler?.apply {
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            adapter = historyAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    wallet_fragment_top_hover?.isSelected = recyclerView.canScrollVertically(-1)
                }
            })
        }

        button_login?.click {
            MozoAuth.getInstance().signIn()
        }
    }

    override fun onInflate(context: Context?, attrs: AttributeSet?, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)

        attrs?.run {
            val typedArray = resources.obtainAttributes(this, R.styleable.MozoWalletFragment)
            buttonPaymentRequest = typedArray.getBoolean(R.styleable.MozoWalletFragment_buttonPaymentRequest, buttonPaymentRequest)
            buttonSend = typedArray.getBoolean(R.styleable.MozoWalletFragment_buttonSend, buttonSend)
            typedArray.recycle()
        }
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.observe(this@MozoWalletFragment, profileObserver)
            balanceAndRateLiveData.observeForever(balanceAndRateObserver)
        }
        MozoSDK.getInstance().profileViewModel.fetchBalance(context ?: return)
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
        generateQRJob?.cancel()
    }

    override fun onRefresh() {
        fetchData()
    }

    private val profileObserver = Observer<Profile?> {
        if (it?.walletInfo != null) {
            currentAddress = it.walletInfo!!.offchainAddress
            historyAdapter.address = currentAddress
            fetchData()

            generateQRJob?.cancel()
            generateQRJob = generateQRImage()
        }
        checkLogin()
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            view?.find<TextView>(R.id.wallet_fragment_balance_value)?.apply {
                text = balanceInDecimal.displayString()
            }
            view?.find<TextView>(R.id.wallet_fragment_currency_value)?.apply {
                text = balanceInCurrencyDisplay
            }
        }
    }

    private fun fetchData() {
        MozoAPIsService.getInstance().getTransactionHistory(
                context ?: return,
                currentAddress ?: return,
                page = Constant.PAGING_START_INDEX,
                size = 10
        ) { data, _ ->
            data ?: return@getTransactionHistory
            data.items ?: return@getTransactionHistory

            fetchDataJob?.cancel()
            fetchDataJob = GlobalScope.launch {
                histories.clear()
                histories.addAll(data.items!!.map {
                    it.apply {
                        contactName = MozoSDK.getInstance().contactViewModel.findByAddress(
                                if (it.type(currentAddress)) it.addressTo else it.addressFrom
                        )?.name
                    }
                })
                launch(Dispatchers.Main) {
                    wallet_fragment_refresh_layout?.isRefreshing = false
                    historyAdapter.setCanLoadMore(false)
                    historyAdapter.notifyData()
                }
            }
        }
    }

    private fun generateQRImage() = GlobalScope.launch {
        if (currentAddress != null) {
            val qrImage = Support.generateQRCode(currentAddress!!, resources.dp2Px(128f).toInt())
            launch(Dispatchers.Main) {
                wallet_fragment_qr_image?.setImageBitmap(qrImage)
            }
        }
        generateQRJob = null
    }

    private fun checkLogin() {
        view?.find<View>(R.id.wallet_fragment_login_required)?.apply {
            isClickable = true
            if (MozoAuth.getInstance().isSignUpCompleted()) gone() else visible()
        }
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
        fun getInstance() = MozoWalletFragment()
    }
}