# BBSmartBridge

Une application native complète pour transformer le BlackBerry Bold 9790 en compagnon intelligent.

## Fonctionnalités Implémentées :
* **Notifications :** Réception des notifications (WhatsApp, etc.), historique limité en RAM pour préserver le téléphone, et bouton "Reply" utilisant le clavier QWERTY.
* **Appels Téléphoniques :** Interception d'appels entrants, interface native plein écran "Incoming Call", avec mapping direct sur les touches matérielles **Verte** (`Keypad.KEY_SEND`) et **Rouge** (`Keypad.KEY_END`).
* **Hardware natif BBOS :** Déclenchement de la vibration (`Alert.startVibrate`) et du voyant LED clignotant rouge/vert selon le type d'alerte (`LED.setConfiguration`).
* **Bluetooth Robuste :** Serveur SPP (`btspp://localhost:...`) tournant dans un thread isolé avec gestion avancée des erreurs (dépassement de tampon, timeout) et reconnexion automatique.
* **Sauvegarde Persistante :** `PersistentStore` de J2ME utilisé pour les paramètres.

## Compilation 
### Locale (Windows)
Nécessite **BlackBerry JDE 7.1.0**.
Lancez le script : `build-blackberry.bat`

### Via GitHub Actions
Nécessite d'installer un **Self-Hosted Runner Windows** lié à votre JDE 7.1 local (cf. `GITHUB_BUILD.md`).

## Installation et Test (PING/PONG)
1. Installez `.cod` et `.jad` sur le Bold 9790.
2. Démarrez l'application (elle écoutera les requêtes Bluetooth).
3. Connectez un terminal SPP Bluetooth depuis Android.
4. Envoyez `PING\n`. Le BlackBerry répondra automatiquement `PONG\n` dans le flux Bluetooth série.
