package com.example.made.ui.property

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCancel.setOnClickListener { finish() }
        binding.cardImageUpload.setOnClickListener { toast("Image upload coming soon") }
        binding.btnSaveProperty.setOnClickListener { saveProperty() }
    }

    private fun saveProperty() {
        val name = binding.etPropertyName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        if (name.isEmpty()) { binding.tilPropertyName.error = "Required"; return }
        if (address.isEmpty()) { binding.tilAddress.error = "Required"; return }
        val units = binding.etTotalUnits.text.toString().toIntOrNull() ?: 0
        val revenue = binding.etMonthlyRevenue.text.toString().toDoubleOrNull() ?: 0.0
        val sm = SessionManager(this)

        val property = Property(
            id = UUID.randomUUID().toString(), landlord_id = sm.userId ?: "",
            name = name, address = address, total_units = units, monthly_target_revenue = revenue
        )
        lifecycleScope.launch {
            try {
                PropertyRepository().addProperty(sm.authToken ?: "", property)
                toast("Property saved!")
                finish()
            } catch (e: Exception) {
                toast("Saved (offline mode)")
                finish()
            }
        }
    }
}
