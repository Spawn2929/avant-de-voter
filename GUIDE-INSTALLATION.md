# Guide d'installation — Avant de voter

🙋 = à faire par Fred · 🤖 = fait par Claude

## Ce qui est en place

| Élément | Où | Remarque |
|---|---|---|
| Dépôt GitHub (public) | https://github.com/Spawn2929/avant-de-voter | code, robot, APK |
| Données publiées | https://spawn2929.github.io/avant-de-voter/ | `manifest.json`, `app-data.json`, `changes.json`, `photos/` |
| Robot quotidien | onglet **Actions** → « Données quotidiennes » | 6 h, heure de Paris |
| APK | onglet **Releases** du dépôt, et `apk/` sur le PC | fabriqué par « APK Android » |
| Clé de signature | `secrets/` sur le PC + secrets GitHub `SIGNING_*` | à sauvegarder (voir plus bas) |

Coût : 0 €. Aucun compte à créer : ni Firebase, ni serveur, ni carte bancaire.

## 🙋 Installer les téléphones (OnePlus 13 de Fred, Pixel 7a de Céline)

1. Sur le téléphone, ouvrez le lien de l'APK envoyé par Claude (page **Releases** du dépôt), ou le fichier
   `AvantDeVoter-X.Y.Z.apk` reçu par WhatsApp (envoyé comme « Document »).
2. Touchez le fichier `AvantDeVoter-X.Y.Z.apk` pour le télécharger, puis **Ouvrir**.
3. La première fois, Android demande d'autoriser cette source : touchez **Paramètres**, activez
   **Autoriser depuis cette source** (Chrome ou WhatsApp), puis revenez en arrière.
4. Touchez **Installer**. Si Play Protect affiche « Appli non reconnue », touchez **Plus de détails** puis
   **Installer quand même** (l'appli n'est pas sur le Play Store, c'est normal).
5. Ouvrez **Avant de voter** : les données s'affichent tout de suite, même sans réseau.

### 🙋 OnePlus 13 seulement : laisser la mise à jour du matin se faire

Sans ce réglage, OnePlus peut retarder la vérification quotidienne quand l'appli est fermée.

1. Appui long sur l'icône **Avant de voter** → **Infos sur l'appli**.
2. **Utilisation de la batterie** → choisissez **Autoriser l'activité en arrière-plan** (ou **Ne pas optimiser**).

Ce n'est pas grave si vous l'oubliez : l'appli se met aussi à jour à l'ouverture.

### Photos des candidats qui n'en ont pas

Depuis la v0.7, les photos de Selma Labib, Benoît Mathieu, Antoine Mikolajczak et Manolo Mlekuz sont fournies avec
l'appli (`android/app/src/main/assets/photos_integrees/`, réduites à 400 px) : rien à faire sur les téléphones.
Ces photos ne sont pas sous licence libre : à retirer de ce dossier si un ayant droit le demande.
Une photo ajoutée à la main sur un téléphone reste prioritaire sur la photo fournie.

### 🙋 Notifications et widget (facultatif)

1. Dans l'appli, ouvrez la fiche d'un candidat et touchez l'**étoile** en haut à droite.
2. Android demande « Autoriser Avant de voter à vous envoyer des notifications ? » : touchez **Autoriser**.
   (Refusé par erreur ? **Plus → Réglages → Autoriser les notifications**.)
3. Widget : appui long sur un espace vide de l'écran d'accueil → **Widgets** → **Avant de voter** → **Ajouter**.

## Mettre à jour l'appli plus tard

Installez simplement le nouvel APK **par-dessus** l'ancien (mêmes étapes 1 à 4) : rien n'est perdu.
Dans l'appli, **Plus → Réglages → Application** indique quand une nouvelle version est disponible.
Ne désinstallez jamais l'appli avant de mettre à jour.

## 🤖 Fabriquer un APK

1. Tests du robot : `python -m pytest pipeline/tests`.
2. Tests de l'appli : dans `android/`, `gradlew testDebugUnitTest`.
3. Commit, étiquette `vX.Y.Z`, envoi sur GitHub : le workflow « APK Android » copie les données du jour dans l'APK,
   lance les tests, signe l'APK et le publie dans les Releases (`versionCode` = numéro de fabrication, toujours croissant).
4. Copie locale : `gh release download vX.Y.Z -D apk`.

Sur ce PC, Gradle a besoin de dossiers temporaires sans accent (le nom d'utilisateur Windows en contient un) :
`TMP`, `TEMP` = `C:\Developpement_Claude\tmp` et
`JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Developpement_Claude\tmp -Djava.io.tmpdir=C:\Developpement_Claude\tmp`.

## 🤖 Le robot quotidien

- Fichier : `.github/workflows/donnees-quotidiennes.yml`. Il se lance à 6 h (heure de Paris), à chaque modification de
  `pipeline/`, ou à la main (onglet **Actions** → « Données quotidiennes » → **Run workflow**).
- Si une donnée de 2027 et moi est invalide, il s'arrête **sans rien publier** : la version de la veille reste en ligne
  et GitHub envoie un e-mail d'échec à Fred. Transmettre l'e-mail à Claude.
- Il garde tous les sondages déjà vus, même quand 2027 et moi les retire de sa fenêtre.
- Il écrit une ligne par mois dans `robot.txt` (branche `gh-pages`) : sans activité pendant 60 jours, GitHub suspendrait la tâche.

## 🙋 Sauvegarder `secrets/`

Copiez le dossier `C:\Developpement_Claude\Avant_de_voter\secrets` sur OneDrive (par exemple
`OneDrive\Sauvegardes\Avant_de_voter\secrets`).

| Si on perd… | Conséquence |
|---|---|
| `avant-de-voter-release.jks` et `keystore.properties` | Plus aucune mise à jour possible : il faudrait désinstaller l'appli et installer une nouvelle appli |

Les secrets GitHub `SIGNING_*` contiennent une copie de la clé, mais GitHub ne permet pas de la relire : la sauvegarde
OneDrive est la seule copie récupérable.
