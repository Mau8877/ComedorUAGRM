package com.comedoruagrm.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crearCanalDeNotificaciones()
    }

    // CU-EXTRA-01: el backend manda los push al canal "notificaciones". Si el canal no existe,
    // Android usa uno generico de importancia baja y el aviso no suena ni aparece como banner.
    private fun crearCanalDeNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_NOTIFICACIONES,
            "Notificaciones",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Avisos del comedor universitario" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    companion object {
        // Debe coincidir con app.notificaciones.android-channel-id del backend y con el
        // meta-data de AndroidManifest.xml.
        const val CANAL_NOTIFICACIONES = "notificaciones"
    }
}
