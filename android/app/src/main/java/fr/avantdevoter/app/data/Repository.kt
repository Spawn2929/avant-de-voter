package fr.avantdevoter.app.data

import android.content.Context
import fr.avantdevoter.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Issue d'une mise à jour des données. */
sealed interface Resultat {
    data object DejaAJour : Resultat
    data object MisAJour : Resultat
    /** Le robot publie un format plus récent : il faut installer la nouvelle version de l'appli. */
    data object AppliTropAncienne : Resultat
    data class Echec(val message: String) : Resultat
}

/**
 * Données de l'appli : lecture dans la base locale, mise à jour depuis les fichiers du robot.
 *
 * Mise à jour : on lit manifest.json (moins d'1 Ko) ; si l'empreinte d'app-data.json n'a pas changé,
 * on s'arrête là. Sinon on télécharge app-data.json et changes.json, on vérifie leur empreinte,
 * on récupère les photos manquantes, puis on remplace la base d'un seul coup.
 */
class Repository(private val context: Context, private val db: AppDatabase, val prefs: Prefs) {

    private val dao = db.dao()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val verrou = Mutex()

    val dossierPhotos: File = File(context.filesDir, "photos")

    fun photo(c: Candidat): File? = c.photo?.let { File(context.filesDir, it.fichier) }?.takeIf { it.exists() }

    val catalogue: Flow<Catalogue> = combine(
        dao.candidats(), dao.mesures(), dao.sondages(), dao.changements(), dao.metas(),
    ) { cands, mesures, sondages, changements, metas ->
        val parCandidat = mesures.groupBy { it.candidatId }
        val meta = metas.associate { it.cle to it.valeur }
        Catalogue(
            source = meta["source"]?.let { json.decodeFromString(SourceDonnees.serializer(), it) },
            themes = meta["themes"]?.let { json.decodeFromString(ListSerializer(Theme.serializer()), it) }.orEmpty(),
            candidats = cands.map { row ->
                json.decodeFromString(Candidat.serializer(), row.json).copy(
                    mesures = parCandidat[row.id].orEmpty().map { m ->
                        Mesure(m.id, m.theme, m.texte, m.nature, Ref(m.sourceTitre, m.sourceUrl, m.sourceDate))
                    },
                )
            },
            sondages = sondages.map { json.decodeFromString(Sondage.serializer(), it.json) },
            nomsHorsListe = meta["noms_hors_liste"]
                ?.let { json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it) }.orEmpty(),
            moyenne = meta["moyenne"]?.let { json.decodeFromString(Moyenne.serializer(), it) },
            calendrier = meta["calendrier"]?.let { json.decodeFromString(ListSerializer(Etape.serializer()), it) }.orEmpty(),
            changements = changements.map { json.decodeFromString(Changement.serializer(), it.json) },
        )
    }.flowOn(Dispatchers.Default)

    /**
     * Premier lancement : la base est remplie avec la copie des données intégrée à l'APK,
     * pour que tout s'affiche tout de suite, même sans réseau.
     */
    suspend fun importerCopieIntegree() = verrou.withLock {
        withContext(Dispatchers.IO) {
            if (dao.nbCandidats() > 0) return@withContext
            val assets = context.assets
            val noms = assets.list("snapshot").orEmpty()
            if ("app-data.json" !in noms) return@withContext // APK fabriqué sans copie : la synchro s'en chargera
            val appBytes = assets.open("snapshot/app-data.json").use { it.readBytes() }
            val changesBytes = if ("changes.json" in noms) assets.open("snapshot/changes.json").use { it.readBytes() } else null
            val app = json.decodeFromString(AppData.serializer(), appBytes.decodeToString())
            dossierPhotos.mkdirs()
            for (c in app.candidats) {
                val p = c.photo ?: continue
                runCatching {
                    assets.open("snapshot/${p.fichier}").use { input ->
                        File(context.filesDir, p.fichier).outputStream().use { input.copyTo(it) }
                    }
                }
            }
            enregistrer(app, changesBytes?.let { json.decodeFromString(Changes.serializer(), it.decodeToString()) })
            prefs.setCopieIntegree(sha256(appBytes))
        }
    }

    /** Vérifie auprès du robot et met à jour si besoin. Sans réseau : Echec, les données locales restent. */
    suspend fun mettreAJour(): Resultat = verrou.withLock {
        withContext(Dispatchers.IO) {
            try {
                val manifest = json.decodeFromString(Manifest.serializer(), telecharger("manifest.json").decodeToString())
                if (manifest.format > FORMAT_LU) return@withContext Resultat.AppliTropAncienne
                val attendu = manifest.fichiers.getValue("app-data.json").sha256
                if (attendu == prefs.sha() && dao.nbCandidats() > 0) {
                    prefs.setVerifie(System.currentTimeMillis())
                    return@withContext Resultat.DejaAJour
                }
                val appBytes = telecharger("app-data.json")
                if (sha256(appBytes) != attendu) throw IOException("fichier incomplet, nouvel essai plus tard")
                val changesBytes = telecharger("changes.json")
                val app = json.decodeFromString(AppData.serializer(), appBytes.decodeToString())
                if (app.format > FORMAT_LU) return@withContext Resultat.AppliTropAncienne
                val changes = json.decodeFromString(Changes.serializer(), changesBytes.decodeToString())
                recupererPhotos(app)
                enregistrer(app, changes)
                prefs.setMisAJour(attendu, System.currentTimeMillis())
                Resultat.MisAJour
            } catch (_: java.net.UnknownHostException) {
                Resultat.Echec("pas de connexion internet")
            } catch (_: java.net.SocketTimeoutException) {
                Resultat.Echec("connexion trop lente")
            } catch (e: IOException) {
                Resultat.Echec(e.message?.takeIf { it.startsWith("le serveur") || it.startsWith("fichier") } ?: "pas de connexion internet")
            } catch (e: Exception) {
                Resultat.Echec("données illisibles (${e.javaClass.simpleName})")
            }
        }
    }

    /** Dernière version de l'appli publiée sur GitHub, ou null si on ne peut pas savoir. */
    suspend fun derniereVersion(): VersionPubliee? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val o = json.parseToJsonElement(r.body!!.string()) as kotlinx.serialization.json.JsonObject
                val tag = (o["tag_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@use null
                val url = (o["html_url"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@use null
                VersionPubliee(tag.removePrefix("v"), url)
            }
        }.getOrNull()
    }

    private fun telecharger(chemin: String): ByteArray {
        val req = Request.Builder().url(BuildConfig.DATA_URL + chemin).header("Cache-Control", "no-cache").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("le serveur de données répond ${r.code}")
            return r.body!!.bytes()
        }
    }

    /** Photos manquantes uniquement (le nom du fichier change quand la photo change) ; les anciennes sont effacées. */
    private fun recupererPhotos(app: AppData) {
        dossierPhotos.mkdirs()
        val voulus = app.candidats.mapNotNull { it.photo?.fichier }.toSet()
        for (fichier in voulus) {
            val f = File(context.filesDir, fichier)
            if (f.exists()) continue
            runCatching {
                val tmp = File(f.path + ".tmp")
                tmp.writeBytes(telecharger(fichier))
                tmp.renameTo(f)
            } // une photo manquée : initiales en attendant la prochaine mise à jour
        }
        dossierPhotos.listFiles()?.forEach { f -> if ("photos/${f.name}" !in voulus) f.delete() }
    }

    private suspend fun enregistrer(app: AppData, changes: Changes?) {
        val candidats = app.candidats.map { c ->
            CandidatRow(c.id, c.tri, c.statut, json.encodeToString(Candidat.serializer(), c.copy(mesures = emptyList())))
        }
        val mesures = app.candidats.flatMap { c ->
            c.mesures.mapIndexed { i, m ->
                MesureRow(c.id, m.id, i, m.theme, m.texte, m.nature, m.source?.titre.orEmpty(), m.source?.url.orEmpty(), m.source?.date.orEmpty())
            }
        }
        val sondages = app.sondages.map { SondageRow(it.id, it.terrainFin, json.encodeToString(Sondage.serializer(), it)) }
        val changements = changes?.changements.orEmpty().map { ChangementRow(date = it.date, json = json.encodeToString(Changement.serializer(), it)) }
        val metas = buildList {
            add(MetaRow("source", json.encodeToString(SourceDonnees.serializer(), app.source)))
            add(MetaRow("themes", json.encodeToString(ListSerializer(Theme.serializer()), app.themes)))
            add(MetaRow("noms_hors_liste", json.encodeToString(MapSerializer(String.serializer(), String.serializer()), app.nomsHorsListe)))
            add(MetaRow("calendrier", json.encodeToString(ListSerializer(Etape.serializer()), app.calendrier)))
            app.moyenne?.let { add(MetaRow("moyenne", json.encodeToString(Moyenne.serializer(), it))) }
        }
        dao.remplacer(candidats, mesures, sondages, changements, metas)
    }

    private fun sha256(b: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
}

data class VersionPubliee(val version: String, val url: String)
