# 05 — Roadmap & Atomic Tasks

> **Status:** Draft v1.0 | **Aturan Eksekusi:** Agent wajib mengerjakan **1 task per sesi** secara berurutan dan menandai `[x]` setelah seluruh perintah verifikasi di `AGENTS.md` lulus.

---

## Phase 0: Setup Fondasi Proyek
- [x] **T0.1:** Inisialisasi proyek Android (Kotlin, Compose, Material 3, Hilt) dengan struktur multi-modul (`:app`, `:core:geopdf`, `:core:location`, `:core:camera`, `:core:database`, `:core:export`).
- [x] **T0.2:** Konfigurasi `libs.versions.toml` sesuai versi terkunci di `docs/02-architecture.md` dan pastikan `./gradlew assembleDebug` sukses.

## Phase 1: Core GeoPDF & Penyimpanan Lokal
- [x] **T1.1:** Implementasi entitas dan DAO di `:core:database` sesuai `docs/03-data-spec.md` beserta unit test migrasi/CRUD Room.
- [x] **T1.2:** Implementasi parser metadata `ISO 32000` (`VP`, `Measure`, `GPTS`, `Bounds`) di `:core:geopdf` + unit test menggunakan file di `sample-data/`.
- [x] **T1.3:** Tambahkan dukungan fallback parser `OGC LGIDict` (`CTM`) di `:core:geopdf` + unit test.
- [x] **T1.4:** Implementasi *tile renderer* PDF + LRU/disk cache di `:core:geopdf` untuk mencegah OOM pada peta berukuran besar.

## Phase 2: Lokasi, Kompas & Map Viewer
- [x] **T2.1:** Implementasi `:core:location` (`FusedLocationProviderClient` Flow, *azimuth* kompas, dan konverter `WGS84 <-> UTM` + unit test akurasi UTM).
- [x] **T2.2:** Bangun `MapLibraryScreen` dengan alur impor SAF dan validasi GeoPDF.
- [x] **T2.3:** Bangun `MapViewerScreen` (kanvas *pan/zoom* tile PDF + *blue dot* GPS *real-time* + panel koordinat WGS84/UTM).
- [x] **T2.4:** Implementasi fitur tambah *Waypoint* manual, *WaypointDetailSheet*, dan perekaman *Track* (termasuk *Foreground Service*).

## Phase 3: Kamera Stamp Terintegrasi
- [x] **T3.1:** Implementasi modul `:core:camera` untuk *preview* dan *capture* menggunakan `CameraX` dengan penanganan rotasi orientasi perangkat.
- [x] **T3.2:** Implementasi *Canvas Watermark Renderer* (menggambar koordinat, akurasi, UTM, azimuth, waktu, dan catatan ke *bitmap*) + *EXIF GPS Writer*.
- [x] **T3.3:** Hubungkan `CameraStampScreen` dengan `MapViewerScreen` sehingga setiap foto tersimpan otomatis membuat *pin* foto di peta aktif.

## Phase 4: Ekspor & Uji Lapangan
- [x] **T4.1:** Implementasi `:core:export` untuk menghasilkan file **GeoJSON**, **CSV**, dan **KMZ** (beserta foto) + unit test format output.
- [x] **T4.2:** Integrasikan `ExportDialog` dengan *Android Share Sheet* dan lakukan uji coba lapangan menyeluruh (*offline mode*).
