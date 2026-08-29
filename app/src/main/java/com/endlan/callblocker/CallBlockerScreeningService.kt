package com.endlan.callblocker

import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse

/**
 * Dipanggil sistem Android setiap ada telepon masuk, SEBELUM HP berdering.
 * Kalau nomor tidak ditemukan di kontak, panggilan otomatis ditolak.
 */
class CallBlockerScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart

        // Kalau nomor tidak ada / disembunyikan (private number), anggap juga "tidak dikenal"
        if (rawNumber.isNullOrBlank()) {
            respondToCall(callDetails, buildBlockResponse())
            return
        }

        val isBlockingEnabled = PrefsHelper.isBlockingEnabled(this)
        if (!isBlockingEnabled) {
            respondToCall(callDetails, buildAllowResponse())
            return
        }

        val isKnown = isNumberInContacts(rawNumber) || PrefsHelper.isWhitelisted(this, rawNumber)

        if (isKnown) {
            respondToCall(callDetails, buildAllowResponse())
        } else {
            PrefsHelper.logBlockedCall(this, rawNumber)
            respondToCall(callDetails, buildBlockResponse())
        }
    }

    private fun isNumberInContacts(number: String): Boolean {
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: SecurityException) {
            // Kalau izin READ_CONTACTS belum diberikan, jangan blokir siapa pun (fail-safe)
            true
        }
    }

    private fun buildBlockResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }

    private fun buildAllowResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .build()
    }
}
