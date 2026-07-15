package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.Instant

/** A tag/category for where a registry's money moved through, e.g. a bank name. */
data class Source(
    val id: String,
    val name: String,
    /** Swatch key (EntitySwatches); null falls back to a hash-picked swatch. */
    val color: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)
