package de.pattaku.otakupulse.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.pattaku.otakupulse.app.CompanionApplication
import de.pattaku.otakupulse.app.R
import java.util.concurrent.TimeUnit

/**
 * Prüft regelmäßig, ob es neue Folgen der gemerkten Titel gibt.
 *
 * Das Ergebnis speist zwei Anzeigen aus einer Quelle: die Benachrichtigung und das
 * „neu"-Abzeichen in der Watchlist. Getrennte Wege würden über kurz oder lang
 * auseinanderlaufen.
 */
class EpisodeCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CompanionApplication).container
        return try {
            val neue = container.airingRepository.pruefeNeueFolgen()
            if (neue.isNotEmpty()) benachrichtige(neue.size, neue.first().title)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun benachrichtige(anzahl: Int, ersterTitel: String) {
        // Ohne die Berechtigung (Android 13+) still aufgeben — das Abzeichen in der
        // Watchlist ist trotzdem gesetzt, die Information geht also nicht verloren.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        erzeugeKanal()

        val text = if (anzahl == 1) {
            "Neue Folge: $ersterTitel"
        } else {
            "$anzahl neue Folgen, u. a. $ersterTitel"
        }

        val notification = NotificationCompat.Builder(applicationContext, KANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Es gibt Nachschub")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(ID, notification)
    }

    private fun erzeugeKanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val kanal = NotificationChannel(
            KANAL,
            "Neue Folgen",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Meldet neue Folgen deiner gemerkten Anime." }
        applicationContext.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(kanal)
    }

    companion object {
        private const val KANAL = "neue-folgen"
        private const val ID = 1001
        private const val NAME = "episode-check"

        /** Alle vier Stunden, nur mit Netz. Häufiger lohnt nicht — Folgen kommen wöchentlich. */
        fun planen(context: Context) {
            val request = PeriodicWorkRequestBuilder<EpisodeCheckWorker>(4, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
