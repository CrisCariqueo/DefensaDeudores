package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.Instant

/** A debtor. No uniqueness constraint on [name] -- see DATA_MODEL.md. */
data class Person(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)
