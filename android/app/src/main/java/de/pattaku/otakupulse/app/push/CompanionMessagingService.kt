package de.pattaku.otakupulse.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.pattaku.otakupulse.app.CompanionApplication
import de.pattaku.otakupulse.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Empfängt Super-Swipes aus der Party. */
class CompanionMessagingService : FirebaseMessagingService() {

    /**
     * Das Token wechselt gelegentlich von selbst (Neuinstallation, Datenlöschung,
     * Wiederherstellung). Wird es dann nicht gemeldet, verstummen die
     * Benachrichtigungen stillschweigend — der Fehler fällt erst Wochen später auf.
     */
    override fun onNewToken(token: String) {
        val container = (application as CompanionApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { container.deckRepository.meldeFcmToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val titel = message.data["title"] ?: message.notification?.title ?: "Super-Swipe"
        val text = message.data["body"] ?: message.notification?.body ?: return
        zeige(titel, text)
    }

    private fun zeige(titel: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(KANAL, "Party", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Super-Swipes aus deinen Partys."
                },
            )
        }

        val notification = NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titel)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(text.hashCode(), notification)
    }

    companion object {
        private const val KANAL = "party"
    }
}
