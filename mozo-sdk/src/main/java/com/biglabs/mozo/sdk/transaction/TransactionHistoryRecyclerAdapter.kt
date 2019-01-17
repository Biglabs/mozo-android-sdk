package com.biglabs.mozo.sdk.transaction

import android.graphics.Typeface.BOLD
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.set
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.OnLoadMoreListener
import com.biglabs.mozo.sdk.common.model.TransactionHistory
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.gone
import com.biglabs.mozo.sdk.utils.visible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_history.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

internal class TransactionHistoryRecyclerAdapter(
        private val histories: List<TransactionHistory>,
        private val itemClick: ((history: TransactionHistory) -> Unit)? = null,
        private val loadMoreListener: OnLoadMoreListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var address: String? = null
    private var dataFilter: List<TransactionHistory>? = null
    private var dataFilterJob: Job? = null

    private var totalItemCount = 0
    private var lastVisibleItem = 0
    private var loading = false
    private var isCanLoadMode = true
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (!isCanLoadMode) return

            totalItemCount = recyclerView.layoutManager!!.itemCount
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
            holder.bind(history, history.type(address), Support.getDisplayDate(history.time * 1000L, Constant.HISTORY_TIME_FORMAT), position == itemCount - 1)
            holder.itemView.click { itemClick?.invoke(history) }
        }
    }

    override fun getItemCount(): Int = getData().size + if (isCanLoadMode && dataFilter == null) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (position < getData().size) VIEW_ITEM else VIEW_LOADING
    }

    private fun getData() = dataFilter ?: histories

    fun filter(@FilterMode mode: Int) {
        stopFilter()
        dataFilterJob = GlobalScope.launch {
            dataFilter = when (mode) {
                FILTER_RECEIVED -> histories.filter { !it.type(address) }
                FILTER_SENT -> histories.filter { it.type(address) }
                else -> null
            }

            launch(Dispatchers.Main) {
                dataFilterJob = null
                notifyDataSetChanged()
            }
        }
    }

    fun stopFilter() {
        dataFilterJob?.cancel()
    }

    fun setCanLoadMore(loadMore: Boolean) {
        isCanLoadMode = loadMore
    }

    fun notifyData() {
        loading = false
        stopFilter()
        notifyDataSetChanged()
    }

    private class ItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(history: TransactionHistory, isSentType: Boolean, dateTime: String, lastItem: Boolean) {

            val amountSign: String
            val amountColor: Int

            if (isSentType) {
                item_history_type.setText(R.string.mozo_view_text_tx_sent)
                amountSign = "-"
                amountColor = R.color.mozo_color_title
                item_history_type_icon.setBackgroundResource(R.drawable.mozo_bg_icon_send)
                item_history_type_icon.rotation = 0f
            } else {
                item_history_type.setText(R.string.mozo_view_text_tx_received)
                amountSign = "+"
                amountColor = R.color.mozo_color_primary
                item_history_type_icon.setBackgroundResource(R.drawable.mozo_bg_icon_received)
                item_history_type_icon.rotation = 180f
            }

            item_history_amount.text = String.format(Locale.US, "%s%s", amountSign, history.amountDisplay())
            item_history_amount.setTextColor(ContextCompat.getColor(itemView.context, amountColor))

            if (history.contactName.isNullOrEmpty())
                item_history_time.text = dateTime
            else {
                item_history_time.text = SpannableString(containerView.context.getString(
                        if (isSentType) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from,
                        history.contactName
                ) + " - $dateTime").apply {
                    val start = indexOf(history.contactName!!, ignoreCase = true)
                    set(start..start + history.contactName!!.length, StyleSpan(BOLD))
                }
            }

            if (lastItem)
                item_divider.gone()
            else
                item_divider.visible()
        }
    }

    class LoadingViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

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