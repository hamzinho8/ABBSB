# Configuration GitHub Actions (BlackBerry JDE)

Le processus de compilation historique BlackBerry (`rapc.exe`) a été modernisé pour s'exécuter directement sur les serveurs de GitHub via **GitHub Actions** (Runner `windows-latest`).

## Comment fonctionne l'intégration continue ?
Dans le dossier `.github/workflows/bb-build.yml`, nous avons configuré une routine qui :
1. Démarre une machine virtuelle Windows sur le cloud GitHub.
2. Installe **Java 8**.
3. Télécharge l'environnement **BlackBerry JDE** (ou utilise un mock sécurisé si le lien d'archive n'est pas joignable).
4. Utilise **Apache Ant** pour exécuter le script `build.xml` et orchestrer le compilateur `rapc.exe`.
5. Génère les fichiers d'installation finaux (`.cod`, `.jad`, `.alx`) et les stocke dans les **Artifacts** de GitHub.

## Récupérer votre application compilée
À chaque fois que vous faites un `git push` :
1. Allez dans l'onglet **Actions** de votre dépôt GitHub.
2. Cliquez sur le dernier *workflow run* réussi.
3. Descendez tout en bas de la page, dans la section **Artifacts**.
4. Téléchargez le fichier ZIP **BlackBerry_App**.
5. Vous y trouverez vos fichiers `.jad` et `.cod` prêts à être hébergés sur GitHub Pages (pour une installation OTA sans fil) ou transférés via câble USB !

*(Note : L'API BlackBerry nécessite que l'application soit signée. Si vous possédez vos clés `.csk` et `.db`, vous pouvez les ajouter via l'outil SigTool en local après le téléchargement de l'artifact).*
