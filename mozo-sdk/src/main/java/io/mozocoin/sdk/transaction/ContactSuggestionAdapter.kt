package io.mozocoin.sdk.transaction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.core.view.isVisible
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.utils.click

class ContactSuggestionAdapter(
        context: Context,
        val contacts: List<Contact>,
        val onFindClicked: (() -> Unit)? = null
) : ArrayAdapter<Contact>(context, R.layout.item_contact_suggest, contacts) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var root = convertView
        val item = getItem(position)

        if (item?.id == ITEM_NOT_FOUND_HOLDER) {
            root = inflater.inflate(R.layout.item_contact_not_found, parent, false)
            root?.findViewById<View>(R.id.button_find)?.click {
                onFindClicked?.invoke()
            }
            return root!!
        }

        if (root == null || root.tag?.toString() != "item_contact_content") {
            root = inflater.inflate(R.layout.item_contact_suggest, parent, false)
        }
        item?.let {
            root?.findViewById<TextView>(R.id.item_contact_name)?.apply {
                text = it.name
                isVisible = !it.name.isNullOrEmpty()
            }
            root?.findViewById<TextView>(R.id.item_contact_phone)?.apply {
                text = it.phoneNo
                isVisible = !it.phoneNo.isNullOrEmpty()
            }
            root?.findViewById<TextView>(R.id.item_contact_wallet_address)?.text = it.soloAddress

            root?.findViewById<View>(R.id.item_contact_divider)?.isVisible = position != 0
        }

        return root!!
    }

    override fun getFilter(): Filter = ContactFilter()

    inner class ContactFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()

            if (constraint.isNullOrEmpty()) {
                results.values = contacts
                results.count = contacts.size
            } else {
                val found = MozoSDK.getInstance().contactViewModel.find(constraint.toString().trim())
                if (found.isNullOrEmpty()) {
                    results.values = arrayListOf(Contact(ITEM_NOT_FOUND_HOLDER))
                    results.count = 1

                } else {
                    results.values = found
                    results.count = found.size
                }
            }

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            @Suppress("UNCHECKED_CAST")
            addAll((results?.values as? List<Contact>) ?: emptyList())
            if (results?.count ?: 0 > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }

    companion object {
        private const val ITEM_NOT_FOUND_HOLDER = 0x131L
    }
}