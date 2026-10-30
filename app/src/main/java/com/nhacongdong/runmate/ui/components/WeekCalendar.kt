package com.nhacongdong.runmate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.DayRingState
import com.nhacongdong.runmate.domain.WeekRules
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import com.nhacongdong.runmate.ui.theme.RunRingSizes
import com.nhacongdong.runmate.ui.theme.tabular
import java.time.LocalDate

// Dữ liệu của một ngày trên lịch tuần
data class CalendarDay(val date: LocalDate, val ring: DayRingState, val planCount: Int)

// Tuần đang hiện cùng dữ liệu của nó, để tuần cũ giữ đúng nội dung trong lúc trượt đi
private data class WeekContent(val weekStart: LocalDate, val days: Map<LocalDate, CalendarDay>)

// Lịch tuần ở đầu S1 (DESIGN 8.3)
@Composable
fun WeekCalendar(
    weekStart: LocalDate,
    days: List<CalendarDay>,
    selected: LocalDate,
    today: LocalDate,
    canGoNext: Boolean,                 // false ở tuần chứa hôm nay + 16 ngày (BR-02)
    onSelect: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    onNextBlocked: () -> Unit,          // màn hình hiện snackbar "Chỉ lên kế hoạch được trong 16 ngày tới"
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberReducedMotion()
    // Cỡ chữ hệ thống lớn thì ẩn chữ thứ, giữ vòng và số ngày (DESIGN 12)
    val showWeekday = LocalDensity.current.fontScale < 1.5f
    val next by rememberUpdatedState(if (canGoNext) onNextWeek else onNextBlocked)
    val prev by rememberUpdatedState(onPrevWeek)

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                RunFormat.formatMonthHeader(weekStart), style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f),
            )
            // Chip Hôm nay chỉ hiện khi đang ở tuần khác hoặc đang chọn ngày khác
            if (weekStart != WeekRules.weekStartOf(today) || selected != today) {
                AssistChip(
                    onClick = onToday,
                    label = { Text(stringResource(R.string.today), maxLines = 1, softWrap = false) },
                    shape = CircleShape,
                )
            }
            IconButton(onClick = onPrevWeek) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.cd_prev_week), tint = colors.onSurface)
            }
            // Nút › ở tuần giới hạn vẫn bấm được để hiện snackbar giải thích (DESIGN 8.3, 14)
            IconButton(onClick = next) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right), stringResource(R.string.cd_next_week),
                    tint = if (canGoNext) colors.onSurface else RunMateTheme.colors.textDisabled,
                )
            }
        }

        AnimatedContent(
            targetState = WeekContent(weekStart, days.associateBy { it.date }),
            contentKey = { it.weekStart },
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    // Trượt 250 ms theo hướng chuyển tuần (DESIGN 11)
                    val direction = if (targetState.weekStart > initialState.weekStart) 1 else -1
                    val spec = tween<IntOffset>(250, easing = FastOutSlowInEasing)
                    slideInHorizontally(spec) { it * direction } togetherWith slideOutHorizontally(spec) { -it * direction }
                }
            },
            label = "week",
            modifier = Modifier.pointerInput(Unit) {
                val threshold = Dimens.space7.toPx()
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > threshold) prev() else if (total < -threshold) next()
                    },
                ) { _, dragAmount -> total += dragAmount }
            },
        ) { week ->
            Row(Modifier.fillMaxWidth()) {
                WeekRules.weekDays(week.weekStart).forEach { date ->
                    DayColumn(
                        day = week.days[date] ?: CalendarDay(date, DayRingState.Empty, 0),
                        isSelected = date == selected,
                        isToday = date == today,
                        showWeekday = showWeekday,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Một cột: thứ, vòng chạy cỡ S, số ngày. Cả cột là một vùng bấm, đọc thành một câu (DESIGN 8.3, 12)
@Composable
private fun DayColumn(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    showWeekday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val description = dayDescription(day)
    val emphasize = isToday && !isSelected

    Column(
        modifier
            .heightIn(min = 48.dp)
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = description }
            .padding(vertical = Dimens.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.space1),
        ) {
            if (showWeekday) {
                Text(
                    RunFormat.formatWeekday(day.date), style = type.labelMedium,
                    color = if (isToday) colors.onSurface else colors.onSurfaceVariant,
                    fontWeight = if (emphasize) FontWeight.Bold else null,
                    maxLines = 1, softWrap = false,
                )
            }
            RunRing(day.ring, RunRingSizes.S)
            // Ngày đang chọn: số nằm trong viên inverse cao 24 dp
            Box(
                Modifier
                    .height(24.dp)
                    .then(if (isSelected) Modifier.background(colors.primary, CircleShape) else Modifier)
                    .padding(horizontal = Dimens.space2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%02d".format(day.date.dayOfMonth),
                    style = type.labelLarge.tabular(),
                    color = when {
                        isSelected -> colors.onPrimary
                        isToday -> colors.onSurface
                        else -> colors.onSurfaceVariant
                    },
                    fontWeight = if (emphasize) FontWeight.Bold else null,
                    maxLines = 1, softWrap = false,
                )
            }
        }
    }
}

// "Thứ Năm, 29/10, đã chạy 1 buổi, đạt mục tiêu" (DESIGN 8.3)
@Composable
private fun dayDescription(day: CalendarDay): String {
    val title = RunFormat.formatDayTitle(day.date)
    return when (val ring = day.ring) {
        DayRingState.Running -> stringResource(R.string.day_cd_running, title)
        is DayRingState.Ran ->
            if (ring.allGoalsReached) stringResource(R.string.day_cd_ran_reached, title, ring.runCount)
            else stringResource(R.string.day_cd_ran, title, ring.runCount)
        DayRingState.Planned -> stringResource(R.string.day_cd_plans, title, day.planCount)
        DayRingState.Missed -> stringResource(R.string.day_cd_missed, title, day.planCount)
        DayRingState.Empty -> stringResource(R.string.day_cd_empty, title)
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 29)

private fun previewDays(weekStart: LocalDate): List<CalendarDay> {
    val rings = listOf(
        DayRingState.Ran(1, true, 1), DayRingState.Missed, DayRingState.Ran(2, false, 2),
        DayRingState.Running, DayRingState.Planned, DayRingState.Empty, DayRingState.Planned,
    )
    return WeekRules.weekDays(weekStart).mapIndexed { i, d -> CalendarDay(d, rings[i], 1) }
}

@ThemePreviews
@Composable
private fun WeekCalendarPreview() = PreviewBox {
    val week = WeekRules.weekStartOf(PreviewToday)
    WeekCalendar(week, previewDays(week), PreviewToday, PreviewToday, true, {}, {}, {}, {}, {})
}

@ThemePreviews
@Composable
private fun WeekCalendarOtherDayPreview() = PreviewBox {
    val week = WeekRules.weekStartOf(PreviewToday)
    WeekCalendar(week, previewDays(week), PreviewToday.plusDays(2), PreviewToday, true, {}, {}, {}, {}, {})
}

@Preview(name = "Tuần giới hạn: nút › mờ", widthDp = 360, showBackground = true)
@Composable
private fun WeekCalendarLastWeekPreview() = PreviewBox {
    val week = WeekRules.lastAllowedWeekStart(PreviewToday)
    WeekCalendar(week, emptyList(), week, PreviewToday, false, {}, {}, {}, {}, {})
}

@Preview(name = "Cỡ chữ 200%: ẩn chữ thứ", widthDp = 360, showBackground = true, fontScale = 2f)
@Composable
private fun WeekCalendarLargeFontPreview() = PreviewBox {
    val week = WeekRules.weekStartOf(PreviewToday)
    WeekCalendar(week, previewDays(week), PreviewToday, PreviewToday, true, {}, {}, {}, {}, {})
}
