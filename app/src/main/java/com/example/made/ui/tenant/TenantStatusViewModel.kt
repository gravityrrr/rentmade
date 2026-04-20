package com.example.made.ui.tenant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.made.data.model.Tenant
import com.example.made.data.repository.TenantRepository
import kotlinx.coroutines.launch

class TenantStatusViewModel : ViewModel() {
    private val repo = TenantRepository()
    private val _tenants = MutableLiveData<List<Tenant>>()
    val tenants: LiveData<List<Tenant>> = _tenants

    fun loadTenants(token: String) {
        viewModelScope.launch {
            repo.getTenants(token).onSuccess { _tenants.value = it }.onFailure { loadDemo() }
        }
    }

    fun loadDemo() {
        _tenants.value = listOf(
            Tenant(id="1",property_id="p1",name="James Smith",email="james@ex.com",phone="+15551234567",
                unit_number="Apt 4B",monthly_rent=2500.0,payment_status="pending",property_name="The Grand",
                lease_start="2023-01-01",lease_end="2024-12-31",due_date="2023-10-01"),
            Tenant(id="2",property_id="p1",name="Sarah Connor",email="sarah@ex.com",phone="+15559876543",
                unit_number="Unit 12",monthly_rent=3200.0,payment_status="overdue",property_name="Riverside",
                lease_start="2023-03-01",lease_end="2025-02-28",due_date="2023-09-25"),
            Tenant(id="3",property_id="p2",name="Michael Ross",email="michael@ex.com",phone="+15555551234",
                unit_number="Penthouse",monthly_rent=8500.0,payment_status="paid",property_name="Skyline",
                lease_start="2023-06-01",lease_end="2024-05-31",due_date="2023-10-01"),
            Tenant(id="4",property_id="p2",name="Elena Rostova",email="elena@ex.com",phone="+15550192834",
                unit_number="Apt 4B",monthly_rent=3450.0,payment_status="paid",property_name="The Lumina",
                lease_start="2023-01-15",lease_end="2024-01-14",due_date="2023-10-01"),
            Tenant(id="5",property_id="p3",name="Julian Davis",email="julian@ex.com",phone="+15553456789",
                unit_number="Apt 402",monthly_rent=2450.0,payment_status="overdue",property_name="The Aria",
                lease_start="2023-02-01",lease_end="2024-01-31",due_date="2023-09-18"),
            Tenant(id="6",property_id="p3",name="Maria Rodriguez",email="maria@ex.com",phone="+15557654321",
                unit_number="Unit B",monthly_rent=1800.0,payment_status="overdue",property_name="Oakwood Suites",
                lease_start="2023-04-01",lease_end="2024-03-31",due_date="2023-09-26")
        )
    }
}
