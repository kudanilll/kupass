package com.nielcode.kupass.data

import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.data.repository.PasswordRepository

/** Process-wide data dependencies that feature modules may use (no DI framework). */
interface CoreContainer {
    val passwordRepository: PasswordRepository
    val preferenceManager: PreferenceManager
}

/** Implemented by the Application so feature modules can reach [CoreContainer]. */
interface CoreContainerProvider {
    val coreContainer: CoreContainer
}
