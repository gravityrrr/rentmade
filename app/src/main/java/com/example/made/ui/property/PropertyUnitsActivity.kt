package com.example.made.ui.property

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.made.data.model.Tenant
import com.example.made.data.model.Unit as RentalUnit
import com.example.made.data.repository.TenantRepository
import com.example.made.data.repository.UnitRepository
import com.example.made.databinding.ActivityPropertyUnitsBinding
import com.example.made.databinding.DialogEditUnitBinding
import com.example.made.ui.tenant.AddTenantActivity
import com.example.made.util.Constants
import com.example.made.util.SessionManager
import com.example.made.util.toast
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.launch
import java.util.UUID

class PropertyUnitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyUnitsBinding
    private lateinit var unitAdapter: UnitAdapter
    private val unitRepository = UnitRepository()
    private val tenantRepository = TenantRepository()
    private var propertyId: String = ""
    private var propertyName: String = ""
    private var tenants: List<Tenant> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowTransitions()
        binding = ActivityPropertyUnitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        propertyId = intent.getStringExtra(Constants.EXTRA_PROPERTY_ID).orEmpty()
        propertyName = intent.getStringExtra(Constants.EXTRA_PROPERTY_NAME).orEmpty()
        binding.tvPropertyName.text = propertyName.ifBlank { "Property Units" }

        unitAdapter = UnitAdapter(
            onEdit = { showEditUnitDialog(it) },
            onAddTenant = { unit ->
                startActivity(Intent(this, AddTenantActivity::class.java).apply {
                    putExtra(Constants.EXTRA_PROPERTY_ID, propertyId)
                    putExtra(Constants.EXTRA_UNIT_ID, unit.id)
                    putExtra(Constants.EXTRA_UNIT_NUMBER, unit.door_number)
                })
            }
        )
        binding.rvUnits.adapter = unitAdapter
        binding.fabAddUnit.setOnClickListener { showEditUnitDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        loadUnitsAndTenants()
    }

    private fun setupWindowTransitions() {
        window.enterTransition = MaterialFadeThrough().apply { duration = 240L }
        window.returnTransition = MaterialFadeThrough().apply { duration = 220L }
    }

    private fun loadUnitsAndTenants() {
        val token = SessionManager(this).authToken.orEmpty()
        if (token.isBlank() || propertyId.isBlank()) return

        lifecycleScope.launch {
            tenantRepository.getTenantsByProperty(token, propertyId).onSuccess {
                tenants = it
                unitAdapter.submitTenants(it)
            }
            unitRepository.getUnitsByProperty(token, propertyId).onSuccess {
                unitAdapter.submitList(it)
            }.onFailure {
                toast("Unable to load units")
            }
        }
    }

    private fun showEditUnitDialog(existing: RentalUnit?) {
        val dialogBinding = DialogEditUnitBinding.inflate(layoutInflater)
        if (existing != null) {
            dialogBinding.etDoor.setText(existing.door_number)
            dialogBinding.etFloor.setText(existing.floor_label.orEmpty())
            dialogBinding.etFans.setText(existing.fan_count.toString())
            dialogBinding.etGeysers.setText(existing.geyser_count.toString())
            dialogBinding.etNotes.setText(existing.notes.orEmpty())
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Unit" else "Edit Unit")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _
                ->
                saveUnit(existing, dialogBinding)
            }
            .show()
    }

    private fun saveUnit(existing: RentalUnit?, dialogBinding: DialogEditUnitBinding) {
        val door = dialogBinding.etDoor.text?.toString()?.trim().orEmpty()
        if (door.isBlank()) {
            toast("Door number required")
            return
        }

        val floor = dialogBinding.etFloor.text?.toString()?.trim().orEmpty()
        val fans = dialogBinding.etFans.text?.toString()?.toIntOrNull() ?: 0
        val geysers = dialogBinding.etGeysers.text?.toString()?.toIntOrNull() ?: 0
        val notes = dialogBinding.etNotes.text?.toString()?.trim().orEmpty()
        val token = SessionManager(this).authToken.orEmpty()

        lifecycleScope.launch {
            if (existing == null) {
                unitRepository.addUnit(
                    token,
                    RentalUnit(
                        id = UUID.randomUUID().toString(),
                        property_id = propertyId,
                        door_number = door,
                        floor_label = floor.ifBlank { null },
                        fan_count = fans,
                        geyser_count = geysers,
                        notes = notes.ifBlank { null }
                    )
                )
            } else {
                unitRepository.updateUnit(
                    token,
                    existing.id,
                    mapOf(
                        "door_number" to door,
                        "floor_label" to floor.ifBlank { null },
                        "fan_count" to fans,
                        "geyser_count" to geysers,
                        "notes" to notes.ifBlank { null }
                    )
                )
            }.onFailure {
                toast("Unable to save unit")
            }
            loadUnitsAndTenants()
        }
    }
}
