# MonExplorateur

Gestionnaire de fichiers Android moderne, rapide et open-source développé en **Kotlin** et **Jetpack Compose** (Material 3), conçu pour fonctionner aussi bien sur un appareil standard que sur un appareil avec **Shizuku**, **Root** ou **LSPosed**.

---

## Fonctionnalités principales

### Exploration et gestion de fichiers
* **Navigation complète :** Stockage interne, dossiers système, favoris, recherche globale et tri multi-critères.
* **Accès privilégiés optionnels :** Prise en charge native de **Shizuku** et **Root (`libsu`)** pour accéder aux répertoires protégés (`/Android/data`, `/Android/obb`, partition système).
* **Corbeille intégrée :** Suppression sécurisée avec restauration en un appui ou vidage définitif.
* **Propriétés avancées :** Affichage détaillé des métadonnées et calcul des empreintes **MD5** et **SHA-256**.
* **Interface bilingue :** Bascule instantanée entre **Français** et **Anglais** avec thèmes clair, sombre ou système.

### Gestion avancée des archives
* **Formats supportés :**
  * **Création :** `ZIP` (standard ou chiffré), `7Z` et `TAR.GZ`.
  * **Extraction :** `ZIP`, `7Z`, `RAR`, `TAR`, `TAR.GZ` (`TGZ`), `BZ2` et `XZ`.
* **Chiffrement AES-256 :** Protection par mot de passe lors de la compression au format ZIP (`Zip4j`) et déchiffrement des archives `ZIP` et `7Z` protégées.
* **Découpage et réassemblage multi-volumes :** Découpage automatique des archives en plusieurs parties (`archive.ext_1`, `archive.ext_2`…) selon une taille maximale définie (en Mo) et réassemblage transparent à l'extraction.
* **Exécution en arrière-plan (`ForegroundService`) :** Compression et décompression maintenues actives écran éteint, avec barre de progression en direct dans l'application et notifications interactives cliquables.

### Réseau FTP
* **Serveur & client FTP intégrés :** Partage de fichiers sans fil sur le réseau local directement depuis l'onglet **FTP**.

### Intégration système Android & LSPosed
* **Ouverture directe depuis les navigateurs :** Apparaît dans le menu *« Ouvrir avec »* lors du clic sur une archive téléchargée (`.zip`, `.7z`, `.rar`, `.tar.gz`…) pour proposer immédiatement son extraction.
* **Sélecteur de fichiers (`ACTION_GET_CONTENT`) :** Accessible depuis le menu latéral *« Ouvrir à partir de »* du sélecteur Android pour joindre des fichiers dans n'importe quelle application.
* **Module LSPosed intégré (optionnel) :** Aucun APK séparé requis. Sur les appareils équipés de **LSPosed**, l'activation du module permet de rediriger automatiquement les appels système (`OPEN_DOCUMENT`, `GET_CONTENT`, `PICK_IMAGES`, `ACTION_PICK`) des applications cochées directement vers **MonExplorateur** — tout en restant totalement transparent pour les utilisateurs sans Root.

---

## Installation et mises à jour

1. Téléchargez le dernier APK signé depuis l'onglet [**Releases**](../../releases).
2. Pour recevoir automatiquement les mises à jour et lire les notes de version, ajoutez l'URL du dépôt dans [**Obtainium**](https://github.com/ImranR98/Obtainium).

### Activation du module LSPosed (facultatif)
1. Ouvrez **LSPosed → Modules → MonExplorateur** et activez le module.
2. Cochez les applications dans lesquelles vous souhaitez remplacer le sélecteur de fichiers système.
3. Forcez l'arrêt des applications cochées pour appliquer la redirection.