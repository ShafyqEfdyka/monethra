package com.example.monethra.data.model

data class CurrencyModel(
    val code: String,          // 3-letter ISO currency code, e.g. "IDR", "USD", "MYR", "SGD"
    val symbol: String,        // e.g. "Rp", "$", "RM", "S$"
    val name: String,          // e.g. "Indonesian Rupiah", "US Dollar"
    val rateFromIdr: Double,   // Exchange rate relative to 1 IDR (1 IDR = rateFromIdr in this currency)
    val isDefault: Boolean = false // True for built-in IDR, USD, MYR, SGD
) {
    val rateToIdr: Double
        get() = if (rateFromIdr > 0.0) 1.0 / rateFromIdr else 1.0
}

data class WorldCurrency(
    val code: String,          // 3-letter ISO code (e.g. "EUR", "JPY")
    val symbol: String,        // Currency symbol (e.g. "€", "¥")
    val name: String,          // Official English name (e.g. "Euro", "Japanese Yen")
    val fallbackRateFromIdr: Double // Sane offline fallback rate relative to 1 IDR
)

val GLOBAL_CURRENCY_CATALOG = listOf(
    WorldCurrency(code = "JPY", symbol = "¥", name = "Japanese Yen", fallbackRateFromIdr = 0.0097),
    WorldCurrency(code = "EUR", symbol = "€", name = "Euro", fallbackRateFromIdr = 0.000057),
    WorldCurrency(code = "GBP", symbol = "£", name = "British Pound", fallbackRateFromIdr = 0.000049),
    WorldCurrency(code = "AUD", symbol = "A$", name = "Australian Dollar", fallbackRateFromIdr = 0.000095),
    WorldCurrency(code = "CAD", symbol = "C$", name = "Canadian Dollar", fallbackRateFromIdr = 0.000085),
    WorldCurrency(code = "CHF", symbol = "CHF", name = "Swiss Franc", fallbackRateFromIdr = 0.000055),
    WorldCurrency(code = "CNY", symbol = "¥", name = "Chinese Yuan", fallbackRateFromIdr = 0.00044),
    WorldCurrency(code = "KRW", symbol = "₩", name = "South Korean Won", fallbackRateFromIdr = 0.086),
    WorldCurrency(code = "INR", symbol = "₹", name = "Indian Rupee", fallbackRateFromIdr = 0.0052),
    WorldCurrency(code = "THB", symbol = "฿", name = "Thai Baht", fallbackRateFromIdr = 0.0022),
    WorldCurrency(code = "PHP", symbol = "₱", name = "Philippine Peso", fallbackRateFromIdr = 0.0035),
    WorldCurrency(code = "VND", symbol = "₫", name = "Vietnamese Dong", fallbackRateFromIdr = 1.55),
    WorldCurrency(code = "SAR", symbol = "﷼", name = "Saudi Riyal", fallbackRateFromIdr = 0.00023),
    WorldCurrency(code = "AED", symbol = "د.إ", name = "UAE Dirham", fallbackRateFromIdr = 0.00023),
    WorldCurrency(code = "NZD", symbol = "NZ$", name = "New Zealand Dollar", fallbackRateFromIdr = 0.0010),
    WorldCurrency(code = "HKD", symbol = "HK$", name = "Hong Kong Dollar", fallbackRateFromIdr = 0.00048),
    WorldCurrency(code = "TWD", symbol = "NT$", name = "New Taiwan Dollar", fallbackRateFromIdr = 0.0020),
    WorldCurrency(code = "BRL", symbol = "R$", name = "Brazilian Real", fallbackRateFromIdr = 0.00035),
    WorldCurrency(code = "TRY", symbol = "₺", name = "Turkish Lira", fallbackRateFromIdr = 0.0021),
    WorldCurrency(code = "RUB", symbol = "₽", name = "Russian Ruble", fallbackRateFromIdr = 0.0055),
    WorldCurrency(code = "ZAR", symbol = "R", name = "South African Rand", fallbackRateFromIdr = 0.0011),
    WorldCurrency(code = "MXN", symbol = "Mex$", name = "Mexican Peso", fallbackRateFromIdr = 0.0012),
    WorldCurrency(code = "KWD", symbol = "KD", name = "Kuwaiti Dinar", fallbackRateFromIdr = 0.000019),
    WorldCurrency(code = "QAR", symbol = "QR", name = "Qatari Riyal", fallbackRateFromIdr = 0.00023),
    WorldCurrency(code = "BND", symbol = "B$", name = "Brunei Dollar", fallbackRateFromIdr = 0.000084),
    WorldCurrency(code = "NOK", symbol = "kr", name = "Norwegian Krone", fallbackRateFromIdr = 0.00067),
    WorldCurrency(code = "SEK", symbol = "kr", name = "Swedish Krona", fallbackRateFromIdr = 0.00065),
    WorldCurrency(code = "DKK", symbol = "kr", name = "Danish Krone", fallbackRateFromIdr = 0.00043),
    WorldCurrency(code = "PLN", symbol = "zł", name = "Polish Zloty", fallbackRateFromIdr = 0.00025)
)
