package com.nhacongdong.runmate.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.data.model.SessionStatus
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.LocalDateTime
import java.time.ZoneId

// Mỗi thành phần xem trước ở cả sáng và tối (DESIGN 13.1)
@Preview(name = "Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

@Composable
internal fun PreviewBox(content: @Composable () -> Unit) {
    RunMateTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(Dimens.space4)) { content() }
    }
}

// Dữ liệu mẫu cho Preview
internal val PreviewZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

internal fun previewMillis(hour: Int, minute: Int = 0): Long =
    LocalDateTime.of(2026, 10, 29, hour, minute).atZone(PreviewZone).toInstant().toEpochMilli()

internal fun previewPlan(
    status: PlanStatus,
    hour: Int = 6,
    location: String = "Hồ Tây",
    goalType: GoalType = GoalType.DISTANCE,
    goalValue: Int = 5_000,
) = RunPlan(
    id = 1, startTime = previewMillis(hour), locationName = location, latitude = 21.05, longitude = 105.82,
    goalType = goalType, goalValue = goalValue, status = status, createdAt = 0, weather = null,
)

internal fun previewSession(
    planId: Long?,
    distanceMeters: Double,
    hour: Int = 6,
    minute: Int = 12,
    activeDurationSec: Long = 1_615,
    id: Long = 1,
) = RunSession(
    id = id, planId = planId, status = SessionStatus.FINISHED, startedAt = previewMillis(hour, minute),
    locationName = "Hồ Tây", latitude = 21.05, longitude = 105.82, goalType = GoalType.DISTANCE,
    goalValue = 5_000, distanceMeters = distanceMeters, activeDurationSec = activeDurationSec, weather = null,
)
