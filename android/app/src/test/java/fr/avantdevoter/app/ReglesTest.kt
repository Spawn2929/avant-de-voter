package fr.avantdevoter.app

import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Changement
import fr.avantdevoter.app.data.Etape
import fr.avantdevoter.app.data.Hypothese
import fr.avantdevoter.app.data.Mesure
import fr.avantdevoter.app.data.Moyenne
import fr.avantdevoter.app.data.Ref
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.ScoreMoyen
import fr.avantdevoter.app.data.Scrutin
import fr.avantdevoter.app.data.SourceDonnees
import fr.avantdevoter.app.data.Sondage
import fr.avantdevoter.app.data.aNotifier
import fr.avantdevoter.app.data.classement
import fr.avantdevoter.app.data.compteARebours
import fr.avantdevoter.app.data.courbe
import fr.avantdevoter.app.data.filtrer
import fr.avantdevoter.app.data.nouveauxChangements
import fr.avantdevoter.app.data.personnes
import fr.avantdevoter.app.data.pourcent
import fr.avantdevoter.app.data.premierTour
import fr.avantdevoter.app.data.questionQuiz
import fr.avantdevoter.app.data.rechercher
import fr.avantdevoter.app.data.scoreDans
import fr.avantdevoter.app.data.terrain
import fr.avantdevoter.app.data.texteDePartage
import fr.avantdevoter.app.data.tirage
import fr.avantdevoter.app.data.votesDe
import fr.avantdevoter.app.sync.contenuWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class ReglesTest {

    private fun m(id: String, theme: String, texte: String = "Mesure $id") = Mesure(id, theme, texte, "programme", Ref("T", "https://s/$id", "2026-01"))
    private fun c(id: String, nom: String = id, statut: String = "declare", vararg mesures: Mesure) =
        Candidat(id = id, tri = id, nom = nom, statut = statut, mesures = mesures.toList())

    private val s1 = Sondage("s1", "Ifop", terrainDebut = "2026-10-17", terrainFin = "2026-10-19", echantillon = 1928,
        hypotheses = listOf(Hypothese("H1", mapOf("x" to 30.0, "y" to 10.0)), Hypothese("H2", mapOf("x" to 34.0))))
    private val s2 = Sondage("s2", "Elabe", terrainFin = "2026-10-02", hypotheses = listOf(Hypothese("H", mapOf("x" to 20.0, "z" to 20.0))))

    @Test
    fun sondages() {
        assertEquals(listOf("x" to 30.0, "y" to 10.0), classement(s1.hypotheses[0]))
        assertEquals(listOf("x" to 20.0, "z" to 20.0), classement(s2.hypotheses[0])) // égalité : ordre alphabétique
        assertEquals(32.0, scoreDans(s1, "x")!!, 0.001)
        assertNull(scoreDans(s2, "y"))
        val pts = courbe(listOf(s1, s2), "x")
        assertEquals(listOf("s2", "s1"), pts.map { it.sondage.id }) // du plus ancien au plus récent
        assertEquals(30.0, pts[1].min, 0.0)
        assertEquals(34.0, pts[1].max, 0.0)
        assertEquals("Terrain du 17/10/2026 au 19/10/2026", terrain(s1))
        assertEquals("Terrain le 02/10/2026", terrain(s2))
        assertEquals("1 928 personnes", personnes(1928))
        assertEquals("36,5 %", pourcent(36.5))
        assertEquals("12 %", pourcent(12.0))
        assertEquals("0,5 %", pourcent(0.5))
    }

    @Test
    fun calendrier_et_compte_a_rebours() {
        val etapes = listOf(
            Etape("2027-04-05", titre = "Début de la campagne officielle"),
            Etape("2027-04-18", titre = "Premier tour de l'élection présidentielle"),
            Etape("2027-05-02", titre = "Second tour de l'élection présidentielle"),
            Etape("2026-10-09", titre = "Primaire PS : désigné dès le 1er tour"), // pas la présidentielle
        )
        val tour = premierTour(etapes)
        assertEquals(LocalDate.of(2027, 4, 18), tour)
        assertEquals("1er tour dans 176 jours", compteARebours(tour, LocalDate.of(2026, 10, 24)))
        assertEquals("1er tour demain", compteARebours(tour, LocalDate.of(2027, 4, 17)))
        assertEquals("1er tour aujourd'hui", compteARebours(tour, LocalDate.of(2027, 4, 18)))
        assertNull(compteARebours(tour, LocalDate.of(2027, 4, 19)))
    }

    @Test
    fun tirage_sans_doublon_et_equiprobable() {
        val cands = listOf(c("a", "A", "declare", m("1", "eco"), m("2", "eco")), c("b", "B", "declare", m("3", "eco"), m("4", "sante")))
        val t = tirage(cands, "eco", 8, Random(1))
        assertEquals(setOf("1", "2", "3"), t.map { it.second.id }.toSet())
        assertEquals(3, t.size)
        // Chaque mesure arrive en tête à peu près une fois sur trois.
        val tetes = (0 until 3000).groupingBy { tirage(cands, "eco", 8, Random(it)).first().second.id }.eachCount()
        tetes.values.forEach { assertTrue("répartition $tetes", it in 850..1150) }
    }

    @Test
    fun quiz_quatre_choix_dont_l_auteur_et_jamais_un_auteur_du_meme_texte() {
        val cands = listOf(
            c("a", "A", "declare", m("1", "eco", "Rétablir l'ISF.")),
            c("b", "B", "declare", m("2", "eco", "Retablir l'ISF.")), // même mesure sans accent
            c("c", "C", "declare", m("3", "eco")),
            c("d", "D", "declare", m("4", "eco")),
            c("e", "E", "declare", m("5", "eco")),
            c("f", "F", "declare"), // sans mesure : jamais proposé
        )
        repeat(200) { graine ->
            val q = questionQuiz(cands, Random(graine))
            assertNotNull(q)
            q!!
            assertEquals(4, q.choix.size)
            assertEquals(4, q.choix.map { it.id }.toSet().size)
            assertTrue(q.candidat in q.choix)
            assertTrue(q.choix.none { it.id == "f" })
            if (q.mesure.id == "1") assertTrue(q.choix.none { it.id == "b" })
            if (q.mesure.id == "2") assertTrue(q.choix.none { it.id == "a" })
        }
        assertNull(questionQuiz(cands.take(3), Random(1)))
    }

    @Test
    fun nouveautes_et_notifications() {
        val ancien = Changement("2026-10-20", "nouveau_sondage", sondage = "s1", libelle = "Nouveau sondage Ifop")
        val ajout = Changement("2026-10-25", "mesure_ajoutee", candidat = "a", mesure = "m1", libelle = "Mesure ajoutée — Économie")
        val autre = Changement("2026-10-25", "statut_candidat", candidat = "b", libelle = "Statut : pressenti → déclaré")
        val sondage = Changement("2026-10-25", "nouveau_sondage", sondage = "s2", libelle = "Nouveau sondage Elabe")
        val nouveaux = nouveauxChangements(listOf(ancien), listOf(ajout, autre, sondage, ancien))
        assertEquals(listOf(ajout, autre, sondage), nouveaux)
        assertEquals(listOf(ajout), aNotifier(nouveaux, setOf("a")))
        assertTrue(aNotifier(nouveaux, emptySet()).isEmpty())
    }

    @Test
    fun filtre_suivis() {
        val tous = listOf(c("a"), c("b", statut = "pressenti"), c("r", statut = "renonce"), c("d"))
        val r = Reglages(suivisSeulement = true, suivis = setOf("b", "r", "d"))
        assertEquals(listOf("b", "d"), filtrer(tous, r).map { it.id }) // pas ceux qui ont renoncé
        assertEquals(listOf("a", "d"), filtrer(tous, Reglages()).map { it.id })
    }

    @Test
    fun recherche_sans_accents_ni_majuscules() {
        val cands = listOf(
            c("a", "A", "declare", m("1", "eco", "SMIC porté à 1 600 euros net."), m("2", "eco", "Relance du nucléaire civil.")),
            c("b", "B", "declare", m("3", "ecologie", "Sortie du nucléaire d'ici 2045.")),
        )
        assertEquals(listOf("1"), rechercher(cands, "smic").map { it.mesure.id })
        assertEquals(listOf("2", "3"), rechercher(cands, "NUCLEAIRE").map { it.mesure.id })
        assertEquals(listOf("3"), rechercher(cands, "nucléaire 2045").map { it.mesure.id })
        assertTrue(rechercher(cands, "a").isEmpty())
    }

    @Test
    fun votes_du_plus_recent_au_plus_ancien() {
        val v = listOf(
            Scrutin("1", date = "2025-01-10", votes = mapOf("x" to "pour")),
            Scrutin("2", date = "2026-03-01", votes = mapOf("x" to "absent", "y" to "contre")),
            Scrutin("3", date = "2026-04-01", votes = mapOf("y" to "pour")),
        )
        assertEquals(listOf("2" to "absent", "1" to "pour"), votesDe(v, "x").map { it.first.id to it.second })
    }

    @Test
    fun texte_partage_avec_source() {
        val t = texteDePartage(c("x", "Jean Dupont"), "Économie", m("1", "eco", "Rétablir l'ISF."))
        assertEquals(
            "Jean Dupont — Économie\n« Rétablir l'ISF. »\nSource : T, 01/2026 — https://s/1\nVia « Avant de voter », données 2027 et moi (2027etmoi.fr)",
            t.replace("\r\n", "\n"),
        )
    }

    @Test
    fun widget() {
        val cat = Catalogue(
            SourceDonnees("", "", "", "", "2026-10-22"), emptyList(),
            listOf(c("le-pen", "Marine Le Pen"), c("melenchon", "Jean-Luc Mélenchon")), emptyList(), emptyMap(),
            Moyenne(30, 7, "", listOf(ScoreMoyen("le-pen", 36.4, 7), ScoreMoyen("melenchon", 15.8, 7))),
            listOf(Etape("2027-04-18", titre = "Premier tour de l'élection présidentielle")), emptyList(),
        )
        val w = contenuWidget(cat, LocalDate.of(2026, 10, 24))
        assertEquals("1er tour dans 176 jours", w.rebours)
        assertEquals("Moyenne 30 j : Le Pen 36,4 % · Mélenchon 15,8 %", w.moyenne)
        assertEquals("Données du 22/10/2026 · 2027 et moi", w.pied)
    }
}
