# 04 — UI/UX & Edge Case Specification

> **Status:** Draft v1.0 | **Peran Dokumen:** SSOT Navigasi Layar, Komponen UI, dan Penanganan Error

## 1. Peta Navigasi & Daftar Layar

1. **`MapLibraryScreen` (Layar Awal):**
   - Daftar proyek GeoPDF yang sudah diimpor (kartu berisi nama peta, jumlah *waypoint*, jumlah foto, dan tanggal).
   - Tombol FAB `+ Impor GeoPDF` (memanggil *System File Picker* khusus `application/pdf`).
2. **`MapViewerScreen` (Layar Kerja Utama):**
   - Kanvas penuh menampilkan peta PDF dengan gestur *pan*, *pinch-to-zoom*, dan *double-tap zoom*.
   - *Overlay* atas: Bar status koordinat *live* (tap untuk berganti antara WGS84 dan UTM) + badge akurasi GPS.
   - *Overlay* peta: Titik posisi pengguna (*blue dot* + kerucut arah kompas), *waypoint pins*, ikon foto, dan garis *track*.
   - *Overlay* bawah: Tombol aksi cepat — `Pusatkan GPS`, `Tambah Pin`, `Rekam Track`, dan tombol menonjol `Kamera Stamp`.
3. **`CameraStampScreen` (Layar Kamera Terintegrasi):**
   - *Preview* kamera penuh dengan pratinjau kotak *watermark* di pojok kiri bawah secara *live*.
   - Kolom input cepat opsional untuk catatan singkat foto sebelum jepret.
   - Tombol shutter + indikator status GPS fix (jika belum ada fix, tampilkan peringatan akurasi).
4. **`WaypointDetailSheet` (Modal Bottom Sheet):**
   - Muncul saat pin pada peta di-tap: menampilkan koordinat, catatan, galeri foto tertaut, dan tombol hapus/edit.
5. **`ExportDialog`:**
   - Pilihan format ekspor (`GeoJSON`, `KMZ`, `CSV`) dan *Android Share Sheet* untuk mengirim hasil ke WhatsApp/Drive/penyimpanan lokal.

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
