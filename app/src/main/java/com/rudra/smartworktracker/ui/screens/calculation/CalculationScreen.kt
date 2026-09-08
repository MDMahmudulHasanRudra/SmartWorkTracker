package com.rudra.smartworktracker.ui.screens.calculation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.util.*

private val Green500 = Color(0xFF4CAF50)
private val Green700 = Color(0xFF388E3C)
private val Blue500 = Color(0xFF2196F3)
private val Orange500 = Color(0xFFFF9800)
private val Purple500 = Color(0xFF9C27B0)
private val Red500 = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: CalculationViewModel = viewModel(factory = CalculationViewModelFactory(context))
    val snackbarHostState = remember { SnackbarHostState() }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Expense Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.exportToExcel(context) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.calculation == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                contentPadding = PaddingValues(bottom = 100.dp),
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
                    HeroSummaryCard(
                        officeDays = uiState.officeDays,
                        homeOfficeDays = uiState.homeOfficeDays,
                        totalCost = uiState.totalExpensePerMonth,
                        totalYearly = uiState.totalExpensePerYear
                    )
                }

                item {
                    QuickStatsRow(
                        mealWeekly = uiState.mealCostPerWeek,
                        travelWeekly = uiState.travelCostPerWeek,
                        mealMonthly = uiState.mealCostPerMonth,
                        travelMonthly = uiState.travelCostPerMonth
                    )
                }

                item {
                    if (uiState.monthlyBreakdown.isNotEmpty()) {
                        MonthlyBarChart(
                            data = uiState.monthlyBreakdown,
                            year = viewModel.getCurrentYear()
                        )
                    }
                }

                item {
                    val totalDays = uiState.officeDays + uiState.homeOfficeDays
                    if (totalDays > 0) {
                        WorkDistributionCard(
                            officeDays = uiState.officeDays,
                            homeOfficeDays = uiState.homeOfficeDays
                        )
                    } else {
                        EmptyStateCard(
                            title = "No Work Data",
                            message = "Log your work days to see calculations",
                            icon = Icons.Default.WorkOutline
                        )
                    }
                }

                item {
                    ExpenseInputCard(
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
                    TotalSummaryCard(
                        monthlyTotal = uiState.totalExpensePerMonth,
                        yearlyTotal = uiState.totalExpensePerYear
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedCounter(targetValue: Double, prefix: String = "", duration: Int = 800) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
        label = "counter"
    )
    Text(
        text = "$prefix${CurrencyManager.format(animatedValue.toDouble())}",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
fun MonthNavigator(month: String, onPrevious: () -> Unit, onNext: () -> Unit, isLoading: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, enabled = !isLoading) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", modifier = Modifier.size(18.dp))
            }
            AnimatedContent(targetState = month, label = "Month") { targetMonth ->
                Text(
                    text = targetMonth,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onNext, enabled = !isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun HeroSummaryCard(officeDays: Int, homeOfficeDays: Int, totalCost: Double, totalYearly: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.tertiary
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "This Month",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                AnimatedCounter(targetValue = totalCost)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${CurrencyManager.format(totalYearly)} yearly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeroStatItem(label = "Office", value = "$officeDays", icon = Icons.Default.Business)
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                    )
                    HeroStatItem(label = "Home", value = "$homeOfficeDays", icon = Icons.Default.Home)
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                    )
                    HeroStatItem(label = "Total", value = "${officeDays + homeOfficeDays}", icon = Icons.Default.CalendarMonth)
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun QuickStatsRow(mealWeekly: Double, travelWeekly: Double, mealMonthly: Double, travelMonthly: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            title = "Meal",
            weekly = mealWeekly,
            monthly = mealMonthly,
            icon = Icons.Default.Restaurant,
            color = Orange500
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            title = "Travel",
            weekly = travelWeekly,
            monthly = travelMonthly,
            icon = Icons.Default.DirectionsCar,
            color = Blue500
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    title: String,
    weekly: Double,
    monthly: Double,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                }
                Spacer(Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(text = "Weekly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = CurrencyManager.format(weekly), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = "Monthly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = CurrencyManager.format(monthly), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MonthlyBarChart(data: List<Pair<String, Double>>, year: Int) {
    val maxValue = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Monthly Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "$year", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, (month, value) ->
                    val heightFraction = (value / maxValue).toFloat()
                    val animatedHeight by animateFloatAsState(
                        targetValue = heightFraction,
                        animationSpec = tween(
                            durationMillis = 600,
                            delayMillis = index * 50,
                            easing = FastOutSlowInEasing
                        ),
                        label = "barHeight"
                    )
                    val barColor = when {
                        value == 0.0 -> MaterialTheme.colorScheme.outlineVariant
                        index == data.indexOfMaxByValue() -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (value > 0) {
                            Text(
                                text = "%.0f".format(value),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height((animatedHeight * 120).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(barColor)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun List<Pair<String, Double>>.indexOfMaxByValue(): Int {
    return if (isNotEmpty()) indexOf(maxByOrNull { it.second }) else -1
}

@Composable
fun WorkDistributionCard(officeDays: Int, homeOfficeDays: Int) {
    val total = officeDays + homeOfficeDays
    val officeFraction = if (total > 0) officeDays.toFloat() / total else 0f
    val homeFraction = if (total > 0) homeOfficeDays.toFloat() / total else 0f

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val animateProgress by animateFloatAsState(
        targetValue = officeFraction,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Work Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animateProgress)
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = primaryColor, label = "Office", value = "$officeDays days", percentage = "%.0f%%".format(officeFraction * 100))
                LegendItem(color = secondaryColor, label = "Home", value = "$homeOfficeDays days", percentage = "%.0f%%".format(homeFraction * 100))
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String, percentage: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Column {
            Text(text = "$label $percentage", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ExpenseInputCard(
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(text = "Expense Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ExpenseInputField(
                        label = "Daily Meal Rate",
                        value = dailyMealRate,
                        onValueChange = onDailyMealRateChange,
                        icon = Icons.Default.Restaurant,
                        iconColor = Orange500,
                        imeAction = ImeAction.Next,
                        onImeAction = { onSaveMealRate(dailyMealRate) },
                        focusManager = focusManager
                    )

                    Spacer(Modifier.height(12.dp))

                    ExpenseInputField(
                        label = "Daily Travel Cost",
                        value = dailyTravelCost,
                        onValueChange = onDailyTravelCostChange,
                        icon = Icons.Default.DirectionsCar,
                        iconColor = Blue500,
                        imeAction = ImeAction.Next,
                        focusManager = focusManager
                    )

                    Spacer(Modifier.height(12.dp))

                    ExpenseInputField(
                        label = "Monthly Other Expenses",
                        value = otherExpenses,
                        onValueChange = onOtherExpensesChange,
                        icon = Icons.Default.Receipt,
                        iconColor = Purple500,
                        imeAction = ImeAction.Next,
                        focusManager = focusManager
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = otherExpenseDescription,
                        onValueChange = onOtherExpenseDescriptionChange,
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSaveTravelExpense(dailyTravelCost, otherExpenses, otherExpenseDescription)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Settings", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpenseInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    iconColor: Color,
    imeAction: ImeAction,
    onImeAction: (() -> Unit)? = null,
    focusManager: FocusManager
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = iconColor)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = CurrencyManager.symbol(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onImeAction?.invoke()
                focusManager.clearFocus()
            }
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun TotalSummaryCard(monthlyTotal: Double, yearlyTotal: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Red500.copy(alpha = 0.9f),
                            Red500
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Total Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Monthly", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Spacer(Modifier.height(4.dp))
                        AnimatedCounter(targetValue = monthlyTotal, duration = 1000)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Yearly", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Spacer(Modifier.height(4.dp))
                        AnimatedCounter(targetValue = yearlyTotal, duration = 1200)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(title: String, message: String, icon: ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
            )
        }
    }
}
