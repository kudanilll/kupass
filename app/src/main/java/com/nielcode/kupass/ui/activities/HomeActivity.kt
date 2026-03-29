package com.nielcode.kupass.ui.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.nielcode.kupass.R
import com.nielcode.kupass.core.PermissionManager
import com.nielcode.kupass.core.PreferenceManager
import com.nielcode.kupass.core.crypto.CryptoManager
import com.nielcode.kupass.core.crypto.SqlCipherKey
import com.nielcode.kupass.data.local.AppDatabase
import com.nielcode.kupass.data.repository.PasswordRepositoryImpl
import com.nielcode.kupass.databinding.ActivityHomeBinding
import com.nielcode.kupass.model.SiteAccount
import com.nielcode.kupass.ui.adapters.AccountListAdapter
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.FileHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var accountAdapter: AccountListAdapter
    private lateinit var searchAdapter: AccountListAdapter

    private val siteAccounts = mutableListOf<SiteAccount>()       // source of truth
    private var filtered = listOf<SiteAccount>()                  // last filtered list
    private var isSearchOpen = false

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

    private val repo by lazy {
        val passphrase = SqlCipherKey.getOrCreate(this)
        val db = AppDatabase.get(this, passphrase)
        val crypto = CryptoManager(this)
        PasswordRepositoryImpl(db.siteDao(), crypto)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDynamicColors()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setupPermissionManager()
        checkAndRequestPermissions()
        setupRecyclerView()
        setupTopBarAndDrawer()
        setupSearch()
        setupNavigationView()
        loadAndDisplayData()

        binding.fab.setOnClickListener {
            startActivity(Intent(this, CreatePasswordActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If the search is open, close it first and reset the list.
                val open = binding.searchView.currentTransitionState ==
                        SearchView.TransitionState.SHOWN ||
                        binding.searchView.currentTransitionState ==
                        SearchView.TransitionState.SHOWING
                if (open) {
                    // Clear the query so that no filters remain.
                    binding.searchView.editText.text?.clear()
                    // Close search overlay
                    binding.searchView.hide()
                    // fallback: immediately restore the main list
                    accountAdapter.submitList(siteAccounts.toList())
                    filtered = siteAccounts
                    updateEmptyState()
                } else {
                    // not searching → continue default back
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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
            .setPositiveButton(getString(R.string.button_request)) { dialog, _ ->
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
                val intent = Intent(this@HomeActivity, ShowPasswordActivity::class.java)
                intent.putExtra(ShowPasswordActivity.EXTRA_SITE_ACCOUNT, account)
                startActivity(intent)
            },
        )

        binding.listPassword.apply {
            adapter = accountAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }

        // Adapter inside SearchView (fullscreen)
        searchAdapter = AccountListAdapter(
            onLongClick = { account -> showDeleteConfirmationDialog(account) },
            onClick = { account ->
                val intent = Intent(this@HomeActivity, ShowPasswordActivity::class.java)
                intent.putExtra(ShowPasswordActivity.EXTRA_SITE_ACCOUNT, account)
                startActivity(intent)
            },
        )

        binding.searchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }
    }

    private fun setupTopBarAndDrawer() {
        // Set layout padding top
        binding.mainContent.setOnApplyWindowInsetsListener { _, insets ->
            val statusBarHeight = insets.systemWindowInsetTop
            binding.mainContent.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        binding.searchPassword.setOnClickListener { binding.searchView.show() }
        binding.searchView.setupWithSearchBar(binding.searchPassword)
    }

    private fun setupNavigationView() {
        val nav = binding.bottomNavigation

        // Handle click menu
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_export -> {
                    exportFileLauncher.launch("kupass_backup.json")
                    true
                }

                R.id.nav_import -> {
                    importFileLauncher.launch("application/json")
                    true
                }

                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    private fun setupSearch() {
        val editText = binding.searchView.editText

        editText.doOnTextChanged { text, _, _, _ ->
            filterAndSubmit(text?.toString().orEmpty())
        }

        editText.setOnEditorActionListener { v, _, _ ->
            filterAndSubmit(v.text.toString())
            binding.searchView.hide()
            true
        }

        binding.searchView.addTransitionListener { _: SearchView, _: SearchView.TransitionState, newState: SearchView.TransitionState ->
            isSearchOpen =
                newState == SearchView.TransitionState.SHOWN || newState == SearchView.TransitionState.SHOWING
            if (!isSearchOpen) {
                // The search has just been closed (e.g., click back on the left side of SearchView).
                // Clear the query + clean the overlay adapter so it doesn't “stick”
                binding.searchView.editText.text?.clear()
                searchAdapter.submitList(emptyList())

                // Restore full main list
                accountAdapter.submitList(siteAccounts.toList())
                filtered = siteAccounts
                updateEmptyState()
            } else {
                // When opened, synchronize the initial results with the current query (if any).
                val q = binding.searchView.editText.text?.toString().orEmpty()
                filterAndSubmit(q)
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
                    cred.username.lowercase().contains(q)
                }
                inSite || inNote || inCreds
            }
        }

        if (isSearchOpen) {
            // show in RecyclerView in SearchView (overlay)
            searchAdapter.submitList(filtered.toList())
        } else {
            // show in the main list
            accountAdapter.submitList(filtered.toList())
            binding.emptyState.isVisible = filtered.isEmpty()
            binding.listPassword.isVisible = filtered.isNotEmpty()
        }
    }

    private fun loadAndDisplayData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.observeAll().collectLatest { list ->
                    // Group first before using the entire flow (search/filter/empty state)
                    val grouped = groupAccounts(list)

                    siteAccounts.clear()
                    siteAccounts += grouped

                    val currentQuery = binding.searchView.editText.text?.toString().orEmpty()
                    if (currentQuery.isNotBlank()) {
                        filterAndSubmit(currentQuery) // will filter from ‘siteAccounts’ that have been grouped
                    } else {
                        accountAdapter.submitList(grouped.toList())
                        filtered = grouped
                        updateEmptyState()
                    }
                }
            }
        }
    }

    /** Group accounts by site (case-insensitive), merge credentials, pick first non-empty note. */
    private fun groupAccounts(list: List<SiteAccount>): List<SiteAccount> {
        return list
            .groupBy { it.site.trim().lowercase(Locale.getDefault()) }
            .map { (_, group) ->
                val siteName = group.first().site
                val id = group.minOf { it.id } // id stable for display
                val note = group.firstOrNull { !it.note.isNullOrBlank() }?.note
                val mergedCreds = group
                    .flatMap { it.credentials }
                    .distinctBy { it.username } // avoid duplicate usernames

                SiteAccount(
                    id = id,
                    site = siteName,
                    note = note,
                    credentials = mergedCreds
                )
            }
            .sortedBy { it.site.lowercase(Locale.getDefault()) }
    }

    private fun updateEmptyState() {
        val isEmpty = siteAccounts.isEmpty()
        binding.emptyState.isVisible = isEmpty
        binding.listPassword.isVisible = !isEmpty
    }

    private fun showDeleteConfirmationDialog(accountToDelete: SiteAccount) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_delete))
            .setMessage("${getString(R.string.dialog_message_delete)} ${accountToDelete.site}?")
            .setNegativeButton(getString(R.string.button_cancel), null)
            .setPositiveButton(getString(R.string.button_delete)) { _, _ ->
                // Delete from the database (will be automatically reflected via Flow observeAll)
                lifecycleScope.launch {
                    repo.deleteSite(accountToDelete.id)
                    Toast.makeText(
                        this@HomeActivity,
                        getString(R.string.toast_success_delete),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}
