package io.mozocoin.sdk.transaction.payment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.utils.*
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_payment_request.*

class PaymentRequestRecyclerAdapter(
        private val requests: List<PaymentRequest>,
        private val itemClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<PaymentRequestRecyclerAdapter.ItemPaymentViewHolder>() {

    var emptyView: View? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPaymentViewHolder =
            ItemPaymentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_payment_request, parent, false))

    override fun getItemCount(): Int {
        emptyView?.run {
            if (requests.isNotEmpty()) gone()
            else visible()
        }
        return requests.size
    }

    override fun onBindViewHolder(holder: ItemPaymentViewHolder, position: Int) {
        holder.bind(requests[position])
        holder.containerView.click {
            itemClick?.invoke(position)
        }
    }

    class ItemPaymentViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(request: PaymentRequest) {
            request.content?.let {
                val param = Support.parsePaymentRequest(it)
                var receiver = param.firstOrNull()
                if (!receiver.isNullOrEmpty()) {
                    receiver = MozoSDK.getInstance().contactViewModel.findByAddress(receiver)?.name
                            ?: receiver
                }
                item_payment_address.text = receiver
                item_payment_amount.text = containerView.context.getString(
                        R.string.mozo_payment_request_item_amount,
                        param.last().toBigDecimal().safe().displayString()
                )
            }

            item_payment_time.text = Support.getDisplayDate(itemView.context, request.timeInSec * 1000, itemView.context.getString(R.string.mozo_format_date_time))
        }
    }
}