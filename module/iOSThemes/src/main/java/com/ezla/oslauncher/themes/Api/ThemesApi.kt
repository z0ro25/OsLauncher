package com.ezla.oslauncher.themes.Api

import com.ezla.oslauncher.themes.Models.ApiResponse
import com.ezla.oslauncher.themes.Models.CategoryPage
import com.ezla.oslauncher.themes.Models.ThemePage
import retrofit2.Response
import retrofit2.http.GET
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
}
