# Aplikasi Auto-Fishing Clicker Android (Pop-Up Overlay + Deteksi Warna)

Proyek ini adalah aplikasi Android Native (Kotlin) dengan **Menu Pop-Up Mengambang (Floating Overlay)** yang dirancang khusus mengikuti mekanisme memancing pada video Anda:
1. **Mancing Otomatis (Auto-Cast)**: Mendeteksi tombol biru **"Mancing"** dan pop-up hasil tangkapan, lalu melempar kail otomatis.
2. **Angkat Otomatis Deteksi (Auto-Strike)**: Memantau perubahan warna tombol kanan bawah dari abu-abu (**"Angkat"**) menjadi merah-oranye (**"Strike!"**).
3. **Tarik Otomatis Deteksi (Auto-Reel Bar Tracking)**: Memindai bar vertikal untuk melacak posisi **Ikan Hitam** terhadap **Bar Hijau**, lalu menahan/melepas tombol hijau **"Tarik"** secara otomatis.

---

## Cara Mengubah Menjadi File `.APK` Langsung dari HP (Tanpa PC/Laptop)

Karena keamanan Android mengharuskan kode dikompilasi menjadi paket `.apk`, Anda bisa melakukan *build* otomatis secara gratis langsung dari browser HP menggunakan **GitHub Actions**:

1. Buka [github.com](https://github.com) di browser HP Anda dan masuk ke akun Anda.
2. Buat **New Repository** (misalnya `auto-fishing-clicker`).
3. Ekstrak file `AutoFishingClicker_Android.zip` ini di HP Anda, lalu unggah (*upload*) seluruh isinya ke repository tersebut.
4. Buka tab **Actions** di repository GitHub Anda -> klik workflow **Build Debug APK**.
5. Tunggu ~2 menit hingga proses selesai (centang hijau), lalu unduh file **`app-debug.apk`** pada bagian **Artifacts** dan instal di HP Anda!

---

## Cara Menggunakan Aplikasi di Dalam Game
1. Buka aplikasi **Auto Fishing Clicker**, tekan tombol:
   - **1. Izinkan Pop-up Overlay** (centang izin tampil di atas aplikasi lain).
   - **2. Aktifkan Layanan Aksesibilitas** (aktifkan *AutoFishing Accessibility Service* untuk simulasi sentuhan/tahan).
   - **3. Mulai Tangkap Layar & Munculkan Pop-Up** (setujui izin rekam layar untuk deteksi warna piksel).
2. Buka game dalam posisi **Landscape**.
3. Geser **Target Bulat [T]** ke atas tombol **Mancing / Strike / Tarik** di pojok kanan bawah.
4. Geser **Garis Sensor Bar [B]** tepat di tengah jalur **Bar Vertikal** tempat ikan hitam bergerak naik-turun.
5. Tekan tombol **MULAI (START)** pada menu pop-up.
