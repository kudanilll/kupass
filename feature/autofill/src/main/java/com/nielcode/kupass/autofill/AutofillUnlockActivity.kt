package com.nielcode.kupass.autofill

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Unlock and account picker. Placeholder until the biometric flow lands: fills nothing. */
class AutofillUnlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        finish()
    }
}
