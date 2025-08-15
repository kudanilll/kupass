package com.nielcode.kupass.core

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * A reusable class to manage runtime permissions using the modern Activity Result API.
 *
 * @param activity The activity that is requesting the permissions.
 * @param permissions The list of permissions to request.
 * @param onGranted Callback function to be invoked when all permissions are granted.
 * @param onDenied Callback function to be invoked when any permission is denied.
 * It provides a list of the permissions that were denied.
 */
class PermissionManager(
    private val activity: ComponentActivity,
    private val permissions: List<String>,
    private val onGranted: () -> Unit,
    private val onDenied: (List<String>) -> Unit
) {

    /**
     * The ActivityResultLauncher that handles the permission request.
     * It processes the results and triggers the appropriate callback.
     */
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val deniedPermissions = permissionsMap.filter { !it.value }.keys.toList()
        if (deniedPermissions.isEmpty()) {
            onGranted()
        } else {
            onDenied(deniedPermissions)
        }
    }

    /**
     * Checks if all the required permissions are already granted.
     * @return `true` if all permissions are granted, `false` otherwise.
     */
    fun arePermissionsGranted(): Boolean {
        if (permissions.isEmpty()) return true
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Launches the permission request dialog to the user.
     * The result will be handled by the launcher's callback.
     */
    fun requestPermissions() {
        permissionLauncher.launch(permissions.toTypedArray())
    }
}