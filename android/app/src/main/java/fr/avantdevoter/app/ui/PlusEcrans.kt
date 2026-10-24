package fr.avantdevoter.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Mesure
import fr.avantdevoter.app.data.QuestionQuiz
import fr.avantdevoter.app.data.compteARebours
import fr.avantdevoter.app.data.dateLongue
import fr.avantdevoter.app.data.filtrer
import fr.avantdevoter.app.data.libelleStatutEtape
import fr.avantdevoter.app.data.premierTour
import fr.avantdevoter.app.data.questionQuiz
import fr.avantdevoter.app.data.rechercher
import fr.avantdevoter.app.data.texteDePartage
import fr.avantdevoter.app.data.tirage
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranSimple(titre: String, onRetour: () -> Unit, contenu: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(titre) },
            navigationIcon = { IconButton(onClick = onRetour) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour") } },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), content = contenu)
    }
}

private fun aujourdHui(): LocalDate = LocalDate.now(ZoneId.of("Europe/Paris"))

// ---------------------------------------------------------------- calendrier

@Composable
fun CalendrierScreen(cat: Catalogue, onRetour: () -> Unit) {
    val jour = aujourdHui()
    val rebours = compteARebours(premierTour(cat.calendrier), jour)
    EcranSimple("Calendrier", onRetour) {
        item {
            if (rebours != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rebours, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        premierTour(cat.calendrier)?.let {
                            Text("Le ${dateLongue(it.toString())}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            Text(
                "Étapes datées par 2027 et moi, avec leur statut et leur source. Les étapes passées sont grisées.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        if (cat.calendrier.isEmpty()) item { EtatVide("Aucune étape publiée pour l'instant.") }
        items(cat.calendrier) { e ->
            val passee = runCatching { LocalDate.parse(e.fin.ifBlank { e.date }) < jour }.getOrDefault(false)
            val couleur = if (passee) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    if (e.fin.isNotBlank() && e.fin != e.date) "Du ${dateLongue(e.date)} au ${dateLongue(e.fin)}" else dateLongue(e.date).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = couleur,
                )
                Text(e.titre, style = MaterialTheme.typography.bodyMedium, color = couleur)
                Text(
                    libelleStatutEtape(e.statut) + if (passee) " · passée" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                e.source?.let { LienSource(it) }
            }
            HorizontalDivider()
        }
        item { PiedDePage(cat.source) }
    }
}

// ---------------------------------------------------------------- Mes priorités

/**
 * « Mes priorités » à l'aveugle : des mesures sans nom, on coche celles qui plaisent, puis on découvre qui les porte.
 * Rien n'est enregistré ; aucun total par candidat (ce serait un classement).
 */
@Composable
fun PrioritesScreen(vm: MainViewModel, cat: Catalogue, onRetour: () -> Unit) {
    var domaine by rememberSaveable { mutableStateOf(cat.themes.firstOrNull()?.id ?: "economie") }
    var graine by rememberSaveable { mutableIntStateOf((0..Int.MAX_VALUE / 2).random()) }
    var revele by rememberSaveable { mutableStateOf(false) }
    var coches by rememberSaveable { mutableStateOf(setOf<String>()) }
    val declares = remember(cat) { cat.candidats.filter { it.statut == "declare" } }
    val mesures = remember(domaine, graine, declares) { tirage(declares, domaine, 8, kotlin.random.Random(graine)) }
    val label = cat.themes.firstOrNull { it.id == domaine }?.label.orEmpty()

    fun nouveauTirage() { graine += 1; revele = false; coches = emptySet() }

    EcranSimple("Mes priorités", onRetour) {
        item {
            Text(
                "Choisissez un domaine. Voici des mesures de candidats déclarés, sans leur nom et dans un ordre tiré au hasard. " +
                    "Cochez celles qui vous parlent, puis découvrez qui les propose. Rien n'est enregistré, rien n'est recommandé.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cat.themes, key = { it.id }) { t ->
                    FilterChip(selected = t.id == domaine, onClick = { domaine = t.id; nouveauTirage() }, label = { Text(t.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (mesures.isEmpty()) EtatVide("Aucune mesure publiée sur ce domaine pour l'instant.")
        }
        itemsIndexed(mesures, key = { _, p -> "${p.first.id}-${p.second.id}" }) { i, (candidat, mesure) ->
            val coche = mesure.id in coches
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = if (coche) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(
                    Modifier
                        .clickable(enabled = !revele, onClickLabel = if (coche) "Décocher" else "Cocher") {
                            coches = if (coche) coches - mesure.id else coches + mesure.id
                        }
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(if (coche) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Mesure ${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(mesure.texte, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (revele) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PhotoCandidat(candidat, vm.repository.photo(candidat), 36.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(candidat.nom, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (candidat.parti.isNotBlank()) Text(candidat.parti, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        LienSource(mesure.source)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            if (!revele && mesures.isNotEmpty()) {
                Button(onClick = { revele = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(when (coches.size) { 0 -> "Révéler qui les propose"; 1 -> "Révéler qui propose mon choix"; else -> "Révéler qui propose mes ${coches.size} choix" })
                }
            }
            OutlinedButton(onClick = { nouveauTirage() }, modifier = Modifier.fillMaxWidth()) { Text("Nouveau tirage ($label)") }
            PiedDePage(cat.source)
        }
    }
}

// ---------------------------------------------------------------- quiz

/** Quiz « Qui propose quoi ? » : une mesure, quatre candidats, deviner l'auteur. Série de 10, rien n'est enregistré. */
@Composable
fun QuizScreen(vm: MainViewModel, cat: Catalogue, onRetour: () -> Unit) {
    val declares = remember(cat) { cat.candidats.filter { it.statut == "declare" } }
    var numero by rememberSaveable { mutableIntStateOf(1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var choix by rememberSaveable { mutableStateOf<String?>(null) }
    var graine by rememberSaveable { mutableIntStateOf((0..Int.MAX_VALUE / 2).random()) }
    var dejaPosees by rememberSaveable { mutableStateOf(setOf<String>()) }
    val q: QuestionQuiz? = remember(graine, numero) { questionQuiz(declares, kotlin.random.Random(graine + numero), dejaPosees) }
    val fini = numero > 10

    EcranSimple("Qui propose quoi ?", onRetour) {
        if (fini) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Série terminée", style = MaterialTheme.typography.headlineSmall)
                    Text("$score bonne${if (score > 1) "s" else ""} réponse${if (score > 1) "s" else ""} sur 10", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                    Button(onClick = { numero = 1; score = 0; choix = null; dejaPosees = emptySet(); graine += 100 }) { Text("Nouvelle série") }
                }
            }
            return@EcranSimple
        }
        if (q == null) {
            item { EtatVide("Pas assez de mesures publiées pour jouer.") }
            return@EcranSimple
        }
        item {
            Text("Question $numero sur 10 · score : $score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(cat.themes.firstOrNull { it.id == q.mesure.theme }?.label.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("« ${q.mesure.texte} »", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text("Qui propose cette mesure ?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 12.dp))
        }
        items(q.choix, key = { it.id }) { c ->
            ChoixQuiz(vm, c, choix, q.candidat.id) {
                if (choix == null) {
                    choix = c.id
                    if (c.id == q.candidat.id) score += 1
                }
            }
        }
        item {
            if (choix != null) {
                Text(
                    if (choix == q.candidat.id) "Bonne réponse : ${q.candidat.nom}." else "C'était ${q.candidat.nom}.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                LienSource(q.mesure.source)
                Button(
                    onClick = { dejaPosees = dejaPosees + q.mesure.id; numero += 1; choix = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text(if (numero == 10) "Voir mon score" else "Question suivante") }
            }
            PiedDePage(cat.source)
        }
    }
}

@Composable
private fun ChoixQuiz(vm: MainViewModel, c: Candidat, choix: String?, bonne: String, onClick: () -> Unit) {
    val etat = when {
        choix == null -> null
        c.id == bonne -> "Bonne réponse"
        c.id == choix -> "Votre choix"
        else -> null
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (c.id == bonne && choix != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (c.id == choix) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = choix == null, onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoCandidat(c, vm.repository.photo(c), 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.nom, style = MaterialTheme.typography.titleMedium)
                if (c.parti.isNotBlank()) Text(c.parti, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (etat != null) Text(etat, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------- recherche

/** Recherche plein texte dans toutes les mesures (« SMIC », « nucléaire »), sans tenir compte des accents. */
@Composable
fun RechercheScreen(vm: MainViewModel, cat: Catalogue, reglages: fr.avantdevoter.app.data.Reglages, onRetour: () -> Unit, onCandidat: (String) -> Unit) {
    var requete by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val candidats = remember(cat, reglages) { filtrer(cat.candidats, reglages) }
    val resultats = remember(requete, candidats) { rechercher(candidats, requete) }
    val labels = cat.themes.associate { it.id to it.label }

    EcranSimple("Rechercher", onRetour) {
        item {
            OutlinedTextField(
                value = requete,
                onValueChange = { requete = it },
                placeholder = { Text("Ex. : SMIC, nucléaire, retraite à 60 ans") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (requete.isNotEmpty()) IconButton(onClick = { requete = "" }) { Icon(Icons.Outlined.Clear, "Effacer") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Text(
                when {
                    requete.trim().length < 2 -> "Tapez au moins 2 lettres. La recherche porte sur les candidats affichés (filtres Déclarés, Pressentis, Suivis)."
                    resultats.isEmpty() -> "Aucune mesure ne contient « ${requete.trim()} »."
                    else -> "${resultats.size} mesure${if (resultats.size > 1) "s" else ""}, par ordre alphabétique des candidats"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        items(resultats, key = { "${it.candidat.id}-${it.mesure.id}" }) { r ->
            ResultatRecherche(vm, r.candidat, r.mesure, labels[r.mesure.theme].orEmpty(), onCandidat)
        }
    }
}

@Composable
private fun ResultatRecherche(vm: MainViewModel, c: Candidat, m: Mesure, domaine: String, onCandidat: (String) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onCandidat(c.id) }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoCandidat(c, vm.repository.photo(c), 32.dp)
            Spacer(Modifier.width(8.dp))
            Text("${c.nom} · $domaine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        CarteMesure(m, texteDePartage(c, domaine, m)) { vm.message("Texte de la mesure copié") }
    }
}
