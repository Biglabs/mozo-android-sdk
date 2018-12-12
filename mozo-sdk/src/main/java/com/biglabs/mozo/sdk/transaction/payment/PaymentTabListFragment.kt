package com.biglabs.mozo.sdk.transaction.payment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.biglabs.mozo.sdk.MozoWallet
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.transaction.TransactionDetails
import com.biglabs.mozo.sdk.utils.Support
import com.biglabs.mozo.sdk.utils.click
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.fragment_payment_list.*

class PaymentTabListFragment : Fragment() {

    private val requests = arrayListOf(
            Models.PaymentRequest(1544585937018L, "0xbc049e92d22a6e544d1032b243310ac167ac2f9a", "mozox:0xbc049e92d22a6e544d1032b243310ac167ac2f9a?amount=1028"),
            Models.PaymentRequest(1544585876325L, "0xbc049e92d22a6e544d1032b243310ac167ac2f9a", "mozox:0x8f62c2331fa2d7c62c34e9653264f56e3e5658c0?amount=100"),
            Models.PaymentRequest(1544585831300L, "0xbc049e92d22a6e544d1032b243310ac167ac2f9a", "mozox:0xa816b085db03f61ed19909fbb0006fb94c0bf959?amount=3.5"),
            Models.PaymentRequest(1544585810627L, "0xbc049e92d22a6e544d1032b243310ac167ac2f9a", "mozox:0xf54dd76cda278afd22e6dd2593b246842c34ba9c?amount=15.0"),
            Models.PaymentRequest(1544585798706L, "0xbc049e92d22a6e544d1032b243310ac167ac2f9a", "mozox:0x011df24265841dCdbf2e60984BB94007b0C1d76A?amount=0.5")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val adapter = PaymentRequestRecyclerAdapter(requests, onItemClickListener)
        payment_request_recycler.setHasFixedSize(true)
        payment_request_recycler.adapter = adapter

        button_scan_qr.click {
            Support.scanQRCode(this)
        }
    }

    private val onItemClickListener: (position: Int) -> Unit = {
        TransactionDetails.start(this.context!!, requests[it])
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
                                Models.PaymentRequest(toAddress = MozoWallet.getInstance().getAddress(), content = it)
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

    companion object {
        fun getInstance() = PaymentTabListFragment()
    }
}