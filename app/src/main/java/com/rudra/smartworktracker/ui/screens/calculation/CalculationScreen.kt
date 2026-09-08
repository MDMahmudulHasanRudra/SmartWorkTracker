package com.rudra.smartworktracker.ui.screens.calculation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.rudra.smartworktracker.ui.theme.SmartWorkTrackerTheme
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: CalculationViewModel = viewModel(factory = CalculationViewModelFactory(context))
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    var dailyMealRate by remember { mutableStateOf("") }
    var dailyTravelCost by remember { mutableStateOf("") }
    var otherExpenses by remember { mutableStateOf("") }
    var otherExpenseDescription by remember { mutableStateOf("") }

    LaunchedEffect(uiState.calculation, uiState.travelExpense) {
        uiState.calculation?.let {
            if (dailyMealRate.toDoubleOrNull() != it.dailyMealRate) {
                dailyMealRate = it.dailyMealRate.toString()
            }
        }
        uiState.travelExpense?.let {
            if (dailyTravelCost.toDoubleOrNull() != it.dailyTravelCost) {
                dailyTravelCost = it.dailyTravelCost.toString()
            }
            if (otherExpenses.toDoubleOrNull() != it.otherExpenses) {
                otherExpenses = it.otherExpenses.toString()
            }
            if (otherExpenseDescription != it.otherExpenseDescription) {
                otherExpenseDescription = it.otherExpenseDescription
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    SmartWorkTrackerTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Expense Calculator",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.exportToExcel(context) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Download, "Export")
                }
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) { paddingValues ->
                if (uiState.isLoading && uiState.calculation == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MonthNavigator(
                            month = monthYearFormat.format(uiState.selectedDate),
                            onPrevious = { viewModel.goToPreviousMonth() },
                            onNext = { viewModel.goToNextMonth() },
                            isLoading = uiState.isLoading
                        )
                    }

                    item {
                        SummaryHeaderCard(
                            officeDays = uiState.officeDays,
                            homeOfficeDays = uiState.homeOfficeDays,
                            totalCost = uiState.totalExpensePerMonth,
                            isLoading = uiState.isLoading
                        )
                    }

                    item {
                        if (uiState.monthlyBreakdown.isNotEmpty()) {
                            MonthlyBreakdownChart(
                                data = uiState.monthlyBreakdown,
                                year = viewModel.getCurrentYear()
                            )
                        }
                    }

                    item {
                        ExpenseInputSection(
                            dailyMealRate = dailyMealRate,
                            dailyTravelCost = dailyTravelCost,
                            otherExpenses = otherExpenses,
                            otherExpenseDescription = otherExpenseDescription,
                            onDailyMealRateChange = { dailyMealRate = it },
                            onDailyTravelCostChange = { dailyTravelCost = it },
                            onOtherExpensesChange = { otherExpenses = it },
                            onOtherExpenseDescriptionChange = { otherExpenseDescription = it },
                            onSaveMealRate = { rate ->
                                rate.toDoubleOrNull()?.let { viewModel.saveDailyMealRate(it) }
                            },
                            onSaveTravelExpense = { travel, other, desc ->
                                viewModel.saveTravelExpense(
                                    travel.toDoubleOrNull() ?: 0.0,
                                    other.toDoubleOrNull() ?: 0.0,
                                    desc
                                )
                            },
                            focusManager = focusManager
                        )
                    }

                    item {
                        val totalDays = uiState.officeDays + uiState.homeOfficeDays
                        if (totalDays > 0) {
                            WorkingDaysPieChart(
                                data = uiState.pieChartData,
                                isLoading = uiState.isLoading
                            )
                        } else {
                            NoDataPlaceholder(
                                title = "No Work Data",
                                message = "Log your work days to see calculations",
                                icon = Icons.Default.WorkOutline
                            )
                        }
                    }

                    item {
                        ExpandableInfoSection(
                            title = "Meal Costs",
                            icon = Icons.Default.Restaurant,
                            color = MaterialTheme.colorScheme.primary,
                            items = listOf(
                                "Weekly" to CurrencyManager.format(uiState.mealCostPerWeek),
                                "Monthly" to CurrencyManager.format(uiState.mealCostPerMonth),
                                "Yearly" to CurrencyManager.format(uiState.mealCostPerYear)
                            )
                        )
                    }

                    item {
                        ExpandableInfoSection(
                            title = "Travel & Other Expenses",
                            icon = Icons.Default.DirectionsCar,
                            color = Color(0xFF388E3C),
                            items = listOf(
                                "Weekly Travel" to CurrencyManager.format(uiState.travelCostPerWeek),
                                "Monthly Travel" to CurrencyManager.format(uiState.travelCostPerMonth),
                                "Yearly Travel" to CurrencyManager.format(uiState.travelCostPerYear),
                                "Monthly Other" to CurrencyManager.format(uiState.otherExpensePerMonth),
                                "Yearly Other" to CurrencyManager.format(uiState.otherExpensePerYear)
                            )
                        )
                    }

                    item {
                        ExpandableInfoSection(
                            title = "Total Summary",
                            icon = Icons.Default.Calculate,
                            color = MaterialTheme.colorScheme.error,
                            items = listOf(
                                "Total Monthly" to CurrencyManager.format(uiState.totalExpensePerMonth),
                                "Total Yearly" to CurrencyManager.format(uiState.totalExpensePerYear)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBreakdownChart(data: List<Pair<String, Double>>, year: Int) {
    val maxValue = data.maxOfOrNull { it.second } ?: 0.0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Breakdown $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (month, value) ->
                    val heightPercent = if (maxValue > 0) value / maxValue else 0.0
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((heightPercent * 120).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.0f".format(value),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableInfoSection(
    title: String,
    icon: ImageVector,
    color: Color,
    items: List<Pair<String, String>>
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items.forEach { (label, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseInputSection(
    dailyMealRate: String,
    dailyTravelCost: String,
    otherExpenses: String,
    otherExpenseDescription: String,
    onDailyMealRateChange: (String) -> Unit,
    onDailyTravelCostChange: (String) -> Unit,
    onOtherExpensesChange: (String) -> Unit,
    onOtherExpenseDescriptionChange: (String) -> Unit,
    onSaveMealRate: (String) -> Unit,
    onSaveTravelExpense: (String, String, String) -> Unit,
    focusManager: FocusManager
) {
    var showOtherExpenseDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Expense Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Meal Rate",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    OutlinedTextField(
                        value = dailyMealRate,
                        onValueChange = onDailyMealRateChange,
                        label = { Text("Amount in Taka") },
                        leadingIcon = {
                            Text(
                                text = CurrencyManager.symbol(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onSaveMealRate(dailyMealRate)
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFF388E3C)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Travel & Other Expenses",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = { showOtherExpenseDetails = !showOtherExpenseDetails },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showOtherExpenseDetails) Icons.Default.Info else Icons.Default.Warning,
                                contentDescription = "More info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = dailyTravelCost,
                        onValueChange = onDailyTravelCostChange,
                        label = { Text("Daily Travel Cost") },
                        leadingIcon = { 
                            Text(
                                text = CurrencyManager.symbol(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    AnimatedVisibility(visible = showOtherExpenseDetails) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = otherExpenses,
                                onValueChange = onOtherExpensesChange,
                                label = { Text("Monthly Other Expenses") },
                                leadingIcon = { 
                                    Text(
                                        text = CurrencyManager.symbol(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            OutlinedTextField(
                                value = otherExpenseDescription,
                                onValueChange = onOtherExpenseDescriptionChange,
                                label = { Text("Description (optional)") },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        onSaveTravelExpense(dailyTravelCost, otherExpenses, otherExpenseDescription)
                                        focusManager.clearFocus()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 2
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            onSaveTravelExpense(dailyTravelCost, otherExpenses, otherExpenseDescription)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF388E3C)
                        )
                    ) {
                        Text("Save Travel Settings")
                    }
                }
            }
        }
    }
}

@Composable
fun MonthNavigator(month: String, onPrevious: () -> Unit, onNext: () -> Unit, isLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = !isLoading) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month")
        }
        AnimatedContent(targetState = month, label = "Month Text") { targetMonth ->
            Text(
                text = targetMonth,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onNext, enabled = !isLoading) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next Month")
        }
    }
}

@Composable
fun SummaryHeaderCard(officeDays: Int, homeOfficeDays: Int, totalCost: Double, isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "This Month's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem("Office", "$officeDays days")
                SummaryItem("Home", "$homeOfficeDays days")
                SummaryItem("Total", "${officeDays + homeOfficeDays} days")
                SummaryItem("Total Cost", CurrencyManager.format(totalCost), isCost = true)
            }
        }
    }
}

@Composable
private fun RowScope.SummaryItem(label: String, value: String, isCost: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = if (isCost) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WorkingDaysPieChart(data: Map<String, Float>, isLoading: Boolean) {
    val pieChartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    )

    val officeValue = data["Office"] ?: 0f
    val homeValue = data["Home Office"] ?: 0f
    val totalValue = officeValue + homeValue
    val officePercentage =
        if (totalValue > 0f) (officeValue / totalValue) * 100f else 0f

    val pieChartData = PieChartData(
        slices = data.entries.mapIndexed { index, entry ->
            PieChartData.Slice(
                label = entry.key,
                value = entry.value,
                color = pieChartColors[index % pieChartColors.size]
            )
        },
        plotType = PlotType.Donut
    )

    val pieChartConfig = PieChartConfig(
        isAnimationEnable = true,
        showSliceLabels = false,
        strokeWidth = 28f,
        chartPadding = 20
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                modifier = Modifier.size(220.dp),
                pieChartData = pieChartData,
                pieChartConfig = pieChartConfig
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${officePercentage.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Office",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isLoading) {
                Box(modifier = Modifier.matchParentSize().background(Color.White.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun NoDataPlaceholder(title: String, message: String, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
