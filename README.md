# Avant de voter

Appli Android personnelle de Fred et Céline : ce que propose chaque candidat à la présidentielle 2027,
domaine par domaine, et tous les sondages, d'après les données ouvertes de [2027 et moi](https://2027etmoi.fr)
(Licence Ouverte 2.0). L'appli n'invente rien : chaque mesure affiche le texte exact de la source, sa nature et un
lien vers la page d'origine.

Version actuelle : **0.6.0** (lots 1, 2 et 3 complets). Cadrage validé : [CADRAGE-VALIDATION.md](CADRAGE-VALIDATION.md).
Installation des téléphones et entretien : [GUIDE-INSTALLATION.md](GUIDE-INSTALLATION.md).

## Mode d'emploi

Cinq onglets en bas : **Domaines**, **Candidats**, **Sondages**, **Nouveautés**, **Plus**.
Glisser vers la gauche ou la droite passe à l'onglet voisin ; tirer l'écran vers le bas met à jour les données ;
le geste retour ferme l'écran ouvert, puis revient à Domaines.

### Domaines

- En haut, le compte à rebours du 1er tour (touchez-le pour le calendrier) et la loupe de **recherche**.
- Les 13 domaines (Économie, Travail et pouvoir d'achat, Retraites…) : touchez-en un.
- Chaque candidat apparaît par ordre alphabétique avec ses mesures sur ce domaine. Sans mesure, l'appli l'écrit :
  « Pas de proposition publiée sur ce domaine », avec l'état de son programme.
- Chaque mesure : badge (Programme, Déclaration, Presse), ligne « Source » qui ouvre la page d'origine,
  bouton **Partager** (envoie le texte avec sa source). Laissez le doigt dessus pour copier le texte.
- Filtres : **Déclarés**, **Pressentis**, ou **Suivis** (seulement vos candidats étoilés). Ceux qui ont renoncé
  ne sont pas listés (ils restent visibles dans les sondages).

### Candidats

- Candidat sans photo publiée : sur sa fiche, **Ajouter une photo (sur ce téléphone)** pour en choisir une dans la
  galerie. Elle reste sur ce téléphone, n'est jamais publiée ; « Changer » ou « Retirer » à tout moment.
- Grille des candidats : photo, parti, état du programme, étoile si vous le suivez. Touchez une carte pour la fiche.
- **Comparer** : laissez le doigt sur une carte, touchez 1 à 3 autres cartes, puis « Comparer ». Le comparateur
  montre un domaine à la fois, les candidats l'un sous l'autre ; glissez pour changer de domaine.
- **Fiche** : étoile en haut à droite pour suivre le candidat ; photo (touchez pour l'agrandir, auteur et licence),
  statut et source, lien vers ses scores dans les sondages, propositions phares, présentation, parcours,
  affaires et votes au Parlement recensés par 2027 et moi, programme rangé par domaine, liens officiels.

### Sondages

- Liste de tous les sondages, du plus récent au plus ancien ; filtre par institut.
- En tête, **Moyenne des 30 derniers jours** (touchez pour la déplier) avec la règle de calcul ;
  le chiffre entre parenthèses dit dans combien de sondages la personne a été testée.
- Touchez un sondage : toutes ses hypothèses, triées par score, et « Voir la notice officielle ».
  Glissez à gauche ou à droite pour passer au sondage suivant.
- Touchez un nom : ses scores dans tous les sondages, en courbe et en liste.

### Nouveautés

- Ce que le robot a détecté ces 90 derniers jours : nouvelles mesures, mesures retirées, programme complété,
  candidature ou renoncement, nouveau sondage. Touchez une ligne pour ouvrir la fiche ou le sondage.
- « Mes candidats suivis » ne garde que les candidats étoilés.
- Quand un candidat suivi change, l'appli envoie une **notification** (si vous l'avez autorisée).

### Plus

- **Calendrier** : compte à rebours et étapes datées (primaires, parrainages, 1er et 2nd tour), avec leur source.
- **Mes priorités** : des mesures sans nom, dans un ordre tiré au hasard ; cochez celles qui vous parlent puis
  « Révéler » montre qui les propose. Rien n'est enregistré, aucun total par candidat.
- **Qui propose quoi ?** : une mesure, quatre candidats, deviner l'auteur ; série de 10.
- **De quel bord es-tu ?** : 10 questions de 2027 et moi (retraite à 62 ans, nucléaire, SMIC, Ukraine…), réponse oui,
  non ou passer. Résultat : les candidats déclarés dont les positions sourcées sont les plus proches de vos réponses
  (pourcentage d'accord, comparaison possible à partir de 5 questions en commun), votre place sur un axe gauche → extrême
  droite d'après les blocs de 2027 et moi, et pour chaque question la position de chaque candidat avec sa source.
  Ce n'est pas une recommandation de vote ; rien n'est enregistré.
- **Réglages** : thème, candidatures pressenties, candidats suivis et notifications, mise à jour des données,
  version de l'appli et lien vers une nouvelle version.
- **Crédits et sources** : 2027 et moi, Licence Ouverte, auteur et licence de chaque photo.

### Widget d'écran d'accueil

Appui long sur l'écran d'accueil → **Widgets** → **Avant de voter** → **Ajouter**. Il affiche le compte à rebours
du 1er tour et les trois premiers de la moyenne des sondages (triés par score) ; le toucher ouvre l'appli.

## Mise à jour des données

Chaque matin à 6 h, un robot GitHub lit les données de 2027 et moi, les contrôle et les publie.
L'appli vérifie après 7 h (et à l'ouverture si la dernière vérification a plus de 24 h, ou juste après une mise à jour
de l'appli) ; elle ne télécharge que si quelque chose a changé. 2027 et moi met ses données à jour à la main,
sans rythme fixe : il est normal que la date ne change pas tous les jours. Tout reste consultable sans réseau.

## Versions

| Version | Contenu |
|---|---|
| 0.1 | Robot quotidien, onglets Domaines, Candidats et Plus (Réglages, Crédits), hors-ligne, thème |
| 0.6 | « De quel bord es-tu ? » (10 questions, proximité, axe gauche-droite) ; photos ajoutées sur le téléphone |
| 0.5.1 | Photos libres de Wikimedia Commons ajoutées quand 2027 et moi n'en a pas (Dubuisson, Platret), origine indiquée sous la photo |
| 0.5 | Sondages (liste, fiches, courbe, moyenne 30 jours), comparateur, calendrier, Nouveautés, partage, Mes priorités, quiz, widget, candidats suivis et notifications, recherche, votes au Parlement |

## Organisation du dépôt

- `pipeline/` : le robot (Python 3.12), ses tests (`python -m pytest pipeline/tests`) et la copie des données dans l'APK.
- `android/` : l'appli (Kotlin, Jetpack Compose, Room, WorkManager, Coil, Glance pour le widget).
- `.github/workflows/` : `donnees-quotidiennes.yml` (robot, 6 h) et `apk-android.yml` (APK signé sur étiquette `v*`).
- `secrets/` (pas dans le dépôt) : clé de signature. `apk/` (pas dans le dépôt) : APK téléchargés.
