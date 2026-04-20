package com.example.made.ui.tenant

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.made.R
import com.example.made.data.model.Property
import com.example.made.data.model.Tenant
import com.example.made.data.repository.PropertyRepository
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityAddTenantBinding
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.toast
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

class AddTenantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTenantBinding
    private val properties = mutableListOf<Property>()
    private var selectedPropertyId: String = ""
    private var selectedUnitId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivityAddTenantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedUnitId = intent.getStringExtra(Constants.EXTRA_UNIT_ID)
        val prefillDoorNumber = intent.getStringExtra(Constants.EXTRA_UNIT_NUMBER).orEmpty()
        if (prefillDoorNumber.isNotBlank()) {
            binding.etTenantUnit.setText(prefillDoorNumber)
        }

        setupDueDateField()
        loadProperties()

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSaveTenant.setOnClickListener { saveTenant() }
    }

    private fun setupWindowTransitions() {
        window.enterTransition = MaterialFadeThrough().apply { duration = 240L }
        window.returnTransition = MaterialFadeThrough().apply { duration = 220L }
    }

    private fun saveTenant() {
        val name = binding.etTenantName.text?.toString()?.trim().orEmpty()
        val email = binding.etTenantEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.etTenantPhone.text?.toString()?.trim().orEmpty()
        val unit = binding.etTenantUnit.text?.toString()?.trim().orEmpty()
        val propertyId = selectedPropertyId
        val rent = binding.etRent.text?.toString()?.toDoubleOrNull() ?: 0.0
        val dueDate = binding.etDueDate.text?.toString()?.trim().orEmpty()

        binding.tilTenantName.error = null
        binding.tilProperty.error = null
        binding.tilRent.error = null
        binding.tilDueDate.error = null

        if (name.isEmpty()) {
            binding.tilTenantName.error = "Required"
            return
        }
        if (propertyId.isEmpty()) {
            binding.tilProperty.error = "Please select property"
            return
        }
        if (rent <= 0.0) {
            binding.tilRent.error = "Enter valid rent"
            return
        }
        if (!isValidDueDate(dueDate)) {
            binding.tilDueDate.error = "Use YYYY-MM-DD format"
            return
        }

        val session = SessionManager(this)
        val tenant = Tenant(
            id = UUID.randomUUID().toString(),
            property_id = propertyId,
            unit_id = selectedUnitId,
            name = name,
            email = email,
            phone = phone,
            unit_number = unit,
            monthly_rent = rent,
            due_date = dueDate.ifBlank { "1" },
            payment_status = "pending"
        )

        lifecycleScope.launch {
            val result = TenantRepository().addTenant(session.authToken ?: "", tenant)
            if (result.isSuccess) {
                toast("Tenant added")
                finish()
            } else {
                toast("Failed to add tenant")
            }
        }
    }

    private fun loadProperties() {
        val token = SessionManager(this).authToken.orEmpty()
        if (token.isBlank()) return
        lifecycleScope.launch {
            PropertyRepository().getProperties(token).onSuccess { result ->
                properties.clear()
                properties.addAll(result)
                val names = properties.map { it.name }
                val adapter = ArrayAdapter(this@AddTenantActivity, android.R.layout.simple_list_item_1, names)
                binding.actProperty.setAdapter(adapter)

                val prefillPropertyId = intent.getStringExtra(Constants.EXTRA_PROPERTY_ID).orEmpty()
                val prefillProperty = properties.firstOrNull { it.id == prefillPropertyId }
                if (prefillProperty != null) {
                    selectedPropertyId = prefillProperty.id
                    binding.actProperty.setText(prefillProperty.name, false)
                }
            }
            binding.actProperty.setOnItemClickListener { _, _, position, _ ->
                val selectedName = binding.actProperty.adapter.getItem(position).toString()
                selectedPropertyId = properties.firstOrNull { it.name == selectedName }?.id.orEmpty()
                binding.tilProperty.error = null
            }
        }
    }

    private fun setupDueDateField() {
        binding.tilDueDate.helperText = getString(R.string.due_date_helper)
        binding.etDueDate.setOnClickListener { showDatePicker() }
        binding.tilDueDate.setEndIconOnClickListener { showDatePicker() }
        binding.etDueDate.doAfterTextChanged {
            val text = it?.toString().orEmpty()
            binding.tilDueDate.error = if (text.isNotBlank() && !isValidDueDate(text)) {
                "Invalid date format"
            } else {
                null
            }
        }
    }

    private fun showDatePicker() {
        val now = LocalDate.now()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selected = LocalDate.of(year, month + 1, dayOfMonth)
            binding.etDueDate.setText(selected.format(DateTimeFormatter.ISO_LOCAL_DATE))
            binding.tilDueDate.error = null
        }, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }

    private fun isValidDueDate(value: String): Boolean {
        return try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }
}
