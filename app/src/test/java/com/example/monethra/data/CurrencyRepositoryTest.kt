package com.example.monethra.data

import com.example.monethra.data.dao.CustomCurrencyDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.CustomCurrencyEntity
import com.example.monethra.data.model.CurrencyModel
import com.example.monethra.data.repository.CurrencyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCustomCurrencyDao : CustomCurrencyDao {
        val list = MutableStateFlow<List<CustomCurrencyEntity>>(emptyList())

        override fun getAllCustomCurrencies(): Flow<List<CustomCurrencyEntity>> = list

        override suspend fun insertCustomCurrency(currency: CustomCurrencyEntity) {
            val current = list.value.toMutableList()
            current.removeAll { it.code == currency.code }
            current.add(currency)
            list.value = current
        }

        override suspend fun deleteCustomCurrency(currency: CustomCurrencyEntity) {
            val current = list.value.toMutableList()
            current.removeAll { it.code == currency.code }
            list.value = current
        }

        override suspend fun deleteCustomCurrencyByCode(code: String) {
            val current = list.value.toMutableList()
            current.removeAll { it.code == code }
            list.value = current
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val selectedCodeFlow = MutableStateFlow("IDR")
        val symbolFlow = MutableStateFlow("Rp")
        val rateFlow = MutableStateFlow(1.0)
        val cachedRatesFlow = MutableStateFlow("")

        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isLoggedIn: Flow<Boolean> = flowOf(true)
        override val activeUserId: Flow<Long> = flowOf(1L)
        override val sessionToken: Flow<String?> = flowOf(null)
        override val userNamePreference: Flow<String?> = flowOf("User")
        override val userEmailPreference: Flow<String?> = flowOf("user@example.com")
        override val currencySymbol: Flow<String> = symbolFlow
        override val selectedCurrencyCode: Flow<String> = selectedCodeFlow
        override val currencyRateFromIdr: Flow<Double> = rateFlow
        override val cachedRatesJson: Flow<String> = cachedRatesFlow
        override val themePreference: Flow<String> = flowOf("SYSTEM")
        override val notificationsEnabled: Flow<Boolean> = flowOf(true)
        override val paydayStartDay: Flow<Int> = flowOf(1)
        override val lastProcessedPaydayCycle: Flow<String> = flowOf("")
        override val lastActiveTimestamp: Flow<Long> = flowOf(0L)

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {}
        override suspend fun clearSession() {}
        override suspend fun updateLastActiveTimestamp(timestamp: Long) {}
        override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean = false
        override suspend fun setCurrencySymbol(symbol: String) {
            symbolFlow.value = symbol
        }
        override suspend fun setSelectedCurrency(code: String, symbol: String, rateFromIdr: Double) {
            selectedCodeFlow.value = code
            symbolFlow.value = symbol
            rateFlow.value = rateFromIdr
        }
        override suspend fun setCachedRatesJson(json: String) {
            cachedRatesFlow.value = json
        }
        override suspend fun setThemePreference(theme: String) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setPaydayStartDay(day: Int) {}
        override suspend fun setLastProcessedPaydayCycle(cycleKey: String) {}
        override suspend fun clearPreferences() {}
    }

    private lateinit var dao: FakeCustomCurrencyDao
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var repository: CurrencyRepository

    @Before
    fun setUp() {
        dao = FakeCustomCurrencyDao()
        prefs = FakeUserPreferencesRepository()
        repository = CurrencyRepository(
            customCurrencyDao = dao,
            userPreferencesRepository = prefs,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun allCurrencies_containsFourDefaultCurrencies() = runTest(testDispatcher) {
        val currencies = repository.allCurrencies.first()
        assertEquals(4, currencies.size)
        val codes = currencies.map { it.code }
        assertTrue(codes.contains("IDR"))
        assertTrue(codes.contains("USD"))
        assertTrue(codes.contains("MYR"))
        assertTrue(codes.contains("SGD"))

        val idr = currencies.first { it.code == "IDR" }
        assertEquals("Rp", idr.symbol)
        assertEquals(1.0, idr.rateFromIdr, 0.000001)
        assertTrue(idr.isDefault)
    }

    @Test
    fun addCustomCurrency_isAppendedToCurrenciesList() = runTest(testDispatcher) {
        repository.addCustomCurrency(
            code = "JPY",
            symbol = "¥",
            name = "Japanese Yen",
            rateFromIdr = 0.0096
        )

        val currencies = repository.allCurrencies.first()
        assertEquals(5, currencies.size)

        val jpy = currencies.find { it.code == "JPY" }
        assertNotNull(jpy)
        assertEquals("¥", jpy!!.symbol)
        assertEquals("Japanese Yen", jpy.name)
        assertEquals(0.0096, jpy.rateFromIdr, 0.000001)
        assertFalse(jpy.isDefault)
    }

    @Test
    fun deleteCustomCurrency_removesCurrency_andResetsToIdrIfActive() = runTest(testDispatcher) {
        repository.addCustomCurrency(
            code = "EUR",
            symbol = "€",
            name = "Euro",
            rateFromIdr = 0.000058
        )

        val eur = repository.allCurrencies.first().first { it.code == "EUR" }
        repository.selectCurrency(eur)
        assertEquals("EUR", prefs.selectedCodeFlow.value)

        repository.deleteCustomCurrency("EUR")

        val currencies = repository.allCurrencies.first()
        assertFalse(currencies.any { it.code == "EUR" })
        assertEquals("IDR", prefs.selectedCodeFlow.value)
        assertEquals("Rp", prefs.symbolFlow.value)
    }

    @Test
    fun getSuggestedMetadata_returnsExpectedValues() {
        val jpyMeta = repository.getSuggestedMetadata("jpy")
        assertNotNull(jpyMeta)
        assertEquals("¥", jpyMeta!!.first)
        assertEquals("Japanese Yen", jpyMeta.second)

        val eurMeta = repository.getSuggestedMetadata("EUR")
        assertNotNull(eurMeta)
        assertEquals("€", eurMeta!!.first)
        assertEquals("Euro", eurMeta.second)
    }
}
