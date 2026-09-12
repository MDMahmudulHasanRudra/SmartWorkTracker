package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.WorkLogUi
import com.rudra.smartworktracker.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.Locale

private val OfficeBlue = Color(0xFF3B82F6)
private val HomeOrange = Color(0xFFF97316)
private val OffPurple = Color(0xFF8B5CF6)
private val OvertimePink = Color(0xFFEC4899)

@Composable
fun RecentActivityTimeline(activities: List<WorkLogUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "Recent Activity")

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No recent activity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                activities.take(7).forEachIndexed { index, activity ->
                    ActivityRow(activity = activity)
                    if (index < activities.lastIndex.coerceAtMost(6)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 19.dp, top = 2.dp, bottom = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: WorkLogUi) {
    val (color, label) = remember(activity.workType) {
        when (activity.workType) {
            WorkType.OFFICE -> OfficeBlue to "Office"
            WorkType.HOME_OFFICE -> HomeOrange to "Home Office"
            WorkType.OFF_DAY -> OffPurple to "Off Day"
            WorkType.EXTRA_WORK -> OvertimePink to "Extra Work"
            WorkType.OVERTIME -> OvertimePink to "Overtime"
        }
    }

    val dateStr = remember(activity.date) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(activity.date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline dot with ring
        Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = activity.duration,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                fontSize = 12.sp
            )
        }
    }
}
