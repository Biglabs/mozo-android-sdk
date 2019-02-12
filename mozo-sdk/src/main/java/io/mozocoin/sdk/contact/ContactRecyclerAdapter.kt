package io.mozocoin.sdk.contact

import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.Contact
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter

internal class ContactRecyclerAdapter(
        private val contacts: List<Contact>,

        private val itemClick: ((contact: Contact) -> Unit)? = null
) : SectionedRecyclerViewAdapter() {

    fun notifyData(hideEmptySection: Boolean = false) {
        removeAllSections()

        Constant.getAlphabets().map {
            val sec = arrayListOf<Contact>()
            contacts.map { c ->
                if (c.name?.startsWith(it, ignoreCase = true) == true) {
                    sec.add(c)
                }
            }

            if (!hideEmptySection || sec.isNotEmpty()) {
                addSection(it.toString(), ContactSection(it.toString(), sec, itemClick))
            }
        }

        val otherContact = arrayListOf<Contact>()
        contacts.map { c ->
            if (c.name.isNullOrEmpty() || c.name[0].toUpperCase() < 'A' || c.name[0].toUpperCase() > 'Z') {
                otherContact.add(c)
            }
        }
        if (!hideEmptySection || otherContact.isNotEmpty()) {
            addSection("#", ContactSection("#", otherContact, itemClick))
        }
        notifyDataSetChanged()
    }
}