package com.nhacongdong.runmate.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.PlanRepository
import com.nhacongdong.runmate.data.SessionRepository
import com.nhacongdong.runmate.domain.DayAgenda
import com.nhacongdong.runmate.domain.DayData
import com.nhacongdong.runmate.domain.DayEntry
import com.nhacongdong.runmate.domain.RefreshMissedPlans
import com.nhacongdong.runmate.domain.WeekRules
import com.nhacongdong.runmate.domain.dayRingState
import com.nhacongdong.runmate.ui.components.CalendarDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Tuần đang xem và ngày đang chọn trên S1
private data class Navigation(val today: LocalDate, val weekStart: LocalDate, val selected: LocalDate)

// Mọi thứ S1 cần để vẽ (SPEC 9.1, DESIGN 9.1)
data class PlanListUiState(
    val today: LocalDate,
    val weekStart: LocalDate,
    val selected: LocalDate,
    val days: List<CalendarDay> = emptyList(),     // 7 ngày của tuần đang xem
    val entries: List<DayEntry> = emptyList(),     // thẻ của ngày đang chọn
    val canGoNext: Boolean = true,                 // BR-02
    val isFirstUse: Boolean = false,               // chưa có kế hoạch hay buổi chạy nào (DESIGN 8.19)
    val isLoading: Boolean = true,
)

class PlanListViewModel(
    planRepository: PlanRepository,
    sessionRepository: SessionRepository,
    private val refreshMissedPlans: RefreshMissedPlans,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val navigation = MutableStateFlow(today().let { Navigation(it, WeekRules.weekStartOf(it), it) })

    // Giai đoạn D (D7): thêm buổi đang RECORDING/PAUSED để vòng "đang chạy" hiện được
    val uiState: StateFlow<PlanListUiState> = combine(
        navigation, planRepository.observePlans(), sessionRepository.observeFinishedSessions(),
    ) { nav, plans, sessions ->
        val byDay = DayAgenda.groupByDay(plans, sessions, zone)
        PlanListUiState(
            today = nav.today,
            weekStart = nav.weekStart,
            selected = nav.selected,
            days = WeekRules.weekDays(nav.weekStart).map { date ->
                val day = byDay[date] ?: DayData()
                CalendarDay(date, dayRingState(day), day.plans.size)
            },
            entries = DayAgenda.dayEntries(byDay[nav.selected] ?: DayData()),
            canGoNext = nav.weekStart < WeekRules.lastAllowedWeekStart(nav.today),
            isFirstUse = plans.isEmpty() && sessions.isEmpty(),
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        navigation.value.let { PlanListUiState(it.today, it.weekStart, it.selected) },
    )

    fun selectDate(date: LocalDate) = navigation.update { it.copy(selected = date) }

    fun prevWeek() = moveWeek(-1)

    // Nút › đã tắt ở tuần giới hạn, kiểm tra lại ở đây cho chắc (BR-02)
    fun nextWeek() {
        val nav = navigation.value
        if (nav.weekStart < WeekRules.lastAllowedWeekStart(nav.today)) moveWeek(1)
    }

    fun goToday() = navigation.update { Navigation(it.today, WeekRules.weekStartOf(it.today), it.today) }

    // Gọi mỗi khi S1 hiện lên (ON_RESUME): cập nhật hôm nay nếu đã qua nửa đêm, rồi đổi kế hoạch quá giờ sang MISSED (SPEC 6.3)
    fun refreshStatuses() {
        val now = clock()
        val today = DayAgenda.dateOf(now, zone)
        navigation.update { nav ->
            // Đang xem hôm nay cũ thì đi theo sang hôm nay mới
            if (nav.today != today && nav.selected == nav.today) Navigation(today, WeekRules.weekStartOf(today), today)
            else nav.copy(today = today)
        }
        viewModelScope.launch { refreshMissedPlans(now) }
    }

    // Chuyển tuần, giữ nguyên thứ đang chọn (đang chọn T5 thì sang T5 tuần kia)
    private fun moveWeek(weeks: Long) = navigation.update {
        val weekStart = it.weekStart.plusWeeks(weeks)
        it.copy(weekStart = weekStart, selected = weekStart.plusDays(ChronoUnit.DAYS.between(it.weekStart, it.selected)))
    }

    private fun today(): LocalDate = DayAgenda.dateOf(clock(), zone)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                PlanListViewModel(container.planRepository, container.sessionRepository, container.refreshMissedPlans)
            }
        }
    }
}
