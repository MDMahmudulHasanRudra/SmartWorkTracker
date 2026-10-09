package com.rudra.smartworktracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.entity.UserProfile
import com.rudra.smartworktracker.data.repository.UserProfileRepository
import com.rudra.smartworktracker.utils.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val profile: UserProfile) : ProfileState()
        data class Error(val message: String) : ProfileState()
        object NotCreated : ProfileState()
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            repository.userProfile.collect { profile ->
                if (profile != null) {
                    _profileState.value = ProfileState.Success(profile)
                } else {
                    _profileState.value = ProfileState.NotCreated
                }
            }
        }
    }

    /** Saves and only then calls [onSaved], so a failed write doesn't navigate away silently. */
    fun saveProfile(profile: UserProfile, onSaved: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveUserProfile(profile.copy(updatedAt = System.currentTimeMillis()))
                onSaved()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save profile")
            }
        }
    }

    class Factory(private val repository: UserProfileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserProfileViewModel(repository) as T
        }
    }
}
