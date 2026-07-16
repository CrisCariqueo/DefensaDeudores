package com.cristobalcariqueo.defensadedeudores.data.remote

import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.RegistryEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SettingsEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackSourceEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PostgREST row shapes (snake_case, timestamptz as ISO-8601 strings). Local
 * entities carry epoch millis; conversion happens in these mappers. Pulled
 * rows land locally with dirty = 0 and remote_updated_at = the server's
 * updated_at (the conflict base).
 */

private fun String.toEpochMs(): Long = Instant.parse(this).toEpochMilliseconds()
private fun Long.toIso(): String = Instant.fromEpochMilliseconds(this).toString()

@Serializable
data class TrackDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

fun TrackDto.toEntity() = TrackEntity(
    id = id,
    name = name,
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
    deletedAt = deletedAt?.toEpochMs(),
    dirty = false,
    remoteUpdatedAt = updatedAt.toEpochMs(),
)

fun TrackEntity.toDto(userId: String) = TrackDto(
    id = id,
    userId = userId,
    name = name,
    createdAt = createdAt.toIso(),
    updatedAt = updatedAt.toIso(),
    deletedAt = deletedAt?.toIso(),
)

@Serializable
data class PersonDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val color: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

fun PersonDto.toEntity() = PersonEntity(
    id = id,
    name = name,
    color = color,
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
    deletedAt = deletedAt?.toEpochMs(),
    dirty = false,
    remoteUpdatedAt = updatedAt.toEpochMs(),
)

fun PersonEntity.toDto(userId: String) = PersonDto(
    id = id,
    userId = userId,
    name = name,
    color = color,
    createdAt = createdAt.toIso(),
    updatedAt = updatedAt.toIso(),
    deletedAt = deletedAt?.toIso(),
)

@Serializable
data class SourceDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val color: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

fun SourceDto.toEntity() = SourceEntity(
    id = id,
    name = name,
    color = color,
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
    deletedAt = deletedAt?.toEpochMs(),
    dirty = false,
    remoteUpdatedAt = updatedAt.toEpochMs(),
)

fun SourceEntity.toDto(userId: String) = SourceDto(
    id = id,
    userId = userId,
    name = name,
    color = color,
    createdAt = createdAt.toIso(),
    updatedAt = updatedAt.toIso(),
    deletedAt = deletedAt?.toIso(),
)

@Serializable
data class RegistryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("track_id") val trackId: String,
    @SerialName("person_id") val personId: String,
    @SerialName("source_id") val sourceId: String? = null,
    val amount: Int,
    val type: Int,
    val checked: Boolean,
    val note: String? = null,
    val date: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("supersedes_id") val supersedesId: String? = null,
    @SerialName("matched_retreg_id") val matchedRetRegId: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

fun RegistryDto.toEntity() = RegistryEntity(
    id = id,
    trackId = trackId,
    personId = personId,
    sourceId = sourceId,
    amount = amount,
    type = type,
    checked = checked,
    note = note,
    date = LocalDate.parse(date).toEpochDays(),
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
    supersedesId = supersedesId,
    matchedRetRegId = matchedRetRegId,
    deletedAt = deletedAt?.toEpochMs(),
    dirty = false,
    remoteUpdatedAt = updatedAt.toEpochMs(),
)

fun RegistryEntity.toDto(userId: String) = RegistryDto(
    id = id,
    userId = userId,
    trackId = trackId,
    personId = personId,
    sourceId = sourceId,
    amount = amount,
    type = type,
    checked = checked,
    note = note,
    date = LocalDate.fromEpochDays(date).toString(),
    createdAt = createdAt.toIso(),
    updatedAt = updatedAt.toIso(),
    supersedesId = supersedesId,
    matchedRetRegId = matchedRetRegId,
    deletedAt = deletedAt?.toIso(),
)

@Serializable
data class TrackPersonDto(
    @SerialName("track_id") val trackId: String,
    @SerialName("person_id") val personId: String,
)

fun TrackPersonDto.toEntity() = TrackPersonEntity(trackId = trackId, personId = personId)

fun TrackPersonEntity.toDto() = TrackPersonDto(trackId = trackId, personId = personId)

@Serializable
data class TrackSourceDto(
    @SerialName("track_id") val trackId: String,
    @SerialName("source_id") val sourceId: String,
)

fun TrackSourceDto.toEntity() = TrackSourceEntity(trackId = trackId, sourceId = sourceId)

fun TrackSourceEntity.toDto() = TrackSourceDto(trackId = trackId, sourceId = sourceId)

@Serializable
data class SettingsDto(
    @SerialName("user_id") val userId: String,
    val font: String,
    val language: String,
    val theme: String = "system",
    @SerialName("return_bg_color") val returnBgColor: String,
    @SerialName("recent_table_size") val recentTableSize: Int,
    @SerialName("historical_table_size") val historicalTableSize: Int,
    val onboarded: Boolean,
    @SerialName("updated_at") val updatedAt: String,
)

fun SettingsDto.toEntity() = SettingsEntity(
    font = font,
    language = language,
    theme = theme,
    returnBgColor = returnBgColor,
    recentTableSize = recentTableSize,
    historicalTableSize = historicalTableSize,
    onboarded = onboarded,
    dirty = false,
    remoteUpdatedAt = updatedAt.toEpochMs(),
)

fun SettingsEntity.toDto(userId: String, updatedAtIso: String) = SettingsDto(
    userId = userId,
    font = font,
    language = language,
    theme = theme,
    returnBgColor = returnBgColor,
    recentTableSize = recentTableSize,
    historicalTableSize = historicalTableSize,
    onboarded = onboarded,
    updatedAt = updatedAtIso,
)
