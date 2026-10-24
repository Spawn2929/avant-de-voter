package fr.avantdevoter.app.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.ThemeChoisi
import fr.avantdevoter.app.sync.Notifications
import fr.avantdevoter.app.ui.theme.AvantDeVoterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lireIntent(intent)
        setContent {
            val reglages by vm.reglages.collectAsState()
            val sombre = when (reglages?.theme) {
                ThemeChoisi.CLAIR -> false
                ThemeChoisi.SOMBRE -> true
                else -> isSystemInDarkTheme()
            }
            // Icônes de la barre d'état (heure, batterie) claires sur fond sombre, foncées sur fond clair.
            LaunchedEffect(sombre) {
                val style = if (sombre) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AvantDeVoterTheme(sombre) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lireIntent(intent)
    }

    /** Toucher une notification ouvre l'onglet Nouveautés. */
    private fun lireIntent(intent: Intent?) {
        intent?.getStringExtra(Notifications.EXTRA_ONGLET)?.let { vm.demanderOnglet(it) }
    }
}

/** Onglets de la barre du bas, dans l'ordre du cadrage. */
private enum class Onglet(val label: String, val icone: ImageVector) {
    DOMAINES("Domaines", Icons.Outlined.Category),
    CANDIDATS("Candidats", Icons.Outlined.Groups),
    SONDAGES("Sondages", Icons.Outlined.BarChart),
    NOUVEAUTES("Nouveautés", Icons.Outlined.NewReleases),
    PLUS("Plus", Icons.Outlined.MoreHoriz),
}

/** Écrans ouverts par-dessus les onglets, empilés : le geste retour ferme le dernier. */
private sealed interface Ecran {
    data class Fiche(val id: String) : Ecran
    data class Photo(val id: String) : Ecran
    data class Sondage(val id: String, val institut: String?) : Ecran
    data class Scores(val id: String) : Ecran
    data class Comparer(val ids: List<String>) : Ecran
    data object Calendrier : Ecran
    data object Priorites : Ecran
    data object Quiz : Ecran
    data object Boussole : Ecran
    data object Recherche : Ecran
    data object Reglages : Ecran
    data object Credits : Ecran
}

private fun encoder(e: Ecran) = when (e) {
    is Ecran.Fiche -> "fiche:${e.id}"
    is Ecran.Photo -> "photo:${e.id}"
    is Ecran.Sondage -> "sondage:${e.id}:${e.institut.orEmpty()}"
    is Ecran.Scores -> "scores:${e.id}"
    is Ecran.Comparer -> "comparer:${e.ids.joinToString(",")}"
    Ecran.Calendrier -> "calendrier"
    Ecran.Priorites -> "priorites"
    Ecran.Quiz -> "quiz"
    Ecran.Boussole -> "boussole"
    Ecran.Recherche -> "recherche"
    Ecran.Reglages -> "reglages"
    Ecran.Credits -> "credits"
}

private fun decoder(s: String): Ecran? {
    val (type, reste) = s.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return when (type) {
        "fiche" -> Ecran.Fiche(reste)
        "photo" -> Ecran.Photo(reste)
        "sondage" -> reste.split(':', limit = 2).let { Ecran.Sondage(it[0], it.getOrElse(1) { "" }.ifBlank { null }) }
        "scores" -> Ecran.Scores(reste)
        "comparer" -> Ecran.Comparer(reste.split(',').filter { it.isNotBlank() })
        "calendrier" -> Ecran.Calendrier
        "priorites" -> Ecran.Priorites
        "quiz" -> Ecran.Quiz
        "boussole" -> Ecran.Boussole
        "recherche" -> Ecran.Recherche
        "reglages" -> Ecran.Reglages
        "credits" -> Ecran.Credits
        else -> null
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val catalogue by vm.catalogue.collectAsState()
    val reglages by vm.reglages.collectAsState()
    val ongletDemande by vm.ongletDemande.collectAsState()
    val cat = catalogue ?: return // base pas encore lue : écran vide un instant
    val r = reglages ?: return
    val context = LocalContext.current

    // Pile des écrans ouverts, gardée en cas de rotation.
    var pile by rememberSaveable { mutableStateOf(listOf<String>()) }
    val ouvrir: (Ecran) -> Unit = { pile = pile + encoder(it) }
    val fermer: () -> Unit = { pile = pile.dropLast(1) }
    val snackbar = remember { SnackbarHostState() }
    val pager = rememberPagerState { Onglet.entries.size }

    // Autorisation d'envoyer des notifications : demandée quand on suit un premier candidat (Android 13 et plus).
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) vm.message("Sans autorisation, l'appli ne pourra pas vous prévenir. Modifiable dans Plus → Réglages.")
    }
    LaunchedEffect(Unit) {
        vm.demanderNotifications.collect {
            if (Build.VERSION.SDK_INT >= 33 && !Notifications.autorisees(context)) demande.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(msg) }
        }
    }

    LaunchedEffect(ongletDemande) {
        if (ongletDemande == Notifications.ONGLET_NOUVEAUTES) {
            pile = emptyList()
            pager.scrollToPage(Onglet.NOUVEAUTES.ordinal)
            vm.demanderOnglet(null)
        }
    }

    val dessus = pile.lastOrNull()?.let(::decoder)
    BackHandler(enabled = dessus != null) { fermer() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // chaque écran gère la barre d'état lui-même
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (dessus !is Ecran.Photo) BarreOnglets(pager) { pile = emptyList() } },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Onglets(vm, pager, cat, r, ouvrir, visible = dessus == null)
            if (dessus != null) {
                Surface(Modifier.fillMaxSize()) {
                    EcranDessus(vm, cat, r, dessus, ouvrir, fermer, context)
                }
            }
        }
    }
}

@Composable
private fun EcranDessus(
    vm: MainViewModel,
    cat: Catalogue,
    r: Reglages,
    dessus: Ecran,
    ouvrir: (Ecran) -> Unit,
    fermer: () -> Unit,
    context: android.content.Context,
) {
    when (dessus) {
        is Ecran.Fiche -> {
            val c = cat.parId[dessus.id]
            if (c == null) LaunchedEffect(Unit) { fermer() }
            else FicheCandidatScreen(
                vm, cat, c, suivi = c.id in r.suivis,
                onRetour = fermer,
                onPhoto = { ouvrir(Ecran.Photo(c.id)) },
                onSondages = { ouvrir(Ecran.Scores(c.id)) },
            )
        }
        is Ecran.Photo -> cat.parId[dessus.id]?.let { PhotoPleinEcran(vm, it, fermer) } ?: LaunchedEffect(Unit) { fermer() }
        is Ecran.Sondage -> FicheSondageScreen(vm, cat, dessus.id, dessus.institut, fermer) { ouvrir(Ecran.Scores(it)) }
        is Ecran.Scores -> ScoresCandidatScreen(vm, cat, dessus.id, fermer) { ouvrir(Ecran.Sondage(it, null)) }
        is Ecran.Comparer -> ComparateurScreen(vm, cat, dessus.ids, fermer) { ouvrir(Ecran.Fiche(it)) }
        Ecran.Calendrier -> CalendrierScreen(cat, fermer)
        Ecran.Priorites -> PrioritesScreen(vm, cat, fermer)
        Ecran.Quiz -> QuizScreen(vm, cat, fermer)
        Ecran.Boussole -> BoussoleScreen(vm, cat, fermer) { ouvrir(Ecran.Fiche(it)) }
        Ecran.Recherche -> RechercheScreen(vm, cat, r, fermer) { ouvrir(Ecran.Fiche(it)) }
        Ecran.Reglages -> ReglagesScreen(vm, cat, r, fermer)
        Ecran.Credits -> CreditsScreen(vm, cat, fermer)
    }
}

@Composable
private fun BarreOnglets(pager: PagerState, onToucher: () -> Unit) {
    val scope = rememberCoroutineScope()
    NavigationBar {
        Onglet.entries.forEachIndexed { i, o ->
            NavigationBarItem(
                selected = pager.currentPage == i,
                onClick = { onToucher(); scope.launch { pager.animateScrollToPage(i) } },
                icon = { Icon(o.icone, null) },
                label = { Text(o.label, maxLines = 1, softWrap = false) },
            )
        }
    }
}

/** Les onglets, côte à côte : glisser vers la gauche affiche l'onglet de droite. */
@Composable
private fun Onglets(vm: MainViewModel, pager: PagerState, cat: Catalogue, r: Reglages, ouvrir: (Ecran) -> Unit, visible: Boolean) {
    val scope = rememberCoroutineScope()
    // Retour depuis un autre onglet : revient à Domaines ; depuis Domaines : quitte l'appli.
    BackHandler(enabled = visible && pager.currentPage != 0) { scope.launch { pager.animateScrollToPage(0) } }

    HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
        val fiche: (String) -> Unit = { ouvrir(Ecran.Fiche(it)) }
        when (Onglet.entries[page]) {
            Onglet.DOMAINES -> DomainesScreen(vm, cat, r, fiche, onCalendrier = { ouvrir(Ecran.Calendrier) }, onRecherche = { ouvrir(Ecran.Recherche) })
            Onglet.CANDIDATS -> CandidatsScreen(vm, cat, r, fiche) { ouvrir(Ecran.Comparer(it)) }
            Onglet.SONDAGES -> SondagesScreen(vm, cat, onSondage = { id, institut -> ouvrir(Ecran.Sondage(id, institut)) }) { ouvrir(Ecran.Scores(it)) }
            Onglet.NOUVEAUTES -> NouveautesScreen(vm, cat, r, fiche) { ouvrir(Ecran.Sondage(it, null)) }
            Onglet.PLUS -> PlusScreen(
                cat,
                onCalendrier = { ouvrir(Ecran.Calendrier) },
                onPriorites = { ouvrir(Ecran.Priorites) },
                onQuiz = { ouvrir(Ecran.Quiz) },
                onBoussole = { ouvrir(Ecran.Boussole) },
                onReglages = { ouvrir(Ecran.Reglages) },
                onCredits = { ouvrir(Ecran.Credits) },
            )
        }
    }
}
