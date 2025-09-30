package com.nielcode.kupass.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.databinding.ItemAccountBinding
import com.nielcode.kupass.model.SiteAccount

private const val tag = "AccountListAdapter"

class AccountListAdapter(
    private val onLongClick: (SiteAccount) -> Unit,
    private val onClick: (SiteAccount) -> Unit
) : ListAdapter<SiteAccount, AccountListAdapter.AccountViewHolder>(AccountDiffCallback) {

    /**
     * ViewHolder untuk menampilkan setiap item akun.
     */
    inner class AccountViewHolder(private val binding: ItemAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(account: SiteAccount) {
            Log.i(tag, "bind: $account")
            binding.itemTitle.text = account.site
            binding.itemSubtitle.text =
                if (account.credentials.size > 1) {
                    if (account.note != "") account.note else account.credentials[0].username
                } else {
                    account.credentials.firstOrNull()?.username ?: ""
                }
            // Credentials count
            binding.itemCount.text = account.credentials.size.toString()
            binding.itemCount.visibility =
                if (account.credentials.size > 1) View.VISIBLE else View.GONE

            // Fallback text
            val initial = account.site.firstOrNull()?.uppercase() ?: ""
            binding.itemIconFallbackText.text = initial

            // Load Image Using Coil
            binding.itemIcon.load(account.logoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                // addHeader("x-api-key", "9b2DzbbB5O6LQzMdO7sVOFmlgJBcL4TOlECozO6i46rRoGgvYy")

                if (BuildConfig.FAVGET_API_KEY.isNotBlank()) {
                    addHeader("x-api-key", BuildConfig.FAVGET_API_KEY)
                }

                listener(
                    onStart = {
                        Log.i(tag, "start load image: $account")
                        binding.itemIcon.visibility = View.VISIBLE
                        binding.itemIconFallbackCard.visibility = View.GONE
                    },
                    onSuccess = { _, _ ->
                        Log.i(tag, "success load image: ${account.logoUrl}")
                        binding.itemIcon.visibility = View.VISIBLE
                        binding.itemIconFallbackCard.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        Log.e(tag, "failed load image: ${account.logoUrl}")
                        binding.itemIcon.visibility = View.GONE
                        binding.itemIconFallbackCard.visibility = View.VISIBLE
                    }
                )
            }


            // Set on-click listener
            itemView.setOnClickListener {
                onClick(account)
            }

            // Set on-long-click listener
            itemView.setOnLongClickListener {
                onLongClick(account)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        holder.bind(account)
    }
}

/**
 * Objek untuk menghitung perbedaan antara dua list, agar RecyclerView tahu
 * item mana yang berubah, ditambah, atau dihapus. Ini membuat update lebih efisien.
 */
object AccountDiffCallback : DiffUtil.ItemCallback<SiteAccount>() {
    override fun areItemsTheSame(oldItem: SiteAccount, newItem: SiteAccount): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SiteAccount, newItem: SiteAccount): Boolean {
        return oldItem == newItem
    }
}