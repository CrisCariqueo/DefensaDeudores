package com.cristobalcariqueo.defensadedeudores.domain.model

data class Settings(
    val font: String,
    val language: String,
    val darkTheme: Boolean,
    val returnBgColor: String,
    val recentTableSize: Int = 50,
    val historicalTableSize: Int = 100,
    val onboarded: Boolean = false,
)
