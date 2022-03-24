package io.mozocoin.sdk.transaction.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.FragmentPaymentListBinding
import io.mozocoin.sdk.transaction.TransactionDetailsActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.SwipeToDeleteCallback
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.mozoSetup

class PaymentTabListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var _binding: FragmentPaymentListBinding? = null
    private val binding get() = _binding!!
    private val requests = arrayListOf<PaymentRequest>()
    private val mAdapter = PaymentRequestRecyclerAdapter(requests) {
        TransactionDetailsActivity.start(this.requireContext(), requests[it])
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.paymentRequestSwipeRefresh.setOnRefreshListener(this@PaymentTabListFragment)
        binding.paymentRequestRecycler.apply {
            mozoSetup(binding.paymentRequestSwipeRefresh)
            adapter = mAdapter
        }

        val onSwipeToDelete = object : SwipeToDeleteCallback(view.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteRequest(viewHolder.adapterPosition)
            }
        }
        ItemTouchHelper(onSwipeToDelete).attachToRecyclerView(binding.paymentRequestRecycler)

        binding.buttonScanQr.click {
            Support.scanQRCode(it.context, ::onScanSuccess)
        }

        fetchData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchData() {
        binding.paymentRequestSwipeRefresh.isRefreshing = true
        MozoAPIsService.getInstance().getPaymentRequests(
            context ?: return,
            Constant.PAGING_START_INDEX,
            100, { data, _ ->
                binding.paymentRequestSwipeRefresh.isRefreshing = false
                mAdapter.emptyView = binding.paymentRequestEmptyView
                data ?: return@getPaymentRequests
                data.items ?: return@getPaymentRequests

                requests.clear()
                requests.addAll(data.items!!)
                mAdapter.notifyDataSetChanged()

            }, this::fetchData
        )
    }

    private fun deleteRequest(position: Int) {
        val itemId = requests.getOrNull(position)?.id
        itemId ?: return
        requests.removeAt(position)
        mAdapter.notifyItemRemoved(position)
        MozoAPIsService.getInstance().deletePaymentRequest(context ?: return, itemId)
    }

    private fun onScanSuccess(result: String) {
        val param = Support.parsePaymentRequest(result)
        if (param.isNotEmpty()) {
            TransactionDetailsActivity.start(
                this.requireContext(),
                PaymentRequest(content = result)
            )
        } else {
            MessageDialog.show(this.requireContext(), R.string.mozo_dialog_error_scan_invalid_msg)
        }
    }

    override fun onRefresh() {
        fetchData()
    }

    companion object {
        fun getInstance() = PaymentTabListFragment()
    }
}