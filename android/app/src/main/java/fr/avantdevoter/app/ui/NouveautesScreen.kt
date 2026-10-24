package fr.avantdevoter.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Changement
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.dateCourte
import fr.avantdevoter.app.data.dateLongue

/** Onglet Nouveautés : journal des changements détectés par le robot (90 derniers jours). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NouveautesScreen(
    vm: MainViewModel,
    cat: Catalogue,
    reglages: Reglages,
    onCandidat: (String) -> Unit,
    onSondage: (String) -> Unit,
) {
    val enCours by vm.enCours.collectAsState()
    val liste = remember(cat.changements, reglages.suivisSeulement, reglages.suivis) {
        cat.changements.filter { !reglages.suivisSeulement || it.candidat in reglages.suivis }
    }
    val parJour = liste.groupBy { it.date }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            Text("Nouveautés", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !reglages.suivisSeulement, onClick = { vm.setSuivisSeulement(false) }, label = { Text("Tout") })
                FilterChip(
                    selected = reglages.suivisSeulement,
                    onClick = { vm.setSuivisSeulement(true) },
                    leadingIcon = { Icon(Icons.Outlined.StarOutline, null, Modifier.size(18.dp)) },
                    label = { Text("Mes candidats suivis") },
                )
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
        PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                if (liste.isEmpty()) {
                    item {
                        EtatVide(
                            when {
                                reglages.suivisSeulement && reglages.suivis.isEmpty() ->
                                    "Aucun candidat suivi. Touchez l'étoile sur la fiche d'un candidat pour être prévenu de ses changements."
                                reglages.suivisSeulement -> "Pas de changement pour vos candidats suivis ces 90 derniers jours."
                                else -> "Pas de changement depuis le ${cat.source?.miseAJour?.let(::dateCourte) ?: "—"}. " +
                                    "2027 et moi met ses données à jour à la main, sans rythme fixe : les changements apparaîtront ici " +
                                    "(nouvelles mesures, programmes complétés, candidatures, sondages)."
                            },
                        )
                    }
                }
                parJour.forEach { (jour, changements) ->
                    item(key = "j$jour") { TitreSection(dateLongue(jour).replaceFirstChar { it.uppercase() }) }
                    items(changements, key = { "${it.date}|${it.type}|${it.candidat}|${it.mesure}|${it.sondage}" }) { ch ->
                        LigneChangement(vm, cat, ch) {
                            when {
                                ch.sondage != null -> onSondage(ch.sondage)
                                ch.candidat != null && cat.parId.containsKey(ch.candidat) -> onCandidat(ch.candidat)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                item { PiedDePage(cat.source) }
            }
        }
    }
}

@Composable
private fun LigneChangement(vm: MainViewModel, cat: Catalogue, ch: Changement, onClick: () -> Unit) {
    val candidat: Candidat? = ch.candidat?.let { cat.parId[it] }
    val sondage = ch.sondage?.let { id -> cat.sondages.firstOrNull { it.id == id } }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (candidat != null) PhotoCandidat(candidat, vm.repository.photo(candidat), 40.dp)
        else Icon(Icons.Outlined.Poll, null, Modifier.size(40.dp).padding(8.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                candidat?.nom ?: sondage?.let { "${it.institut} · ${it.commanditaire}" } ?: ch.candidat.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(ch.libelle, style = MaterialTheme.typography.bodyMedium)
            if (!ch.texte.isNullOrBlank()) {
                Text(
                    "« ${ch.texte} »",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
