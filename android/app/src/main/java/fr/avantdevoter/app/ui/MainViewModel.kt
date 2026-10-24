package fr.avantdevoter.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.avantdevoter.app.AvantDeVoterApp
import fr.avantdevoter.app.BuildConfig
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.Resultat
import fr.avantdevoter.app.data.ThemeChoisi
import fr.avantdevoter.app.data.VersionPubliee
import fr.avantdevoter.app.data.estPlusRecente
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repository = (app as AvantDeVoterApp).repository
    private val prefs = repository.prefs

    /** null tant que la base n'a pas été lue (écran vide un instant). */
    val catalogue: StateFlow<Catalogue?> =
        repository.catalogue.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val reglages: StateFlow<Reglages?> = prefs.reglages.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val derniereVerification: StateFlow<Long?> =
        prefs.derniereVerification.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours.asStateFlow()

    /** Nouvelle version de l'appli disponible sur GitHub (null : à jour ou inconnu). */
    private val _nouvelleVersion = MutableStateFlow<VersionPubliee?>(null)
    val nouvelleVersion: StateFlow<VersionPubliee?> = _nouvelleVersion.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _appliTropAncienne = MutableStateFlow(false)
    val appliTropAncienne: StateFlow<Boolean> = _appliTropAncienne.asStateFlow()

    init {
        viewModelScope.launch {
            repository.importerCopieIntegree()
            // À l'ouverture : mise à jour si la dernière vérification date de plus de 24 h.
            val derniere = prefs.verificationNow()
            if (derniere == null || System.currentTimeMillis() - derniere > 24 * 3600_000L) mettreAJour(silencieux = true)
        }
    }

    /** « Mettre à jour maintenant » ou tirer l'écran vers le bas. */
    fun mettreAJour(silencieux: Boolean = false) {
        if (_enCours.value) return
        viewModelScope.launch {
            _enCours.value = true
            val r = repository.mettreAJour()
            _enCours.value = false
            _appliTropAncienne.value = r is Resultat.AppliTropAncienne
            val msg = when (r) {
                Resultat.MisAJour -> "Données mises à jour"
                Resultat.DejaAJour -> "Les données sont déjà à jour"
                Resultat.AppliTropAncienne -> "Installez la nouvelle version de l'appli pour avoir les dernières données"
                is Resultat.Echec -> "Mise à jour impossible (${r.message}). Les données déjà enregistrées restent consultables."
            }
            if (!silencieux || r == Resultat.MisAJour || r == Resultat.AppliTropAncienne) _messages.send(msg)
        }
    }

    fun verifierVersion() {
        viewModelScope.launch {
            _nouvelleVersion.value = repository.derniereVersion()
                ?.takeIf { estPlusRecente(it.version, BuildConfig.VERSION_NAME) }
        }
    }

    fun message(texte: String) {
        viewModelScope.launch { _messages.send(texte) }
    }

    fun setTheme(t: ThemeChoisi) = viewModelScope.launch { prefs.setTheme(t) }
    fun setFiltres(declares: Boolean, pressentis: Boolean) = viewModelScope.launch { prefs.setFiltres(declares, pressentis) }
    fun setDomaine(id: String) = viewModelScope.launch { prefs.setDomaine(id) }
}
