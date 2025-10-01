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
import com.nielcode.kupass.utils.LogoCache

private const val tag = "AccountListAdapter"

class AccountListAdapter(
    private val onLongClick: (SiteAccount) -> Unit,
    private val onClick: (SiteAccount) -> Unit
) : ListAdapter<SiteAccount, AccountListAdapter.AccountViewHolder>(AccountDiffCallback) {

    /**
     * ViewHolder to display each account item.
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

            // Load Image Using Coil with caching
            val domain = account.domain

            val cachedUrl = LogoCache.get(domain)
            val urlToLoad = cachedUrl ?: account.logoUrl.also {
                LogoCache.put(domain, it) // save to cache if it doesn't exist yet
            }

            binding.itemIcon.load(urlToLoad) {
                crossfade(true)
                transformations(CircleCropTransformation())

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
                        Log.i(tag, "success load image: $urlToLoad")
                        binding.itemIcon.visibility = View.VISIBLE
                        binding.itemIconFallbackCard.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        Log.e(tag, "failed load image: $urlToLoad")
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
 * An object to calculate the differences between two lists, so that RecyclerView knows which items have changed,
 *  been added, or been removed. This makes updates more efficient.
 */
object AccountDiffCallback : DiffUtil.ItemCallback<SiteAccount>() {
    override fun areItemsTheSame(oldItem: SiteAccount, newItem: SiteAccount): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SiteAccount, newItem: SiteAccount): Boolean {
        return oldItem == newItem
    }
}