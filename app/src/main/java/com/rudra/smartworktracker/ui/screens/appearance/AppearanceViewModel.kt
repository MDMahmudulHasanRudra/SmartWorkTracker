package com.rudra.smartworktracker.ui.screens.appearance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.repository.SettingsRepository
import com.rudra.smartworktracker.data.repository.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: Float = 1.0f,
    val accentColorIndex: Int = 0,
    val isDynamicColor: Boolean = true
)

class AppearanceViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppearanceUiState())
    val uiState: StateFlow<AppearanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.themeMode,
                settingsRepository.fontSize,
                settingsRepository.accentColor,
                settingsRepository.dynamicColor
            ) { mode, size, color, dynamic ->
                AppearanceUiState(
                    themeMode = mode,
                    fontSize = size.toFloat(),
                    accentColorIndex = color,
                    isDynamicColor = dynamic
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(size.toDouble()) }
    }

    fun setAccentColor(index: Int) {
        viewModelScope.launch { settingsRepository.setAccentColor(index) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppearanceViewModel(SettingsRepository(context)) as T
        }
    }
}
