package com.example.made.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.made.data.model.Payment
import com.example.made.data.repository.TenantRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class DashboardViewModel : ViewModel() {
    private val tenantRepository = TenantRepository()
    private val _tenants = MutableLiveData<List<com.example.made.data.model.Tenant>>()
    val tenants: LiveData<List<com.example.made.data.model.Tenant>> = _tenants
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

            tenantRepository.getAllPayments(token).onSuccess { payments ->
                calculateRevenueTrend(payments)
            }.onFailure {
                _revenueLabels.value = emptyList()
                _revenueValues.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    private fun calculateFinancials(tenants: List<com.example.made.data.model.Tenant>) {
        val expected = tenants.sumOf { it.monthly_rent }
        val collected = tenants.filter { it.payment_status == "paid" }.sumOf { it.monthly_rent }
        _totalExpected.value = expected
        _totalCollected.value = collected
        _totalOutstanding.value = expected - collected
    }

    private fun calculateRevenueTrend(payments: List<Payment>) {
        val formatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
        val sorted = linkedMapOf<String, Double>()
        payments.forEach { payment ->
            val parsed = parsePaymentDate(payment.payment_date) ?: return@forEach
            val key = parsed.toString().substring(0, 7)
            sorted[key] = (sorted[key] ?: 0.0) + payment.amount
        }

        val labels = sorted.keys.takeLast(7).map {
            LocalDate.parse("$it-01").format(formatter)
        }
        val values = sorted.values.takeLast(7).map { it.toFloat() }
        _revenueLabels.value = labels
        _revenueValues.value = values
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
