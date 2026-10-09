package com.rudra.smartworktracker.ui.screens.profile

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.SalaryPeriod
import com.rudra.smartworktracker.data.entity.UserProfile
import com.rudra.smartworktracker.data.repository.UserProfileRepository
import com.rudra.smartworktracker.ui.components.sanitizeAmountInput
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    onProfileSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserProfileViewModel = viewModel(
        factory = UserProfileViewModel.Factory(
            UserProfileRepository(AppDatabase.getDatabase(context).userProfileDao())
        )
    )
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val existing = (profileState as? UserProfileViewModel.ProfileState.Success)?.profile
    val snackbarHostState = remember { SnackbarHostState() }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }
    var portfolio by rememberSaveable { mutableStateOf("") }
    var skillInput by rememberSaveable { mutableStateOf("") }
    var skills by rememberSaveable { mutableStateOf(listOf<String>()) }
    var monthlySalary by rememberSaveable { mutableStateOf("") }
    var initialSavings by rememberSaveable { mutableStateOf("") }
    var salaryPeriod by rememberSaveable { mutableStateOf(SalaryPeriod.MONTHLY) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    // Fill the form once from the stored profile (re-filling on every emission wiped edits)
    LaunchedEffect(profileState) {
        if (loaded) return@LaunchedEffect
        when (val state = profileState) {
            is UserProfileViewModel.ProfileState.Success -> {
                val p = state.profile
                name = p.name
                email = p.email
                phone = p.phone
                bio = p.bio
                location = p.location
                experience = p.experience
                portfolio = p.portfolioUrl
                skills = p.skills
                monthlySalary = if (p.monthlySalary > 0) plainAmount(p.monthlySalary) else ""
                initialSavings = if (p.initialSavings > 0) plainAmount(p.initialSavings) else ""
                salaryPeriod = p.salaryPeriod
                loaded = true
            }
            is UserProfileViewModel.ProfileState.NotCreated -> loaded = true
            else -> Unit
        }
    }

    val nameError = name.isBlank()
    val emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val phoneError = phone.isNotBlank() && phone.count { it.isDigit() } < 6

    fun addSkill() {
        val skill = skillInput.trim()
        if (skill.isNotEmpty() && skills.none { it.equals(skill, ignoreCase = true) }) skills = skills + skill
        skillInput = ""
    }

    fun save() {
        showErrors = true
        if (nameError || emailError || phoneError || saving) return
        saving = true
        // Start from the stored row so fields this form doesn't show (language, image, createdAt) survive
        val base = existing ?: UserProfile()
        viewModel.saveProfile(
            base.copy(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                bio = bio.trim(),
                location = location.trim(),
                experience = experience.trim(),
                portfolioUrl = portfolio.trim(),
                skills = skills,
                monthlySalary = monthlySalary.toDoubleOrNull() ?: 0.0,
                initialSavings = initialSavings.toDoubleOrNull() ?: 0.0,
                salaryPeriod = salaryPeriod
            ),
            onSaved = onProfileSaved,
            onError = {
                saving = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit profile" else "Set up profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = ::save, enabled = !saving) { Text("Save") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormSection("About you") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Full name *") },
                    isError = showErrors && nameError,
                    supportingText = if (showErrors && nameError) ({ Text("Name is required") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(200) },
                    label = { Text("Short bio") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(60) },
                    label = { Text("Location") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
            }

            FormSection("Contact") {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(80) },
                    label = { Text("Email") },
                    isError = showErrors && emailError,
                    supportingText = if (showErrors && emailError) ({ Text("Enter a valid email address") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input -> phone = input.filter { it.isDigit() || it in "+-() " }.take(20) },
                    label = { Text("Phone") },
                    isError = showErrors && phoneError,
                    supportingText = if (showErrors && phoneError) ({ Text("Enter a valid phone number") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
                OutlinedTextField(
                    value = portfolio,
                    onValueChange = { portfolio = it.trim().take(120) },
                    label = { Text("Website / portfolio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
                )
            }

            FormSection("Work") {
                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it.take(200) },
                    label = { Text("Role / experience") },
                    placeholder = { Text("e.g. Senior engineer, 5 years") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) }
                )
                OutlinedTextField(
                    value = skillInput,
                    onValueChange = { skillInput = it.take(30) },
                    label = { Text("Add a skill") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addSkill() }),
                    trailingIcon = {
                        IconButton(onClick = ::addSkill, enabled = skillInput.isNotBlank()) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add skill")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (skills.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        skills.forEach { skill ->
                            InputChip(
                                selected = false,
                                onClick = { skills = skills - skill },
                                label = { Text(skill) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $skill", modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            FormSection("Money") {
                Text("Salary paid", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SalaryPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = salaryPeriod == period,
                            onClick = { salaryPeriod = period },
                            shape = SegmentedButtonDefaults.itemShape(index, SalaryPeriod.entries.size)
                        ) {
                            Text(
                                when (period) {
                                    SalaryPeriod.MONTHLY -> "Monthly"
                                    SalaryPeriod.WEEKLY -> "Weekly"
                                    SalaryPeriod.BI_WEEKLY -> "Bi-weekly"
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = monthlySalary,
                    onValueChange = { input -> sanitizeAmountInput(input)?.let { monthlySalary = it } },
                    label = { Text("Salary per period") },
                    singleLine = true,
                    prefix = { Text(CurrencyManager.symbol()) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = initialSavings,
                    onValueChange = { input -> sanitizeAmountInput(input)?.let { initialSavings = it } },
                    label = { Text("Savings when you started") },
                    singleLine = true,
                    prefix = { Text(CurrencyManager.symbol()) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Button(
                onClick = ::save,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save profile")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

/** 1500.0 -> "1500", 1500.5 -> "1500.5" for editing. */
private fun plainAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
