package com.rudra.smartworktracker.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rudra.smartworktracker.model.MoodType
import java.time.LocalDate

@Composable
fun MoodCard(
    currentMood: MoodType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(currentMood?.emoji ?: "\uD83D\uDE42", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Mood", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9800))
            Text(currentMood?.label ?: "Tap to log", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800).copy(alpha = 0.7f))
        }
    }
}

@Composable
fun StressCard(
    currentStress: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stressColor = when {
        currentStress == null -> Color(0xFF9E9E9E)
        currentStress <= 3 -> Color(0xFF4CAF50)
        currentStress <= 6 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = stressColor.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, stressColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u26A1", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Stress", style = MaterialTheme.typography.labelMedium, color = stressColor)
            Text(currentStress?.let { "$it/10" } ?: "Tap to log", style = MaterialTheme.typography.labelSmall, color = stressColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun MoodCardLarge(
    moodTrend: List<Pair<LocalDate, MoodType>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDE0A", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mood", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (moodTrend.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    moodTrend.takeLast(7).forEach { (date, mood) ->
                        Text(mood.emoji, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text("Tap to track your mood", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Track how you feel", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
        }
    }
}

@Composable
fun StressCardLarge(
    stressTrend: List<Pair<LocalDate, Int>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avgStress = if (stressTrend.isNotEmpty()) stressTrend.map { it.second }.average().toInt() else null
    val stressColor = when {
        avgStress == null -> Color(0xFF9E9E9E)
        avgStress <= 3 -> Color(0xFF4CAF50)
        avgStress <= 6 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = stressColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u26A1", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(avgStress?.let { "$it/10" } ?: "--", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = stressColor)
            Text("Average this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap to log stress level", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
        }
    }
}

@Composable
fun MeditationCard(
    totalMinutes: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(56.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFE91E63).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SelfImprovement, null, tint = Color(0xFFE91E63), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Meditation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Today's: ${totalMinutes.toInt()} minutes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFE91E63))
        }
    }
}

@Composable
fun MoodInputDialog(
    onSave: (MoodType, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("How are you feeling?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MoodType.entries.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { mood ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { selectedMood = mood },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selectedMood == mood) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(mood.emoji, style = MaterialTheme.typography.headlineSmall)
                                        Text(mood.label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { selectedMood?.let { onSave(it, notes.takeIf { n -> n.isNotBlank() }) } },
                        enabled = selectedMood != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun StressInputDialog(
    onSave: (Int, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var stressLevel by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }

    val stressColor = when {
        stressLevel <= 3 -> Color(0xFF4CAF50)
        stressLevel <= 6 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Stress Level", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                Text("$stressLevel", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = stressColor)
                Text("/10", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = stressLevel.toFloat(),
                    onValueChange = { stressLevel = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Low", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    Text("High", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("What's causing stress? (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(stressLevel, notes.takeIf { it.isNotBlank() }) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickWellnessActions(
    onMood: () -> Unit,
    onStress: () -> Unit,
    onMeditation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                WellnessActionTile("Mood", "\uD83D\uDE0A", Color(0xFFFFB74D), onMood, Modifier.weight(1f))
                WellnessActionTile("Stress", "\u26A1", Color(0xFFFF9800), onStress, Modifier.weight(1f))
                WellnessActionTile("Meditate", "\uD83E\uDDD8", Color(0xFFE91E63), onMeditation, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun WellnessActionTile(label: String, emoji: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
