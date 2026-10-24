package fr.avantdevoter.app.sync

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import fr.avantdevoter.app.AvantDeVoterApp
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.compteARebours
import fr.avantdevoter.app.data.dateCourte
import fr.avantdevoter.app.data.pourcent
import fr.avantdevoter.app.data.premierTour
import fr.avantdevoter.app.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/** Lignes affichées par le widget (fonction pure, testée). */
data class ContenuWidget(val rebours: String, val moyenne: String, val pied: String)

fun contenuWidget(cat: Catalogue, aujourdHui: LocalDate): ContenuWidget {
    val rebours = compteARebours(premierTour(cat.calendrier), aujourdHui) ?: "Présidentielle 2027"
    val m = cat.moyenne
    val moyenne = if (m == null || m.scores.isEmpty()) {
        "Pas de sondage ces 30 derniers jours"
    } else {
        "Moyenne 30 j : " + m.scores.take(3).joinToString(" · ") { "${cat.nomDe(it.id).substringAfter(' ')} ${pourcent(it.score)}" }
    }
    return ContenuWidget(rebours, moyenne, "Données du ${cat.source?.miseAJour?.let(::dateCourte) ?: "—"} · 2027 et moi")
}

/** Widget d'écran d'accueil : compte à rebours et dernière moyenne des sondages (3 premiers, triés par score). */
class AvantDeVoterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cat = (context.applicationContext as AvantDeVoterApp).repository.catalogue.first()
        val c = contenuWidget(cat, LocalDate.now(ZoneId.of("Europe/Paris")))
        provideContent {
            val blanc = ColorProvider(Color.White)
            Column(
                GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF3F5A73))
                    .cornerRadius(16.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(c.rebours, style = TextStyle(color = blanc, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Text(c.moyenne, style = TextStyle(color = blanc, fontSize = 14.sp), maxLines = 3)
                Spacer(GlanceModifier.height(4.dp))
                Text(c.pied, style = TextStyle(color = ColorProvider(Color(0xFFD3E3F3)), fontSize = 11.sp))
            }
        }
    }

    companion object {
        suspend fun rafraichir(context: Context) = runCatching { AvantDeVoterWidget().updateAll(context) }
    }
}

class AvantDeVoterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AvantDeVoterWidget()
}
