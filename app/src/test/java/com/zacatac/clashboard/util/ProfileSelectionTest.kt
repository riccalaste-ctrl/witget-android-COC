package com.zacatac.clashboard.util

import com.zacatac.clashboard.data.model.PlayerAccount
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ProfileSelectionTest {

    @Test
    fun storedProfileIsUsedWhenItExists() {
        val older = PlayerAccount(lastImportDate = Instant.fromEpochSeconds(100))
        val newer = PlayerAccount(lastImportDate = Instant.fromEpochSeconds(900))
        assertEquals(older, resolveSelectedProfile(listOf(older, newer), older.id))
    }

    @Test
    fun mostRecentImportIsUsedWhenNothingIsStored() {
        val older = PlayerAccount(lastImportDate = Instant.fromEpochSeconds(100))
        val newer = PlayerAccount(lastImportDate = Instant.fromEpochSeconds(900))
        assertEquals(newer, resolveSelectedProfile(listOf(older, newer), null))
    }

    @Test
    fun deletedStoredProfileFallsBackToMostRecent() {
        val only = PlayerAccount(lastImportDate = Instant.fromEpochSeconds(100))
        assertEquals(only, resolveSelectedProfile(listOf(only), UUID.randomUUID()))
    }

    @Test
    fun noProfilesGivesNull() {
        assertNull(resolveSelectedProfile(emptyList(), null))
    }
}
