# Application du Design Moderne SmartWatch sur l'Application BlackBerry (Curve 9300)

Transposer fidèlement le design haute fidélité visualisé dans la Preview Web sur l'application native BlackBerry OS 5.0/6.0 (Java RIM J2ME / Curve 9300), avec la barre d'état cyan/jaune/verte, l'horloge numérique géante, la grille 3x2 avec bordures arrondies lumineuses et le thème sombre étendu aux écrans d'appel et d'historique.

## User Review & Critical Decisions

> [!IMPORTANT]
> Les trois choix confirmés lors de la phase de clarification sont intégrés :
> 1. **Fidélité visuelle** : Reproduction exacte de l'écran principal avec barre d'état bicolore, grande horloge blanche centrée, statut double SIM et grille 3x2.
> 2. **Périmètre du thème** : Application cohérente du thème sombre et cyan sur l'écran d'accueil (`SmartBridgeScreen`), l'écran d'appel en cours (`PhoneCallScreen`), et création de l'écran natif d'historique des appels (`CallHistoryScreen`).
> 3. **Rendu graphique des boutons** : Rendu vectoriel personnalisé dans `DarkButtonField` avec coins arrondis (rayon 8-10px), bordures colorées par fonction (cyan pour Calls/History, ardoise pour Messages/Notifs, doré pour HFP), et effet de halo lumineux réactif au trackpad optique.

- **Décision 1 (Confirmée)** : Interface d'accueil conforme à l'écran 320x240 du Curve 9300 avec disposition compacte sans dépassement vertical.
- **Décision 2 (Confirmée)** : Ajout d'un écran natif d'historique d'appels (`CallHistoryScreen`) accessible directement depuis la touche "History" de la grille.
- **Décision 3 (Confirmée)** : Optimisation des contrastes et des polices système RIM pour une lisibilité maximale en plein jour sur l'écran LCD du Curve 9300.

---

## 1. Vue d'Ensemble & Objectifs

- **Ce que fait la mise à jour** : Rehausse le design de l'application native BlackBerry OS (`BBSmartBridge.cod`) pour lui donner l'apparence moderne d'un compagnon SmartWatch haut de gamme, synchronisé avec le smartphone Android.
- **Plateforme cible** : BlackBerry Curve 9300 (écran 320x240, RIM OS 5.0/6.0, CLDC 1.1).
- **Valeur ajoutée** : Expérience visuelle fluide et élégante, retour haptique et visuel au trackpad, informations clés (HFP, SIM, Batterie, Heure) immédiatement visibles.

---

## 2. Expérience Utilisateur & Design Graphique

### Palette de Couleurs & Tokens RIM Graphics (Hex)
- **Fond d'écran global** : Noir pur `0x000000` (contraste maximal LCD / économie d'énergie).
- **Barre d'état supérieure** :
  - Fond : `0x11161B` avec ligne de démarcation `0x1F2937`
  - Statut Bluetooth : `[HFP/SPP]` en Cyan électrique `0x00E5FF`
  - Statut Réseau & SIM : `[4G] inwi - Signal: [||||]` en Jaune/Doré `0xFACC15`
  - Batterie BlackBerry : `[BB: 88%]` en Vert émeraude `0x22C55E`
- **Zone Horloge & Date** :
  - Heure principale : `0xFFFFFF` (Blanc pur), police 28-30pt gras centrée
  - Date : `0x94A3B8` (Gris ardoise clair), police 11pt
  - Double SIM : `Double SIM Active : [inwi | Orange]` en Cyan vif `0x38BDF8`
  - Mains-libres SCO : `Audio Mains-Libres Prêt (SCO)` en Vert néon `0x4ADE80`
- **Boutons de la Grille 3x2 (96x28 px)** :
  - *Calls* & *History* : Fond `0x16202A`, bordure cyan `0x0284C7`, texte `0x38BDF8`
  - *Messages*, *Notifs*, *WhatsApp* : Fond `0x1E242B`, bordure `0x374151`, texte `0xD1D5DB`
  - *Audio HFP* : Fond `0x262015`, bordure ambrée `0xB45309`, texte `0xFDE047`
  - *Focus Trackpad (Surbrillance)* : Fond illuminé `0x0369A1` / `0x0E7490`, bordure cyan néon double trait `0x00F0FF`, texte blanc éclatant `0xFFFFFF`.
- **Ligne d'Aide Basse** :
  - Texte discret `0x64748B` : `Touche Verte pour composer | Menu pour options HFP`

---

## 3. Architecture Technique & Modifications Java RIM

```
┌────────────────────────────────────────────────────────────────────────┐
│                   BlackBerry Curve 9300 (320x240)                      │
│                                                                        │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ TopStatusBarManager: [HFP/SPP]   [4G] inwi [||||]   [BB: 88%]      │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│                     14:42 (Font.BOLD, 30pt White)                      │
│                   Jeudi 24 Septembre 2026 (Slate)                      │
│              Double SIM Active : [inwi | Orange] (Cyan)                │
│                 Audio Mains-Libres Prêt (SCO) (Green)                  │
│                                                                        │
│ ┌──────────────────────┬──────────────────────┬──────────────────────┐ │
│ │  📞 Calls (Cyan)     │  📜 History (Cyan)   │  ✉️ Messages (Slate) │ │
│ ├──────────────────────┼──────────────────────┼──────────────────────┤ │
│ │  🔔 Notifs (Slate)   │  💬 WhatsApp (Slate) │  🎧 Audio HFP (Gold) │ │
│ └──────────────────────┴──────────────────────┴──────────────────────┘ │
│                                                                        │
│         Touche Verte pour composer | Menu pour options HFP             │
└────────────────────────────────────────────────────────────────────────┘
```

### Composants Java RIM à Moderniser :
1. **`DarkButtonField.java`** :
   - Ajout du support des styles thématiques (Cyan, Slate, Gold/Warning).
   - Peinture personnalisée `Graphics.fillRoundRect` et `Graphics.drawRoundRect` avec coins adoucis (rayon 8).
   - Halo lumineux de sélection au trackpad avec surépaisseur de bordure.
2. **`SmartBridgeScreen.java`** :
   - Réorganisation du layout pour reproduire exactement la hiérarchie visuelle de la preview (Statuts alignés, Horloge 30pt, Textes colorés, Grille 3x2 de dimensions 96x28 px).
   - Intégration du bouton `History` ouvrant le journal d'appels natif.
   - Pied d'écran avec raccourcis physiques.
3. **`CallHistoryScreen.java`** *(Nouveau)* :
   - Écran de consultation des appels récents sur BlackBerry.
   - Liste déroulante des appels entrants et sortants avec nom, numéro, heure, durée et badge SIM.
   - Raccourci au clic trackpad ou touche verte pour rappeler immédiatement le contact.
4. **`PhoneCallScreen.java`** :
   - Mise à niveau graphique : Carte sombre avec halo vert d'appel actif, chrono grand format, badges de routage audio (`Bluetooth SCO / BB` vs `HP Tel`).
5. **Compilation & Packaging** :
   - Exécution de `build.sh` (javac 1.3, Proguard preverify, RAPC RIM) pour générer les nouveaux exécutables `BBSmartBridge.cod` et `BBSmartBridge.jad` prêts pour installation.

---

## 4. Vérification & Tests
- Compilation sans avertissement via le SDK RIM JDE 5.0.
- Vérification du respect des contraintes mémoires et de dimensions (320x240 pixels exacts, aucun débordement).
- Test du focus trackpad et des raccourcis matériels (Touche verte, rouge, menu, échap).
