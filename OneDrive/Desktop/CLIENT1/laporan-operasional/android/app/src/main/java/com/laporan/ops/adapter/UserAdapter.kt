package com.laporan.ops.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.laporan.ops.databinding.ItemUserBinding
import com.laporan.ops.model.UserDetail

class UserAdapter(
    private val onClick: (UserDetail) -> Unit
) : ListAdapter<UserDetail, UserAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserDetail>() {
            override fun areItemsTheSame(a: UserDetail, b: UserDetail) = a.id == b.id
            override fun areContentsTheSame(a: UserDetail, b: UserDetail) = a == b
        }
    }

    inner class VH(val b: ItemUserBinding) : RecyclerView.ViewHolder(b.root) {
        init { b.root.setOnClickListener { onClick(getItem(adapterPosition)) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = getItem(position)
        holder.b.apply {
            tvName.text     = user.name
            tvUsername.text  = "@${user.username}"
            chipRole.text   = user.role.replaceFirstChar { it.uppercase() }

            if (!user.jabatan.isNullOrBlank()) {
                tvJabatan.text = user.jabatan
                tvJabatan.visibility = android.view.View.VISIBLE
            } else {
                tvJabatan.visibility = android.view.View.GONE
            }

            // Role chip color
            val (bgColor, textColor) = when (user.role) {
                "admin"    -> Color.parseColor("#F3E5F5") to Color.parseColor("#7B1FA2")
                "helpdesk" -> Color.parseColor("#E0F2F1") to Color.parseColor("#00796B")
                else       -> Color.parseColor("#E8EAF6") to Color.parseColor("#3F51B5")
            }
            chipRole.setChipBackgroundColorResource(android.R.color.transparent)
            chipRole.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgColor)
            chipRole.setTextColor(textColor)

            // Active status dot
            val statusColor = if (user.isActive == 1) Color.parseColor("#43A047") else Color.parseColor("#E53935")
            (viewStatus.background as? GradientDrawable)?.setColor(statusColor)
                ?: run {
                    val dot = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(statusColor)
                    }
                    viewStatus.background = dot
                }
        }
    }
}
