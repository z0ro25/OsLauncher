package com.ezla.oslauncher.themes.Models

import com.google.gson.annotations.SerializedName

// Envelope chung của server: { "data": ..., "status": 200 }.
data class ApiResponse<T>(
    @SerializedName("data") val data: T?,
    @SerializedName("status") val status: Int
)
