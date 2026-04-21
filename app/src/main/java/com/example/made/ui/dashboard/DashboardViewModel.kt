package com.example.made.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.made.data.model.BillLedgerEntry
import com.example.made.data.model.Tenant
import com.example.made.data.repository.TenantRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class DashboardViewModel : ViewModel() {
    private val tenantRepository = TenantRepository()
    private val _tenants = MutableLiveData<List<Tenant>>()
    val tenants: LiveData<List<Tenant>> = _tenants
    private val _totalExpected = MutableLiveData<Double>()
    val totalExpected: LiveData<Double> = _totalExpected
    private val _totalCollected = MutableLiveData<Double>()
    val totalCollected: LiveData<Double> = _totalCollected
    private val _totalOutstanding = MutableLiveData<Double>()
    val totalOutstanding: LiveData<Double> = _totalOutstanding
    private val _revenueLabels = MutableLiveData<List<String>>()
    val revenueLabels: LiveData<List<String>> = _revenueLabels
    private val _revenueValues = MutableLiveData<List<Float>>()
    val revenueValues: LiveData<List<Float>> = _revenueValues
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadDashboardData(token: String) {
        if (token.isBlank()) {
            _tenants.value = emptyList()
            _totalExpected.value = 0.0
            _totalCollected.value = 0.0
            _totalOutstanding.value = 0.0
            _revenueLabels.value = emptyList()
            _revenueValues.value = emptyList()
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            tenantRepository.getTenants(token).onSuccess { list ->
                _tenants.value = list
                calculateFinancials(list)
            }.onFailure {
                _tenants.value = emptyList()
                _totalExpected.value = 0.0
                _totalCollected.value = 0.0
                _totalOutstanding.value = 0.0
            }

            tenantRepository.getAllBillLedger(token).onSuccess { entries ->
                calculateRevenueTrend(entries)
            }.onFailure {
                _revenueLabels.value = emptyList()
                _revenueValues.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    private fun calculateFinancials(tenants: List<Tenant>) {
        val activeTenancies = tenants.filter { !it.unit_id.isNullOrBlank() }
        val expected = activeTenancies.sumOf { it.monthly_rent }
        val collected = activeTenancies.filter { it.payment_status.equals("paid", ignoreCase = true) }
            .sumOf { it.monthly_rent }
        _totalExpected.value = expected
        _totalCollected.value = collected
        _totalOutstanding.value = (expected - collected).coerceAtLeast(0.0)
    }

    private fun calculateRevenueTrend(entries: List<BillLedgerEntry>) {
        val formatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
        val sorted = linkedMapOf<String, Double>()
        entries
            .filter { it.status.equals("paid", ignoreCase = true) }
            .forEach { entry ->
                val key = parseMonthKey(entry.period_month)
                    ?: entry.paid_on?.let { parseMonthKey(it) }
                    ?: return@forEach
                sorted[key] = (sorted[key] ?: 0.0) + entry.total_amount
            }

        if (sorted.isEmpty()) {
            _revenueLabels.value = emptyList()
            _revenueValues.value = emptyList()
            return
        }

        val labels = sorted.keys.toList().takeLast(7).map {
            LocalDate.parse("$it-01").format(formatter)
        }
        val values = sorted.values.toList().takeLast(7).map { it.toFloat() }
        _revenueLabels.value = labels
        _revenueValues.value = values
    }

    private fun parseMonthKey(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null

        return try {
            when {
                trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-' -> {
                    LocalDate.parse(trimmed.take(10)).toString().substring(0, 7)
                }
                trimmed.length >= 7 && trimmed[4] == '-' -> {
                    YearMonth.parse(trimmed.take(7)).toString()
                }
                else -> null
            }
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun parsePaymentDate(value: String): LocalDate? {
        return try {
            if (value.contains("-")) LocalDate.parse(value.take(10))
            else LocalDate.parse(value, DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
