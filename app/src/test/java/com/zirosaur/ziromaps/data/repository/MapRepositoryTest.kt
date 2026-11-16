package com.zirosaur.ziromaps.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.model.MapProjectSummary
import com.zirosaur.ziromaps.core.geopdf.parser.CompositeGeoPdfParser
import com.zirosaur.ziromaps.core.geopdf.parser.Iso32000Parser
import com.zirosaur.ziromaps.core.geopdf.parser.OgcLgiDictParser
import com.zirosaur.ziromaps.domain.NotGeoPdfException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapRepositoryTest {

    private lateinit var context: Context
    private val mapProjectDao = mockk<MapProjectDao>()
    private val insertedEntities = mutableListOf<MapProjectEntity>()
    private val summariesFlow = MutableStateFlow<List<MapProjectSummary>>(emptyList())
    private lateinit var repository: MapRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        insertedEntities.clear()
        summariesFlow.value = emptyList()

        every { mapProjectDao.getAllProjectSummaries() } returns summariesFlow
        coEvery { mapProjectDao.insert(any()) } answers {
            val entity = firstArg<MapProjectEntity>()
            insertedEntities.add(entity)
            summariesFlow.value = insertedEntities.map { MapProjectSummary(it, 0, 0) }
        }
        coEvery { mapProjectDao.getProjectByIdSync(any()) } answers {
            val id = firstArg<String>()
            insertedEntities.find { it.id == id }
        }
        coEvery { mapProjectDao.deleteById(any()) } answers {
            val id = firstArg<String>()
            insertedEntities.removeAll { it.id == id }
            summariesFlow.value = insertedEntities.map { MapProjectSummary(it, 0, 0) }
        }

        val parser = CompositeGeoPdfParser(
            iso32000Parser = Iso32000Parser(),
            ogcLgiDictParser = OgcLgiDictParser()
        )

        repository = MapRepositoryImpl(
            context = context,
            geoPdfParser = parser,
            mapProjectDao = mapProjectDao,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        File(context.filesDir, "maps").deleteRecursively()
    }

    private fun getSampleGeoPdfFile(): File {
        val candidates = listOf(
            File("../sample-data/BKPH RAWA TMR.pdf"),
            File("sample-data/BKPH RAWA TMR.pdf"),
            File("../../sample-data/BKPH RAWA TMR.pdf")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find sample-data/BKPH RAWA TMR.pdf")
    }

    @Test
    fun importValidGeoPdfSucceedsAndPersists() = runTest(testDispatcher) {
        val targetSource = getSampleGeoPdfFile()

        val uri = Uri.fromFile(targetSource)
        val result = repository.importMapFromUri(uri)

        assertTrue("Import should succeed", result.isSuccess)
        val item = result.getOrThrow()

        assertEquals("BKPH RAWA TMR", item.name)
        assertEquals("ISO_32000", item.geoStandard)
        assertTrue(item.pageWidthPt > 0)
        assertTrue(item.pageHeightPt > 0)

        // Verify stored in DAO
        val entity = mapProjectDao.getProjectByIdSync(item.id)
        assertNotNull(entity)
        assertEquals(item.id, entity?.id)

        // Verify file stored on disk
        val localPdf = File(entity?.pdfLocalPath ?: "")
        assertTrue(localPdf.exists())
        assertTrue(localPdf.length() > 0)

        // Verify Flow emits project
        repository.getMapProjects().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("BKPH RAWA TMR", list[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun importNonGeoPdfFailsWithNotGeoPdfExceptionAndCleansUp() = runTest(testDispatcher) {
        // Create a plain dummy PDF
        val dummyFile = File(context.cacheDir, "plain.pdf")
        FileOutputStream(dummyFile).use { out ->
            out.write("%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\nxref\n0 4\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000120 00000 n \ntrailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n200\n%%EOF".toByteArray())
        }

        val uri = Uri.fromFile(dummyFile)
        val result = repository.importMapFromUri(uri)

        assertTrue("Import should fail for plain PDF", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Exception should be NotGeoPdfException", exception is NotGeoPdfException)

        // Verify nothing inserted to DAO
        repository.getMapProjects().test {
            val list = awaitItem()
            assertTrue(list.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // Verify no leftover project dirs
        val mapsDir = File(context.filesDir, "maps")
        if (mapsDir.exists()) {
            val children = mapsDir.listFiles() ?: emptyArray()
            assertEquals(0, children.size)
        }
    }

    @Test
    fun deleteMapProjectRemovesDatabaseEntryAndLocalFiles() = runTest(testDispatcher) {
        val targetSource = getSampleGeoPdfFile()

        val uri = Uri.fromFile(targetSource)
        val result = repository.importMapFromUri(uri)
        val item = result.getOrThrow()

        val localPdf = File(item.pdfLocalPath)
        assertTrue(localPdf.exists())

        // Now delete
        repository.deleteMapProject(item.id)

        // Verify removed from DAO
        assertNull(mapProjectDao.getProjectByIdSync(item.id))

        // Verify file deleted
        assertFalse(localPdf.exists())
    }
}
