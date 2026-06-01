package com.laporan.ops.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.laporan.ops.databinding.ItemTowerBinding
import com.laporan.ops.model.Tower

class TowerAdapter(
    private val onClick: (Tower) -> Unit
) : ListAdapter<Tower, TowerAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Tower>() {
            override fun areItemsTheSame(a: Tower, b: Tower) = a.id == b.id
            override fun areContentsTheSame(a: Tower, b: Tower) = a == b
        }
    }

    inner class VH(val b: ItemTowerBinding) : RecyclerView.ViewHolder(b.root) {
        init { b.root.setOnClickListener { onClick(getItem(adapterPosition)) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTowerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = getItem(position)
        holder.b.apply {
            tvNama.text = t.nama
            tvAlamat.text = if (!t.alamat.isNullOrBlank()) t.alamat else "Tanpa alamat"

            if (t.isActive == 1) {
                chipStatus.text = "Aktif"
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                chipStatus.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                chipStatus.text = "Nonaktif"
                chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                chipStatus.setTextColor(Color.parseColor("#C62828"))
            }
        }
    }
}
