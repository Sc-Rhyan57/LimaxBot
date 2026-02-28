package com.limaxbot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter

class LimaxApplication : Application() {

    companion object {
        const val PREF_CRASH = "limax_crash"
        const val KEY_CRASH = "trace"
        const val CH_SERVICE = "limax_service"
        const val CH_MEDIA = "limax_media"
        const val CH_ALERT = "limax_alert"
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        setupCrash()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            listOf(
                NotificationChannel(CH_SERVICE, "Serviço", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
                NotificationChannel(CH_MEDIA, "Mídias", NotificationManager.IMPORTANCE_DEFAULT).apply { enableVibration(true) },
                NotificationChannel(CH_ALERT, "Alertas", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
            ).forEach { nm.createNotificationChannel(it) }
        }
    }

    private fun setupCrash() {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
                val log = "LimaxBot 2.0 Crash\nDispositivo: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\n\n$sw"
                getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE).edit().putString(KEY_CRASH, log).commit()
                startActivity(android.content.Intent(this, MainActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            } catch (_: Exception) { def?.uncaughtException(t, e) }
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
