package com.biglabs.mozo.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.biglabs.mozo.sdk.MozoAuth
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.MozoTx
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.transaction.PaymentRequestActivity
import com.biglabs.mozo.sdk.transaction.TransactionDetails
import com.biglabs.mozo.sdk.transaction.TransactionHistoryActivity
import com.biglabs.mozo.sdk.transaction.TransactionHistoryRecyclerAdapter
import com.biglabs.mozo.sdk.ui.dialog.QRCodeDialog
import com.biglabs.mozo.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MozoWalletFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private val histories = arrayListOf<Models.TransactionHistory>()
    private val onItemClick = { history: Models.TransactionHistory ->
        if (context != null) {
            TransactionDetails.start(context!!, history)
        }
    }
    private var historyAdapter = TransactionHistoryRecyclerAdapter(histories, onItemClick, null)
    private var currentAddress: String? = null
    private var fetchDataJob: Job? = null
    private var generateQRJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_refresh.click {
            if (context != null) MozoSDK.getInstance().profileViewModel.fetchBalance(context!!)
        }
        button_payment_request.click {
            if (context != null) PaymentRequestActivity.start(context!!)
        }
        button_send.click {
            MozoTx.getInstance().transfer()
        }
        button_view_all.click {
            if (context != null) TransactionHistoryActivity.start(context!!)
        }
        mozo_wallet_qr_image?.click {
            if (context != null && currentAddress != null)
                QRCodeDialog.show(context!!, currentAddress!!)
        }
        mozo_wallet_qr_image_button?.click {
            if (context != null && currentAddress != null)
                QRCodeDialog.show(context!!, currentAddress!!)
        }

        list_history_refresh?.apply {
            mozoSetup()
            setOnRefreshListener(this@MozoWalletFragment)
        }

        list_history.setHasFixedSize(true)
        list_history.itemAnimator = DefaultItemAnimator()
        list_history.adapter = historyAdapter
        list_history.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                history_top_bar_hover.isSelected = recyclerView.canScrollVertically(-1)
            }
        })

        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.observe(this@MozoWalletFragment, profileObserver)
            balanceAndRateLiveData.observeForever(balanceAndRateObserver)
        }
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchDataJob?.cancel()
        generateQRJob?.cancel()
    }

    override fun onRefresh() {
        fetchData()
    }

    private val profileObserver = Observer<Models.Profile?> {
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
            view?.find<TextView>(R.id.mozo_wallet_balance_value)?.apply {
                text = balanceInDecimal.displayString()
            }
            view?.find<TextView>(R.id.mozo_wallet_currency_balance)?.apply {
                text = balanceInCurrencyDisplay
            }
        }
    }

    private fun fetchData() {
        fetchDataJob?.cancel()
        fetchDataJob = GlobalScope.launch {
            if (currentAddress == null || context == null) return@launch

            val response = MozoService.getInstance(context!!)
                    .getTransactionHistory(currentAddress!!, page = Constant.PAGING_START_INDEX, size = 10) {
                        fetchData()
                    }.await()

            histories.clear()

            response.map {
                val contact = MozoSDK.getInstance().contactViewModel.findByAddress(if (it.type(currentAddress)) it.addressTo else it.addressFrom)
                if (contact?.name != null) {
                    it.contactName = contact.name
                }
            }
            histories.addAll(response)
            historyAdapter.setCanLoadMore(false)

            launch(Dispatchers.Main) {
                list_history_refresh.isRefreshing = false
                historyAdapter.notifyData()
            }
        }
    }

    private fun generateQRImage() = GlobalScope.launch {
        if (currentAddress != null) {
            val qrImage = Support.generateQRCode(currentAddress!!, resources.dp2Px(128f).toInt())
            launch(Dispatchers.Main) {
                mozo_wallet_qr_image?.setImageBitmap(qrImage)
            }
        }
        generateQRJob = null
    }

    private fun checkLogin() {
        view?.find<View>(R.id.view_login_required)?.apply {
            isClickable = true
            if (MozoAuth.getInstance().isSignUpCompleted()) gone() else visible()
        }
    }
}