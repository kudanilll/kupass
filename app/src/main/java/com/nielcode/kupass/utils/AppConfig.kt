package com.nielcode.kupass.utils

object AppConfig {
    const val FILES_PREFIX = "kupass_backup_"
    const val EMPTY_STRING = ""

    object Language {
        // Storing the actual language tag is more robust than an index.
        const val ENGLISH_TAG = "en"
        const val INDONESIA_TAG = "id"

        object Code {
            const val ENGLISH = 0
            const val INDONESIA = 1
        }
    }

    object Theme {
        object Code {
            const val SYSTEM = 0
            const val LIGHT = 1
            const val DARK = 2
        }
    }

    object DynamicColors {
        object Code {
            const val NOT_SUPPORTED = 0
            const val ENABLE = 1
            const val DISABLE = 2
        }
    }
}