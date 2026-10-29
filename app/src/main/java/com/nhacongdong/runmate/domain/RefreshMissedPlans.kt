package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.PlanRepository
import kotlinx.coroutines.flow.first

// SPEC 6.3: kế hoạch PLANNED đã quá giờ (BR-09) thì chuyển sang MISSED.
// Gọi khi mở app và mỗi khi hiển thị S1, S4.
class RefreshMissedPlans(private val planRepository: PlanRepository) {
    suspend operator fun invoke(now: Long) {
        // Đọc danh sách hiện tại một lần. Trạng thái mới do PlanRules quyết định, không ghi cứng ở đây
        planRepository.observePlans().first().forEach { plan ->
            val newStatus = PlanRules.refreshStatus(plan.status, plan.startTime, now)
            if (newStatus != plan.status) planRepository.updateStatus(plan.id, newStatus)
        }
    }
}
