package io.mozocoin.sdk.transaction.payment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.zxing.integration.android.IntentIntegrator
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.transaction.TransactionDetailsActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.SwipeToDeleteCallback
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.mozoSetup
import kotlinx.android.synthetic.main.fragment_payment_list.*

class PaymentTabListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val requests = arrayListOf<PaymentRequest>()
    private val adapter = PaymentRequestRecyclerAdapter(requests) {
        TransactionDetailsActivity.start(this.context!!, requests[it])
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        payment_request_swipe_refresh.mozoSetup()
        payment_request_swipe_refresh.setOnRefreshListener(this)

        payment_request_recycler.setHasFixedSize(true)
        payment_request_recycler.itemAnimator = DefaultItemAnimator()
        payment_request_recycler.adapter = adapter

        val onSwipeToDelete = object : SwipeToDeleteCallback(view.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteRequest(viewHolder.adapterPosition)
            }
        }
        ItemTouchHelper(onSwipeToDelete).attachToRecyclerView(payment_request_recycler)

        button_scan_qr.click {
            Support.scanQRCode(this)
        }

        fetchData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) return
        when {
            data != null -> {
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data).contents?.let {
                    val param = Support.parsePaymentRequest(it)
                    if (param.isNotEmpty()) {
                        TransactionDetailsActivity.start(
                                this.context!!,
                                PaymentRequest(content = it)
                        )
                    } else {
                        MessageDialog.show(this.context!!, R.string.mozo_dialog_error_scan_invalid_msg)
                    }
                }
            }
        }
    }

    private fun fetchData() {
        payment_request_swipe_refresh?.isRefreshing = true
        MozoAPIsService.getInstance().getPaymentRequests(
                context ?: return,
                Constant.PAGING_START_INDEX,
                100, { data, _ ->
            payment_request_swipe_refresh?.isRefreshing = false
            adapter.emptyView = payment_request_empty_view
            data ?: return@getPaymentRequests
            data.items ?: return@getPaymentRequests

            requests.clear()
            requests.addAll(data.items!!)
            adapter.notifyDataSetChanged()

        }, this::fetchData)
    }

    private fun deleteRequest(position: Int) {
        val itemId = requests.getOrNull(position)?.id
        itemId ?: return
        requests.removeAt(position)
        payment_request_recycler?.adapter?.notifyItemRemoved(position)
        MozoAPIsService.getInstance().deletePaymentRequest(context ?: return, itemId)
    }

    override fun onRefresh() {
        fetchData()
    }

    companion object {
        fun getInstance() = PaymentTabListFragment()
    }
}