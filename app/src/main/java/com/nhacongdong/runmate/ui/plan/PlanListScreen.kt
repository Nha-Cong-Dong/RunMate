package com.nhacongdong.runmate.ui.plan

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.PlanStatus
import com.nhacongdong.runmate.domain.DayEntry
import com.nhacongdong.runmate.domain.DayRingState
import com.nhacongdong.runmate.domain.WeekRules
import com.nhacongdong.runmate.ui.components.CalendarDay
import com.nhacongdong.runmate.ui.components.DayList
import com.nhacongdong.runmate.ui.components.EmptyState
import com.nhacongdong.runmate.ui.components.PreviewZone
import com.nhacongdong.runmate.ui.components.WeekCalendar
import com.nhacongdong.runmate.ui.components.previewPlan
import com.nhacongdong.runmate.ui.components.previewSession
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

// S1 nối với ViewModel: đọc trạng thái, cập nhật MISSED mỗi lần màn hình hiện lại (SPEC 6.3)
@Composable
fun PlanListRoute(
    onCreatePlan: (LocalDate) -> Unit,
    onPlanDay: (LocalDate) -> Unit,
    onOpenPlan: (Long) -> Unit,
    onViewResult: (Long) -> Unit,
    onRunNow: () -> Unit,
    viewModel: PlanListViewModel = viewModel(factory = PlanListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshStatuses() }

    PlanListScreen(
        state = state,
        onCreatePlan = onCreatePlan,
        onSelectDate = viewModel::selectDate,
        onPrevWeek = viewModel::prevWeek,
        onNextWeek = viewModel::nextWeek,
        onToday = viewModel::goToday,
        onPlanDay = onPlanDay,
        onOpenPlan = onOpenPlan,
        onViewResult = onViewResult,
        onRunNow = onRunNow,
        onSwipeDay = viewModel::swipeDay,
    )
}

// S1 · Lịch (DESIGN 9 S1): thanh trên → WeekCalendar cố định → DayList cuộn
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanListScreen(
    state: PlanListUiState,
    onCreatePlan: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    onPlanDay: (LocalDate) -> Unit,
    onOpenPlan: (Long) -> Unit,
    onViewResult: (Long) -> Unit,
    onRunNow: () -> Unit,
    onSwipeDay: (Long) -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val limitMessage = stringResource(R.string.plan_limit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { onCreatePlan(state.selected) }) {
                        Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_create_plan))
                    }
                },
                // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            WeekCalendar(
                weekStart = state.weekStart,
                days = state.days,
                selected = state.selected,
                today = state.today,
                canGoNext = state.canGoNext,
                onSelect = onSelectDate,
                onPrevWeek = onPrevWeek,
                onNextWeek = onNextWeek,
                onToday = onToday,
                onNextBlocked = { scope.launch { snackbarHostState.showSnackbar(limitMessage) } },
                modifier = Modifier.padding(horizontal = Dimens.space4),
            )
            when {
                // Chưa có dữ liệu thì chưa vẽ gì bên dưới, tránh chớp EmptyState
                state.isLoading -> Unit
                state.isFirstUse -> EmptyState(
                    title = stringResource(R.string.first_use_title),
                    message = stringResource(R.string.first_use_message),
                    actionLabel = stringResource(R.string.action_plan_first),
                    onAction = { onCreatePlan(state.selected) },
                    modifier = Modifier.padding(horizontal = Dimens.space4),
                )
                else -> SwipeDayContainer(
                    selected = state.selected,
                    onSwipeDay = onSwipeDay,
                    modifier = Modifier.weight(1f),
                ) { date ->
                    DayList(
                        date = date,
                        today = state.today,
                        entries = state.entriesByDay[date].orEmpty(),
                        onOpenPlan = onOpenPlan,
                        onViewResult = onViewResult,
                        onPlanDay = onPlanDay,
                        onRunNow = onRunNow,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimens.space4, end = Dimens.space4, top = Dimens.space5, bottom = Dimens.space4,
                        ),
                        zone = zone,
                    )
                }
            }
        }
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 29)

private fun previewState(isFirstUse: Boolean): PlanListUiState {
    val weekStart = WeekRules.weekStartOf(PreviewToday)
    val rings = if (isFirstUse) List(7) { DayRingState.Empty } else listOf(
        DayRingState.Ran(1, true, 1), DayRingState.Missed, DayRingState.Empty,
        DayRingState.Ran(2, false, 1), DayRingState.Planned, DayRingState.Empty, DayRingState.Empty,
    )
    val plan = previewPlan(PlanStatus.COMPLETED)
    return PlanListUiState(
        today = PreviewToday,
        weekStart = weekStart,
        selected = PreviewToday,
        days = WeekRules.weekDays(weekStart).mapIndexed { i, d -> CalendarDay(d, rings[i], 1) },
        entries = if (isFirstUse) emptyList() else listOf(
            DayEntry.ResultEntry(plan, previewSession(planId = 1, distanceMeters = 5_240.0)),
            DayEntry.ResultEntry(null, previewSession(planId = null, distanceMeters = 3_100.0, hour = 17, minute = 30, id = 2)),
            DayEntry.PlanEntry(previewPlan(PlanStatus.PLANNED, hour = 19, location = "Công viên Thống Nhất").copy(id = 3)),
        ),
        canGoNext = true,
        isFirstUse = isFirstUse,
        isLoading = false,
    )
}

@Composable
private fun PreviewScreen(isFirstUse: Boolean) = RunMateTheme {
    PlanListScreen(previewState(isFirstUse), {}, {}, {}, {}, {}, {}, {}, {}, {}, onSwipeDay = {}, zone = PreviewZone)
}

@Preview(name = "Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlanListScreenPreview() = PreviewScreen(isFirstUse = false)

@Preview(name = "Lần đầu dùng app", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun PlanListScreenFirstUsePreview() = PreviewScreen(isFirstUse = true)
