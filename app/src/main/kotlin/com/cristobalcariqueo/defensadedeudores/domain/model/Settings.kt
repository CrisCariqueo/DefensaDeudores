package com.cristobalcariqueo.defensadedeudores.domain.model

data class Settings(
    val font: String,
    val language: String,
    /** 'system', 'dark' or 'light'. */
    val theme: String,
    val returnBgColor: String,
    val recentTableSize: Int = 50,
    val historicalTableSize: Int = 100,
    val onboarded: Boolean = false,
)
