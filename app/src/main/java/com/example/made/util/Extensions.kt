package com.example.made.util

import android.view.View
import android.widget.Toast
import android.content.Context
import java.text.NumberFormat
import java.util.Locale

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Double.toCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(this)
}

fun Double.toCompactCurrency(): String {
    return when {
        this >= 1_000_000 -> String.format("$%.1fM", this / 1_000_000)
        this >= 1_000 -> String.format("$%.1fK", this / 1_000)
        else -> String.format("$%.2f", this)
    }
}
