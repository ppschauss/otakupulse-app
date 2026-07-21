package de.pattaku.otakupulse.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.pattaku.otakupulse.app.CompanionApplication

/**
 * Schiebt gepufferte Swipes zum Server.
 *
 * Läuft nur mit Netz und wird nach jedem Swipe angestoßen. Fehlschläge enden in
 * [Result.retry] — WorkManager wiederholt dann mit wachsendem Abstand, und der
 * Puffer bleibt unangetastet, bis die Übertragung wirklich geklappt hat.
 */
class SwipeSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CompanionApplication).container
        return try {
            container.watchlistRepository.syncPending()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NAME = "swipe-sync"

        /** Anstoßen nach einem Swipe. Mehrfaches Anstoßen ersetzt den wartenden Lauf. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SwipeSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
