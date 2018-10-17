package com.biglabs.mozo.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.utils.*
import kotlinx.android.synthetic.main.view_address_add_new.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

internal class AddressAddActivity : BaseActivity() {

    private val mozoService by lazy { MozoService.getInstance(this) }

    private var mShowMessageDuration: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_address_add_new)

        mShowMessageDuration = getInteger(R.integer.security_pin_show_msg_duration)

        text_contact_address.text = intent.getStringExtra(FLAG_ADDRESS)

        input_contact_name.onTextChanged {
            button_save.isEnabled = input_contact_name.length() > 0
        }

        button_save.click { executeSaveContact() }
    }

    private fun executeSaveContact() {
        showLoading()

        val contact = Models.Contact(0, input_contact_name.text.toString().trim(), text_contact_address.text.toString())
        launch {
            val response = mozoService.saveContact(contact) {
                executeSaveContact()
            }.await()
            if (response != null) {
                showDoneMsg()
            } else {
                hideLoading()
                showErrorMsg()
            }
        }
    }

    private fun showDoneMsg() = async(UI) {
        input_contact_name.isEnabled = false
        loading_container.hide()
        text_msg_saved.visible()

        delay(mShowMessageDuration)
        finishAndRemoveTask()
    }

    private fun showErrorMsg() = async(UI) {
        text_msg_error.visible()
    }

    private fun showLoading() {
        input_contact_name.hideKeyboard()
        button_save.isEnabled = false
        loading_container.show()
        text_msg_saved.gone()
        text_msg_error.gone()
    }

    private fun hideLoading() = async(UI) {
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
                putExtra(FLAG_ADDRESS, address)
                context.startActivity(this)
            }
        }
    }
}