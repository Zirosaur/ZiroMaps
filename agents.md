# AGENTS.md — SOP Eksekusi & Aturan Kerja Coding Agent

Dokumen ini adalah **Standard Operating Procedure (SOP)** wajib bagi setiap AI *coding agent* yang bekerja di repositori ini. Seluruh keputusan produk, arsitektur, skema data, dan UI mengacu pada folder `docs/` sebagai **Single Source of Truth (SSOT)**.

---

## 1. Peta Dokumen SSOT (Wajib Dibaca Sebelum Coding)

Sebelum menulis atau mengubah kode apa pun, baca dokumen yang relevan dengan tugas:

| Dokumen | Cakupan | Kapan Wajib Dibaca |
| :--- | :--- | :--- |
| `docs/01-prd.md` | Scope MVP, alur pengguna, dan daftar *out-of-scope* | Saat memulai fitur baru atau memastikan batasan fitur |
| `docs/02-architecture.md` | Struktur modul Gradle, *tech stack* terkunci, dan aturan dependensi | Sebelum membuat file/kelas baru atau menambah dependensi |
| `docs/03-data-spec.md` | Skema Room DB, kontrak parser GeoPDF, rumus koordinat, dan spec EXIF/stamp | Sebelum menyentuh database, parser PDF, lokasi, atau kamera |
| `docs/04-ui-ux-spec.md` | Daftar layar, navigasi Compose, state UI, dan penanganan *edge case* | Sebelum membangun atau mengubah komponen UI |
| `docs/05-roadmap-tasks.md` | Daftar *task* atomik berurutan per fase | Di awal setiap sesi untuk memilih tepat 1 *task* |
| `docs/adr/` | Catatan keputusan arsitektur (*Architecture Decision Records*) | Saat ragu mengapa suatu library atau pola dipilih |

---

## 2. Aturan Keras (Hard Rules)

1. **SSOT-First & Doc-First:**
   - Dilarang mengarang skema tabel, format koordinat, atau alur UI di luar yang tertulis di `docs/`.
   - Jika implementasi nyata menuntut perubahan skema DB, kontrak API internal, atau library, **perbarui dokumen SSOT terkait terlebih dahulu** (dan catat alasannya di `docs/adr/` jika bersifat arsitektural) sebelum mengubah kode.
2. **Kunci Tech Stack:**
   - Dilarang mengganti *tech stack* inti (Kotlin, Jetpack Compose, CameraX, PdfBox-Android, Room) atau menambah library pihak ketiga baru tanpa persetujuan eksplisit pengguna.
   - Dilarang menggunakan `android.graphics.pdf.PdfRenderer` untuk membaca metadata koordinat GeoPDF; seluruh pembacaan struktur objek PDF wajib melalui modul `:core:geopdf`.
3. **Batas Scope (Anti Over-Engineering):**
   - Kerjakan **hanya 1 task** dari `docs/05-roadmap-tasks.md` per sesi.
   - Dilarang mengimplementasikan fitur yang masuk daftar *Out of Scope* di `docs/01-prd.md` (seperti *cloud sync*, *login*, atau *map store*).
4. **Offline-First & Privasi Data:**
   - Semua fitur inti (buka GeoPDF, GPS tracking, kamera stamp, simpan pin, ekspor file) wajib berjalan **100% offline** tanpa panggilan jaringan.
   - Dilarang menanam *hardcoded API key*, *token*, atau pelacak analitik pihak ketiga.
5. **Integritas Batas Modul:**
   - Patuhi arah dependensi di `docs/02-architecture.md`. Modul `:core:*` tidak boleh bergantung pada `:app` atau satu sama lain secara siklik.

---

## 3. Alur Kerja Standar per Task

Setiap kali menerima tugas, jalankan urutan berikut secara disiplin:

1. **Identifikasi & Baca:** Baca task target di `docs/05-roadmap-tasks.md` beserta bagian SSOT yang terkait di `docs/01-prd.md` s.d. `04-ui-ux-spec.md`.
2. **Inspeksi Kode:** Periksa file eksisting di modul terkait agar tidak menduplikasi utilitas atau melanggar pola yang sudah ada.
3. **Implementasi Terfokus:** Tulis kode yang bersih, *idiomatic Kotlin*, dengan komentar singkat hanya pada bagian non-trivial (misal matriks transformasi affine GeoPDF).
4. **Tulis/Perbarui Unit Test:** Setiap logika bisnis (terutama parser GeoPDF, konversi WGS84/UTM, dan penulisan EXIF) wajib memiliki unit test yang memverifikasi kasus normal dan *edge case*.
5. **Verifikasi Otomatis (Gerbang Wajib):** Jalankan perintah di Bagian 4 sebelum menyatakan task selesai.
6. **Update Status Task:** Tandai task selesai (`[x]`) di `docs/05-roadmap-tasks.md` hanya setelah semua verifikasi lulus.

---

## 4. Definition of Done (DoD) & Perintah Verifikasi

Sebuah task dianggap selesai **hanya jika** seluruh perintah berikut berjalan sukses tanpa error:

```bash
# 1. Jalankan unit test seluruh modul
./gradlew testDebugUnitTest

# 2. Jalankan Android Lint check
./gradlew lintDebug

# 3. Pastikan APK debug berhasil di-build
./gradlew assembleDebug
```

Aturan pelaporan akhir kepada pengguna:
- Sebutkan file apa saja yang dibuat/diubah.
- Sertakan ringkasan hasil eksekusi perintah verifikasi di atas secara jujur (jangan pernah mengklaim lulus jika belum dijalankan atau ada tes yang gagal).
- Jika ada *blocker* atau ambiguitas pada SSOT, berhenti dan tanyakan ke pengguna daripada menebak.

---

## 5. Konvensi Kode & Git

- **Bahasa Kode:** Gunakan Bahasa Inggris untuk nama kelas, fungsi, variabel, dan komentar teknis; gunakan Bahasa Indonesia untuk *string resource* UI (`strings.xml`) kecuali diminta lain.
- **State Management:** Gunakan unidirectional data flow (`ViewModel` + `StateFlow` + `UiState` *immutable data class*).
- **Conventional Commits:** Gunakan format `<type>(<scope>): <ringkasan>`, contoh:
  - `feat(geopdf): parse ISO 32000 VP and GPTS dictionary`
  - `fix(camera): prevent bitmap OOM on high-res watermark stamp`
  - `docs(data-spec): add azimuth field to PhotoRecordEntity`
