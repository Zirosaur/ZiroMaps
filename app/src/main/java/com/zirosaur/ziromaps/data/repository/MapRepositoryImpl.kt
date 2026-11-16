package com.zirosaur.ziromaps.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.zirosaur.ziromaps.core.database.dao.MapProjectDao
import com.zirosaur.ziromaps.core.database.entity.MapProjectEntity
import com.zirosaur.ziromaps.core.database.model.MapProjectSummary
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParseResult
import com.zirosaur.ziromaps.core.geopdf.parser.GeoPdfParser
import com.zirosaur.ziromaps.domain.GeoPdfParseException
import com.zirosaur.ziromaps.domain.NotGeoPdfException
import com.zirosaur.ziromaps.model.MapProjectItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geoPdfParser: GeoPdfParser,
    private val mapProjectDao: MapProjectDao
) : MapRepository {

    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Visible for testing with custom test dispatchers.
     */
    internal constructor(
        context: Context,
        geoPdfParser: GeoPdfParser,
        mapProjectDao: MapProjectDao,
        ioDispatcher: CoroutineDispatcher
    ) : this(context, geoPdfParser, mapProjectDao) {
        this.ioDispatcher = ioDispatcher
    }

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

    override fun getMapProjects(): Flow<List<MapProjectItem>> {
        return mapProjectDao.getAllProjectSummaries().map { list ->
            list.map { summary -> summary.toItem() }
        }
    }

    override suspend fun importMapFromUri(uri: Uri): Result<MapProjectItem> = withContext(ioDispatcher) {
        val projectId = UUID.randomUUID().toString()
        val mapsDir = File(context.filesDir, "maps/$projectId")
        if (!mapsDir.exists()) {
            mapsDir.mkdirs()
        }
        val targetPdf = File(mapsDir, "source.pdf")

        try {
            // 1. Resolve original filename
            val rawName = resolveFileName(uri)
            val mapTitle = if (rawName.endsWith(".pdf", ignoreCase = true)) {
                rawName.substring(0, rawName.length - 4)
            } else {
                rawName
            }

            // 2. Copy SAF stream to internal storage
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                targetPdf.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Tidak dapat membaca data dari file yang dipilih.")

            // 3. Validate and parse GeoPDF metadata
            when (val parseResult = geoPdfParser.parse(targetPdf, pageIndex = 0)) {
                is GeoPdfParseResult.Success -> {
                    val model = parseResult.model
                    val entity = MapProjectEntity(
                        id = projectId,
                        name = mapTitle,
                        pdfLocalPath = targetPdf.absolutePath,
                        pageWidthPt = model.pageWidthPt,
                        pageHeightPt = model.pageHeightPt,
                        geoStandard = model.geoStandard.name,
                        epsgCode = model.epsgCode,
                        transformMatrixJson = model.transformMatrix.toJson(),
                        createdAtEpochMs = System.currentTimeMillis()
                    )
                    mapProjectDao.insert(entity)

                    val item = MapProjectItem(
                        id = entity.id,
                        name = entity.name,
                        pdfLocalPath = entity.pdfLocalPath,
                        pageWidthPt = entity.pageWidthPt,
                        pageHeightPt = entity.pageHeightPt,
                        geoStandard = entity.geoStandard,
                        epsgCode = entity.epsgCode,
                        waypointCount = 0,
                        photoCount = 0,
                        createdAtEpochMs = entity.createdAtEpochMs,
                        formattedDate = dateFormatter.format(Date(entity.createdAtEpochMs))
                    )
                    Result.success(item)
                }
                is GeoPdfParseResult.NotGeoPdf -> {
                    cleanUpDirectory(mapsDir)
                    Result.failure(NotGeoPdfException())
                }
                is GeoPdfParseResult.Error -> {
                    cleanUpDirectory(mapsDir)
                    Result.failure(GeoPdfParseException(parseResult.message, parseResult.cause))
                }
            }
        } catch (e: Exception) {
            cleanUpDirectory(mapsDir)
            Result.failure(e)
        }
    }

    override suspend fun deleteMapProject(projectId: String) = withContext(ioDispatcher) {
        val project = mapProjectDao.getProjectByIdSync(projectId)
        if (project != null) {
            val file = File(project.pdfLocalPath)
            cleanUpDirectory(file.parentFile)
            mapProjectDao.deleteById(projectId)
        }
    }

    private fun resolveFileName(uri: Uri): String {
        var result = "Peta_${System.currentTimeMillis()}"
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = it.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                result = name
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to default name
            }
        } else if (uri.scheme == "file") {
            uri.lastPathSegment?.let { segment ->
                if (segment.isNotBlank()) {
                    result = segment
                }
            }
        }
        return result
    }

    private fun cleanUpDirectory(dir: File?) {
        if (dir != null && dir.exists()) {
            dir.deleteRecursively()
        }
    }

    private fun MapProjectSummary.toItem(): MapProjectItem {
        return MapProjectItem(
            id = project.id,
            name = project.name,
            pdfLocalPath = project.pdfLocalPath,
            pageWidthPt = project.pageWidthPt,
            pageHeightPt = project.pageHeightPt,
            geoStandard = project.geoStandard,
            epsgCode = project.epsgCode,
            waypointCount = waypointCount,
            photoCount = photoCount,
            createdAtEpochMs = project.createdAtEpochMs,
            formattedDate = dateFormatter.format(Date(project.createdAtEpochMs))
        )
    }
}
