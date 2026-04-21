package com.example.made.util

import android.view.View
import android.widget.Toast
import android.content.Context
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

private val inrLocale: Locale = Locale.forLanguageTag("en-IN")
private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

fun Double.toCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(inrLocale)
    return format.format(this)
}

fun Double.toGroupedNumber(maxFractionDigits: Int = 2): String {
    val format = NumberFormat.getNumberInstance(inrLocale)
    format.maximumFractionDigits = maxFractionDigits
    format.minimumFractionDigits = 0
    return format.format(this)
}

fun String?.toAmountOrZero(): Double {
    if (this.isNullOrBlank()) return 0.0
    val normalized = this
        .replace("₹", "")
        .replace(",", "")
        .trim()
    return normalized.toDoubleOrNull() ?: 0.0
}

fun String?.toAmountOrNull(): Double? {
    if (this.isNullOrBlank()) return null
    val normalized = this
        .replace("₹", "")
        .replace(",", "")
        .trim()
    return normalized.toDoubleOrNull()
}

fun Double.toCompactCurrency(): String {
    return when {
        this >= 1_000_000 -> String.format(inrLocale, "₹%.1fM", this / 1_000_000)
        this >= 1_000 -> String.format(inrLocale, "₹%.1fK", this / 1_000)
        else -> String.format(inrLocale, "₹%.2f", this)
    }
}

fun String.toDisplayDateOrSelf(): String {
    val value = trim()
    if (value.isEmpty()) return value

    val parsed = parseFlexibleDate(value) ?: return value
    return parsed.format(displayDateFormatter)
}

fun String.toStorageIsoDateOrSelf(): String {
    val value = trim()
    if (value.isEmpty()) return value

    val parsed = parseFlexibleDate(value) ?: return value
    return parsed.toString()
}

fun parseFlexibleDate(raw: String): LocalDate? {
    val value = raw.trim()
    if (value.isEmpty()) return null

    val direct = runCatching {
        when {
            value.length >= 10 && value[4] == '-' && value[7] == '-' -> LocalDate.parse(value.take(10))
            value.length >= 10 && value[2] == '-' && value[5] == '-' -> LocalDate.parse(value.take(10), displayDateFormatter)
            else -> null
        }
    }.getOrNull()
    if (direct != null) return direct

    val day = value.toIntOrNull() ?: return null
    val month = YearMonth.now()
    return if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
}
