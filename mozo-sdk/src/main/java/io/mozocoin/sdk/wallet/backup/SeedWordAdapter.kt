package io.mozocoin.sdk.wallet.backup

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import io.mozocoin.sdk.databinding.ItemSeedWordBinding
import io.mozocoin.sdk.utils.dp2Px

class SeedWordAdapter(
        private val lsData: MutableList<String>
) : RecyclerView.Adapter<SeedWordAdapter.RecyclerViewHolder>() {

    override fun getItemCount() = lsData.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.binding.txtIndex.text = "${position + 1}."
        holder.binding.txtWord.text = lsData.getOrNull(position)?.toString()
    }

    override fun onCreateViewHolder(g1: ViewGroup, viewType: Int): RecyclerViewHolder {
        val binding = ItemSeedWordBinding.inflate(LayoutInflater.from(g1.context), g1, false)
        binding.root.doOnLayout {
            val width = g1.context.resources.run {
                displayMetrics.widthPixels - dp2Px(80f) //margin start/end 40dp
            }
            val lp = it.layoutParams
            lp.width = (width / 2).toInt()
            it.layoutParams = lp
        }
        return RecyclerViewHolder(binding)
    }

    inner class RecyclerViewHolder(val binding: ItemSeedWordBinding) : RecyclerView.ViewHolder(binding.root)
}