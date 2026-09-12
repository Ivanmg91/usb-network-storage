package com.usblocal.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.usblocal.app.data.model.SmbConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveConnections(connections: List<SmbConnection>) {
        val jsonString = json.encodeToString(connections)
        prefs.edit().putString(KEY_CONNECTIONS, jsonString).apply()
    }

    fun loadConnections(): List<SmbConnection> {
        val jsonString = prefs.getString(KEY_CONNECTIONS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SmbConnection>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveConnection(connection: SmbConnection) {
        val current = loadConnections().toMutableList()
        val index = current.indexOfFirst { it.id == connection.id }
        if (index >= 0) {
            current[index] = connection
        } else {
            current.add(connection)
        }
        saveConnections(current)
    }

    fun deleteConnection(connectionId: String) {
        val current = loadConnections().toMutableList()
        current.removeAll { it.id == connectionId }
        saveConnections(current)
    }

    fun getConnection(connectionId: String): SmbConnection? {
        return loadConnections().find { it.id == connectionId }
    }

    companion object {
        private const val PREFS_FILE = "usblocal_secure_prefs"
        private const val KEY_CONNECTIONS = "smb_connections"
    }
}
