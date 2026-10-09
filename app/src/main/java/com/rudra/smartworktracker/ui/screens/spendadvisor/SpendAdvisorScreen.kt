package com.rudra.smartworktracker.ui.screens.spendadvisor

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.AdviceSeverity
import com.rudra.smartworktracker.model.ExpenseAdvice
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.model.SavingsTip
import com.rudra.smartworktracker.model.SpendAdvisor
import com.rudra.smartworktracker.model.SpendingTrend
import java.text.NumberFormat
import java.util.Locale
import com.rudra.smartworktracker.utils.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendAdvisorScreen() {
    val context = LocalContext.current
    val viewModel: SpendAdvisorViewModel = viewModel(
        factory = SpendAdvisorViewModelFactory(context.applicationContext as Application)
    )

    val spendAdvisor by viewModel.spendAdvisor.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recentAnalyses by viewModel.recentAnalyses.collectAsState()
    val savingsTips by viewModel.savingsTips.collectAsState()

    var plannedAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var showBudgetGoalDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Spend Advisor", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Smart spending insights", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showBudgetGoalDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Set Budget Goal")
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (analysis != null) {
                FloatingActionButton(
                    onClick = { viewModel.clearAnalysis() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Analyze") }, icon = { Icon(Icons.Default.Analytics, contentDescription = null) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Insights") }, icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("History") }, icon = { Icon(Icons.Default.History, contentDescription = null) })
                }

                when (selectedTab) {
                    0 -> AnalysisTab(spendAdvisor, plannedAmount, { plannedAmount = it }, selectedCategory, { selectedCategory = it }, {
                        plannedAmount.toDoubleOrNull()?.let { if (it > 0) viewModel.analyzeExpense(it, selectedCategory) }
                    }, analysis)
                    1 -> InsightsTab(spendAdvisor, savingsTips)
                    2 -> HistoryTab(recentAnalyses)
                }
            }
        }

        if (showBudgetGoalDialog) {
            BudgetGoalDialog(
                currentGoal = spendAdvisor.monthlyGoal,
                onDismiss = { showBudgetGoalDialog = false },
                onSave = { viewModel.updateMonthlyGoal(it); showBudgetGoalDialog = false }
            )
        }
    }
}

@Composable
private fun AnalysisTab(
    spendAdvisor: SpendAdvisor,
    plannedAmount: String,
    onAmountChange: (String) -> Unit,
    selectedCategory: ExpenseCategory,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onAnalyze: () -> Unit,
    analysis: com.rudra.smartworktracker.model.ExpenseAnalysis?
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { FinancialSummaryCard(spendAdvisor) }
        item { PlannedExpenseCard(plannedAmount, onAmountChange, selectedCategory, onCategoryChange, onAnalyze) }
        analysis?.let {
            item { AdviceCard(it.advice) }
            item { DetailsCard(it.remainingAfterExpense, it.safeLimit, it.warningLimit, it.confidenceScore, it.suggestion, it.category) }
        }
    }
}

@Composable
private fun InsightsTab(spendAdvisor: SpendAdvisor, savingsTips: List<SavingsTip>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SpendingTrendCard(spendAdvisor.spendingTrend) }
        item { WeeklySpendCard(spendAdvisor.trendData, spendAdvisor.dailyAverageExpense) }
        if (spendAdvisor.categoryBreakdown.isNotEmpty()) {
            item { CategoryBreakdownCard(spendAdvisor.categoryBreakdown) }
        }
        item { SavingsTipsCard(savingsTips) }
        item { SmartSuggestionsCard(generateSmartSuggestions(spendAdvisor)) }
    }
}

@Composable
private fun HistoryTab(recentAnalyses: List<com.rudra.smartworktracker.model.ExpenseAnalysis>) {
    if (recentAnalyses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No analysis history yet", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentAnalyses) { item -> HistoryItemCard(item) }
        }
    }
}

@Composable
private fun FinancialSummaryCard(spendAdvisor: SpendAdvisor) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Financial Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Current Balance", style = MaterialTheme.typography.bodySmall)
                        Text(formatCurrency(spendAdvisor.currentBalance), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (spendAdvisor.currentBalance >= 0) Color(0xFF43A047) else Color(0xFFE53935))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatItemCard("Income this month", formatCurrency(spendAdvisor.totalIncome), Color(0xFF43A047), Modifier.weight(1f))
                StatItemCard("Spent this month", formatCurrency(spendAdvisor.totalExpenses), Color(0xFFE53935), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            val progress = if (spendAdvisor.monthlyGoal > 0) (spendAdvisor.totalExpenses / spendAdvisor.monthlyGoal).toFloat().coerceIn(0f, 1f) else 0f
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly budget", style = MaterialTheme.typography.bodySmall)
                Text("${formatCurrency(spendAdvisor.totalExpenses)} / ${formatCurrency(spendAdvisor.monthlyGoal)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = when { progress < 0.5f -> Color(0xFF43A047); progress < 0.8f -> Color(0xFFFF9800); else -> Color(0xFFE53935) })
        }
    }
}

@Composable
private fun StatItemCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun PlannedExpenseCard(plannedAmount: String, onAmountChange: (String) -> Unit, selectedCategory: ExpenseCategory, onCategoryChange: (ExpenseCategory) -> Unit, onCheck: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Plan a Future Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = plannedAmount, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) onAmountChange(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Amount (${CurrencyManager.symbol()})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Expense Category", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategory.entries.toTypedArray()) { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { onCategoryChange(category) }, label = { Text(category.displayName) }, leadingIcon = if (selectedCategory == category) {{ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCheck, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = plannedAmount.isNotEmpty() && plannedAmount.toDoubleOrNull() != null, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze Expense", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AdviceCard(advice: ExpenseAdvice) {
    val (backgroundColor, iconColor, icon) = when (advice.severity) {
        AdviceSeverity.GOOD -> Triple(Color(0xFF43A047).copy(alpha = 0.1f), Color(0xFF43A047), Icons.Default.CheckCircle)
        AdviceSeverity.WARNING -> Triple(Color(0xFFFF9800).copy(alpha = 0.1f), Color(0xFFFF9800), Icons.Default.Error)
        AdviceSeverity.DANGER -> Triple(Color(0xFFE53935).copy(alpha = 0.1f), Color(0xFFE53935), Icons.Default.Error)
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = backgroundColor), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(advice.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = iconColor)
                Text(advice.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DetailsCard(remainingAfterExpense: Double, safeLimit: Double, warningLimit: Double, confidenceScore: Int, suggestion: String?, category: ExpenseCategory) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Analysis Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow("Remaining after expense", formatCurrency(remainingAfterExpense))
            DetailRow("Comfortable amount", formatCurrency(safeLimit))
            DetailRow("Budget left this month", formatCurrency(warningLimit))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Confidence Score", style = MaterialTheme.typography.bodyMedium)
                Text("$confidenceScore%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = when { confidenceScore >= 70 -> Color(0xFF43A047); confidenceScore >= 40 -> Color(0xFFFF9800); else -> Color(0xFFE53935) })
            }
            if (suggestion != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5).copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1E88E5))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingTrendCard(trend: SpendingTrend) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Spending Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            val trendColor = when (trend) { SpendingTrend.INCREASING -> Color(0xFFE53935); SpendingTrend.DECREASING -> Color(0xFF43A047); SpendingTrend.STABLE -> Color(0xFF1E88E5) }
            Text(trend.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = trendColor)
        }
    }
}

@Composable
private fun SavingsTipsCard(tips: List<SavingsTip>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Savings Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            tips.forEach { tip ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(tip.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Text(tip.description, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSuggestionsCard(suggestions: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Smart Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            suggestions.forEach { suggestion ->
                Text("• $suggestion", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun HistoryItemCard(analysis: com.rudra.smartworktracker.model.ExpenseAnalysis) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(analysis.advice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Confidence: ${analysis.confidenceScore}%", style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(analysis.remainingAfterExpense), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BudgetGoalDialog(currentGoal: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var goalAmount by remember { mutableStateOf(currentGoal.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Set Monthly Budget Goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = goalAmount, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) goalAmount = it }, label = { Text("Monthly Goal (${CurrencyManager.symbol()})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)) { Text("Cancel") }
                    Button(onClick = { goalAmount.toDoubleOrNull()?.let { if (it > 0) onSave(it) } }, modifier = Modifier.weight(1f), enabled = goalAmount.toDoubleOrNull() != null && goalAmount.toDouble() > 0) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    format.maximumFractionDigits = 0
    return CurrencyManager.formatWhole(amount)
}

private fun generateSmartSuggestions(spendAdvisor: SpendAdvisor): List<String> {
    val suggestions = mutableListOf<String>()
    if (spendAdvisor.totalExpenses > spendAdvisor.monthlyGoal * 0.8) suggestions.add("You're close to your monthly budget limit.")
    if (spendAdvisor.spendingTrend == SpendingTrend.INCREASING) suggestions.add("Your spending is trending upward.")
    if (spendAdvisor.currentBalance < spendAdvisor.monthlyGoal * 0.3) suggestions.add("Low balance alert!")
    if (suggestions.isEmpty()) suggestions.add("Great job! You're on track with your financial goals.")
    return suggestions
}

/** Last 7 days of spending as simple bars (oldest on the left). */
@Composable
private fun WeeklySpendCard(dailyTotals: List<Double>, dailyAverage: Double) {
    val max = (dailyTotals.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val dayFormat = remember { java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()) }
    val labels = remember(dailyTotals.size) {
        val count = dailyTotals.size
        (count - 1 downTo 0).map { daysAgo ->
            dayFormat.format(java.util.Date(System.currentTimeMillis() - daysAgo * 24L * 60 * 60 * 1000)).take(2)
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyTotals.forEachIndexed { index, total ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((96 * (total / max)).coerceAtLeast(2.0).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (index == dailyTotals.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(labels.getOrElse(index) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Average this month: ${formatCurrency(dailyAverage)} per day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: Map<ExpenseCategory, Double>) {
    val total = breakdown.values.sum().coerceAtLeast(1.0)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Where it went this month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            breakdown.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { (amount / total).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = category.color,
                        trackColor = category.color.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}
