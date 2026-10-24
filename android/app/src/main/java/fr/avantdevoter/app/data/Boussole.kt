package fr.avantdevoter.app.data

import kotlin.math.abs

// « De quel bord es-tu ? » : 10 questions de 2027 et moi, réponses oui / non, proximité avec chaque candidat déclaré.
// Fonctions pures, testées. Les questions et les positions sont celles de la source, sans reformulation.

enum class Reponse(val valeur: Double?) { OUI(1.0), NON(-1.0), PASSER(null) }

/** Échelle de la source → nombre entre -1 (contre) et 1 (pour) ; null si la position n'est pas connue. */
fun valeurPosition(p: String?): Double? = when (p) {
    "pour" -> 1.0
    "plutot_pour" -> 0.5
    "nuance" -> 0.0
    "plutot_contre" -> -0.5
    "contre" -> -1.0
    else -> null
}

fun libellePositionCle(p: String): String = when (p) {
    "pour" -> "Pour"
    "plutot_pour" -> "Plutôt pour"
    "nuance" -> "Nuancé"
    "plutot_contre" -> "Plutôt contre"
    "contre" -> "Contre"
    else -> "Position non connue"
}

/**
 * Les questions posées : celles où le plus de candidats déclarés ont une position connue, à condition qu'elles
 * départagent (au moins un « pour » et un « contre », même nuancés), 2 au plus par domaine.
 */
fun choisirQuestions(
    questions: List<QuestionCle>,
    positions: Map<String, Map<String, PositionCle>>,
    declares: Set<String>,
    nombre: Int = 10,
    maxParDomaine: Int = 2,
): List<QuestionCle> {
    fun valeurs(q: QuestionCle) = declares.mapNotNull { valeurPosition(positions[it]?.get(q.id)?.position) }
    val candidates = questions
        .filter { q -> valeurs(q).let { v -> v.any { it > 0 } && v.any { it < 0 } } }
        .sortedWith(compareByDescending<QuestionCle> { valeurs(it).size }.thenBy { it.id })
    val choisies = mutableListOf<QuestionCle>()
    val parDomaine = mutableMapOf<String, Int>()
    for (q in candidates) {
        if (choisies.size == nombre) break
        if ((parDomaine[q.theme] ?: 0) >= maxParDomaine) continue
        choisies += q
        parDomaine[q.theme] = (parDomaine[q.theme] ?: 0) + 1
    }
    return choisies
}

/** Accord sur une question : 1 si identique, 0 si opposé (« oui » face à « contre »), 0,5 face à « nuancé ». */
fun accord(reponse: Double, position: Double): Double = 1 - abs(reponse - position) / 2

data class Proximite(val candidat: Candidat, val pourcent: Int, val questionsCommunes: Int)

/** En dessous de 5 questions en commun (sur 10), la comparaison serait trop fragile pour être montrée. */
const val QUESTIONS_COMMUNES_MIN = 5

/**
 * Proximité avec chaque candidat déclaré : moyenne des accords sur les questions où l'on a répondu
 * et où sa position est connue. Il faut au moins [QUESTIONS_COMMUNES_MIN] questions en commun.
 */
fun proximites(
    candidats: List<Candidat>,
    positions: Map<String, Map<String, PositionCle>>,
    reponses: Map<String, Reponse>,
    minCommunes: Int = QUESTIONS_COMMUNES_MIN,
): List<Proximite> = candidats.mapNotNull { c ->
    val accords = reponses.mapNotNull { (qid, r) ->
        val u = r.valeur ?: return@mapNotNull null
        val p = valeurPosition(positions[c.id]?.get(qid)?.position) ?: return@mapNotNull null
        accord(u, p)
    }
    if (accords.size < minCommunes) null
    else Proximite(c, Math.round(accords.average() * 100).toInt(), accords.size)
}.sortedWith(compareByDescending<Proximite> { it.pourcent }.thenByDescending { it.questionsCommunes }.thenBy { it.candidat.tri })

/**
 * Place sur l'axe gauche-droite, d'après le bloc que 2027 et moi attribue au candidat.
 * Les blocs « souverainiste » et « autre » ne sont pas placés : la source ne les situe pas sur cet axe.
 */
fun positionBloc(bloc: String): Double? = when (bloc) {
    "gauche" -> -2.0
    "ecolo" -> -1.0
    "centre" -> 0.0
    "droite" -> 1.0
    "extdroite" -> 2.0
    else -> null
}

val GRADUATIONS = listOf(-2.0 to "Gauche", -1.0 to "Écologistes", 0.0 to "Centre", 1.0 to "Droite", 2.0 to "Extrême droite")

/**
 * Votre place sur l'axe : moyenne des places des 3 candidats placés les plus proches, pondérée par la proximité.
 * Null si aucun candidat placé n'est comparable.
 */
fun positionUtilisateur(proches: List<Proximite>): Double? {
    val places = proches.mapNotNull { p -> positionBloc(p.candidat.bloc)?.let { p to it } }.take(3)
    val poids = places.sumOf { it.first.pourcent.toDouble() }
    if (places.isEmpty() || poids == 0.0) return null
    return places.sumOf { it.first.pourcent * it.second } / poids
}

/** « plutôt à gauche », « au centre »… pour la phrase de résultat (seuils au milieu des graduations). */
fun libelleZone(x: Double): String = when {
    x < -1.5 -> "à gauche"
    x < -0.5 -> "entre la gauche et le centre, proche des écologistes"
    x <= 0.5 -> "au centre"
    x <= 1.5 -> "à droite"
    else -> "à l'extrême droite"
}

/** Nom du bloc attribué par 2027 et moi. */
fun libelleBloc(bloc: String): String = when (bloc) {
    "gauche" -> "Gauche"
    "ecolo" -> "Écologistes"
    "centre" -> "Centre"
    "droite" -> "Droite"
    "extdroite" -> "Extrême droite"
    "souverainiste" -> "Souverainiste"
    "autre" -> "Autre"
    else -> bloc
}
