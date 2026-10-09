package com.rudra.smartworktracker.ui.screens.health

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
fun HydrationCard(
    currentWater: Double,
    targetWater: Double,
    onAddWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentWater / targetWater).toFloat().coerceIn(0f, 1f)
    Card(
        modifier = modifier.clickable(onClick = onAddWater),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalDrink, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
                Surface(color = Color(0xFF2196F3).copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.CircleShape) {
                    Text("${(progress * 100).toInt()}%", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("${currentWater.toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2196F3))
            Text("/ ${targetWater.toInt()} ml", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2196F3).copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
            Text("Tap to add 250ml", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3).copy(alpha = 0.5f), fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
fun WaterLogPopup(
    onLogWater: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocalDrink, null, tint = Color(0xFF2196F3), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Stay Hydrated!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Select your serving size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WaterLogTile(250.0, "Glass", Icons.Default.LocalDrink, Modifier.weight(1f), onLogWater)
                    WaterLogTile(500.0, "Bottle", Icons.Default.LocalDrink, Modifier.weight(1f), onLogWater)
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss) { Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun WaterLogTile(amount: Double, label: String, icon: ImageVector, modifier: Modifier, onLogWater: (Double) -> Unit) {
    Surface(
        modifier = modifier.clickable { onLogWater(amount) },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text("${amount.toInt()}ml", fontWeight = FontWeight.ExtraBold, color = Color(0xFF2196F3))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
        }
    }
}
