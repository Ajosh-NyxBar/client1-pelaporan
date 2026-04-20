package com.laporan.ops.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.laporan.ops.R
import com.laporan.ops.model.ReportPhoto

class PhotoAdapter(
    private val photos: List<ReportPhoto>,
    private val baseUrl: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val photo = photos[position]
        val fullUrl = "${baseUrl}${photo.photoPath}"

        Glide.with(holder.imageView.context)
            .load(fullUrl)
            .transform(CenterCrop(), RoundedCorners(24))
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.imageView)

        holder.imageView.setOnClickListener { onClick(fullUrl) }
    }

    override fun getItemCount() = photos.size
}
