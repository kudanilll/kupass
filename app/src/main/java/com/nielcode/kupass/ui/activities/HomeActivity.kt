package com.nielcode.kupass.ui.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.R
import com.nielcode.kupass.core.PermissionManager
import com.nielcode.kupass.core.PreferenceManager
import com.nielcode.kupass.databinding.ActivityHomeBinding
import com.nielcode.kupass.databinding.NavHeaderBinding
import com.nielcode.kupass.model.SiteAccount
import com.nielcode.kupass.model.UserCredential
import com.nielcode.kupass.ui.adapters.AccountListAdapter
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.FileHandler
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var accountAdapter: AccountListAdapter

    private val siteAccounts = mutableListOf<SiteAccount>()       // source of truth
    private var filtered = listOf<SiteAccount>()                  // last filtered list

    private val requiredPermissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else emptyList()

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
        setupRecyclerView()
        setupTopBarAndDrawer()
        setupSearch()
        setupNavigationView()
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
        if (requiredPermissions.isEmpty() || permissionManager.arePermissionsGranted()) return
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

    private fun setupRecyclerView() {
        accountAdapter = AccountListAdapter(
            onLongClick = { account -> showDeleteConfirmationDialog(account) },
            onClick = { account ->
                // TODO: Navigate to DetailActivity
            },
        )
        binding.listPassword.apply {
            adapter = accountAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }
    }

    private fun setupTopBarAndDrawer() {
        // Set layout padding top
        binding.mainContent.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarHeight = insets.systemWindowInsetTop
            binding.mainContent.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        // Hamburger opens the left drawer
        binding.searchPassword.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        // Right search icon opens the SearchView
        binding.searchPassword.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.menu_search) {
                binding.searchView.show()
                true
            } else false
        }

        // Link SearchView with SearchBar (handles motion + insets)
        binding.searchView.setupWithSearchBar(binding.searchPassword)
    }

    private fun setupNavigationView() {
        val navigationView = binding.navigationView

        // Set Version Text
        val headerView = navigationView.getHeaderView(0)
        val headerBinding = NavHeaderBinding.bind(headerView)
        headerBinding.appVersion.text = BuildConfig.VERSION_NAME

        /*
        // Set Footer View
        layoutInflater.inflate(R.layout.nav_header, navigationView, false)
        val footerView = layoutInflater.inflate(R.layout.nav_footer, navigationView, false)
        navigationView.addView(footerView)
        val params = footerView.layoutParams
        footerView.layoutParams = params

        // Footer Button Click Listener
        val footerBinding = NavFooterBinding.bind(footerView)
        footerBinding.githubButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, BuildConfig.GIT_URL.toUri())
            startActivity(intent)
        }
        */

        // Set Navigation View Item Click Listener
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    binding.drawerLayout.closeDrawers()
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                R.id.nav_export -> {
                    // TODO: Handle export
                    true
                }

                R.id.nav_import -> {
                    // TODO: Handle import
                    true
                }

                else -> false
            }
        }
    }

    private fun setupSearch() {
        // Real-time filtering
        val editText = binding.searchView.editText
        editText.doOnTextChanged { text, _, _, _ ->
            filterAndSubmit(text?.toString().orEmpty())
        }

        // Submit from keyboard IME
        editText.setOnEditorActionListener { v, _, _ ->
            filterAndSubmit(v.text.toString())
            binding.searchView.hide()
            true
        }

        // Reset list when SearchView is closed
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.HIDDEN) {
                accountAdapter.submitList(siteAccounts.toList())
                filtered = siteAccounts
            }
        }
    }

    private fun filterAndSubmit(query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        filtered = if (q.isEmpty()) {
            siteAccounts
        } else {
            siteAccounts.filter { account ->
                val inSite = account.site.lowercase().contains(q)
                val inNote = account.note?.lowercase()?.contains(q) == true
                val inCreds = account.credentials.any { cred ->
                    (cred.username.lowercase().contains(q))
                }
                inSite || inNote || inCreds
            }
        }
        accountAdapter.submitList(filtered.toList())
    }

    private fun loadAndDisplayData() {
        siteAccounts.clear()
        siteAccounts += listOf(
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
            siteAccounts += SiteAccount(
                id = i.toLong(),
                site = "Website $i",
                note = "Catatan $i",
                credentials = listOf(UserCredential("user$i@web.com", "pass$i", Date()))
            )
        }
        accountAdapter.submitList(siteAccounts.toList())
        filtered = siteAccounts
    }

    private fun showDeleteConfirmationDialog(accountToDelete: SiteAccount) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_delete))
            .setMessage("${getString(R.string.dialog_message_delete)} ${accountToDelete.site}?")
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                siteAccounts.remove(accountToDelete)
                accountAdapter.submitList(siteAccounts.toList())
                Toast.makeText(this, getString(R.string.toast_success_delete), Toast.LENGTH_SHORT)
                    .show()
            }
            .show()
    }
}
