package com.nhacongdong.runmate.ui.place

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nhacongdong.runmate.RunMateApplication
import com.nhacongdong.runmate.data.place.PlaceRepository
import com.nhacongdong.runmate.domain.model.Place
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

// Trạng thái của S3 (DESIGN 9 S3)
data class PlacePickerUiState(
    val query: String = "",
    val results: List<Place> = emptyList(),
    val searchedQuery: String = "",     // chữ đã tìm xong, dùng cho "Không tìm thấy “…”"
    val isSearching: Boolean = false,
    val searchFailed: Boolean = false,  // không có mạng (SPEC 10.3)
    val isLocating: Boolean = false,    // đang lấy vị trí hiện tại
)

// Việc chỉ xảy ra một lần: chọn xong thì về màn hình trước, hoặc báo lỗi bằng snackbar
sealed interface PlacePickerEvent {
    data class Selected(val place: Place) : PlacePickerEvent
    data object PermissionDenied : PlacePickerEvent
    data object LocateFailed : PlacePickerEvent
}

// locate lấy vị trí hiện tại (LocationProvider.currentLocation). Tách thành hàm để test không cần Context
class PlacePickerViewModel(
    private val placeRepository: PlaceRepository,
    private val locate: suspend () -> Location?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacePickerUiState())
    val uiState: StateFlow<PlacePickerUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlacePickerEvent>(Channel.BUFFERED)
    val events: Flow<PlacePickerEvent> = _events.receiveAsFlow()

    private var searchJob: Job? = null

    // Gõ tới đâu cũng cập nhật ô nhập ngay. Tìm kiếm chờ người dùng ngừng gõ (SEARCH_DELAY_MS),
    // lần gõ mới huỷ lần tìm trước để không gọi API cho từng ký tự
    fun onQueryChange(text: String) {
        _uiState.update { it.copy(query = text) }
        searchJob?.cancel()
        val term = text.trim()
        if (term.isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), searchedQuery = "", isSearching = false, searchFailed = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true, searchFailed = false) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MS)
            try {
                val results = placeRepository.search(term)
                _uiState.update { it.copy(results = results, searchedQuery = term, isSearching = false) }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(results = emptyList(), searchedQuery = "", isSearching = false, searchFailed = true)
                }
            }
        }
    }

    // Người dùng đã cấp quyền vị trí (hoặc vừa cấp xong) thì lấy vị trí hiện tại
    fun useCurrentLocation() {
        if (_uiState.value.isLocating) return
        _uiState.update { it.copy(isLocating = true) }
        viewModelScope.launch {
            val location = try {
                locate()
            } catch (e: SecurityException) {
                null
            }
            _uiState.update { it.copy(isLocating = false) }
            _events.trySend(
                if (location == null) {
                    PlacePickerEvent.LocateFailed
                } else {
                    PlacePickerEvent.Selected(Place(CURRENT_LOCATION_NAME, "", location.latitude, location.longitude))
                }
            )
        }
    }

    fun onLocationDenied() {
        _events.trySend(PlacePickerEvent.PermissionDenied)
    }

    companion object {
        const val SEARCH_DELAY_MS = 400L

        // Tên lưu cùng buổi chạy và kế hoạch (SPEC 8.2). Không dịch vì là dữ liệu đã lưu
        const val CURRENT_LOCATION_NAME = "Vị trí hiện tại"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RunMateApplication).container
                PlacePickerViewModel(container.placeRepository, container.locationProvider::currentLocation)
            }
        }
    }
}
