package com.example.autofishing

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FishingAccessibilityService : AccessibilityService() {

    companion object {
        var instance: FishingAccessibilityService? = null
    }

    private lateinit var windowManager: WindowManager
    private var floatingMenu: View? = null
    private var statusText: TextView? = null

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var screenWidth = 2400
    private var screenHeight = 1080
    private var screenDensity = 400

    // Koordinat proporsional sesuai layout video (Landscape)
    // Tombol kanan bawah (Mancing / Angkat / Strike! / Tarik)
    private var btnXRatio = 0.91f
    private var btnYRatio = 0.83f

    // Area Bar Vertikal Mini-game (sebelah kanan karakter)
    private var barXRatio = 0.614f
    private var barTopYRatio = 0.36f
    private var barBottomYRatio = 0.78f

    private var lastActionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateMetrics()
        showFloatingMenu()
    }

    private fun updateMetrics() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    fun startMediaProjection(resultCode: Int, data: Intent) {
        startForegroundNotification()
        updateMetrics()
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "FishingScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun startForegroundNotification() {
        val channelId = "fishing_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Auto Fishing Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Auto Fishing Clicker Aktif")
                .setContentText("Mendeteksi indikator pancing di layar...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("Auto Fishing Clicker").build()
        }
        startForeground(1, notification)
    }

    private fun showFloatingMenu() {
        if (floatingMenu != null) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC1E293B"))
            setPadding(24, 16, 24, 16)
        }

        val title = TextView(this).apply {
            text = "🎣 Auto-Fishing"
            setTextColor(Color.WHITE)
            textSize = 13f
        }

        statusText = TextView(this).apply {
            text = "Status: IDLE"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 11f
        }

        val btnToggle = Button(this).apply {
            text = "MULAI"
            textSize = 12f
            setOnClickListener {
                isRunning = !isRunning
                if (isRunning) {
                    text = "STOP"
                    setBackgroundColor(Color.parseColor("#EF4444"))
                    handler.post(fishingLoop)
                } else {
                    text = "MULAI"
                    setBackgroundColor(Color.parseColor("#22C55E"))
                    statusText?.text = "Status: BERHENTI"
                }
            }
        }

        container.addView(title)
        container.addView(statusText)
        container.addView(btnToggle)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 120
        }

        // Agar pop-up bisa digeser (drag) di layar HP
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(container, params)
                        return true
                    }
                }
                return false
            }
        })

        floatingMenu = container
        windowManager.addView(floatingMenu, params)
    }

    private val fishingLoop = object : Runnable {
        override fun run() {
            if (!isRunning) return
            processScreenFrame()
            handler.postDelayed(this, 60) // Cek layar setiap 60ms (~16 FPS)
        }
    }

    private fun processScreenFrame() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return

        try {
            val width = image.width
            val height = image.height
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride

            fun getRgb(x: Int, y: Int): Triple<Int, Int, Int> {
                val safeX = x.coerceIn(0, width - 1)
                val safeY = y.coerceIn(0, height - 1)
                val offset = safeY * rowStride + safeX * pixelStride
                val r = buffer.get(offset).toInt() and 0xFF
                val g = buffer.get(offset + 1).toInt() and 0xFF
                val b = buffer.get(offset + 2).toInt() and 0xFF
                return Triple(r, g, b)
            }

            val btnX = (width * btnXRatio).toInt()
            val btnY = (height * btnYRatio).toInt()
            val (rBtn, gBtn, bBtn) = getRgb(btnX, btnY)

            val now = System.currentTimeMillis()

            // 1. DETEKSI TOMBOL MERAH-ORANYE "Strike!" (Angkat Otomatis)
            // Pada video detik 00:09.5, tombol berubah menjadi merah-oranye terang (R > 200, G < 120, B < 90)
            if (rBtn > 200 && gBtn in 50..130 && bBtn < 90) {
                if (now - lastActionTime > 250) {
                    statusText?.text = "Status: STRIKE! (Angkat)"
                    performTap(btnX.toFloat(), btnY.toFloat(), 55L)
                    lastActionTime = now
                }
                return
            }

            // 2. DETEKSI TOMBOL HIJAU "Tarik (tahan biar naik)" (Mini-game Tarik Otomatis)
            // Pada video detik 00:10.2 - 00:14.2, tombol kanan bawah berwarna hijau (G > 130, R < 110, B < 90)
            if (gBtn > 130 && rBtn < 115 && bBtn < 95) {
                val barX = (width * barXRatio).toInt()
                val topY = (height * barTopYRatio).toInt()
                val bottomY = (height * barBottomYRatio).toInt()

                var fishYSum = 0
                var fishCount = 0
                var greenBarYSum = 0
                var greenBarCount = 0

                // Pindai jalur vertikal bar dari atas ke bawah
                for (y in topY..bottomY step 3) {
                    val (r, g, b) = getRgb(barX, y)
                    // Siluet ikan berwarna hitam/gelap pekat (R < 45, G < 55, B < 60)
                    if (r < 45 && g < 55 && b < 60) {
                        fishYSum += y
                        fishCount++
                    }
                    // Area Catch Bar berwarna hijau muda terang (R > 120, G > 200, B > 100)
                    else if (g > 200 && r in 110..195 && b in 95..175) {
                        greenBarYSum += y
                        greenBarCount++
                    }
                }

                if (fishCount > 0 && greenBarCount > 0) {
                    val fishY = fishYSum / fishCount
                    val barY = greenBarYSum / greenBarCount
                    val diff = fishY - barY // Negatif berarti ikan di ATAS bar hijau

                    if (diff < -12) {
                        // Ikan berada di atas bar hijau -> Tahan tombol "Tarik" agar bar naik
                        val holdDuration = abs(diff).coerceIn(90, 260).toLong()
                        statusText?.text = "Status: TARIK NAIK (${holdDuration}ms)"
                        performTap(btnX.toFloat(), btnY.toFloat(), holdDuration)
                    } else if (abs(diff) <= 12) {
                        // Posisi sejajar -> Tap halus untuk menahan gravitasi bar
                        statusText?.text = "Status: STABIL"
                        performTap(btnX.toFloat(), btnY.toFloat(), 65L)
                    } else {
                        // Ikan berada di bawah bar hijau -> Lepas sentuhan agar bar turun
                        statusText?.text = "Status: LEPAS TURUN"
                    }
                } else {
                    // Fallback jika bar tertutup efek visual: tap stabil
                    performTap(btnX.toFloat(), btnY.toFloat(), 85L)
                }
                return
            }

            // 3. DETEKSI TOMBOL BIRU "Mancing" (Mancing Otomatis / Lempar Kail)
            // Pada video detik 00:16, tombol kanan bawah berwarna biru (B > 170, G > 100, R < 70)
            if (bBtn > 170 && gBtn > 90 && rBtn < 75) {
                if (now - lastActionTime > 1200) {
                    statusText?.text = "Status: LEMPAR KAIL"
                    performTap(btnX.toFloat(), btnY.toFloat(), 450L)
                    lastActionTime = now
                }
                return
            }

            // 4. DETEKSI POP-UP KARTU HASIL TANGKAPAN (Detik 00:15 - Kartu Krem/Putih di Tengah)
            val centerX = width / 2
            val centerY = height / 2
            val (rMid, gMid, bMid) = getRgb(centerX, centerY)
            if (rMid > 235 && gMid > 230 && bMid > 210 && (now - lastActionTime > 1000)) {
                statusText?.text = "Status: TUTUP HASIL"
                performTap(centerX.toFloat(), (height * 0.85f), 60L)
                lastActionTime = now
                return
            }

            // 5. STANDBY SAAT TOMBOL ABU-ABU "Angkat" (Menunggu Ikan Menyambar)
            statusText?.text = "Status: MENUNGGU STRIKE..."

        } finally {
            image.close()
        }
    }

    private fun performTap(x: Float, y: Float, durationMs: Long) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingMenu?.let { windowManager.removeView(it) }
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        instance = null
    }
}
