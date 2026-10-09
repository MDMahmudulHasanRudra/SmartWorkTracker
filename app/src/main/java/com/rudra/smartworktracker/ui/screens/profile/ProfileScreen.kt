package com.rudra.smartworktracker.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.SalaryPeriod
import com.rudra.smartworktracker.data.entity.UserProfile
import com.rudra.smartworktracker.data.repository.UserProfileRepository
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.viewmodel.UserProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModel.Factory(
            UserProfileRepository(AppDatabase.getDatabase(context).userProfileDao())
        )
    )
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (profileState is UserProfileViewModel.ProfileState.Success) {
                        IconButton(onClick = onNavigateToSetup) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = profileState) {
                is UserProfileViewModel.ProfileState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UserProfileViewModel.ProfileState.Success -> ProfileContent(state.profile)
                is UserProfileViewModel.ProfileState.NotCreated -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("No profile yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Add your name and salary so reports and the dashboard can use them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateToSetup) { Text("Set up profile") }
                }
                is UserProfileViewModel.ProfileState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileContent(profile: UserProfile) {
    val context = LocalContext.current
    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found for this action", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                ),
            contentAlignment = Alignment.Center
        ) {
            val initials = profile.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
            if (initials.isNotEmpty()) {
                Text(initials, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(profile.name.ifEmpty { "Your name" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (profile.experience.isNotEmpty()) {
                Text(profile.experience, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }
            if (profile.bio.isNotEmpty()) {
                Text(
                    profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (profile.monthlySalary > 0 || profile.initialSavings > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyTile(
                    label = when (profile.salaryPeriod) {
                        SalaryPeriod.MONTHLY -> "Monthly salary"
                        SalaryPeriod.WEEKLY -> "Weekly salary"
                        SalaryPeriod.BI_WEEKLY -> "Bi-weekly salary"
                    },
                    value = CurrencyManager.formatWhole(profile.monthlySalary),
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
                MoneyTile(
                    label = "Starting savings",
                    value = CurrencyManager.formatWhole(profile.initialSavings),
                    icon = Icons.Default.Savings,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ProfileInfoItem(Icons.Default.Email, "Email", profile.email) {
            open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${profile.email}")))
        }
        ProfileInfoItem(Icons.Default.Phone, "Phone", profile.phone) {
            open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.phone}")))
        }
        ProfileInfoItem(Icons.Default.LocationOn, "Location", profile.location)
        ProfileInfoItem(Icons.Default.Language, "Website", profile.portfolioUrl) {
            val url = profile.portfolioUrl.let { if (it.startsWith("http")) it else "https://$it" }
            open(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        if (profile.skills.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.skills.forEach { skill ->
                        SuggestionChip(onClick = {}, label = { Text(skill) })
                    }
                }
            }
        }

        Text(
            "Member since ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(profile.createdAt))}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun MoneyTile(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    if (value.isEmpty()) return
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
