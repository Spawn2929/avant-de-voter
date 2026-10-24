package fr.avantdevoter.app.data

import java.text.Normalizer
import java.time.LocalDate

/** Tout ce que l'appli affiche, lu depuis la base locale. */
data class Catalogue(
    val source: SourceDonnees?,
    val themes: List<Theme>,
    val candidats: List<Candidat>, // ordre alphabétique par nom de famille
    val sondages: List<Sondage>, // du plus récent au plus ancien
    val nomsHorsListe: Map<String, String>,
    val moyenne: Moyenne?,
    val calendrier: List<Etape>,
    val changements: List<Changement>, // du plus récent au plus ancien
    val scrutins: List<Scrutin> = emptyList(), // du plus récent au plus ancien
    val mandats: Map<String, List<Mandat>> = emptyMap(),
    val questions: List<QuestionCle> = emptyList(),
    val positions: Map<String, Map<String, PositionCle>> = emptyMap(),
) {
    val parId: Map<String, Candidat> = candidats.associateBy { it.id }

    val estVide get() = candidats.isEmpty()

    fun nomDe(id: String): String = parId[id]?.nom ?: nomsHorsListe[id] ?: id

    companion object {
        val VIDE = Catalogue(null, emptyList(), emptyList(), emptyList(), emptyMap(), null, emptyList(), emptyList())
    }
}

// ---------------------------------------------------------------- règles d'affichage (fonctions pures, testées)

/** Statuts affichés selon les filtres. Ceux qui ont renoncé ne sont jamais listés (ils restent dans les sondages). */
fun filtrer(candidats: List<Candidat>, declares: Boolean, pressentis: Boolean): List<Candidat> =
    candidats.filter { (declares && it.statut == "declare") || (pressentis && it.statut == "pressenti") }

/** Mêmes filtres, plus « Suivis » : seulement les candidats étoilés (déclarés ou pressentis). */
fun filtrer(candidats: List<Candidat>, r: Reglages): List<Candidat> =
    if (r.suivisSeulement) candidats.filter { it.id in r.suivis && it.statut != "renonce" }
    else filtrer(candidats, r.declares, r.pressentis)

fun mesuresDu(candidat: Candidat, theme: String): List<Mesure> = candidat.mesures.filter { it.theme == theme }

fun etatProgramme(candidat: Candidat): String = candidat.programme?.etat ?: "aucun"

fun libelleEtat(etat: String): String = when (etat) {
    "complet" -> "complet"
    "partiel" -> "partiel"
    else -> "aucun"
}

fun libelleNature(nature: String): String = when (nature) {
    "programme" -> "Programme"
    "declaration" -> "Déclaration"
    "presse" -> "Presse"
    else -> nature.replaceFirstChar { it.uppercase() }
}

fun libelleStatut(statut: String): String = when (statut) {
    "declare" -> "Candidature déclarée"
    "pressenti" -> "Candidature pressentie"
    "renonce" -> "A renoncé"
    else -> statut
}

fun libelleEtatAffaire(etat: String): String = when (etat) {
    "relaxe" -> "Relaxe"
    "condamnation_definitive" -> "Condamnation définitive"
    "condamnation_non_definitive" -> "Condamnation non définitive"
    "classement" -> "Classement sans suite"
    "enquete" -> "Enquête en cours"
    "renvoi_proces" -> "Renvoi en procès"
    "non_lieu" -> "Non-lieu"
    "instruction_close" -> "Instruction close"
    "" -> ""
    else -> etat.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Initiales affichées quand la source n'a pas de photo : « Jean-Luc Mélenchon » → « JM ». */
fun initiales(nom: String): String {
    val mots = nom.split(' ').filter { it.isNotBlank() }
    if (mots.isEmpty()) return "?"
    val prenom = mots.first().first()
    // Le nom de famille commence au premier mot en majuscule après le prénom
    // (« Dominique de Villepin » → V, « Marine Le Pen » → L).
    val famille = mots.drop(1).firstOrNull { it.first().isUpperCase() } ?: return prenom.uppercase()
    return "${prenom.uppercaseChar()}${famille.first().uppercaseChar()}"
}

/** Dates de la source : « 2026-10-22 » → « 22/10/2026 », « 2025-01 » → « 01/2025 », « 2025 » → « 2025 ». */
fun dateCourte(iso: String): String {
    val p = iso.trim().split('-')
    return when (p.size) {
        3 -> "${p[2].take(2)}/${p[1]}/${p[0]}"
        2 -> "${p[1]}/${p[0]}"
        else -> iso
    }
}

private val MOIS = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** « 2026-10-22 » → « 22 octobre 2026 ». */
fun dateLongue(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10))
    "${if (d.dayOfMonth == 1) "1er" else d.dayOfMonth} ${MOIS[d.monthValue - 1]} ${d.year}"
}.getOrElse { dateCourte(iso) }

/** Ligne de crédit en pied d'écran, exigée par la Licence Ouverte. */
fun mentionSource(source: SourceDonnees?): String =
    "Source : 2027 et moi (2027etmoi.fr), données mises à jour le " +
        (source?.miseAJour?.let(::dateCourte) ?: "—")

/** Ligne « Source : titre · date » d'une mesure. */
fun ligneSource(ref: Ref?): String {
    if (ref == null) return "Source"
    val morceaux = listOfNotNull(ref.titre.ifBlank { null }, ref.date.ifBlank { null }?.let(::dateCourte))
    return if (morceaux.isEmpty()) "Source" else "Source : " + morceaux.joinToString(" · ")
}

/** « 0.10.0 » est plus récente que « 0.9.2 » (comparaison nombre par nombre). */
fun estPlusRecente(candidate: String, actuelle: String): Boolean {
    val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
    val b = actuelle.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** Comparaison sans majuscules ni accents (recherche, tri). */
fun normaliser(texte: String): String =
    Normalizer.normalize(texte, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
