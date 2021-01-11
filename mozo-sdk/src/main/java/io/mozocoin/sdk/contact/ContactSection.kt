package io.mozocoin.sdk.contact

import android.view.View
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.utils.click
import kotlinx.android.extensions.LayoutContainer

internal class ContactSection(
        private val title: String,
        private val contacts: List<Contact>,
        private val itemClick: ((contact: Contact) -> Unit)? = null
) : StatelessSection(
        SectionParameters.builder()
                .itemResourceId(R.layout.item_contact)
                .headerResourceId(R.layout.item_contact_header)
                .build()
) {
    override fun getContentItemsTotal(): Int = contacts.size

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder = HeaderViewHolder(view)

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder?) {
        if (holder is HeaderViewHolder) {
            holder.bind(title)
        }
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder = ItemViewHolder(view)

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder is ItemViewHolder) {
            val contact = contacts[position]
            holder.itemView.click { itemClick?.invoke(contact) }
            holder.bind(contact)
        }
    }

    class HeaderViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(header: String) {
            itemView.findViewById<TextView>(R.id.item_header).text = header
        }
    }

    class ItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(contact: Contact) {
            itemView.findViewById<TextView>(R.id.item_title).text = contact.name
            itemView.findViewById<TextView>(R.id.item_physical_address).apply {
                text = contact.physicalAddress
                isVisible = contact.isStore
            }
            itemView.findViewById<TextView>(R.id.item_content).text = contact.soloAddress
            itemView.findViewById<TextView>(R.id.item_phone).apply {
                text = contact.phoneNo
                isGone = contact.phoneNo.isNullOrEmpty()
            }
        }
    }
}