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
import com.example.made.util.handleAuthExpired
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
            onAddTenant = { unit -> showTenantOptions(unit) }
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

    private fun showTenantOptions(unit: RentalUnit) {
        val assignedTenantFromList = tenants.firstOrNull { it.unit_id == unit.id }
        val isAlreadyAssigned = !unit.tenant_id.isNullOrBlank() || assignedTenantFromList != null
        if (isAlreadyAssigned) {
            val assignedName = assignedTenantFromList?.name
            if (assignedName.isNullOrBlank()) {
                toast("This unit already has an assigned tenant")
            } else {
                toast("This unit is already assigned to $assignedName")
            }
            return
        }

        val options = arrayOf("Add New Tenant", "Assign Existing Tenant")
        AlertDialog.Builder(this)
            .setTitle("Assign tenant")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAddTenantScreen(unit)
                    1 -> showAssignExistingTenantDialog(unit)
                }
            }
            .show()
    }

    private fun openAddTenantScreen(unit: RentalUnit) {
        startActivity(Intent(this, AddTenantActivity::class.java).apply {
            putExtra(Constants.EXTRA_PROPERTY_ID, propertyId)
            putExtra(Constants.EXTRA_UNIT_ID, unit.id)
            putExtra(Constants.EXTRA_UNIT_NUMBER, unit.door_number)
        })
    }

    private fun showAssignExistingTenantDialog(unit: RentalUnit) {
        val unassignedTenants = tenants.filter { it.unit_id.isNullOrBlank() }
        if (unassignedTenants.isEmpty()) {
            toast("No unassigned tenants available for this property")
            return
        }

        val tenantLabels = unassignedTenants.map { tenant ->
            val phoneLabel = tenant.phone.takeIf { it.isNotBlank() } ?: "No phone"
            "${tenant.name} • $phoneLabel"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select existing tenant")
            .setItems(tenantLabels) { _, index ->
                assignExistingTenantToUnit(unassignedTenants[index], unit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun assignExistingTenantToUnit(tenant: Tenant, unit: RentalUnit) {
        val token = SessionManager(this).authToken.orEmpty()
        if (token.isBlank()) return
        if (!tenant.unit_id.isNullOrBlank()) {
            toast("Tenant is already assigned to a unit")
            return
        }

        lifecycleScope.launch {
            val tenantUpdate = tenantRepository.updateTenant(
                token,
                tenant.id,
                mapOf(
                    "unit_id" to unit.id,
                    "unit_number" to unit.door_number,
                    "property_id" to propertyId
                )
            )

            if (tenantUpdate.isFailure) {
                val message = tenantUpdate.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast(message ?: "Unable to assign tenant")
                }
                return@launch
            }

            val unitUpdate = unitRepository.updateUnit(
                token,
                unit.id,
                mapOf("tenant_id" to tenant.id)
            )

            if (unitUpdate.isFailure) {
                tenantRepository.updateTenant(
                    token,
                    tenant.id,
                    mapOf(
                        "unit_id" to tenant.unit_id,
                        "unit_number" to tenant.unit_number
                    )
                )
                val message = unitUpdate.exceptionOrNull()?.message
                if (!handleAuthExpired(message)) {
                    toast(message ?: "Tenant linked, but unit update failed")
                }
                return@launch
            }

            toast("Tenant assigned to Door ${unit.door_number}")
            loadUnitsAndTenants()
        }
    }
}
