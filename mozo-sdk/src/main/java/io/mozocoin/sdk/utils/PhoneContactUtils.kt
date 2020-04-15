package io.mozocoin.sdk.utils

import android.content.Context
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.dialog.MessageDialog

internal class PhoneContactUtils(input: TextView?, val callback: (Contact?) -> Unit) {

    private val mozoServices: MozoAPIsService by lazy { MozoAPIsService.getInstance() }
    private val context = input?.context

    val onFindInSystemClick: () -> Unit = OnClicked@{
        context ?: return@OnClicked
        val value = input?.text?.toString()?.trim()
        if (!value.isNullOrEmpty()) {
            val result = validatePhone(context, value)
            when {
                result > 0 -> MessageDialog.show(context, context.getString(result).split(": ")[1])
                result == 0 -> findContact(value)
                else -> MessageDialog.show(context, R.string.mozo_transfer_contact_find_err)
            }
        } else {
            MessageDialog.show(context, R.string.mozo_transfer_contact_find_err)
        }
    }

    fun validatePhone(ctx: Context, value: String): Int = when {
        value.isDigitsOnly() -> when {
            value.startsWith("0") -> 0
            else -> R.string.mozo_transfer_amount_error_invalid_phone
        }

        value.startsWith("+") -> if (MozoSDK.getInstance().contactViewModel.containCountryCode(value)) {
            if (value.isValidPhone(ctx)) 0
            else R.string.mozo_transfer_amount_error_invalid_phone
        } else R.string.mozo_transfer_amount_error_invalid_country_code

        else -> -1
    }

    private fun findContact(phone: String) {
        context ?: return
        mozoServices.findContact(context, phone, { data, _ ->
            if (data?.soloAddress.isNullOrEmpty()) {
                MessageDialog.show(context, R.string.mozo_transfer_contact_find_no_address)

            } else callback.invoke(data)
        }, {
            findContact(phone)
        })
    }
}