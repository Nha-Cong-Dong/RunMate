package com.nhacongdong.runmate.domain.model

// Địa điểm tìm được ở S3 (SPEC 8.2, BR-06): tên hiển thị kèm toạ độ.
// Serializable để gửi từ S3 về S2 qua SavedStateHandle
data class Place(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
) : java.io.Serializable
