package fr.avantdevoter.app

import fr.avantdevoter.app.data.AppData
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Mesure
import fr.avantdevoter.app.data.Ref
import fr.avantdevoter.app.data.SourceDonnees
import fr.avantdevoter.app.data.dateCourte
import fr.avantdevoter.app.data.dateLongue
import fr.avantdevoter.app.data.estPlusRecente
import fr.avantdevoter.app.data.filtrer
import fr.avantdevoter.app.data.initiales
import fr.avantdevoter.app.data.json
import fr.avantdevoter.app.data.libelleNature
import fr.avantdevoter.app.data.ligneSource
import fr.avantdevoter.app.data.mentionSource
import fr.avantdevoter.app.data.mesuresDu
import fr.avantdevoter.app.sync.SyncWorker
import fr.avantdevoter.app.ui.quand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CatalogueTest {

    private fun c(id: String, statut: String, vararg mesures: Mesure) =
        Candidat(id = id, tri = id, nom = id, statut = statut, mesures = mesures.toList())

    @Test
    fun filtres_de_statut() {
        val tous = listOf(c("a", "declare"), c("b", "pressenti"), c("c", "renonce"))
        assertEquals(listOf("a"), filtrer(tous, declares = true, pressentis = false).map { it.id })
        assertEquals(listOf("a", "b"), filtrer(tous, declares = true, pressentis = true).map { it.id })
        assertEquals(listOf("b"), filtrer(tous, declares = false, pressentis = true).map { it.id })
        // Ceux qui ont renoncé ne sont jamais listés.
        assertTrue(filtrer(tous, true, true).none { it.statut == "renonce" })
    }

    @Test
    fun mesures_par_domaine_dans_l_ordre_de_la_source() {
        val m1 = Mesure("1", "economie", "A", "programme")
        val m2 = Mesure("2", "sante", "B", "presse")
        val m3 = Mesure("3", "economie", "C", "declaration")
        assertEquals(listOf("A", "C"), mesuresDu(c("x", "declare", m1, m2, m3), "economie").map { it.texte })
        assertTrue(mesuresDu(c("x", "declare", m1), "societe").isEmpty())
    }

    @Test
    fun initiales_quand_pas_de_photo() {
        assertEquals("JM", initiales("Jean-Luc Mélenchon"))
        assertEquals("ML", initiales("Marine Le Pen"))
        assertEquals("DV", initiales("Dominique de Villepin"))
        assertEquals("ND", initiales("Nicolas Dupont-Aignan"))
        assertEquals("É", initiales("Édouard"))
    }

    @Test
    fun dates() {
        assertEquals("22/10/2026", dateCourte("2026-10-22"))
        assertEquals("01/2025", dateCourte("2025-01"))
        assertEquals("2025", dateCourte("2025"))
        assertEquals("1er avril 2027", dateLongue("2027-04-01"))
        assertEquals("22 octobre 2026", dateLongue("2026-10-22"))
    }

    @Test
    fun mention_et_ligne_de_source() {
        val s = SourceDonnees("2027 et moi", "", "", "", "2026-10-22")
        assertEquals("Source : 2027 et moi (2027etmoi.fr), données mises à jour le 22/10/2026", mentionSource(s))
        assertEquals("Source : L'Avenir en commun · 01/2025", ligneSource(Ref("L'Avenir en commun", "https://a", "2025-01")))
        assertEquals("Source", ligneSource(Ref(url = "https://a")))
    }

    @Test
    fun natures() {
        assertEquals("Déclaration", libelleNature("declaration"))
        assertEquals("Presse", libelleNature("presse"))
    }

    @Test
    fun versions() {
        assertTrue(estPlusRecente("0.10.0", "0.9.2"))
        assertTrue(estPlusRecente("0.2.0", "0.1.0"))
        assertFalse(estPlusRecente("0.1.0", "0.1.0"))
        assertFalse(estPlusRecente("0.1.0", "0.2.0"))
    }

    @Test
    fun synchro_apres_7_h_a_paris() {
        val paris = ZoneId.of("Europe/Paris")
        assertEquals(Duration.ofMinutes(75), SyncWorker.delaiJusquA7h(ZonedDateTime.of(2026, 10, 24, 6, 0, 0, 0, paris)))
        assertEquals(Duration.ofHours(23), SyncWorker.delaiJusquA7h(ZonedDateTime.of(2026, 10, 20, 8, 15, 0, 0, paris)))
        // Passage à l'heure d'hiver dans la nuit du 24 au 25 octobre 2026 : une heure de plus.
        assertEquals(Duration.ofHours(24), SyncWorker.delaiJusquA7h(ZonedDateTime.of(2026, 10, 24, 8, 15, 0, 0, paris)))
    }

    @Test
    fun heure_de_la_derniere_verification() {
        val paris = ZoneId.of("Europe/Paris")
        val jour = LocalDate.of(2026, 10, 24)
        val ms = ZonedDateTime.of(2026, 10, 24, 8, 5, 0, 0, paris).toInstant().toEpochMilli()
        assertEquals("aujourd'hui à 8 h 05", quand(ms, jour))
        assertEquals("hier à 8 h 05", quand(ms, jour.plusDays(1)))
        assertEquals("le 24/10/2026 à 8 h 05", quand(ms, jour.plusDays(3)))
    }

    /** Le vrai fichier publié par le robot se lit sans erreur (si la copie intégrée est présente). */
    @Test
    fun lecture_du_fichier_publie() {
        val f = File("src/main/assets/snapshot/app-data.json")
        assumeTrue(f.exists())
        val app = json.decodeFromString(AppData.serializer(), f.readText())
        assertEquals(13, app.themes.size)
        assertTrue(app.candidats.count { it.statut == "declare" } >= 10)
        assertTrue(app.candidats.flatMap { it.mesures }.all { it.source?.url?.startsWith("http") == true })
        assertEquals(app.candidats.map { it.tri }.sorted(), app.candidats.map { it.tri })
    }
}
