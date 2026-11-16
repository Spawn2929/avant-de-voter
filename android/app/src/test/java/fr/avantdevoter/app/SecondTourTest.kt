package fr.avantdevoter.app

import fr.avantdevoter.app.data.AppData
import fr.avantdevoter.app.data.Hypothese
import fr.avantdevoter.app.data.Moyenne
import fr.avantdevoter.app.data.ReferenceMoyenne
import fr.avantdevoter.app.data.Tendance
import fr.avantdevoter.app.data.libelleTendance
import fr.avantdevoter.app.data.tendance
import fr.avantdevoter.app.data.ORIGINE_2027
import fr.avantdevoter.app.data.ORIGINE_WIKIPEDIA
import fr.avantdevoter.app.data.Sondage
import fr.avantdevoter.app.data.duels
import fr.avantdevoter.app.data.json
import fr.avantdevoter.app.data.titreSondage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecondTourTest {

    private fun s(id: String, institut: String, fin: String, vararg duels: Pair<String, Map<String, Double>>) =
        Sondage(id = id, institut = institut, terrainFin = fin, origine = ORIGINE_WIKIPEDIA, hypotheses = duels.map { Hypothese(it.first, it.second) })

    @Test
    fun duels_regroupes_le_plus_recent_d_abord() {
        val sondages = listOf(
            s("e", "Elabe", "2026-11-13", "Philippe – Le Pen" to mapOf("philippe" to 45.0, "le-pen" to 55.0)),
            s("h", "Harris", "2026-11-05", "Philippe – Le Pen" to mapOf("philippe" to 42.0, "le-pen" to 58.0),
                "Mélenchon – Le Pen" to mapOf("melenchon" to 32.0, "le-pen" to 68.0)),
            s("i", "Ifop", "2026-10-19", "Attal – Le Pen" to mapOf("attal" to 42.0, "le-pen" to 58.0)),
        )
        val d = duels(sondages)
        assertEquals(listOf("Philippe – Le Pen", "Mélenchon – Le Pen", "Attal – Le Pen"), d.map { it.label })
        assertEquals(listOf("e", "h"), d.first().resultats.map { it.first.id })
        // Ordre des duellistes conservé tel que publié (« Philippe – Le Pen » : Philippe d'abord).
        assertEquals(listOf("philippe", "le-pen"), d.first().resultats.first().second.scores.keys.toList())
    }

    @Test
    fun titre_sans_commanditaire() {
        assertEquals("Elabe", titreSondage(Sondage(id = "a", institut = "Elabe", terrainFin = "2026-11-13")))
        assertEquals("Ifop · LCI", titreSondage(Sondage(id = "b", institut = "Ifop", commanditaire = "LCI", terrainFin = "2026-10-29")))
    }

    @Test
    fun anciennes_donnees_sans_second_tour_ni_origine() {
        val app = json.decodeFromString(
            AppData.serializer(),
            """{"format":1,"source":{"nom":"2027 et moi","site":"s","depot":"d","licence":"l","mise_a_jour":"2026-11-09"},
               "themes":[],"candidats":[],"sondages":[{"id":"x","institut":"Ifop","terrain_fin":"2026-10-29"}]}""",
        )
        assertEquals(emptyList<Sondage>(), app.secondTour)
        assertEquals(ORIGINE_2027, app.sondages.first().origine)
    }

    @Test
    fun tendance_en_points_par_rapport_a_la_moyenne_precedente() {
        val sans = Moyenne(30, 12, "r", emptyList())
        assertNull(tendance(sans, "le-pen", 33.6))
        val m = sans.copy(reference = ReferenceMoyenne("2026-11-15", mapOf("le-pen" to 32.8, "melenchon" to 17.7, "philippe" to 13.2)))
        assertEquals(Tendance.Ecart(0.8), tendance(m, "le-pen", 33.6))
        assertEquals("▲ +0,8 pt", libelleTendance(tendance(m, "le-pen", 33.6)!!))
        assertEquals("▼ −0,4 pt", libelleTendance(tendance(m, "melenchon", 17.3)!!))
        assertEquals("=", libelleTendance(tendance(m, "philippe", 13.2)!!))
        assertEquals("▲ +2 pt", libelleTendance(tendance(m, "philippe", 15.2)!!))
        assertEquals(Tendance.Nouveau, tendance(m, "villepin", 2.5))
    }
}
