package com.zacatac.clashboard.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zacatac.clashboard.data.model.UpgradeCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object WidgetConfigStore {
    private const val STORE_NAME = "widget_config"

    // Keys for main widget
    private val MAIN_WIDGET_PROFILE_ID = stringPreferencesKey("main_widget_profile_id")
    private val MAIN_WIDGET_AUTO_PROFILE = booleanPreferencesKey("main_widget_auto_profile")

    // Keys for closest upgrade widget
    private val CLOSEST_WIDGET_PROFILE_ID = stringPreferencesKey("closest_widget_profile_id")
    private val CLOSEST_WIDGET_AUTO_PROFILE = booleanPreferencesKey("closest_widget_auto_profile")
    private val CLOSEST_WIDGET_UPGRADE_TYPE = stringPreferencesKey("closest_widget_upgrade_type")

    private val Context.dataStore by preferencesDataStore(STORE_NAME)

    suspend fun getMainWidgetProfileId(context: Context): String? = context.dataStore.data
        .map { it[MAIN_WIDGET_PROFILE_ID] }
        .first()

    suspend fun setMainWidgetProfileId(context: Context, profileId: String?) {
        context.dataStore.edit {
            if (profileId != null) it[MAIN_WIDGET_PROFILE_ID] = profileId else it.remove(MAIN_WIDGET_PROFILE_ID)
        }
    }

    suspend fun getMainWidgetAutoProfile(context: Context): Boolean = context.dataStore.data
        .map { it[MAIN_WIDGET_AUTO_PROFILE] ?: true }
        .first()

    suspend fun setMainWidgetAutoProfile(context: Context, auto: Boolean) {
        context.dataStore.edit { it[MAIN_WIDGET_AUTO_PROFILE] = auto }
    }

    suspend fun getClosestWidgetProfileId(context: Context): String? = context.dataStore.data
        .map { it[CLOSEST_WIDGET_PROFILE_ID] }
        .first()

    suspend fun setClosestWidgetProfileId(context: Context, profileId: String?) {
        context.dataStore.edit {
            if (profileId != null) it[CLOSEST_WIDGET_PROFILE_ID] = profileId else it.remove(CLOSEST_WIDGET_PROFILE_ID)
        }
    }

    suspend fun getClosestWidgetAutoProfile(context: Context): Boolean = context.dataStore.data
        .map { it[CLOSEST_WIDGET_AUTO_PROFILE] ?: true }
        .first()

    suspend fun setClosestWidgetAutoProfile(context: Context, auto: Boolean) {
        context.dataStore.edit { it[CLOSEST_WIDGET_AUTO_PROFILE] = auto }
    }

    suspend fun getClosestWidgetUpgradeType(context: Context): UpgradeCategory = context.dataStore.data
        .map { it[CLOSEST_WIDGET_UPGRADE_TYPE] }
        .map { stored -> UpgradeCategory.entries.firstOrNull { it.value == stored } ?: UpgradeCategory.BUILDER_VILLAGE }
        .first()

    suspend fun setClosestWidgetUpgradeType(context: Context, type: UpgradeCategory) {
        context.dataStore.edit { it[CLOSEST_WIDGET_UPGRADE_TYPE] = type.value }
    }

}
