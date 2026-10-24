# Avant de voter

Appli Android personnelle de Fred et Céline : ce que propose chaque candidat à la présidentielle 2027,
domaine par domaine, d'après les données ouvertes de [2027 et moi](https://2027etmoi.fr) (Licence Ouverte 2.0).
L'appli n'invente rien : chaque mesure affiche le texte exact de la source, sa nature et un lien vers la page d'origine.

Version actuelle : **0.1.0** (étape 1). Cadrage validé : [CADRAGE-VALIDATION.md](CADRAGE-VALIDATION.md).
Installation des téléphones et entretien : [GUIDE-INSTALLATION.md](GUIDE-INSTALLATION.md).

## Mode d'emploi

### Onglet Domaines

- En haut, les 13 domaines (Économie, Travail et pouvoir d'achat, Retraites…) : faites défiler la rangée et touchez-en un.
- Chaque candidat apparaît par ordre alphabétique avec ses mesures sur ce domaine. Sans mesure, l'appli l'écrit :
  « Pas de proposition publiée sur ce domaine », avec l'état de son programme (complet, partiel, aucun).
- Chaque mesure porte un badge (Programme, Déclaration, Presse) et une ligne « Source » : touchez-la pour ouvrir la page d'origine.
- Laissez le doigt sur une mesure pour copier son texte.
- « Afficher : Déclarés / Pressentis » choisit les candidatures montrées. Celles qui ont renoncé ne sont pas listées.
- Touchez un nom ou une photo pour ouvrir sa fiche.

### Onglet Candidats

- Grille des candidats avec photo, parti et état du programme. Touchez une carte pour ouvrir la fiche.
- **Fiche** : photo (touchez-la pour l'agrandir, avec son auteur et sa licence), statut et sa source, propositions phares,
  présentation, parcours, affaires recensées par 2027 et moi, puis tout le programme rangé par domaine
  (touchez un domaine pour le déplier), et les liens vers le site de campagne et le programme officiel.

### Onglet Plus

- **Réglages** : thème (automatique, clair, sombre), affichage des candidatures pressenties, date des données,
  bouton « Mettre à jour maintenant », version de l'appli et lien vers une nouvelle version s'il y en a une.
- **Crédits et sources** : 2027 et moi, Licence Ouverte, auteur et licence de chaque photo.

### Gestes

- Glisser vers la gauche ou la droite : onglet voisin.
- Tirer l'écran vers le bas : mettre à jour les données.
- Geste retour : ferme la fiche ouverte ; depuis un autre onglet, revient à Domaines.

## Mise à jour des données

Chaque matin à 6 h, un robot GitHub lit les données de 2027 et moi, les contrôle et les publie.
L'appli vérifie après 7 h (et à l'ouverture si la dernière vérification a plus de 24 h) ; elle ne télécharge
que si quelque chose a changé. 2027 et moi met ses données à jour à la main, sans rythme fixe : il est normal
que la date ne change pas tous les jours. Tout reste consultable sans réseau.

## Étapes

| Version | Contenu | État |
|---|---|---|
| 0.1 | Robot quotidien, onglets Domaines, Candidats et Plus (Réglages, Crédits), hors-ligne, thème | **livrée** |
| 0.2 | Onglet Sondages (liste, fiches, courbe d'un candidat, moyenne 30 jours), comparateur 2 à 4 candidats, calendrier et compte à rebours, onglet Nouveautés | à venir |
| 0.3 | Partage d'une mesure, Mes priorités, Quiz « Qui propose quoi ? », widget d'écran d'accueil | à venir |
| 0.4 | Candidats suivis (étoile), notifications, recherche dans les mesures | à venir |
| 0.5 | Votes au Parlement sur la fiche candidat | optionnel |

## Organisation du dépôt

- `pipeline/` : le robot (Python 3.12), ses tests (`python -m pytest pipeline/tests`) et la copie des données dans l'APK.
- `android/` : l'appli (Kotlin, Jetpack Compose, Room, WorkManager, Coil).
- `.github/workflows/` : `donnees-quotidiennes.yml` (robot, 6 h) et `apk-android.yml` (APK signé sur étiquette `v*`).
- `secrets/` (pas dans le dépôt) : clé de signature. `apk/` (pas dans le dépôt) : APK téléchargés.
