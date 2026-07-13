package com.cristobalcariqueo.defensadedeudores.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Swap FontFamily.Default for the pinned family once chosen (SCOPE.md: "close
// to Claude's UI font"). Font-size scaling itself is user-configurable (Config
// screen), applied at the theme layer, not hardcoded per-composable.
val DefensaTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
)
