package io.mozocoin.sdk.transaction

import android.graphics.Typeface.BOLD
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
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
import java.util.*

internal class TransactionHistoryRecyclerAdapter(
    private val layoutInflater: LayoutInflater,
    private val itemClick: ((history: TransactionHistory) -> Unit)? = null,
    private val loadMoreListener: OnLoadMoreListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val data = mutableListOf<TransactionHistory>()
    private var totalItemCount = 0
    private var lastVisibleItem = 0
    private var loading = false
    private var isCanLoadMode = true
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (!isCanLoadMode) return

            totalItemCount = recyclerView.layoutManager!!.itemCount
            lastVisibleItem =
                (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            if (!loading && totalItemCount <= (lastVisibleItem + Constant.LIST_VISIBLE_THRESHOLD)) {
                loadMoreListener?.onLoadMore()
                loading = true
            }
        }
    }
    var emptyView: View? = null
    var address: String? = null
    var showReceived: Boolean? = null

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
            val history = data[position]
            holder.bind(
                history,
                history.type(address),
                Support.getDisplayDate(
                    holder.itemView.context,
                    history.time * 1000L,
                    holder.itemView.context.getString(R.string.mozo_format_date_time)
                ),
                position == itemCount - 1,
                showReceived
            )
            holder.itemView.click { itemClick?.invoke(history) }
        }
    }

    override fun getItemCount(): Int {
        return data.size //+ if (isCanLoadMode) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < data.size) VIEW_ITEM else VIEW_LOADING
    }

    fun setCanLoadMore(loadMore: Boolean) {
        isCanLoadMode = loadMore
    }

    fun setData(newData: MutableList<TransactionHistory>) {
        loading = false
        emptyView?.isVisible = newData.isEmpty()

        val diffCallback = TransactionHistory.DiffCallback(data, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        data.clear()
        data.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
    }

    private class ItemViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            history: TransactionHistory,
            isSentType: Boolean,
            dateTime: String,
            lastItem: Boolean,
            showReceived: Boolean?
        ) {

            val amountSign: String
            val amountColor: Int

            if (showReceived == true || !isSentType) {
                binding.itemHistoryType.setText(R.string.mozo_view_text_tx_received)
                amountSign = "+"
                amountColor = R.color.mozo_color_primary
                binding.itemHistoryTypeIcon.setBackgroundResource(R.drawable.mozo_bg_icon_received)
                binding.itemHistoryTypeIcon.rotation = 180f
                binding.itemHistoryTypeIcon.setImageResource(R.drawable.ic_action_send)
            } else {
                binding.itemHistoryType.setText(R.string.mozo_view_text_tx_sent)
                amountSign = "-"
                amountColor = R.color.mozo_color_title
                binding.itemHistoryTypeIcon.setBackgroundResource(R.drawable.mozo_bg_icon_send)
                binding.itemHistoryTypeIcon.rotation = 0f
                if(history.addressFrom == history.addressTo) {
                    binding.itemHistoryTypeIcon.setImageResource(R.drawable.ic_action_transfer_myself)
                } else {
                    binding.itemHistoryTypeIcon.setImageResource(R.drawable.ic_action_send)
                }
            }

            binding.itemHistoryAmount.text =
                String.format(Locale.US, "%s%s", amountSign, history.amountDisplay())
            binding.itemHistoryAmount.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    amountColor
                )
            )

            binding.itemHistoryTime.text = dateTime
            val name = if (history.contactName.isNullOrEmpty()) {
                binding.itemHistoryAddress.ellipsize = TextUtils.TruncateAt.MIDDLE
                if (isSentType) history.addressTo else history.addressFrom
            } else {
                binding.itemHistoryAddress.ellipsize = TextUtils.TruncateAt.END
                history.contactName
            } ?: ""
            binding.itemHistoryAddress.text = SpannableString(
                itemView.context
                    .getString(
                        if (isSentType) R.string.mozo_notify_content_to else R.string.mozo_notify_content_from,
                        name
                    )
            ).apply {
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
        private const val VIEW_ITEM = 1
        private const val VIEW_LOADING = 0
    }
}