package fr.avantdevoter.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.avantdevoter.app.BuildConfig
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.ThemeChoisi
import fr.avantdevoter.app.data.dateCourte
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Onglet Plus : accès aux Réglages et aux Crédits (le calendrier, le quiz et « Mes priorités » viendront ici). */
@Composable
fun PlusScreen(catalogue: Catalogue, onReglages: () -> Unit, onCredits: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Plus",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            )
        }
        item { LigneMenu(Icons.Outlined.Settings, "Réglages", "Thème, affichage, mise à jour des données", onReglages) }
        item { LigneMenu(Icons.Outlined.Info, "Crédits et sources", "2027 et moi, licences des photos", onCredits) }
        item { PiedDePage(catalogue.source) }
    }
}

@Composable
private fun LigneMenu(icone: ImageVector, titre: String, detail: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(titre) },
        supportingContent = { Text(detail) },
        leadingContent = { Icon(icone, null) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EcranAvecRetour(titre: String, onRetour: () -> Unit, contenu: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(titre) },
            navigationIcon = { IconButton(onClick = onRetour) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour") } },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp), content = contenu)
    }
}

/** « aujourd'hui à 8 h 05 », « hier à 7 h 15 », « le 20/10/2026 à 7 h 15 ». */
fun quand(ms: Long, aujourdHui: LocalDate = LocalDate.now(ZoneId.of("Europe/Paris"))): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.of("Europe/Paris"))
    val heure = "${t.hour} h ${"%02d".format(t.minute)}"
    val jour = when (t.toLocalDate()) {
        aujourdHui -> "aujourd'hui"
        aujourdHui.minusDays(1) -> "hier"
        else -> "le ${dateCourte(t.toLocalDate().toString())}"
    }
    return "$jour à $heure"
}

@Composable
fun ReglagesScreen(vm: MainViewModel, catalogue: Catalogue, reglages: Reglages, onRetour: () -> Unit) {
    val enCours by vm.enCours.collectAsState()
    val verifie by vm.derniereVerification.collectAsState()
    val nouvelle by vm.nouvelleVersion.collectAsState()
    val tropAncienne by vm.appliTropAncienne.collectAsState()
    val ouvrir = rememberOuvrir()
    LaunchedEffect(Unit) { vm.verifierVersion() }

    EcranAvecRetour("Réglages", onRetour) {
        item { TitreSection("Thème") }
        items(ThemeChoisi.entries) { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = reglages.theme == t, role = Role.RadioButton) { vm.setTheme(t) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = reglages.theme == t, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(t.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 12.dp))
            TitreSection("Affichage")
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = reglages.pressentis, role = Role.Switch) { vm.setFiltres(reglages.declares, it) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Afficher les candidatures pressenties", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "En plus des candidatures déclarées, dans les onglets Domaines et Candidats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = reglages.pressentis, onCheckedChange = null)
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 12.dp))
            TitreSection("Données")
            Text(
                "Données de 2027 et moi du ${catalogue.source?.miseAJour?.let(::dateCourte) ?: "—"}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Dernière vérification : " + (verifie?.let { quand(it) } ?: "pas encore faite"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "L'appli vérifie chaque matin après 7 h. 2027 et moi met ses données à jour à la main, sans rythme fixe : " +
                    "il est normal que la date ne change pas tous les jours.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.mettreAJour() }, enabled = !enCours, modifier = Modifier.fillMaxWidth()) {
                Text(if (enCours) "Mise à jour en cours…" else "Mettre à jour maintenant")
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 16.dp))
            TitreSection("Application")
            Text("Avant de voter, version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
            val n = nouvelle
            if (n != null || tropAncienne) {
                Text(
                    if (n != null) "Nouvelle version ${n.version} disponible." else "Une nouvelle version est nécessaire pour les dernières données.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = { ouvrir(n?.url ?: "https://github.com/${BuildConfig.GITHUB_REPO}/releases/latest") }) {
                    Text("Télécharger la nouvelle version")
                }
            } else {
                Text(
                    "C'est la dernière version (vérifié en ligne quand le réseau est disponible).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CreditsScreen(vm: MainViewModel, catalogue: Catalogue, onRetour: () -> Unit) {
    val ouvrir = rememberOuvrir()
    val avecPhoto = catalogue.candidats.filter { it.photo != null && vm.repository.photo(it) != null }
    EcranAvecRetour("Crédits et sources", onRetour) {
        item {
            TitreSection("Données")
            Text(
                "Toutes les informations affichées (candidats, mesures, sondages, calendrier) proviennent de « 2027 et moi », " +
                    "publiées sous Licence Ouverte 2.0. Les textes sont repris tels quels : l'appli ne résume, ne reformule " +
                    "et n'évalue rien.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Données mises à jour le ${catalogue.source?.miseAJour?.let(::dateCourte) ?: "—"}.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = { ouvrir("https://2027etmoi.fr") }) { Text("Site 2027etmoi.fr") }
            TextButton(onClick = { ouvrir("https://github.com/2027etmoi/2027etmoi") }) { Text("Données ouvertes de 2027 et moi (GitHub)") }
            TextButton(onClick = { ouvrir("https://www.etalab.gouv.fr/licence-ouverte-open-licence/") }) { Text("Licence Ouverte 2.0") }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            TitreSection("Photos")
            Text(
                "Les photos viennent de Wikimedia Commons et ne sont pas sous Licence Ouverte : chacune garde sa licence et son auteur. " +
                    "Touchez une ligne pour voir la page d'origine.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(avecPhoto, key = { it.id }) { c ->
            val p = c.photo!!
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = p.page.isNotBlank()) { ouvrir(p.page) }
                    .padding(vertical = 8.dp),
            ) {
                Text(c.nom, style = MaterialTheme.typography.bodyLarge)
                Text("${p.auteur} · ${p.licence}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            TitreSection("L'appli")
            Text(
                "Appli personnelle, sans compte, sans publicité, sans statistiques d'usage. Elle n'est liée à aucun candidat, " +
                    "parti ou média, ni à 2027 et moi. Les candidats sont classés par ordre alphabétique, avec la même présentation pour tous.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PiedDePage(catalogue.source)
        }
    }
}
