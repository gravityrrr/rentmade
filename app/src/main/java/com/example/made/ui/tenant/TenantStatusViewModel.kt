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
        if (token.isBlank()) {
            _tenants.value = emptyList()
            return
        }
        viewModelScope.launch {
            repo.getTenants(token).onSuccess {
                _tenants.value = it
            }.onFailure {
                _tenants.value = emptyList()
            }
        }
    }
}
