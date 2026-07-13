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
)

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
    val query: String = "",
) {
    val isEmpty: Boolean
        get() = personIds.isEmpty() && sourceIds.isEmpty() && type == null &&
            dateFrom == null && dateTo == null && checked == null && query.isBlank()
}
