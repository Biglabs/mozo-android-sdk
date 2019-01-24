package com.biglabs.mozo.sdk.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.OnLoadMoreListener
import com.biglabs.mozo.sdk.common.model.Profile
import com.biglabs.mozo.sdk.common.model.TransactionHistory
import com.biglabs.mozo.sdk.common.service.MozoAPIsService
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.mozoSetup
import kotlinx.android.synthetic.main.view_transaction_history.*
import kotlinx.coroutines.*

internal class TransactionHistoryActivity : BaseActivity(), OnLoadMoreListener, SwipeRefreshLayout.OnRefreshListener {

    private val histories = arrayListOf<TransactionHistory>()
    private val onItemClick = { history: TransactionHistory ->
        TransactionDetails.start(this@TransactionHistoryActivity, history)
    }
    private var historyAdapter = TransactionHistoryRecyclerAdapter(histories, onItemClick, this)

    private var currentAddress: String? = null
    private var currentPage = Constant.PAGING_START_INDEX
    private var loadFirstPage = true

    private var fetchDataJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_history)

        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.observe(this@TransactionHistoryActivity, profileObserver)
        }

        list_history_refresh?.apply {
            mozoSetup()
            setOnRefreshListener(this@TransactionHistoryActivity)
        }

        list_history.setHasFixedSize(true)
        list_history.itemAnimator = DefaultItemAnimator()
        list_history.adapter = historyAdapter
        list_history.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                history_top_bar_hover.isSelected = recyclerView.canScrollVertically(-1)
            }
        })
        history_filter_group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.history_filter_all -> historyAdapter.filter(TransactionHistoryRecyclerAdapter.FILTER_ALL)
                R.id.history_filter_received -> historyAdapter.filter(TransactionHistoryRecyclerAdapter.FILTER_RECEIVED)
                R.id.history_filter_sent -> historyAdapter.filter(TransactionHistoryRecyclerAdapter.FILTER_SENT)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchDataJob?.cancel()
        historyAdapter.stopFilter()
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.removeObservers(this@TransactionHistoryActivity)
        }
    }

    private val profileObserver = Observer<Profile?> {
        currentAddress = it?.walletInfo?.offchainAddress
        historyAdapter.address = currentAddress
        if (!loadFirstPage && currentPage == Constant.PAGING_START_INDEX) {
            fetchData()
        }
    }

    private fun fetchData() {
        MozoAPIsService.getInstance().getTransactionHistory(
                this,
                currentAddress ?: return,
                page = currentPage
        ) { data, _ ->
            list_history_refresh?.isRefreshing = false

            data ?: return@getTransactionHistory
            data.items ?: return@getTransactionHistory

            fetchDataJob?.cancel()
            fetchDataJob = GlobalScope.launch {
                if (currentPage <= Constant.PAGING_START_INDEX) histories.clear()
                histories.addAll(data.items!!.map {
                    it.apply {
                        contactName = MozoSDK.getInstance().contactViewModel.findByAddress(
                                if (it.type(currentAddress)) it.addressTo else it.addressFrom
                        )?.name
                    }
                })
                launch(Dispatchers.Main) {
                    historyAdapter.setCanLoadMore(data.items!!.size == Constant.PAGING_SIZE)
                    historyAdapter.notifyData()
                }
            }
        }
    }

    override fun onLoadMore() {
        if (loadFirstPage) loadFirstPage = false
        else currentPage++
        fetchData()
    }

    override fun onRefresh() {
        currentPage = Constant.PAGING_START_INDEX
        fetchData()
    }

    companion object {
        fun start(context: Context) {
            Intent(context, TransactionHistoryActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                context.applicationContext.startActivity(this)
            }
        }
    }
}