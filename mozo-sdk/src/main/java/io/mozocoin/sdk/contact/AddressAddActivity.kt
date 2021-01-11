package io.mozocoin.sdk.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivityAddressAddNewBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import java.util.*

internal class AddressAddActivity : BaseActivity() {

    private lateinit var binding: ActivityAddressAddNewBinding
    private val mozoService by lazy { MozoAPIsService.getInstance() }

    private var mShowMessageDuration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressAddNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mShowMessageDuration = getInteger(R.integer.security_pin_show_msg_duration).toLong()

        binding.textContactAddress.text = intent.getStringExtra(FLAG_ADDRESS)

        binding.inputContactName.onTextChanged {
            binding.buttonSave.isEnabled = binding.inputContactName.length() > 0
        }

        binding.buttonSave.click { executeSaveContact() }
        binding.container.click {
            binding.inputContactName.hideKeyboard()
        }
    }

    private fun executeSaveContact() {
        showLoading()
        val contact = Contact(
                id = 0,
                name = binding.inputContactName.text.toString().trim(),
                physicalAddress = null,
                soloAddress = binding.textContactAddress.text.toString()
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

    private fun showDoneMsg() = MainScope().launch {
        binding.inputContactName.isEnabled = false
        binding.loadingContainer.hide()
        binding.textMsgSaved.visible()
        MozoSDK.getInstance().contactViewModel.fetchData(this@AddressAddActivity)

        delay(mShowMessageDuration)
        finishAndRemoveTask()
    }

    private fun showLoading() {
        binding.inputContactName.hideKeyboard()
        binding.buttonSave.isEnabled = false
        binding.loadingContainer.show()
        binding.textMsgSaved.gone()
    }

    private fun hideLoading() {
        binding.inputContactName.requestFocus()
        binding.inputContactName.showKeyboard()
        binding.buttonSave.isEnabled = true
        binding.loadingContainer.hide()
        binding.textMsgSaved.gone()
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