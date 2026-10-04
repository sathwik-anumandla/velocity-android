package com.velocity.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ServerConfig(
    val baseUrl: String = "",
    val cfClientId: String = "",
    val cfClientSecret: String = "",
    val isPaired: Boolean = false
) {
    val normalizedUrl: String
        get() {
            var url = baseUrl.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            if (!url.endsWith("/")) {
                url = "$url/"
            }
            return url
        }
}

object ServerConfigManager {
    private const val PREFS_NAME = "velocity_server_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_CF_CLIENT_ID = "cf_client_id"
    private const val KEY_CF_CLIENT_SECRET = "cf_client_secret"
    private const val KEY_IS_PAIRED = "is_paired"

    private val json = Json { ignoreUnknownKeys = true }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadConfig(context: Context): ServerConfig {
        val prefs = getPrefs(context)
        val url = prefs.getString(KEY_BASE_URL, "") ?: ""
        val clientId = prefs.getString(KEY_CF_CLIENT_ID, "") ?: ""
        val clientSecret = prefs.getString(KEY_CF_CLIENT_SECRET, "") ?: ""
        val isPaired = prefs.getBoolean(KEY_IS_PAIRED, false) && url.isNotBlank()

        return ServerConfig(
            baseUrl = url,
            cfClientId = clientId,
            cfClientSecret = clientSecret,
            isPaired = isPaired
        )
    }

    fun saveConfig(context: Context, config: ServerConfig) {
        getPrefs(context).edit()
            .putString(KEY_BASE_URL, config.normalizedUrl)
            .putString(KEY_CF_CLIENT_ID, config.cfClientId.trim())
            .putString(KEY_CF_CLIENT_SECRET, config.cfClientSecret.trim())
            .putBoolean(KEY_IS_PAIRED, config.isPaired)
            .apply()
    }

    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun isPaired(context: Context): Boolean {
        return loadConfig(context).isPaired
    }

    /**
     * Parses scanned text from QR code or manual input.
     * Can parse JSON: {"url":"...","client_id":"...","client_secret":"..."}
     * or a direct URL string: "https://chat.sathwik.work"
     */
    fun parseQrPayload(text: String): ServerConfig? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // Try parsing JSON first
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val map = json.decodeFromString<Map<String, String>>(trimmed)
                val url = map["url"] ?: map["baseUrl"] ?: map["host"] ?: ""
                val clientId = map["client_id"] ?: map["cf_client_id"] ?: ""
                val clientSecret = map["client_secret"] ?: map["cf_client_secret"] ?: ""

                if (url.isNotBlank()) {
                    return ServerConfig(
                        baseUrl = url,
                        cfClientId = clientId,
                        cfClientSecret = clientSecret,
                        isPaired = true
                    )
                }
            } catch (_: Exception) {}
        }

        // Fallback: direct URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains(".")) {
            return ServerConfig(
                baseUrl = trimmed,
                cfClientId = "",
                cfClientSecret = "",
                isPaired = true
            )
        }

        return null
    }
}
