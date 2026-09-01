package com.example.gamepadmapper.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gamepadmapper.model.InjectionEngine
import com.example.gamepadmapper.model.MappingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mappingDataStore by preferencesDataStore(name = "mapping_preferences")

class MappingRepository(private val context: Context) {
    private object Keys {
        val engine = stringPreferencesKey("engine")
        val overlayEnabled = booleanPreferencesKey("overlay_enabled")
        val deadZone = floatPreferencesKey("dead_zone")
        val sensitivity = floatPreferencesKey("sensitivity")
    }

    val config: Flow<MappingConfig> = context.mappingDataStore.data.map { preferences ->
        MappingConfig(
            engine = preferences[Keys.engine]
                ?.let { value -> runCatching { InjectionEngine.valueOf(value) }.getOrNull() }
                ?: InjectionEngine.TOUCH,
            overlayEnabled = preferences[Keys.overlayEnabled] ?: true,
            deadZone = preferences[Keys.deadZone] ?: 0.12f,
            sensitivity = preferences[Keys.sensitivity] ?: 1.0f
        )
    }

    suspend fun setEngine(engine: InjectionEngine) {
        context.mappingDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[Keys.engine] = engine.name
            }
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.mappingDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[Keys.overlayEnabled] = enabled
            }
        }
    }

    suspend fun setSensitivity(value: Float) {
        context.mappingDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[Keys.sensitivity] = value.coerceIn(0.25f, 2.0f)
            }
        }
    }
}
