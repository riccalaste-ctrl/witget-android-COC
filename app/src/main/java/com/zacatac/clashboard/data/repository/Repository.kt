package com.zacatac.clashboard.data.repository

import com.zacatac.clashboard.data.database.AppDatabase
import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class ProfileRepository(private val database: AppDatabase) {
    private val dao = database.playerAccountDao()

    fun getAllProfiles(): Flow<List<PlayerAccount>> = dao.getAllProfiles()

    fun getProfileById(id: UUID): Flow<PlayerAccount?> = dao.getProfileById(id)

    suspend fun insertProfile(profile: PlayerAccount) = dao.insertProfile(profile)

    suspend fun updateProfile(profile: PlayerAccount) = dao.updateProfile(profile)

    suspend fun deleteProfile(id: UUID) = dao.deleteProfile(id)

    suspend fun clearAllProfiles() = dao.clearAllProfiles()
}

class UpgradeRepository(private val database: AppDatabase) {
    // Upgrades are stored within PlayerAccount, so we use ProfileRepository
    // This class can be extended for more complex upgrade-specific queries
}