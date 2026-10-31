package com.nhacongdong.runmate.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// Nhận vị trí liên tục khi đang ghi buổi chạy (SPEC 10.2, BR-32). Chỉ bọc Fused Location Provider
class TrackingLocationSource(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    // Gọi khi đã có quyền vị trí. Mỗi điểm mới được đưa cho onLocation
    @SuppressLint("MissingPermission")
    fun start(onLocation: (Location) -> Unit) {
        stop()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS).build()
        val newCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(onLocation)
            }
        }
        callback = newCallback
        client.requestLocationUpdates(request, newCallback, Looper.getMainLooper())
    }

    // Dừng nhận vị trí, dùng khi tạm dừng và khi kết thúc
    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }

    companion object {
        const val INTERVAL_MS = 2_000L   // BR-32: khoảng 2 giây một điểm
    }
}
