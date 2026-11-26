# 📁 MonExplorateur

**Gestionnaire de fichiers Android moderne, complet et open-source (Material 3 / Jetpack Compose)**  
*Modern, full-featured, open-source Android file manager built with Material 3 & Jetpack Compose*

---

## ✨ Fonctionnalités principales / Key Features

- **Exploration complète du stockage / Full Storage Browsing**
  - Navigation fluide dans le stockage interne (`/storage/emulated/0`), cartes SD et dossiers système.
  - Sélection multiple (appui long), copier, couper, coller, renommer et création de dossiers.
  - Recherche instantanée dans le répertoire courant et tri flexible (nom, date, taille).

- **Accès privilégiés à la demande / Privileged Access On-Demand**
  - **Mode Shizuku** : accès direct en lecture/écriture à `/Android/data` sans root (activable à la demande depuis les Réglages).
  - **Mode Root (`su`)** : exploration complète de la partition racine `/` et des dossiers système.

- **Archives & Compression multi-formats / Multi-Format Archives**
  - Compression et extraction natives : **ZIP (`.zip`)**, **7-Zip (`.7z`)**, **TAR.GZ (`.tar.gz` / `.tgz`)**.
  - Extraction des archives **RAR (`.rar`)**.

- **Corbeille intégrée / Built-in Trash Bin**
  - Suppression sécurisée avec restauration en un clic vers l'emplacement exact d'origine, ou suppression définitive.

- **Réseau FTP intégré / Built-in FTP Server & Client**
  - **Serveur FTP Wi-Fi** : transformez votre téléphone en serveur FTP pour transférer vos fichiers depuis un PC sans câble.
  - **Client FTP** : connexion directe à des serveurs FTP distants pour parcourir et télécharger vos fichiers.

- **Inspecteur de fichiers & Empreintes / File Inspector & Checksums**
  - Calcul d'empreintes cryptographiques **MD5**, **SHA-1** et **SHA-256** sur n'importe quel fichier.
  - Éditeur de texte intégré et lecteur multimédia/visionneuse.

- **Personnalisation / Customization**
  - Thème **Material 3** adaptatif : **Clair**, **Sombre** ou **Système**.
  - Interface bilingue **Français / English** commutable instantanément dans les Réglages.

---

## 📲 Installation

1. Rendez-vous dans l'onglet [**Releases**](../../releases) de ce dépôt.
2. Téléchargez le fichier `MonExplorateur-v1.0.0.apk`.
3. Installez l'APK sur votre appareil Android (Android 8.0+ / API 26+) et accordez l'autorisation d'accès à tous les fichiers (`MANAGE_EXTERNAL_STORAGE`).

---

## 🛠️ Stack Technique / Tech Stack

- **Langage** : Kotlin 2.0 & Coroutines
- **Interface** : Jetpack Compose + Material Design 3
- **Privilèges** : Rikka Shizuku API (UserService AIDL) & `libsu` (Root)
- **Archives** : Apache Commons Compress, XZ for Java, Junrar
- **Réseau** : Apache FTPServer & Apache Commons Net
- **CI/CD** : GitHub Actions (compilation et signature automatique des releases)

## Télécharger & Mises à jour automatiques

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/JulieRose23/MonExplorateur"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="54" alt="Get it on Obtainium" /></a>

