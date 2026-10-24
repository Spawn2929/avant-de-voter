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
import fr.avantdevoter.app.sync.AvantDeVoterWidget
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
            val appliMiseAJour = prefs.nouvelleVersionDeLAppli(BuildConfig.VERSION_CODE.toLong())
            repository.importerCopieIntegree(remplacer = appliMiseAJour)
            // À l'ouverture : mise à jour si l'appli vient d'être mise à jour
            // ou si la dernière vérification date de plus de 24 h.
            val derniere = prefs.verificationNow()
            if (appliMiseAJour || derniere == null || System.currentTimeMillis() - derniere > 24 * 3600_000L) {
                mettreAJour(silencieux = true)
            }
        }
    }

    /** « Mettre à jour maintenant » ou tirer l'écran vers le bas. */
    fun mettreAJour(silencieux: Boolean = false) {
        if (_enCours.value) return
        viewModelScope.launch {
            _enCours.value = true
            val r = repository.mettreAJour()
            _enCours.value = false
            AvantDeVoterWidget.rafraichir(getApplication())
            _appliTropAncienne.value = r is Resultat.AppliTropAncienne
            val msg = when (r) {
                is Resultat.MisAJour -> "Données mises à jour" +
                    if (r.nouveaux.isNotEmpty()) " : ${r.nouveaux.size} nouveauté${if (r.nouveaux.size > 1) "s" else ""}" else ""
                Resultat.DejaAJour -> "Les données sont déjà à jour"
                Resultat.AppliTropAncienne -> "Installez la nouvelle version de l'appli pour avoir les dernières données"
                is Resultat.Echec -> "Mise à jour impossible (${r.message}). Les données déjà enregistrées restent consultables."
            }
            if (!silencieux || r is Resultat.MisAJour || r == Resultat.AppliTropAncienne) _messages.send(msg)
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

    /** Onglet à afficher à l'ouverture (toucher une notification ouvre Nouveautés). */
    private val _ongletDemande = MutableStateFlow<String?>(null)
    val ongletDemande: StateFlow<String?> = _ongletDemande.asStateFlow()
    fun demanderOnglet(o: String?) { _ongletDemande.value = o }

    /** Vrai quand il faut demander l'autorisation d'envoyer des notifications (premier candidat étoilé). */
    private val _demanderNotifications = Channel<Unit>(Channel.CONFLATED)
    val demanderNotifications = _demanderNotifications.receiveAsFlow()

    fun basculerSuivi(id: String, nom: String) {
        viewModelScope.launch {
            val suivi = prefs.basculerSuivi(id)
            _messages.send(if (suivi) "Vous suivez $nom : l'appli vous préviendra de ses changements" else "Vous ne suivez plus $nom")
            if (suivi) _demanderNotifications.send(Unit)
        }
    }

    fun setSuivisSeulement(v: Boolean) = viewModelScope.launch { prefs.setSuivisSeulement(v) }

    fun setTheme(t: ThemeChoisi) = viewModelScope.launch { prefs.setTheme(t) }
    fun setFiltres(declares: Boolean, pressentis: Boolean) = viewModelScope.launch { prefs.setFiltres(declares, pressentis) }
    fun setDomaine(id: String) = viewModelScope.launch { prefs.setDomaine(id) }
}
