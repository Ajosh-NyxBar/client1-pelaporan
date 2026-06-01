package com.laporan.ops.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.laporan.ops.databinding.ItemLaporanBinding
import com.laporan.ops.model.Report

class LaporanAdapter(
    private val onClick: (Report) -> Unit
) : ListAdapter<Report, LaporanAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Report>() {
            override fun areItemsTheSame(o: Report, n: Report) = o.id == n.id
            override fun areContentsTheSame(o: Report, n: Report) = o == n
        }
    }

    inner class VH(private val b: ItemLaporanBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: Report) {
            b.tvReportCode.text      = r.reportCode
            b.tvJenisPekerjaan.text  = r.jenisPekerjaan
            b.tvLokasi.text          = "🗼 ${r.towerNama ?: r.lokasi}"
            b.tvWaktu.text           = "🕐 ${r.waktuKerja}"
            b.tvTeknisi.text         = "👷 ${r.teknisiName ?: r.teknisiUsername ?: "–"}"
            b.tvPhotoCount.text      = if (r.photoCount > 0) "📷 ${r.photoCount} foto" else "📷 Tidak ada foto"

            val (statusLabel, chipBg) = when (r.status) {
                "menunggu"  -> Pair("⏳ MENUNGGU",  android.R.color.holo_orange_light)
                "disetujui" -> Pair("✅ DISETUJUI", android.R.color.holo_green_light)
                "ditolak"   -> Pair("❌ DITOLAK",   android.R.color.holo_red_light)
                else        -> Pair(r.status.uppercase(), android.R.color.darker_gray)
            }
            b.chipStatus.text = statusLabel
            b.chipStatus.setChipBackgroundColorResource(chipBg)

            b.root.setOnClickListener { onClick(r) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
