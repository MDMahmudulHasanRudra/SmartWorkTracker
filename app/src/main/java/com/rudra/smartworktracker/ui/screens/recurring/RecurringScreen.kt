package com.rudra.smartworktracker.ui.screens.recurring

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.PreferredTime
import com.rudra.smartworktracker.data.entity.RecurringPriority
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import com.rudra.smartworktracker.data.entity.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: RecurringViewModel = viewModel(factory = RecurringViewModelFactory(context))
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddRuleSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RecurringRule?>(null) }
    var showManualExecutionDialog by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val prefs = remember { context.getSharedPreferences("recurring_prefs", Context.MODE_PRIVATE) }
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_shown", false)) }

    val tabs = listOf("Rules", "Transactions", "Calendar", "Insights", "History")

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showTemplateSheet = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = "Templates", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { showManualExecutionDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Execute", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { showAddRuleSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RecurringHeader(
                activeRulesCount = uiState.activeRulesCount,
                upcomingTransactionsCount = uiState.upcomingTransactions.size,
                totalIncomeThisMonth = uiState.totalIncomeThisMonth,
                totalExpensesThisMonth = uiState.totalExpensesThisMonth
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> RulesTab(
                    rules = if (searchQuery.isBlank() && selectedFilter == RuleFilter.ALL) uiState.rules else uiState.filteredRules,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    isRefreshing = uiState.isRefreshing,
                    isMultiSelectMode = uiState.isMultiSelectMode,
                    selectedRuleIds = uiState.selectedRuleIds,
                    accounts = uiState.accounts,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onFilterChange = { viewModel.updateFilter(it) },
                    onToggleRule = { viewModel.toggleRuleActive(it) },
                    onEditRule = { editingRule = it },
                    onDeleteRule = { viewModel.deleteRule(it) },
                    onExecuteNow = { viewModel.executeRuleNow(it) },
                    onRefresh = { viewModel.refreshData() },
                    onToggleMultiSelect = { viewModel.toggleMultiSelect() },
                    onToggleRuleSelection = { viewModel.toggleRuleSelection(it) },
                    onSelectAll = { viewModel.selectAllRules() },
                    onDeselectAll = { viewModel.deselectAllRules() },
                    onDeleteSelected = { viewModel.deleteSelectedRules() },
                    onToggleSelected = { viewModel.toggleSelectedRulesActive() }
                )
                1 -> TransactionsTab(
                    transactions = uiState.allTransactions,
                    isRefreshing = uiState.isRefreshing,
                    pendingConfirmations = uiState.pendingConfirmations,
                    onSkipTransaction = { viewModel.skipTransaction(it) },
                    onConfirmTransaction = { viewModel.confirmTransaction(it) },
                    onConfirmAllPending = { viewModel.confirmAllPending() },
                    onSnoozeTransaction = { viewModel.snoozeTransaction(it) },
                    onSnoozeAllFailed = { viewModel.snoozeAllFailed() },
                    onRefresh = { viewModel.refreshData() }
                )
                2 -> CalendarTab(
                    rules = uiState.rules,
                    transactions = uiState.upcomingTransactions
                )
                3 -> InsightsTab(
                    yearlyProjection = uiState.yearlyProjection,
                    patternSuggestions = uiState.patternSuggestions,
                    spendingAlerts = uiState.spendingAlerts,
                    categoryBreakdown = uiState.categoryBreakdown,
                    onAddFromPattern = { suggestion ->
                        val rule = RecurringRule(
                            name = suggestion.name,
                            transactionType = TransactionType.EXPENSE,
                            amount = suggestion.amount,
                            category = suggestion.category,
                            sourceAccount = AccountType.BALANCE,
                            frequency = suggestion.frequency,
                            startDate = System.currentTimeMillis(),
                            nextExecutionDate = System.currentTimeMillis(),
                            preferredTime = PreferredTime.MORNING,
                            priority = RecurringPriority.MEDIUM,
                            autoExecute = true,
                            isActive = true
                        )
                        viewModel.addRule(rule)
                    }
                )
                4 -> HistoryTab(viewModel = viewModel)
            }
        }

        if (showManualExecutionDialog) {
            ManualExecutionDialog(
                onDismiss = { showManualExecutionDialog = false },
                onExecute = { rulesToExecute ->
                    viewModel.manualExecuteRules(rulesToExecute)
                    showManualExecutionDialog = false
                },
                rules = uiState.rules.filter { it.isActive }
            )
        }

        if (uiState.lastExecutionResult != null) {
            ExecutionResultDialog(
                result = uiState.lastExecutionResult!!,
                onDismiss = { viewModel.clearExecutionResult() }
            )
        }

        if (showAddRuleSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddRuleSheet = false },
                sheetState = sheetState
            ) {
                AddRuleContent(
                    accounts = uiState.accounts,
                    onSave = { rule ->
                        viewModel.addRule(rule)
                        showAddRuleSheet = false
                    },
                    onCancel = { showAddRuleSheet = false }
                )
            }
        }

        if (editingRule != null) {
            ModalBottomSheet(
                onDismissRequest = { editingRule = null },
                sheetState = sheetState
            ) {
                AddRuleContent(
                    existingRule = editingRule,
                    accounts = uiState.accounts,
                    onSave = { rule ->
                        viewModel.updateRule(rule)
                        editingRule = null
                    },
                    onCancel = { editingRule = null }
                )
            }
        }

        if (showTemplateSheet) {
            TemplateSelectionSheet(
                templates = viewModel.getRuleTemplates(),
                onSelectTemplate = { template ->
                    viewModel.addRuleFromTemplate(template)
                    showTemplateSheet = false
                    Toast.makeText(context, "${template.name} added!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showTemplateSheet = false }
            )
        }
    }

    if (showOnboarding) {
        RecurringOnboardingDialog(
            onDismiss = {
                showOnboarding = false
                prefs.edit().putBoolean("onboarding_shown", true).apply()
            }
        )
    }
}
