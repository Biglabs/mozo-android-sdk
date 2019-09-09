package io.mozocoin.sdk.transaction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact

class ContactSuggestionAdapter(context: Context, val contacts: List<Contact>) : ArrayAdapter<Contact>(context, R.layout.item_contact_suggess, contacts) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var root = convertView
        if (root == null) {
            root = inflater.inflate(R.layout.item_contact_suggess, parent, false)
        }

        getItem(position)?.let {
            root?.findViewById<TextView>(R.id.item_contact_name)?.text = it.name
            root?.findViewById<TextView>(R.id.item_contact_phone)?.text = it.phoneNo
            root?.findViewById<TextView>(R.id.item_contact_wallet_address)?.text = it.soloAddress
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
                val found = MozoSDK.getInstance().contactViewModel.find(constraint.toString())
                results.values = found
                results.count = found?.size ?: 0
            }

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        }
    }
}