package com.ug.air.ocular_tuberculosis.adapters.kotlinFinder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ug.air.ocular_tuberculosis.R
import com.ug.air.ocular_tuberculosis.databinding.ItemGalleryBinding
import com.ug.air.ocular_tuberculosis.models.Urls

class MediaAdapter (private val category: String) : ListAdapter<Urls, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    inner class MediaViewHolder(private val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(url: Urls) {
            // Handle the "both" category differently
            if (category == "both") {

                if (!url.analysedView) {
                    // Original image
                    Glide.with(itemView)
                        .load(url.original)
                        .placeholder(R.color.crime)
                        .into(binding.imageView)
                } else {
                    // Analysed image
                    if (url.analysed.isEmpty()) {
                        Glide.with(itemView)
                            .load(R.color.crime)
                            .into(binding.imageView)
                    } else {
                        Glide.with(itemView)
                            .load(url.analysed)
                            .placeholder(R.color.crime)
                            .into(binding.imageView)
                    }
                }
            } else if (category == "original") {
                Glide.with(itemView)
                    .load(url.original)
                    .placeholder(R.color.crime)
                    .into(binding.imageView)
            } else { // analysed
                if (url.analysed.isEmpty()) {
                    Glide.with(itemView)
                        .load(R.color.crime)
                        .into(binding.imageView)
                } else {
                    Glide.with(itemView)
                        .load(url.analysed)
                        .placeholder(R.color.crime)
                        .into(binding.imageView)
                }
            }

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(position)
                }
            }
        }
    }


    private var onItemClickListener : ((Int) -> Unit?)? = null
    fun setOnItemClickListener(listener: (Int) -> Unit){
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGalleryBinding.inflate(inflater, parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = getItem(position)
        holder.bind(mediaItem)
    }
}