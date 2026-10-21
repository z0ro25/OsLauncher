package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Payload mã hoá thành "data" của POST /credits và /credits/consume. time = epoch GIÂY.
data class Credit(
    @SerializedName("amount") val amount: Int,
    @SerializedName("type") val type: String,
    @SerializedName("time") val time: Long
)

// Payload mã hoá thành "data" của POST /unlocks. type: "theme_icons" (id = theme) hoặc
// "background" (id = background). time = epoch GIÂY.
data class Unlock(
    @SerializedName("type") val type: String,
    @SerializedName("unlockable_id") val unlockableId: Int,
    @SerializedName("time") val time: Long
)

// Payload mã hoá thành "data" của POST /device/sub. is_sub: 1 = đang subs, 0 = hết. time = epoch GIÂY.
data class SubsModel(
    @SerializedName("is_sub") val isSub: Int,
    @SerializedName("time") val time: Long
)

// data của POST /device/sub: {status, is_sub}; bị từ chối thì {error}.
data class SubsData(
    @SerializedName("status") val status: String? = null,
    @SerializedName("is_sub") val isSub: Int? = null,
    @SerializedName("error") val error: String? = null
)

// Body POST credit / unlock / device/sub: { "data": chuỗi mã hoá từ EncryptionUtil }.
data class CreditRequest(
    @SerializedName("data") val data: String
)

// data của GET /credits ({credit}) và POST ({status, credit}); credit = số dư SAU thao tác.
// Bị từ chối thì server trả {error} (vd "Update failed or not enough credit"), không có credit.
data class CreditData(
    @SerializedName("credit") val credit: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("error") val error: String? = null
)
