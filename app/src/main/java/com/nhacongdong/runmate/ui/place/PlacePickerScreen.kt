package com.nhacongdong.runmate.ui.place

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhacongdong.runmate.R
import com.nhacongdong.runmate.domain.model.Place
import com.nhacongdong.runmate.ui.theme.Dimens
import com.nhacongdong.runmate.ui.theme.RunMateTheme

// Hàng cao 64 dp (DESIGN 9 S3). Dòng "Dùng vị trí hiện tại" cũng cao như vậy cho khớp
private val RowHeight = 64.dp

// S3 nối với ViewModel: xin quyền khi bấm "Dùng vị trí hiện tại", báo lỗi bằng snackbar (DESIGN 9 S3)
@Composable
fun PlacePickerRoute(
    onBack: () -> Unit,
    onPlaceSelected: (Place) -> Unit,
    viewModel: PlacePickerViewModel = viewModel(factory = PlacePickerViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val locateFailedMessage = stringResource(R.string.place_locate_failed)
    // Hộp thoại khi quyền bị từ chối hoặc GPS tắt (DESIGN 10, SPEC 9.2)
    var locationDialog by rememberSaveable { mutableStateOf<LocationDialog?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.useCurrentLocation() else viewModel.onLocationDenied()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PlacePickerEvent.Selected -> onPlaceSelected(event.place)
                PlacePickerEvent.PermissionDenied -> locationDialog = LocationDialog.PERMISSION
                PlacePickerEvent.LocateFailed -> snackbarHostState.showSnackbar(locateFailedMessage)
            }
        }
    }

    PlacePickerScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onUseCurrentLocation = {
            when {
                // Chưa có quyền: hệ thống hỏi. Nếu đã từ chối trước đó, hệ thống không hỏi nữa và trả về ngay
                !context.hasLocationPermission() -> permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
                // Có quyền nhưng GPS của máy đang tắt
                !context.isLocationOn() -> locationDialog = LocationDialog.LOCATION_OFF
                else -> viewModel.useCurrentLocation()
            }
        },
        onSelect = onPlaceSelected,
        onBack = onBack,
    )

    when (locationDialog) {
        LocationDialog.PERMISSION -> AlertDialog(
            onDismissRequest = { locationDialog = null },
            title = { Text(stringResource(R.string.location_permission_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.place_permission_denied), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    locationDialog = null
                    context.startActivity(appSettingsIntent(context))
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { locationDialog = null }) { Text(stringResource(R.string.action_later)) }
            },
            tonalElevation = 0.dp,
        )
        LocationDialog.LOCATION_OFF -> AlertDialog(
            onDismissRequest = { locationDialog = null },
            title = { Text(stringResource(R.string.location_off_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.location_off_message), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    locationDialog = null
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text(stringResource(R.string.action_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { locationDialog = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            tonalElevation = 0.dp,
        )
        null -> Unit
    }
}

private enum class LocationDialog { PERMISSION, LOCATION_OFF }

// Chỉ cần một trong hai quyền (SPEC 8.3)
private fun Context.hasLocationPermission(): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

// GPS của máy có đang bật không (minSdk 26 nên dùng LocationManagerCompat)
private fun Context.isLocationOn(): Boolean =
    LocationManagerCompat.isLocationEnabled(getSystemService(LocationManager::class.java))

// Trang cài đặt quyền của chính app
private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

// S3 · Chọn vị trí (DESIGN 9 S3): thanh trên có ô tìm, dòng cố định, kết quả, các trạng thái
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerScreen(
    state: PlacePickerUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSelect: (Place) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back))
                    }
                },
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(stringResource(R.string.place_search_hint), style = MaterialTheme.typography.bodyLarge) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(Dimens.radiusM),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                // Thanh trạng thái đã được Scaffold của RunMateApp chừa ra
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isSearching) LinearProgressIndicator(Modifier.fillMaxWidth())

            CurrentLocationRow(isLocating = state.isLocating, onClick = onUseCurrentLocation)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                state.searchFailed -> NetworkErrorBlock()
                state.results.isEmpty() && state.searchedQuery.isNotEmpty() && !state.isSearching ->
                    Text(
                        stringResource(R.string.place_not_found, state.searchedQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.space4, vertical = Dimens.space3),
                    )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = Dimens.space4),
            ) {
                items(state.results) { place ->
                    PlaceResultRow(place = place, onClick = { onSelect(place) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// Dòng đầu cố định. Đang lấy vị trí thì thay biểu tượng bằng vòng xoay nhỏ (DESIGN 9 S3)
@Composable
private fun CurrentLocationRow(isLocating: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(enabled = !isLocating, onClick = onClick)
            .padding(horizontal = Dimens.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        if (isLocating) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painterResource(R.drawable.ic_my_location),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            stringResource(R.string.place_use_current),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Tên (bodyLarge) và mô tả cấp trên (bodyMedium, textSecondary), ví dụ "Hồ Hoàn Kiếm" / "Hoàn Kiếm, Hà Nội"
@Composable
private fun PlaceResultRow(place: Place, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.space4),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            place.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (place.subtitle.isNotEmpty()) {
            Text(
                place.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Khối lỗi mạng: nền errorContainer (DESIGN 4.3)
@Composable
private fun NetworkErrorBlock() {
    Text(
        stringResource(R.string.place_network_error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.space4, vertical = Dimens.space3)
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(Dimens.radiusL))
            .padding(Dimens.space4),
    )
}

private val PreviewPlace = Place("Hồ Hoàn Kiếm", "Hoàn Kiếm, Hà Nội", 21.028, 105.852)

@Composable
private fun PreviewScreen(state: PlacePickerUiState) = RunMateTheme {
    PlacePickerScreen(
        state = state,
        snackbarHostState = remember { SnackbarHostState() },
        onQueryChange = {},
        onUseCurrentLocation = {},
        onSelect = {},
        onBack = {},
    )
}

@Preview(name = "Chưa gõ · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Chưa gõ · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePickerIdlePreview() = PreviewScreen(PlacePickerUiState())

@Preview(name = "Có kết quả · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Có kết quả · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePickerResultsPreview() = PreviewScreen(
    PlacePickerUiState(query = "Hồ Gươm", results = listOf(PreviewPlace), searchedQuery = "Hồ Gươm"),
)

@Preview(name = "Không có kết quả · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Không có kết quả · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePickerNotFoundPreview() = PreviewScreen(
    PlacePickerUiState(query = "xyz", searchedQuery = "xyz"),
)

@Preview(name = "Mất mạng · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Mất mạng · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePickerNetworkErrorPreview() = PreviewScreen(
    PlacePickerUiState(query = "Hồ Gươm", searchFailed = true),
)

@Preview(name = "Đang lấy vị trí · Sáng", widthDp = 360, heightDp = 720, showBackground = true)
@Preview(name = "Đang lấy vị trí · Tối", widthDp = 360, heightDp = 720, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlacePickerLocatingPreview() = PreviewScreen(PlacePickerUiState(isLocating = true))
