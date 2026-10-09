package com.rudra.smartworktracker.ui.screens.emi

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.EmiStatus
import com.rudra.smartworktracker.ui.components.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiScreen(
    viewModel: EmiViewModel = viewModel(factory = EmiViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMI Management") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (uiState.statistics.overdueCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text(uiState.statistics.overdueCount.toString()) }
                            }
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Overdue EMIs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddEmiDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add EMI")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StatisticsCard(uiState.statistics)

            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Scrollable: five tabs with counts don't fit a fixed row on phones
            ScrollableTabRow(
                selectedTabIndex = EmiTab.entries.indexOf(uiState.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp
            ) {
                EmiTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.setSelectedTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.title)
                                val count = when (tab) {
                                    EmiTab.ALL -> uiState.emis.size
                                    EmiTab.UPCOMING -> uiState.emis.count { it.emi.status == EmiStatus.UPCOMING }
                                    EmiTab.DUE -> uiState.emis.count { it.emi.status == EmiStatus.DUE }
                                    EmiTab.OVERDUE -> uiState.statistics.overdueCount
                                    EmiTab.PAID -> uiState.emis.count { it.emi.status == EmiStatus.PAID }
                                }
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredEmis.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Payment,
                    title = "No EMIs found",
                    message = "Tap + to add your first EMI to start tracking."
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(uiState.filteredEmis, key = { it.emi.id }) { emiWithLoan ->
                        EmiCard(
                            emiWithLoan = emiWithLoan,
                            onPayClick = { viewModel.openPayEmiDialog(emiWithLoan) },
                            onSkipClick = { viewModel.skipEmi(emiWithLoan.emi) },
                            onDeleteClick = { viewModel.openDeleteConfirmation(emiWithLoan) }
                        )
                    }
                }
            }
        }

        if (uiState.showAddEmiDialog) {
            AddEmiBottomSheet(
                loans = uiState.availableLoans,
                onDismiss = { viewModel.closeAddEmiDialog() },
                onSave = { loanId, amount, principal, interest, dueDay, notes, account ->
                    viewModel.addEmi(loanId, amount, principal, interest, dueDay, notes, account)
                }
            )
        }

        uiState.showPayEmiDialog?.let { emiWithLoan ->
            PayEmiDialog(
                emiWithLoan = emiWithLoan,
                onDismiss = { viewModel.closePayEmiDialog() },
                onConfirm = { viewModel.payEmi(emiWithLoan.emi) }
            )
        }

        uiState.showDeleteConfirmation?.let { emiWithLoan ->
            DeleteConfirmationDialog(
                emiWithLoan = emiWithLoan,
                onDismiss = { viewModel.closeDeleteConfirmation() },
                onConfirm = { viewModel.deleteEmi(emiWithLoan.emi) }
            )
        }
    }
}
