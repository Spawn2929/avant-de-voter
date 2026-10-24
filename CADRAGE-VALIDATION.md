# Avant de voter — cadrage à valider (phase 1, rien n'est codé)

24 octobre 2026 · à relire par Fred : barrer ce qui ne convient pas, répondre aux 5 questions en fin de document.

---

## 1. Ce que j'ai compris

- « **Avant de voter** » est une appli Android pour Fred et Céline : ce que propose chaque candidat à la présidentielle 2027, **domaine par domaine**, et **tous les sondages**.
- Tout vient du dépôt open data **2027 et moi** ; l'appli **n'invente rien** : chaque mesure garde son texte d'origine, sa source et sa date.
- Un petit robot gratuit (GitHub) prépare les données **chaque matin à 6 h** ; chaque téléphone les télécharge et les garde : **tout marche en mode avion**.
- Pas de compte, pas de serveur à nous, pas de publicité, pas de Play Store : un APK installé à la main.
- **Neutralité stricte** : ordre alphabétique, même couleur pour tous, aucune note, aucune recommandation.

**Vérification faite ce matin sur la source** (dépôt public, données du 22/10/2026) : 54 personnalités (32 déclarées, 5 pressenties, 17 ont renoncé), 40 fiches programme (6 complètes, 24 partielles, 10 « aucun »), 670 mesures, 20 sondages, 7 instituts, 35 questions clés. Les 13 domaines du cadrage existent tels quels. Trois points ont changé mes propositions :

| Constat dans les données | Conséquence |
|---|---|
| 7 personnalités n'ont **pas de photo** | Cercle gris neutre avec leurs initiales, identique pour tous |
| Le domaine **Société** n'a que **3 mesures** au total | 29 candidats sur 32 y afficheront « Pas de proposition publiée » : normal, c'est la règle « absence visible » |
| `sondages.json` ne garde qu'une **fenêtre** (17/08 → 19/10) : un vieux sondage peut en sortir | Le robot **conserve tous les sondages déjà vus** (avec leur source) pour que la courbe d'un candidat ne rétrécisse pas |
| Un sondage peut citer un nom hors liste (ex. « Sébastien Bonnal ») | Affiché avec son nom, sans photo, comme les autres |
| Les fiches bio contiennent aussi une rubrique **« affaires »** (judiciaires, sourcées) | Question 3 |

---

## 2. Onglets et écrans

### Barre d'onglets définitive (5 onglets, en bas)

```
┌──────────┬───────────┬───────────┬────────────┬────────┐
│ Domaines │ Candidats │ Sondages  │ Nouveautés │  Plus  │
└──────────┴───────────┴───────────┴────────────┴────────┘
```

- **Plus** regroupe : Calendrier, Mes priorités, Quiz, **Réglages**, Crédits. (Le cadrage n'avait pas de place pour l'agenda, le quiz et les réglages : c'est le piège « onglet Profil remplacé par Réglages une heure après ».)
- **Nouveautés** : voir question 1 (je propose de l'avancer au lot 1).
- Glisser à gauche / à droite passe à l'onglet voisin, **sauf** sur la rangée de puces des domaines et dans une fiche de sondage (là, le glissement sert à autre chose).
- Geste retour Android : revient à l'écran précédent ; depuis un onglet autre que Domaines, revient à Domaines ; ne ferme l'appli que depuis Domaines.
- Pied de chaque écran, en petit : « Source : 2027 et moi (2027etmoi.fr), données mises à jour le 22/10/2026 ».
- Tirer vers le bas sur n'importe quel onglet = « Mettre à jour maintenant ».

### Onglet Domaines

```
┌─────────────────────────────────────────────┐
│ Avant de voter        1er tour dans 170 j › │  ← touche : Calendrier
│ (Économie)(Travail et pouvoir d'achat)(Re…→ │  ← puces défilantes, 13 domaines
│ Afficher : [Déclarés ✓] [Pressentis]        │
├─────────────────────────────────────────────┤
│ (●) Nathalie Arthaud · Lutte ouvrière       │  ← toucher nom/photo : fiche candidat
│   ┌ [Programme] Texte exact de la mesure…   │
│   │ Source : titre · 01/2025  ↗  ⤴partager  │  ← « ↗ » ouvre la page d'origine
│   └ [Déclaration] Autre mesure…             │
│ (●) François Asselineau · UPR               │
│   Pas de proposition publiée sur ce domaine │
│   Programme : partiel                       │
│ …                                           │
│ Source : 2027 et moi … le 22/10/2026        │
└─────────────────────────────────────────────┘
```

- Le domaine choisi est retenu à la réouverture.
- Badge de nature : Programme / Déclaration / Presse, même couleur neutre, seul le mot change.
- Lien « Source » : ouvre la page dans le navigateur. Appui long sur une mesure : copier le texte.
- Ordre : alphabétique par nom de famille (Arthaud, Asselineau, Attal… Le Pen à « L », de Villepin à « V »).
- Rien à créer, modifier ou supprimer dans l'appli : tout vient de la source (donc pas de bouton +, pas d'« Annuler »).

### Onglet Candidats

```
┌─────────────────────────────────────────────┐
│ Afficher : [Déclarés ✓] [Pressentis]        │
│ ┌──────────┐ ┌──────────┐                   │
│ │  (photo) │ │  (photo) │   ← grille 2 col. │
│ │ Arthaud  │ │Asselineau│                   │
│ │ LO       │ │ UPR      │                   │
│ │ ◐ partiel│ │ ○ aucun  │   ← état programme│
│ └──────────┘ └──────────┘                   │
│ …                                           │
│        [ Comparer (2) ]   ← apparaît dès 2  │
└─────────────────────────────────────────────┘
```

- **Toucher** une carte : ouvre la fiche. **Appui long** : coche la carte (mode sélection, 4 au maximum ; un 5e affiche « 4 candidats au plus »). Le bouton « Comparer » apparaît à partir de 2.
- **Fiche candidat** : photo (toucher = en grand) avec « Photo : auteur · licence » dessous ; nom, parti, statut et son détail ; propositions phares ; présentation et parcours ; programme groupé par domaine (13 sections repliables, celles sans mesure indiquent « Pas de proposition publiée ») ; liens « Site de campagne » et « Programme officiel ». Note de la source sur l'état du programme affichée telle quelle.
- **Comparateur** (2 à 4) : 4 colonnes côte à côte seraient illisibles sur un téléphone. Je propose : puces des domaines en haut, puis les candidats **l'un sous l'autre** avec leurs mesures sur ce domaine ; glisser à gauche / à droite = domaine suivant / précédent.

### Onglet Sondages

```
┌─────────────────────────────────────────────┐
│ ▾ Moyenne des 30 derniers jours (7 sond.)   │  ← repliable
│   Le Pen      ████████████████ 36,4 %  (7)  │  ← (7) = nb de sondages
│   Mélenchon   ███████ 15,8 %           (7)  │
│   Règle de calcul ›                          │
│ Institut : [Tous ▾]                          │
├─────────────────────────────────────────────┤
│ Ifop-Fiducial · LCI, Le Figaro, Sud Radio   │
│ Terrain 17–19/10/2026 · 1 928 pers. · 5 hyp.│
│ OpinionWay · …                               │
│ Un sondage est une estimation avec une marge│
│ d'erreur, pas une prédiction.                │
└─────────────────────────────────────────────┘
```

- Liste du plus récent au plus ancien ; filtre par institut.
- **Fiche d'un sondage** : fiche technique, puis chaque hypothèse avec son titre et ses barres (photo, nom, score), triées par score — mention « Trié par score » visible ; bouton « Voir la notice officielle ». Glisser = sondage suivant / précédent.
- Toucher un candidat dans une fiche : **ses scores dans tous les sondages**, en courbe (un point par sondage = moyenne de ses hypothèses, trait vertical du plus bas au plus haut) et en liste (chaque hypothèse détaillée).
- Moyenne : le chiffre entre parenthèses dit dans combien de sondages le candidat a été testé, pour ne pas comparer des moyennes de poids différents sans le savoir.

### Onglet Nouveautés

- Liste datée des changements détectés par le robot : « Nouvelle mesure — Glucksmann — Économie », « Programme de X : partiel → complet », « Y a renoncé », « Nouveau sondage Elabe ».
- Toucher une ligne : ouvre la mesure, la fiche ou le sondage concerné.
- 90 derniers jours. Si rien n'a changé : « Pas de changement depuis le JJ/MM — 2027 et moi met ses données à jour à la main, sans rythme fixe. »

### Onglet Plus

- **Calendrier** : étapes datées de la source (primaires, parrainages, tours), avec leur statut (officielle / prévue / estimée) et leur source ; compte à rebours vers le 1er tour en tête.
- **Mes priorités** (à l'aveugle) : on choisit un domaine, l'appli montre 8 mesures **sans nom**, mélangées au hasard ; on coche celles qui plaisent ; « Révéler » affiche sous chaque mesure le nom du candidat et sa source. Rien n'est enregistré. (Question 4.)
- **Quiz « Qui propose quoi ? »** : une mesure, 4 photos, deviner l'auteur ; série de 10 ; réponse montrée avec la source ; score de la série affiché à la fin, non enregistré. Les 3 leurres sont tirés au hasard parmi les déclarés qui ont des mesures, jamais un candidat qui porte la même mesure.
- **Réglages** : thème (clair / sombre / automatique) ; afficher les pressentis par défaut (oui/non) ; mise à jour : date de la dernière, bouton « Mettre à jour maintenant » ; version de l'appli et « Nouvelle version disponible » si une release plus récente existe sur GitHub.
- **Crédits** : 2027 et moi, Licence Ouverte 2.0, lien vers leur site et leur dépôt ; liste des photos avec auteur et licence ; Wikimedia Commons.

### Widget d'écran d'accueil

```
┌──────────────────────────────┐
│ 1er tour dans 170 jours      │
│ Moyenne 30 j : Le Pen 36 % · │
│ Mélenchon 16 % · Philippe 14 %│  ← 3 premiers, trié par score
│ données du 22/10              │
└──────────────────────────────┘
```

Toucher le widget ouvre l'onglet Sondages.

---

## 3. Listes métier (reprises telles quelles de la source)

| Liste | Valeurs |
|---|---|
| **13 domaines** (ordre des puces) | Économie · Travail et pouvoir d'achat · Retraites · Santé · Éducation · Écologie · Immigration · Sécurité et justice · International et défense · Institutions · Logement · Territoires · Société |
| Nombre de mesures par domaine (22/10) | Économie 78 · Institutions 75 · Travail 74 · Éducation 66 · Sécurité 57 · Écologie 56 · International 55 · Retraites 51 · Immigration 48 · Territoires 46 · Logement 33 · Santé 28 · Société 3 |
| **Nature d'une mesure** | Programme (533) · Déclaration (95) · Presse (42) |
| **État du programme** | ● complet · ◐ partiel · ○ aucun |
| **Statut du candidat** | Déclaré (affiché par défaut) · Pressenti (en option) · A renoncé (masqué, sauf dans les sondages où il a été testé) |
| **Instituts** | Cluster17 · Elabe · Ifop-Fiducial · Ipsos bva · OpinionWay · Toluna Harris Interactive · YouGov (s'allonge tout seul si un nouvel institut apparaît) |
| **Types de nouveautés** | Mesure ajoutée · Mesure retirée · État du programme · Statut du candidat · Nouveau candidat · Nouveau sondage |
| **Règle de la moyenne** | Sondages dont la fin de terrain a moins de 30 jours ; pour un sondage à plusieurs hypothèses, moyenne des hypothèses où le candidat figure ; puis moyenne simple entre sondages, arrondie à 0,1 point |

Ces listes ne sont **pas modifiables** dans l'appli : elles viennent de la source, et les changer casserait la neutralité.

---

## 4. Comment ça marche « en vrai »

Aujourd'hui, pour savoir qui propose quoi, on lit des articles ou le site 2027etmoi.fr, un candidat à la fois, et on oublie d'où vient l'info. Les sondages se lisent un par un dans la presse, souvent réduits à « X est en tête ».

L'appli colle à deux habitudes :
- **« Et sur les retraites, ils disent quoi ? »** → l'entrée principale est le domaine, pas le candidat (onglet Domaines en premier).
- **« Il a vraiment dit ça ? »** → la source est à un toucher, et le partage envoie la mesure avec sa source, pour en discuter à deux sans déformer.

---

## 5. Choix par défaut retenus (barrer ceux qui ne conviennent pas)

1. Dossier du projet : `C:\Developpement_Claude\Avant_de_voter` ; nom affiché « Avant de voter ».
2. Icône : une urne avec un bulletin, gris-bleu neutre (pas de bleu-blanc-rouge ni de couleur de parti).
3. Couleur unique de l'appli : gris-bleu neutre, la même pour tous les candidats.
4. Pas d'écran « Qui es-tu ? » : rien n'est partagé entre les téléphones, l'appli n'a pas besoin de savoir qui la tient.
5. Pas de serveur à nous, pas de Firebase : un robot GitHub (gratuit) + des fichiers en ligne (GitHub Pages, gratuit).
6. L'APK contient une copie des données du jour de sa fabrication : l'appli marche dès l'installation, même sans réseau ; elle se met à jour ensuite.
7. Les photos (54, environ 1 Mo au total) sont téléchargées une fois sur le téléphone, pas au fil de l'eau.
8. Synchro automatique une fois par jour après 7 h (si réseau), plus à l'ouverture si les données ont plus de 24 h, plus « tirer pour actualiser ».
9. Le robot tourne à 6 h heure de Paris toute l'année (passage heure d'été / d'hiver géré) et peut être lancé à la main.
10. Le robot garde **tous les sondages déjà vus**, même si 2027 et moi les sort de sa fenêtre.
11. Le robot écrit au moins une fois par mois dans le dépôt, pour que GitHub ne mette pas la tâche quotidienne en pause (il le fait après 60 jours sans activité).
12. Si une donnée de la source est invalide, rien n'est publié et l'appli garde la version précédente ; Fred reçoit l'e-mail d'échec de GitHub.
13. Les notes internes de la source (« à vérifier ») ne sont pas affichées.
14. Liens externes ouverts dans le navigateur du téléphone.
15. Partage d'une mesure : texte (« Candidat — Domaine — mesure — Source : titre, date, lien — via 2027 et moi ») par le menu Partager d'Android ; l'image viendra plus tard si Fred la veut.
16. Thème clair / sombre / automatique, automatique par défaut.
17. Pas de statistiques d'usage, pas de publicité, aucune donnée envoyée.
18. Réglage « Nouvelle version disponible » qui lit les releases GitHub (gratuit, sans compte).

---

## 6. Découpage en étapes (chaque étape livre un APK utilisable)

| Étape | Version | Contenu | Lot |
|---|---|---|---|
| 1 | v0.1 | Robot quotidien + fichiers en ligne ; onglets **Domaines**, **Candidats** (grille, fiche complète), **Plus** (Réglages, Crédits) ; hors-ligne ; thème ; synchro quotidienne | 1 |
| 2 | v0.2 | Onglet **Sondages** complet (liste, fiches, courbe d'un candidat, moyenne 30 j) ; **comparateur** 2 à 4 ; **Calendrier** et compte à rebours ; **Nouveautés** (si oui à la question 1) | 1 |
| 3 | v0.3 | **Partage**, **Mes priorités**, **Quiz**, **widget** → contrôle de toutes les cases du lot 1 | 1 |
| 4 | v0.4 | Candidats suivis (étoile, filtre), **notifications** sur leurs changements, **recherche** plein texte | 2 |
| 5 | v0.5 | Votes au Parlement sur la fiche candidat (si Fred le veut encore) | 3 |

Chaque version s'installe par-dessus la précédente sans rien perdre.

---

## 7. Ce que Fred aura à faire (🙋) et ce que je fais (🤖)

- 🤖 Créer le dépôt GitHub `Spawn2929/avant-de-voter`, activer GitHub Pages, créer la clé de signature et la ranger dans `secrets/` et dans les secrets GitHub (ton compte GitHub est déjà connecté sur ce PC).
- 🤖 Écrire le robot, l'appli, les tests, contrôler chaque écran sur l'émulateur, fabriquer l'APK.
- 🙋 Sur chaque téléphone (OnePlus 13 de Fred, Pixel 7a de Céline) : ouvrir le lien de l'APK, autoriser l'installation « depuis cette source » une fois, installer. Pas à pas fourni dans `GUIDE-INSTALLATION.md`.
- 🙋 Sur le OnePlus seulement : désactiver l'optimisation de batterie pour l'appli (sinon la synchro du matin peut être retardée). Pas à pas fourni.
- 🙋 Sauvegarder le dossier `secrets/` (clé de signature) sur OneDrive : sans elle, plus de mise à jour possible.

**Coût : 0 €.** GitHub Actions et GitHub Pages sont gratuits pour un dépôt public, sans carte bancaire.

---

## 8. Questions (réponse en un mot)

1. **Nouveautés dès le lot 1 ?** Le robot calcule déjà les changements au lot 1 ; afficher leur liste coûte peu et fixe la barre d'onglets dès le départ. L'étoile et les notifications restent au lot 2.
   **A** oui (recommandé) · **B** non, au lot 2 comme prévu.

2. **Dépôt GitHub public ?** GitHub Pages gratuit exige un dépôt public : le code de l'appli et l'APK seront visibles par tous (les données le sont déjà ; la clé de signature, elle, reste secrète).
   **A** oui, un seul dépôt public (recommandé) · **B** non : code privé, données publiées sur Cloudflare Pages (gratuit aussi, un peu plus de mise en place).

3. **Affaires judiciaires dans la fiche candidat ?** La source les recense, avec sources et dates (ex. condamnations, procédures en cours). Le cadrage ne les demandait pas.
   **A** oui, section repliée « Affaires (selon 2027 et moi) », même place pour tous, texte tel quel (recommandé : c'est sourcé et traité pareil pour chacun) · **B** non.

4. **Mes priorités : un total par candidat à la fin ?** Afficher « Vous avez choisi 3 mesures de X » ressemble à un classement, donc à une recommandation.
   **A** non, seulement le nom sous chaque mesure (recommandé) · **B** oui, avec le total.

5. **Afficher aussi les candidats qui ont renoncé dans la fiche candidat et le comparateur** (accessibles via un filtre « A renoncé ») ?
   **A** non, masqués partout sauf dans les sondages où ils ont été testés (recommandé, conforme au cadrage) · **B** oui, avec un filtre.

Un « oui à tout » vaut validation de toutes les recommandations ci-dessus.
