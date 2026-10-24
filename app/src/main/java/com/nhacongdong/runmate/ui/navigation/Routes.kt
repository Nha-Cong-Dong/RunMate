package com.nhacongdong.runmate.ui.navigation

import kotlinx.serialization.Serializable

// Mỗi màn hình trong SPEC mục 9 là một route

@Serializable data object PlanList                               // S1 (tab)
@Serializable data class PlanEdit(val planId: Long? = null)      // S2: planId null là tạo mới
@Serializable data object LocationPicker                         // S3
@Serializable data class PlanDetail(val planId: Long)            // S4
@Serializable data object FreeRunSetup                           // S5
@Serializable data object Running                                // S6: tối đa 1 buổi chạy (BR-08)
@Serializable data class SessionDetail(val sessionId: Long)      // S7
@Serializable data object History                                // S8 (tab)
