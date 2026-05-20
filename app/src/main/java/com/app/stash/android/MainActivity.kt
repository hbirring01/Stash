package com.app.stash.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.stash.android.ui.AppViewModel
import com.app.stash.android.ui.auth.BiometricGate
import com.app.stash.android.ui.navigation.AppNavGraph
import com.app.stash.android.ui.theme.CreditCardAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE: hide the app from screenshots, screen recording, and the
        // Recents thumbnail. Card numbers, balances, and the Plaid setup form
        // are never rendered to non-protected surfaces. Casting/mirroring to an
        // insecure display is also blocked.
        //
        // Disabled in debug builds so dev screenshots, the Layout Inspector,
        // and emulator screen capture work for development. Release builds
        // (the only thing users install) always have FLAG_SECURE on.
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        // Tap-jacking defense: drop any touch event delivered while an obscuring
        // overlay (rogue notification toast, accessibility shim, draw-over-other-
        // apps) is on top of the activity.
        window.decorView.filterTouchesWhenObscured = true

        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            CreditCardAppTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BiometricGate {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
