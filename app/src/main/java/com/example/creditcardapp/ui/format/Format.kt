package com.example.creditcardapp.ui.format

import java.text.NumberFormat
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

fun Double.asCurrency(): String = currencyFormat.format(this)

fun maskedNumber(last4: String): String = "•••• •••• •••• $last4"

fun expiry(month: Int, year: Int): String = "%02d/%02d".format(month, year % 100)
