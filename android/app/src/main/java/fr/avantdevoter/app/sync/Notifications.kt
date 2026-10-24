package fr.avantdevoter.app.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.avantdevoter.app.R
import fr.avantdevoter.app.data.AppData
import fr.avantdevoter.app.data.Changement
import fr.avantdevoter.app.ui.MainActivity

/**
 * Notification Android quand un candidat suivi (étoile) change : nouvelle mesure, mesure retirée,
 * programme complété, changement de statut. Générée par l'appli elle-même, sans serveur.
 */
object Notifications {
    private const val CANAL = "candidats-suivis"
    const val EXTRA_ONGLET = "onglet"
    const val ONGLET_NOUVEAUTES = "nouveautes"

    fun autorisees(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun creerCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Candidats suivis", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Nouvelles mesures et changements des candidats marqués d'une étoile"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Texte d'une ligne : « Raphaël Glucksmann — Mesure ajoutée — Économie ». */
    fun ligne(c: Changement, app: AppData): String {
        val nom = app.candidats.firstOrNull { it.id == c.candidat }?.nom ?: c.candidat.orEmpty()
        return "$nom — ${c.libelle}"
    }

    fun signaler(context: Context, changements: List<Changement>, app: AppData) {
        if (changements.isEmpty() || !autorisees(context)) return
        creerCanal(context)
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ONGLET, ONGLET_NOUVEAUTES)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val ouvrir = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val lignes = changements.map { ligne(it, app) }
        val titre = if (changements.size == 1) "Nouveauté pour un candidat suivi" else "${changements.size} nouveautés pour vos candidats suivis"
        val style = NotificationCompat.InboxStyle().also { s -> lignes.take(6).forEach { s.addLine(it) } }
        val notif = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titre)
            .setContentText(lignes.first())
            .setStyle(style)
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, notif)
        } catch (_: SecurityException) {
            // autorisation retirée entre-temps : rien à faire
        }
    }
}
