package com.rudra.smartworktracker.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Currency
import java.util.Locale

object CurrencyManager {
    // Compose state, so every amount on screen re-renders when the currency is changed in Settings
    private var currentCurrency: String by mutableStateOf("BDT")

    fun init(currencyCode: String) {
        currentCurrency = currencyCode
    }

    fun setCurrency(currencyCode: String) {
        currentCurrency = currencyCode
    }

    fun getCurrencyCode(): String = currentCurrency

    /** "৳1,234.50" / "-৳1,234.50" using the selected currency's symbol. */
    fun format(amount: Double): String = formatWithPattern(amount, "%,.2f")

    /** Same as [format] without decimals: "৳1,235". */
    fun formatWhole(amount: Double): String = formatWithPattern(amount, "%,.0f")

    fun formatCompact(amount: Double): String {
        // Thresholds on the magnitude so large negative amounts are abbreviated too
        val sign = if (amount < 0) "-" else ""
        val abs = kotlin.math.abs(amount)
        val symbol = symbol()
        return when {
            abs >= 1_000_000 -> String.format(Locale.US, "%s%s%.1fM", sign, symbol, abs / 1_000_000)
            abs >= 1_000 -> String.format(Locale.US, "%s%s%.1fK", sign, symbol, abs / 1_000)
            else -> String.format(Locale.US, "%s%s%.0f", sign, symbol, abs)
        }
    }

    fun symbol(): String {
        // Java often renders unfamiliar currencies as their code (e.g. "BDT"), so prefer our table
        SUPPORTED_CURRENCIES.firstOrNull { it.code == currentCurrency }?.let { return it.symbol }
        return try {
            Currency.getInstance(currentCurrency).symbol
        } catch (e: Exception) {
            currentCurrency
        }
    }

    private fun formatWithPattern(amount: Double, pattern: String): String {
        val sign = if (amount < 0) "-" else ""
        return sign + symbol() + String.format(Locale.US, pattern, kotlin.math.abs(amount))
    }
}

val SUPPORTED_CURRENCIES = listOf(
    CurrencyOption("BDT", "Bangladeshi Taka", "৳"),
    CurrencyOption("USD", "US Dollar", "$"),
    CurrencyOption("EUR", "Euro", "€"),
    CurrencyOption("GBP", "British Pound", "£"),
    CurrencyOption("INR", "Indian Rupee", "₹"),
    CurrencyOption("JPY", "Japanese Yen", "¥"),
    CurrencyOption("CAD", "Canadian Dollar", "CA$"),
    CurrencyOption("AUD", "Australian Dollar", "A$"),
    CurrencyOption("SGD", "Singapore Dollar", "S$"),
    CurrencyOption("AED", "UAE Dirham", "د.إ"),
    CurrencyOption("SAR", "Saudi Riyal", "﷼"),
    CurrencyOption("MYR", "Malaysian Ringgit", "RM"),
    CurrencyOption("THB", "Thai Baht", "฿"),
    CurrencyOption("IDR", "Indonesian Rupiah", "Rp"),
    CurrencyOption("PHP", "Philippine Peso", "₱"),
    CurrencyOption("PKR", "Pakistani Rupee", "₨"),
    CurrencyOption("LKR", "Sri Lankan Rupee", "Rs"),
    CurrencyOption("NPR", "Nepalese Rupee", "Rs"),
    CurrencyOption("CNY", "Chinese Yuan", "¥"),
    CurrencyOption("KRW", "South Korean Won", "₩"),
    CurrencyOption("TRY", "Turkish Lira", "₺"),
    CurrencyOption("RUB", "Russian Ruble", "₽"),
    CurrencyOption("BRL", "Brazilian Real", "R$"),
    CurrencyOption("MXN", "Mexican Peso", "MX$"),
    CurrencyOption("ZAR", "South African Rand", "R"),
    CurrencyOption("EGP", "Egyptian Pound", "E£"),
    CurrencyOption("NGN", "Nigerian Naira", "₦"),
    CurrencyOption("KES", "Kenyan Shilling", "KSh"),
    CurrencyOption("GHS", "Ghanaian Cedi", "GH₵"),
    CurrencyOption("CHF", "Swiss Franc", "CHF")
)

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
) {
    val displayName: String get() = "$symbol $code — $name"
}
