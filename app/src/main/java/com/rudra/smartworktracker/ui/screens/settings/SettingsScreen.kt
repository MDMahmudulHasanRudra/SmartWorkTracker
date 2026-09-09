package com.rudra.smartworktracker.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.utils.SUPPORTED_CURRENCIES
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(application))
    val scope = rememberCoroutineScope()

    var showResetDialog by remember { mutableStateOf(false) }
    var showMealRateDialog by remember { mutableStateOf(false) }
    var showOvertimeRateDialog by remember { mutableStateOf(false) }
    var showDailyWorkHoursDialog by remember { mutableStateOf(false) }
    var showWorkingDaysPerWeekDialog by remember { mutableStateOf(false) }

    val mealRate by viewModel.mealRate.collectAsState()
    var newMealRate by remember(mealRate) { mutableStateOf(mealRate.toString()) }
    val overtimeRate by viewModel.overtimeRate.collectAsState()
    var newOvertimeRate by remember(overtimeRate) { mutableStateOf(overtimeRate.toString()) }
    val dailyWorkHours by viewModel.dailyWorkHours.collectAsState()
    var newDailyWorkHours by remember(dailyWorkHours) { mutableStateOf(dailyWorkHours.toString()) }
    val workingDaysPerWeek by viewModel.workingDaysPerWeek.collectAsState()
    var newWorkingDaysPerWeek by remember(workingDaysPerWeek) { mutableStateOf(workingDaysPerWeek.toString()) }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val currency by viewModel.currency.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.backupResult.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.restoreResult.collect { result ->
            result.onSuccess {
                Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Restore failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance Section
            item {
                SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Switch between light and dark mode",
                        isChecked = isDarkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) }
                    )
                }
            }

            // Security Section
            item {
                SettingsSection(title = "Security", icon = Icons.Default.Lock) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Lock",
                        subtitle = "Require fingerprint/face to open app",
                        isChecked = biometricEnabled,
                        onCheckedChange = { viewModel.setBiometric(it) }
                    )
                }
            }

            // Notifications Section
            item {
                SettingsSection(title = "Notifications", icon = Icons.Default.Notifications) {
                    SettingsSwitchItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Enable Notifications",
                        subtitle = "Receive app notifications",
                        isChecked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotifications(it) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = "Enable Vibration",
                        subtitle = "Vibrate on notifications",
                        isChecked = vibrationEnabled,
                        onCheckedChange = { viewModel.setVibration(it) }
                    )
                }
            }

            // Financial Settings Section
            item {
                SettingsSection(title = "Financial", icon = Icons.Default.AttachMoney) {
                    val currencyName = SUPPORTED_CURRENCIES.find { it.code == currency }?.name ?: currency
                    val currencySymbol = SUPPORTED_CURRENCIES.find { it.code == currency }?.symbol ?: currency
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        subtitle = "Current: $currencySymbol $currency — $currencyName",
                        onClick = { showCurrencyDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Restaurant,
                        title = "Meal Rate",
                        subtitle = "Current: ${CurrencyManager.format(mealRate).removePrefix(CurrencyManager.symbol())} per meal",
                        onClick = { showMealRateDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Overtime Rate",
                        subtitle = "Current: ${CurrencyManager.format(overtimeRate).removePrefix(CurrencyManager.symbol())} per hour",
                        onClick = { showOvertimeRateDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.AccessTime,
                        title = "Daily Work Hours",
                        subtitle = "Current: $dailyWorkHours hours",
                        onClick = { showDailyWorkHoursDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.CalendarMonth,
                        title = "Working Days / Week",
                        subtitle = "Current: $workingDaysPerWeek days",
                        onClick = { showWorkingDaysPerWeekDialog = true }
                    )
                }
            }

            // Data Management Section
            item {
                SettingsSection(title = "Data Management", icon = Icons.Default.Storage) {
                    SettingsSwitchItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Auto Backup",
                        subtitle = "Automatically backup your data",
                        isChecked = autoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackup(it) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Backup Data",
                        subtitle = "Create a backup file of your data",
                        onClick = {
                            backupLauncher.launch("smart_work_tracker_backup_${System.currentTimeMillis()}.json")
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Restore,
                        title = "Restore Data",
                        subtitle = "Restore from previous backup file",
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        title = "Export as CSV",
                        subtitle = "Export income & expenses to a spreadsheet",
                        onClick = {
                            scope.launch {
                                try {
                                    val file = com.rudra.smartworktracker.utils.CsvExporter.exportAll(context)
                                    com.rudra.smartworktracker.utils.CsvExporter.shareFile(context, file)
                                    Toast.makeText(context, "CSV exported!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.CalendarMonth,
                        title = "Export to Calendar",
                        subtitle = "Export recurring transactions to calendar app",
                        onClick = {
                            scope.launch {
                                try {
                                    val database = com.rudra.smartworktracker.data.AppDatabase.getDatabase(context)
                                    val repository = com.rudra.smartworktracker.data.repository.RecurringRepository(
                                        database.recurringRuleDao(),
                                        database.recurringTransactionDao()
                                    )
                                    val rules = repository.getAllRules().first()
                                    val uri = com.rudra.smartworktracker.utils.CalendarExporter.exportToIcs(context, rules)
                                    if (uri != null) {
                                        com.rudra.smartworktracker.utils.CalendarExporter.shareCalendar(context, uri)
                                        Toast.makeText(context, "Calendar exported!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Reset All Data",
                        subtitle = "Permanently delete all app data",
                        onClick = { showResetDialog = true },
                        isDestructive = true
                    )
                }
            }

            // About Section
            item {
                SettingsSection(title = "About", icon = Icons.Default.Info) {
                    SettingsItem(
                        icon = Icons.Default.Shield,
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rudra2001-coder.github.io/my/"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "View terms and conditions",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rudra2001-coder.github.io/my/"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "Contact Support",
                        subtitle = "Get help and support",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:mhrudra064@gmail.com\n")
                                putExtra(Intent.EXTRA_SUBJECT, "Smart Work Tracker Support")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Star,
                        title = "Rate App",
                        subtitle = "Share your experience",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                context.startActivity(webIntent)
                            }
                        }
                    )
                }
            }

            // App Version
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Smart Work Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Version 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // Meal Rate Dialog
    if (showMealRateDialog) {
        AlertDialog(
            onDismissRequest = { showMealRateDialog = false },
            title = {
                Text(
                    "Set Meal Rate",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        "Set the default cost per meal for expense calculations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newMealRate,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                newMealRate = it
                            }
                        },
                        label = { Text("Meal Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        prefix = { Text(CurrencyManager.symbol()) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newMealRate.toDoubleOrNull()?.let { rate ->
                            if (rate >= 0) {
                                viewModel.setMealRate(rate)
                                showMealRateDialog = false
                            }
                        }
                    },
                    enabled = newMealRate.toDoubleOrNull() != null && newMealRate.toDoubleOrNull()!! >= 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMealRateDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Overtime Rate Dialog
    if (showOvertimeRateDialog) {
        AlertDialog(
            onDismissRequest = { showOvertimeRateDialog = false },
            title = {
                Text(
                    "Set Overtime Rate",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        "Set the hourly rate for overtime work",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newOvertimeRate,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                newOvertimeRate = it
                            }
                        },
                        label = { Text("Overtime Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        prefix = { Text(CurrencyManager.symbol()) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newOvertimeRate.toDoubleOrNull()?.let { rate ->
                            if (rate >= 0) {
                                viewModel.setOvertimeRate(rate)
                                showOvertimeRateDialog = false
                            }
                        }
                    },
                    enabled = newOvertimeRate.toDoubleOrNull() != null && newOvertimeRate.toDoubleOrNull()!! >= 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOvertimeRateDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Daily Work Hours Dialog
    if (showDailyWorkHoursDialog) {
        AlertDialog(
            onDismissRequest = { showDailyWorkHoursDialog = false },
            title = {
                Text(
                    "Set Daily Work Hours",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        "Standard number of work hours per day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newDailyWorkHours,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                newDailyWorkHours = it
                            }
                        },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("hrs") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newDailyWorkHours.toDoubleOrNull()?.let { hours ->
                            if (hours > 0) {
                                viewModel.setDailyWorkHours(hours)
                                showDailyWorkHoursDialog = false
                            }
                        }
                    },
                    enabled = newDailyWorkHours.toDoubleOrNull() != null && newDailyWorkHours.toDoubleOrNull()!! > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDailyWorkHoursDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Working Days Per Week Dialog
    if (showWorkingDaysPerWeekDialog) {
        val dayOptions = listOf(1, 2, 3, 4, 5, 6, 7)
        AlertDialog(
            onDismissRequest = { showWorkingDaysPerWeekDialog = false },
            title = {
                Text(
                    "Set Working Days / Week",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        "Number of working days per week",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    dayOptions.forEach { day ->
                        val isSelected = day == workingDaysPerWeek
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    viewModel.setWorkingDaysPerWeek(day)
                                    showWorkingDaysPerWeekDialog = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$day day${if (day > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showWorkingDaysPerWeekDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filtered = SUPPORTED_CURRENCIES.filter {
            searchQuery.isBlank() || it.code.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Text(
                    "Select Currency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        items(filtered) { option ->
                            val isSelected = option.code == currency
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        viewModel.setCurrency(option.code)
                                        showCurrencyDialog = false
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        option.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        option.code,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        option.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCurrencyDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Reset Data Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    "Reset All Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "This will permanently delete:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• All journal entries")
                    Text("• Financial records")
                    Text("• User preferences")
                    Text("• App settings")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "This action cannot be undone!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                        Toast.makeText(context, "All data has been reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset All Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Section Content
            content()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

    val subtitleColor = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
