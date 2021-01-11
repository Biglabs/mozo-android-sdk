package io.mozocoin.sdk.contact

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import kotlinx.android.extensions.LayoutContainer

internal class ContactCTASection(
        private val onUpdateContact: (() -> Unit)? = null
) : StatelessSection(
        SectionParameters.builder()
                .itemResourceId(R.layout.item_contact_cta)
                .build()
) {
    override fun getContentItemsTotal(): Int = 1

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder = HeaderViewHolder(view)

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder?) {

    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder = ItemViewHolder(view)

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder is ItemViewHolder) {
//            val contact = contacts[position]
//            holder.itemView.click { itemClick?.invoke(contact) }
//            holder.bind(contact)

            holder.itemView.findViewById<View>(R.id.button_update_contact)?.click {
                onUpdateContact?.invoke()
            }
        }
    }

    class HeaderViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

    class ItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
}