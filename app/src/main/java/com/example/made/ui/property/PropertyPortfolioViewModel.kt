package com.example.made.ui.property

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.made.data.model.Property
import com.example.made.data.repository.PropertyRepository
import kotlinx.coroutines.launch

class PropertyPortfolioViewModel : ViewModel() {
    private val repository = PropertyRepository()
    private val _properties = MutableLiveData<List<Property>>()
    val properties: LiveData<List<Property>> = _properties

    fun loadProperties(token: String) {
        viewModelScope.launch {
            repository.getProperties(token).onSuccess {
                _properties.value = it
            }.onFailure { loadDemoProperties() }
        }
    }

    private fun loadDemoProperties() {
        _properties.value = listOf(
            Property(id="p1",name="The Aurelia",address="4500 Pinnacle Drive, Austin TX",
                total_units=24,occupancy_rate=1.0,estimated_value=42500000.0,
                monthly_target_revenue=85000.0,status="active",property_type="multi-family"),
            Property(id="p2",name="Oakwood Terraces",address="1200 Elm Street, Seattle WA",
                total_units=8,occupancy_rate=0.87,estimated_value=12000000.0,
                monthly_target_revenue=28000.0,status="maintenance"),
            Property(id="p3",name="Maple Grove Estate",address="789 Maple Way, Denver CO",
                total_units=1,occupancy_rate=1.0,estimated_value=850000.0,
                monthly_target_revenue=4500.0,status="active"),
            Property(id="p4",name="The Zenith",address="88 Horizon Blvd, Miami FL",
                total_units=120,occupancy_rate=0.75,estimated_value=95000000.0,
                monthly_target_revenue=450000.0,status="active")
        )
    }
}
