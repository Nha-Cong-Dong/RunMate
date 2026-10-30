package com.nhacongdong.runmate.data.place

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.nhacongdong.runmate.domain.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Tìm địa điểm bằng Geocoder của máy (SPEC 8.2). Android 13 trở lên dùng API bất đồng bộ.
// Bản cũ hơn dùng hàm chặn nên phải chạy trên Dispatchers.IO.
class GeocoderPlaceSearch(private val geocoder: Geocoder) : PlaceSearch {

    override suspend fun search(query: String): List<Place> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            searchAsync(query)
        } else {
            withContext(Dispatchers.IO) { searchBlocking(query) }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun searchAsync(query: String): List<Place> = suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocationName(query, MAX_RESULTS, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                continuation.resume(addresses.mapNotNull { it.toPlace() })
            }

            override fun onError(errorMessage: String?) {
                continuation.resumeWithException(IOException(errorMessage ?: "Geocoder lỗi"))
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun searchBlocking(query: String): List<Place> =
        geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty().mapNotNull { it.toPlace() }

    // Bỏ kết quả không có tên. Tên hiển thị lấy từ featureName, dòng phụ là quận và thành phố
    private fun Address.toPlace(): Place? {
        val placeName = featureName ?: locality ?: return null
        return Place(
            name = placeName,
            subtitle = listOfNotNull(subLocality ?: locality, adminArea).distinct().joinToString(", "),
            latitude = latitude,
            longitude = longitude
        )
    }

    private companion object {
        const val MAX_RESULTS = 10
    }
}
