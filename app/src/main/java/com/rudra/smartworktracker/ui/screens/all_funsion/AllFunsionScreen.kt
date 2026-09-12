package com.rudra.smartworktracker.ui.screens.all_funsion

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rudra.smartworktracker.ui.navigation.NavigationItem

data class FeatureSection(val title: String, val items: List<NavigationItem>, val emoji: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllFunsionScreen(navController: NavController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel: AllFunsionViewModel = viewModel(factory = AllFunsionViewModelFactory(context.applicationContext as Application))
    val recentFeatures by viewModel.recentFeatures.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableIntStateOf(0) }

    val categories = remember {
        listOf("All", "Productivity", "Finance", "Insights", "Tools")
    }

    val quickAccessFeatures = remember {
        listOf(
            NavigationItem.Accounts,
            NavigationItem.Transfer,
            NavigationItem.AddEntry,
            NavigationItem.WorkTimer,
            NavigationItem.Focus,
            NavigationItem.Calendar,
            NavigationItem.Analytics,
            NavigationItem.Team
        )
    }
    val featureSections = remember {
        listOf(
            FeatureSection(
                "Productivity & Wellness", listOf(
                    NavigationItem.Journal,
                    NavigationItem.Habit,
                    NavigationItem.Health,
                    NavigationItem.Achievements,
                    NavigationItem.MindfulBreak,
                    NavigationItem.Wisdom,
                    NavigationItem.Focus,
                    NavigationItem.WorkTimer,
                    NavigationItem.Overtime,
                    NavigationItem.Scheduler,
                    NavigationItem.FutureImpact,
                    NavigationItem.RealityTracker
                ), "\uD83D\uDCCB"
            ),
            FeatureSection(
                "Financials", listOf(
                    NavigationItem.Accounts,
                    NavigationItem.Transfer,
                    NavigationItem.Income,
                    NavigationItem.Expense,
                    NavigationItem.Savings,
                    NavigationItem.Loans,
                    NavigationItem.EMI,
                    NavigationItem.CreditCard,
                    NavigationItem.FinancialStatement,
                    NavigationItem.Reports,
                    NavigationItem.MonthlyReport,
                    NavigationItem.Calculation,
                    NavigationItem.AddEntry,
                    NavigationItem.Recurring,
                    NavigationItem.SpendAdvisor,
                    NavigationItem.BillSplit
                ), "\uD83D\uDCB0"
            ),
            FeatureSection(
                "General", listOf(
                    NavigationItem.Backup,
                    NavigationItem.Settings,
                    NavigationItem.Team,
                    NavigationItem.UserProfile
                ), "\u2699\uFE0F"
            ),
        )
    }

    val filteredSections = remember(selectedCategory, featureSections) {
        when (selectedCategory) {
            1 -> featureSections.filter { it.title.contains("Productivity") }
            2 -> featureSections.filter { it.title.contains("Financial") }
            3 -> featureSections.filter { false }
            4 -> featureSections.filter { it.title.contains("General") }
            else -> featureSections
        }
    }

    val allFeatures = remember { (quickAccessFeatures + featureSections.flatMap { it.items }).distinctBy { it.route } }

    val totalFeatures = remember { allFeatures.size }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "All Features",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            "$totalFeatures tools in one place",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Header with Search
            item {
                HeroHeader(
                    searchText = searchText,
                    onSearchChange = { searchText = it },
                    totalFeatures = totalFeatures
                )
            }

            // Category Chips
            item {
                CategoryChips(
                    categories = categories,
                    selectedIndex = selectedCategory,
                    onSelected = { selectedCategory = it }
                )
            }

            // Recently Used
            if (searchText.isBlank() && recentFeatures.isNotEmpty()) {
                item {
                    SectionTitle("Recently Used", "Last opened")
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(recentFeatures) { feature ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { visible = true }
                            RecentFeatureCard(
                                feature = feature,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onFeatureClicked(feature)
                                    navController.navigate(feature.route)
                                },
                                isVisible = visible
                            )
                        }
                    }
                }
            }

            // Quick Access
            if (searchText.isBlank() && selectedCategory == 0) {
                item {
                    SectionTitle("Quick Access", "Frequently used")
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(quickAccessFeatures) { feature ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { visible = true }
                            QuickAccessChip(
                                feature = feature,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onFeatureClicked(feature)
                                    navController.navigate(feature.route)
                                },
                                isVisible = visible
                            )
                        }
                    }
                }
            }

            // Feature Sections
            filteredSections.forEach { section ->
                item {
                    SectionTitle(
                        title = "${section.emoji} ${section.title}",
                        subtitle = "${section.items.size} features"
                    )
                }
                items(section.items, key = { it.route }) { feature ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    PremiumFeatureCard(
                        feature = feature,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onFeatureClicked(feature)
                            navController.navigate(feature.route)
                        },
                        isVisible = visible
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Search Results
            if (searchText.isNotBlank()) {
                val searchResults = allFeatures.filter {
                    it.title.contains(searchText, ignoreCase = true) ||
                            it.description?.contains(searchText, ignoreCase = true) == true
                }
                item {
                    SectionTitle(
                        title = "Search Results",
                        subtitle = "${searchResults.size} found"
                    )
                }
                if (searchResults.isNotEmpty()) {
                    items(searchResults, key = { it.route }) { feature ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }
                        PremiumFeatureCard(
                            feature = feature,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onFeatureClicked(feature)
                                navController.navigate(feature.route)
                            },
                            isVisible = visible
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "\uD83D\uDD0D",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No features found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Try a different search term",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeroHeader(
    searchText: String,
    onSearchChange: (String) -> Unit,
    totalFeatures: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Column {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search $totalFeatures features...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
    ) {
        itemsIndexed(categories) { index, category ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                label = {
                    Text(
                        category,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun getFeatureColor(feature: NavigationItem): Color {
    return when (feature.route) {
        NavigationItem.Income.route, NavigationItem.Savings.route, NavigationItem.AddEntry.route, NavigationItem.Health.route -> Color(0xFF43A047)
        NavigationItem.Expense.route, NavigationItem.Loans.route, NavigationItem.EMI.route, NavigationItem.CreditCard.route -> Color(0xFFE53935)
        NavigationItem.WorkTimer.route, NavigationItem.Focus.route, NavigationItem.Analytics.route, NavigationItem.Calendar.route, NavigationItem.Reports.route, NavigationItem.MonthlyReport.route, NavigationItem.FinancialStatement.route, NavigationItem.Transfer.route -> Color(0xFF1E88E5)
        NavigationItem.Habit.route, NavigationItem.Journal.route, NavigationItem.MindfulBreak.route, NavigationItem.Wisdom.route, NavigationItem.Achievements.route -> Color(0xFF8E24AA)
        NavigationItem.Settings.route, NavigationItem.Backup.route, NavigationItem.UserProfile.route, NavigationItem.Team.route, NavigationItem.Overtime.route, NavigationItem.Scheduler.route -> Color(0xFF546E7A)
        else -> Color(0xFF3949AB)
    }
}

@Composable
private fun RecentFeatureCard(
    feature: NavigationItem,
    onClick: () -> Unit,
    isVisible: Boolean
) {
    val featureColor = getFeatureColor(feature)
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400), label = "alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(400), label = "offsetY"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(140.dp)
            .alpha(alpha)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(featureColor.copy(alpha = 0.7f), featureColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (feature.description != null) {
                    Text(
                        text = feature.description!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessChip(
    feature: NavigationItem,
    onClick: () -> Unit,
    isVisible: Boolean
) {
    val featureColor = getFeatureColor(feature)
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300), label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = featureColor.copy(alpha = 0.1f),
        modifier = Modifier
            .alpha(alpha)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = featureColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = feature.title,
                style = MaterialTheme.typography.labelMedium,
                color = featureColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    feature: NavigationItem,
    onClick: () -> Unit,
    isVisible: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val featureColor = getFeatureColor(feature)

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f), label = "cardScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400), label = "alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f), label = "translationY"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed) featureColor.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .scale(cardScale)
            .alpha(alpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                featureColor.copy(alpha = 0.6f),
                                featureColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (feature.description != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = feature.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
            Icon(
                imageVector = NavigationItem.Dashboard.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(16.dp)
                    .scale(if (isPressed) 1.2f else 0.8f)
            )
        }
    }
}
