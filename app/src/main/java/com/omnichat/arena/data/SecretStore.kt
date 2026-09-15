package com.omnichat.arena.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.omnichat.arena.core.ProviderId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All API keys + session tokens live here. Backed by Android Keystore
 * (AES256-GCM). Nothing is logged, nothing leaves the device.
 */
interface SecretStore {
    fun get(provider: ProviderId): String?
    fun set(provider: ProviderId, secret: String)
    fun clear(provider: ProviderId)
    fun has(provider: ProviderId): Boolean = get(provider)?.isNotBlank() == true
    fun getRaw(key: String): String?
    fun setRaw(key: String, secret: String)
    fun clearRaw(key: String)
}

@Singleton
class EncryptedSecretStore @Inject constructor(
    @ApplicationContext ctx: Context,
) : SecretStore {
    private val prefs = EncryptedSharedPreferences.create(
        ctx,
        "omni_secrets",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun get(provider: ProviderId): String? = getRaw(provider.name)

    override fun set(provider: ProviderId, secret: String) = setRaw(provider.name, secret)

    override fun clear(provider: ProviderId) {
        prefs.edit().remove(provider.name).apply()
    }

    override fun getRaw(key: String): String? =
        prefs.getString(key, null) ?: prefs.getString("raw:$key", null)?.also {
            prefs.edit().putString(key, it).remove("raw:$key").apply() // migrate once
        }

    override fun setRaw(key: String, secret: String) {
        prefs.edit().putString(key, secret).apply()
    }

    override fun clearRaw(key: String) {
        prefs.edit().remove(key).apply()
    }
}
