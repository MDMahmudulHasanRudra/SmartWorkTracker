package com.rudra.smartworktracker.ui.screens.analytics

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.model.Expense
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.*
import com.rudra.smartworktracker.utils.CurrencyManager

private val IncomeGreen = Color(0xFF4CAF50)
private val ExpenseRed = Color(0xFFF44336)

@Composable
fun EnhancedFinancialChart(
    incomes: List<Income>,
    expenses: List<Expense>,
    savings: Double,
    modifier: Modifier = Modifier
) {
    val totalIncome = incomes.sumOf { it.amount }
    val totalExpense = expenses.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0

    var animatedIncome by remember { mutableStateOf(0.0) }
    var animatedExpense by remember { mutableStateOf(0.0) }

    LaunchedEffect(totalIncome, totalExpense) {
        animatedIncome = 0.0
        animatedExpense = 0.0
        delay(100)
        animatedIncome = totalIncome
        animatedExpense = totalExpense
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EnhancedFinanceItem(
                    label = "Income",
                    value = animatedIncome,
                    color = Color(0xFF4CAF50),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    prefix = CurrencyManager.symbol()
                )
                EnhancedFinanceItem(
                    label = "Expenses",
                    value = animatedExpense,
                    color = Color(0xFFF44336),
                    icon = Icons.Default.BarChart,
                    prefix = CurrencyManager.symbol()
                )
                EnhancedFinanceItem(
                    label = "Savings",
                    value = savings,
                    color = Color(0xFF2196F3),
                    icon = Icons.Default.Savings,
                    prefix = CurrencyManager.symbol()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Savings Rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${String.format(Locale.getDefault(), "%.1f", savingsRate)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (savingsRate > 20) Color(0xFF4CAF50) else Color(0xFFFFC107)
                )
            }
            LinearProgressIndicator(
                progress = { (savingsRate / 100f).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (expenses.isNotEmpty()) {
                Text(
                    "Expense Distribution",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ExpenseDistributionChart(expenses)
            }
        }
    }
}

@Composable
fun ExpenseDistributionChart(expenses: List<Expense>) {
    val groupedExpenses = expenses.groupBy { it.category }
        .mapValues { it.value.sumOf { expense -> expense.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    val total = groupedExpenses.sumOf { it.second }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedExpenses.forEach { (category, amount) ->
            val percentage = (amount / total * 100).toFloat()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(80.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage / 100f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    "${String.format(Locale.getDefault(), "%.1f", percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun EnhancedFinanceItem(
    label: String,
    value: Double,
    color: Color,
    icon: ImageVector,
    prefix: String = ""
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "animatedValue"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(24.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$prefix${String.format(Locale.getDefault(), "%.0f", animatedValue)}",
            fontWeight = FontWeight.Bold,
            color = color,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun MonthlyIncomeExpenseChart(
    monthlyData: List<MonthlyFinancialData>,
    modifier: Modifier = Modifier,
    onMonthClick: ((MonthlyFinancialData) -> Unit)? = null
) {
    if (monthlyData.isEmpty()) {
        EmptyStateMessage(modifier = modifier)
        return
    }

    val maxValue = remember(monthlyData) {
        monthlyData.maxOfOrNull { maxOf(it.income, it.expense) }
            ?.coerceAtLeast(1.0)
            ?: 1.0
    }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
    }

    val barHeightAnimation = remember(maxValue) {
        Animatable(0f)
    }

    LaunchedEffect(maxValue) {
        barHeightAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            ChartHeader(
                totalIncome = monthlyData.sumOf { it.income },
                totalExpense = monthlyData.sumOf { it.expense },
                currencyFormat = currencyFormat
            )

            Spacer(modifier = Modifier.height(20.dp))

            ChartBars(
                monthlyData = monthlyData,
                maxValue = maxValue,
                animationProgress = barHeightAnimation.value,
                onMonthClick = onMonthClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ChartValues(
                monthlyData = monthlyData,
                currencyFormat = currencyFormat
            )

            if (monthlyData.size > 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                ChartSummary(
                    monthlyData = monthlyData,
                    currencyFormat = currencyFormat
                )
            }
        }
    }
}

@Composable
private fun ChartHeader(
    totalIncome: Double,
    totalExpense: Double,
    currencyFormat: NumberFormat
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LegendItem(color = IncomeGreen, label = "Income")
            LegendItem(color = ExpenseRed, label = "Expense")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(totalIncome),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Total Expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(totalExpense),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChartBars(
    monthlyData: List<MonthlyFinancialData>,
    maxValue: Double,
    animationProgress: Float,
    onMonthClick: ((MonthlyFinancialData) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        monthlyData.forEach { monthData ->
            val incomeHeight = ((monthData.income / maxValue) * 120).dp.coerceAtLeast(4.dp)
            val expenseHeight = ((monthData.expense / maxValue) * 120).dp.coerceAtLeast(4.dp)

            val clickModifier = if (onMonthClick != null) {
                Modifier.clickable { onMonthClick(monthData) }
            } else Modifier

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .then(clickModifier)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .graphicsLayer {
                                scaleY = animationProgress
                                translationY = size.height * (1 - animationProgress)
                            }
                            .height(incomeHeight * animationProgress)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(IncomeGreen, IncomeGreen.copy(alpha = 0.7f))
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .graphicsLayer {
                                scaleY = animationProgress
                                translationY = size.height * (1 - animationProgress)
                            }
                            .height(expenseHeight * animationProgress)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(ExpenseRed, ExpenseRed.copy(alpha = 0.7f))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = monthData.month,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartValues(
    monthlyData: List<MonthlyFinancialData>,
    currencyFormat: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        monthlyData.forEach { monthData ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currencyFormat.format(monthData.income),
                    style = MaterialTheme.typography.labelSmall,
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currencyFormat.format(monthData.expense),
                    style = MaterialTheme.typography.labelSmall,
                    color = ExpenseRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChartSummary(
    monthlyData: List<MonthlyFinancialData>,
    currencyFormat: NumberFormat
) {
    val averageIncome = monthlyData.map { it.income }.average()
    val averageExpense = monthlyData.map { it.expense }.average()
    val netChange = monthlyData.sumOf { it.income - it.expense }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Monthly Average",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${currencyFormat.format(averageIncome)} / ${currencyFormat.format(averageExpense)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Net Change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(netChange),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (netChange >= 0) IncomeGreen else ExpenseRed
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
