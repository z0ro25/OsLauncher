package com.ezla.oslauncher.themes.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.ezla.oslauncher.themes.BuildConfig
import com.google.gson.Gson
import com.ezla.oslauncher.themes.Models.DeviceData
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object EncryptionUtil {
    private const val payLoadKey = "f0v132ca7c8f544258458b34d91df0s86f5scx"
    private const val Key = "abcdfhjagdshj@12"

    @SuppressLint("HardwareIds")
    fun getDeviceID(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getBase64DecryptConfig(config: String): String? {
        val rawConfig: String
        try {
            rawConfig = decrypt(config)
            return android.util.Base64.encodeToString(
                rawConfig.toByteArray(),
                android.util.Base64.DEFAULT
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }



    fun encrypt(context: Context): String = encryptJson(Gson().toJson(buildDeviceData(context)))

    // JSON payload CHƯA mã hoá, chỉ để log (ApiLog) đối chiếu với server.
    fun describePayload(context: Context): String = Gson().toJson(buildDeviceData(context))

    private fun buildDeviceData(context: Context): DeviceData {
        val data = DeviceData()
        try {
            data.apply {
                client_id = getDeviceID(context)
                platform = "1"
                package_id = BuildConfig.LIBRARY_PACKAGE_NAME
                time = System.currentTimeMillis()/1000L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    /** Mã hoá 1 object bất kỳ (JSON) thành chuỗi base64 để đưa vào body `{data:...}`. */
    fun encryptPayload(payload: Any): String = encryptJson(Gson().toJson(payload))

    /** AES/CBC/PKCS5 + IV rỗng, trả base64 (dùng chung cho mọi payload). */
    private fun encryptJson(json: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        val ivParams = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(payLoadKey.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams)
        val encrypted = cipher.doFinal(json.toByteArray())
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(combined)
        } else android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
    }

    @Throws(Exception::class)
    fun decrypt(encoded: String): String {
        var combined = ByteArray(0)
        combined = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        val iv = ByteArray(16)
        val encrypted = ByteArray(combined.size - iv.size)
        System.arraycopy(combined, 0, iv, 0, iv.size)
        System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivParams = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(payLoadKey.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted)
    }
}
