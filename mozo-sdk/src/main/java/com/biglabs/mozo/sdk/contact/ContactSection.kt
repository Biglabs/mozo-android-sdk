package com.biglabs.mozo.sdk.contact

import android.support.v7.widget.RecyclerView
import android.view.View
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.utils.click
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_contact.*
import kotlinx.android.synthetic.main.item_contact_header.*

internal class ContactSection(
        private val title: String,
        private val contacts: List<Models.Contact>,
        private val itemClick: ((contact: Models.Contact) -> Unit)? = null
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
            item_header.text = header
        }
    }

    class ItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(contact: Models.Contact) {
            item_title.text = contact.name
            item_content.text = contact.soloAddress
        }
    }
}