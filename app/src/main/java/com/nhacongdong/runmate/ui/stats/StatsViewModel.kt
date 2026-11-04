package com.nhacongdong.runmate.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.data.local.RunPlan
import com.nhacongdong.runmate.data.local.RunSession
import com.nhacongdong.runmate.domain.PeriodSummary
import com.nhacongdong.runmate.domain.StatsPeriod
import com.nhacongdong.runmate.domain.StatsRules
import com.nhacongdong.runmate.ui.components.BarChartItem
import com.nhacongdong.runmate.ui.format.RunFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

// Trạng thái giao diện màn hình S8 (SPEC 9.1, DESIGN 9 S8)
data class StatsUiState(
    val period: StatsPeriod,
    val periodTitle: String,             // Chuỗi hiển thị ở bộ chuyển kỳ (ví dụ: "26/10 – 01/11, 2026" hoặc "Tháng 10, 2026")
    val summary: PeriodSummary,          // 4 số liệu ô (Tổng km, Số buổi, Thời gian, Pace TB) và tỷ lệ hoàn thành
    val barItems: List<BarChartItem>,    // 7 cột cho kỳ Tuần hoặc 4–5 cột cho kỳ Tháng
    val canGoNext: Boolean,              // Chặn nút tiến khi đang ở tuần/tháng hiện tại
    val isWeekTab: Boolean,              // true: tab Tuần, false: tab Tháng
    val isEmptyPeriod: Boolean,          // true: kỳ này không có buổi chạy nào
    val isLoading: Boolean = false,
    val runningDays: Set<LocalDate> = emptySet(), // Kỳ Tháng: các ngày có buổi chạy đã lưu, để vẽ chấm trên lịch
)

// ViewModel quản lý số liệu thống kê ở màn hình S8
class StatsViewModel(
    private val planRepository: PlanRepository,
    private val sessionRepository: SessionRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val currentPeriod = MutableStateFlow<StatsPeriod>(StatsPeriod.currentWeek(todayProvider()))

    val uiState: StateFlow<StatsUiState> = combine(
        currentPeriod,
        planRepository.observePlans(),
        sessionRepository.observeFinishedSessions(),
    ) { period, plans, sessions ->
        buildUiState(period, plans, sessions, todayProvider(), zone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildInitialState(todayProvider(), zone),
    )

    // Chọn tab Tuần: chuyển về tuần hiện tại
    fun selectWeekTab() {
        currentPeriod.update { StatsPeriod.currentWeek(todayProvider()) }
    }

    // Chọn tab Tháng: chuyển về tháng hiện tại
    fun selectMonthTab() {
        currentPeriod.update { StatsPeriod.currentMonth(todayProvider()) }
    }

    // Lùi 1 kỳ (tuần trước hoặc tháng trước)
    fun previousPeriod() {
        currentPeriod.update { it.previous() }
    }

    // Tiến 1 kỳ (tuần sau hoặc tháng sau, chặn nếu đã là tuần/tháng hiện tại)
    fun nextPeriod() {
        val now = todayProvider()
        currentPeriod.update { if (it.canGoNext(now)) it.next() else it }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunMateApplication
                StatsViewModel(
                    planRepository = app.container.planRepository,
                    sessionRepository = app.container.sessionRepository,
                )
            }
        }

        private fun buildInitialState(today: LocalDate, zone: ZoneId): StatsUiState {
            val week = StatsPeriod.currentWeek(today)
            val emptySummary = PeriodSummary(
                sessionCount = 0,
                totalDistanceM = 0.0,
                totalDurationSec = 0L,
                avgPaceSecPerKm = null,
                completedPlans = 0,
                missedPlans = 0,
                planCompletionRate = null,
            )
            return StatsUiState(
                period = week,
                periodTitle = formatPeriodTitle(week),
                summary = emptySummary,
                barItems = emptyList(),
                canGoNext = false,
                isWeekTab = true,
                isEmptyPeriod = true,
                isLoading = true,
            )
        }

        private fun buildUiState(
            period: StatsPeriod,
            plans: List<RunPlan>,
            sessions: List<RunSession>,
            today: LocalDate,
            zone: ZoneId,
        ): StatsUiState {
            val summary = StatsRules.summarize(period, plans, sessions, zone)
            val barItems = when (period) {
                is StatsPeriod.Week -> {
                    StatsRules.dailyDistances(period, sessions, zone).map { day ->
                        BarChartItem(
                            label = RunFormat.formatWeekday(day.date),
                            subLabel = "%02d/%02d".format(day.date.dayOfMonth, day.date.monthValue),
                            distanceMeters = day.distanceMeters,
                        )
                    }
                }
                is StatsPeriod.Month -> {
                    StatsRules.weeklyDistances(period, sessions, zone).mapIndexed { index, week ->
                        BarChartItem(
                            label = "Tuần ${index + 1}",
                            subLabel = "%02d/%02d – %02d/%02d".format(
                                week.startDate.dayOfMonth, week.startDate.monthValue,
                                week.endDate.dayOfMonth, week.endDate.monthValue,
                            ),
                            distanceMeters = week.distanceMeters,
                        )
                    }
                }
            }

            val runningDays = when (period) {
                is StatsPeriod.Month -> StatsRules.runningDaysInMonth(period, sessions, zone)
                is StatsPeriod.Week -> emptySet()
            }

            return StatsUiState(
                period = period,
                periodTitle = formatPeriodTitle(period),
                summary = summary,
                barItems = barItems,
                runningDays = runningDays,
                canGoNext = period.canGoNext(today),
                isWeekTab = period is StatsPeriod.Week,
                isEmptyPeriod = summary.sessionCount == 0,
                isLoading = false,
            )
        }

        // Định dạng tiêu đề kỳ ở bộ chuyển kỳ ‹ ... ›
        fun formatPeriodTitle(period: StatsPeriod): String = when (period) {
            is StatsPeriod.Week -> {
                val start = period.startOfWeek
                val end = period.endOfWeek
                if (start.year == end.year) {
                    "%02d/%02d – %02d/%02d, %d".format(
                        start.dayOfMonth, start.monthValue,
                        end.dayOfMonth, end.monthValue,
                        end.year,
                    )
                } else {
                    "%02d/%02d, %d – %02d/%02d, %d".format(
                        start.dayOfMonth, start.monthValue, start.year,
                        end.dayOfMonth, end.monthValue, end.year,
                    )
                }
            }
            is StatsPeriod.Month -> {
                "Tháng %d, %d".format(period.yearMonth.monthValue, period.yearMonth.year)
            }
        }
    }
}
