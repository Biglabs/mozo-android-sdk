package com.biglabs.mozo.sdk.transaction

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.OnLoadMoreListener
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.mozoSetup
import kotlinx.android.synthetic.main.view_transaction_history.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

internal class TransactionHistoryActivity : BaseActivity(), OnLoadMoreListener, SwipeRefreshLayout.OnRefreshListener {

    private val histories = arrayListOf<Models.TransactionHistory>()
    private val onItemClick = { history: Models.TransactionHistory ->
        TransactionDetails.start(this@TransactionHistoryActivity, history)
    }
    private var historyAdapter = TransactionHistoryRecyclerAdapter(histories, onItemClick, this)

    private var currentAddress: String? = null
    private var currentPage = Constant.PAGING_START_INDEX
    private var loadFirstPage = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_history)

        MozoSDK.profileViewModel?.run {
            profileLiveData.observeForever(profileObserver)
        }

        list_history_refresh?.apply {
            mozoSetup()
            setOnRefreshListener(this@TransactionHistoryActivity)
        }

        list_history.setHasFixedSize(true)
        list_history.itemAnimator = DefaultItemAnimator()
        list_history.adapter = historyAdapter
        list_history.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                history_top_bar_hover.isSelected = list_history.canScrollVertically(-1)
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
        historyAdapter.stopFilter()
        MozoSDK.profileViewModel?.run {
            profileLiveData.removeObserver(profileObserver)
        }
    }

    private val profileObserver = Observer<Models.Profile?> {
        currentAddress = it?.walletInfo?.offchainAddress
        historyAdapter.address = currentAddress
        if (!loadFirstPage && currentPage == Constant.PAGING_START_INDEX) {
            fetchData()
        }
    }

    private fun fetchData() = async {
        if (currentAddress == null) return@async

        val response = MozoService.getInstance(this@TransactionHistoryActivity)
                .getTransactionHistory(currentAddress!!, page = currentPage) { fetchData() }
                .await()

        if (response != null) {
            if (currentPage <= Constant.PAGING_START_INDEX) histories.clear()

            response.map {
                val contact = MozoSDK.contactViewModel?.findByAddress(if (it.type(currentAddress)) it.addressTo else it.addressFrom)
                if (contact?.name != null) {
                    it.contactName = contact.name
                }
            }
            histories.addAll(response)
            historyAdapter.setCanLoadMore(response.size == Constant.PAGING_SIZE)
        }

        launch(UI) {
            list_history_refresh.isRefreshing = false
            historyAdapter.notifyData()
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
                context.startActivity(this)
            }
        }
    }
}