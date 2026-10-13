package com.zacatac.clashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import com.zacatac.clashboard.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profiles = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selectedProfileId = MutableStateFlow<java.util.UUID?>(null)

    fun refreshProfiles() {
        viewModelScope.launch {
            // Profiles are observed via Flow, no manual refresh needed
        }
    }

    fun addProfile(profile: PlayerAccount) {
        viewModelScope.launch {
            profileRepository.insertProfile(profile)
        }
    }

    fun updateProfile(profile: PlayerAccount) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    fun deleteProfile(id: java.util.UUID) {
        viewModelScope.launch {
            profileRepository.deleteProfile(id)
        }
    }
}