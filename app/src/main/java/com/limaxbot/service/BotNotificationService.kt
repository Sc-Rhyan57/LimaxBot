package com.limaxbot.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.limaxbot.LimaxApplication
import com.limaxbot.MainActivity
import com.limaxbot.R

class BotNotificationService : Service() {

    companion object {
        const val NOTIF_ID = 1001

        fun notifyConnected(ctx: Context) = notify(ctx, 2001, LimaxApplication.CH_ALERT,
            "🟢 Bot conectado e pronto para uso!", "LimaxBot está online e monitorando mensagens")

        fun notifyDisconnected(ctx: Context, reason: String) = notify(ctx, 2002, LimaxApplication.CH_ALERT,
            "🔴 Bot perdeu a conexão com o servidor", reason)

        fun notifyMediaDownloading(ctx: Context, type: String) = notify(ctx, 2003, LimaxApplication.CH_MEDIA,
            "📥 Baixando arquivo de mídia...", "Tipo: $type — salvo automaticamente")

        private fun notify(ctx: Context, id: Int, channel: String, title: String, text: String) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val n = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            nm.notify(id, n)
        }

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, BotNotificationService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, BotNotificationService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildPersistentNotif())
        return START_STICKY
    }

    private fun buildPersistentNotif(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, LimaxApplication.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("LimaxBot Ativo")
            .setContentText("Bot conectado e monitorando mensagens")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
