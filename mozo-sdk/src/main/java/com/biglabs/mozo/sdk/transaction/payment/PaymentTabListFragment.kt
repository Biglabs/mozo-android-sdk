package com.biglabs.mozo.sdk.transaction.payment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.transaction.TransactionDetails
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.SwipeToDeleteCallback
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.mozoSetup
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.fragment_payment_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PaymentTabListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val requests = arrayListOf<Models.PaymentRequest>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        payment_request_swipe_refresh.mozoSetup()
        payment_request_swipe_refresh.setOnRefreshListener(this)

        val adapter = PaymentRequestRecyclerAdapter(requests, payment_request_empty_view, onItemClickListener)
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
                        TransactionDetails.start(
                                this.context!!,
                                Models.PaymentRequest(content = it)
                        )
                    } else {
                        AlertDialog.Builder(this.context!!)
                                .setMessage(R.string.mozo_dialog_error_scan_invalid_msg)
                                .setNegativeButton(android.R.string.ok, null)
                                .show()
                    }
                }
            }
        }
    }

    private fun fetchData() {
        GlobalScope.launch {
            val response = MozoService.getInstance(MozoSDK.getInstance().context)
                    .getPaymentRequests(0, 100, onTryAgain = { fetchData() })
                    .await()
            requests.clear()
            requests.addAll(response)
            launch(Dispatchers.Main) {
                payment_request_swipe_refresh?.isRefreshing = false
                payment_request_recycler?.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun deleteRequest(position: Int) {
        val itemId = requests.getOrNull(position)?.id
        itemId ?: return
        requests.removeAt(position)
        payment_request_recycler?.adapter?.notifyItemRemoved(position)

        GlobalScope.launch {
            MozoService.getInstance(MozoSDK.getInstance().context)
                    .deletePaymentRequest(itemId) {
                        deleteRequest(position)
                    }
                    .await()
        }
    }

    private val onItemClickListener: (position: Int) -> Unit = {
        TransactionDetails.start(this.context!!, requests[it])
    }

    override fun onRefresh() {
        fetchData()
    }

    companion object {
        fun getInstance() = PaymentTabListFragment()
    }
}