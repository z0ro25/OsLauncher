package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Payload mã hoá thành "secret" của add-device (xem EncryptionUtil.encrypt).
data class DeviceData(
    @SerializedName("client_id")
    var client_id: String = "",

    @SerializedName("platform")
    var platform: String = "1",

    @SerializedName("package_id")
    var package_id: String = "",

    @SerializedName("time")
    var time: Long  = 0L
)
