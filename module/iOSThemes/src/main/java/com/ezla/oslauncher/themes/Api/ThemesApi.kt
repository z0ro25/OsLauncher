package com.ezla.oslauncher.themes.Api

import com.ezla.oslauncher.themes.Models.ApiResponse
import com.ezla.oslauncher.themes.Models.BackgroundPage
import com.ezla.oslauncher.themes.Models.CategoryPage
import com.ezla.oslauncher.themes.Models.CreditData
import com.ezla.oslauncher.themes.Models.CreditRequest
import com.ezla.oslauncher.themes.Models.ThemePage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Endpoint nghiệp vụ của server launcher-os. Request tự được SAG ký (Bearer JWT + X-Timestamp/Nonce/
// Signature); app KHÔNG gọi add-device thủ công — SAG tự tạo device session ở request đầu.
interface ThemesApi {

    // GET /api/v1/categories — query fields/where/order_by theo server.
    @GET("categories")
    suspend fun getCategories(
        @Query("fields") fields: String,
        @Query("where") where: String,
        @Query("order_by") orderBy: String
    ): Response<ApiResponse<CategoryPage>>

    // GET /api/v1/themes — where lọc theo category_id của danh mục đang chọn.
    @GET("themes")
    suspend fun getThemes(
        @Query("fields") fields: String,
        @Query("where") where: String,
        @Query("order_by") orderBy: String
    ): Response<ApiResponse<ThemePage>>

    // GET /api/v1/my-themes — theme device đã mở khoá (cùng query + item như /themes).
    @GET("my-themes")
    suspend fun getMyThemes(
        @Query("fields") fields: String,
        @Query("where") where: String,
        @Query("order_by") orderBy: String
    ): Response<ApiResponse<ThemePage>>

    // GET /api/v1/my-backgrounds — nền device đã mở khoá.
    @GET("my-backgrounds")
    suspend fun getMyBackgrounds(
        @Query("fields") fields: String,
        @Query("where") where: String,
        @Query("order_by") orderBy: String
    ): Response<ApiResponse<BackgroundPage>>

    // GET /api/v1/credits — số dư xu hiện tại của device.
    @GET("credits")
    suspend fun getCredits(): Response<ApiResponse<CreditData>>

    // POST /api/v1/credits — cộng xu; body data = AES(Credit).
    @POST("credits")
    suspend fun addCredits(@Body body: CreditRequest): Response<ApiResponse<CreditData>>

    // POST /api/v1/credits/consume — trừ xu; body data = AES(Credit).
    @POST("credits/consume")
    suspend fun consumeCredits(@Body body: CreditRequest): Response<ApiResponse<CreditData>>

    // POST /api/v1/unlocks — mở khoá icon/nền (server tự trừ xu); body data = AES(Unlock).
    @POST("unlocks")
    suspend fun unlock(@Body body: CreditRequest): Response<ApiResponse<CreditData>>
}
