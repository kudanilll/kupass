package com.nielcode.kupass.ui.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nielcode.kupass.R
import com.nielcode.kupass.core.PermissionManager
import com.nielcode.kupass.core.PreferenceManager
import com.nielcode.kupass.databinding.ActivityHomeBinding
import com.nielcode.kupass.ui.adapters.ListPasswordAdapter
import com.nielcode.kupass.ui.adapters.ListPasswordItem
import com.nielcode.kupass.utils.AppConfig
import com.nielcode.kupass.utils.FileHandler

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var adapter: ListPasswordAdapter
    private var listPasswordItems = ArrayList<ListPasswordItem>()

    // Daftar permission yang dibutuhkan.
    // Untuk Android 13 (API 33) ke atas, izin ini tidak lagi diperlukan untuk
    // mengakses file melalui Storage Access Framework (SAF).
    private val requiredPermissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        emptyList()
    }

    /**
     * Launcher untuk membuat file (Export). Menggantikan onActivityResult.
     */
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

    /**
     * Launcher untuk memilih file (Import). Menggantikan onActivityResult.
     */
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = FileHandler.readFileContent(this, it)
            if (content != null) {
                // TODO: Lakukan sesuatu dengan konten yang diimpor, misal parsing JSON
                Toast.makeText(this, getString(R.string.toast_success_import), Toast.LENGTH_SHORT)
                    .show()
                // Contoh: new MaterialAlertDialogBuilder(this).setMessage(content).show()
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
        loadDummyData()
        updateListView()
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
                // Semua izin diberikan, aplikasi bisa berjalan normal
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
            },
            onDenied = { deniedPermissions ->
                // Ada izin yang ditolak, beri tahu pengguna
                Toast.makeText(
                    this,
                    "Permissions Denied: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun checkAndRequestPermissions() {
        if (permissionManager.arePermissionsGranted()) {
            // Izin sudah ada, tidak perlu melakukan apa-apa
            return
        }

        // Tampilkan dialog penjelasan sebelum meminta izin
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
                // TODO: Tambahkan listener untuk menu import/export
                // R.id.menu_export -> {
                //    exportFileLauncher.launch("kupass_backup.json")
                //    true
                // }
                // R.id.menu_import -> {
                //    importFileLauncher.launch("*/*")
                //    true
                // }
                else -> false
            }
        }
    }

    private fun loadDummyData() {
        // Hapus data lama untuk menghindari duplikasi
        listPasswordItems.clear()
        // Tambah dummy data
        for (i in 0 until 20) {
            listPasswordItems.add(
                ListPasswordItem(
                    i.toLong(),
                    "Password $i",
                    "Username",
                    "Password",
                    "Note"
                )
            )
        }
    }

    private fun updateListView() {
        adapter = ListPasswordAdapter(this, listPasswordItems)
        binding.listPassword.adapter = adapter

        binding.listPassword.setOnItemClickListener { _, _, position, _ ->
            // Handle item click
            val item = listPasswordItems[position]
            Toast.makeText(this, "Clicked on ${item.passwordName}", Toast.LENGTH_SHORT).show()
        }

        binding.listPassword.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteConfirmationDialog(position)
            true // Mengindikasikan bahwa event telah di-handle
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_delete))
            .setMessage(getString(R.string.dialog_message_delete))
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                // Logika untuk menghapus item
                listPasswordItems.removeAt(position)
                Toast.makeText(this, getString(R.string.toast_success_delete), Toast.LENGTH_SHORT)
                    .show()
                adapter.notifyDataSetChanged() // Beri tahu adapter bahwa data telah berubah
            }
            .show()
    }
}