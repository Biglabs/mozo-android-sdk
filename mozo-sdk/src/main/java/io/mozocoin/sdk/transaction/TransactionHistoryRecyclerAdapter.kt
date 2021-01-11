package io.mozocoin.sdk.transaction

import android.graphics.Typeface.BOLD
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.OnLoadMoreListener
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.databinding.ItemHistoryBinding
import io.mozocoin.sdk.databinding.ItemLoadingBinding
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.visible
import kotlinx.coroutines.*
import java.util.*

internal class TransactionHistoryRecyclerAdapter(
        private val layoutInflater: LayoutInflater,
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
    var mEmptyView: View? = null

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
            ItemViewHolder(ItemHistoryBinding.inflate(layoutInflater, parent, false))
        } else LoadingViewHolder(ItemLoadingBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val history = getData()[position]
            holder.bind(
                    history,
                    history.type(address),
                    Support.getDisplayDate(
                            holder.itemView.context,
                            history.time * 1000L,
                            holder.itemView.context.getString(R.string.mozo_format_date_time)
                    ),
                    position == itemCount - 1
            )
            holder.itemView.click { itemClick?.invoke(history) }
        }
    }

    override fun getItemCount(): Int {
        mEmptyView?.isVisible = getData().isEmpty()
        return getData().size + if (isCanLoadMode && dataFilter == null) 1 else 0
    }

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

            withContext(Dispatchers.Main) {
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

    private class ItemViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(history: TransactionHistory, isSentType: Boolean, dateTime: String, lastItem: Boolean) {

            val amountSign: String
            val amountColor: Int

            if (isSentType) {
                binding.itemHistoryType.setText(R.string.mozo_view_text_tx_sent)
                amountSign = "-"
                amountColor = R.color.mozo_color_title
                binding.itemHistoryTypeIcon.setBackgroundResource(R.drawable.mozo_bg_icon_send)
                binding.itemHistoryTypeIcon.rotation = 0f
            } else {
                binding.itemHistoryType.setText(R.string.mozo_view_text_tx_received)
                amountSign = "+"
                amountColor = R.color.mozo_color_primary
                binding.itemHistoryTypeIcon.setBackgroundResource(R.drawable.mozo_bg_icon_received)
                binding.itemHistoryTypeIcon.rotation = 180f
            }

            binding.itemHistoryAmount.text = String.format(Locale.US, "%s%s", amountSign, history.amountDisplay())
            binding.itemHistoryAmount.setTextColor(ContextCompat.getColor(itemView.context, amountColor))

            binding.itemHistoryTime.text = dateTime
            val name = if (history.contactName.isNullOrEmpty()) {
                binding.itemHistoryAddress.ellipsize = TextUtils.TruncateAt.MIDDLE
                if (isSentType) history.addressTo else history.addressFrom
            } else {
                binding.itemHistoryAddress.ellipsize = TextUtils.TruncateAt.END
                history.contactName
            } ?: ""
            binding.itemHistoryAddress.text = SpannableString(itemView.context
                    .getString(if (isSentType) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from, name)).apply {
                val start = indexOf(name, ignoreCase = true)
                set(start..start + name.length, StyleSpan(BOLD))
            }

            if (lastItem)
                binding.itemDivider.gone()
            else
                binding.itemDivider.visible()
        }
    }

    class LoadingViewHolder(binding: ItemLoadingBinding) : RecyclerView.ViewHolder(binding.root)

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