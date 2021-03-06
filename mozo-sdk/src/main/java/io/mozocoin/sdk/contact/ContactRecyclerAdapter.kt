package io.mozocoin.sdk.contact

import android.view.View
import androidx.core.view.isVisible
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.Contact
import java.util.*

internal class ContactRecyclerAdapter(
        private val contacts: List<Contact>,
        private val itemClick: ((contact: Contact) -> Unit)? = null,
        private val onUpdateContact: (() -> Unit)? = null
) : SectionedRecyclerViewAdapter() {

    var mEmptyView: View? = null
    var isShowEmptyView = true
    var isShowSyncContactsUI = true

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        mEmptyView?.isVisible = isShowEmptyView && count == 0
        return count
    }

    fun notifyData(hideEmptySection: Boolean = false, showEmptyView: Boolean) {
        isShowEmptyView = showEmptyView
        removeAllSections()

        /**
         * Add contacts to Alphabet section
         */
        Constant.getAlphabets().map {
            val sec = arrayListOf<Contact>()
            contacts.map { c ->
                val ch = if (OrderingByKorean.isKorean(c.name?.getOrNull(0)))
                    KoreanChar.getCompatChoseong(c.name?.getOrNull(0))
                else c.name?.getOrNull(0)

                if (ch?.equals(it, ignoreCase = true) == true) {
                    sec.add(c)
                }
            }

            if (!hideEmptySection || sec.isNotEmpty()) {
                addSection(it.toString(), ContactSection(it.toString(), sec, itemClick))
            }
        }

        /**
         * Add contacts to Other(#) section
         */
        val otherContact = arrayListOf<Contact>()
        contacts.map { c ->
            if (c.name.isNullOrEmpty() || c.name[0].uppercaseChar() < 'A' || c.name[0].uppercaseChar() > 'Z') {
                if (Locale.getDefault().language == Locale.KOREA.language && OrderingByKorean.isKorean(c.name?.getOrNull(0)))
                    return@map

                otherContact.add(c)
            }
        }
        if (!hideEmptySection || otherContact.isNotEmpty()) {
            addSection("#", ContactSection("#", otherContact, itemClick))
        }

        /**
         * Add contacts to Alphabet section
         */
        if (isShowSyncContactsUI)
            addSection("update-contacts", ContactCTASection(onUpdateContact))

        notifyDataSetChanged()
    }
}
