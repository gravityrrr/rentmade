package com.example.made.ui.property

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.example.made.data.model.Property
import com.example.made.data.repository.PropertyRepository
import com.example.made.databinding.ActivityAddPropertyBinding
import com.example.made.util.SessionManager
import com.example.made.util.toast
import kotlinx.coroutines.launch
import java.util.UUID

class AddPropertyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPropertyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.alpha = 0f
        binding.root.translationY = 18f
        binding.root.animate().alpha(1f).translationY(0f).setDuration(300L).start()

        binding.btnCancel.setOnClickListener { finish() }
        binding.cardImageUpload.setOnClickListener { toast("Image upload coming soon") }
        binding.btnSaveProperty.setOnClickListener { saveProperty() }
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
        val revenue = binding.etMonthlyRevenue.text.toString().toDoubleOrNull() ?: 0.0
        val sm = SessionManager(this)

        val token = sm.authToken.orEmpty()
        val landlordId = sm.userId.orEmpty()
        if (token.isBlank() || landlordId.isBlank()) {
            toast("Session expired. Please sign in again")
            return
        }

        val property = Property(
            id = UUID.randomUUID().toString(), landlord_id = landlordId,
            name = name, address = address, total_units = units, monthly_target_revenue = revenue
        )
        lifecycleScope.launch {
            PropertyRepository().addProperty(token, property)
                .onSuccess {
                toast("Property saved!")
                finish()
                }
                .onFailure {
                    toast("Unable to save property")
                }
        }
    }
}
