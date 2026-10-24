package fr.avantdevoter.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Mesure
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.Theme
import fr.avantdevoter.app.data.compteARebours
import fr.avantdevoter.app.data.filtrer
import fr.avantdevoter.app.data.mesuresDu
import fr.avantdevoter.app.data.premierTour
import fr.avantdevoter.app.data.texteDePartage
import java.time.LocalDate
import java.time.ZoneId

/** Onglet Domaines : un domaine choisi, chaque candidat avec ses mesures sur ce domaine. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainesScreen(
    vm: MainViewModel,
    catalogue: Catalogue,
    reglages: Reglages,
    onCandidat: (String) -> Unit,
    onCalendrier: () -> Unit,
    onRecherche: () -> Unit,
) {
    val enCours by vm.enCours.collectAsState()
    val domaine = catalogue.themes.firstOrNull { it.id == reglages.domaine } ?: catalogue.themes.firstOrNull()
    val candidats = remember(catalogue, reglages) { filtrer(catalogue.candidats, reglages) }
    val rebours = remember(catalogue.calendrier) {
        compteARebours(premierTour(catalogue.calendrier), LocalDate.now(ZoneId.of("Europe/Paris")))
    }
    val chips = rememberLazyListState()
    val liste = rememberLazyListState()
    LaunchedEffect(domaine?.id) {
        liste.scrollToItem(0)
        val i = catalogue.themes.indexOfFirst { it.id == domaine?.id }
        if (i >= 0) chips.animateScrollToItem(maxOf(0, i - 1))
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Avant de voter", style = MaterialTheme.typography.headlineSmall)
                    if (rebours != null) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClickLabel = "Voir le calendrier") { onCalendrier() }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(rebours, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                IconButton(onClick = onRecherche) { Icon(Icons.Outlined.Search, "Rechercher dans les mesures") }
            }
            LazyRow(
                state = chips,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(catalogue.themes, key = { it.id }) { t ->
                    FilterChip(
                        selected = t.id == domaine?.id,
                        onClick = { vm.setDomaine(t.id) },
                        label = { Text(t.label) },
                    )
                }
            }
            FiltresStatut(reglages, vm::setFiltres, vm::setSuivisSeulement)
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }

        PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = liste, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (domaine == null || candidats.isEmpty()) {
                    item {
                        EtatVide(
                            if (reglages.suivisSeulement) "Aucun candidat suivi. Touchez l'étoile sur la fiche d'un candidat pour le suivre."
                            else "Aucun candidat à afficher avec ces filtres.",
                        )
                    }
                }
                if (domaine != null) {
                    items(candidats, key = { it.id }) { c ->
                        BlocCandidat(vm, c, domaine, mesuresDu(c, domaine.id), c.id in reglages.suivis, onCandidat)
                    }
                }
                item { PiedDePage(catalogue.source) }
            }
        }
    }
}

@Composable
private fun BlocCandidat(vm: MainViewModel, c: Candidat, domaine: Theme, mesures: List<Mesure>, suivi: Boolean, onCandidat: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = "Voir la fiche de ${c.nom}") { onCandidat(c.id) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoCandidat(c, vm.repository.photo(c), 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (c.parti.isNotBlank()) {
                    Text(c.parti, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (suivi) Icon(Icons.Outlined.Star, "Suivi", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        if (mesures.isEmpty()) {
            AucuneProposition(c)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mesures.forEach { m ->
                    CarteMesure(m, texteDePartage(c, domaine.label, m)) { vm.message("Texte de la mesure copié") }
                }
            }
        }
    }
}
