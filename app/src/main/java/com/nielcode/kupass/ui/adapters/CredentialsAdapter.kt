package com.nielcode.kupass.ui.adapters

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nielcode.kupass.R
import com.nielcode.kupass.databinding.ItemCredentialCardBinding
import com.nielcode.kupass.model.UserCredential
import java.text.DateFormat

class CredentialsAdapter(
    private val siteNote: String?,
    private val onEdit: (UserCredential) -> Unit = {},
    private val onCopy: (label: String, value: String) -> Unit,
    private val onDelete: (UserCredential) -> Unit
) : ListAdapter<UserCredential, CredentialsAdapter.VH>(Diff) {

    inner class VH(private val b: ItemCredentialCardBinding) : RecyclerView.ViewHolder(b.root) {
        private var revealed = false
        private lateinit var current: UserCredential

        init {
            b.btnCopyUsername.setOnClickListener {
                onCopy("username", current.username)
            }
            b.btnCopyPassword.setOnClickListener {
                onCopy("password", current.password)
            }
            b.btnReveal.setOnClickListener {
                revealed = !revealed
                renderPassword()
                b.btnReveal.setIconResource(if (revealed) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
            }
            b.btnEdit.setOnClickListener {
                onEdit(current)
            }
            b.btnDelete.setOnClickListener { onDelete(current) }
        }

        fun bind(item: UserCredential, position: Int) {
            current = item
            b.tvUsername.text = item.username

            val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            b.tvLastUpdated.text = df.format(item.lastUpdated)

            // site note only on the first card & if any
            if (!siteNote.isNullOrBlank() && position == 0) {
                b.tvNotes.visibility = View.VISIBLE
                b.tvNotes.text = siteNote
            } else {
                b.tvNotes.visibility = View.GONE
            }

            revealed = false
            b.btnReveal.setIconResource(R.drawable.ic_visibility)
            renderPassword()
        }

        private fun renderPassword() {
            b.tvPassword.text = if (revealed) {
                SpannableStringBuilder(current.password)
            } else {
                SpannableStringBuilder("•".repeat(current.password.length.coerceAtLeast(8)))
            }
        }
    }

    object Diff : DiffUtil.ItemCallback<UserCredential>() {
        override fun areItemsTheSame(oldItem: UserCredential, newItem: UserCredential): Boolean =
            oldItem.username == newItem.username && oldItem.lastUpdated == newItem.lastUpdated

        override fun areContentsTheSame(oldItem: UserCredential, newItem: UserCredential): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding =
            ItemCredentialCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position)
    }
}
