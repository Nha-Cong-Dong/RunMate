package com.nhacongdong.runmate.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.ui.navigation.FreeRunSetup
import com.nhacongdong.runmate.ui.navigation.History
import com.nhacongdong.runmate.ui.navigation.LocationPicker
import com.nhacongdong.runmate.ui.navigation.PlanDetail
import com.nhacongdong.runmate.ui.navigation.PlanEdit
import com.nhacongdong.runmate.ui.navigation.PlanList
import com.nhacongdong.runmate.ui.navigation.Running
import com.nhacongdong.runmate.ui.navigation.SessionDetail
import com.nhacongdong.runmate.ui.plan.PlanListRoute
import com.nhacongdong.runmate.ui.screens.PlaceholderAction
import com.nhacongdong.runmate.ui.screens.PlaceholderScreen
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Id giả để thử điều hướng trong khung code, chưa có dữ liệu thật
private const val SAMPLE_ID = 1L

@Composable
fun RunMateApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    // Thanh dưới chỉ hiện ở 2 tab (SPEC 9.3)
    val showBottomBar = destination?.hasRoute<PlanList>() == true ||
        destination?.hasRoute<History>() == true

    Scaffold(
        bottomBar = { if (showBottomBar) RunMateBottomBar(navController, destination) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PlanList,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<PlanList> {
                PlanListRoute(
                    onCreatePlan = { navController.navigate(PlanEdit()) },
                    // B4: điền sẵn ngày đang chọn vào S2
                    onPlanDay = { navController.navigate(PlanEdit()) },
                    onOpenPlan = { navController.navigate(PlanDetail(it)) },
                    onViewResult = { navController.navigate(SessionDetail(it)) },
                    onRunNow = { navController.navigate(FreeRunSetup) },
                )
            }
            composable<PlanEdit> {
                // Lưu khi tạo thì về S1, khi sửa thì về S4: cả hai đều là quay lại màn hình trước
                PlaceholderScreen(R.string.screen_plan_edit, listOf(
                    PlaceholderAction(R.string.action_pick_location) { navController.navigate(LocationPicker) },
                    PlaceholderAction(R.string.action_save) { navController.popBackStack() },
                ))
            }
            composable<LocationPicker> {
                PlaceholderScreen(R.string.screen_location_picker, listOf(
                    PlaceholderAction(R.string.action_select_location) { navController.popBackStack() },
                ))
            }
            composable<PlanDetail> {
                PlaceholderScreen(R.string.screen_plan_detail, listOf(
                    PlaceholderAction(R.string.action_edit) { navController.navigate(PlanEdit(SAMPLE_ID)) },
                    PlaceholderAction(R.string.action_start_run) { navController.navigate(Running) },
                    PlaceholderAction(R.string.action_view_session) { navController.navigate(SessionDetail(SAMPLE_ID)) },
                    PlaceholderAction(R.string.action_delete) { navController.popBackStack() },
                ))
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
            composable<History> {
                PlaceholderScreen(R.string.screen_history, listOf(
                    PlaceholderAction(R.string.action_open_session) { navController.navigate(SessionDetail(SAMPLE_ID)) },
                ))
            }
        }
    }
}

@Composable
private fun RunMateBottomBar(navController: NavHostController, destination: NavDestination?) {
    NavigationBar {
        NavigationBarItem(
            selected = destination?.hasRoute<PlanList>() == true,
            onClick = { navController.navigateToTab(PlanList) },
            icon = { Icon(painterResource(R.drawable.ic_plan_list), contentDescription = null) },
            label = { Text(stringResource(R.string.tab_plans)) },
        )
        // Nút hành động, không phải tab (SPEC 9.3).
        // Giai đoạn D: nếu đang có buổi chạy dở thì mở thẳng S6 (BR-08).
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(FreeRunSetup) },
            icon = { Icon(painterResource(R.drawable.ic_run), contentDescription = null) },
            label = { Text(stringResource(R.string.tab_run)) },
        )
        NavigationBarItem(
            selected = destination?.hasRoute<History>() == true,
            onClick = { navController.navigateToTab(History) },
            icon = { Icon(painterResource(R.drawable.ic_history), contentDescription = null) },
            label = { Text(stringResource(R.string.tab_history)) },
        )
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
