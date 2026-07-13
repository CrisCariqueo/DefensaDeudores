package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

enum class RegistryType {
    NORMAL,
    RETURN,
    SUPERSEDED,
}

/**
 * A single ledger entry. See DATA_MODEL.md for the full field-by-field
 * rationale, in particular the supersede (edit) flow and retReg matching.
 */
data class Registry(
    val id: String,
    val trackId: String,
    val personId: String,
    val sourceId: String,
    /** CLP, always positive; [type] carries the sign at display/sum time. */
    val amount: Int,
    val type: RegistryType,
    /** Normal reg: fully returned. RetReg: fully consumed. */
    val checked: Boolean,
    val note: String?,
    val date: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Set on the new row when it replaces an older one (edit-after-today flow). */
    val supersedesId: String? = null,
    /** Normal reg only -- the retReg that checked it. Needed to undo on supersede. */
    val matchedRetRegId: String? = null,
    val deletedAt: Instant? = null,
)
