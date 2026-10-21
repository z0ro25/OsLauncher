package com.ezla.oslauncher.themes.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.ezla.oslauncher.themes.Models.Credit
import com.ezla.oslauncher.themes.Models.SubsModel
import com.ezla.oslauncher.themes.Models.Unlock
import com.google.gson.Gson
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val payLoadKey = "b50b04fb046868b4bc808f5b2510aa9c"
    private const val Key = "abcdfhjagdshj@12"

    private const val IV_SIZE = 16
    private val secureRandom = SecureRandom()
    private val secretKey by lazy { SecretKeySpec(payLoadKey.toByteArray(), "AES") }

    @SuppressLint("HardwareIds")
    fun getDeviceID(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getBase64DecryptConfig(config: String): String? {
        return try {
            val rawConfig = decrypt(config)
            android.util.Base64.encodeToString(
                rawConfig.toByteArray(),
                android.util.Base64.DEFAULT
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun encryptBytes(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plain)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return combined
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        }
    }

    fun encryptCredit(model: Credit): String =
        encodeBase64(encryptBytes(Gson().toJson(model).toByteArray()))

    fun encryptUnlock(model: Unlock): String =
        encodeBase64(encryptBytes(Gson().toJson(model).toByteArray()))

    fun encryptSubs(model: SubsModel): String =
        encodeBase64(encryptBytes(Gson().toJson(model).toByteArray()))

    @Throws(Exception::class)
    fun decrypt(encoded: String): String {
        val combined = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        require(combined.size > IV_SIZE) { "Encrypted payload too short" }
        val iv = ByteArray(IV_SIZE)
        val encrypted = ByteArray(combined.size - iv.size)
        System.arraycopy(combined, 0, iv, 0, iv.size)
        System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return String(cipher.doFinal(encrypted))
    }
}
