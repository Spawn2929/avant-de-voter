package fr.avantdevoter.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

// Règles des onglets Sondages, Nouveautés, Plus et de la recherche. Fonctions pures, testées.

// ---------------------------------------------------------------- sondages

/** Scores d'une hypothèse, triés du plus haut au plus bas (puis par identifiant, pour un ordre stable). */
fun classement(h: Hypothese): List<Pair<String, Double>> =
    h.scores.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key }).map { it.key to it.value }

/** Score d'un candidat dans un sondage : moyenne des hypothèses où il figure (même règle que la moyenne 30 jours). */
fun scoreDans(s: Sondage, id: String): Double? {
    val v = s.hypotheses.mapNotNull { it.scores[id] }
    return if (v.isEmpty()) null else v.average()
}

/** Un point de la courbe d'un candidat : un sondage, sa moyenne, et l'écart entre ses hypothèses. */
data class PointSondage(val sondage: Sondage, val moyenne: Double, val min: Double, val max: Double)

/** Tous les sondages où le candidat a été testé, du plus ancien au plus récent. */
fun courbe(sondages: List<Sondage>, id: String): List<PointSondage> =
    sondages.mapNotNull { s ->
        val v = s.hypotheses.mapNotNull { it.scores[id] }
        if (v.isEmpty()) null else PointSondage(s, v.average(), v.min(), v.max())
    }.sortedWith(compareBy({ it.sondage.terrainFin }, { it.sondage.institut }))

fun instituts(sondages: List<Sondage>): List<String> = sondages.map { it.institut }.distinct().sorted()

/** « Terrain du 17 au 19/10/2026 ». */
fun terrain(s: Sondage): String =
    if (s.terrainDebut.isBlank() || s.terrainDebut == s.terrainFin) "Terrain le ${dateCourte(s.terrainFin)}"
    else "Terrain du ${dateCourte(s.terrainDebut)} au ${dateCourte(s.terrainFin)}"

/** « 1 928 personnes » avec l'espace des milliers. */
fun personnes(n: Int?): String =
    if (n == null) "échantillon non précisé" else String.format(java.util.Locale.ROOT, "%,d", n).replace(',', ' ') + " personnes"

/** 36.5 → « 36,5 % », 12.0 → « 12 % ». */
fun pourcent(v: Double): String {
    val arrondi = Math.round(v * 10) / 10.0
    val texte = if (arrondi % 1.0 == 0.0) arrondi.toLong().toString() else arrondi.toString().replace('.', ',')
    return "$texte %"
}

// ---------------------------------------------------------------- calendrier

/** Date du premier tour, lue dans le calendrier de la source (« Premier tour de l'élection présidentielle »). */
fun premierTour(etapes: List<Etape>): LocalDate? =
    etapes.firstOrNull { normaliser(it.titre).let { t -> "premier tour" in t || "1er tour" in t } && "presidentielle" in normaliser(it.titre) }
        ?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() }

fun joursAvant(cible: LocalDate, aujourdHui: LocalDate): Long = ChronoUnit.DAYS.between(aujourdHui, cible)

/** « 1er tour dans 176 jours », « 1er tour demain », « 1er tour aujourd'hui », null une fois passé. */
fun compteARebours(cible: LocalDate?, aujourdHui: LocalDate): String? {
    if (cible == null) return null
    return when (val j = joursAvant(cible, aujourdHui)) {
        in Long.MIN_VALUE..-1 -> null
        0L -> "1er tour aujourd'hui"
        1L -> "1er tour demain"
        else -> "1er tour dans $j jours"
    }
}

fun libelleStatutEtape(statut: String): String = when (statut) {
    "officielle" -> "Date officielle"
    "prevue" -> "Date prévue"
    "estimee" -> "Date estimée"
    else -> statut
}

// ---------------------------------------------------------------- Mes priorités, quiz

/** Mesures d'un domaine, tirées au hasard et sans nom : 8 au plus, chaque mesure a la même chance. */
fun tirage(candidats: List<Candidat>, theme: String, nombre: Int = 8, hasard: Random = Random.Default): List<Pair<Candidat, Mesure>> =
    candidats.flatMap { c -> mesuresDu(c, theme).map { c to it } }.shuffled(hasard).take(nombre)

data class QuestionQuiz(val candidat: Candidat, val mesure: Mesure, val choix: List<Candidat>)

/**
 * Une question : une mesure tirée au hasard et 4 candidats, dont son auteur.
 * Les leurres ont tous des mesures et ne portent jamais la même mesure (même texte, sans accents ni majuscules).
 */
fun questionQuiz(candidats: List<Candidat>, hasard: Random = Random.Default, dejaPosees: Set<String> = emptySet()): QuestionQuiz? {
    val avecMesures = candidats.filter { it.mesures.isNotEmpty() }
    if (avecMesures.size < 4) return null
    val toutes = avecMesures.flatMap { c -> c.mesures.map { c to it } }.filter { it.second.id !in dejaPosees }
    if (toutes.isEmpty()) return null
    val (auteur, mesure) = toutes.random(hasard)
    val texte = normaliser(mesure.texte)
    val leurres = avecMesures
        .filter { c -> c.id != auteur.id && c.mesures.none { normaliser(it.texte) == texte } }
        .shuffled(hasard)
        .take(3)
    if (leurres.size < 3) return null
    return QuestionQuiz(auteur, mesure, (leurres + auteur).shuffled(hasard))
}

// ---------------------------------------------------------------- Nouveautés et notifications

fun cleChangement(c: Changement): String = listOf(c.date, c.type, c.candidat, c.mesure, c.sondage, c.libelle).joinToString("|")

/** Changements apparus depuis la dernière mise à jour. */
fun nouveauxChangements(avant: List<Changement>, apres: List<Changement>): List<Changement> {
    val connus = avant.map(::cleChangement).toSet()
    return apres.filter { cleChangement(it) !in connus }
}

/** Ceux qui méritent une notification : seulement les candidats suivis. */
fun aNotifier(nouveaux: List<Changement>, suivis: Set<String>): List<Changement> =
    nouveaux.filter { it.candidat != null && it.candidat in suivis }

// ---------------------------------------------------------------- recherche

data class Trouvaille(val candidat: Candidat, val mesure: Mesure)

/** Mesures contenant tous les mots tapés, sans tenir compte des accents ni des majuscules (« smic », « nucleaire »). */
fun rechercher(candidats: List<Candidat>, requete: String): List<Trouvaille> {
    val mots = normaliser(requete).split(Regex("[^\\p{L}\\p{N}%]+")).filter { it.length >= 2 }
    if (mots.isEmpty()) return emptyList()
    return candidats.flatMap { c ->
        c.mesures.filter { m -> normaliser(m.texte).let { t -> mots.all { it in t } } }.map { Trouvaille(c, it) }
    }
}

// ---------------------------------------------------------------- votes au Parlement

fun votesDe(scrutins: List<Scrutin>, id: String): List<Pair<Scrutin, String>> =
    scrutins.mapNotNull { s -> s.votes[id]?.let { s to it } }.sortedByDescending { it.first.date }

fun libellePosition(p: String): String = when (p) {
    "pour" -> "Pour"
    "contre" -> "Contre"
    "abstention" -> "Abstention"
    "absent" -> "Absent"
    "non_votant" -> "Non-votant"
    else -> p.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

fun libelleChambre(c: String): String = when (c.uppercase()) {
    "AN" -> "Assemblée nationale"
    "SENAT" -> "Sénat"
    "PE" -> "Parlement européen"
    else -> c
}

// ---------------------------------------------------------------- partage

/** Texte envoyé par « Partager » : nom, domaine, texte exact, source, crédit. */
fun texteDePartage(candidat: Candidat, domaine: String, mesure: Mesure): String = buildString {
    appendLine("${candidat.nom} — $domaine")
    appendLine("« ${mesure.texte} »")
    val src = mesure.source
    if (src != null) {
        val details = listOfNotNull(src.titre.ifBlank { null }, src.date.ifBlank { null }?.let(::dateCourte)).joinToString(", ")
        appendLine("Source : $details${if (src.url.isNotBlank()) " — ${src.url}" else ""}")
    }
    append("Via « Avant de voter », données 2027 et moi (2027etmoi.fr)")
}
