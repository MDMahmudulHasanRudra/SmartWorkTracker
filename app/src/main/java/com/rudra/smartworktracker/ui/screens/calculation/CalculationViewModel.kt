package com.rudra.smartworktracker.ui.screens.calculation

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.Calculation
import com.rudra.smartworktracker.data.entity.TravelAndExpense
import com.rudra.smartworktracker.model.WorkType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CalculationUiState(
    val isLoading: Boolean = true,
    val selectedDate: Date = Date(),
    val calculation: Calculation? = null,
    val travelExpense: TravelAndExpense? = null,
    val mealCostPerWeek: Double = 0.0,
    val mealCostPerMonth: Double = 0.0,
    val mealCostPerYear: Double = 0.0,
    val travelCostPerWeek: Double = 0.0,
    val travelCostPerMonth: Double = 0.0,
    val travelCostPerYear: Double = 0.0,
    val otherExpensePerMonth: Double = 0.0,
    val otherExpensePerYear: Double = 0.0,
    val totalExpensePerMonth: Double = 0.0,
    val totalExpensePerYear: Double = 0.0,
    val officeDays: Int = 0,
    val homeOfficeDays: Int = 0,
    val pieChartData: Map<String, Float> = emptyMap(),
    val monthlyBreakdown: List<Pair<String, Double>> = emptyList()
)

class CalculationViewModel(private val db: AppDatabase) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculationUiState())
    val uiState: StateFlow<CalculationUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    db.calculationDao().getCalculation(),
                    db.travelExpenseDao().getTravelExpense()
                ) { calc, travelExpense ->
                    Pair(calc, travelExpense)
                }.collectLatest { (calc, travelExp) ->
                    val currentCalc = calc ?: Calculation(
                        dailyMealRate = 58.0,
                        lastUpdated = System.currentTimeMillis()
                    )
                    val currentTravelExp = travelExp ?: TravelAndExpense(
                        dailyTravelCost = 150.0,
                        otherExpenses = 0.0,
                        otherExpenseDescription = "",
                        lastUpdated = System.currentTimeMillis()
                    )

                    _uiState.update { it.copy(calculation = currentCalc, travelExpense = currentTravelExp) }

                    fetchWorkLogData(currentCalc.dailyMealRate, currentTravelExp, _uiState.value.selectedDate)
                    fetchMonthlyBreakdown(currentCalc.dailyMealRate, currentTravelExp)
                }
            } catch (e: Exception) {
                _errorMessage.emit("Failed to load data: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun fetchWorkLogData(
        dailyMealRate: Double,
        travelExpense: TravelAndExpense,
        date: Date
    ) {
        try {
            val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val selectedMonthYear = monthYearFormat.format(date)

            val workLogs = db.workLogDao().getWorkLogsByMonth(selectedMonthYear)
            val officeDaysCount = workLogs.count { it.workType == WorkType.OFFICE }
            val homeOfficeDaysCount = workLogs.count { it.workType == WorkType.HOME_OFFICE }

            val pieData = mapOf(
                "Office" to officeDaysCount.toFloat(),
                "Home Office" to homeOfficeDaysCount.toFloat()
            )

            calculateAllCosts(dailyMealRate, travelExpense, officeDaysCount, officeDaysCount, homeOfficeDaysCount, pieData)
        } catch (e: Exception) {
            _errorMessage.emit("Failed to fetch work logs: ${e.message}")
        }
    }

    private suspend fun fetchMonthlyBreakdown(
        dailyMealRate: Double,
        travelExpense: TravelAndExpense
    ) {
        try {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val monthlyData = mutableListOf<Pair<String, Double>>()

            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

            for (month in 0..11) {
                calendar.set(currentYear, month, 1)
                val monthName = monthFormat.format(calendar.time)

                val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val selectedMonthYear = yearMonthFormat.format(calendar.time)
                val workLogs = db.workLogDao().getWorkLogsByMonth(selectedMonthYear)
                val officeDaysCount = workLogs.count { it.workType == WorkType.OFFICE }

                val monthlyCost = (dailyMealRate + travelExpense.dailyTravelCost) * officeDaysCount + travelExpense.otherExpenses
                monthlyData.add(monthName to monthlyCost)
            }

            _uiState.update { it.copy(monthlyBreakdown = monthlyData) }
        } catch (e: Exception) {
            _errorMessage.emit("Failed to fetch monthly breakdown: ${e.message}")
        }
    }

    private fun calculateAllCosts(
        dailyMealRate: Double,
        travelExpense: TravelAndExpense,
        officeDays: Int,
        officeDaysForState: Int = officeDays,
        homeOfficeDaysForState: Int = 0,
        pieData: Map<String, Float> = emptyMap()
    ) {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = _uiState.value.selectedDate
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val weeksInMonth = if (daysInMonth > 0) daysInMonth / 7.0 else 4.33

            val weeklyOfficeDays = if (weeksInMonth > 0) officeDays / weeksInMonth else 0.0

            val mealWeek = dailyMealRate * weeklyOfficeDays
            val mealMonth = dailyMealRate * officeDays
            val mealYear = mealMonth * 12
            val travelWeek = travelExpense.dailyTravelCost * weeklyOfficeDays
            val travelMonth = travelExpense.dailyTravelCost * officeDays
            val travelYear = travelMonth * 12
            val otherMonth = travelExpense.otherExpenses
            val otherYear = otherMonth * 12
            val totalMonth = mealMonth + travelMonth + otherMonth
            val totalYear = mealYear + travelYear + otherYear

            _uiState.update {
                it.copy(
                    mealCostPerWeek = mealWeek,
                    mealCostPerMonth = mealMonth,
                    mealCostPerYear = mealYear,
                    travelCostPerWeek = travelWeek,
                    travelCostPerMonth = travelMonth,
                    travelCostPerYear = travelYear,
                    otherExpensePerMonth = otherMonth,
                    otherExpensePerYear = otherYear,
                    totalExpensePerMonth = totalMonth,
                    totalExpensePerYear = totalYear,
                    officeDays = officeDaysForState,
                    homeOfficeDays = homeOfficeDaysForState,
                    pieChartData = pieData
                )
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _errorMessage.emit("Calculation error: ${e.message}")
            }
        }
    }

    fun saveDailyMealRate(rate: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentCalculation = _uiState.value.calculation ?: Calculation(
                    dailyMealRate = 58.0,
                    lastUpdated = System.currentTimeMillis()
                )
                val updatedCalculation = currentCalculation.copy(
                    dailyMealRate = rate,
                    lastUpdated = System.currentTimeMillis()
                )
                db.calculationDao().insert(updatedCalculation)
                _uiState.update { it.copy(calculation = updatedCalculation) }

                fetchWorkLogData(rate, _uiState.value.travelExpense ?: TravelAndExpense(), _uiState.value.selectedDate)
                fetchMonthlyBreakdown(rate, _uiState.value.travelExpense ?: TravelAndExpense())
            } catch (e: Exception) {
                _errorMessage.emit("Failed to save meal rate: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun saveTravelExpense(dailyTravelCost: Double, otherExpenses: Double, description: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentExpense = _uiState.value.travelExpense ?: TravelAndExpense()
                val updatedExpense = currentExpense.copy(
                    dailyTravelCost = dailyTravelCost,
                    otherExpenses = otherExpenses,
                    otherExpenseDescription = description,
                    lastUpdated = System.currentTimeMillis()
                )
                db.travelExpenseDao().insert(updatedExpense)
                _uiState.update { it.copy(travelExpense = updatedExpense) }

                fetchWorkLogData(_uiState.value.calculation?.dailyMealRate ?: 100.0, updatedExpense, _uiState.value.selectedDate)
                fetchMonthlyBreakdown(_uiState.value.calculation?.dailyMealRate ?: 100.0, updatedExpense)
            } catch (e: Exception) {
                _errorMessage.emit("Failed to save travel expense: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun goToPreviousMonth() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedDate
        calendar.add(Calendar.MONTH, -1)
        _uiState.update { it.copy(selectedDate = calendar.time) }
        refreshData()
    }

    fun goToNextMonth() {
        val calendar = Calendar.getInstance()
        calendar.time = _uiState.value.selectedDate
        calendar.add(Calendar.MONTH, 1)
        _uiState.update { it.copy(selectedDate = calendar.time) }
        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                fetchWorkLogData(
                    _uiState.value.calculation?.dailyMealRate ?: 100.0,
                    _uiState.value.travelExpense ?: TravelAndExpense(),
                    _uiState.value.selectedDate
                )
            } catch (e: Exception) {
                _errorMessage.emit("Failed to refresh data: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun exportToExcel(context: Context) {
        // Placeholder for export functionality
        Toast.makeText(context, "Exporting to Excel...", Toast.LENGTH_SHORT).show()
    }
}
