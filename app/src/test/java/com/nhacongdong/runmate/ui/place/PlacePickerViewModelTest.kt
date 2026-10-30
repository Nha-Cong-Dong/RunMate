package com.nhacongdong.runmate.ui.place

import com.nhacongdong.runmate.data.place.PlaceRepository
import com.nhacongdong.runmate.data.place.PlaceSearch
import com.nhacongdong.runmate.domain.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// Test ô tìm kiếm và luồng vị trí hiện tại của S3 (DESIGN 9 S3)
@OptIn(ExperimentalCoroutinesApi::class)
class PlacePickerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val hoGuom = Place("Hồ Hoàn Kiếm", "Hoàn Kiếm, Hà Nội", 21.028, 105.852)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // Giả lập Geocoder, ghi lại chữ đã được tìm
    private class FakeSearch(private val answer: suspend () -> List<Place>) : PlaceSearch {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): List<Place> {
            queries += query
            return answer()
        }
    }

    private fun viewModel(
        geocoder: PlaceSearch,
        fallback: PlaceSearch = FakeSearch { emptyList() },
        locate: suspend () -> android.location.Location? = { null },
    ) = PlacePickerViewModel(PlaceRepository(geocoder, fallback), locate)

    @Test
    fun blankQuery_doesNotSearch() = runTest(dispatcher) {
        val geocoder = FakeSearch { listOf(hoGuom) }
        val vm = viewModel(geocoder)

        vm.onQueryChange("   ")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), geocoder.queries)
        assertEquals(emptyList<Place>(), vm.uiState.value.results)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun query_waitsForDelayThenShowsResults() = runTest(dispatcher) {
        val geocoder = FakeSearch { listOf(hoGuom) }
        val vm = viewModel(geocoder)

        vm.onQueryChange("Hồ Gươm")
        advanceTimeBy(PlacePickerViewModel.SEARCH_DELAY_MS - 1)
        assertEquals(emptyList<String>(), geocoder.queries)
        assertTrue(vm.uiState.value.isSearching)

        advanceUntilIdle()
        assertEquals(listOf("Hồ Gươm"), geocoder.queries)
        assertEquals(listOf(hoGuom), vm.uiState.value.results)
        assertEquals("Hồ Gươm", vm.uiState.value.searchedQuery)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun fastTyping_searchesOnlyLastText() = runTest(dispatcher) {
        val geocoder = FakeSearch { emptyList() }
        val vm = viewModel(geocoder)

        vm.onQueryChange("H")
        advanceTimeBy(100)
        vm.onQueryChange("Hồ")
        advanceUntilIdle()

        assertEquals(listOf("Hồ"), geocoder.queries)
    }

    @Test
    fun noResults_keepsSearchedTextForMessage() = runTest(dispatcher) {
        val vm = viewModel(FakeSearch { emptyList() })

        vm.onQueryChange("xyz")
        advanceUntilIdle()

        assertEquals(emptyList<Place>(), vm.uiState.value.results)
        assertEquals("xyz", vm.uiState.value.searchedQuery)
        assertFalse(vm.uiState.value.searchFailed)
    }

    @Test
    fun networkError_setsSearchFailed() = runTest(dispatcher) {
        // Geocoder lỗi rồi dự phòng cũng lỗi mạng thì mới báo mất mạng
        val vm = viewModel(
            geocoder = FakeSearch { throw IOException("Geocoder lỗi") },
            fallback = FakeSearch { throw IOException("mất mạng") },
        )

        vm.onQueryChange("Hồ Gươm")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.searchFailed)
        assertEquals(emptyList<Place>(), vm.uiState.value.results)
    }

    @Test
    fun locateWithoutLocation_sendsLocateFailed() = runTest(dispatcher) {
        val vm = viewModel(FakeSearch { emptyList() }, locate = { null })
        val events = mutableListOf<PlacePickerEvent>()
        val collector = launchCollector(this, vm, events)

        vm.useCurrentLocation()
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf(PlacePickerEvent.LocateFailed), events)
        assertFalse(vm.uiState.value.isLocating)
    }

    @Test
    fun denied_sendsPermissionDenied() = runTest(dispatcher) {
        val vm = viewModel(FakeSearch { emptyList() })
        val events = mutableListOf<PlacePickerEvent>()
        val collector = launchCollector(this, vm, events)

        vm.onLocationDenied()
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf(PlacePickerEvent.PermissionDenied), events)
    }

    private fun launchCollector(
        scope: TestScope,
        vm: PlacePickerViewModel,
        events: MutableList<PlacePickerEvent>,
    ) = scope.launch(dispatcher) { vm.events.collect { events += it } }
}
