package com.endlan.callblocker

import android.content.Context
import org.json.JSONArray

object PrefsHelper {
    private const val PREFS_NAME = "call_blocker_prefs"
    private const val KEY_ENABLED = "blocking_enabled"
    private const val KEY_WHITELIST = "whitelist_numbers"
    private const val KEY_LOG = "blocked_log"
    private const val MAX_LOG_ENTRIES = 100

    fun isBlockingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isWhitelisted(context: Context, number: String): Boolean {
        return getWhitelist(context).contains(normalize(number))
    }

    fun getWhitelist(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_WHITELIST, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun addToWhitelist(context: Context, number: String) {
        val current = getWhitelist(context).toMutableList()
        val normalized = normalize(number)
        if (!current.contains(normalized)) {
            current.add(normalized)
            saveWhitelist(context, current)
        }
    }

    fun removeFromWhitelist(context: Context, number: String) {
        val current = getWhitelist(context).toMutableList()
        current.remove(normalize(number))
        saveWhitelist(context, current)
    }

    private fun saveWhitelist(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_WHITELIST, arr.toString()).apply()
    }

    fun logBlockedCall(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOG, "[]") ?: "[]"
        val arr = JSONArray(raw)

        val entry = org.json.JSONObject()
        entry.put("number", number)
        entry.put("timestamp", System.currentTimeMillis())

        val newArr = JSONArray()
        newArr.put(entry)
        for (i in 0 until minOf(arr.length(), MAX_LOG_ENTRIES - 1)) {
            newArr.put(arr.get(i))
        }

        prefs.edit().putString(KEY_LOG, newArr.toString()).apply()
    }

    fun getBlockedLog(context: Context): List<Pair<String, Long>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOG, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            obj.getString("number") to obj.getLong("timestamp")
        }
    }

    /**
     * Menghapus seluruh riwayat panggilan yang diblokir.
     */
    fun clearBlockedLog(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOG, "[]").apply()
    }

    /**
     * Menghapus satu entri riwayat berdasarkan posisinya di list (index 0 = paling baru).
     */
    fun removeLogEntry(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOG, "[]") ?: "[]"
        val arr = JSONArray(raw)
        if (index < 0 || index >= arr.length()) return

        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            if (i != index) newArr.put(arr.get(i))
        }
        prefs.edit().putString(KEY_LOG, newArr.toString()).apply()
    }

    private fun normalize(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }
}
