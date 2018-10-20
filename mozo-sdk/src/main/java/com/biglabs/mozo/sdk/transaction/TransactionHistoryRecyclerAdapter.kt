package com.biglabs.mozo.sdk.transaction

import android.graphics.Typeface.BOLD
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.OnLoadMoreListener
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.gone
import com.biglabs.mozo.sdk.utils.visible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_history.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.*

internal class TransactionHistoryRecyclerAdapter(
        private val histories: List<Models.TransactionHistory>,
        private val itemClick: ((history: Models.TransactionHistory) -> Unit)? = null,
        private val loadMoreListener: OnLoadMoreListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var address: String? = null
    private var dataFilter: List<Models.TransactionHistory>? = null
    private var dataFilterJob: Job? = null
    private val dateFormat = SimpleDateFormat(Constant.HISTORY_TIME_FORMAT, Locale.getDefault())

    private var totalItemCount = 0
    private var lastVisibleItem = 0
    private var loading = false
    private var isCanLoadMode = true
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (recyclerView == null || !isCanLoadMode) return

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
            holder.bind(history, history.type(address), dateFormat.format(Date(history.time * 1000L)), position == itemCount - 1)
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
        dataFilterJob = launch {
            dataFilter = when (mode) {
                FILTER_RECEIVED -> histories.filter { it.type(address) == false }
                FILTER_SENT -> histories.filter { it.type(address) == true }
                else -> null
            }

            launch(UI) {
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

    private class ItemViewHolder(override val containerView: View?) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(history: Models.TransactionHistory, isSentType: Boolean, dateTime: String, lastItem: Boolean) {

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
                val prefix = if (isSentType) "To" else "From"
                val content = SpannableString(String.format(Locale.US, "%s %s - %s", prefix, history.contactName, dateTime))
                val start = prefix.length + 1
                content.setSpan(StyleSpan(BOLD), start, start + history.contactName!!.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                item_history_time.text = content
            }

            if (lastItem)
                item_divider.gone()
            else
                item_divider.visible()
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