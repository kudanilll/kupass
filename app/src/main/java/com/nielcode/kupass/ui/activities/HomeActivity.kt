package com.nielcode.kupass.ui.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nielcode.kupass.R
import com.nielcode.kupass.core.PermissionManager
import com.nielcode.kupass.core.PreferenceManager
import com.nielcode.kupass.databinding.ActivityHomeBinding
import com.nielcode.kupass.model.SiteAccount
import com.nielcode.kupass.model.UserCredential
import com.nielcode.kupass.ui.adapters.AccountListAdapter
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.FileHandler
import java.util.Date

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var permissionManager: PermissionManager

    private lateinit var accountAdapter: AccountListAdapter
    private var siteAccounts = mutableListOf<SiteAccount>()

    private val requiredPermissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        emptyList()
    }

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val contentToExport = "{\"message\":\"This is a dummy JSON content for export.\"}"
            val success = FileHandler.writeFileContent(this, it, contentToExport)
            val message =
                if (success) R.string.toast_success_export else R.string.toast_failed_export
            Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()
        }
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = FileHandler.readFileContent(this, it)
            if (content != null) {
                Toast.makeText(this, getString(R.string.toast_success_import), Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, getString(R.string.toast_failed_import), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDynamicColors()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPermissionManager()
        checkAndRequestPermissions()
        setupButtonListeners()

        setupRecyclerView()
        loadAndDisplayData()
    }

    private fun applyDynamicColors() {
        val pref = PreferenceManager(this)
        if (pref.dynamicColor == AppConfig.DynamicColors.Code.ENABLE && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }

    private fun setupPermissionManager() {
        permissionManager = PermissionManager(
            activity = this,
            permissions = requiredPermissions,
            onGranted = {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT)
                    .show()
            },
            onDenied = { deniedPermissions ->
                Toast.makeText(
                    this,
                    "${getString(R.string.permission_denied)}: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun checkAndRequestPermissions() {
        if (requiredPermissions.isEmpty() || permissionManager.arePermissionsGranted()) {
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_permission))
            .setMessage(getString(R.string.dialog_message_permission))
            .setPositiveButton(getString(R.string.dialog_request)) { dialog, _ ->
                permissionManager.requestPermissions()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupButtonListeners() {
        binding.searchPassword.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        accountAdapter = AccountListAdapter(
            onLongClick = { account: SiteAccount -> showDeleteConfirmationDialog(account) },
            onClick = { account: SiteAccount ->
                Toast.makeText(this, "${account.site} clicked", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to DetailActivity
            },
        )

        binding.listPassword.apply {
            adapter = accountAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }
    }

    private fun loadAndDisplayData() {
        // Dummy data
        siteAccounts = mutableListOf(
            SiteAccount(
                id = 1, site = "Google", note = "Akun utama", credentials = listOf(
                    UserCredential("johndoe@gmail.com", "password123", Date()),
                    UserCredential("secondary.acc@gmail.com", "password456", Date())
                )
            ),
            SiteAccount(
                id = 2, site = "Facebook", note = "", credentials = listOf(
                    UserCredential("john_doe", "fb_pass", Date())
                )
            ),
            SiteAccount(
                id = 3, site = "Github", note = "Akun kerja", credentials = listOf(
                    UserCredential("johndoe_dev", "git_secret", Date()),
                    UserCredential("johndoe_dev", "git_secret", Date()),
                    UserCredential("johndoe_dev", "git_secret", Date()),
                )
            )
        )
        for (i in 4..20) {
            siteAccounts.add(
                SiteAccount(
                    id = i.toLong(), site = "Website $i", note = "Catatan $i", credentials = listOf(
                        UserCredential("user$i@web.com", "pass$i", Date())
                    )
                )
            )
        }

        accountAdapter.submitList(siteAccounts)
    }

    private fun showDeleteConfirmationDialog(accountToDelete: SiteAccount) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_delete))
            .setMessage("${getString(R.string.dialog_message_delete)} ${accountToDelete.site}?")
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                // Logic for removing items from the source list
                siteAccounts.remove(accountToDelete)
                // Send the updated list to the adapter
                // Important: send a copy of the new list for DiffUtil to work
                accountAdapter.submitList(siteAccounts.toList())

                Toast.makeText(this, getString(R.string.toast_success_delete), Toast.LENGTH_SHORT)
                    .show()
            }
            .show()
    }
}