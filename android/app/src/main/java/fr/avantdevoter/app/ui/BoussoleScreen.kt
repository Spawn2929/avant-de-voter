package fr.avantdevoter.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.avantdevoter.app.data.Catalogue
import fr.avantdevoter.app.data.GRADUATIONS
import fr.avantdevoter.app.data.Proximite
import fr.avantdevoter.app.data.QUESTIONS_COMMUNES_MIN
import fr.avantdevoter.app.data.QuestionCle
import fr.avantdevoter.app.data.Reponse
import fr.avantdevoter.app.data.choisirQuestions
import fr.avantdevoter.app.data.libelleBloc
import fr.avantdevoter.app.data.libellePositionCle
import fr.avantdevoter.app.data.libelleZone
import fr.avantdevoter.app.data.positionBloc
import fr.avantdevoter.app.data.positionUtilisateur
import fr.avantdevoter.app.data.proximites
import fr.avantdevoter.app.data.valeurPosition

private const val AVERTISSEMENT =
    "Ce n'est pas une recommandation de vote. Le résultat ne porte que sur 10 questions et sur les seules positions " +
        "recensées par 2027 et moi : beaucoup de candidats n'ont pas de position connue sur chaque question. " +
        "Rien n'est enregistré."

private const val REGLE =
    "Règle de calcul : pour chaque question, « oui » vaut +1 et « non » −1 ; la position d'un candidat vaut +1 (pour), " +
        "+0,5 (plutôt pour), 0 (nuancé), −0,5 (plutôt contre) ou −1 (contre). L'accord sur une question va de 100 % " +
        "(même réponse) à 0 % (réponse opposée). La proximité est la moyenne des accords sur les questions où vous avez " +
        "répondu et où la position du candidat est connue (au moins $QUESTIONS_COMMUNES_MIN). Sur l'axe, chaque candidat " +
        "est placé selon le bloc que lui attribue 2027 et moi ; votre place est la moyenne des places de vos 3 candidats " +
        "placés les plus proches, pondérée par la proximité. Les blocs « souverainiste » et « autre » ne sont pas placés sur l'axe."

/** « De quel bord es-tu ? » : 10 questions oui / non, puis les candidats les plus proches et votre place sur l'axe. */
@Composable
fun BoussoleScreen(vm: MainViewModel, cat: Catalogue, onRetour: () -> Unit, onCandidat: (String) -> Unit) {
    val declares = remember(cat) { cat.candidats.filter { it.statut == "declare" } }
    val questions = remember(cat) { choisirQuestions(cat.questions, cat.positions, declares.map { it.id }.toSet()) }
    var etape by rememberSaveable { mutableIntStateOf(-1) } // -1 : présentation ; 0..9 : question ; 10 : résultat
    // Réponses gardées en cas de rotation, sous la forme « q05=OUI ».
    var reponsesTexte by rememberSaveable { mutableStateOf(listOf<String>()) }
    val reponses = reponsesTexte.associate { it.substringBefore('=') to Reponse.valueOf(it.substringAfter('=')) }

    fun repondre(q: QuestionCle, r: Reponse) {
        reponsesTexte = reponsesTexte.filterNot { it.startsWith("${q.id}=") } + "${q.id}=${r.name}"
        etape += 1
    }

    EcranSimple("De quel bord es-tu ?", onRetour) {
        when {
            questions.isEmpty() -> item { EtatVide("Les questions de 2027 et moi ne sont pas encore disponibles : mettez les données à jour.") }
            etape < 0 -> item {
                Text(
                    "${questions.size} questions posées aux candidats par 2027 et moi, choisies parmi celles où le plus de " +
                        "candidats déclarés ont une position connue. Répondez par oui ou par non (ou passez) ; à la fin, " +
                        "l'appli indique les candidats dont les positions sont les plus proches de vos réponses, et votre place " +
                        "sur un axe de la gauche à l'extrême droite.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(AVERTISSEMENT, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { reponsesTexte = emptyList(); etape = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Commencer") }
                PiedDePage(cat.source)
            }
            etape < questions.size -> item {
                val q = questions[etape]
                Text("Question ${etape + 1} sur ${questions.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(cat.themes.firstOrNull { it.id == q.theme }?.label.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(q.texte, style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { repondre(q, Reponse.OUI) }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Oui", fontSize = 18.sp) }
                    Button(onClick = { repondre(q, Reponse.NON) }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Non", fontSize = 18.sp) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { etape -= 1 }, enabled = etape > 0) { Text("Question précédente") }
                    TextButton(onClick = { repondre(q, Reponse.PASSER) }) { Text("Passer") }
                }
                reponses[q.id]?.let {
                    Text(
                        "Votre réponse précédente : ${if (it == Reponse.OUI) "oui" else if (it == Reponse.NON) "non" else "passée"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> resultat(vm, cat, questions, reponses, declares, onCandidat) { reponsesTexte = emptyList(); etape = 0 }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultat(
    vm: MainViewModel,
    cat: Catalogue,
    questions: List<QuestionCle>,
    reponses: Map<String, Reponse>,
    declares: List<fr.avantdevoter.app.data.Candidat>,
    onCandidat: (String) -> Unit,
    onRecommencer: () -> Unit,
) {
    val liste = proximites(declares, cat.positions, reponses)
    val comparables = liste.map { it.candidat.id }.toSet()
    val nonComparables = declares.filter { it.id !in comparables }
    val vous = positionUtilisateur(liste)
    val repondues = reponses.values.count { it != Reponse.PASSER }

    item {
        if (liste.isEmpty()) {
            EtatVide("Pas assez de réponses ou de positions connues pour vous comparer ($QUESTIONS_COMMUNES_MIN questions en commun au moins). Répondez à plus de questions.")
        } else {
            val premier = liste.first()
            val exAequo = liste.filter { it.pourcent == premier.pourcent && it.questionsCommunes == premier.questionsCommunes }
            Text("Le plus proche de vos réponses", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            exAequo.forEach { p ->
                Text(
                    "${p.candidat.nom}${if (p.candidat.parti.isNotBlank()) " (${p.candidat.parti})" else ""}",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                "${premier.pourcent} % d'accord sur ${premier.questionsCommunes} question${if (premier.questionsCommunes > 1) "s" else ""}" +
                    if (exAequo.size > 1) " (ex æquo)" else "",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Vous avez répondu à $repondues question${if (repondues > 1) "s" else ""} sur ${questions.size}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TitreSection("Votre place de la gauche à l'extrême droite")
            Axe(liste, vous)
            if (vous != null) {
                Text(
                    "Vos réponses vous placent ${libelleZone(vous)}, d'après les blocs de 2027 et moi.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    "Vos candidats les plus proches ne sont pas placés sur l'axe (blocs « souverainiste » ou « autre »).",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                "2027 et moi ne distingue pas d'extrême gauche : les candidats de gauche, de la gauche sociale-démocrate " +
                    "à l'extrême gauche, sont tous dans le bloc « gauche ».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TitreSection("Proximité avec chaque candidat déclaré")
        }
    }
    items(liste, key = { it.candidat.id }) { p -> LigneProximite(vm, p) { onCandidat(p.candidat.id) } }
    item {
        if (nonComparables.isNotEmpty()) {
            Text(
                "Non comparables (moins de $QUESTIONS_COMMUNES_MIN positions connues sur vos réponses) : " +
                    nonComparables.joinToString(", ") { it.nom } + ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Text(AVERTISSEMENT, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
        Text(REGLE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
        TitreSection("Vos réponses et les positions des candidats")
    }
    items(questions, key = { it.id }) { q ->
        val r = reponses[q.id]
        SectionRepliable(
            q.texte,
            "detail-${q.id}",
            sousTitre = "Votre réponse : " + when (r) {
                Reponse.OUI -> "oui"
                Reponse.NON -> "non"
                else -> "passée"
            },
        ) {
            val connus = declares.mapNotNull { c -> cat.positions[c.id]?.get(q.id)?.takeIf { valeurPosition(it.position) != null }?.let { c to it } }
            if (connus.isEmpty()) Text("Aucune position connue.", style = MaterialTheme.typography.bodySmall)
            connus.forEach { (c, pos) ->
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text("${c.nom} : ${libellePositionCle(pos.position)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (pos.resume.isNotBlank()) Text(pos.resume, style = MaterialTheme.typography.bodySmall)
                    pos.source?.let { LienSource(it) }
                }
            }
        }
        HorizontalDivider()
    }
    item {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRecommencer, modifier = Modifier.fillMaxWidth()) { Text("Recommencer") }
        PiedDePage(cat.source)
    }
}

@Composable
private fun LigneProximite(vm: MainViewModel, p: Proximite, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = "Voir la fiche", onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        PhotoCandidat(p.candidat, vm.repository.photo(p.candidat), 36.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(p.candidat.nom, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${p.pourcent} %", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "${libelleBloc(p.candidat.bloc)} · ${p.questionsCommunes} question${if (p.questionsCommunes > 1) "s" else ""} en commun",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p.pourcent / 100f).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

/** Axe gauche → extrême droite : les candidats comparables (points), et vous (triangle « Vous »). */
@Composable
private fun Axe(liste: List<Proximite>, vous: Double?) {
    val mesure = rememberTextMeasurer()
    val couleur = MaterialTheme.colorScheme.primary
    val neutre = MaterialTheme.colorScheme.onSurfaceVariant
    val ligne = MaterialTheme.colorScheme.outline
    val style = MaterialTheme.typography.labelSmall.copy(color = neutre)
    val styleVous = TextStyle(color = couleur, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val places = liste.mapNotNull { p -> positionBloc(p.candidat.bloc)?.let { it to p } }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics {
                contentDescription = "Axe de la gauche à l'extrême droite" + (vous?.let { ", vous êtes ${libelleZone(it)}" } ?: "")
            },
    ) {
        val marge = 24.dp.toPx()
        val largeur = size.width - 2 * marge
        fun x(v: Double) = (marge + (v + 2) / 4 * largeur).toFloat()
        val yLigne = 110.dp.toPx()
        drawLine(ligne, Offset(marge, yLigne), Offset(size.width - marge, yLigne), 3f)
        GRADUATIONS.forEach { (v, nom) ->
            drawLine(ligne, Offset(x(v), yLigne - 8f), Offset(x(v), yLigne + 8f), 3f)
            val t = mesure.measure(nom, style)
            val gauche = (x(v) - t.size.width / 2f).coerceIn(0f, size.width - t.size.width)
            drawText(t, topLeft = Offset(gauche, yLigne + 6.dp.toPx()))
        }
        // Un point par candidat comparable, empilés au-dessus de leur bloc ; plus foncé = plus proche.
        places.groupBy { it.first }.forEach { (v, groupe) ->
            groupe.forEachIndexed { i, (_, p) ->
                drawCircle(neutre.copy(alpha = 0.25f + 0.75f * p.pourcent / 100f), 7.dp.toPx() / 2, Offset(x(v), yLigne - 14.dp.toPx() - i * 9.dp.toPx()))
            }
        }
        if (vous != null) {
            val cx = x(vous)
            val pointe = yLigne + 26.dp.toPx() // sous les noms des blocs
            val haut = pointe + 18.dp.toPx()
            val triangle = Path().apply {
                moveTo(cx, pointe); lineTo(cx - 9.dp.toPx(), haut); lineTo(cx + 9.dp.toPx(), haut); close()
            }
            drawPath(triangle, couleur)
            val t = mesure.measure("Vous", styleVous)
            drawText(t, topLeft = Offset((cx - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), haut + 2f))
        }
    }
    Text(
        "Points : les candidats comparables, au-dessus de leur bloc (plus foncé = plus proche de vous). Triangle : vous.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
}
