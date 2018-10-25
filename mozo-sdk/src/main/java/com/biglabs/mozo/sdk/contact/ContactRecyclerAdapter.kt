package com.biglabs.mozo.sdk.contact

import com.biglabs.mozo.sdk.common.Models
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter

internal class ContactRecyclerAdapter(
        private val contacts: List<Models.Contact>,

        private val itemClick: ((contact: Models.Contact) -> Unit)? = null
) : SectionedRecyclerViewAdapter() {

    fun notifyData(hideEmptySection: Boolean = false) {
        removeAllSections()

        var alphabet = 'A'
        while (alphabet <= 'Z') {
            val sec = arrayListOf<Models.Contact>()
            contacts.map { c ->
                if (c.name != null && c.name[0].equals(alphabet, ignoreCase = true)) {
                    sec.add(c)
                }
            }

            if (!hideEmptySection || sec.isNotEmpty()) {
                addSection(alphabet.toString(), ContactSection(alphabet.toString(), sec, itemClick))
            }
            alphabet++
        }

        val otherContact = arrayListOf<Models.Contact>()
        contacts.map { c ->
            if (c.name == null || c.name[0].toUpperCase() < 'A' || c.name[0].toUpperCase() > 'Z') {
                otherContact.add(c)
            }
        }
        if (!hideEmptySection || otherContact.isNotEmpty()) {
            addSection("#", ContactSection("#", otherContact, itemClick))
        }
        notifyDataSetChanged()
    }
}