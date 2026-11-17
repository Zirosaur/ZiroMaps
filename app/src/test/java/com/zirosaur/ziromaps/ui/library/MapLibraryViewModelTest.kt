package com.zirosaur.ziromaps.ui.library

import android.net.Uri
import app.cash.turbine.test
import com.zirosaur.ziromaps.core.geopdf.tile.PdfTileRenderer
import com.zirosaur.ziromaps.data.repository.MapRepository
import com.zirosaur.ziromaps.domain.NotGeoPdfException
import com.zirosaur.ziromaps.model.MapProjectItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapLibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<MapRepository>()
    private val pdfTileRenderer = mockk<PdfTileRenderer>(relaxed = true)
    private val mapsFlow = MutableStateFlow<List<MapProjectItem>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getMapProjects() } returns mapsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateLoadsMapsFromRepository() = runTest(testDispatcher) {
        val sampleItem = MapProjectItem(
            id = "proj-1",
            name = "Test Map",
            pdfLocalPath = "/path/test.pdf",
            pageWidthPt = 600.0,
            pageHeightPt = 800.0,
            geoStandard = "ISO_32000",
            epsgCode = 4326,
            waypointCount = 3,
            photoCount = 2,
            createdAtEpochMs = 1000L,
            formattedDate = "16 Nov 2026, 18:00"
        )
        mapsFlow.value = listOf(sampleItem)

        val viewModel = MapLibraryViewModel(repository, pdfTileRenderer)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.maps.size)
            assertEquals("Test Map", state.maps[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchFilteringWorksCorrectly() = runTest(testDispatcher) {
        val map1 = MapProjectItem(
            id = "p1", name = "Gunung Slamet", pdfLocalPath = "/p1.pdf",
            pageWidthPt = 500.0, pageHeightPt = 500.0, geoStandard = "ISO_32000", epsgCode = null,
            waypointCount = 0, photoCount = 0, createdAtEpochMs = 100L, formattedDate = ""
        )
        val map2 = MapProjectItem(
            id = "p2", name = "Gunung Merapi", pdfLocalPath = "/p2.pdf",
            pageWidthPt = 500.0, pageHeightPt = 500.0, geoStandard = "ISO_32000", epsgCode = null,
            waypointCount = 0, photoCount = 0, createdAtEpochMs = 200L, formattedDate = ""
        )
        mapsFlow.value = listOf(map1, map2)

        val viewModel = MapLibraryViewModel(repository, pdfTileRenderer)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.filteredMaps.size)

        viewModel.onSearchQueryChanged("Slamet")
        assertEquals(1, viewModel.uiState.value.filteredMaps.size)
        assertEquals("Gunung Slamet", viewModel.uiState.value.filteredMaps[0].name)

        viewModel.onSearchQueryChanged("")
        assertEquals(2, viewModel.uiState.value.filteredMaps.size)
    }

    @Test
    fun importMapSuccessUpdatesUserMessage() = runTest(testDispatcher) {
        val viewModel = MapLibraryViewModel(repository, pdfTileRenderer)
        testDispatcher.scheduler.advanceUntilIdle()

        val mockUri = mockk<Uri>()
        val importedItem = MapProjectItem(
            id = "proj-new",
            name = "New Map",
            pdfLocalPath = "/path/new.pdf",
            pageWidthPt = 500.0,
            pageHeightPt = 700.0,
            geoStandard = "OGC_LGIDICT",
            epsgCode = 32749,
            waypointCount = 0,
            photoCount = 0,
            createdAtEpochMs = 2000L,
            formattedDate = "16 Nov 2026, 19:00"
        )
        coEvery { repository.importMapFromUri(mockUri) } returns Result.success(importedItem)

        viewModel.importMap(mockUri)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.notGeoPdfDialogVisible)
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage?.contains("New Map") == true)
    }

    @Test
    fun importMapNotGeoPdfTriggersNotGeoPdfDialog() = runTest(testDispatcher) {
        val viewModel = MapLibraryViewModel(repository, pdfTileRenderer)
        testDispatcher.scheduler.advanceUntilIdle()

        val mockUri = mockk<Uri>()
        coEvery { repository.importMapFromUri(mockUri) } returns Result.failure(NotGeoPdfException())

        viewModel.importMap(mockUri)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.notGeoPdfDialogVisible)
        assertNull(state.errorMessage)

        viewModel.dismissNotGeoPdfDialog()
        assertFalse(viewModel.uiState.value.notGeoPdfDialogVisible)
    }

    @Test
    fun deleteMapFlowExecutesSuccessfully() = runTest(testDispatcher) {
        val viewModel = MapLibraryViewModel(repository, pdfTileRenderer)
        testDispatcher.scheduler.advanceUntilIdle()

        val target = MapProjectItem(
            id = "proj-delete",
            name = "Delete Me",
            pdfLocalPath = "/path/delete.pdf",
            pageWidthPt = 500.0,
            pageHeightPt = 700.0,
            geoStandard = "ISO_32000",
            epsgCode = null,
            waypointCount = 0,
            photoCount = 0,
            createdAtEpochMs = 3000L,
            formattedDate = "16 Nov 2026, 19:30"
        )
        coEvery { repository.deleteMapProject("proj-delete") } returns Unit

        viewModel.confirmDeleteMap(target)
        assertEquals(target, viewModel.uiState.value.deleteConfirmationProject)

        viewModel.deleteConfirmedMap()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteConfirmationProject)
        coVerify { repository.deleteMapProject("proj-delete") }
        assertTrue(viewModel.uiState.value.userMessage?.contains("Delete Me") == true)
    }
}
