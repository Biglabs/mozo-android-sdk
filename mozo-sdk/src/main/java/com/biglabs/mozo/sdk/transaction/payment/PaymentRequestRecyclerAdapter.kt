package com.biglabs.mozo.sdk.transaction.payment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.model.PaymentRequest
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.gone
import com.biglabs.mozo.sdk.utils.visible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_payment_request.*

class PaymentRequestRecyclerAdapter(
        private val requests: List<PaymentRequest>,
        private val emptyView: View? = null,
        private val itemClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<PaymentRequestRecyclerAdapter.ItemPaymentViewHolder>() {

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
                }
                item_payment_address.text = receiver
                item_payment_amount.text = containerView.context.getString(R.string.mozo_payment_request_item_amount, param.lastOrNull())
            }

            item_payment_time.text = Support.getDisplayDate(request.timeInSec * 1000, "h:mm aa MMM dd, yyyy")
        }
    }
}