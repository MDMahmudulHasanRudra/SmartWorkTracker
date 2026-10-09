package com.rudra.smartworktracker.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: BackupViewModel = viewModel(factory = BackupViewModelFactory(context))
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsStateWithLifecycle()
    val nextBackupTime by viewModel.nextBackupTime.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestore.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::inspectBackup)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::createBackup)
    }

    LaunchedEffect(Unit) {
        viewModel.backupResult.collect { result ->
            snackbarHostState.showSnackbar(
                when (result) {
                    is BackupResult.Success -> result.message
                    is BackupResult.Error -> result.message
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            StatusHeader(
                lastBackup = if (lastBackupTime > 0) dateFormat.format(Date(lastBackupTime)) else "Never",
                nextBackup = when {
                    !isAutoBackupEnabled -> "Off"
                    nextBackupTime > 0 -> dateFormat.format(Date(nextBackupTime))
                    else -> "Scheduled"
                },
                isAutoEnabled = isAutoBackupEnabled,
                stale = lastBackupTime == 0L || System.currentTimeMillis() - lastBackupTime > STALE_AFTER_MS
            )

            AutoBackupToggleCard(
                isEnabled = isAutoBackupEnabled,
                lastAuto = if (lastAutoBackupTime > 0) dateFormat.format(Date(lastAutoBackupTime)) else null,
                onToggle = viewModel::toggleAutoBackup
            )

            SectionLabel("Back up")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Save file",
                    icon = Icons.Default.Save,
                    color = MaterialTheme.colorScheme.primary,
                    description = "Choose where to save",
                    enabled = !isLoading,
                    onClick = {
                        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        backupLauncher.launch("smart_work_backup_$ts.json")
                    }
                )
                ActionTile(
                    modifier = Modifier.weight(1f),
                    title = "To Downloads",
                    icon = Icons.Default.Download,
                    color = MaterialTheme.colorScheme.secondary,
                    description = "Quick backup now",
                    enabled = !isLoading,
                    onClick = viewModel::backupNowToDownloads
                )
            }

            SectionLabel("Restore")
            ActionTile(
                modifier = Modifier.fillMaxWidth(),
                title = "Restore from a backup file",
                icon = Icons.Default.Restore,
                color = MaterialTheme.colorScheme.tertiary,
                description = "You'll see what's inside before anything changes",
                enabled = !isLoading,
                onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
            )

            DetailedInfoCard(
                title = "How backups work",
                icon = Icons.Default.Info,
                detail = "Backups are plain JSON files containing all of your records and settings. " +
                    "Daily backups are written to your Downloads folder around 12:05 AM. " +
                    "Restoring merges the file into your current data: records with the same ID are " +
                    "replaced, everything else is kept."
            )

            DetailedInfoCard(
                title = "Moving to a new phone",
                icon = Icons.Default.PhoneAndroid,
                detail = "Save a backup file to cloud storage or send it to yourself, install the app on " +
                    "the new phone, then use Restore."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    pendingRestore?.let { pending ->
        val summary = pending.summary
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Restore this backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Created ${dateFormat.format(Date(summary.timestamp))} (app ${summary.appVersion})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (summary.counts.isEmpty()) {
                        Text("The backup contains settings only.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        summary.counts.forEach { (label, count) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                                Text("$count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text(
                        "Matching records are replaced; your other data is kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { Button(onClick = viewModel::confirmRestore) { Text("Restore") } },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancel") } }
        )
    }
}

private const val STALE_AFTER_MS = 7L * 24 * 60 * 60 * 1000

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun StatusHeader(lastBackup: String, nextBackup: String, isAutoEnabled: Boolean, stale: Boolean) {
    val container = when {
        stale -> MaterialTheme.colorScheme.errorContainer
        isAutoEnabled -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val onContainer = when {
        stale -> MaterialTheme.colorScheme.onErrorContainer
        isAutoEnabled -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(onContainer.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        stale -> Icons.Default.Warning
                        isAutoEnabled -> Icons.Default.CloudDone
                        else -> Icons.Default.CloudOff
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = onContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                when {
                    stale && lastBackup == "Never" -> "No backup yet"
                    stale -> "Last backup is over a week old"
                    isAutoEnabled -> "Daily backups are on"
                    else -> "Backups are manual"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("Last backup", lastBackup, Alignment.Start, onContainer)
                InfoColumn("Next automatic", nextBackup, Alignment.End, onContainer)
            }
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, alignment: Alignment.Horizontal, color: Color) {
    Column(horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun AutoBackupToggleCard(isEnabled: Boolean, lastAuto: String?, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily auto-backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    lastAuto?.let { "Last automatic backup: $it" } ?: "Saves a copy to Downloads every night",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isEnabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun ActionTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = color)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DetailedInfoCard(title: String, icon: ImageVector, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
