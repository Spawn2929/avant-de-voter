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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.mesuresDu
import fr.avantdevoter.app.data.texteDePartage
import kotlinx.coroutines.launch

/**
 * Comparateur de 2 à 4 candidats : un domaine à la fois, les candidats l'un sous l'autre
 * (ordre alphabétique). Glisser à gauche / à droite = domaine suivant / précédent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparateurScreen(vm: MainViewModel, cat: Catalogue, ids: List<String>, onRetour: () -> Unit, onCandidat: (String) -> Unit) {
    val candidats = cat.candidats.filter { it.id in ids } // ordre alphabétique du catalogue
    val themes = cat.themes
    val pager = rememberPagerState { themes.size }
    val puces = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { puces.animateScrollToItem(maxOf(0, pager.currentPage - 1)) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Comparer ${candidats.size} candidats") },
            navigationIcon = { IconButton(onClick = onRetour) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour") } },
        )
        LazyRow(state = puces, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(themes, key = { _, t -> t.id }) { i, t ->
                FilterChip(selected = pager.currentPage == i, onClick = { scope.launch { pager.animateScrollToPage(i) } }, label = { Text(t.label) })
            }
        }
        Text(
            candidats.joinToString(" · ") { it.nom },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        HorizontalDivider()
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val theme = themes[page]
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                item { TitreSection(theme.label) }
                candidats.forEach { c ->
                    item(key = "${theme.id}-${c.id}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClickLabel = "Voir la fiche") { onCandidat(c.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PhotoCandidat(c, vm.repository.photo(c), 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(c.nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        val mesures = mesuresDu(c, theme.id)
                        if (mesures.isEmpty()) AucuneProposition(c)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mesures.forEach { m -> CarteMesure(m, texteDePartage(c, theme.label, m)) { vm.message("Texte de la mesure copié") } }
                        }
                        HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                }
                item { PiedDePage(cat.source) }
            }
        }
    }
}
