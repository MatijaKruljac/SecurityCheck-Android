package com.securitycheck.demoapp.services.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/*
Example:
When storing a password in the KeyStore, it is recommended to
use a symmetric encryption algorithm to protect the password.
*/
class KeyStoreHandler(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH = 128
        private const val PREFERENCES = "MyPrefs"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

    init {
        keyStore.load(null)
    }

    fun saveString(string: String, key: String) {
        val secretKey = generateSecretKey(key)
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(string.toByteArray(Charset.defaultCharset()))
        val encodedData = android.util.Base64.encodeToString(encryptedData, android.util.Base64.DEFAULT)
        val encodedIV = android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT)
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(key, encodedData)
            .putString(key + "_iv", encodedIV)
            .apply()
    }

    fun getString(key: String): String? {
        val encodedData = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(key, null)
        val encodedIV = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(key + "_iv", null)
        if (encodedData != null && encodedIV != null) {
            val secretKey = getKeyFromKeyStore(key)
            val cipher = getCipher()
            val iv = android.util.Base64.decode(encodedIV, android.util.Base64.DEFAULT)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val encryptedData = android.util.Base64.decode(encodedData, android.util.Base64.DEFAULT)
            val decryptedData = cipher.doFinal(encryptedData)
            return String(decryptedData, Charset.defaultCharset())
        }

        return null
    }

    private fun generateSecretKey(key: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val builder = KeyGenParameterSpec.Builder(
            key,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setIsStrongBoxBacked(false) /* Disable StrongBox */
        keyGenerator.init(builder.build())

        return keyGenerator.generateKey()
    }


    private fun getKeyFromKeyStore(key: String): SecretKey {
        return keyStore.getKey(key, null) as SecretKey
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION)
    }
}
