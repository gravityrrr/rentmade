package com.example.made.ui.property

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.example.made.data.model.Property
import com.example.made.data.repository.DocumentVaultRepository
import com.example.made.data.repository.PropertyRepository
import com.example.made.databinding.ActivityAddPropertyBinding
import com.example.made.util.Constants
import com.example.made.util.handleAuthExpired
import com.example.made.util.SessionManager
import com.example.made.util.toAmountOrZero
import com.example.made.util.toast
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class AddPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPropertyBinding
    private var selectedPropertyImageUri: Uri? = null
    private var isEditMode: Boolean = false
    private var existingPropertyId: String = ""
    private var existingImageUrl: String? = null
    private val documentRepository = DocumentVaultRepository()

    private val pickPropertyImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        selectedPropertyImageUri = uri
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivPropertyPreview)
        binding.tvPropertyImageHint.text = "Image selected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.alpha = 0f
        binding.root.translationY = 18f
        binding.root.animate().alpha(1f).translationY(0f).setDuration(300L).start()

        isEditMode = intent.getBooleanExtra(Constants.EXTRA_EDIT_MODE, false)
        existingPropertyId = intent.getStringExtra(Constants.EXTRA_PROPERTY_ID).orEmpty()
        prefillIfEdit()

        binding.btnCancel.setOnClickListener { finish() }
        binding.cardImageUpload.setOnClickListener { pickPropertyImage.launch("image/*") }
        binding.btnSaveProperty.setOnClickListener { saveProperty() }
    }

    private fun prefillIfEdit() {
        if (!isEditMode) return

        binding.headerLayout.tvHeaderTitle.text = "Edit Property"
        binding.tvNewProperty.text = "Edit Property"
        binding.btnSaveProperty.text = "Update Property"

        binding.etPropertyName.setText(intent.getStringExtra(Constants.EXTRA_PROPERTY_NAME).orEmpty())
        binding.etAddress.setText(intent.getStringExtra(Constants.EXTRA_PROPERTY_ADDRESS).orEmpty())
        binding.etTotalUnits.setText(intent.getIntExtra(Constants.EXTRA_PROPERTY_TOTAL_UNITS, 0).toString())
        binding.etMonthlyRevenue.setText(
            intent.getDoubleExtra(Constants.EXTRA_PROPERTY_MONTHLY_TARGET, 0.0).toString()
        )
        existingImageUrl = intent.getStringExtra(Constants.EXTRA_PROPERTY_IMAGE_URL)
        if (!existingImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(existingImageUrl)
                .centerCrop()
                .into(binding.ivPropertyPreview)
            binding.tvPropertyImageHint.text = "Tap to replace image"
        }
    }

    private fun setupWindowTransitions() {
        window.enterTransition = MaterialFadeThrough().apply { duration = 260L }
        window.returnTransition = MaterialFadeThrough().apply { duration = 220L }
    }

    private fun saveProperty() {
        val name = binding.etPropertyName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        if (name.isEmpty()) { binding.tilPropertyName.error = "Required"; return }
        if (address.isEmpty()) { binding.tilAddress.error = "Required"; return }
        val units = binding.etTotalUnits.text.toString().toIntOrNull() ?: 0
        val revenue = binding.etMonthlyRevenue.text?.toString().toAmountOrZero()
        val sm = SessionManager(this)

        val token = sm.authToken.orEmpty()
        val landlordId = sm.userId.orEmpty().ifBlank { extractUserIdFromToken(token) }
        if (token.isBlank() || landlordId.isBlank()) {
            toast("Session expired. Please sign in again")
            return
        }

        lifecycleScope.launch {
            val propertyId = if (isEditMode && existingPropertyId.isNotBlank()) {
                existingPropertyId
            } else {
                UUID.randomUUID().toString()
            }
            var imageUrl: String? = existingImageUrl
            if (selectedPropertyImageUri != null) {
                val uploadResult = documentRepository.uploadUserImage(
                    context = this@AddPropertyActivity,
                    token = token,
                    userId = landlordId,
                    entityId = propertyId,
                    prefix = "hero",
                    imageUri = selectedPropertyImageUri!!,
                    bucket = DocumentVaultRepository.PROPERTY_IMAGES_BUCKET
                )
                if (uploadResult.isFailure) {
                    val message = uploadResult.exceptionOrNull()?.message
                    if (!handleAuthExpired(message)) {
                        toast(message ?: "Unable to upload property image")
                    }
                    return@launch
                }
                val path = uploadResult.getOrNull().orEmpty()
                imageUrl = documentRepository.buildPublicObjectUrl(DocumentVaultRepository.PROPERTY_IMAGES_BUCKET, path)
            }

            val property = Property(
                id = propertyId,
                landlord_id = landlordId,
                name = name,
                address = address,
                total_units = units,
                monthly_target_revenue = revenue,
                image_url = imageUrl
            )

            val result = if (isEditMode) {
                PropertyRepository().updateProperty(token, propertyId, property)
            } else {
                PropertyRepository().addProperty(token, property)
            }

            result
                .onSuccess {
                    toast(if (isEditMode) "Property updated!" else "Property saved!")
                    finish()
                }
                .onFailure { err ->
                    val message = err.message.orEmpty()
                    if (!handleAuthExpired(message)) {
                        toast(message.ifBlank { if (isEditMode) "Unable to update property" else "Unable to save property" })
                    }
                }
        }
    }

    private fun extractUserIdFromToken(token: String): String {
        return runCatching {
            val parts = token.split('.')
            if (parts.size < 2) return ""
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            JSONObject(payload).optString("sub", "")
        }.getOrDefault("")
    }
}
