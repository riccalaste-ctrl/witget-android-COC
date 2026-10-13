package com.zacatac.clashboard.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerAccountDao {
    @Query("SELECT * FROM player_accounts ORDER BY displayName ASC")
    fun getAllProfiles(): Flow<List<PlayerAccount>>

    @Query("SELECT * FROM player_accounts WHERE id = :id")
    fun getProfileById(id: java.util.UUID): Flow<PlayerAccount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlayerAccount)

    @Update
    suspend fun updateProfile(profile: PlayerAccount)

    @Query("DELETE FROM player_accounts WHERE id = :id")
    suspend fun deleteProfile(id: java.util.UUID)

    @Query("DELETE FROM player_accounts")
    suspend fun clearAllProfiles()
}