package com.example.autofishing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_MEDIA_PROJECTION = 1001
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val infoText = TextView(this).apply {
            text = "Panduan Cepat Auto-Fishing:
" +
                   "1. Aktifkan Izin Overlay Pop-up.
" +
                   "2. Aktifkan Accessibility Service (Auto Fishing Clicker).
" +
                   "3. Tekan 'Mulai Pop-Up & Deteksi Layar' lalu buka game."
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }

        val btnOverlay = Button(this).apply {
            text = "1. Izinkan Menu Pop-Up (Overlay)"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "Izin Pop-Up sudah aktif!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnAccessibility = Button(this).apply {
            text = "2. Aktifkan Layanan Klik (Aksesibilitas)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val btnStartCapture = Button(this).apply {
            text = "3. Munculkan Pop-Up & Deteksi Layar"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(context, "Aktifkan Izin Overlay terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (FishingAccessibilityService.instance == null) {
                    Toast.makeText(context, "Aktifkan Layanan Aksesibilitas terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
            }
        }

        layout.addView(infoText)
        layout.addView(btnOverlay)
        layout.addView(btnAccessibility)
        layout.addView(btnStartCapture)
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            FishingAccessibilityService.instance?.startMediaProjection(resultCode, data)
            Toast.makeText(this, "Menu Pop-Up Aktif! Silakan buka game.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        }
    }
}
