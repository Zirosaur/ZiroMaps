package com.zirosaur.ziromaps.core.export.model

enum class ExportFormat(
    val displayName: String,
    val fileExtension: String,
    val mimeType: String
) {
    GEOJSON("GeoJSON", "geojson", "application/geo+json"),
    CSV("CSV (Excel/QGIS)", "csv", "text/csv"),
    KMZ("KMZ (Google Earth)", "kmz", "application/vnd.google-earth.kmz")
}
