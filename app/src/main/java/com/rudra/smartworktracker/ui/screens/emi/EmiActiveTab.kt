package com.rudra.smartworktracker.ui.screens.emi

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmiCard(
    emiWithLoan: EmiWithLoan,
    onPayClick: () -> Unit,
    onSkipClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val emi = emiWithLoan.emi
    val loan = emiWithLoan.loan
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    val cardColor by animateColorAsState(
        targetValue = when (emi.status) {
            com.rudra.smartworktracker.data.entity.EmiStatus.PAID -> MaterialTheme.colorScheme.surfaceVariant
            com.rudra.smartworktracker.data.entity.EmiStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            com.rudra.smartworktracker.data.entity.EmiStatus.DUE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        },
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when (emi.status) {
                                    com.rudra.smartworktracker.data.entity.EmiStatus.PAID -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    com.rudra.smartworktracker.data.entity.EmiStatus.OVERDUE -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = when (emi.status) {
                                com.rudra.smartworktracker.data.entity.EmiStatus.PAID -> Color(0xFF4CAF50)
                                com.rudra.smartworktracker.data.entity.EmiStatus.OVERDUE -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            loan?.personName ?: "Unknown Loan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        loan?.let {
                            Text(
                                it.loanType.name + " - " + it.loanCategory.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                StatusChip(status = emi.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "EMI Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        CurrencyManager.format(emi.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Principal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        CurrencyManager.format(emi.principalAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Interest",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        CurrencyManager.format(emi.interestAmount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (emi.penaltyAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Penalty: ${CurrencyManager.format(emi.penaltyAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Due: ${dateFormat.format(Date(emi.nextDueDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Day: ${emi.dueDateOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            emi.notes?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Notes: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!emi.isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onSkipClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skip")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                emi.lastPaymentDate?.let {
                    Text(
                        "Paid on: ${dateFormat.format(Date(it))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: com.rudra.smartworktracker.data.entity.EmiStatus) {
    val (color, text) = when (status) {
        com.rudra.smartworktracker.data.entity.EmiStatus.PAID -> Pair(Color(0xFF4CAF50), "PAID")
        com.rudra.smartworktracker.data.entity.EmiStatus.OVERDUE -> Pair(MaterialTheme.colorScheme.error, "OVERDUE")
        com.rudra.smartworktracker.data.entity.EmiStatus.DUE -> Pair(MaterialTheme.colorScheme.tertiary, "DUE")
        com.rudra.smartworktracker.data.entity.EmiStatus.SKIPPED -> Pair(MaterialTheme.colorScheme.outline, "SKIPPED")
        com.rudra.smartworktracker.data.entity.EmiStatus.UPCOMING -> Pair(MaterialTheme.colorScheme.primary, "UPCOMING")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
