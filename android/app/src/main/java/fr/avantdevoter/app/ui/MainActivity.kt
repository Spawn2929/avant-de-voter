package fr.avantdevoter.app.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
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
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.ThemeChoisi
import fr.avantdevoter.app.ui.theme.AvantDeVoterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}

/** Onglets de la barre du bas (Sondages et Nouveautés arrivent à l'étape 2). */
private enum class Onglet(val label: String, val icone: ImageVector) {
    DOMAINES("Domaines", Icons.Outlined.Category),
    CANDIDATS("Candidats", Icons.Outlined.Groups),
    PLUS("Plus", Icons.Outlined.MoreHoriz),
}

/** Écrans ouverts par-dessus les onglets, empilés : le geste retour ferme le dernier. */
private sealed interface Ecran {
    data class Fiche(val id: String) : Ecran
    data class Photo(val id: String) : Ecran
    data object Reglages : Ecran
    data object Credits : Ecran
}

private fun encoder(e: Ecran) = when (e) {
    is Ecran.Fiche -> "fiche:${e.id}"
    is Ecran.Photo -> "photo:${e.id}"
    Ecran.Reglages -> "reglages"
    Ecran.Credits -> "credits"
}

private fun decoder(s: String): Ecran? = when {
    s.startsWith("fiche:") -> Ecran.Fiche(s.removePrefix("fiche:"))
    s.startsWith("photo:") -> Ecran.Photo(s.removePrefix("photo:"))
    s == "reglages" -> Ecran.Reglages
    s == "credits" -> Ecran.Credits
    else -> null
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val catalogue by vm.catalogue.collectAsState()
    val reglages by vm.reglages.collectAsState()
    val cat = catalogue ?: return // base pas encore lue : écran vide un instant
    val r = reglages ?: return

    // Pile des écrans ouverts, gardée en cas de rotation.
    var pile by rememberSaveable { mutableStateOf(listOf<String>()) }
    val ouvrir: (Ecran) -> Unit = { pile = pile + encoder(it) }
    val fermer: () -> Unit = { pile = pile.dropLast(1) }
    val snackbar = remember { SnackbarHostState() }
    val pager = rememberPagerState { Onglet.entries.size }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(msg) }
        }
    }

    val dessus = pile.lastOrNull()?.let(::decoder)
    BackHandler(enabled = dessus != null) { fermer() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // chaque écran gère la barre d'état lui-même
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (dessus !is Ecran.Photo) BarreOnglets(pager) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Onglets(vm, pager, cat, r, ouvrir, visible = dessus == null)
            when (dessus) {
                is Ecran.Fiche -> {
                    val c = cat.parId[dessus.id]
                    if (c == null) fermer() else Surface(Modifier.fillMaxSize()) {
                        FicheCandidatScreen(vm, cat, c, onRetour = fermer, onPhoto = { ouvrir(Ecran.Photo(c.id)) })
                    }
                }
                is Ecran.Photo -> cat.parId[dessus.id]?.let { PhotoPleinEcran(vm, it, fermer) } ?: fermer()
                Ecran.Reglages -> Surface(Modifier.fillMaxSize()) { ReglagesScreen(vm, cat, r, fermer) }
                Ecran.Credits -> Surface(Modifier.fillMaxSize()) { CreditsScreen(vm, cat, fermer) }
                null -> {}
            }
        }
    }
}

@Composable
private fun BarreOnglets(pager: PagerState) {
    val scope = rememberCoroutineScope()
    NavigationBar {
        Onglet.entries.forEachIndexed { i, o ->
            NavigationBarItem(
                selected = pager.currentPage == i,
                onClick = { scope.launch { pager.animateScrollToPage(i) } },
                icon = { Icon(o.icone, null) },
                label = { Text(o.label) },
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
        when (Onglet.entries[page]) {
            Onglet.DOMAINES -> DomainesScreen(vm, cat, r) { ouvrir(Ecran.Fiche(it)) }
            Onglet.CANDIDATS -> CandidatsScreen(vm, cat, r) { ouvrir(Ecran.Fiche(it)) }
            Onglet.PLUS -> PlusScreen(cat, onReglages = { ouvrir(Ecran.Reglages) }, onCredits = { ouvrir(Ecran.Credits) })
        }
    }
}
