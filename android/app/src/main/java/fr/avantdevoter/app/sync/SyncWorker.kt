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
import java.util.concurrent.TimeUnit

/**
 * Vérification des données toutes les 6 heures, appli fermée comprise. Le robot est programmé à 6 h, mais GitHub
 * le lance souvent vers midi : une vérification par jour à heure fixe faisait attendre le lendemain.
 * Elle ne lit que manifest.json (moins d'1 Ko) tant que rien n'a changé.
 * Sans réseau à ce moment-là, Android la relance dès que le réseau revient.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as AvantDeVoterApp).repository
        val r = repository.mettreAJour()
        AvantDeVoterWidget.rafraichir(applicationContext) // compte à rebours du jour, même sans nouvelles données
        return when (r) {
            is Resultat.Echec -> if (runAttemptCount < 5) Result.retry() else Result.success()
            else -> Result.success()
        }
    }

    companion object {
        private const val NOM = "mise-a-jour-quotidienne"
        const val INTERVALLE_HEURES = 6L

        /** À l'ouverture de l'appli : vérifier si la dernière vérification date de plus de 6 heures (ou n'a jamais eu lieu). */
        fun verificationDue(derniere: Long?, maintenant: Long): Boolean =
            derniere == null || maintenant - derniere > INTERVALLE_HEURES * 3600_000L

        fun planifier(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(INTERVALLE_HEURES, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            // UPDATE : remplace aussi la vérification quotidienne de 7 h planifiée par les versions précédentes.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOM, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
