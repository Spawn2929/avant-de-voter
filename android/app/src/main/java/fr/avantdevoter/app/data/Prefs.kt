package fr.avantdevoter.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("avant_de_voter")

enum class ThemeChoisi(val id: String, val label: String) {
    AUTO("auto", "Automatique (comme le téléphone)"),
    CLAIR("clair", "Clair"),
    SOMBRE("sombre", "Sombre");

    companion object {
        fun de(id: String?) = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class Reglages(
    val theme: ThemeChoisi = ThemeChoisi.AUTO,
    val declares: Boolean = true,
    val pressentis: Boolean = false,
    val domaine: String = "economie",
)

/** Réglages propres à ce téléphone et état de la mise à jour des données. */
class Prefs(context: Context) {

    private val store = context.applicationContext.dataStore

    private object K {
        val THEME = stringPreferencesKey("theme")
        val DECLARES = booleanPreferencesKey("declares")
        val PRESSENTIS = booleanPreferencesKey("pressentis")
        val DOMAINE = stringPreferencesKey("domaine")
        val SHA = stringPreferencesKey("app_data_sha")
        val VERIFIE = longPreferencesKey("derniere_verification")
        val MAJ = longPreferencesKey("derniere_mise_a_jour")
    }

    val reglages: Flow<Reglages> = store.data.map {
        Reglages(
            theme = ThemeChoisi.de(it[K.THEME]),
            declares = it[K.DECLARES] ?: true,
            pressentis = it[K.PRESSENTIS] ?: false,
            domaine = it[K.DOMAINE] ?: "economie",
        )
    }.distinctUntilChanged()

    /** Heure (ms) de la dernière vérification réussie auprès du robot, même s'il n'y avait rien de neuf. */
    val derniereVerification: Flow<Long?> = store.data.map { it[K.VERIFIE] }.distinctUntilChanged()

    suspend fun setTheme(t: ThemeChoisi) = store.edit { it[K.THEME] = t.id }

    /** Au moins un des deux filtres reste coché : sinon la liste serait vide. */
    suspend fun setFiltres(declares: Boolean, pressentis: Boolean) = store.edit {
        it[K.DECLARES] = declares || !pressentis
        it[K.PRESSENTIS] = pressentis
    }

    suspend fun setDomaine(id: String) = store.edit { it[K.DOMAINE] = id }

    suspend fun sha(): String? = store.data.first()[K.SHA]

    suspend fun verificationNow(): Long? = store.data.first()[K.VERIFIE]

    suspend fun setVerifie(at: Long) = store.edit { it[K.VERIFIE] = at }

    suspend fun setMisAJour(sha: String, at: Long) = store.edit {
        it[K.SHA] = sha
        it[K.MAJ] = at
        it[K.VERIFIE] = at
    }

    /** Copie intégrée à l'APK : pas encore vérifiée auprès du robot. */
    suspend fun setCopieIntegree(sha: String) = store.edit { it[K.SHA] = sha }
}
