package com.rudra.smartworktracker.ui.screens.overtime

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.WorkLog
import java.text.SimpleDateFormat
import java.util.*
import com.rudra.smartworktracker.utils.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeScreen(viewModel: OvertimeViewModel = viewModel()) {
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var overtimeRate by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val startTimePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
    val endTimePickerState = rememberTimePickerState(initialHour = 18, initialMinute = 0)

    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    val defaultRate by viewModel.defaultOvertimeRate.collectAsState()
    // Pre-fill the rate from Settings so entries don't silently earn 0
    LaunchedEffect(showAddSheet, defaultRate) {
        if (showAddSheet && overtimeRate.isBlank() && defaultRate > 0) {
            overtimeRate = String.format(Locale.US, "%.2f", defaultRate).trimEnd('0').trimEnd('.')
        }
    }

    val monthlyLogs by viewModel.monthlyOvertimeLogs.collectAsState()
    val yearlyLogs by viewModel.yearlyOvertimeLogs.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState(initial = OvertimeSummary())
    val yearlySummary by viewModel.yearlySummary.collectAsState(initial = OvertimeSummary())

    var showDeleteDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<WorkLog?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Color(0xFFFF6B6B),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Overtime")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            OvertimeHeader()
            Spacer(modifier = Modifier.height(16.dp))

            OvertimeSummaryView(monthlySummary, yearlySummary, viewModel.getMonthName(), viewModel.getYearString())
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Overtime History",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OvertimeLogsList(monthlyLogs, yearlyLogs, viewModel) { log ->
                logToDelete = log
                showDeleteDialog = true
            }
        }

        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                AddOvertimeContent(
                    startTime = startTime,
                    endTime = endTime,
                    overtimeRate = overtimeRate,
                    selectedDate = selectedDate,
                    onStartTimeChange = { startTime = it },
                    onEndTimeChange = { endTime = it },
                    onOvertimeRateChange = { overtimeRate = it },
                    onShowDatePicker = { showDatePicker = true },
                    onShowStartTimePicker = { showStartTimePicker = true },
                    onShowEndTimePicker = { showEndTimePicker = true },
                    onSave = {
                        if (viewModel.saveOvertime(selectedDate!!, startTime, endTime, overtimeRate.toDoubleOrNull() ?: 0.0)) {
                            Toast.makeText(context, "✓ Overtime Saved!", Toast.LENGTH_SHORT).show()
                            startTime = ""
                            endTime = ""
                            overtimeRate = ""
                            showAddSheet = false
                        } else {
                            Toast.makeText(context, "Invalid time format. Please use HH:mm", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Shown from an effect: calling show() during composition stacked a new dialog on every
        // recomposition, and cancelling left showDatePicker stuck at true
        LaunchedEffect(showDatePicker) {
            if (!showDatePicker) return@LaunchedEffect
            val calendar = Calendar.getInstance().apply { time = selectedDate ?: Date() }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                setOnDismissListener { showDatePicker = false }
            }.show()
        }

        if (showStartTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showStartTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startTime = String.format("%02d:%02d", startTimePickerState.hour, startTimePickerState.minute)
                        showStartTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
                }
            ) {
                TimePicker(state = startTimePickerState)
            }
        }

        if (showEndTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showEndTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        endTime = String.format("%02d:%02d", endTimePickerState.hour, endTimePickerState.minute)
                        showEndTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
                }
            ) {
                TimePicker(state = endTimePickerState)
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Overtime Log") },
                text = { Text("Are you sure you want to delete this log?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            logToDelete?.let { log ->
                                viewModel.deleteOvertime(log)
                                Toast.makeText(context, "Log Deleted", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun OvertimeHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bolt, "Overtime", tint = Color(0xFFFF6B6B), modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Overtime Tracking", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun OvertimeSummaryView(monthlySummary: OvertimeSummary, yearlySummary: OvertimeSummary, monthName: String, year: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(16.dp)) {
            SummaryColumn("This Month ($monthName)", monthlySummary, Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.height(60.dp).padding(horizontal = 8.dp))
            SummaryColumn("This Year ($year)", yearlySummary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryColumn(title: String, summary: OvertimeSummary, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${String.format("%.2f", summary.totalHours)}h", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("${CurrencyManager.format(summary.totalEarnings)}", color = Color(0xFF00C853), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AddOvertimeContent(
    startTime: String, endTime: String, overtimeRate: String, selectedDate: Date?,
    onStartTimeChange: (String) -> Unit, onEndTimeChange: (String) -> Unit, onOvertimeRateChange: (String) -> Unit,
    onShowDatePicker: () -> Unit, onShowStartTimePicker: () -> Unit, onShowEndTimePicker: () -> Unit, onSave: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFFF6B6B),
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = Color(0xFFFF6B6B),
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTextColor = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Add New Overtime",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = selectedDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "Select a date",
            onValueChange = {}, label = { Text("Date") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, "Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowDatePicker),
            shape = RoundedCornerShape(12.dp), colors = textFieldColors, readOnly = true, enabled = false
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = startTime, onValueChange = onStartTimeChange, label = { Text("Start Time") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Schedule, 
                        "Start Time", 
                        modifier = Modifier.clickable { onShowStartTimePicker() }
                    ) 
                }, 
                modifier = Modifier
                    .weight(1f)
                    .clickable { onShowStartTimePicker() },
                shape = RoundedCornerShape(12.dp), colors = textFieldColors,
                placeholder = { Text("09:00") },
                readOnly = true,
                enabled = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = endTime, onValueChange = onEndTimeChange, label = { Text("End Time") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Schedule, 
                        "End Time", 
                        modifier = Modifier.clickable { onShowEndTimePicker() }
                    ) 
                }, 
                modifier = Modifier
                    .weight(1f)
                    .clickable { onShowEndTimePicker() },
                shape = RoundedCornerShape(12.dp), colors = textFieldColors,
                placeholder = { Text("18:00") },
                readOnly = true,
                enabled = false
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = overtimeRate, onValueChange = onOvertimeRateChange, label = { Text("Overtime Rate (per hour)") },
            leadingIcon = { Icon(Icons.Default.AttachMoney, "Rate") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
        ) {
            Icon(Icons.Default.Save, "Save", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Overtime", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OvertimeLogsList(monthlyLogs: List<WorkLog>, yearlyLogs: List<WorkLog>, viewModel: OvertimeViewModel, onDeleteClick: (WorkLog) -> Unit) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFF6B6B),
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { viewModel.setSelectedTab(0) }, text = { Text("Monthly") })
            Tab(selected = selectedTab == 1, onClick = { viewModel.setSelectedTab(1) }, text = { Text("Yearly") })
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val logs = if (selectedTab == 0) monthlyLogs else yearlyLogs
            items(logs) { log ->
                OvertimeLogItem(log, viewModel, onDelete = { onDeleteClick(log) })
            }
        }
    }
}

@Composable
private fun OvertimeLogItem(log: WorkLog, viewModel: OvertimeViewModel, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(log.date), fontWeight = FontWeight.Bold)
                Text("${log.startTime} - ${log.endTime}", style = MaterialTheme.typography.bodyMedium)
                val duration = viewModel.calculateDuration(log.startTime, log.endTime)
                val earnings = duration * (log.overtimeRate ?: 0.0)
                Text("Hours: ${String.format("%.2f", duration)}h, Earned: ${CurrencyManager.format(earnings)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete Log", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).fillMaxHeight().background(color = MaterialTheme.colorScheme.outlineVariant))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Select Time",
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
