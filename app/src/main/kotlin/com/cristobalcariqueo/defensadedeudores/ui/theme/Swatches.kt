package com.cristobalcariqueo.defensadedeudores.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Fixed swatch set for debtor/source identity colors. Both variants validated
 * with the dataviz palette checker against the app surfaces (#FBFDF7 light,
 * #1A1C19 dark): lightness band, chroma floor, adjacent-pair CVD separation
 * and >= 3:1 contrast all pass. Identity never rides on color alone -- chips
 * and legends always carry the name.
 */
data class EntitySwatch(val key: String, val light: Color, val dark: Color)

val EntitySwatches = listOf(
    EntitySwatch("blue", Color(0xFF1D6FE0), Color(0xFF4C8DF0)),
    EntitySwatch("orange", Color(0xFFE8590C), Color(0xFFE5672B)),
    EntitySwatch("teal", Color(0xFF00A37A), Color(0xFF0CA678)),
    EntitySwatch("red", Color(0xFFE03131), Color(0xFFF25555)),
    EntitySwatch("purple", Color(0xFF6C3FD8), Color(0xFF9775FA)),
    EntitySwatch("lime", Color(0xFF5C940D), Color(0xFF66A80F)),
    EntitySwatch("pink", Color(0xFFD6336C), Color(0xFFE64980)),
    EntitySwatch("cyan", Color(0xFF0B96B3), Color(0xFF0F97AA)),
    EntitySwatch("amber", Color(0xFFD97706), Color(0xFFC77908)),
    EntitySwatch("indigo", Color(0xFF4263EB), Color(0xFF5C7CFA)),
    EntitySwatch("green", Color(0xFF008300), Color(0xFF2F9E44)),
    EntitySwatch("magenta", Color(0xFFBE4BDB), Color(0xFFC65DE0)),
)

private val byKey = EntitySwatches.associateBy { it.key }

/** Resolves a stored swatch key; unknown/null keys fall back deterministically by hashing [fallbackSeed]. */
fun swatchColor(key: String?, dark: Boolean, fallbackSeed: String = ""): Color {
    val swatch = byKey[key] ?: fallbackSwatch(fallbackSeed)
    return if (dark) swatch.dark else swatch.light
}

/** Stable fallback for rows created before colors existed (or synced from an older app). */
private fun fallbackSwatch(seed: String): EntitySwatch =
    EntitySwatches[(seed.hashCode().mod(EntitySwatches.size))]

/** Least-used swatch key -- the suggested preselection when creating an entity. */
fun nextSwatchKey(usedKeys: List<String?>): String {
    val counts = usedKeys.filterNotNull().groupingBy { it }.eachCount()
    return EntitySwatches.minByOrNull { counts[it.key] ?: 0 }?.key ?: EntitySwatches.first().key
}
