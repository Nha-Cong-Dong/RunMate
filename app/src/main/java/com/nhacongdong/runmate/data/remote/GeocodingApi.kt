package com.nhacongdong.runmate.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

// Open-Meteo Geocoding API (SPEC 8.2). Dùng khi Geocoder của máy không có sẵn hoặc lỗi.
// Khi không có kết quả, API không trả trường "results" (results = null).
interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("language") language: String
    ): GeocodingResponse
}

@Serializable
data class GeocodingResponse(val results: List<GeocodingResult>? = null)

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null,
    val country: String? = null
)
