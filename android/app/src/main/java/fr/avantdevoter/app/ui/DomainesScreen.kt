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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.filtrer
import fr.avantdevoter.app.data.mesuresDu

/** Onglet Domaines : un domaine choisi, chaque candidat avec ses mesures sur ce domaine. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainesScreen(vm: MainViewModel, catalogue: Catalogue, reglages: Reglages, onCandidat: (String) -> Unit) {
    val enCours by vm.enCours.collectAsState()
    val domaine = catalogue.themes.firstOrNull { it.id == reglages.domaine } ?: catalogue.themes.firstOrNull()
    val candidats = remember(catalogue, reglages.declares, reglages.pressentis) {
        filtrer(catalogue.candidats, reglages.declares, reglages.pressentis)
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
            Text(
                "Avant de voter",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
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
            FiltresStatut(reglages, vm::setFiltres, Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }

        PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(state = liste, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (domaine == null || candidats.isEmpty()) {
                    item { EtatVide("Aucun candidat à afficher avec ces filtres.") }
                }
                if (domaine != null) {
                    items(candidats, key = { it.id }) { c ->
                        BlocCandidat(vm, c, mesuresDu(c, domaine.id), onCandidat)
                    }
                }
                item { PiedDePage(catalogue.source) }
            }
        }
    }
}

@Composable
private fun BlocCandidat(vm: MainViewModel, c: Candidat, mesures: List<fr.avantdevoter.app.data.Mesure>, onCandidat: (String) -> Unit) {
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
        }
        Spacer(Modifier.height(6.dp))
        if (mesures.isEmpty()) {
            AucuneProposition(c)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mesures.forEach { m -> CarteMesure(m) { vm.message("Texte de la mesure copié") } }
            }
        }
    }
}
