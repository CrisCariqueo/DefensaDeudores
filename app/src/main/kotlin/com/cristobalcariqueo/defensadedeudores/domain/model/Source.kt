package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.Instant

/** A tag/category for where a registry's money moved through, e.g. a bank name. */
data class Source(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)
