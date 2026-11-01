package com.nhacongdong.runmate.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.nhacongdong.runmate.ui.run.FreeRunRoute
import com.nhacongdong.runmate.ui.run.RecoverRunDialog
import com.nhacongdong.runmate.ui.run.RunRoute
import com.nhacongdong.runmate.ui.place.PlacePickerRoute
import com.nhacongdong.runmate.ui.screens.PlaceholderAction
import com.nhacongdong.runmate.ui.screens.PlaceholderScreen
import com.nhacongdong.runmate.ui.session.SessionDetailRoute
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import java.time.LocalDate

// Id giả để thử điều hướng trong khung code, chưa có dữ liệu thật
private const val SAMPLE_ID = 1L

// Khoá lưu địa điểm chọn ở S3 để trả về S2
private const val PICKED_PLACE_KEY = "picked_place"

@Composable
fun RunMateApp(
    openRun: Boolean = false,           // bấm thông báo của service thì mở S6 (SPEC 8.3)
    onOpenRunHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    LaunchedEffect(openRun) {
        if (openRun) {
            navController.navigate(Running) { launchSingleTop = true }
            onOpenRunHandled()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val appViewModel: RunMateAppViewModel = viewModel(factory = RunMateAppViewModel.Factory)
    val hasActiveRun by appViewModel.hasActiveRun.collectAsStateWithLifecycle()
    val interrupted by appViewModel.interrupted.collectAsStateWithLifecycle()
    // Mỗi lần app hiện lên thì kiểm tra buổi dở đã mất service chưa (SPEC 10.1)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { appViewModel.refresh() }
    LaunchedEffect(appViewModel) {
        appViewModel.openRun.collect { navController.navigate(Running) { launchSingleTop = true } }
    }
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
                    // Nút hành động, không phải tab (SPEC 9.3). Có buổi chạy dở thì mở thẳng S6 (BR-08), không thì mở S5
                    onRun = { navController.navigate(if (hasActiveRun) Running else FreeRunSetup) },
                    hasActiveRun = hasActiveRun,
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
                    // Băng "Đang có buổi chạy" mở thẳng S6 (DESIGN 8.4)
                    onOpenRun = { navController.navigate(Running) },
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
            composable<FreeRunSetup> { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<Place?>(PICKED_PLACE_KEY, null)
                    .collectAsStateWithLifecycle()
                FreeRunRoute(
                    onBack = { navController.popBackStack() },
                    onPickLocation = { navController.navigate(LocationPicker) },
                    // Chạy theo kế hoạch (BR-44): thay S5 bằng S4 của kế hoạch đó
                    onOpenPlanRun = { planId ->
                        navController.navigate(PlanDetail(planId)) { popUpTo<FreeRunSetup> { inclusive = true } }
                    },
                    // Bắt đầu chạy xong thì sang S6 và bỏ S5 khỏi lịch sử
                    onStarted = { navController.navigate(Running) { popUpTo<FreeRunSetup> { inclusive = true } } },
                    pickedPlace = picked,
                    onPlaceConsumed = { entry.savedStateHandle.remove<Place>(PICKED_PLACE_KEY) },
                )
            }
            composable<Running> {
                RunRoute(
                    // Lưu xong thì sang S7 và bỏ S6 khỏi lịch sử, để Back không quay lại (SPEC 9.3)
                    onSaved = { id ->
                        navController.navigate(SessionDetail(id)) { popUpTo<Running> { inclusive = true } }
                    },
                    // Quá ngắn hoặc không có buổi dở thì quay lại màn trước
                    onBack = { navController.popBackStack() },
                )
            }
            composable<SessionDetail> { entry ->
                SessionDetailRoute(
                    sessionId = entry.toRoute<SessionDetail>().sessionId,
                    onBack = { navController.popBackStack() },
                    // Xoá xong thì về màn trước (buổi chạy đã mất) hoặc về S1 (kế hoạch đã xoá, SPEC 9.3)
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable<Stats> {
                PlaceholderScreen(R.string.screen_stats, listOf(
                    PlaceholderAction(R.string.action_open_session) { navController.navigate(SessionDetail(SAMPLE_ID)) },
                ))
            }
        }
    }
    // Buổi dở đã mất service: hỏi Tiếp tục hay Kết thúc, chỉ hiện ở S1 (DESIGN 9 S1)
    val pending = interrupted
    if (pending != null && destination?.hasRoute<PlanList>() == true) {
        RecoverRunDialog(run = pending, onResume = appViewModel::resume, onFinish = appViewModel::finish)
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
