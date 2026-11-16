# 03 — Data, GeoPDF & EXIF Specification

> **Status:** Draft v1.0 | **Peran Dokumen:** SSOT Skema Database, Kontrak Parser GeoPDF, dan Format Stamp/EXIF

## 1. Skema Database Lokal (Room)

Semua entitas disimpan di `AppDatabase` (`:core:database`).

### `MapProjectEntity`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `String` (UUID, PK) | Identitas unik proyek peta |
| `name` | `String` | Nama peta yang ditampilkan ke pengguna |
| `pdfLocalPath` | `String` | Path absolut file PDF di `filesDir` |
| `pageWidthPt` | `Double` | Lebar halaman PDF dalam satuan *points* (72 DPI) |
| `pageHeightPt` | `Double` | Tinggi halaman PDF dalam satuan *points* |
| `geoStandard` | `String` | `ISO_32000` atau `OGC_LGIDICT` |
| `epsgCode` | `Int?` | Kode proyeksi EPSG jika terdeteksi (misal `4326`, `32749`) |
| `transformMatrixJson` | `String` | Serialisasi matriks transformasi affine (PDF pt <-> WGS84) |
| `createdAtEpochMs` | `Long` | Waktu impor peta |

### `WaypointEntity`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `String` (UUID, PK) | Identitas unik titik |
| `projectId` | `String` (FK) | Referensi ke `MapProjectEntity.id` (CASCADE delete) |
| `title` | `String` | Judul titik (default: `WP-001` atau `FOTO-001`) |
| `note` | `String` | Catatan lapangan |
| `latitude` | `Double` | Lintang WGS84 |
| `longitude` | `Double` | Bujur WGS84 |
| `altitudeM` | `Double?` | Ketinggian dalam meter |
| `accuracyM` | `Float?` | Akurasi GPS saat pengambilan (meter) |
| `hasPhoto` | `Boolean` | Penanda apakah titik memiliki lampiran foto |
| `createdAtEpochMs` | `Long` | Waktu pembuatan titik |

### `PhotoRecordEntity`
| Kolom | Tipe | Keterangan |
| :--- | :--- | :--- |
| `id` | `String` (UUID, PK) | Identitas unik foto |
| `waypointId` | `String` (FK) | Referensi ke `WaypointEntity.id` (CASCADE delete) |
| `filePath` | `String` | Path file JPEG final (sudah ber-watermark & EXIF) |
| `azimuthDeg` | `Float?` | Arah hadap kompas saat memotret (0–360 derajat) |
| `capturedAtEpochMs` | `Long` | Waktu pengambilan foto |

### `TrackEntity` & `TrackPointEntity`
- `TrackEntity`: `id` (UUID, PK), `projectId` (FK), `name`, `startedAtEpochMs`, `endedAtEpochMs?`, `totalDistanceM`.
- `TrackPointEntity`: `id` (Long auto-increment, PK), `trackId` (FK), `latitude`, `longitude`, `altitudeM?`, `accuracyM?`, `recordedAtEpochMs`.

---

## 2. Kontrak Parser GeoPDF (`:core:geopdf`)

Parser wajib mencoba membaca dua standar georeferensi berikut secara berurutan:

1. **ISO 32000-1 ExtensionLevel 3 (Prioritas 1 — Standar QGIS Modern):**
   - Baca array `VP` (*Viewport*) pada objek halaman PDF.
   - Di dalam entri `VP`, baca `BBox` (area peta pada halaman dalam satuan points) dan kamus `Measure` (`Subtype = /GEO`).
   - Ekstrak `Bounds` (koordinat relatif viewport), `GPTS` (pasangan lat/lon titik sudut geografis), dan `GCS` / `DCS` (sistem proyeksi / WKT).
2. **OGC GeoPDF 2.2 / TerraGo (Prioritas 2 — Standar Legacy/ArcGIS):**
   - Baca array/kamus `LGIDict` pada objek halaman PDF.
   - Ekstrak `CTM` (*Coordinate Transformation Matrix*), `Projection`, serta batas *neatline*.
3. **Output Parser (`GeoTransformModel`):**
   - Harus menyediakan dua fungsi deterministik:
     - `latLonToPagePoint(lat: Double, lon: Double): PagePoint?` (mengembalikan `null` jika di luar *bounds* atau gagal proyeksi).
     - `pagePointToLatLon(xPt: Double, yPt: Double): LatLon?`

---

## 3. Spesifikasi Konversi Koordinat (`:core:location`)

- Seluruh penyimpanan internal menggunakan **WGS84 (`EPSG:4326`)**.
- Untuk tampilan UI dan *watermark* foto, sediakan konverter ke **UTM**:
  - Format output: `<Zone><Band> <Easting>m E <Northing>m N` (contoh: `49M 301450m E 9176210m N` untuk wilayah Jawa Tengah).

---

## 4. Spesifikasi Watermark & EXIF Foto (`:core:camera`)

### Layout Teks Watermark (Canvas Overlay)
Ditulis di pojok kiri bawah foto dengan kotak latar semi-transparan (`#80000000`) dan teks putih ber-padding proporsional terhadap resolusi foto:
```text
Lat: -7.431250°  Lon: 109.234510° (±4.2 m)
UTM: 49M 305120m E 9178110m N  |  Azimuth: 142° SE
Waktu: 2026-11-15 22:05:10 WIB
Peta: [Nama Proyek Peta] | [Catatan Opsional]
```

### Tag EXIF Wajib (`androidx.exifinterface.media.ExifInterface`)
- `TAG_GPS_LATITUDE` & `TAG_GPS_LATITUDE_REF` (`N`/`S`)
- `TAG_GPS_LONGITUDE` & `TAG_GPS_LONGITUDE_REF` (`E`/`W`)
- `TAG_GPS_ALTITUDE` & `TAG_GPS_ALTITUDE_REF`
- `TAG_GPS_IMG_DIRECTION` & `TAG_GPS_IMG_DIRECTION_REF` (`T` untuk True North / `M` untuk Magnetic)
- `TAG_DATETIME_ORIGINAL` & `TAG_OFFSET_TIME_ORIGINAL`
