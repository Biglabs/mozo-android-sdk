package com.biglabs.mozo.sdk.common

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SectionIndexer
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.utils.click
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_contact.*

class ContactRecyclerAdapter(private val contacts: List<Models.Contact>, private val itemClick: ((position: Int) -> Unit)? = null) : RecyclerView.Adapter<ContactRecyclerAdapter.ItemViewHolder>(), SectionIndexer {

    private var mSectionPositions: ArrayList<Int> = arrayListOf()

    override fun getSections(): Array<String> {
        mSectionPositions.clear()

        val sections: ArrayList<String> = arrayListOf()
        var i = 0
        val size = contacts.size
        while (i < size) {
            val section = contacts[i].name.substring(0..0).toUpperCase()
            if (!sections.contains(section)) {
                sections.add(section)
                mSectionPositions.add(i)
            }
            i++
        }
        return sections.toTypedArray()
    }

    override fun getSectionForPosition(position: Int): Int {
        return 0
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return if (sectionIndex < mSectionPositions.size) mSectionPositions[sectionIndex] else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
            ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false))

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(contacts[position])
        holder.itemView.click { itemClick?.invoke(position) }
    }

    class ItemViewHolder(override val containerView: View?) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(contact: Models.Contact) {
            item_title.text = contact.name
            item_content.text = contact.soloAddress
        }
    }
}