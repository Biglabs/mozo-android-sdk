package io.mozocoin.sdk.ui.setting

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.dp2Px
import kotlinx.android.synthetic.main.item_seed_word.view.*

class SeedWordAdapter(
    private val lsData: MutableList<String>
) : RecyclerView.Adapter<SeedWordAdapter.RecyclerViewHolder>() {

    override fun getItemCount() = lsData.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.itemView.apply {
            txt_index.text = "${position + 1}."
            txt_word.text = lsData.getOrNull(position)?.toString()
        }
    }

    override fun onCreateViewHolder(g1: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(g1.context).inflate(R.layout.item_seed_word, g1, false)
        view.doOnLayout {
            val width = g1.context.resources.run {
                displayMetrics.widthPixels - dp2Px(80f) //margin start/end 40dp
            }
            val lp = it.layoutParams
            lp.width = (width / 2).toInt()
            it.layoutParams = lp
        }
        return RecyclerViewHolder(view)
    }

    inner class RecyclerViewHolder(v: View) : RecyclerView.ViewHolder(v)
}