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
        if (token.isBlank()) {
            _properties.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.getProperties(token).onSuccess {
                _properties.value = it
            }.onFailure {
                _properties.value = emptyList()
            }
        }
    }
}
