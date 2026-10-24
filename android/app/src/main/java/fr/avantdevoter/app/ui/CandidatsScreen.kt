package fr.avantdevoter.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.filtrer

/** Onglet Candidats : grille de photos, toucher une carte ouvre la fiche. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidatsScreen(vm: MainViewModel, catalogue: Catalogue, reglages: Reglages, onCandidat: (String) -> Unit) {
    val enCours by vm.enCours.collectAsState()
    val candidats = remember(catalogue, reglages.declares, reglages.pressentis) {
        filtrer(catalogue.candidats, reglages.declares, reglages.pressentis)
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            Text(
                "Candidats",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            FiltresStatut(reglages, vm::setFiltres, Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
        PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "${candidats.size} candidat${if (candidats.size > 1) "s" else ""}, par ordre alphabétique",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (candidats.isEmpty()) {
                    item(span = { GridItemSpan(2) }) { EtatVide("Aucun candidat à afficher avec ces filtres.") }
                }
                items(candidats, key = { it.id }) { c ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClickLabel = "Voir la fiche") { onCandidat(c.id) },
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PhotoCandidat(c, vm.repository.photo(c), 96.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                c.nom,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                minLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                c.parti.ifBlank { "Sans parti indiqué" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                minLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            PastilleEtat(c)
                        }
                    }
                }
                item(span = { GridItemSpan(2) }) { PiedDePage(catalogue.source) }
            }
        }
    }
}
