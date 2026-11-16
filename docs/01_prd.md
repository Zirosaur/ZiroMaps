# 01 — Product Requirements Document (PRD)

> **Status:** Draft v1.0 | **Peran Dokumen:** SSOT Ruang Lingkup Produk & Batasan MVP

## 1. Ringkasan & Visi Produk

Membangun satu aplikasi Android independen berbasis **offline-first** yang menggabungkan:
1. **Offline GeoPDF Map Viewer** (setara fungsi inti Avenza Maps): membuka peta PDF bergeoreferensi, menampilkan posisi GPS *real-time* di atas peta, serta merekam titik (*waypoint*) dan jalur (*track*).
2. **Geotag Stamp Camera** (setara fungsi inti Timestamp Camera): mengambil foto dokumentasi lapangan dengan *watermark* koordinat, waktu, dan arah kompas, sekaligus menanam metadata EXIF GPS dan menautkan foto otomatis ke *pin* pada peta.

---

## 2. Target Pengguna & Konteks Penggunaan

- **Pengguna Utama:** Surveyor lapangan, petugas kehutanan/perkebunan, tim pemetaan, dan peneliti lapangan.
- **Kondisi Lingkungan:** Area kerja tanpa sinyal seluler/internet (*blank spot*), membutuhkan konsumsi baterai efisien, pengoperasian cepat dengan satu tangan, dan akurasi koordinat yang dapat diaudit.

---

## 3. Ruang Lingkup MVP (Fitur Wajib)

### A. Manajemen Peta Offline (GeoPDF)
- Impor file `.pdf` (GeoPDF) dari penyimpanan lokal perangkat melalui Android Storage Access Framework (SAF).
- Validasi otomatis apakah PDF memiliki metadata georeferensi (`ISO 32000` atau `OGC LGIDict`).
- Daftar proyek peta tersimpan lokal dengan informasi nama, sistem proyeksi, dan tanggal impor.

### B. Navigasi Peta & Perekaman Spasial
- *Pan* dan *zoom* mulus pada peta PDF resolusi tinggi tanpa *crash* memori.
- Indikator posisi pengguna (*blue dot*) *real-time* di atas peta lengkap dengan lingkaran akurasi GPS dan arah hadap kompas (*azimuth*).
- Panel info koordinat *live* (mendukung format **WGS84 Decimal/DMS** dan **UTM**).
- Tambah *Waypoint* (pin manual di tengah crosshair atau di posisi GPS saat ini) dengan judul, kategori, dan catatan.
- Rekam *Track* (jejak perjalanan GPS otomatis berdasarkan interval waktu/jarak).

### C. Kamera Stamp Terintegrasi
- Tombol akses cepat kamera langsung dari layar peta (otomatis terhubung ke peta yang sedang aktif).
- Pengambilan foto dengan *overlay watermark* permanen pada gambar:
  - Koordinat (WGS84 & UTM) + Akurasi (meter)
  - Tanggal, jam lokal, dan zona waktu
  - Arah kompas (*azimuth* dalam derajat)
  - Nama proyek peta & catatan singkat opsional
- Penulisan metadata GPS & arah kompas ke tag EXIF file JPEG asli.
- Setiap foto yang diambil otomatis membuat *Waypoint* berikon kamera di titik koordinat foto pada peta aktif.

### D. Ekspor Data Independen
- Ekspor seluruh *waypoint* dan *track* dari suatu proyek peta ke **GeoJSON** atau **KMZ/KML** (beserta lampiran foto).
- Ekspor ringkasan data titik ke **CSV** untuk dibuka di Excel/QGIS.

---

## 4. Out of Scope (Dilarang di Fase MVP)

Fitur berikut **tidak boleh** dikerjakan pada fase MVP agar proyek tetap fokus:
- Akun pengguna, autentikasi/login, dan sinkronisasi *cloud* (*backend server*).
- *Map store* atau katalog pembelian/unduhan peta online.
- *Basemap* vektor/satelit online (Google Maps / Mapbox / OSM tiles online).
- Pengeditan geometri poligon kompleks atau *geoprocessing* (buffer, intersect) di dalam aplikasi.
- Dukungan format CAD (`.dwg`/`.dxf`) atau *shapefile* mentah sebagai *basemap*.

---

## 5. Kriteria Keberhasilan MVP

1. **Akurasi Transformasi:** Titik GPS uji pada `sample-data/` jatuh pada piksel peta dengan galat tidak melebihi toleransi *rendering* peta (target < 2 meter di skala 1:10.000).
2. **Stabilitas Memori:** Peta GeoPDF ukuran A0/A1 (> 50 MB) dapat dibuka dan di-*zoom* tanpa *OutOfMemoryError* pada perangkat RAM 4 GB.
3. **Keutuhan Data Foto:** Foto hasil tangkapan memiliki *watermark* terbaca jelas dan tag EXIF GPS terbaca valid saat diimpor ke QGIS (*Import Geotagged Photos*).
