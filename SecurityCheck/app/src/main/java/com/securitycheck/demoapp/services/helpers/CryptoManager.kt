package com.securitycheck.demoapp.services.helpers

import android.content.Context
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(context: Context, key: String) {
    private val context: Context
    private val key: String

    init {
        this.context = context
        this.key = key
    }

    fun encrypt(strToEncrypt: String): ByteArray {
        val plainText = strToEncrypt.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val iv = generateRandomIV() // Generate a new IV for each encryption operation
        cipher.init(Cipher.ENCRYPT_MODE, generateKey(key), IvParameterSpec(iv))
        val cipherText = cipher.doFinal(plainText)

        // Combine the IV and ciphertext into a single byte array
        val encryptedData = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, encryptedData, 0, iv.size)
        System.arraycopy(cipherText, 0, encryptedData, iv.size, cipherText.size)

        return encryptedData
    }

    fun decrypt(dataToDecrypt: ByteArray): String {
        val iv = dataToDecrypt.copyOfRange(0, 16) // Extract the IV from the encrypted data
        val encryptedData = dataToDecrypt.copyOfRange(16, dataToDecrypt.size) // Extract the ciphertext
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, generateKey(key), IvParameterSpec(iv))
        val plainText = cipher.doFinal(encryptedData)

        return String(plainText, Charsets.UTF_8)
    }

    private fun generateKey(password: String): SecretKeySpec {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val bytes = password.toByteArray()
        digest.update(bytes, 0, bytes.size)
        val key = digest.digest()

        return SecretKeySpec(key, "AES")
    }

    private fun generateRandomIV(): ByteArray {
        val random = Random()
        val iv = ByteArray(16)
        random.nextBytes(iv)

        return iv
    }
}