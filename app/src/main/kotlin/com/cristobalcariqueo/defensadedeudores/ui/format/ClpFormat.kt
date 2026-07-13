package com.cristobalcariqueo.defensadedeudores.ui.format

import java.text.NumberFormat
import java.util.Locale

private val clpLocale = Locale("es", "CL")

/** CLP has no decimals: "$1.234.567". Sign comes from the caller (returns render negative). */
fun formatClp(amount: Long): String {
    val formatted = NumberFormat.getIntegerInstance(clpLocale).format(amount)
    return if (amount < 0) "-$${formatted.trimStart('-')}" else "$$formatted"
}

fun formatClp(amount: Int): String = formatClp(amount.toLong())
