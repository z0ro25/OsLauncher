package com.ezla.oslauncher.themes.Api

import com.ezla.oslauncher.themes.Models.ApiResponse
import com.ezla.oslauncher.themes.Models.AddDeviceRequest
import com.ezla.oslauncher.themes.Models.AddDeviceData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Endpoint server launcher-os. Thêm API theme vào đây; token tự gắn qua AuthInterceptor.
interface ThemesApi {

    @POST("auth/add-device")
    suspend fun addDevice(@Body body: AddDeviceRequest): Response<ApiResponse<AddDeviceData>>
}
