package fr.avantdevoter.app

import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.PositionCle
import fr.avantdevoter.app.data.QuestionCle
import fr.avantdevoter.app.data.Reponse
import fr.avantdevoter.app.data.accord
import fr.avantdevoter.app.data.choisirQuestions
import fr.avantdevoter.app.data.libelleZone
import fr.avantdevoter.app.data.positionUtilisateur
import fr.avantdevoter.app.data.proximites
import fr.avantdevoter.app.data.valeurPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoussoleTest {

    private fun c(id: String, bloc: String) = Candidat(id = id, tri = id, nom = id, statut = "declare", bloc = bloc)
    private fun pos(vararg p: Pair<String, String>) = p.associate { (q, v) -> q to PositionCle(v) }

    @Test
    fun echelle_et_accord() {
        assertEquals(1.0, valeurPosition("pour"))
        assertEquals(-0.5, valeurPosition("plutot_contre"))
        assertNull(valeurPosition("non_connu"))
        assertEquals(1.0, accord(1.0, 1.0), 0.0) // oui / pour
        assertEquals(0.0, accord(1.0, -1.0), 0.0) // oui / contre
        assertEquals(0.5, accord(-1.0, 0.0), 0.0) // non / nuancé
        assertEquals(0.75, accord(1.0, 0.5), 0.0) // oui / plutôt pour
    }

    @Test
    fun questions_qui_departagent_les_plus_couvertes_deux_par_domaine() {
        val qs = listOf(
            QuestionCle("q1", "eco", "A"), QuestionCle("q2", "eco", "B"), QuestionCle("q3", "eco", "C"),
            QuestionCle("q4", "sante", "D"), QuestionCle("q5", "sante", "E"),
        )
        val p = mapOf(
            "a" to pos("q1" to "pour", "q2" to "pour", "q3" to "pour", "q4" to "pour", "q5" to "pour"),
            "b" to pos("q1" to "contre", "q2" to "contre", "q3" to "contre", "q4" to "plutot_pour", "q5" to "contre"),
            "c" to pos("q1" to "contre", "q2" to "contre"),
        )
        // q4 ne départage pas (tous pour) ; q3 est le 3e de « eco » ; q1 et q2 (3 positions) passent avant q5 (2).
        assertEquals(listOf("q1", "q2", "q5"), choisirQuestions(qs, p, setOf("a", "b", "c")).map { it.id })
    }

    @Test
    fun proximites_classees_et_minimum_de_questions_communes() {
        val cands = listOf(c("a", "gauche"), c("b", "droite"), c("d", "centre"))
        val p = mapOf(
            "a" to pos("q1" to "pour", "q2" to "pour", "q3" to "contre"),
            "b" to pos("q1" to "contre", "q2" to "contre", "q3" to "pour"),
            "d" to pos("q1" to "pour", "q2" to "contre"), // 2 positions seulement : non comparable
        )
        val r = mapOf("q1" to Reponse.OUI, "q2" to Reponse.OUI, "q3" to Reponse.NON, "q4" to Reponse.PASSER)
        val l = proximites(cands, p, r, minCommunes = 3)
        assertEquals(listOf("a" to 100, "b" to 0), l.map { it.candidat.id to it.pourcent })
        assertEquals(3, l.first().questionsCommunes)
        // Une question passée ne compte pas.
        assertTrue(proximites(cands, p, r.mapValues { Reponse.PASSER }, minCommunes = 3).isEmpty())
        // Avec le seuil réel (5 questions), 3 positions ne suffisent pas.
        assertTrue(proximites(cands, p, r).isEmpty())
    }

    @Test
    fun place_sur_l_axe() {
        val cands = listOf(c("g", "gauche"), c("e", "ecolo"), c("s", "souverainiste"), c("x", "extdroite"))
        val p = mapOf(
            "g" to pos("q1" to "pour", "q2" to "pour", "q3" to "pour"),
            "e" to pos("q1" to "pour", "q2" to "pour", "q3" to "contre"),
            "s" to pos("q1" to "pour", "q2" to "pour", "q3" to "pour"),
            "x" to pos("q1" to "contre", "q2" to "contre", "q3" to "contre"),
        )
        val l = proximites(cands, p, mapOf("q1" to Reponse.OUI, "q2" to Reponse.OUI, "q3" to Reponse.OUI), minCommunes = 3)
        // Placés les plus proches : g (100 %, -2), e (67 %, -1), x (0 %, +2) ; « s » n'est pas placé.
        val v = positionUtilisateur(l)!!
        assertEquals((100 * -2.0 + 67 * -1.0 + 0 * 2.0) / 167, v, 0.001)
        assertEquals("à gauche", libelleZone(v))
        assertNull(positionUtilisateur(l.filter { it.candidat.id == "s" }))
    }
}
