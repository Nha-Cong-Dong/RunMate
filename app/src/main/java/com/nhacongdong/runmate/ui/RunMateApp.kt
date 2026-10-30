package com.nhacongdong.runmate.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.ui.components.BottomBar
import com.nhacongdong.runmate.ui.components.BottomTab
import com.nhacongdong.runmate.ui.navigation.FreeRunSetup
import com.nhacongdong.runmate.ui.navigation.LocationPicker
import com.nhacongdong.runmate.ui.navigation.PlanDetail
import com.nhacongdong.runmate.ui.navigation.PlanEdit
import com.nhacongdong.runmate.ui.navigation.PlanList
import com.nhacongdong.runmate.ui.navigation.Running
import com.nhacongdong.runmate.ui.navigation.SessionDetail
import com.nhacongdong.runmate.ui.navigation.Stats
import com.nhacongdong.runmate.ui.plan.PlanDetailRoute
import com.nhacongdong.runmate.ui.plan.PlanEditRoute
import com.nhacongdong.runmate.ui.plan.PlanListRoute
import com.nhacongdong.runmate.ui.place.PlacePickerRoute
import com.nhacongdong.runmate.ui.screens.PlaceholderAction
import com.nhacongdong.runmate.ui.screens.PlaceholderScreen
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.LocalDate

// Id giả để thử điều hướng trong khung code, chưa có dữ liệu thật
private const val SAMPLE_ID = 1L

// Khoá lưu địa điểm chọn ở S3 để trả về S2
private const val PICKED_PLACE_KEY = "picked_place"

@Composable
fun RunMateApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    // Thanh dưới chỉ hiện ở 2 tab (SPEC 9.3, DESIGN 8.2)
    val tab = when {
        destination?.hasRoute<PlanList>() == true -> BottomTab.Calendar
        destination?.hasRoute<Stats>() == true -> BottomTab.Stats
        else -> null
    }

    Scaffold(
        bottomBar = {
            if (tab != null) {
                BottomBar(
                    selected = tab,
                    onSelect = { navController.navigateToTab(if (it == BottomTab.Calendar) PlanList else Stats) },
                    // Nút hành động, không phải tab (SPEC 9.3). Giai đoạn D (D7): có buổi chạy dở thì mở thẳng S6 (BR-08)
                    onRun = { navController.navigate(FreeRunSetup) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PlanList,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<PlanList> {
                PlanListRoute(
                    // Điền sẵn ngày đang chọn trên S1 vào S2 (PLAN B4)
                    onCreatePlan = { navController.navigate(PlanEdit(initialDay = it.toEpochDay())) },
                    onPlanDay = { navController.navigate(PlanEdit(initialDay = it.toEpochDay())) },
                    onOpenPlan = { navController.navigate(PlanDetail(it)) },
                    onViewResult = { navController.navigate(SessionDetail(it)) },
                    onRunNow = { navController.navigate(FreeRunSetup) },
                )
            }
            composable<PlanEdit> { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<Place?>(PICKED_PLACE_KEY, null)
                    .collectAsStateWithLifecycle()
                // Lưu khi tạo thì về S1, khi sửa thì về S4: cả hai đều là quay lại màn hình trước
                PlanEditRoute(
                    args = entry.toRoute<PlanEdit>(),
                    pickedPlace = picked,
                    onPlaceConsumed = { entry.savedStateHandle.remove<Place>(PICKED_PLACE_KEY) },
                    onPickLocation = { navController.navigate(LocationPicker) },
                    onDone = { navController.popBackStack() },
                )
            }
            composable<LocationPicker> {
                PlacePickerRoute(
                    onBack = { navController.popBackStack() },
                    onPlaceSelected = { place ->
                        // Trả địa điểm về S2 qua SavedStateHandle của màn hình trước
                        navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_PLACE_KEY, place)
                        navController.popBackStack()
                    },
                )
            }
            composable<PlanDetail> { entry ->
                PlanDetailRoute(
                    args = entry.toRoute<PlanDetail>(),
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(PlanEdit(planId = it)) },
                    onOpenRun = { navController.navigate(Running) },
                    // Xoá xong thì về S1, nơi S4 được mở từ đó
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable<FreeRunSetup> {
                PlaceholderScreen(R.string.screen_free_run_setup, listOf(
                    PlaceholderAction(R.string.action_change_location) { navController.navigate(LocationPicker) },
                    // Chạy theo kế hoạch (BR-44): thay S5 bằng S4
                    PlaceholderAction(R.string.action_run_with_plan) {
                        navController.navigate(PlanDetail(SAMPLE_ID)) {
                            popUpTo<FreeRunSetup> { inclusive = true }
                        }
                    },
                    PlaceholderAction(R.string.action_start_run) {
                        navController.navigate(Running) { popUpTo<FreeRunSetup> { inclusive = true } }
                    },
                ))
            }
            composable<Running> {
                PlaceholderScreen(R.string.screen_running, listOf(
                    // Kết thúc thì sang S7 và bỏ S6 khỏi lịch sử, để Back không quay lại (SPEC 9.3)
                    PlaceholderAction(R.string.action_finish_saved) {
                        navController.navigate(SessionDetail(SAMPLE_ID)) {
                            popUpTo<Running> { inclusive = true }
                        }
                    },
                    PlaceholderAction(R.string.action_finish_too_short) { navController.popBackStack() },
                ))
            }
            composable<SessionDetail> {
                PlaceholderScreen(R.string.screen_session_detail, listOf(
                    PlaceholderAction(R.string.action_delete) { navController.popBackStack() },
                ))
            }
            composable<Stats> {
                PlaceholderScreen(R.string.screen_stats, listOf(
                    PlaceholderAction(R.string.action_open_session) { navController.navigate(SessionDetail(SAMPLE_ID)) },
                ))
            }
        }
    }
}

// Chuyển tab mà không chồng thêm màn hình mỗi lần bấm, và giữ trạng thái từng tab
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview(showBackground = true)
@Composable
private fun RunMateAppPreview() {
    RunMateTheme { RunMateApp() }
}
