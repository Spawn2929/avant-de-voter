package fr.avantdevoter.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.filtrer

/**
 * Onglet Candidats : grille de photos. Toucher une carte ouvre la fiche ;
 * appui long : sélection de 2 à 4 candidats pour les comparer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CandidatsScreen(
    vm: MainViewModel,
    catalogue: Catalogue,
    reglages: Reglages,
    onCandidat: (String) -> Unit,
    onComparer: (List<String>) -> Unit,
) {
    val enCours by vm.enCours.collectAsState()
    val haptic = LocalHapticFeedback.current
    val candidats = remember(catalogue, reglages) { filtrer(catalogue.candidats, reglages) }
    var selection by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun basculer(id: String) {
        selection = when {
            id in selection -> selection - id
            selection.size >= 4 -> { vm.message("4 candidats au plus"); selection }
            else -> selection + id
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            if (selection.isEmpty()) {
                Text(
                    "Candidats",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selection = emptyList() }) { Icon(Icons.Outlined.Close, "Annuler la sélection") }
                    Text(
                        "${selection.size} sélectionné${if (selection.size > 1) "s" else ""} (4 au plus)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            FiltresStatut(reglages, vm::setFiltres, vm::setSuivisSeulement)
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (selection.size >= 2) 88.dp else 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            if (selection.isEmpty()) {
                                "${candidats.size} candidat${if (candidats.size > 1) "s" else ""}, par ordre alphabétique. " +
                                    "Laissez le doigt sur une carte pour comparer plusieurs candidats."
                            } else {
                                "Touchez les cartes à comparer (2 à 4), puis « Comparer »."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (candidats.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            EtatVide(
                                if (reglages.suivisSeulement) "Aucun candidat suivi. Touchez l'étoile sur la fiche d'un candidat pour le suivre."
                                else "Aucun candidat à afficher avec ces filtres.",
                            )
                        }
                    }
                    items(candidats, key = { it.id }) { c ->
                        val choisi = c.id in selection
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (choisi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (choisi) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClickLabel = if (selection.isEmpty()) "Voir la fiche" else "Sélectionner",
                                    onLongClickLabel = "Sélectionner pour comparer",
                                    onClick = { if (selection.isEmpty()) onCandidat(c.id) else basculer(c.id) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        basculer(c.id)
                                    },
                                ),
                        ) {
                            Box {
                                Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                                if (choisi) {
                                    Icon(Icons.Outlined.CheckCircle, "Sélectionné", Modifier.align(Alignment.TopEnd).padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                                } else if (c.id in reglages.suivis) {
                                    Icon(Icons.Outlined.Star, "Suivi", Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(2) }) { PiedDePage(catalogue.source) }
                }
            }
            if (selection.size >= 2) {
                Button(
                    onClick = { onComparer(selection); selection = emptyList() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                ) {
                    Text("Comparer (${selection.size})")
                }
            }
        }
    }
}
