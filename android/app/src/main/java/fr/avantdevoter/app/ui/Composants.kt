package fr.avantdevoter.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.IconButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.avantdevoter.app.data.Candidat
import fr.avantdevoter.app.data.Mesure
import fr.avantdevoter.app.data.Ref
import fr.avantdevoter.app.data.Reglages
import fr.avantdevoter.app.data.SourceDonnees
import fr.avantdevoter.app.data.etatProgramme
import fr.avantdevoter.app.data.initiales
import fr.avantdevoter.app.data.libelleEtat
import fr.avantdevoter.app.data.libelleNature
import fr.avantdevoter.app.data.ligneSource
import fr.avantdevoter.app.data.mentionSource
import java.io.File

/** Ouvre une adresse dans le navigateur ; ne plante pas si l'adresse est invalide. */
@Composable
fun rememberOuvrir(): (String) -> Unit {
    val uri = LocalUriHandler.current
    return { url -> if (url.startsWith("http")) runCatching { uri.openUri(url) } }
}

/** Photo ronde du candidat, ou ses initiales sur fond neutre (même rendu pour tous). */
@Composable
fun PhotoCandidat(candidat: Candidat, fichier: File?, taille: Dp, modifier: Modifier = Modifier) {
    val m = modifier.size(taille).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
    if (fichier != null) {
        AsyncImage(
            model = fichier,
            contentDescription = "Photo de ${candidat.nom}",
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter, // visages en haut des portraits
            modifier = m,
        )
    } else {
        Box(m, contentAlignment = Alignment.Center) {
            Text(
                initiales(candidat.nom),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = (taille.value * 0.36f).sp,
            )
        }
    }
}

/** Badge de nature d'une mesure : même couleur pour toutes, seul le mot change. */
@Composable
fun BadgeNature(nature: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(libelleNature(nature), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

/** Pastille « ● complet / ◐ partiel / ○ aucun » de l'état du programme. */
@Composable
fun PastilleEtat(candidat: Candidat, prefixe: String = "Programme : ") {
    val etat = etatProgramme(candidat)
    val symbole = when (etat) {
        "complet" -> "●"
        "partiel" -> "◐"
        else -> "○"
    }
    Text(
        "$symbole $prefixe${libelleEtat(etat)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Lien « Source : titre · date ↗ » qui ouvre la page d'origine. */
@Composable
fun LienSource(ref: Ref?, modifier: Modifier = Modifier, texte: String = ligneSource(ref)) {
    val ouvrir = rememberOuvrir()
    val url = ref?.url.orEmpty()
    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = url.isNotBlank(), role = Role.Button, onClickLabel = "Ouvrir la source") { ouvrir(url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            texte,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (url.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Ouvre le menu Partager d'Android avec un texte. */
@Composable
fun rememberPartager(): (String) -> Unit {
    val context = LocalContext.current
    return { texte ->
        val envoi = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texte)
        context.startActivity(Intent.createChooser(envoi, "Partager la mesure"))
    }
}

/**
 * Une mesure : badge de nature, texte exact de la source, lien vers la source, bouton Partager.
 * Appui long : copier le texte. [partage] = texte envoyé par « Partager » (null : pas de bouton).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteMesure(mesure: Mesure, partage: String? = null, onCopie: () -> Unit) {
    val partager = rememberPartager()
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {},
                onLongClickLabel = "Copier le texte",
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboard.setText(AnnotatedString(mesure.texte))
                    onCopie()
                },
            ),
    ) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgeNature(mesure.nature)
                Spacer(Modifier.weight(1f))
                if (partage != null) {
                    IconButton(onClick = { partager(partage) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Share, "Partager la mesure", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(mesure.texte, style = MaterialTheme.typography.bodyMedium)
            LienSource(mesure.source)
        }
    }
}

/** « Pas de proposition publiée sur ce domaine » + état du programme : l'absence reste visible. */
@Composable
fun AucuneProposition(candidat: Candidat) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "Pas de proposition publiée sur ce domaine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PastilleEtat(candidat)
    }
}

/**
 * Filtres de statut : déclarés (par défaut) et pressentis, ou seulement les candidats suivis (étoile).
 * Ceux qui ont renoncé ne sont pas listés.
 */
@Composable
fun FiltresStatut(
    reglages: Reglages,
    onChange: (declares: Boolean, pressentis: Boolean) -> Unit,
    onSuivis: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = !reglages.suivisSeulement && reglages.declares,
            onClick = { onSuivis(false); onChange(!reglages.declares || reglages.suivisSeulement, reglages.pressentis) },
            label = { Text("Déclarés") },
        )
        FilterChip(
            selected = !reglages.suivisSeulement && reglages.pressentis,
            onClick = { onSuivis(false); onChange(reglages.declares, !reglages.pressentis || reglages.suivisSeulement) },
            label = { Text("Pressentis") },
        )
        FilterChip(
            selected = reglages.suivisSeulement,
            onClick = { onSuivis(!reglages.suivisSeulement) },
            leadingIcon = { Icon(Icons.Outlined.StarOutline, null, Modifier.size(18.dp)) },
            label = { Text("Suivis (${reglages.suivis.size})") },
        )
    }
}

/** Pied de chaque écran : crédit 2027 et moi et date des données. */
@Composable
fun PiedDePage(source: SourceDonnees?, modifier: Modifier = Modifier) {
    Text(
        mentionSource(source),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

/** Section qui s'ouvre et se ferme en touchant son titre. */
@Composable
fun SectionRepliable(
    titre: String,
    cle: String,
    ouverteAuDepart: Boolean = false,
    sousTitre: String? = null,
    contenu: @Composable () -> Unit,
) {
    var ouverte by rememberSaveable(cle) { mutableStateOf(ouverteAuDepart) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = if (ouverte) "Replier" else "Déplier") { ouverte = !ouverte }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(titre, style = MaterialTheme.typography.titleMedium)
                if (sousTitre != null) {
                    Text(sousTitre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(if (ouverte) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        AnimatedVisibility(ouverte) { Column { contenu() } }
    }
}

@Composable
fun TitreSection(texte: String, modifier: Modifier = Modifier) {
    Text(texte, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
fun EtatVide(texte: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(texte, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 360.dp))
    }
}
