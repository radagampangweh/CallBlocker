package com.endlan.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

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

    private lateinit var iconScreening: ImageView
    private lateinit var statusScreeningText: TextView
    private lateinit var iconContacts: ImageView
    private lateinit var statusContactsText: TextView
    private lateinit var switchEnabled: SwitchMaterial
    private lateinit var logList: ListView
    private lateinit var emptyLogText: TextView

    private val colorActive = android.graphics.Color.parseColor("#2E7D32")
    private val colorInactive = android.graphics.Color.parseColor("#E57373")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        iconScreening = findViewById(R.id.iconScreening)
        statusScreeningText = findViewById(R.id.statusScreeningText)
        iconContacts = findViewById(R.id.iconContacts)
        statusContactsText = findViewById(R.id.statusContactsText)
        switchEnabled = findViewById(R.id.switchEnabled)
        logList = findViewById(R.id.logList)
        emptyLogText = findViewById(R.id.emptyLogText)

        findViewById<MaterialButton>(R.id.btnRequestRole).setOnClickListener {
            requestScreeningRole()
        }

        findViewById<MaterialButton>(R.id.btnRequestPermissions).setOnClickListener {
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

        iconScreening.setColorFilter(if (isDefaultScreener) colorActive else colorInactive, PorterDuff.Mode.SRC_IN)
        statusScreeningText.text = if (isDefaultScreener) "Default call screening: aktif" else "Belum diset sebagai default call screening"

        iconContacts.setColorFilter(if (hasContacts) colorActive else colorInactive, PorterDuff.Mode.SRC_IN)
        statusContactsText.text = if (hasContacts) "Izin akses kontak: aktif" else "Izin akses kontak belum diberikan"
    }

    private fun loadLog() {
        val log = PrefsHelper.getBlockedLog(this)
        if (log.isEmpty()) {
            emptyLogText.visibility = TextView.VISIBLE
            logList.visibility = ListView.GONE
        } else {
            emptyLogText.visibility = TextView.GONE
            logList.visibility = ListView.VISIBLE
            val items = log.map { (number, timestamp) ->
                val date = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                    .format(java.util.Date(timestamp))
                "$number  —  $date"
            }
            logList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        }
    }
}
