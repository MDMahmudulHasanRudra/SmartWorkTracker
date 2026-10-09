package com.rudra.smartworktracker.ui.screens.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.IncomeCategories
import com.rudra.smartworktracker.ui.components.AccountPickerField
import com.rudra.smartworktracker.ui.components.AmountInputField
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import com.rudra.smartworktracker.ui.components.EntryDateField
import com.rudra.smartworktracker.ui.components.EntryHeroCard
import com.rudra.smartworktracker.ui.components.EntrySectionCard
import com.rudra.smartworktracker.ui.components.RecentEntryRow
import com.rudra.smartworktracker.ui.components.formatEntryDate
import com.rudra.smartworktracker.utils.CurrencyManager

private val IncomeGreen = Color(0xFF10B981)

private fun incomeCategoryIcon(category: String): ImageVector = when (category) {
    "Salary" -> Icons.Default.Work
    "Side Income" -> Icons.AutoMirrored.Filled.TrendingUp
    "Freelance" -> Icons.Default.Laptop
    "Business Income" -> Icons.Default.Storefront
    "Investment" -> Icons.Default.ShowChart
    "Gift" -> Icons.Default.CardGiftcard
    "Refund" -> Icons.Default.Replay
    else -> Icons.Default.AttachMoney
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomeScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: IncomeViewModel = viewModel(factory = IncomeViewModelFactory(application))

    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(IncomeCategories.categories.first()) }
    var timestamp by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountInitialized by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val monthIncome by viewModel.income.collectAsState()
    val recentIncomes by viewModel.recentIncomes.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Default to the cash wallet once; balance updates must not reset the user's choice
    LaunchedEffect(accounts) {
        if (!accountInitialized && accounts.isNotEmpty()) {
            selectedAccountId = (accounts.find { it.name == "Cash" } ?: accounts.first()).id
            accountInitialized = true
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    val parsedAmount = amount.toDoubleOrNull()
    val amountError = if (showErrors && (parsedAmount == null || parsedAmount <= 0)) "Enter an amount greater than zero" else null

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EntryHeroCard(
                    title = "Log Income",
                    subtitle = "Record money coming in",
                    icon = Icons.Default.AttachMoney,
                    gradient = listOf(Color(0xFF14B8A6), IncomeGreen),
                    totalLabel = "Earned this month",
                    totalValue = monthIncome
                )
            }

            item {
                EntrySectionCard {
                    AmountInputField(
                        value = amount,
                        onValueChange = { amount = it },
                        errorText = amountError
                    )
                    Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IncomeCategories.categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                leadingIcon = {
                                    Icon(
                                        incomeCategoryIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                EntrySectionCard {
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onAccountSelected = { selectedAccountId = it },
                        label = "Deposit to"
                    )
                    EntryDateField(timestamp = timestamp, onTimestampChange = { timestamp = it })
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("Source (optional)") },
                        placeholder = { Text("Employer, client, …") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        showErrors = true
                        val value = parsedAmount
                        if (value != null && value > 0) {
                            viewModel.saveIncome(
                                amount = value,
                                description = description.trim().ifBlank { selectedCategory },
                                category = selectedCategory,
                                source = source.trim(),
                                timestamp = timestamp,
                                accountId = selectedAccountId
                            )
                            amount = ""
                            description = ""
                            source = ""
                            timestamp = System.currentTimeMillis()
                            showErrors = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Income", fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Text("Recent income", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (recentIncomes.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Payments,
                        title = "No income logged yet",
                        message = "Income you record will show up here."
                    )
                }
            } else {
                items(recentIncomes, key = { it.id }) { income ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        RecentEntryRow(
                            title = income.description.ifBlank { income.category },
                            subtitle = listOf(income.category, income.source.takeIf { it.isNotBlank() }, formatEntryDate(income.timestamp))
                                .filterNotNull()
                                .joinToString(" • "),
                            amountText = "+${CurrencyManager.format(income.amount)}",
                            amountColor = IncomeGreen,
                            icon = incomeCategoryIcon(income.category),
                            iconTint = IncomeGreen,
                            onDelete = { viewModel.deleteIncome(income) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
