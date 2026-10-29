package com.nhacongdong.runmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.DayEntry
import com.nhacongdong.runmate.domain.WeekRules
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.NhipDiagonal
import java.time.LocalDate
import java.time.ZoneId

// Danh sách kế hoạch và buổi chạy của ngày đang chọn (DESIGN 8.7). Phần duy nhất cuộn ở S1
@Composable
fun DayList(
    date: LocalDate,
    today: LocalDate,
    entries: List<DayEntry>,
    onOpenPlan: (Long) -> Unit,
    onViewResult: (Long) -> Unit,
    onPlanDay: (LocalDate) -> Unit,
    onRunNow: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    LazyColumn(
        modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        item(key = "header") { DayHeader(date, entries) }
        if (entries.isEmpty()) {
            item(key = "empty") { DayEmpty(date, today, onPlanDay, onRunNow) }
        } else {
            items(entries, key = { it.key() }) { entry ->
                when (entry) {
                    is DayEntry.PlanEntry -> PlanCard(entry.plan, onClick = { onOpenPlan(entry.plan.id) }, zone = zone)
                    is DayEntry.ResultEntry -> ResultCard(
                        entry,
                        // Bấm cả thẻ: kế hoạch mở S4, chạy tự do mở S7 (DESIGN 8.6)
                        onOpen = { entry.plan?.let { onOpenPlan(it.id) } ?: onViewResult(entry.session.id) },
                        onViewResult = { onViewResult(entry.session.id) },
                        zone = zone,
                    )
                }
            }
        }
    }
}

private fun DayEntry.key(): String = when (this) {
    is DayEntry.PlanEntry -> "plan-${plan.id}"
    is DayEntry.ResultEntry -> "run-${session.id}"
}

// "Thứ Năm, 29/10" và bên phải "2 buổi · 15,4 km" nếu ngày đó có buổi chạy
@Composable
private fun DayHeader(date: LocalDate, entries: List<DayEntry>) {
    val runs = entries.filterIsInstance<DayEntry.ResultEntry>()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            RunFormat.formatDayTitle(date), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f),
        )
        if (runs.isNotEmpty()) {
            Text(
                RunFormat.formatDaySummary(runs.size, runs.sumOf { it.session.distanceMeters }),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false,
            )
        }
    }
}

// Ngày trống: hôm nay / trong 16 ngày tới / đã qua / sau mốc 16 ngày (DESIGN 8.7)
@Composable
private fun DayEmpty(date: LocalDate, today: LocalDate, onPlanDay: (LocalDate) -> Unit, onRunNow: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val shape = MaterialTheme.shapes.medium
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
        when {
            date == today -> {
                Text(stringResource(R.string.day_empty_today), style = type.bodyMedium, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                    Button(onClick = { onPlanDay(date) }, shape = shape, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.action_plan), style = type.labelLarge, maxLines = 1, softWrap = false)
                    }
                    // Viền gradient 2 dp: nút "bấm để chạy" (DESIGN 2.2, 8.7)
                    OutlinedButton(
                        onClick = onRunNow, shape = shape, border = BorderStroke(2.dp, NhipDiagonal),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_run_now), style = type.labelLarge, maxLines = 1, softWrap = false)
                    }
                }
            }
            date.isBefore(today) ->
                Text(stringResource(R.string.day_empty_past), style = type.bodyMedium, color = colors.onSurfaceVariant)
            WeekRules.isPlannable(date, today) -> {
                Text(stringResource(R.string.day_empty_future), style = type.bodyMedium, color = colors.onSurfaceVariant)
                OutlinedButton(
                    onClick = { onPlanDay(date) }, shape = shape,
                    border = BorderStroke(1.dp, colors.outline), modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_plan_this_day), style = type.labelLarge, maxLines = 1, softWrap = false)
                }
            }
            else -> {
                Text(stringResource(R.string.day_empty_future), style = type.bodyMedium, color = colors.onSurfaceVariant)
                Text(stringResource(R.string.plan_limit), style = type.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

private val PreviewDay: LocalDate = LocalDate.of(2026, 10, 29)

@ThemePreviews
@Composable
private fun DayListPreview() = PreviewBox {
    val plan = previewPlan(PlanStatus.COMPLETED)
    DayList(
        date = PreviewDay, today = PreviewDay,
        entries = listOf(
            DayEntry.ResultEntry(plan, previewSession(planId = 1, distanceMeters = 5_240.0)),
            DayEntry.ResultEntry(null, previewSession(planId = null, distanceMeters = 3_100.0, hour = 17, minute = 30, id = 2)),
            DayEntry.PlanEntry(previewPlan(PlanStatus.PLANNED, hour = 19, location = "Công viên Thống Nhất").copy(id = 3)),
        ),
        onOpenPlan = {}, onViewResult = {}, onPlanDay = {}, onRunNow = {}, zone = PreviewZone,
    )
}

@ThemePreviews
@Composable
private fun DayListEmptyPreview() = PreviewBox {
    val noop: (LocalDate) -> Unit = {}
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space5)) {
        // Hôm nay, ngày trong 16 ngày tới, ngày đã qua, ngày sau mốc 16 ngày
        listOf(PreviewDay, PreviewDay.plusDays(3), PreviewDay.minusDays(2), PreviewDay.plusDays(17)).forEach { date ->
            DayList(date, PreviewDay, emptyList(), {}, {}, noop, {}, Modifier.heightIn(max = 160.dp))
        }
    }
}

