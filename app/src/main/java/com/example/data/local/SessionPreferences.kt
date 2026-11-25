package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AuthProviderType
import com.example.model.JenjangPramuka
import com.example.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "my_scout_session")

class SessionPreferences(private val context: Context) {

    private object Keys {
        val SESSION_ID = stringPreferencesKey("session_id")
        val AUTH_PROVIDER = stringPreferencesKey("auth_provider")
        val FULL_NAME = stringPreferencesKey("full_name")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val JENJANG = stringPreferencesKey("jenjang")
        val HAS_SELECTED_JENJANG = booleanPreferencesKey("has_selected_jenjang")
        val INSTITUTION = stringPreferencesKey("institution")
        val GUDEP_NUMBER = stringPreferencesKey("gudep_number")
        val UNIT_NAME = stringPreferencesKey("unit_name")
        val XP = longPreferencesKey("xp")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val activeSessionFlow: Flow<UserSession?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[Keys.SESSION_ID] ?: return@map null
        if (id.isBlank()) return@map null
        val provider = if (prefs[Keys.AUTH_PROVIDER] == AuthProviderType.GOOGLE.name) {
            AuthProviderType.GOOGLE
        } else {
            AuthProviderType.MANUAL
        }
        UserSession(
            sessionId = id,
            authProvider = provider,
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            username = prefs[Keys.USERNAME].orEmpty(),
            email = prefs[Keys.EMAIL].orEmpty(),
            jenjang = JenjangPramuka.fromLabel(prefs[Keys.JENJANG]),
            hasCompletedJenjangSelection = prefs[Keys.HAS_SELECTED_JENJANG] ?: true,
            institutionName = prefs[Keys.INSTITUTION].orEmpty(),
            gudepNumber = prefs[Keys.GUDEP_NUMBER].orEmpty(),
            unitName = prefs[Keys.UNIT_NAME].orEmpty(),
            xp = (prefs[Keys.XP] ?: 0L).toInt(),
            lastSyncedMillis = prefs[Keys.LAST_SYNC] ?: 0L
        )
    }

    suspend fun saveSession(session: UserSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.SESSION_ID] = session.sessionId
            prefs[Keys.AUTH_PROVIDER] = session.authProvider.name
            prefs[Keys.FULL_NAME] = session.fullName
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.EMAIL] = session.email
            prefs[Keys.JENJANG] = session.jenjang.label
            prefs[Keys.HAS_SELECTED_JENJANG] = session.hasCompletedJenjangSelection
            prefs[Keys.INSTITUTION] = session.institutionName
            prefs[Keys.GUDEP_NUMBER] = session.gudepNumber
            prefs[Keys.UNIT_NAME] = session.unitName
            prefs[Keys.XP] = session.xp.toLong()
            prefs[Keys.LAST_SYNC] = session.lastSyncedMillis
        }
    }

    suspend fun updateLastSynced(timestampMillis: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC] = timestampMillis
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }
}
