package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.data.local.dao.SettingsDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SettingsEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(private val dao: SettingsDao) : SettingsRepository {

    /** Missing row (first launch) falls back to defaults without blocking the read. */
    override fun observeSettings(): Flow<Settings> =
        dao.observe().map { entity -> (entity ?: SettingsEntity()).toDomain() }

    override suspend fun update(settings: Settings) {
        dao.seed()
        dao.updateValues(
            font = settings.font,
            language = settings.language,
            darkTheme = settings.darkTheme,
            returnBgColor = settings.returnBgColor,
            recentTableSize = settings.recentTableSize,
            historicalTableSize = settings.historicalTableSize,
        )
    }

    override suspend fun markOnboarded() {
        dao.seed()
        dao.markOnboarded()
    }
}
