package com.endlan.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.ListView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Berhasil diset sebagai default Call Screening app", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Belum diset sebagai default. Fitur blokir tidak akan aktif.", Toast.LENGTH_LONG).show()
        }
        refreshStatus()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshStatus() }

    private lateinit var statusText: TextView
    private lateinit var switchEnabled: SwitchMaterial
    private lateinit var logList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        switchEnabled = findViewById(R.id.switchEnabled)
        logList = findViewById(R.id.logList)

        findViewById<android.widget.Button>(R.id.btnRequestRole).setOnClickListener {
            requestScreeningRole()
        }

        findViewById<android.widget.Button>(R.id.btnRequestPermissions).setOnClickListener {
            requestPermissions()
        }

        switchEnabled.isChecked = PrefsHelper.isBlockingEnabled(this)
        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            PrefsHelper.setBlockingEnabled(this, isChecked)
        }

        refreshStatus()
        loadLog()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        loadLog()
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleRequestLauncher.launch(intent)
                } else {
                    Toast.makeText(this, "Sudah aktif sebagai default", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Role call screening tidak tersedia di device ini", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Fitur ini butuh Android 10 ke atas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE
            )
        )
    }

    private fun refreshStatus() {
        val hasContacts = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        var isDefaultScreener = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            isDefaultScreener = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }

        statusText.text = buildString {
            append(if (isDefaultScreener) "✅ Default call screening: AKTIF\n" else "❌ Belum diset sebagai default call screening\n")
            append(if (hasContacts) "✅ Izin akses kontak: OK" else "❌ Izin akses kontak belum diberikan")
        }
    }

    private fun loadLog() {
        val log = PrefsHelper.getBlockedLog(this)
        val items = log.map { (number, timestamp) ->
            val date = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                .format(java.util.Date(timestamp))
            "$number  —  $date"
        }
        logList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }
}
