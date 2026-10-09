package com.rudra.smartworktracker.ui.screens.journal

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.DailyJournal
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyJournalScreen(viewModel: DailyJournalViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Journal", "History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Daily Journal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Custom Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    CustomTab(
                        title = title,
                        isSelected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            when (selectedTab) {
                0 -> JournalEditor(
                    viewModel = viewModel,
                    onSave = { editedJournal ->
                        viewModel.saveOrUpdateJournal(editedJournal)
                    }
                )
                1 -> JournalHistory(
                    viewModel = viewModel,
                    onEdit = { journal ->
                        viewModel.updateSelectedDate(journal.date)
                        selectedTab = 0
                    },
                    onDelete = { journal ->
                        viewModel.deleteJournal(journal)
                    }
                )
            }
        }
    }
}

@Composable
fun CustomTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = ""
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = ""
    )

    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun JournalEditor(viewModel: DailyJournalViewModel, onSave: (DailyJournal) -> Unit) {
    val todayJournal by viewModel.todayJournal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val loadedJournal by viewModel.loadedJournal.collectAsState()
    val context = LocalContext.current

    // Text is loaded from the database once per date. Re-keying on every DB emission (as before)
    // reset the fields after each auto-save and dropped whatever was typed meanwhile.
    var intention by remember(selectedDate) { mutableStateOf("") }
    var reflection by remember(selectedDate) { mutableStateOf("") }
    var gratitude by remember(selectedDate) { mutableStateOf("") }
    var initializedFor by remember { mutableStateOf<java.time.LocalDate?>(null) }

    LaunchedEffect(loadedJournal, selectedDate) {
        val load = loadedJournal ?: return@LaunchedEffect
        if (load.date == selectedDate && initializedFor != selectedDate) {
            intention = load.journal?.morningIntention ?: ""
            reflection = load.journal?.eveningReflection ?: ""
            gratitude = load.journal?.gratitude ?: ""
            initializedFor = selectedDate
        }
    }
    var currentSection by remember { mutableIntStateOf(0) }

    val sections = listOf("Morning Intention", "Evening Reflection", "Gratitude")
    val sectionIcons = listOf(Icons.Default.LightMode, Icons.Default.Nightlight, Icons.Default.Favorite)
    val sectionColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )

    val autoSaveJob = remember { mutableStateOf<Job?>(null) }
    var autoSaveTrigger by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(intention, reflection, gratitude, autoSaveTrigger) {
        autoSaveJob.value?.cancel()
        val unchanged = todayJournal?.let {
            it.morningIntention == intention && it.eveningReflection == reflection && it.gratitude == gratitude
        } ?: false
        if (initializedFor == selectedDate && !unchanged &&
            (intention.isNotBlank() || reflection.isNotBlank() || gratitude.isNotBlank())
        ) {
            autoSaveJob.value = coroutineScope.launch {
                kotlinx.coroutines.delay(2000) // 2 second delay
                val journalEntry = (todayJournal ?: DailyJournal(date = selectedDate)).copy(
                    morningIntention = intention,
                    eveningReflection = reflection,
                    gratitude = gratitude
                )
                onSave(journalEntry)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DateHeader(
                    date = selectedDate,
                    onDateChange = { viewModel.updateSelectedDate(it) }
                )
            }

            item {
                SectionTabRow(
                    sections = sections,
                    sectionIcons = sectionIcons,
                    currentSection = currentSection,
                    onSectionChange = { currentSection = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                AnimatedVisibility(visible = currentSection == 0) {
                    JournalSection(
                        title = "Morning Intention",
                        subtitle = "What I want to focus on today",
                        placeholder = "My main goal for today is...\nWhat will make today successful?\nHow do I want to show up today?",
                        value = intention,
                        onValueChange = {
                            intention = it
                            autoSaveTrigger++
                        },
                        icon = Icons.Default.LightMode,
                        color = sectionColors[0],
                        characterLimit = 500
                    )
                }
            }

            item {
                AnimatedVisibility(visible = currentSection == 1) {
                    JournalSection(
                        title = "Evening Reflection",
                        subtitle = "How did today go?",
                        placeholder = "Today went well...\nWhat did I learn today?\nWhat could I improve tomorrow?",
                        value = reflection,
                        onValueChange = {
                            reflection = it
                            autoSaveTrigger++
                        },
                        icon = Icons.Default.Nightlight,
                        color = sectionColors[1],
                        characterLimit = 500
                    )
                }
            }

            item {
                AnimatedVisibility(visible = currentSection == 2) {
                    JournalSection(
                        title = "Gratitude Journal",
                        subtitle = "What I am grateful for today",
                        placeholder = "I am grateful for...\nThe small joys in life\nPeople who supported me\nLessons learned",
                        value = gratitude,
                        onValueChange = {
                            gratitude = it
                            autoSaveTrigger++
                        },
                        icon = Icons.Default.Favorite,
                        color = sectionColors[2],
                        characterLimit = 300
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        val isFormEmpty = intention.isBlank() && reflection.isBlank() && gratitude.isBlank()
        val hasChanges = todayJournal?.let {
            it.morningIntention != intention ||
                    it.eveningReflection != reflection ||
                    it.gratitude != gratitude
        } ?: !isFormEmpty

        val buttonScale = remember { Animatable(1f) }

        LaunchedEffect(hasChanges) {
            if (hasChanges) {
                buttonScale.animateTo(
                    1.02f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                buttonScale.animateTo(1f)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Button(
                onClick = {
                    val journalEntry = (todayJournal ?: DailyJournal(date = selectedDate)).copy(
                        morningIntention = intention,
                        eveningReflection = reflection,
                        gratitude = gratitude
                    )
                    onSave(journalEntry)
                    Toast.makeText(context, "✨ Journal Saved Successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = hasChanges && !isFormEmpty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (todayJournal != null) "Update Journal" else "Save Journal",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SectionTabRow(
    sections: List<String>,
    sectionIcons: List<ImageVector>,
    currentSection: Int,
    onSectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEachIndexed { index, section ->
                val isSelected = currentSection == index
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent, label = ""
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant, label = ""
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onSectionChange(index) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            sectionIcons[index],
                            contentDescription = section,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = section.split(" ")[0], // Show only first word
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalHistory(
    viewModel: DailyJournalViewModel,
    onEdit: (DailyJournal) -> Unit,
    onDelete: (DailyJournal) -> Unit
) {
    val journalHistory by viewModel.journalHistory.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<DailyJournal?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredJournals = journalHistory.filter { journal ->
        searchQuery.isEmpty() ||
                journal.morningIntention.contains(searchQuery, ignoreCase = true) ||
                journal.eveningReflection.contains(searchQuery, ignoreCase = true) ||
                journal.gratitude.contains(searchQuery, ignoreCase = true) ||
                journal.date.toDisplayFormat().contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        if (journalHistory.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search journal entries...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        if (filteredJournals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "No entries",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (journalHistory.isEmpty()) "No journal entries yet."
                        else "No matching entries found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredJournals, key = { it.id }) { journal ->
                    JournalHistoryItem(
                        journal = journal,
                        onEdit = onEdit,
                        onDelete = { showDeleteDialog = it }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { journal ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(
                    "Delete Journal Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("Are you sure you want to delete the journal for ${journal.date.toDisplayFormat()}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(journal)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun JournalHistoryItem(
    journal: DailyJournal,
    onEdit: (DailyJournal) -> Unit,
    onDelete: (DailyJournal) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(journal) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    journal.date.toDisplayFormat(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(
                        onClick = { onEdit(journal) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onDelete(journal) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview of content
            val previewText = buildString {
                if (journal.morningIntention.isNotBlank()) {
                    append("🌅 ${journal.morningIntention.take(60)}")
                    if (journal.morningIntention.length > 60) append("...")
                }
                if (journal.eveningReflection.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("🌙 ${journal.eveningReflection.take(60)}")
                    if (journal.eveningReflection.length > 60) append("...")
                }
                if (journal.gratitude.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("❤️ ${journal.gratitude.take(60)}")
                    if (journal.gratitude.length > 60) append("...")
                }
            }.ifBlank { "No content" }

            Text(
                text = previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun DateHeader(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onDateChange(date.minusDays(1)) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.toDisplayFormat(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (date == LocalDate.now()) "Today"
                    else if (date == LocalDate.now().minusDays(1)) "Yesterday"
                    else if (date == LocalDate.now().plusDays(1)) "Tomorrow"
                    else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = { onDateChange(date.plusDays(1)) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun JournalSection(
    title: String,
    subtitle: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    color: Color,
    characterLimit: Int
) {
    var isFocused by remember { mutableStateOf(false) }
    val characterCount = value.length
    val progress = characterCount.toFloat() / characterLimit

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 4.dp
        ),
        border = if (isFocused) BorderStroke(2.dp, color) else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= characterLimit) onValueChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "$characterCount/$characterLimit",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (characterCount > characterLimit * 0.9) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
