package com.ug.air.ocular_tuberculosis.adapters.kotlinFinder

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.ug.air.ocular_tuberculosis.models.Urls

class MediaDiffCallback : DiffUtil.ItemCallback<Urls>() {
    override fun areItemsTheSame(oldItem: Urls, newItem: Urls): Boolean {
        return oldItem.original == newItem.original
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Urls, newItem: Urls): Boolean {
        return oldItem == newItem
    }
}