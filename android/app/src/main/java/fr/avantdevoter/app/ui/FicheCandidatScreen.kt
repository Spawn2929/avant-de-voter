package fr.avantdevoter.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.avantdevoter.app.data.Affaire
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.Entree
import fr.avantdevoter.app.data.dateCourte
import fr.avantdevoter.app.data.libelleEtatAffaire
import fr.avantdevoter.app.data.libelleStatut
import fr.avantdevoter.app.data.mesuresDu
import fr.avantdevoter.app.data.Ref
import fr.avantdevoter.app.data.libelleChambre
import fr.avantdevoter.app.data.libellePosition
import fr.avantdevoter.app.data.texteDePartage
import fr.avantdevoter.app.data.votesDe
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/** Fiche complète d'un candidat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FicheCandidatScreen(
    vm: MainViewModel,
    catalogue: Catalogue,
    candidat: Candidat,
    suivi: Boolean,
    onRetour: () -> Unit,
    onPhoto: () -> Unit,
    onSondages: () -> Unit,
) {
    val ouvrir = rememberOuvrir()
    val photo = vm.repository.photo(candidat)
    val votes = remember(catalogue.scrutins, candidat.id) { votesDe(catalogue.scrutins, candidat.id) }
    val mandats = catalogue.mandats[candidat.id].orEmpty()
    val testeDansSondages = remember(catalogue.sondages, candidat.id) {
        catalogue.sondages.any { s -> s.hypotheses.any { candidat.id in it.scores } }
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(candidat.nom) },
            navigationIcon = { IconButton(onClick = onRetour) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour") } },
            actions = {
                IconButton(onClick = { vm.basculerSuivi(candidat.id, candidat.nom) }) {
                    Icon(
                        if (suivi) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                        if (suivi) "Ne plus suivre" else "Suivre ce candidat",
                    )
                }
            },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            // En-tête : photo, crédit, parti, statut
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .clickable(enabled = photo != null, onClickLabel = "Agrandir la photo") { onPhoto() },
                    ) {
                        PhotoCandidat(candidat, photo, 120.dp)
                    }
                    candidat.photo?.takeIf { photo != null }?.let { p ->
                        Text(
                            "Photo : ${p.auteur} · ${p.licence}" +
                                if (p.origine != "2027 et moi") "\n(${p.origine})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (candidat.photo == null) {
                        // Pas de photo publiée : on peut en choisir une dans la galerie, gardée sur ce téléphone.
                        val choisir = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                            if (uri != null) vm.ajouterPhotoPerso(candidat.id, uri)
                        }
                        val image = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        if (photo != null) {
                            Text(
                                "Photo ajoutée sur ce téléphone (non publiée)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Row {
                                TextButton(onClick = { choisir.launch(image) }) { Text("Changer") }
                                TextButton(onClick = { vm.retirerPhotoPerso(candidat.id) }) { Text("Retirer") }
                            }
                        } else {
                            Text(
                                "Pas de photo libre publiée par 2027 et moi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            TextButton(onClick = { choisir.launch(image) }) { Text("Ajouter une photo (sur ce téléphone)") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(candidat.nom, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (candidat.parti.isNotBlank()) {
                        Text(candidat.parti, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(4.dp))
                    PastilleEtat(candidat)
                    if (testeDansSondages) {
                        TextButton(onClick = onSondages) { Text("Voir ses scores dans les sondages") }
                    }
                }
            }
            item {
                TitreSection(libelleStatut(candidat.statut))
                if (candidat.statutDetail.isNotBlank()) Text(candidat.statutDetail, style = MaterialTheme.typography.bodyMedium)
                candidat.statutSource?.let { LienSource(it) }
            }
            if (candidat.propositionsPhares.isNotEmpty()) {
                item {
                    TitreSection("Propositions phares")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidat.propositionsPhares.forEach { Puce(it) }
                    }
                }
            }
            if (candidat.presentation.isNotBlank()) {
                item {
                    TitreSection("Présentation")
                    Text(candidat.presentation, style = MaterialTheme.typography.bodyMedium)
                    candidat.presentationSources.forEach { LienSource(it) }
                }
            }
            item {
                HorizontalDivider(Modifier.padding(top = 12.dp))
                if (candidat.parcoursPolitique.isNotEmpty()) {
                    SectionRepliable("Parcours politique", "pp-${candidat.id}") { Parcours(candidat.parcoursPolitique) }
                }
                if (candidat.parcoursProfessionnel.isNotEmpty()) {
                    SectionRepliable("Parcours professionnel", "pro-${candidat.id}") { Parcours(candidat.parcoursProfessionnel) }
                }
                SectionRepliable(
                    "Affaires (selon 2027 et moi)",
                    "aff-${candidat.id}",
                    sousTitre = when (candidat.affaires.size) {
                        0 -> "Aucune affaire recensée"
                        1 -> "1 affaire recensée"
                        else -> "${candidat.affaires.size} affaires recensées"
                    },
                ) {
                    if (candidat.affaires.isEmpty()) {
                        Text(
                            "Aucune affaire recensée par 2027 et moi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    candidat.affaires.forEach { BlocAffaire(it) }
                }
                SectionRepliable(
                    "Votes au Parlement (selon 2027 et moi)",
                    "votes-${candidat.id}",
                    sousTitre = when (votes.size) {
                        0 -> "Aucun vote recensé"
                        1 -> "1 vote recensé"
                        else -> "${votes.size} votes recensés"
                    },
                ) {
                    if (mandats.isNotEmpty()) {
                        Text("Mandats", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                        mandats.forEach { m ->
                            val periode = when {
                                m.debut.isBlank() -> ""
                                m.fin.isBlank() -> " (depuis le ${dateCourte(m.debut)})"
                                else -> " (${dateCourte(m.debut)} – ${dateCourte(m.fin)})"
                            }
                            Text("${libelleChambre(m.chambre)} : ${m.detail}$periode", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (votes.isEmpty()) {
                        Text(
                            "Aucun vote au Parlement recensé par 2027 et moi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    votes.forEach { (scrutin, position) ->
                        Column(Modifier.padding(bottom = 10.dp)) {
                            Text(
                                "${dateCourte(scrutin.date)} · ${libelleChambre(scrutin.chambre)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(scrutin.titre, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Vote : ${libellePosition(position)}" + (if (scrutin.resultat.isNotBlank()) " · texte ${scrutin.resultat}" else ""),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (scrutin.url.isNotBlank()) LienSource(Ref("Scrutin officiel", scrutin.url, ""), texte = "Source : scrutin officiel")
                        }
                    }
                }
                HorizontalDivider()
            }

            // Programme groupé par domaine
            item {
                TitreSection("Programme")
                val prog = candidat.programme
                if (prog != null && prog.titre.isNotBlank()) Text(prog.titre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                PastilleEtat(candidat, prefixe = "État : ")
                if (prog != null && prog.note.isNotBlank()) {
                    Text(
                        prog.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            catalogue.themes.forEach { t ->
                val mesures = mesuresDu(candidat, t.id)
                item(key = "theme-${t.id}") {
                    SectionRepliable(
                        t.label,
                        "th-${candidat.id}-${t.id}",
                        sousTitre = when (mesures.size) {
                            0 -> "Pas de proposition publiée sur ce domaine"
                            1 -> "1 mesure"
                            else -> "${mesures.size} mesures"
                        },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            if (mesures.isEmpty()) AucuneProposition(candidat)
                            mesures.forEach { m -> CarteMesure(m, texteDePartage(candidat, t.label, m)) { vm.message("Texte de la mesure copié") } }
                        }
                    }
                    HorizontalDivider()
                }
            }

            // Liens officiels
            item {
                TitreSection("Liens")
                val campagne = candidat.liens["campagne"]
                val programme = candidat.programme?.url?.ifBlank { null } ?: candidat.liens["programme"]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoutonLien("Site de campagne", campagne, ouvrir)
                    BoutonLien("Programme officiel", programme, ouvrir)
                }
            }
            item { PiedDePage(catalogue.source) }
        }
    }
}

@Composable
private fun BoutonLien(libelle: String, url: String?, ouvrir: (String) -> Unit) {
    if (url.isNullOrBlank()) {
        Text(
            "$libelle : non recensé par la source",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        OutlinedButton(onClick = { ouvrir(url) }, modifier = Modifier.fillMaxWidth()) {
            Text(libelle)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Puce(texte: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(texte, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Parcours(entrees: List<Entree>) {
    Column(Modifier.padding(bottom = 8.dp)) {
        entrees.forEach { e ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text(
                    e.periode,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(96.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(e.texte, style = MaterialTheme.typography.bodyMedium)
                    e.source?.let { LienSource(it) }
                }
            }
        }
    }
}

@Composable
private fun BlocAffaire(a: Affaire) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(a.titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val etat = listOfNotNull(
                libelleEtatAffaire(a.etat).ifBlank { null },
                a.dateEtat.ifBlank { null }?.let { "au ${dateCourte(it)}" },
            ).joinToString(" ")
            if (etat.isNotBlank()) {
                Text(etat, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (a.etatDetail.isNotBlank()) {
                Text(a.etatDetail, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(a.texte, style = MaterialTheme.typography.bodyMedium)
            a.sources.forEach { LienSource(it) }
        }
    }
}

/** Photo en grand, avec son auteur et sa licence. */
@Composable
fun PhotoPleinEcran(vm: MainViewModel, candidat: Candidat, onFermer: () -> Unit) {
    val ouvrir = rememberOuvrir()
    val fichier = vm.repository.photo(candidat)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (fichier != null) {
            AsyncImage(
                model = fichier,
                contentDescription = "Photo de ${candidat.nom}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clickable(onClickLabel = "Fermer") { onFermer() },
            )
        }
        IconButton(onClick = onFermer, modifier = Modifier.statusBarsPadding().padding(8.dp).align(Alignment.TopStart)) {
            Icon(Icons.Outlined.Close, "Fermer", tint = Color.White)
        }
        if (candidat.photo == null) {
            Text(
                "${candidat.nom} — photo ajoutée sur ce téléphone (non publiée)",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.6f)).navigationBarsPadding().padding(16.dp),
            )
        }
        candidat.photo?.let { p ->
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Text(candidat.nom, color = Color.White, style = MaterialTheme.typography.titleMedium)
                if (p.legende.isNotBlank()) Text(p.legende, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("Photo : ${p.auteur} · ${p.licence}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                if (p.page.isNotBlank()) {
                    Text(
                        "Voir sur Wikimedia Commons",
                        color = Color(0xFFA9C7E3),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { ouvrir(p.page) }.padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
