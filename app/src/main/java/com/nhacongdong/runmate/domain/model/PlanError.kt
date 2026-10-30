package com.nhacongdong.runmate.domain.model

// Lỗi khi kiểm tra kế hoạch (SPEC 5.1).
enum class PlanError {
    START_IN_PAST,          // BR-01
    START_TOO_FAR,          // BR-02
    DISTANCE_OUT_OF_RANGE,  // BR-04
    DURATION_OUT_OF_RANGE   // BR-05
}
