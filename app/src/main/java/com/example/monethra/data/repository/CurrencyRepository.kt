package com.example.monethra.data.repository

import com.example.monethra.data.dao.CustomCurrencyDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.CustomCurrencyEntity
import com.example.monethra.data.model.CurrencyModel
import com.example.monethra.data.model.GLOBAL_CURRENCY_CATALOG
import com.example.monethra.data.model.WorldCurrency
import com.example.monethra.utils.CurrencyConfig
import com.example.monethra.utils.CurrencyFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CurrencyRepository(
    private val customCurrencyDao: CustomCurrencyDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        const val BASE_CURRENCY_CODE = "IDR"
        private const val API_ENDPOINT = "https://open.er-api.com/v6/latest/IDR"

        val DEFAULT_CURRENCIES = listOf(
            CurrencyModel(code = "IDR", symbol = "Rp", name = "Indonesian Rupiah", rateFromIdr = 1.0, isDefault = true),
            CurrencyModel(code = "USD", symbol = "$", name = "US Dollar", rateFromIdr = 0.000062, isDefault = true),
            CurrencyModel(code = "MYR", symbol = "RM", name = "Malaysian Ringgit", rateFromIdr = 0.00028, isDefault = true),
            CurrencyModel(code = "SGD", symbol = "S$", name = "Singapore Dollar", rateFromIdr = 0.000084, isDefault = true)
        )

        val KNOWN_CURRENCY_METADATA = mapOf(
            "USD" to ("$" to "US Dollar"),
            "EUR" to ("€" to "Euro"),
            "GBP" to ("£" to "British Pound"),
            "JPY" to ("¥" to "Japanese Yen"),
            "CNY" to ("¥" to "Chinese Yuan"),
            "AUD" to ("A$" to "Australian Dollar"),
            "CAD" to ("C$" to "Canadian Dollar"),
            "SGD" to ("S$" to "Singapore Dollar"),
            "MYR" to ("RM" to "Malaysian Ringgit"),
            "THB" to ("฿" to "Thai Baht"),
            "KRW" to ("₩" to "South Korean Won"),
            "INR" to ("₹" to "Indian Rupee"),
            "PHP" to ("₱" to "Philippine Peso"),
            "VND" to ("₫" to "Vietnamese Dong"),
            "CHF" to ("CHF" to "Swiss Franc"),
            "NZD" to ("NZ$" to "New Zealand Dollar"),
            "HKD" to ("HK$" to "Hong Kong Dollar"),
            "BRL" to ("R$" to "Brazilian Real"),
            "RUB" to ("₽" to "Russian Ruble"),
            "SAR" to ("SR" to "Saudi Riyal"),
            "AED" to ("AED" to "UAE Dirham"),
            "TRY" to ("₺" to "Turkish Lira")
        )
    }

    val cachedRates: Flow<Map<String, Double>> = userPreferencesRepository.cachedRatesJson.map { json ->
        parseRatesJson(json)
    }

    val allCurrencies: Flow<List<CurrencyModel>> = combine(
        customCurrencyDao.getAllCustomCurrencies(),
        userPreferencesRepository.cachedRatesJson
    ) { customEntities, cachedJson ->
        val cachedRatesMap = parseRatesJson(cachedJson)
        val defaultList = DEFAULT_CURRENCIES.map { def ->
            val liveRate = cachedRatesMap[def.code]
            if (liveRate != null && liveRate > 0.0) {
                def.copy(rateFromIdr = liveRate)
            } else {
                def
            }
        }

        val customList = customEntities.map { entity ->
            val liveRate = cachedRatesMap[entity.code] ?: entity.rateFromIdr
            CurrencyModel(
                code = entity.code,
                symbol = entity.symbol,
                name = entity.name,
                rateFromIdr = liveRate,
                isDefault = false
            )
        }

        defaultList + customList
    }

    val activeCurrency: Flow<CurrencyModel> = combine(
        userPreferencesRepository.selectedCurrencyCode,
        allCurrencies
    ) { selectedCode, currencies ->
        currencies.find { it.code.equals(selectedCode, ignoreCase = true) }
            ?: DEFAULT_CURRENCIES.first()
    }

    suspend fun selectCurrency(currency: CurrencyModel) {
        userPreferencesRepository.setSelectedCurrency(
            currency.code,
            currency.symbol,
            currency.rateFromIdr
        )
        CurrencyFormatter.currentConfig = CurrencyConfig(
            currencyCode = currency.code,
            symbol = currency.symbol,
            rateFromIdr = currency.rateFromIdr
        )
    }

    suspend fun addCustomCurrency(
        code: String,
        symbol: String,
        name: String,
        rateFromIdr: Double
    ) {
        val uppercaseCode = code.trim().uppercase()
        val entity = CustomCurrencyEntity(
            code = uppercaseCode,
            symbol = symbol.trim(),
            name = name.trim(),
            rateFromIdr = rateFromIdr
        )
        customCurrencyDao.insertCustomCurrency(entity)
    }

    suspend fun addCurrencyFromCatalog(worldCurrency: WorldCurrency): CurrencyModel {
        val rate = resolveRateForCurrency(worldCurrency.code, worldCurrency.fallbackRateFromIdr)
        val entity = CustomCurrencyEntity(
            code = worldCurrency.code,
            symbol = worldCurrency.symbol,
            name = worldCurrency.name,
            rateFromIdr = rate
        )
        customCurrencyDao.insertCustomCurrency(entity)
        return CurrencyModel(
            code = entity.code,
            symbol = entity.symbol,
            name = entity.name,
            rateFromIdr = rate,
            isDefault = false
        )
    }

    fun getAvailableWorldCurrencies(existingCodes: Set<String>): List<WorldCurrency> {
        val excluded = (DEFAULT_CURRENCIES.map { it.code } + existingCodes)
            .map { it.uppercase() }
            .toSet()
        return GLOBAL_CURRENCY_CATALOG.filter { it.code.uppercase() !in excluded }
    }

    suspend fun resolveRateForCurrency(code: String, fallbackRate: Double): Double = withContext(ioDispatcher) {
        val upper = code.trim().uppercase()
        val cached = parseRatesJson(userPreferencesRepository.cachedRatesJson.first())
        val cachedRate = cached[upper]
        if (cachedRate != null && cachedRate > 0.0) {
            return@withContext cachedRate
        }
        val live = fetchLiveRates()
        live[upper]?.takeIf { it > 0.0 } ?: fallbackRate
    }

    suspend fun deleteCustomCurrency(code: String) {
        val uppercaseCode = code.trim().uppercase()
        customCurrencyDao.deleteCustomCurrencyByCode(uppercaseCode)
        val currentCode = userPreferencesRepository.selectedCurrencyCode.first()
        if (currentCode.equals(uppercaseCode, ignoreCase = true)) {
            val base = DEFAULT_CURRENCIES.first()
            selectCurrency(base)
        }
    }

    suspend fun fetchLiveRates(): Map<String, Double> = withContext(ioDispatcher) {
        try {
            val url = URL(API_ENDPOINT)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val jsonObject = JSONObject(response)
                val ratesObj = jsonObject.optJSONObject("rates")
                if (ratesObj != null) {
                    val map = mutableMapOf<String, Double>()
                    val keys = ratesObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = ratesObj.optDouble(key, 0.0)
                    }

                    userPreferencesRepository.setCachedRatesJson(ratesObj.toString())

                    // 1. Silently update all user-added currencies in Room
                    try {
                        val existingCustom = customCurrencyDao.getAllCustomCurrencies().first()
                        for (custom in existingCustom) {
                            val updatedRate = map[custom.code]
                            if (updatedRate != null && updatedRate > 0.0 && updatedRate != custom.rateFromIdr) {
                                customCurrencyDao.insertCustomCurrency(custom.copy(rateFromIdr = updatedRate))
                            }
                        }
                    } catch (_: Exception) {}

                    // 2. If active currency is in fetched rates, update active rate
                    val activeCode = userPreferencesRepository.selectedCurrencyCode.first()
                    val newRate = map[activeCode]
                    if (newRate != null && newRate > 0.0) {
                        val currentSymbol = userPreferencesRepository.currencySymbol.first()
                        userPreferencesRepository.setSelectedCurrency(activeCode, currentSymbol, newRate)
                        CurrencyFormatter.currentConfig = CurrencyConfig(activeCode, currentSymbol, newRate)
                    }

                    return@withContext map
                }
            }
        } catch (_: Exception) {
            // Network failure / offline: fall back gracefully
        }

        val cached = userPreferencesRepository.cachedRatesJson.first()
        parseRatesJson(cached)
    }

    suspend fun fetchRateForCode(code: String): Double? = withContext(ioDispatcher) {
        val upper = code.trim().uppercase()
        val cached = parseRatesJson(userPreferencesRepository.cachedRatesJson.first())
        cached[upper]?.takeIf { it > 0.0 } ?: run {
            val live = fetchLiveRates()
            live[upper]?.takeIf { it > 0.0 }
        }
    }

    fun getSuggestedMetadata(code: String): Pair<String, String>? {
        return KNOWN_CURRENCY_METADATA[code.trim().uppercase()]
    }

    private fun parseRatesJson(json: String): Map<String, Double> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optDouble(key, 0.0)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
