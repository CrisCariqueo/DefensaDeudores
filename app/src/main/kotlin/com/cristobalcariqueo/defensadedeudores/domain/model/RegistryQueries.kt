package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.LocalDate

/** A registry plus the display names its table row needs. */
data class RegistryWithNames(
    val registry: Registry,
    val personName: String,
    /** Null for return regs. */
    val sourceName: String?,
)

/** One slice of a circular graph: outstanding (unchecked, normal) debt per debtor or source. */
data class GraphSlice(
    val id: String,
    val label: String,
    val total: Long,
    /** Owning entity's swatch key -- slices follow the entity color. */
    val color: String? = null,
)

/** Outcome of the edit flow (SCOPE.md): same-day edits mutate, older ones supersede. */
sealed interface EditResult {
    data class InPlace(val registryId: String) : EditResult

    /** [rematched] = the corrected amount was re-applied against the original retReg. */
    data class Superseded(val newReg: Registry, val rematched: Boolean) : EditResult
}

/**
 * AND-combinable table filters + plain-text search (matches amount and note) --
 * SCOPE.md Track screen toolbar. Empty/null fields don't filter.
 */
data class RegistryFilter(
    val personIds: Set<String> = emptySet(),
    val sourceIds: Set<String> = emptySet(),
    val type: RegistryType? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val checked: Boolean? = null,
    val amountMin: Int? = null,
    val amountMax: Int? = null,
    val query: String = "",
) {
    val isEmpty: Boolean
        get() = personIds.isEmpty() && sourceIds.isEmpty() && type == null &&
            dateFrom == null && dateTo == null && checked == null &&
            amountMin == null && amountMax == null && query.isBlank()

    /** The sheet-managed filters, ignoring the search box -- drives the filter button's active state. */
    val hasSheetFilters: Boolean
        get() = copy(query = "").isEmpty.not()
}
