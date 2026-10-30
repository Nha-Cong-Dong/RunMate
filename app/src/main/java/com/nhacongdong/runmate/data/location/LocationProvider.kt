package com.nhacongdong.runmate.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Lấy vị trí hiện tại một lần cho "Dùng vị trí hiện tại" ở S3 (SPEC 8.2).
// Không dùng để ghi buổi chạy, việc đó do Service của F3 làm.
class LocationProvider(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    // Gọi khi người dùng đã cấp quyền vị trí. Chưa có quyền thì hàm ném SecurityException.
    // Trả về null khi Google Play services không có vị trí nào.
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
            .addOnSuccessListener { location -> continuation.resume(location) }
            .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
}
