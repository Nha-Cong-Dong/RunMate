package com.nhacongdong.runmate.ui.run

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.data.model.GoalType
import com.nhacongdong.runmate.domain.WeatherRules
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.service.RunServiceControl
import com.nhacongdong.runmate.ui.components.GearChips
import com.nhacongdong.runmate.ui.components.GoalPicker
import com.nhacongdong.runmate.ui.components.LocationRow
import com.nhacongdong.runmate.ui.components.StartRunButton
import com.nhacongdong.runmate.ui.components.WarningList
import com.nhacongdong.runmate.ui.components.WeatherBlock
import com.nhacongdong.runmate.ui.location.LocationDialog
import com.nhacongdong.runmate.ui.location.LocationDialogs
import com.nhacongdong.runmate.ui.location.hasLocationPermission
import com.nhacongdong.runmate.ui.location.rememberLocationGate
import com.nhacongdong.runmate.ui.notification.NotificationDeniedDialog
import com.nhacongdong.runmate.ui.notification.rememberNotificationGate
import com.nhacongdong.runmate.ui.power.PowerSaveDialog
import com.nhacongdong.runmate.ui.power.isPowerSaveWarningOn
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme
import kotlinx.coroutines.launch
import java.time.ZoneId
import com.nhacongdong.runmate.domain.model.PlanError

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

// S5 nối với ViewModel. Mở màn hình thì hỏi quyền vị trí (đã chốt), nút Bắt đầu chạy qua cổng kiểm tra quyền và GPS
@Composable
fun FreeRunRoute(
    onBack: () -> Unit,
    onPickLocation: () -> Unit,
    onOpenPlanRun: (Long) -> Unit,
    onStarted: () -> Unit,
    pickedPlace: Place?,
    onPlaceConsumed: () -> Unit,
    viewModel: FreeRunViewModel = viewModel(factory = FreeRunViewModel.factory()),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Quay lại màn hình thì kiểm tra buổi chạy dở để bật hoặc tắt nút Bắt đầu chạy
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshActiveRun() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.useCurrentLocation()
    }
    // Mở S5 thì hỏi quyền vị trí ngay, vì chạy tự do mặc định dùng vị trí hiện tại (BR-41)
    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) viewModel.useCurrentLocation() else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }
    LaunchedEffect(pickedPlace) {
        pickedPlace?.let {
            viewModel.onLocationPicked(it)
            onPlaceConsumed()
        }
    }

    // Bắt đầu chạy: tạo buổi chạy, bật service và sang S6 (SPEC 6.3)
    val startNow: () -> Unit = {
        scope.launch {
            val sessionId = viewModel.startRun()
            if (sessionId != null) {
                RunServiceControl.start(context, sessionId)
                onStarted()
            }
        }
    }
    // Tiết kiệm pin có thể làm mất GPS thì hỏi trước khi chạy (BR-61)
    var powerWarning by rememberSaveable { mutableStateOf(false) }
    // Sau khi có đủ quyền: hỏi tiết kiệm pin nếu cần, rồi mới bắt đầu
    val afterPermissions: () -> Unit = {
        if (context.isPowerSaveWarningOn()) powerWarning = true else startNow()
    }
    // Quyền thông báo (SPEC 8.3) sau quyền vị trí. Kiểm tra quyền và GPS theo DESIGN 10, SPEC 9.2
    val notificationGate = rememberNotificationGate(onReady = afterPermissions)
    val gate = rememberLocationGate(onReady = { notificationGate.start() })
    NotificationDeniedDialog(
        show = notificationGate.denied,
        onContinue = {
            notificationGate.dismiss()
            afterPermissions()
        },
        onDismiss = notificationGate.dismiss,
    )
    PowerSaveDialog(
        show = powerWarning,
        onContinue = {
            powerWarning = false
            startNow()
        },
        onDismiss = { powerWarning = false },
    )

    FreeRunScreen(
        state = state,
        onClose = onBack,
        onPickLocation = onPickLocation,
        onGoalTypeChange = viewModel::onGoalTypeChange,
        onGoalValueChange = viewModel::onGoalValueChange,
        onRunPlan = onOpenPlanRun,
        onDismissSuggestion = viewModel::dismissSuggestion,
        onStartRun = gate.start,
        onWeatherRetry = viewModel::retryWeather,
        onWeatherDismiss = viewModel::dismissWeatherError,
        locationDialog = gate.dialog,
        onDismissLocationDialog = gate.dismiss,
    )
}

// S5 · Chuẩn bị chạy tự do (DESIGN 9 S5): gợi ý kế hoạch, vị trí, mục tiêu, thời tiết, đáy là nút Bắt đầu chạy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeRunScreen(
    state: FreeRunUiState,
    onClose: () -> Unit,
    onPickLocation: () -> Unit,
    onGoalTypeChange: (GoalType) -> Unit,
    onGoalValueChange: (Int) -> Unit,
    onRunPlan: (Long) -> Unit,
    onDismissSuggestion: () -> Unit,
    onStartRun: () -> Unit,
    onWeatherRetry: () -> Unit,
    onWeatherDismiss: () -> Unit,
    locationDialog: LocationDialog?,
    onDismissLocationDialog: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.cd_close))
                    }
                },
                title = {
                    Text(stringResource(R.string.free_run_title), style = MaterialTheme.typography.titleLarge)
                },
                // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Column(Modifier.padding(horizontal = Dimens.space4, vertical = Dimens.space4)) {
                StartRunButton(
                    label = stringResource(R.string.action_start_run),
                    enabled = !state.hasActiveRun && state.location != null,
                    onClick = onStartRun,
                    icon = R.drawable.ic_play_arrow,
                    disabledReason = when {
                        state.hasActiveRun -> stringResource(R.string.start_active_run)
                        state.location == null -> stringResource(R.string.start_need_location)
                        else -> null
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.space4),
            verticalArrangement = Arrangement.spacedBy(Dimens.space4),
        ) {
            state.suggestion?.let { plan ->
                SuggestionCard(
                    plan = plan,
                    onRunPlan = { onRunPlan(plan.id) },
                    onDismiss = onDismissSuggestion,
                    zone = zone,
                )
            }

            LocationRow(
                name = state.location?.name,
                isError = state.locationFailed,
                onClick = onPickLocation,
            )

            GoalPicker(
                goalType = state.goalType,
                goalValue = state.goalValue,
                onGoalTypeChange = onGoalTypeChange,
                onGoalValueChange = onGoalValueChange,
            )

            if (state.location == null) {
                Text(
                    stringResource(R.string.weather_need_inputs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                WeatherBlock(
                    snapshot = state.weather,
                    forecastHour = WeatherRules.forecastHour(System.currentTimeMillis(), zone),
                    zone = zone,
                    isRefreshing = state.weatherLoading,
                    offline = state.weatherOffline,
                    showRefresh = false,
                    onRefresh = {},
                )
            }
            state.weather?.let { weather ->
                WarningList(
                    snapshot = weather,
                    warnings = WeatherRules.warnings(
                        weather.precipitationProb, weather.temperatureC, weather.windSpeedKmh, weather.uvIndex,
                    ),
                )
                GearChips(
                    gear = WeatherRules.gear(
                        weather.precipitationProb, weather.temperatureC, weather.windSpeedKmh, weather.uvIndex,
                        state.goalType, state.goalValue,
                    ),
                )
            }
        }
    }

    LocationDialogs(
        dialog = locationDialog,
        permissionMessage = R.string.location_permission_run_message,
        locationOffMessage = R.string.location_off_run_message,
        onDismiss = onDismissLocationDialog,
    )

    if (state.weatherFailed) {
        WeatherErrorDialog(onRetry = onWeatherRetry, onContinue = onWeatherDismiss)
    }
}

@Composable
private fun WeatherErrorDialog(onRetry: () -> Unit, onContinue: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onContinue,
        title = {
            Text(stringResource(R.string.weather_error_title), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(stringResource(R.string.weather_error_message), style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(R.string.weather_retry)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onContinue) {
                Text(stringResource(R.string.weather_continue_without))
            }
        },
        tonalElevation = 0.dp,
    )
}

private val PreviewPlace = Place("Hồ Hoàn Kiếm", "Hoàn Kiếm, Hà Nội", 21.028, 105.852)

@Composable
private fun FreeRunPreviewScreen(state: FreeRunUiState) = RunMateTheme {
    FreeRunScreen(
        state = state,
        onClose = {}, onPickLocation = {}, onGoalTypeChange = {}, onGoalValueChange = {},
        onRunPlan = {}, onDismissSuggestion = {}, onStartRun = {},
        onWeatherRetry = {}, onWeatherDismiss = {},
        locationDialog = null, onDismissLocationDialog = {},
    )
}

@Preview(name = "Có vị trí · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Có vị trí · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FreeRunLocationPreview() = FreeRunPreviewScreen(FreeRunUiState(location = PreviewPlace))

@Preview(name = "Chưa có vị trí · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Chưa có vị trí · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FreeRunNoLocationPreview() = FreeRunPreviewScreen(FreeRunUiState())

@Preview(name = "Đang có buổi dở · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang có buổi dở · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FreeRunActiveRunPreview() = FreeRunPreviewScreen(FreeRunUiState(location = PreviewPlace, hasActiveRun = true))
