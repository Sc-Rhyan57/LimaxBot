package com.janeasystems.rn_nodejs_mobile

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Serviço declarado no AndroidManifest.xml do LimaxBot
// Mantido vazio — o Node.js roda em thread dentro de RNNodeJsMobile
class RNNodeJsMobileService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
