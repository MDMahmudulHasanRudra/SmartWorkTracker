package com.rudra.smartworktracker.ui.components

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rudra.smartworktracker.utils.BiometricHelper
import com.rudra.smartworktracker.utils.findFragmentActivity

private const val RELOCK_AFTER_MS = 60_000L

/**
 * Covers [content] with a lock screen while the biometric app lock is on. The content stays
 * composed underneath so navigation state survives a re-lock. [enabled] is null while the
 * setting is still loading, during which an opaque placeholder avoids flashing private data.
 */
@Composable
fun AppLockGate(enabled: Boolean?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var backgroundedAt by rememberSaveable { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    // Short trips (file picker, share sheet, rotation) don't re-lock
                    if (backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt > RELOCK_AFTER_MS) {
                        unlocked = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Never lock someone out of a device that has no way to authenticate
    val canLock = activity != null && remember(enabled) { BiometricHelper.isBiometricAvailable(context) }
    val locked = enabled == true && canLock && !unlocked

    fun authenticate() {
        val host = activity ?: return
        error = null
        BiometricHelper.authenticate(
            activity = host,
            title = "Unlock Smart Work Tracker",
            subtitle = "Confirm it's you",
            onSuccess = { unlocked = true },
            onError = { error = it }
        )
    }

    LaunchedEffect(locked) {
        if (locked) authenticate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        when {
            enabled == null -> Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            locked -> LockScreen(error = error, onUnlock = ::authenticate, onExit = { activity?.finish() })
        }
    }
}

@Composable
private fun LockScreen(error: String?, onUnlock: () -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            // Swallow touches so nothing underneath can be tapped
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Smart Work Tracker is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                error ?: "Use your fingerprint, face or screen lock to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Unlock")
            }
        }
    }
}
