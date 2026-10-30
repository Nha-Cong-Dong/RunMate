package com.nhacongdong.runmate.domain

import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.model.GoalType

// Kết quả so với mục tiêu (SPEC 5.3, BR-37). 1.05 nghĩa là 105%.
object SessionResult {
    fun goalRatio(goalType: GoalType, goalValue: Int, distanceMeters: Double, activeDurationSec: Long): Double =
        when (goalType) {
            GoalType.DISTANCE -> distanceMeters / goalValue
            GoalType.DURATION -> activeDurationSec.toDouble() / goalValue
        }

    fun goalRatio(session: RunSession): Double =
        goalRatio(session.goalType, session.goalValue, session.distanceMeters, session.activeDurationSec)

    // BR-37: đạt mục tiêu khi tỷ lệ ≥ 100%
    fun isGoalReached(session: RunSession): Boolean = goalRatio(session) >= 1.0
}
