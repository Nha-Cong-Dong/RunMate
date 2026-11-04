package com.nhacongdong.runmate.ui.stats

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.PeriodSummary
import com.nhacongdong.runmate.domain.StatsPeriod
import com.nhacongdong.runmate.ui.components.BarChartItem
import com.nhacongdong.runmate.ui.components.PlanCompletionBar
import com.nhacongdong.runmate.ui.components.StatTile
import com.nhacongdong.runmate.ui.components.StatsBarChart
import com.nhacongdong.runmate.ui.components.StatsMonthCalendar
import com.nhacongdong.runmate.ui.format.RunFormat
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.LocalDate
import java.time.YearMonth

// Route nối màn hình S8 với StatsViewModel
@Composable
fun StatsRoute(
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        state = state,
        onSelectWeek = viewModel::selectWeekTab,
        onSelectMonth = viewModel::selectMonthTab,
        onPrevPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
    )
}

// S8 · Thống kê (SPEC 9.1, DESIGN 9 S8)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onSelectWeek: () -> Unit,
    onSelectMonth: () -> Unit,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.space4),
        ) {
            // 1. Bộ chuyển đổi Tab (Tuần / Tháng)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.isWeekTab,
                    onClick = onSelectWeek,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    label = { Text(stringResource(R.string.stats_tab_week)) },
                )
                SegmentedButton(
                    selected = !state.isWeekTab,
                    onClick = onSelectMonth,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    label = { Text(stringResource(R.string.stats_tab_month)) },
                )
            }

            Spacer(Modifier.height(Dimens.space1))

            // 2. Bộ chuyển kỳ (‹ khoảng ngày/tháng ›)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.space1),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevPeriod) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = stringResource(R.string.cd_prev_week),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = state.periodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                IconButton(
                    onClick = onNextPeriod,
                    enabled = state.canGoNext,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = stringResource(R.string.cd_next_week),
                        tint = if (state.canGoNext) MaterialTheme.colorScheme.onSurface else RunMateTheme.colors.textDisabled,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.space2))

            // 3. Lưới 4 ô số liệu chính (2 hàng × 2 cột)
            // Hàng 1: Tổng quãng đường & Số buổi chạy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
            ) {
                StatTile(
                    label = stringResource(R.string.stats_total_distance),
                    value = RunFormat.formatDistanceKm(state.summary.totalDistanceM),
                    unit = "km",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.stats_total_sessions),
                    value = "${state.summary.sessionCount}",
                    unit = "buổi",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Dimens.space3))

            // Hàng 2: Tổng thời gian & Pace trung bình
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
            ) {
                val durationText = if (state.summary.totalDurationSec > 0) {
                    RunFormat.formatDuration(state.summary.totalDurationSec)
                } else {
                    "0:00"
                }
                StatTile(
                    label = stringResource(R.string.stats_total_duration),
                    value = durationText,
                    modifier = Modifier.weight(1f),
                )
                val paceText = state.summary.avgPaceSecPerKm?.let {
                    RunFormat.formatDuration(it.toLong())
                } ?: "--"
                StatTile(
                    label = stringResource(R.string.stats_avg_pace),
                    value = paceText,
                    unit = if (state.summary.avgPaceSecPerKm != null) "/km" else null,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Dimens.space4))

            // 4. Thanh hoàn thành kế hoạch
            PlanCompletionBar(
                rate = state.summary.planCompletionRate,
                completed = state.summary.completedPlans,
                missed = state.summary.missedPlans,
            )

            Spacer(Modifier.height(Dimens.space5))

            // 5. Kỳ Tuần: biểu đồ cột theo ngày. Kỳ Tháng: lịch có chấm và quãng đường từng tuần
            val period = state.period
            if (period is StatsPeriod.Month) {
                StatsMonthCalendar(yearMonth = period.yearMonth, runningDays = state.runningDays)
            } else {
                StatsBarChart(items = state.barItems)
            }

            // 6. Trạng thái kỳ trống
            if (state.isEmptyPeriod) {
                Spacer(Modifier.height(Dimens.space3))
                Text(
                    text = stringResource(R.string.stats_empty_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.space2),
                )
            }

            Spacer(Modifier.height(Dimens.space5))
        }
    }
}

@Preview(name = "Màn hình S8 · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Màn hình S8 · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatsScreenPreview() = RunMateTheme {
    val sampleSummary = PeriodSummary(
        sessionCount = 5,
        totalDistanceM = 32_600.0,
        totalDurationSec = 10_260L, // 2 giờ 51 phút
        avgPaceSecPerKm = 315.0,     // 5:15 /km
        completedPlans = 4,
        missedPlans = 1,
        planCompletionRate = 80,
    )
    val sampleItems = listOf(
        BarChartItem("T2", "26/10", 5_200.0),
        BarChartItem("T3", "27/10", 0.0),
        BarChartItem("T4", "28/10", 10_000.0),
        BarChartItem("T5", "29/10", 0.0),
        BarChartItem("T6", "30/10", 9_400.0),
        BarChartItem("T7", "31/10", 0.0),
        BarChartItem("CN", "01/11", 8_000.0),
    )
    StatsScreen(
        state = StatsUiState(
            period = StatsPeriod.Week(LocalDate.of(2026, 10, 26)),
            periodTitle = "26/10 – 01/11, 2026",
            summary = sampleSummary,
            barItems = sampleItems,
            canGoNext = false,
            isWeekTab = true,
            isEmptyPeriod = false,
        ),
        onSelectWeek = {},
        onSelectMonth = {},
        onPrevPeriod = {},
        onNextPeriod = {},
    )
}

@Preview(name = "Màn hình S8 Tháng · Sáng", widthDp = 360, showBackground = true)
@Preview(name = "Màn hình S8 Tháng · Tối", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatsScreenMonthPreview() = RunMateTheme {
    val sampleSummary = PeriodSummary(
        sessionCount = 18,
        totalDistanceM = 112_400.0,
        totalDurationSec = 36_000L, // 10 giờ
        avgPaceSecPerKm = 320.0,     // 5:20 /km
        completedPlans = 15,
        missedPlans = 2,
        planCompletionRate = 88,
    )
    val sampleMonthItems = listOf(
        BarChartItem("Tuần 1", "01 – 04/10", 12_500.0),
        BarChartItem("Tuần 2", "05 – 11/10", 25_000.0),
        BarChartItem("Tuần 3", "12 – 18/10", 0.0),
        BarChartItem("Tuần 4", "19 – 25/10", 18_200.0),
        BarChartItem("Tuần 5", "26 – 31/10", 15_000.0),
    )
    StatsScreen(
        state = StatsUiState(
            period = StatsPeriod.Month(YearMonth.of(2026, 10)),
            periodTitle = "Tháng 10, 2026",
            summary = sampleSummary,
            barItems = sampleMonthItems,
            canGoNext = false,
            isWeekTab = false,
            isEmptyPeriod = false,
        ),
        onSelectWeek = {},
        onSelectMonth = {},
        onPrevPeriod = {},
        onNextPeriod = {},
    )
}
