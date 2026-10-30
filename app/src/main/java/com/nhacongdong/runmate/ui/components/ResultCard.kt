package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.DayEntry
import com.nhacongdong.runmate.domain.DayRingState
import com.nhacongdong.runmate.domain.SessionResult
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.RunRingSizes
import com.nhacongdong.runmate.ui.theme.tabular
import java.time.ZoneId

// Thẻ buổi đã chạy: kế hoạch COMPLETED hoặc chạy tự do (DESIGN 8.6).
// onOpen: bấm cả thẻ, mở S7 cho cả kế hoạch đã hoàn thành và chạy tự do. onViewResult: nút "Xem kết quả" mở S7
@Composable
fun ResultCard(
    entry: DayEntry.ResultEntry,
    onOpen: () -> Unit,
    onViewResult: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val session = entry.session
    val reached = SessionResult.isGoalReached(session)

    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainer,
    ) {
        Column(Modifier.heightIn(min = 72.dp).padding(start = Dimens.space4, top = Dimens.space4, end = Dimens.space2)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RunRing(DayRingState.Ran(1, reached, session.id), RunRingSizes.M)
                Column(Modifier.weight(1f).padding(horizontal = Dimens.space3)) {
                    // Dòng 1: giờ bắt đầu chạy · địa điểm hoặc "Chạy tự do"
                    Text(
                        stringResource(
                            R.string.time_and_place,
                            RunFormat.formatTime(session.startedAt, zone),
                            entry.plan?.locationName ?: stringResource(R.string.free_run),
                        ),
                        style = type.titleMedium.tabular(), color = colors.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    // Dòng 2: quãng đường · thời lượng bằng chữ, giống nhau ở cả hai loại thẻ
                    Text(
                        stringResource(
                            R.string.distance_and_duration,
                            RunFormat.formatDistanceKm(session.distanceMeters),
                            RunFormat.formatDurationWords(session.activeDurationSec),
                        ),
                        style = type.bodyMedium.tabular(), color = colors.onSurfaceVariant,
                        maxLines = 1, softWrap = false,
                    )
                }
                // Chạy tự do không có chip (SPEC 6)
                if (entry.plan != null) StatusChip(PlanStatus.COMPLETED)
            }
            // Hàng dưới: tỷ lệ mục tiêu bên trái (thẳng cột với chữ ở trên), "Xem kết quả" bên phải.
            // Không thêm dòng nên hai loại thẻ luôn cao bằng nhau
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).padding(start = RunRingSizes.M.outer + Dimens.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (entry.plan != null) {
                        Text(
                            stringResource(R.string.goal_percent, RunFormat.formatPercent(SessionResult.goalRatio(session))),
                            style = type.bodyMedium.tabular(),
                            color = if (reached) RunMateTheme.colors.success else colors.onSurfaceVariant,
                            maxLines = 1, softWrap = false,
                        )
                        if (reached) {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.cd_goal_reached),
                                tint = RunMateTheme.colors.success,
                                modifier = Modifier.padding(start = Dimens.space1).size(16.dp),
                            )
                        }
                    }
                }
                TextButton(onClick = onViewResult, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.view_result), style = type.labelLarge, maxLines = 1, softWrap = false)
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ResultCardPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        val plan = previewPlan(PlanStatus.COMPLETED)
        ResultCard(DayEntry.ResultEntry(plan, previewSession(planId = 1, distanceMeters = 5_240.0)), {}, {}, zone = PreviewZone)
        ResultCard(DayEntry.ResultEntry(plan, previewSession(planId = 1, distanceMeters = 3_870.0)), {}, {}, zone = PreviewZone)
        ResultCard(
            DayEntry.ResultEntry(null, previewSession(planId = null, distanceMeters = 3_100.0, hour = 17, minute = 30, activeDurationSec = 1_145)),
            {}, {}, zone = PreviewZone,
        )
    }
}
