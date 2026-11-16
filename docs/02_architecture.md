# 02 — System Architecture & Tech Stack

> **Status:** Draft v1.0 | **Peran Dokumen:** SSOT Struktur Modul, Teknologi Terkunci, dan Pola Desain

## 1. Tech Stack Terkunci

Seluruh modul wajib menggunakan versi dan pustaka berikut (perubahan versi mayor wajib melalui persetujuan & update dokumen ini):

| Lapisan | Teknologi / Library | Catatan Penggunaan |
| :--- | :--- | :--- |
| Build System & Tooling | AGP `9.5.0` + Gradle `9.7.1` + KSP `2.3.14` | Kompilasi Kotlin terintegrasi native di AGP 9 |
| Bahasa & Async | Kotlin `2.4.21` + Coroutines `1.9.0` + Flow | *Structured concurrency* untuk I/O PDF & lokasi |
| UI Framework | Jetpack Compose (BOM `2024.09.00`) + Material 3 | Single-Activity architecture dengan Compose Navigation (`2.8.4`) |
| Dependency Injection | Hilt `2.60.1` (via KSP) | Mengikat implementasi antarmodul `:core:*` |
| GeoPDF Parsing | `PdfBox-Android` `2.0.27.0` (Apache 2.0) | Membaca dictionary `VP`, `Measure`, `GPTS`, dan `LGIDict` |
| PDF Tile Rendering | `PdfRenderer` / `Pdfium` + Disk/LRU Tile Cache | Merender bagian peta ke *bitmap tiles* agar bebas OOM |
| Kamera & EXIF | AndroidX `CameraX` `1.4.1` + `ExifInterface` `1.3.7` | *Preview*, *ImageCapture*, dan penulisan metadata GPS |
| Lokasi & Sensor | `FusedLocationProviderClient` (`21.3.0`) + `SensorManager` | Koordinat GPS + *Rotation Vector* untuk *azimuth* |
| Database Lokal | AndroidX `Room` `2.8.5` (SQLite via KSP) | Menyimpan metadata proyek, *waypoint*, *track*, dan relasi foto |
| Unit & UI Test | JUnit `4.13.2` / MockK `1.13.13` / Turbine `1.2.0` / Compose Test | Pengujian logika parser, konversi koordinat, dan ViewModel |

---

## 2. Struktur Modul Gradle

Proyek dipecah menjadi modul independen untuk mencegah kode saling mengikat (*tight coupling*):

```text
app/                  # Entry point, DI graph, Navigation host, layar utama
├── core/
│   ├── geopdf/       # Parser metadata VP/LGIDict, transformasi affine, tile generator
│   ├── location/     # Location provider Flow, sensor kompas, konverter WGS84 <-> UTM
│   ├── camera/       # CameraX wrapper, Canvas watermark renderer, EXIF writer
│   ├── database/     # Room database, DAO, Entity, migrasi skema
│   └── export/       # Generator GeoJSON, KML/KMZ, dan CSV
```

### Aturan Dependensi Antarmodul
- `:app` boleh bergantung pada semua modul `:core:*`.
- `:core:geopdf`, `:core:location`, `:core:camera`, dan `:core:database` **tidak boleh** saling bergantung secara langsung; koordinasi antarmodul dilakukan di layer `:app` (ViewModel / UseCase).
- `:core:export` hanya boleh membaca model domain murni (atau bergantung pada kontrak dari `:core:database`).

---

## 3. Arsitektur Rendering GeoPDF (Anti-OOM)

1. **Pra-pemrosesan Saat Impor:**
   - Salin file PDF dari URI SAF ke penyimpanan internal aplikasi (`filesDir/maps/<project_id>/source.pdf`).
   - Ekstrak metadata georeferensi menggunakan `:core:geopdf` dan simpan matriks transformasinya ke Room.
2. **Tile Pyramid & Region Rendering:**
   - Halaman peta tidak pernah di-decode penuh ke memori pada skala zoom tinggi.
   - Kanvas peta meminta *tile* berukuran tetap (misal `512x512 px`) sesuai *viewport* layar yang terlihat dan tingkat zoom aktif, dengan batas cache memori (LRU) dan *disk cache* lokal.

---

## 4. Alur Data Kamera & Penautan Otomatis

1. Pengguna menekan tombol kamera dari layar peta aktif.
2. `:core:location` menyuplai snapshot lokasi terakhir (`lat`, `lon`, `alt`, `accuracy`, `azimuth`, `timestamp`).
3. `:core:camera` menangkap gambar resolusi penuh, menggambar *watermark* via `Canvas` di *background thread* (`Dispatchers.Default`), menulis file JPEG ke `filesDir/photos/<project_id>/`, dan menyematkan tag EXIF.
4. `:app` menerima path file foto + snapshot koordinat, lalu menyimpan entri `PhotoRecordEntity` dan otomatis membuat `WaypointEntity` baru di `:core:database` agar pin langsung muncul di peta.
