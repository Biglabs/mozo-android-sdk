package io.mozocoin.sdk.transaction.payment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.databinding.ItemPaymentRequestBinding
import io.mozocoin.sdk.utils.*

class PaymentRequestRecyclerAdapter(
        private val requests: List<PaymentRequest>,
        private val itemClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<PaymentRequestRecyclerAdapter.ItemPaymentViewHolder>() {

    var emptyView: View? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPaymentViewHolder =
            ItemPaymentViewHolder(ItemPaymentRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int {
        emptyView?.run {
            if (requests.isNotEmpty()) gone()
            else visible()
        }
        return requests.size
    }

    override fun onBindViewHolder(holder: ItemPaymentViewHolder, position: Int) {
        holder.bind(requests[position])
        holder.itemView.click {
            itemClick?.invoke(position)
        }
    }

    class ItemPaymentViewHolder(private val binding: ItemPaymentRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: PaymentRequest) {
            request.content?.let {
                val param = Support.parsePaymentRequest(it)
                var receiver = param.firstOrNull()
                if (!receiver.isNullOrEmpty()) {
                    receiver = MozoSDK.getInstance().contactViewModel.findByAddress(receiver)?.name
                            ?: receiver
                }
                binding.itemPaymentAddress.text = receiver
                binding.itemPaymentAmount.text = itemView.context.getString(
                        R.string.mozo_payment_request_item_amount,
                        param.last().toBigDecimal().safe().displayString()
                )
            }

            binding.itemPaymentTime.text = Support.getDisplayDate(itemView.context, request.timeInSec * 1000, itemView.context.getString(R.string.mozo_format_date_time))
        }
    }
}