package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Envelope chung của server: { "data": ..., "status": 200 }.
data class ApiResponse<T>(
    @SerializedName("data") val data: T?,
    @SerializedName("status") val status: Int
)

// Body add-device: secret = EncryptionUtil.encrypt(context) (client_id/platform/package_id/time đã mã hoá).
data class AddDeviceRequest(
    @SerializedName("secret") val secret: String
)

data class AddDeviceData(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("device") val device: DeviceInfo?
)

data class DeviceInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("client_id") val clientId: String?
)
