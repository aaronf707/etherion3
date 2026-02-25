package com.etherion.network.wallet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64

class WalletManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("wallet_store")

        private val KEY_PUBLIC = stringPreferencesKey("wallet_public")
        private val KEY_PRIVATE = stringPreferencesKey("wallet_private")
        private val KEY_ADDRESS = stringPreferencesKey("wallet_address")
    }

    data class Wallet(
        val publicKey: PublicKey,
        val privateKey: PrivateKey,
        val address: String
    )

    fun getOrCreateWallet(): Wallet = runBlocking {
        val prefs = context.dataStore.data.first()

        val pubEncoded = prefs[KEY_PUBLIC]
        val privEncoded = prefs[KEY_PRIVATE]
        val address = prefs[KEY_ADDRESS]

        if (pubEncoded != null && privEncoded != null && address != null) {
            return@runBlocking Wallet(
                decodePublicKey(pubEncoded),
                decodePrivateKey(privEncoded),
                address
            )
        }

        // Generate new keypair
        // Note: Ed25519 might require API 33+ or a security provider like BouncyCastle on older versions.
        // For this protocol, we'll assume the environment supports it or use a fallback if needed.
        val generator = KeyPairGenerator.getInstance("EC") // Using EC as a more compatible fallback if Ed25519 is missing
        // However, user specifically asked for Ed25519. Let's try Ed25519 first.
        val keyPair = try {
            KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        } catch (e: Exception) {
            // Fallback for older APIs if Ed25519 is not found
            KeyPairGenerator.getInstance("EC").generateKeyPair()
        }

        val newAddress = generateAddress(keyPair.public)

        // Persist
        context.dataStore.edit { store ->
            store[KEY_PUBLIC] = encodeKey(keyPair.public)
            store[KEY_PRIVATE] = encodeKey(keyPair.private)
            store[KEY_ADDRESS] = newAddress
        }

        Wallet(keyPair.public, keyPair.private, newAddress)
    }

    private fun generateAddress(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey.encoded)
        val hex = hash.take(20).joinToString("") { "%02x".format(it) }
        return "etr_$hex"
    }

    private fun encodeKey(key: java.security.Key): String =
        Base64.encodeToString(key.encoded, Base64.DEFAULT)

    private fun decodePublicKey(encoded: String): PublicKey {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(bytes)
        return try {
            KeyFactory.getInstance("Ed25519").generatePublic(spec)
        } catch (e: Exception) {
            KeyFactory.getInstance("EC").generatePublic(spec)
        }
    }

    private fun decodePrivateKey(encoded: String): PrivateKey {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(bytes)
        return try {
            KeyFactory.getInstance("Ed25519").generatePrivate(spec)
        } catch (e: Exception) {
            KeyFactory.getInstance("EC").generatePrivate(spec)
        }
    }
}
