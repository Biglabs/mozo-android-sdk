package com.biglabs.mozo.sdk.trans

import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_history.*
import java.text.SimpleDateFormat
import java.util.*
import com.biglabs.mozo.sdk.common.OnLoadMoreListener
import kotlinx.coroutines.experimental.async


internal class TransactionHistoryRecyclerAdapter(
        private val histories: List<Models.TransactionHistory>,
        private val itemClick: ((position: Int) -> Unit)? = null,
        private val loadMoreListener: OnLoadMoreListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var address: String? = null
    private var dataFilter: List<Models.TransactionHistory>? = null
    private val dateFormat = SimpleDateFormat(Constant.HISTORY_TIME_FORMAT, Locale.getDefault())

    private var totalItemCount = 0
    private var lastVisibleItem = 0
    private var loading = false
    private var isCanLoadMode = true
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (recyclerView == null) return

            totalItemCount = recyclerView.layoutManager.itemCount
            lastVisibleItem = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            if (!loading && totalItemCount <= (lastVisibleItem + Constant.LIST_VISIBLE_THRESHOLD)) {
                loadMoreListener?.onLoadMore()
                loading = true
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(onScrollListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeOnScrollListener(onScrollListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_ITEM) {
            ItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.item_history,
                            parent,
                            false
                    )
            )
        } else LoadingViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.item_loading,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val history = getData()[position]
            holder.bind(history, history.type(address), dateFormat.format(Date(history.time * 1000L)))
            holder.itemView.click { itemClick?.invoke(position) }
        }
    }

    override fun getItemCount(): Int = getData().size + if (isCanLoadMode && dataFilter == null) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (position < getData().size) VIEW_ITEM else VIEW_LOADING
    }

    private fun getData() = dataFilter ?: histories

    fun filter(@FilterMode mode: Int) = async {
        dataFilter = when (mode) {
            FILTER_RECEIVED -> histories.filter { it.type(address) == false }
            FILTER_SENT -> histories.filter { it.type(address) == true }
            else -> null
        }

        notifyDataSetChanged()
    }

    fun setCanLoadMore(loadMore: Boolean) {
        isCanLoadMode = loadMore
    }

    class ItemViewHolder(override val containerView: View?) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(history: Models.TransactionHistory, isSentType: Boolean, dateTime: String) {

            val amountSign: String
            val amountColor: Int

            if (isSentType) {
                item_history_type.setText(R.string.mozo_view_text_tx_sent)
                amountSign = "-"
                amountColor = ContextCompat.getColor(itemView.context, R.color.mozo_color_title)
            } else {
                item_history_type.setText(R.string.mozo_view_text_tx_received)
                amountSign = "+"
                amountColor = ContextCompat.getColor(itemView.context, R.color.mozo_color_primary)
            }

            item_history_amount.text = itemView.resources.getString(R.string.mozo_transaction_history_amount_text, amountSign, history.amountDisplay())
            item_history_amount.setTextColor(amountColor)

            item_history_amount_fiat.text = "₩0"

            item_history_time.text = dateTime
        }
    }

    class LoadingViewHolder(override val containerView: View?) : RecyclerView.ViewHolder(containerView), LayoutContainer

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(FILTER_ALL, FILTER_RECEIVED, FILTER_SENT)
        annotation class FilterMode

        const val FILTER_ALL = 0x1
        const val FILTER_RECEIVED = 0x2
        const val FILTER_SENT = 0x3

        private const val VIEW_ITEM = 1
        private const val VIEW_LOADING = 0
    }
}