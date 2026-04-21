package com.example.made.ui.tenant

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import com.example.made.R
import com.example.made.data.model.Property
import com.example.made.data.model.Tenant
import com.example.made.data.repository.DocumentVaultRepository
import com.example.made.data.repository.PropertyRepository
import com.example.made.data.repository.TenantRepository
import com.example.made.databinding.ActivityAddTenantBinding
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.handleAuthExpired
import com.example.made.util.parseFlexibleDate
import com.example.made.util.toStorageIsoDateOrSelf
import com.example.made.util.toAmountOrZero
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
    private var selectedTenantImageUri: Uri? = null
    private val documentRepository = DocumentVaultRepository()

    private val pickTenantImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        selectedTenantImageUri = uri
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivTenantPreview)
        binding.tvTenantImageHint.text = "Image selected"
    }

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
        binding.cardTenantImageUpload.setOnClickListener { pickTenantImage.launch("image/*") }
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
        val rent = binding.etRent.text?.toString().toAmountOrZero()
        val dueDateDisplay = binding.etDueDate.text?.toString()?.trim().orEmpty()
        val dueDate = dueDateDisplay.toStorageIsoDateOrSelf()

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
        if (!isValidDueDate(dueDateDisplay)) {
            binding.tilDueDate.error = "Use DD-MM-YYYY format"
            return
        }

        val session = SessionManager(this)
        val token = session.authToken.orEmpty()
        val userId = session.userId.orEmpty()
        if (token.isBlank() || userId.isBlank()) {
            toast("Session expired. Please sign in again")
            return
        }

        lifecycleScope.launch {
            val tenantId = UUID.randomUUID().toString()
            var avatarUrl: String? = null

            if (selectedTenantImageUri != null) {
                val uploadResult = documentRepository.uploadUserImage(
                    context = this@AddTenantActivity,
                    token = token,
                    userId = userId,
                    entityId = tenantId,
                    prefix = "avatar",
                    imageUri = selectedTenantImageUri!!,
                    bucket = DocumentVaultRepository.TENANT_IMAGES_BUCKET
                )
                if (uploadResult.isFailure) {
                    val message = uploadResult.exceptionOrNull()?.message
                    if (!handleAuthExpired(message)) {
                        toast(message ?: "Unable to upload tenant image")
                    }
                    return@launch
                }
                val path = uploadResult.getOrNull().orEmpty()
                avatarUrl = documentRepository.buildPublicObjectUrl(DocumentVaultRepository.TENANT_IMAGES_BUCKET, path)
            }

            val tenant = Tenant(
                id = tenantId,
                property_id = propertyId,
                unit_id = selectedUnitId,
                name = name,
                email = email,
                phone = phone,
                unit_number = unit,
                monthly_rent = rent,
                due_date = dueDate.ifBlank { "1" },
                payment_status = "pending",
                avatar_url = avatarUrl
            )

            val result = TenantRepository().addTenant(token, tenant)
            if (result.isSuccess) {
                toast("Tenant added")
                finish()
            } else {
                val message = result.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast(message ?: "Failed to add tenant")
                }
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
            }.onFailure { err ->
                if (!handleAuthExpired(err.message)) {
                    toast("Unable to load properties")
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
            binding.etDueDate.setText(selected.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            binding.tilDueDate.error = null
        }, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }

    private fun isValidDueDate(value: String): Boolean {
        return parseFlexibleDate(value) != null
    }
}
