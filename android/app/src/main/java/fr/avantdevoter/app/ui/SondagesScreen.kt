package fr.avantdevoter.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Sondage
import fr.avantdevoter.app.data.classement
import fr.avantdevoter.app.data.courbe
import fr.avantdevoter.app.data.dateCourte
import fr.avantdevoter.app.data.instituts
import fr.avantdevoter.app.data.personnes
import fr.avantdevoter.app.data.pourcent
import fr.avantdevoter.app.data.terrain

private const val MENTION = "Un sondage est une estimation avec une marge d'erreur, pas une prédiction."

/** Petite photo ou initiales, y compris pour une personnalité citée dans un sondage sans fiche candidat. */
@Composable
private fun PhotoOuInitiales(vm: MainViewModel, cat: Catalogue, id: String, taille: androidx.compose.ui.unit.Dp) {
    val c = cat.parId[id] ?: Candidat(id = id, tri = id, nom = cat.nomDe(id), statut = "")
    PhotoCandidat(c, vm.repository.photo(c), taille)
}

/** Ligne « photo, nom, barre, score » ; même couleur pour tous. */
@Composable
private fun LigneScore(vm: MainViewModel, cat: Catalogue, id: String, score: Double, max: Double, suffixe: String? = null, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Voir ses scores dans tous les sondages", onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoOuInitiales(vm, cat, id, 32.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat.nomDe(id), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(pourcent(score), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (suffixe != null) {
                    Text(" $suffixe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val part = if (max > 0) (score / max).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(part).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

/** Onglet Sondages : moyenne 30 jours en résumé, puis tous les sondages du plus récent au plus ancien. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SondagesScreen(vm: MainViewModel, cat: Catalogue, onSondage: (String, String?) -> Unit, onCandidat: (String) -> Unit) {
    val enCours by vm.enCours.collectAsState()
    var institut by rememberSaveable { mutableStateOf<String?>(null) }
    var moyenneOuverte by rememberSaveable { mutableStateOf(false) } // la liste des sondages d'abord
    val liste = remember(cat.sondages, institut) { cat.sondages.filter { institut == null || it.institut == institut } }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            Text("Sondages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = institut == null, onClick = { institut = null }, label = { Text("Tous les instituts") }) }
                items(instituts(cat.sondages)) { i ->
                    FilterChip(selected = institut == i, onClick = { institut = if (institut == i) null else i }, label = { Text(i) })
                }
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
        PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                val m = cat.moyenne
                item {
                    SectionRepliable(
                        "Moyenne des 30 derniers jours",
                        "moyenne",
                        ouverteAuDepart = moyenneOuverte,
                        sousTitre = if (m == null || m.nbSondages == 0) "Aucun sondage ces 30 derniers jours"
                        else "${m.nbSondages} sondage${if (m.nbSondages > 1) "s" else ""} · trié par score · (n) = nombre de sondages",
                    ) {
                        if (m != null && m.scores.isNotEmpty()) {
                            val max = m.scores.maxOf { it.score }
                            m.scores.forEach { s ->
                                LigneScore(vm, cat, s.id, s.score, max, "(${s.nbSondages})") { onCandidat(s.id) }
                            }
                            Text(
                                "Règle de calcul : ${m.regle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(MENTION, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "${liste.size} sondage${if (liste.size > 1) "s" else ""}, du plus récent au plus ancien",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                if (liste.isEmpty()) item { EtatVide("Aucun sondage pour cet institut.") }
                items(liste, key = { it.id }) { s ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClickLabel = "Voir le sondage") { onSondage(s.id, institut) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text("${s.institut} · ${s.commanditaire}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${terrain(s)} · ${personnes(s.echantillon)} · ${s.hypotheses.size} hypothèse${if (s.hypotheses.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
                item {
                    Text(
                        "Intentions de vote au 1er tour. Le 2nd tour s'affichera si 2027 et moi l'ajoute.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    PiedDePage(cat.source)
                }
            }
        }
    }
}

/** Fiche d'un sondage ; glisser à gauche / à droite passe au sondage suivant / précédent de la liste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FicheSondageScreen(vm: MainViewModel, cat: Catalogue, id: String, institut: String?, onRetour: () -> Unit, onCandidat: (String) -> Unit) {
    val liste = remember(cat.sondages, institut) { cat.sondages.filter { institut == null || it.institut == institut } }
    val depart = liste.indexOfFirst { it.id == id }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = depart) { liste.size }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sondage ${if (liste.isEmpty()) "" else "${pager.currentPage + 1} sur ${liste.size}"}") },
            navigationIcon = { IconButton(onClick = onRetour) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour") } },
        )
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { liste[it].id }) { page ->
            FicheSondage(vm, cat, liste[page], onCandidat)
        }
    }
}

@Composable
private fun FicheSondage(vm: MainViewModel, cat: Catalogue, s: Sondage, onCandidat: (String) -> Unit) {
    val ouvrir = rememberOuvrir()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        item {
            Text(s.institut, style = MaterialTheme.typography.headlineSmall)
            Text("Pour ${s.commanditaire}", style = MaterialTheme.typography.bodyLarge)
            Text("${terrain(s)} · ${personnes(s.echantillon)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { ouvrir(s.source?.url.orEmpty()) }, enabled = !s.source?.url.isNullOrBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Voir la notice officielle")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
            }
            Text(
                "Notice de la Commission des sondages${s.source?.date?.ifBlank { null }?.let { " du ${dateCourte(it)}" } ?: ""}. " +
                    "Scores triés du plus haut au plus bas. Touchez un nom pour voir ses scores dans tous les sondages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(MENTION, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        s.hypotheses.forEachIndexed { i, h ->
            item(key = "h$i") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(h.label.ifBlank { "Hypothèse ${i + 1}" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                val rangs = classement(h)
                val max = rangs.maxOfOrNull { it.second } ?: 0.0
                rangs.forEach { (id, score) -> LigneScore(vm, cat, id, score, max) { onCandidat(id) } }
            }
        }
        item { PiedDePage(cat.source) }
    }
}

/** Scores d'une personnalité dans tous les sondages : courbe puis liste détaillée. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresCandidatScreen(vm: MainViewModel, cat: Catalogue, id: String, onRetour: () -> Unit, onSondage: (String) -> Unit) {
    val points = remember(cat.sondages, id) { courbe(cat.sondages, id) }
    val couleur = MaterialTheme.colorScheme.primary
    val grille = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(cat.nomDe(id)) },
            navigationIcon = { IconButton(onClick = onRetour) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour") } },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhotoOuInitiales(vm, cat, id, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(cat.nomDe(id), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Testé dans ${points.size} sondage${if (points.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (points.isEmpty()) {
                    EtatVide("Aucun sondage ne teste cette personnalité.")
                } else {
                    TitreSection("Évolution")
                    val hautEchelle = (points.maxOf { it.max } * 1.15).coerceAtLeast(5.0)
                    Row {
                        Column(Modifier.height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(pourcent(hautEchelle), style = MaterialTheme.typography.labelSmall)
                            Text("0 %", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(6.dp))
                        Canvas(
                            Modifier
                                .weight(1f)
                                .height(180.dp)
                                .semantics { contentDescription = "Courbe des scores de ${cat.nomDe(id)}, du plus ancien au plus récent sondage" },
                        ) {
                            val n = points.size
                            fun x(i: Int) = if (n == 1) size.width / 2 else size.width * i / (n - 1)
                            fun y(v: Double) = (size.height * (1 - v / hautEchelle)).toFloat()
                            drawLine(grille, Offset(0f, size.height), Offset(size.width, size.height), 2f)
                            drawLine(grille, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                            for (i in 0 until n - 1) {
                                drawLine(couleur, Offset(x(i), y(points[i].moyenne)), Offset(x(i + 1), y(points[i + 1].moyenne)), 4f)
                            }
                            points.forEachIndexed { i, p ->
                                // trait vertical : du plus bas au plus haut des hypothèses de ce sondage
                                if (p.max > p.min) drawLine(couleur.copy(alpha = 0.4f), Offset(x(i), y(p.min)), Offset(x(i), y(p.max)), 6f)
                                drawCircle(couleur, 7f, Offset(x(i), y(p.moyenne)))
                                drawCircle(couleur, 7f, Offset(x(i), y(p.moyenne)), style = Stroke(2f))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 4.dp)) {
                        Text(dateCourte(points.first().sondage.terrainFin), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text(dateCourte(points.last().sondage.terrainFin), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "Un point par sondage : moyenne des hypothèses où la personnalité figure. Le trait vertical va du score le plus bas au plus haut selon les hypothèses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(MENTION, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    TitreSection("Détail, du plus récent au plus ancien")
                }
            }
            items(points.reversed(), key = { it.sondage.id }) { p ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Voir le sondage") { onSondage(p.sondage.id) }
                        .padding(vertical = 8.dp),
                ) {
                    Row {
                        Text("${p.sondage.institut} · ${dateCourte(p.sondage.terrainFin)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(pourcent(p.moyenne), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    p.sondage.hypotheses.filter { id in it.scores }.forEach { h ->
                        Text(
                            "${h.label.ifBlank { "Hypothèse" }} : ${pourcent(h.scores.getValue(id))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
            item { PiedDePage(cat.source) }
        }
    }
}
