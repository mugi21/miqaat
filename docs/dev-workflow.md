# Workflow de développement

## Commandes

```powershell
.\gradlew.bat assembleDebug           # APK debug
.\gradlew.bat :app:testDebugUnitTest  # tests JVM (rapides)
.\gradlew.bat installDebug            # installer sur l'appareil connecté
.\gradlew.bat clean
```

APK : `app/build/outputs/apk/debug/app-debug.apk`.

### Build lancé depuis Claude Code (cette machine uniquement)

Les sockets AF_UNIX sont bloquées → le JDK ≥ 16 échoue (« Unable to establish
loopback connection »). Il faut forcer le repli TCP **sur les trois JVM**
(client Gradle, daemon Gradle, daemon Kotlin) :

```powershell
$fix = "-Djdk.net.unixdomain.tmpdir=C:\claude-afunix-fallback-inexistant"
$env:GRADLE_OPTS = $fix
$env:JAVA_TOOL_OPTIONS = $fix   # indispensable : hérité par les daemons
.\gradlew.bat <tâche> "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 $fix" "-Pkotlin.daemon.jvmargs=-Xmx1024m $fix"
```

Si « A new daemon was started but could not be connected to » : `.\gradlew.bat --stop`, puis relancer.
Android Studio et un terminal normal ne sont pas concernés.

## Rituel de session

1. Lire `CLAUDE.md` (surtout « État actuel ») et les docs pertinentes de `docs/`.
2. Coder.
3. `:app:testDebugUnitTest` doit rester vert ; ajouter des tests pour toute
   logique métier nouvelle (le domaine est du JVM pur, donc testable sans émulateur).
4. Mettre à jour :
   - `CLAUDE.md` → section « État actuel » (obligatoire en fin de session) ;
   - `docs/decisions.md` → si un choix d'architecture notable a été fait ;
   - `docs/file-map.md` et `docs/INDEX.md` → si des fichiers importants sont apparus.

## Conventions de code

- Kotlin idiomatique. Classes et fonctions en **anglais**, textes UI en ressources.
- `domain/` n'importe **jamais** d'API Android : c'est ce qui le rend testable en JVM pur.
  Le cas limite utile : une formule (horaires, Qibla) va dans `domain/`, la lecture
  des capteurs ou de la position va dans `data/`.
- Un écran (`XxxScreen`) reçoit son état du ViewModel ; les composables enfants
  reçoivent des données simples, jamais le ViewModel.
- Compose : `start`/`end`, jamais `left`/`right` — le RTL doit marcher tout seul.
- Aucun texte en dur dans un composable : tout passe par `strings.xml`
  (voir [i18n.md](i18n.md)).
- Couleurs : toujours `MaterialTheme.colorScheme.*`, jamais de `Color(0xFF…)` dans
  l'UI — c'est ce qui garantit les modes clair et sombre, y compris pour les dessins Canvas.
- Dépendances déclarées dans `gradle/libs.versions.toml` (version catalog).
- Pas de framework de DI : des `by lazy` sur `MiqaatApp` suffisent à cette échelle.

## Contraintes permanentes

- **Offline-first** : aucune fonctionnalité cœur ne dépend du réseau.
- Aucun SDK de tracking, d'analytics ou de publicité.
- Les notifications de prière ne doivent jamais être en retard (alarmes exactes,
  replanification après reboot).
