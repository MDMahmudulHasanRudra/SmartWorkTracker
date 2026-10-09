package com.rudra.smartworktracker.ui.screens.wisdom

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.Goal
import com.rudra.smartworktracker.model.GoalCategory
import com.rudra.smartworktracker.model.Target
import com.rudra.smartworktracker.model.Wisdom
import com.rudra.smartworktracker.model.WisdomCategory
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

private val StreakColor = Color(0xFFFF7043)
private val BoostColor = Color(0xFFFFA000)
private val SuccessColor = Color(0xFF43A047)

@Composable
fun WisdomScreen(viewModel: WisdomViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Wisdom", "Life Plan", "Boosters", "Shop")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatsDashboard(uiState.userStats, uiState.goals)

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title, maxLines = 1) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> WisdomContent(viewModel)
                    1 -> GoalScreen(uiState, viewModel)
                    2 -> BoosterManagementScreen(uiState, viewModel, onOpenShop = { selectedTab = 3 })
                    3 -> EnhancedShopScreen(uiState, viewModel)
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.showTargetCelebration,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TargetCelebrationAnimation(uiState.lastAchievedItemName, uiState.lastXpGained)
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        if (uiState.showGoalCelebration) {
            GoalCelebrationAnimation(
                achievementName = uiState.lastAchievedItemName,
                xpGained = uiState.lastXpGained,
                onDismiss = viewModel::dismissGoalCelebration
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsDashboard(stats: UserStats, goals: List<Goal>) {
    val progress = (stats.experiencePoints.toFloat() / stats.xpForNextLevel).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, tween(800), label = "xp")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stats.level.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Level ${stats.level}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${stats.xpForNextLevel - stats.experiencePoints} XP to level ${stats.level + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = "${stats.experiencePoints} / ${stats.xpForNextLevel}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatPill(Icons.Default.Whatshot, "${stats.streak}d streak", StreakColor)
                StatPill(Icons.Default.Shield, "${stats.streakProtectionAvailable} saves", MaterialTheme.colorScheme.primary)
                StatPill(Icons.Default.EmojiEvents, "${goals.count { it.isCompleted }}/${goals.size} goals", SuccessColor)
                if (stats.xpMultiplier > 1.0f) {
                    StatPill(Icons.Default.Bolt, "${formatMultiplier(stats.xpMultiplier)} XP", BoostColor)
                }
            }
        }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------------ Wisdom tab

@Composable
fun WisdomContent(viewModel: WisdomViewModel) {
    val quoteOfDay = remember { viewModel.quoteOfTheDay() }
    val grouped = remember { viewModel.getWisdom().groupBy { it.category } }
    var filter by rememberSaveable { mutableStateOf<WisdomCategory?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { QuoteOfTheDayCard(quoteOfDay) }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                WisdomCategory.entries.forEach { category ->
                    FilterChip(
                        selected = filter == category,
                        onClick = { filter = if (filter == category) null else category },
                        label = { Text(category.displayName(), maxLines = 1) }
                    )
                }
            }
        }
        grouped.filterKeys { filter == null || it == filter }.forEach { (category, wisdoms) ->
            item(key = "header_${category.name}") {
                Text(
                    text = category.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(wisdoms, key = { it.text }) { wisdom -> WisdomItem(wisdom) }
        }
    }
}

@Composable
private fun QuoteOfTheDayCard(wisdom: Wisdom) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Quote of the day",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = wisdom.text,
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            wisdom.author?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "— $it",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WisdomItem(wisdom: Wisdom) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "“${wisdom.text}”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            wisdom.author?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ------------------------------------------------------------------------ Life plan tab

@Composable
fun GoalScreen(uiState: WisdomUiState, viewModel: WisdomViewModel) {
    val goals = uiState.goals
    val selectedGoal = uiState.selectedGoal
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAddGoal = {
                viewModel.addGoal(it)
                showAddGoalDialog = false
            }
        )
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete goal?") },
            text = { Text("\"${goal.title}\" and its ${goal.totalTargets} target(s) will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGoal(goal.id)
                    goalToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text("Cancel") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            goals.isEmpty() -> EmptyGoals(
                modifier = Modifier.align(Alignment.Center),
                onAdd = { showAddGoalDialog = true }
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    val expanded = selectedGoal?.id == goal.id
                    Column {
                        GoalCard(
                            goal = goal,
                            expanded = expanded,
                            onClick = { viewModel.selectGoal(if (expanded) null else goal) },
                            onDelete = { goalToDelete = goal }
                        )
                        AnimatedVisibility(visible = expanded) {
                            GoalDetailSection(
                                goal = goal,
                                targets = uiState.targetsMap[goal.id].orEmpty(),
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        if (goals.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { showAddGoalDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New goal") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyGoals(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Flag,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text("Plan your life goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Set a goal, split it into small targets and earn ${WisdomViewModel.TARGET_XP} XP for each one you finish.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add your first goal")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onAddGoal: (Goal) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(GoalCategory.PERSONAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Goal") },
                    placeholder = { Text("e.g. Run a half marathon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(240) },
                    label = { Text("Why it matters (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName()) },
                            leadingIcon = { Icon(category.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddGoal(Goal(title = title.trim(), description = description.trim(), category = selectedCategory)) },
                enabled = title.isNotBlank()
            ) { Text("Add goal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun GoalCard(goal: Goal, expanded: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val progress = if (goal.totalTargets > 0) goal.completedTargets.toFloat() / goal.totalTargets else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "GoalProgress"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (goal.isCompleted) SuccessColor.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (goal.isCompleted) Icons.Default.CheckCircle else goal.category.icon(),
                            contentDescription = null,
                            tint = if (goal.isCompleted) SuccessColor else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = goal.category.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = if (goal.isCompleted) SuccessColor else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    goal.totalTargets == 0 -> "No targets yet — tap to add some"
                    goal.isCompleted -> "All ${goal.totalTargets} targets done"
                    else -> "${goal.completedTargets}/${goal.totalTargets} targets · ${(progress * 100).toInt()}%"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GoalDetailSection(goal: Goal, targets: List<Target>, viewModel: WisdomViewModel) {
    var newTargetTitle by rememberSaveable(goal.id) { mutableStateOf("") }
    val submit = {
        if (newTargetTitle.isNotBlank()) {
            viewModel.addTargetToGoal(goal.id, newTargetTitle)
            newTargetTitle = ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Targets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            if (targets.isEmpty()) {
                Text(
                    "Add the first step toward this goal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            targets.forEach { target ->
                TargetItem(
                    target = target,
                    onComplete = { viewModel.completeTarget(goal.id, target.id) },
                    onDelete = { viewModel.deleteTarget(goal.id, target.id) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newTargetTitle,
                onValueChange = { newTargetTitle = it.take(80) },
                label = { Text("New target") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(onClick = submit, enabled = newTargetTitle.isNotBlank()) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add target")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TargetItem(target: Target, onComplete: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = target.isCompleted,
            onCheckedChange = { if (it) onComplete() },
            enabled = !target.isCompleted
        )
        Text(
            text = target.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (target.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (target.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        if (!target.isCompleted) {
            Text(
                "+${WisdomViewModel.TARGET_XP} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete target",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun GoalCelebrationAnimation(achievementName: String, xpGained: Int, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "goal_celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Goal achieved!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievementName,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF80DEEA),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "+$xpGained XP", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Tap to continue", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun TargetCelebrationAnimation(achievementName: String, xpGained: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Target achieved!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(achievementName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = "+$xpGained XP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ------------------------------------------------------------------------ Shop & boosters

@Composable
fun EnhancedShopScreen(uiState: WisdomUiState, viewModel: WisdomViewModel) {
    val xp = uiState.userStats.experiencePoints

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Spend XP earned this level. Spending never lowers your level.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            ShopSection(
                title = "Streak protection",
                subtitle = "Each one covers a missed day so your streak keeps going."
            ) {
                ShopOption("1 save", 100, xp, Icons.Default.Shield, MaterialTheme.colorScheme.primary) {
                    viewModel.purchaseStreakProtection(1, 100)
                }
                ShopOption("3 saves", 250, xp, Icons.Default.Shield, MaterialTheme.colorScheme.primary) {
                    viewModel.purchaseStreakProtection(3, 250)
                }
                ShopOption("5 saves", 400, xp, Icons.Default.Shield, MaterialTheme.colorScheme.primary) {
                    viewModel.purchaseStreakProtection(5, 400)
                }
            }
        }
        item {
            ShopSection(
                title = "XP boosters",
                subtitle = "Multiply XP from targets, goals and streaks for ${WisdomViewModel.BOOSTER_HOURS} hour."
            ) {
                ShopOption(BoosterType.XP_1_5X.label, 100, xp, Icons.Default.Bolt, BoostColor) {
                    viewModel.purchaseBooster(BoosterType.XP_1_5X, 100)
                }
                ShopOption(BoosterType.XP_2X.label, 150, xp, Icons.Default.Bolt, BoostColor) {
                    viewModel.purchaseBooster(BoosterType.XP_2X, 150)
                }
            }
        }
    }
}

@Composable
private fun ShopSection(title: String, subtitle: String, content: @Composable RowScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun RowScope.ShopOption(
    name: String,
    cost: Int,
    xpBalance: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onPurchase: () -> Unit
) {
    val canAfford = xpBalance >= cost
    Card(
        onClick = onPurchase,
        enabled = canAfford,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) tint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = if (canAfford) tint else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(
                "$cost XP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (canAfford) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun BoosterManagementScreen(uiState: WisdomUiState, viewModel: WisdomViewModel, onOpenShop: () -> Unit) {
    val inventory = uiState.inventoryBoosters
    val active = uiState.activeBoosters

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (active.isEmpty()) {
            item {
                Text(
                    "No boosters running.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(active, key = { it.id }) { booster -> ActiveBoosterItem(booster) }
        }

        item {
            Text(
                "Inventory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (inventory.isEmpty()) {
            item {
                Column {
                    Text(
                        "Your inventory is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onOpenShop) { Text("Visit the shop") }
                }
            }
        } else {
            items(inventory, key = { it.id }) { booster ->
                val sameTypeRunning = active.any { it.type == booster.type }
                InventoryBoosterItem(booster, enabled = !sameTypeRunning) { viewModel.activateBooster(booster) }
            }
        }
    }
}

@Composable
fun ActiveBoosterItem(booster: Booster) {
    // Ticks every second so the countdown actually moves
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(booster.id) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val remaining = booster.expiresAt?.let { Duration.between(now, it) }?.takeIf { !it.isNegative } ?: Duration.ZERO
    val total = Duration.ofHours(booster.durationHours.toLong()).seconds.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BoostColor.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = BoostColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(booster.type.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    String.format(
                        java.util.Locale.getDefault(),
                        "%d:%02d:%02d",
                        // toMinutesPart()/toSecondsPart() need API 31; minSdk is 28
                        remaining.seconds / 3600,
                        (remaining.seconds % 3600) / 60,
                        remaining.seconds % 60
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { remaining.seconds.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
                color = BoostColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun InventoryBoosterItem(booster: Booster, enabled: Boolean, onActivate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = BoostColor)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(booster.type.label, fontWeight = FontWeight.Bold)
                Text(
                    if (enabled) "${booster.durationHours} hour duration" else "Same booster already running",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onActivate, enabled = enabled) { Text("Activate") }
        }
    }
}

// ------------------------------------------------------------------------ helpers

private fun formatMultiplier(value: Float): String =
    if (value % 1f == 0f) "${value.toInt()}x" else "${"%.1f".format(value)}x"

private fun WisdomCategory.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun GoalCategory.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun GoalCategory.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    GoalCategory.CAREER -> Icons.Default.Work
    GoalCategory.HEALTH -> Icons.Default.Favorite
    GoalCategory.FINANCE -> Icons.Default.AccountBalance
    GoalCategory.LEARNING -> Icons.Default.School
    GoalCategory.PERSONAL -> Icons.Default.Person
    GoalCategory.OTHER -> Icons.Default.Flag
}
