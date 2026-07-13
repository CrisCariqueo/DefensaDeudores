package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.Instant

data class Track(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)
