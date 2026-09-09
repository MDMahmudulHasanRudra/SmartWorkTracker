package com.rudra.smartworktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import com.rudra.smartworktracker.alarm.RecurringNotificationWorker
import com.rudra.smartworktracker.data.SampleData
import com.rudra.smartworktracker.data.SharedPreferenceManager
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.ui.navigation.MainApp
import com.rudra.smartworktracker.ui.theme.SmartWorkTrackerTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferenceManager = SharedPreferenceManager(this)
        if (sharedPreferenceManager.getWorkLogs().isEmpty()) {
            sharedPreferenceManager.saveWorkLogs(SampleData.getSampleWorkLogs())
        }

        RecurringNotificationWorker.schedule(this)

        setContent {
            SmartWorkTrackerMain()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SmartWorkTrackerMain() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val isDarkTheme by settingsRepository.darkTheme.collectAsState(initial = isSystemInDarkTheme())

    SmartWorkTrackerTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainApp()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    SmartWorkTrackerMain()
}