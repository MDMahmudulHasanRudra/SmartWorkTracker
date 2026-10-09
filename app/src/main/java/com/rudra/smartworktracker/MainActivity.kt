package com.rudra.smartworktracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rudra.smartworktracker.alarm.RecurringNotificationWorker
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.data.repository.ThemeMode
import com.rudra.smartworktracker.ui.components.AppLockGate
import com.rudra.smartworktracker.ui.navigation.MainApp
import com.rudra.smartworktracker.ui.theme.SmartWorkTrackerTheme

// FragmentActivity (a ComponentActivity) so BiometricPrompt can host the app lock
class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        RecurringNotificationWorker.schedule(this)
        if (savedInstanceState == null) requestNotificationPermissionIfNeeded()

        // Set by recurring-transaction notifications, e.g. "recurring"
        val deepLinkRoute = intent.getStringExtra(EXTRA_NAVIGATE_TO)

        setContent {
            SmartWorkTrackerMain(deepLinkRoute = deepLinkRoute)
        }
    }

    /** Alarms, recurring transactions and duty reminders all post notifications (Android 13+). */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
}

@Composable
fun SmartWorkTrackerMain(deepLinkRoute: String? = null) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = false)
    val accentColor by settingsRepository.accentColor.collectAsState(initial = 0)
    val fontSize by settingsRepository.fontSize.collectAsState(initial = 1.0)
    val appLockEnabled by settingsRepository.biometric.collectAsState(initial = null)

    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SmartWorkTrackerTheme(
        darkTheme = isDarkTheme,
        dynamicColor = dynamicColor,
        accentColorIndex = accentColor,
        fontScale = fontSize.toFloat()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppLockGate(enabled = appLockEnabled) {
                MainApp(deepLinkRoute = deepLinkRoute)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    SmartWorkTrackerMain()
}
