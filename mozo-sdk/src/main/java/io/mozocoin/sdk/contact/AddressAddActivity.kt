package io.mozocoin.sdk.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.activity_address_add_new.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

internal class AddressAddActivity : BaseActivity() {

    private val mozoService by lazy { MozoAPIsService.getInstance() }

    private var mShowMessageDuration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_add_new)

        mShowMessageDuration = getInteger(R.integer.security_pin_show_msg_duration).toLong()

        text_contact_address.text = intent.getStringExtra(FLAG_ADDRESS)

        input_contact_name.onTextChanged {
            button_save.isEnabled = input_contact_name.length() > 0
        }

        button_save.click { executeSaveContact() }
    }

    private fun executeSaveContact() {
        showLoading()
        val contact = Contact(
            id = 0,
            name = input_contact_name.text.toString().trim(),
            physicalAddress = null,
            soloAddress = text_contact_address.text.toString()
        )

        mozoService.saveContact(this, contact) { data, errorCode ->
            hideLoading()

            if (errorCode == ErrorCode.ERROR_DUPLICATE_ADDRESS.key) {
                MessageDialog.show(this, R.string.mozo_address_add_msg_error)
                return@saveContact
            }
            if (data != null) showDoneMsg()
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

    private fun showLoading() {
        input_contact_name.hideKeyboard()
        button_save.isEnabled = false
        loading_container.show()
        text_msg_saved.gone()
    }

    private fun hideLoading() {
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
                putExtra(FLAG_ADDRESS, address?.toLowerCase(Locale.getDefault()))
                context.startActivity(this)
            }
        }
    }
}