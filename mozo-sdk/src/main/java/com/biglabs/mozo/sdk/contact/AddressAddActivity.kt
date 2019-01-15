package com.biglabs.mozo.sdk.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.model.Contact
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.*
import kotlinx.android.synthetic.main.view_address_add_new.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AddressAddActivity : BaseActivity() {

    private val mozoService by lazy { MozoService.getInstance() }

    private var mShowMessageDuration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_address_add_new)

        mShowMessageDuration = getInteger(R.integer.security_pin_show_msg_duration).toLong()

        text_contact_address.text = intent.getStringExtra(FLAG_ADDRESS)

        input_contact_name.onTextChanged {
            button_save.isEnabled = input_contact_name.length() > 0
        }

        button_save.click { executeSaveContact() }
    }

    private fun executeSaveContact() {
        showLoading()

        val contact = Contact(0, input_contact_name.text.toString().trim(), null, text_contact_address.text.toString())
        mozoService.saveContact(this, contact) { data, errorCode ->
            hideLoading()

            if (data != null) showDoneMsg()

            if (errorCode == "DUPLICATED_ERROR_CODE") {
                showErrorMsg()
            }
        }
    }

    private fun showDoneMsg() = GlobalScope.launch(Dispatchers.Main) {
        input_contact_name.isEnabled = false
        loading_container.hide()
        text_msg_saved.visible()
        MozoSDK.getInstance().contactViewModel.fetchData(this@AddressAddActivity)

        delay(mShowMessageDuration)
        finishAndRemoveTask()
    }

    private fun showErrorMsg() = GlobalScope.launch(Dispatchers.Main) {
        text_msg_error.visible()
    }

    private fun showLoading() {
        input_contact_name.hideKeyboard()
        button_save.isEnabled = false
        loading_container.show()
        text_msg_saved.gone()
        text_msg_error.gone()
    }

    private fun hideLoading() = GlobalScope.launch(Dispatchers.Main) {
        input_contact_name.requestFocus()
        input_contact_name.showKeyboard()
        button_save.isEnabled = true
        loading_container.hide()
        text_msg_saved.gone()
    }

    companion object {
        private const val FLAG_ADDRESS = "FLAG_ADDRESS"

        fun start(context: Context, address: String?) {
            Intent(context, AddressAddActivity::class.java).apply {
                putExtra(FLAG_ADDRESS, address?.toLowerCase())
                context.startActivity(this)
            }
        }
    }
}