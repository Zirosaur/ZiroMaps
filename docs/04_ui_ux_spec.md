# 04 — UI/UX & Edge Case Specification

> **Status:** Revised v2.0 (Avenza & Timestamp Camera Ergonomics) | **Peran Dokumen:** SSOT Navigasi Layar, Komponen UI, Ergonomi Lapangan, dan Penanganan Error

## 1. Peta Navigasi & Daftar Layar

1. **`MapLibraryScreen` (Layar Awal - Standar Ergonomi Avenza Maps):**
   - **Bilah Pencarian Cepat (Quick Search Bar):** Kolom pencarian di bagian atas dengan tombol *clear* untuk memfilter peta berdasarkan nama secara instan.
   - **Kartu Proyek Peta (Rich Map Cards):**
     - Thumbnail pratinjau halaman pertama PDF (di-*cache* lokal agar instan dan hemat memori).
     - Nama peta (elipsis 2 baris jika panjang), ukuran file format memori (`MB`/`KB`), dan tanggal impor.
     - Chip metrik: jumlah *Waypoint*, *Track*, dan *Foto*.
     - Menu aksi cepat: buka peta, ekspor, atau hapus proyek.
   - **Tombol FAB `+ Impor GeoPDF`:** Tombol mengambang di pojok kanan bawah memanggil Android Storage Access Framework (SAF) khusus tipe MIME `application/pdf`.

2. **`MapViewerScreen` (Layar Kerja Utama - Standar Ergonomi Avenza Maps):**
   - **Full-Bleed Map Canvas:** Kanvas peta memenuhi layar (*edge-to-edge*), dengan seluruh kontrol mengambang menghormati `WindowInsets` (`statusBarsPadding` & `navigationBarsPadding`).
   - **Center Crosshair:** Crosshair tipis dengan titik pusat di tengah layar saat mode navigasi peta untuk membidik koordinat titik secara presisi.
   - **Live Coordinate Status Bar:**
     - Menampilkan koordinat live yang dapat di-tap untuk berganti format secara siklik (`WGS84 Decimal` -> `WGS84 DMS` -> `UTM`).
     - Badge akurasi GPS (`±m`) dengan warna indikator (Hijau jika akurasi tinggi <= 15m, Kuning jika <= 30m, Oranye jika mencari/lemah), elevasi (`m`), dan azimuth hadap.
   - **Floating Map Controls (Atas & Samping):**
     - **Kompas Mengambang (Floating Compass):** Ikon jarum kompas berputar mengikuti rotasi peta; tap tombol kompas untuk mereset orientasi peta tegak lurus ke Utara (0°).
     - **Skala Peta Dinamis (Dynamic Scale Bar):** Batang skala visual (misal 50m, 100m, 500m, 1km) yang terkalibrasi otomatis terhadap tingkat zoom dan resolusi piksel peta.
   - **Bottom Action Bar (Ergonomis Satu Tangan):**
     - **Pusatkan GPS (GPS Center Mode):** Tombol 3 status siklus:
       1. *Idle / Free Pan*: Navigasi peta bebas.
       2. *Follow Location*: Peta otomatis berpusat pada posisi *blue dot* GPS.
       3. *Compass Heading*: Peta berpusat pada GPS dan otomatis berputar mengikuti hadap kompas.
     - **Tambah Pin (Add Pin):** Dialog cepat menambah waypoint dengan opsi posisi: `Di Titik Tengah Crosshair` atau `Di Posisi GPS Saat Ini`.
     - **Rekam Track (Record Track):** Tombol toggle mulai/henti rekam track dengan chip status mengambang menampilkan *timer live* (HH:MM:SS) dan akumulasi jarak tempuh (km).
     - **Layers / Daftar Objek (Layers Sheet):** Membuka modal bottom sheet berisi tab daftar Waypoints dan Tracks pada peta aktif dengan pencarian cepat, navigasi fokus pin, dan hapus.
     - **Kamera Stamp:** Tombol menonjol dengan kontras tinggi untuk meluncurkan kamera geotagging terintegrasi.
   - **Top Bar Overlay:** Tombol Kembali, Coordinate Status Bar, dan Tombol Berbagi (*Share / Export*).

3. **`CameraStampScreen` (Kamera Dokumentasi - Standar Ergonomi Timestamp Camera):**
   - **Viewfinder Ergonomis:**
     - *Preview CameraX* penuh (*full bleed* edge-to-edge).
     - Tombol *shutter* fisik/layar yang besar dan menonjol.
     - Toggle Flash kamera (`Auto` / `On` / `Off`).
     - Kontrol Zoom Cepat (`0.6x` / `1x` / `2x` disesuaikan batas rasio hardware kamera perangkat).
     - Thumbnail foto terakhir di samping tombol shutter untuk pratinjau cepat hasil tangkapan.
   - **Live WYSIWYG Watermark Preview:**
     - Kotak watermark semi-transparan yang ditampilkan secara *real-time* di atas preview kamera persis 1:1 dengan gambar hasil jepretan.
   - **Quick Stamp Settings (Modal Bottom Sheet):**
     - Pengaturan cepat konfigurasi stempel yang dapat dibuka langsung dari layar kamera via tombol setelan:
       - **Toggle Field Stempel:** Koordinat Lat/Lon, Koordinat UTM, Azimuth Hadap, Elevasi GPS, Tanggal & Jam, Nama Peta Proyek, dan Catatan Lapangan.
       - **Posisi Stempel:** Kiri Bawah (*default*), Kanan Bawah, Kiri Atas.
       - **Ukuran Teks Stempel:** Kecil, Sedang (*default*), Besar.
       - **Input Catatan Cepat:** Kolom input catatan lapangan pra-jepret yang langsung tercetak di atas foto dan tersimpan pada metadata waypoint.
     - **Persistensi Lokal:** Seluruh preferensi stempel tersimpan di penyimpanan lokal (*SharedPreferences* / *DataStore*) dan disinkronkan langsung ke `CanvasWatermarkRenderer` di `:core:camera`.

4. **`WaypointDetailSheet` (Modal Bottom Sheet):**
   - Muncul saat pin pada peta di-tap: menampilkan judul titik, koordinat WGS84 & UTM, elevasi, akurasi, waktu pencatatan, catatan lapangan, galeri foto berstempel, tombol edit, dan tombol hapus.

5. **`LayersBottomSheet` (Modal Bottom Sheet Daftar Layer & Objek):**
   - Menampilkan ringkasan seluruh objek pada peta aktif:
     - Tab **Waypoints**: Daftar seluruh titik pin dengan ikon foto/pin standar, jarak dari lokasi pengguna, dan tombol fokus ke peta.
     - Tab **Tracks**: Daftar track yang tersimpan, panjang jarak (km), durasi rekaman, dan opsi ekspor/hapus.

6. **`ExportDialog`:**
   - Pilihan format ekspor (`GeoJSON`, `KMZ`, `CSV`) dan *Android Share Sheet* untuk mengirim hasil ke WhatsApp/Drive/penyimpanan lokal tanpa internet.

---

## 2. State UI Wajib

Setiap layar yang memproses I/O atau sensor wajib memodelkan state berikut secara eksplisit:
- **`Loading`:** Progress indicator saat menyalin dan memproses metadata PDF baru.
- **`Empty`:** Ilustrasi/teks ajakan mengimpor GeoPDF pertama saat `MapLibraryScreen` kosong.
- **`Error`:** Pesan error yang informatif dalam Bahasa Indonesia jika file gagal diparsing.

---

## 3. Matriks Penanganan Edge Case

| Kondisi / Edge Case | Perilaku Wajib Aplikasi |
| :--- | :--- |
| **PDF tidak punya metadata GeoPDF** | Tolak impor atau tampilkan dialog jelas bahwa PDF bukan GeoPDF (belum mendukung *manual georeferencing* di MVP). |
| **Izin Lokasi atau Kamera ditolak** | Tampilkan penjelasan alasan izin dibutuhkan dan tombol pintas menuju *App Settings*. |
| **GPS belum mendapat *fix* (atau akurasi > 30 m)** | Izinkan pengguna tetap memotret jika mendesak, namun beri label jelas `Akurasi Rendah` atau `Mencari GPS` pada *watermark* agar data tidak menyesatkan. |
| **Aplikasi di-background saat *Track* aktif** | Gunakan *Foreground Service* dengan notifikasi permanen agar perekaman jalur GPS tidak dimatikan oleh sistem Android. |
| **Orientasi foto Portrait vs Landscape** | Putar *bitmap* sesuai rotasi sensor/EXIF terlebih dahulu sebelum menggambar *watermark* `Canvas` agar teks stamp tidak miring atau terpotong. |
