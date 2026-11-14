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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import fr.avantdevoter.app.data.ORIGINE_WIKIPEDIA
import fr.avantdevoter.app.data.Sondage
import fr.avantdevoter.app.data.classement
import fr.avantdevoter.app.data.courbe
import fr.avantdevoter.app.data.dateCourte
import fr.avantdevoter.app.data.duels
import fr.avantdevoter.app.data.instituts
import fr.avantdevoter.app.data.personnes
import fr.avantdevoter.app.data.pourcent
import fr.avantdevoter.app.data.terrain
import fr.avantdevoter.app.data.titreSondage

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

/** Onglet Sondages : 1er tour (moyenne 30 jours puis tous les sondages) ou 2nd tour (duels puis sondages). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SondagesScreen(vm: MainViewModel, cat: Catalogue, onSondage: (String, String?) -> Unit, onCandidat: (String) -> Unit) {
    val enCours by vm.enCours.collectAsState()
    var secondTour by rememberSaveable { mutableStateOf(false) }
    var institut by rememberSaveable { mutableStateOf<String?>(null) }
    var moyenneOuverte by rememberSaveable { mutableStateOf(false) } // la liste des sondages d'abord
    val tous = if (secondTour) cat.secondTour else cat.sondages
    val liste = remember(tous, institut) { tous.filter { institut == null || it.institut == institut } }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            Text("Sondages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                listOf(false to "1er tour", true to "2nd tour").forEachIndexed { i, (tour2, libelle) ->
                    SegmentedButton(
                        selected = secondTour == tour2,
                        onClick = {
                            secondTour = tour2
                            val autres = if (tour2) cat.secondTour else cat.sondages
                            if (autres.none { it.institut == institut }) institut = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(libelle) }
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = institut == null, onClick = { institut = null }, label = { Text("Tous les instituts") }) }
                items(instituts(tous)) { i ->
                    FilterChip(selected = institut == i, onClick = { institut = if (institut == i) null else i }, label = { Text(i) })
                }
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
        PullToRefreshBox(isRefreshing = enCours, onRefresh = { vm.mettreAJour() }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                if (secondTour) {
                    duelsDuSecondTour(vm, cat, liste, institut, onSondage, onCandidat)
                } else {
                    item { EnTetePremierTour(vm, cat, liste, institut, moyenneOuverte, onSondage, onCandidat) }
                }
                item {
                    HorizontalDivider()
                    Text(MENTION, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "${liste.size} sondage${if (liste.size > 1) "s" else ""}${if (secondTour) " du 2nd tour" else ""}, du plus récent au plus ancien",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                if (liste.isEmpty()) {
                    item { EtatVide(if (institut != null) "Aucun sondage pour cet institut." else "Aucun sondage pour l'instant.") }
                }
                items(liste, key = { it.id }) { s ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClickLabel = "Voir le sondage") { onSondage(s.id, institut) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(titreSondage(s), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val nb = s.hypotheses.size
                        Text(
                            "${terrain(s)} · ${personnes(s.echantillon)} · " +
                                (if (secondTour) "$nb duel${if (nb > 1) "s" else ""}" else "$nb hypothèse${if (nb > 1) "s" else ""}") +
                                if (s.origine == ORIGINE_WIKIPEDIA) " · via Wikipédia" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
                item {
                    SourcesSondages(cat, secondTour)
                    PiedDePage(cat.source)
                }
            }
        }
    }
}

/** 1er tour : derniers scores de l'institut choisi, sinon la moyenne des 30 derniers jours. */
@Composable
private fun EnTetePremierTour(
    vm: MainViewModel,
    cat: Catalogue,
    liste: List<Sondage>,
    institut: String?,
    moyenneOuverte: Boolean,
    onSondage: (String, String?) -> Unit,
    onCandidat: (String) -> Unit,
) {
    val m = cat.moyenne
    val dernier = if (institut != null) liste.firstOrNull() else null
    if (dernier != null) {
        // Un institut est choisi : ses derniers scores remplacent la moyenne de tous les instituts.
        val h = dernier.hypotheses.firstOrNull()
        Text("Dernier sondage $institut", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            listOf(dernier.commanditaire, terrain(dernier)).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (h != null) {
            Text(
                h.label.ifBlank { "Hypothèse 1" } + if (dernier.hypotheses.size > 1) " (1re sur ${dernier.hypotheses.size})" else "",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            val rangs = classement(h)
            val max = rangs.maxOfOrNull { it.second } ?: 0.0
            rangs.forEach { (id, score) -> LigneScore(vm, cat, id, score, max) { onCandidat(id) } }
        }
        TextButton(onClick = { onSondage(dernier.id, institut) }) {
            Text(if (dernier.hypotheses.size > 1) "Voir les ${dernier.hypotheses.size} hypothèses" else "Voir le détail")
        }
    } else SectionRepliable(
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
}

/** 2nd tour : un bloc par duel, avec son dernier résultat et, déplié, tous ses résultats. */
private fun LazyListScope.duelsDuSecondTour(
    vm: MainViewModel,
    cat: Catalogue,
    liste: List<Sondage>,
    institut: String?,
    onSondage: (String, String?) -> Unit,
    onCandidat: (String) -> Unit,
) {
    val tous = duels(liste)
    item {
        Text("Duels testés", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Le plus récemment testé d'abord. Touchez un duel pour voir tous ses résultats.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (tous.isEmpty()) EtatVide("Aucun duel du 2nd tour pour l'instant.")
    }
    items(tous, key = { "duel-${it.label}" }) { d ->
        val (dernier, h) = d.resultats.first()
        SectionRepliable(
            d.label,
            "duel-${d.label}",
            sousTitre = "Dernier : ${dernier.institut}, ${dateCourte(dernier.terrainFin)} · ${d.resultats.size} sondage${if (d.resultats.size > 1) "s" else ""}",
        ) {
            d.resultats.forEach { (s, hyp) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Voir le sondage") { onSondage(s.id, institut) }
                        .padding(vertical = 6.dp),
                ) {
                    Text("${s.institut} · ${dateCourte(s.terrainFin)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(hyp.scores.values.joinToString(" – ") { pourcent(it) }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        // Dernier résultat, toujours visible sous le titre du duel.
        h.scores.forEach { (id, score) -> LigneScore(vm, cat, id, score, 100.0) { onCandidat(id) } }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

/** D'où viennent les sondages : 2027 et moi, complété par Wikipédia (licence et lien vers la page). */
@Composable
private fun SourcesSondages(cat: Catalogue, secondTour: Boolean) {
    val ouvrir = rememberOuvrir()
    val wikipedia = cat.sourcesSondages.firstOrNull { it.nom == ORIGINE_WIKIPEDIA }
    Text(
        if (secondTour) "Intentions de vote au 2nd tour, reprises de Wikipédia (2027 et moi ne publie pas le 2nd tour). Chaque sondage renvoie à sa notice ou au rapport de l'institut."
        else "Intentions de vote au 1er tour, de 2027 et moi ; « via Wikipédia » : sondage que 2027 et moi n'a pas encore repris, lu sur Wikipédia avec le lien vers sa notice.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (wikipedia != null) {
        TextButton(onClick = { ouvrir(wikipedia.url) }, contentPadding = PaddingValues(0.dp)) {
            Text("Wikipédia : « ${wikipedia.titre} » (${wikipedia.licence})", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Fiche d'un sondage (1er ou 2nd tour) ; glisser à gauche / à droite passe au sondage suivant / précédent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FicheSondageScreen(vm: MainViewModel, cat: Catalogue, id: String, institut: String?, onRetour: () -> Unit, onCandidat: (String) -> Unit) {
    val secondTour = cat.secondTour.any { it.id == id }
    val tous = if (secondTour) cat.secondTour else cat.sondages
    val liste = remember(tous, institut) { tous.filter { institut == null || it.institut == institut } }
    val depart = liste.indexOfFirst { it.id == id }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = depart) { liste.size }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text("Sondage${if (secondTour) " du 2nd tour" else ""} ${if (liste.isEmpty()) "" else "${pager.currentPage + 1} sur ${liste.size}"}")
            },
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
    val wikipedia = s.origine == ORIGINE_WIKIPEDIA
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        item {
            Text(s.institut, style = MaterialTheme.typography.headlineSmall)
            if (s.commanditaire.isNotBlank()) Text("Pour ${s.commanditaire}", style = MaterialTheme.typography.bodyLarge)
            Text("${terrain(s)} · ${personnes(s.echantillon)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { ouvrir(s.source?.url.orEmpty()) }, enabled = !s.source?.url.isNullOrBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (wikipedia) "Voir la notice ou le rapport" else "Voir la notice officielle")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
            }
            Text(
                (if (wikipedia) "Sondage repris de la page Wikipédia des sondages 2027 (CC BY-SA 4.0) ; le lien est celui que cite Wikipédia. "
                else "Notice de la Commission des sondages${s.source?.date?.ifBlank { null }?.let { " du ${dateCourte(it)}" } ?: ""}. ") +
                    "Scores triés du plus haut au plus bas. Touchez un nom pour voir ses scores dans tous les sondages du 1er tour.",
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
