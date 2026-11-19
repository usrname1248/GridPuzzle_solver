package dev.fezojko.solver.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun enumPreferencesKey(name: String): Preferences.Key<String> = stringPreferencesKey(name)

inline fun <reified T : Enum<T>> preferenceEnumFlow(
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<String>,
    defaultValue: T
): Flow<T> {
    return dataStore.data.map { preferences ->
        enumValueOrNull<T>(preferences[key]) ?: defaultValue
    }
}

suspend inline fun <reified T : Enum<T>> updateEnumPreference(
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<String>,
    value: T
) {
    dataStore.edit { preferences ->
        preferences[key] = value.name
    }
}

inline fun <reified T: Enum<T>> enumValueOrNull(stringValue: String?): T? {
    stringValue?: return null
    return try {
        enumValueOf<T>(stringValue)
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: IllegalStateException) {
        null
    }
}
