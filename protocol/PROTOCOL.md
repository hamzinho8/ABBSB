# BlackBerrySmartBridge Protocol (BSB/1)

Le protocole BSB/1 est un protocole texte transitant via Bluetooth SPP (Serial Port Profile).

- **Encodage** : UTF-8
- **Fin de message** : `\n` (Line Feed)
- **Séparateur d'arguments** : `|`

## 1. Handshake & Heartbeat

### PING / PONG
Utilisé pour vérifier la connexion.
- `PING` (Envoyé par Android)
- `PONG` (Réponse du BlackBerry)

### HELLO (Négociation)
Envoyé par Android : `HELLO|BSB/1|ANDROID`
Réponse BlackBerry : `HELLO|BSB/1|BLACKBERRY_9790` puis `READY`

## 2. Notifications

### NOTIFICATION (Android -> BB)
Transfère une notification vers l'écran du BlackBerry.
`NOTIFICATION|id_unique|nom_application|expéditeur|contenu_du_message`

### REPLY (BB -> Android)
L'utilisateur tape une réponse avec le clavier QWERTY.
`REPLY|id_unique|texte_de_reponse`

## 3. Appels Téléphoniques

### CALL_INCOMING (Android -> BB)
`CALL_INCOMING|id_unique|Nom_Contact|Numero`

### CALL_ACTIVE (Android -> BB)
`CALL_ACTIVE|id_unique`

### CALL_END (Android -> BB)
`CALL_END|id_unique`

### CALL_MISSED (Android -> BB)
`CALL_MISSED|id_unique|Nom_Contact|Numero`

### CALL_ANSWER (BB -> Android)
L'utilisateur appuie sur la touche verte.
`CALL_ANSWER|id_unique`

### CALL_REJECT (BB -> Android)
L'utilisateur appuie sur la touche rouge.
`CALL_REJECT|id_unique`

### CALL (BB -> Android)
L'utilisateur initie un appel depuis les contacts.
`CALL|numero`

## 4. Contacts

### CONTACTS_REQUEST (BB -> Android)
`CONTACTS_REQUEST`

### CONTACT (Android -> BB)
`CONTACT|id_unique|Nom|Numero`

## 5. Média (BB -> Android)
`MEDIA_PLAY`
`MEDIA_PAUSE`
`MEDIA_NEXT`
`MEDIA_PREVIOUS`

## 6. Batterie
`PHONE_BATTERY|niveau`
`BATTERY|niveau` (BB -> Android)

## 7. Erreurs
- `ERROR|UNKNOWN_COMMAND`
- `ERROR|INVALID_PACKET`
- `ERROR|MESSAGE_TOO_LONG`
