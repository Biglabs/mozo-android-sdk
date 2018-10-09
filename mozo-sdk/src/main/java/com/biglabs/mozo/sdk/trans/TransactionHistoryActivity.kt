package com.biglabs.mozo.sdk.trans

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.PopupMenu
import android.view.MenuItem
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.OnLoadMoreListener
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.services.WalletService
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.synthetic.main.view_transaction_history.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

internal class TransactionHistoryActivity : AppCompatActivity(), OnLoadMoreListener, SwipeRefreshLayout.OnRefreshListener {

    private val walletService: WalletService by lazy { WalletService.getInstance() }

    private val histories = arrayListOf<Models.TransactionHistory>()
    private val onItemClick = { position: Int ->
        TransactionDetails.start(this@TransactionHistoryActivity, histories[position])
    }
    private var historyAdapter = TransactionHistoryRecyclerAdapter(histories, onItemClick, this)

    private var currentAddress: String? = null
    private var popupFilter: PopupMenu? = null
    private var currentPage = Constant.PAGING_START_INDEX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_history)

        list_history_refresh?.apply {
            val offset = resources.getDimensionPixelSize(R.dimen.mozo_refresh_progress_offset)
            setProgressViewOffset(true, progressViewStartOffset + offset, progressViewEndOffset + offset)
            setColorSchemeResources(R.color.mozo_color_primary)
            isRefreshing = true
            setOnRefreshListener(this@TransactionHistoryActivity)
        }

        list_history.setHasFixedSize(true)
        list_history.itemAnimator = DefaultItemAnimator()
        list_history.adapter = historyAdapter

        popupFilter = PopupMenu(ContextThemeWrapper(this, R.style.MozoPopup), button_filter)
                .apply {
                    menuInflater.inflate(R.menu.menu_transaction_type, menu)
                    setOnMenuItemClickListener(onFilterSelect)
                }
        button_filter.click {
            popupFilter?.show()
        }
    }

    override fun onStart() {
        super.onStart()
        fetchData()
    }

    override fun onDestroy() {
        super.onDestroy()
        popupFilter?.dismiss()
        popupFilter?.setOnMenuItemClickListener(null)
        popupFilter = null
    }

    private fun fetchData() = async {
        if (currentAddress == null) {
            currentAddress = walletService.getAddress().await() ?: return@async
            historyAdapter.address = currentAddress
        }
        val response = MozoService.getInstance(this@TransactionHistoryActivity)
                .getTransactionHistory(currentAddress!!, page = currentPage)
                .await()

        if (response != null) {
            if (currentPage <= Constant.PAGING_START_INDEX) histories.clear()

            historyAdapter.setCanLoadMore(response.size == Constant.PAGING_SIZE)
            histories.addAll(response)
        }

        launch(UI) {
            list_history_refresh.isRefreshing = false
            historyAdapter.notifyDataSetChanged()
        }
    }

    private val onFilterSelect = PopupMenu.OnMenuItemClickListener { item: MenuItem ->
        button_filter_text.text = item.title
        button_filter_text.tag = item.itemId
        item.isChecked = true
        when (item.itemId) {
            R.id.action_type_all -> historyAdapter.filter(TransactionHistoryRecyclerAdapter.FILTER_ALL)
            R.id.action_type_received -> historyAdapter.filter(TransactionHistoryRecyclerAdapter.FILTER_RECEIVED)
            R.id.action_type_send -> historyAdapter.filter(TransactionHistoryRecyclerAdapter.FILTER_SENT)
        }
        return@OnMenuItemClickListener true
    }

    override fun onLoadMore() {
        currentPage++
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