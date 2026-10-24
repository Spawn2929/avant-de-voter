package fr.avantdevoter.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Format des fichiers publiés que cette version de l'appli sait lire. */
const val FORMAT_LU = 1

val json = Json {
    ignoreUnknownKeys = true // le robot peut ajouter des champs sans casser les anciennes versions
    explicitNulls = false
}

// Fichiers publiés par le robot (voir pipeline/build.py) : mêmes noms de champs.

@Serializable
data class Manifest(
    val format: Int,
    @SerialName("source_mise_a_jour") val sourceMiseAJour: String,
    @SerialName("publie_le") val publieLe: String,
    val fichiers: Map<String, FichierPublie>,
)

@Serializable
data class FichierPublie(val sha256: String, val taille: Long)

@Serializable
data class AppData(
    val format: Int,
    val source: SourceDonnees,
    val themes: List<Theme>,
    val candidats: List<Candidat>,
    val sondages: List<Sondage> = emptyList(),
    @SerialName("noms_hors_liste") val nomsHorsListe: Map<String, String> = emptyMap(),
    val moyenne: Moyenne? = null,
    val calendrier: List<Etape> = emptyList(),
    val scrutins: List<Scrutin> = emptyList(),
    val mandats: Map<String, List<Mandat>> = emptyMap(),
)

/** Vote au Parlement : position de chaque personnalité (pour, contre, abstention, absent, non_votant). */
@Serializable
data class Scrutin(
    val id: String,
    val chambre: String = "",
    val date: String,
    val titre: String = "",
    val type: String = "",
    val resultat: String = "",
    val url: String = "",
    val votes: Map<String, String> = emptyMap(),
)

@Serializable
data class Mandat(
    val chambre: String = "",
    val detail: String = "",
    val debut: String = "",
    val fin: String = "",
    val source: Ref? = null,
)

@Serializable
data class SourceDonnees(
    val nom: String,
    val site: String,
    val depot: String,
    val licence: String,
    @SerialName("mise_a_jour") val miseAJour: String,
)

@Serializable
data class Theme(val id: String, val label: String)

@Serializable
data class Ref(val titre: String = "", val url: String = "", val date: String = "")

@Serializable
data class Candidat(
    val id: String,
    val tri: String,
    val nom: String,
    val parti: String = "",
    val bloc: String = "",
    val statut: String,
    @SerialName("statut_detail") val statutDetail: String = "",
    @SerialName("statut_source") val statutSource: Ref? = null,
    @SerialName("propositions_phares") val propositionsPhares: List<String> = emptyList(),
    val liens: Map<String, String> = emptyMap(),
    val photo: Photo? = null,
    val presentation: String = "",
    @SerialName("presentation_sources") val presentationSources: List<Ref> = emptyList(),
    @SerialName("parcours_politique") val parcoursPolitique: List<Entree> = emptyList(),
    @SerialName("parcours_professionnel") val parcoursProfessionnel: List<Entree> = emptyList(),
    val affaires: List<Affaire> = emptyList(),
    val programme: Programme? = null,
    val mesures: List<Mesure> = emptyList(),
)

@Serializable
data class Photo(
    val fichier: String,
    val page: String = "",
    val auteur: String = "",
    val licence: String = "",
    val legende: String = "",
    /** « 2027 et moi », ou « ajoutée par l'appli depuis Wikimedia Commons » quand la source n'en avait pas. */
    val origine: String = "2027 et moi",
)

@Serializable
data class Entree(val periode: String = "", val texte: String, val source: Ref? = null)

@Serializable
data class Affaire(
    val titre: String = "",
    val texte: String = "",
    val etat: String = "",
    @SerialName("etat_detail") val etatDetail: String = "",
    @SerialName("date_etat") val dateEtat: String = "",
    val sources: List<Ref> = emptyList(),
)

@Serializable
data class Programme(val etat: String = "aucun", val titre: String = "", val url: String = "", val note: String = "")

@Serializable
data class Mesure(
    val id: String,
    val theme: String,
    val texte: String,
    val nature: String,
    val source: Ref? = null,
)

@Serializable
data class Sondage(
    val id: String,
    val institut: String,
    val commanditaire: String = "",
    @SerialName("terrain_debut") val terrainDebut: String = "",
    @SerialName("terrain_fin") val terrainFin: String,
    val echantillon: Int? = null,
    val source: Ref? = null,
    val hypotheses: List<Hypothese> = emptyList(),
)

@Serializable
data class Hypothese(val label: String = "", val scores: Map<String, Double> = emptyMap())

@Serializable
data class Moyenne(
    @SerialName("fenetre_jours") val fenetreJours: Int,
    @SerialName("nb_sondages") val nbSondages: Int,
    val regle: String,
    val scores: List<ScoreMoyen>,
)

@Serializable
data class ScoreMoyen(val id: String, val score: Double, @SerialName("nb_sondages") val nbSondages: Int)

@Serializable
data class Etape(
    val date: String,
    val fin: String = "",
    val titre: String,
    val statut: String = "",
    val source: Ref? = null,
)

@Serializable
data class Changes(val format: Int, val changements: List<Changement>)

@Serializable
data class Changement(
    val date: String,
    val type: String,
    val candidat: String? = null,
    val theme: String? = null,
    val mesure: String? = null,
    val sondage: String? = null,
    val libelle: String = "",
    val texte: String? = null,
)
