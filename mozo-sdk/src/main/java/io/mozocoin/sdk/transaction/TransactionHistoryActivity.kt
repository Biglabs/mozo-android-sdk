package io.mozocoin.sdk.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.OnLoadMoreListener
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ViewTransactionHistoryBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.utils.mozoSetup
import kotlinx.coroutines.*

internal class TransactionHistoryActivity : BaseActivity(), OnLoadMoreListener,
    SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: ViewTransactionHistoryBinding
    private val onItemClick = { history: TransactionHistory ->
        TransactionDetailsActivity.start(this@TransactionHistoryActivity, history)
    }
    private val historyAdapter: TransactionHistoryRecyclerAdapter by lazy {
        TransactionHistoryRecyclerAdapter(layoutInflater, onItemClick, this)
    }

    private val historiesAll = mutableListOf<TransactionHistory>()
    private val historiesReceived = mutableListOf<TransactionHistory>()
    private val historiesSent = mutableListOf<TransactionHistory>()
    private var pageAll = Constant.PAGING_START_INDEX
    private var pageReceived = Constant.PAGING_START_INDEX
    private var pageSent = Constant.PAGING_START_INDEX
    private var currentPage: Int
        get() = when (currentFilter) {
            R.id.history_filter_received -> pageReceived
            R.id.history_filter_sent -> pageSent
            else -> pageAll
        }
        set(value) {
            when (currentFilter) {
                R.id.history_filter_received -> pageReceived = value
                R.id.history_filter_sent -> pageSent = value
                else -> pageAll = value
            }
        }
    private var fetchDataJob: Job? = null
    private var currentAddress: String? = null
    private var currentFilter: Int = R.id.history_filter_all

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listHistoryRefresh.apply {
            setOnRefreshListener(this@TransactionHistoryActivity)
            isRefreshing = true
        }

        historyAdapter.emptyView = binding.listHistoryEmptyView
        binding.listHistory.apply {
            mozoSetup(binding.listHistoryRefresh)
            adapter = historyAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.historyTopBarHover.isSelected = recyclerView.canScrollVertically(-1)
                }
            })
        }

        binding.historyFilterGroup.setOnCheckedChangeListener { _, checked ->
            currentFilter = checked
            historyAdapter.showReceived = if (checked == R.id.history_filter_all) null
            else checked == R.id.history_filter_received
            val collection = historyCollection()
            if (collection.isEmpty()) {
                fetchData()
            } else {
                historyAdapter.setData(collection)
            }
        }

        binding.listHistoryEmptyView.onPrimaryClicked = {
            onBackPressed()
        }

        /**
         * Waiting for profile and load data
         */
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.observe(this@TransactionHistoryActivity, profileObserver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchDataJob?.cancel()
        MozoSDK.getInstance().profileViewModel.run {
            profileLiveData.removeObservers(this@TransactionHistoryActivity)
        }
    }

    private val profileObserver = Observer<Profile?> {
        currentAddress = it?.walletInfo?.offchainAddress
        historyAdapter.address = currentAddress
        if (currentPage == Constant.PAGING_START_INDEX) {
            fetchData()
        }
    }

    private fun fetchData() {
        val isReceived: Boolean? = when (currentFilter) {
            R.id.history_filter_received -> true
            R.id.history_filter_sent -> false
            else -> null
        }
        MozoAPIsService.getInstance().getTransactionHistory(
            this,
            currentAddress ?: return,
            isReceived,
            page = currentPage,
            callback = { data, _ ->
                binding.listHistoryRefresh.isRefreshing = false

                if (data?.items == null) {
                    historyAdapter.setCanLoadMore(false)
                    historyAdapter.setData(mutableListOf())
                    return@getTransactionHistory
                }

                fetchDataJob?.cancel()
                fetchDataJob = MozoSDK.scope.launch {
                    val collection = historyCollection()
                    if (currentPage <= Constant.PAGING_START_INDEX) collection.clear()
                    collection.addAll(data.items!!.map {
                        it.apply {
                            contactName =
                                MozoWallet.getInstance().findContact(it, currentAddress)?.name
                            filter = currentFilter
                        }
                    })
                    withContext(Dispatchers.Main) {
                        historyAdapter.setCanLoadMore(data.items!!.size == Constant.PAGING_SIZE)
                        historyAdapter.setData(collection)
                    }
                }
            },
            retry = this::fetchData
        )
    }

    override fun onLoadMore() {
        currentPage++
        fetchData()
    }

    override fun onRefresh() {
        currentPage = Constant.PAGING_START_INDEX
        fetchData()
    }

    private fun historyCollection() = when (currentFilter) {
        R.id.history_filter_received -> historiesReceived
        R.id.history_filter_sent -> historiesSent
        else -> historiesAll
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