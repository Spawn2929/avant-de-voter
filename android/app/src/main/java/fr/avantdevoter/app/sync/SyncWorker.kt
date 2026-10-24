package fr.avantdevoter.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.avantdevoter.app.AvantDeVoterApp
import fr.avantdevoter.app.data.Resultat
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Mise à jour quotidienne des données, appli fermée comprise : chaque jour après 7 h (le robot publie à 6 h).
 * Sans réseau à ce moment-là, Android la relance dès que le réseau revient.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as AvantDeVoterApp).repository
        return when (repository.mettreAJour()) {
            is Resultat.Echec -> if (runAttemptCount < 5) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    companion object {
        private const val NOM = "mise-a-jour-quotidienne"
        private val PARIS = ZoneId.of("Europe/Paris")

        /** Délai jusqu'au prochain 7 h 15 à Paris. */
        fun delaiJusquA7h(maintenant: ZonedDateTime = ZonedDateTime.now(PARIS)): Duration {
            var cible = maintenant.withZoneSameInstant(PARIS).withHour(7).withMinute(15).withSecond(0).withNano(0)
            if (!cible.isAfter(maintenant)) cible = cible.plusDays(1)
            return Duration.between(maintenant, cible)
        }

        fun planifier(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delaiJusquA7h())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOM, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
