# Miqaat — Mémoire du projet

## Vision

Application Android native de temps de prière (Salat) :

- **100 % gratuite**, sans publicité, sans achat intégré
- **Offline-first** : aucune dépendance réseau pour les fonctionnalités cœur (calculs 100 % locaux via la librairie Adhan)
- **Aucun SDK** de tracking, analytics ou publicité
- Fiabilité absolue des notifications de prière : elles ne doivent **jamais** être en retard, même app fermée, téléphone en Doze, ou après un reboot
- Langue principale : **arabe** (UI RTL) ; français et anglais viendront avec le multilingue

## Stack et décisions techniques

| Choix | Raison |
|---|---|
| **Android natif (Kotlin)** plutôt que Flutter | Fiabilité des alarmes exactes (`AlarmManager`) et des notifications en arrière-plan — critique pour une app de prière, mal supporté par les frameworks cross-platform |
| **Jetpack Compose + Material 3** | UI déclarative moderne, standard actuel d'Android ; excellent support RTL natif |
| **Adhan** (`com.batoulapps.adhan:adhan2`, batoulapps/adhan-kotlin) | Calcul astronomique des temps de prière 100 % local, aucun appel réseau, librairie de référence maintenue |
| **AlarmManager seul** (alarmes exactes) | `setExactAndAllowWhileIdle` pour la ponctualité même en Doze ; la chaîne se replanifie elle-même, et une seconde alarme inexacte sert de chien de garde. **Pas de WorkManager** : travail déférable, donc incapable de garantir la ponctualité — voir D33 |
| **Media3 / ExoPlayer** (écoute du Coran seulement) | Seule dépendance lourde acceptée : buffering d'un flux instable, focus audio, notification média et écran verrouillé. Voir D41 et D42 |
| **`HttpURLConnection` + `org.json`** pour tout le réseau (mp3quran, GitHub) | Deux à trois appels GET sans authentification ne justifient pas Retrofit/OkHttp/Moshi. Le parseur est toujours un `object` pur prenant une `String`, donc testable en JVM |
| **Room** | Stockage local : paramètres, cache des coordonnées, futur tracker de prières |
| **MVVM + ViewModel + StateFlow** | Architecture standard Android, testable, survit aux rotations |
| `minSdk 26` | java.time et `HijrahDate` disponibles nativement ; couvre ~95 % du parc |
| AGP 9.x | Kotlin est intégré à AGP 9 : pas de plugin `org.jetbrains.kotlin.android` séparé ; syntaxe `compileSdk { version = release(36) }` — ne pas la « corriger » |
| Méthode de calcul : **automatique selon le pays** (repli Muslim World League) | Chaque pays suit la méthode de son ministère des Affaires religieuses ; celles absentes d'Adhan sont définies dans `MethodOption` (angles de l'API AlAdhan) |
| Ville de test en dur : **Skikda, Algérie** (36.8665°N, 6.9063°E, `Africa/Algiers`) | Temporaire, en attendant la géolocalisation réelle |
| Textes UI dans `strings.xml`, **arabe = langue par défaut** (`values/`) | FR/EN seront des traductions (`values-fr/`, `values-en/`) — sens naturel du système de ressources Android |

## Architecture des packages (`com.mohamed.miqaat`)

```
domain/          Logique métier pure (sans dépendance Android → testable en JVM)
  model/         PrayerName, DailyPrayerTimes
  PrayerTimesCalculator.kt
data/            Sources de données
  location/      LocationRepository (en dur Skikda pour l'instant ; interface prête pour la géoloc)
ui/              Compose + ViewModels (MVVM, StateFlow)
  theme/         Thème Material 3
  home/          HomeScreen, HomeViewModel, HomeUiState
notifications/   Alarmes exactes, receivers, canaux, service sonore
quran/           Lecture du Coran (MediaSessionService Media3, contrôleur)
widget/          Widget d'écran d'accueil (RemoteViews)
```

## Roadmap

### MVP (en cours)
1. ✅ Calcul des temps de prière hors ligne (Adhan) — coordonnées en dur pour l'instant
2. ✅ Écran principal : 5 prières du jour, prochaine prière en évidence, compte à rebours, dates Hijri + Grégorienne
3. ✅ Notifications à l'heure exacte (alarmes exactes, fiables même app fermée) — sonnerie « حيّ على الصلاة » fournie par l'utilisateur (canal `prayer_times_v2`)
4. ✅ Fonctionnement 100 % hors ligne après configuration initiale (géoloc réelle + cache Room)

### v1.1
✅ Réglages (méthode de calcul, madhab, ajustement Hijri) · ✅ méthodes nationales + sélection automatique selon le pays · ✅ boussole Qibla · ✅ widget écran d'accueil · ✅ rappels avant la prière · ✅ vue calendrier mensuel + horaires Ramadan · ✅ invocations et adhkār (livrées + créées par l'utilisateur, rappel configurable) · ⬜ sélection manuelle de ville

### v1.2
✅ écoute du Coran (mp3quran, récitateurs + favoris, pause automatique à l'adhan, sourate du moment) · ⬜ tracker de prières · ⬜ événements du calendrier islamique

### v1.3
✅ mise à jour de l'app depuis GitHub Releases (note d'accueil, téléchargement et installation dans l'app, opt-out) — **temporaire**, jusqu'à une publication sur Google Play

### Plus tard
Multilingue (FR/AR/EN) · sons d'adhan personnalisés · statistiques

## Contraintes techniques permanentes

- Permissions : `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, `POST_NOTIFICATIONS` (Android 13+), localisation — à demander proprement avec explications
- `INTERNET` depuis la session 14, pour **deux** fonctionnalités seulement, dont aucune n'est cœur : l'écoute du Coran (mp3quran) et la mise à jour depuis GitHub, celle-ci coupable par l'utilisateur. Aucune fonction cœur n'y touche, aucun SDK de tracking, aucune donnée envoyée (voir D41 amendée par D44)
- `BOOT_COMPLETED` → replanifier toutes les alarmes après reboot
- Doze mode : utiliser `setExactAndAllowWhileIdle` / `setAlarmClock` ; les notifications ne doivent JAMAIS être en retard
- Aucune dépendance réseau pour les fonctionnalités cœur ; aucun SDK tiers de tracking

## Documentation

Depuis la session 5, `docs/` complète ce fichier — voir [docs/INDEX.md](docs/INDEX.md) :
`dev-workflow.md` (build, rituel, conventions), `decisions.md` (choix d'architecture
et leurs raisons), `file-map.md` (carte des fichiers), `i18n.md` (multilingue),
`notifications.md` (chaîne, canaux, mode d'alerte), `reliability.md` (pourquoi
l'adhan n'arrive pas et comment le réparer), `prayer-times-accuracy.md` (coller à un
calendrier officiel : arrondi, marge de précaution, protocole de mesure).
`quran.md` (l'API mp3quran et ses pièges, le cache, le lecteur, la sourate du moment),
`release.md` (publier une version) et `updates.md` (ce que l'app attend d'une release
GitHub pour se mettre à jour, et la friction MIUI à l'installation).
`CLAUDE.md` reste la mémoire vivante : vision, stack, roadmap, État actuel.

## État actuel

**Dernière mise à jour : 2026-08-18 (fin de session 15)**

### Fait (session 1)
- Squelette Android Studio (AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, minSdk 26, targetSdk 36)
- CLAUDE.md créé
- Adhan 0.0.5 intégré ; `PrayerTimesCalculator` (domain, pur JVM) + `DailyPrayerTimes`/`PrayerName`
- `FixedLocationRepository` (Skikda en dur, derrière une interface)
- `HomeScreen` + `HomeViewModel` (MVVM/StateFlow) : 6 horaires en arabe, prochaine prière en évidence, ville + date grégorienne (locale ar-DZ)
- 3 tests unitaires verts (référence api.aladhan.com ±3 min, ordre chronologique, logique nextPrayer)

### Fait (session 2) — refonte UI/UX verte
- **Thème vert émeraude** (graine `#1B6B4C`) : palettes M3 complètes light + dark dans `Color.kt`/`Theme.kt`, `dynamicColor = false` (identité de marque), `themes.xml` + `values-night/` avec `windowBackground` (plus de flash au lancement)
- **Police IBM Plex Sans Arabic** embarquée (`res/font/`, 3 graisses, SIL OFL), `Typography` complète (letterSpacing 0, lineHeight ~1,5× pour les diacritiques)
- **Nouveau HomeScreen** : `HeroSection` (dégradé, ville + dates Hijri/grégorienne, prochaine prière en grand + compte à rebours) + `PrayerList` (une seule surface arrondie 28dp sans ombre, prochaine en pastille primaryContainer, passées et shurūq atténués)
- **Domaine** : `NextPrayerResolver` (après Isha → Fajr du lendemain, jamais null), `HijriFormatter` (Umm al-Qura, ar-DZ), `formatCountdown`
- **ViewModel réactif** : flow froid + `stateIn(WhileSubscribed)` — tick 1 s aligné sur l'horloge, s'arrête en arrière-plan, état frais au retour ; horaires du jour/lendemain mémorisés (recalcul au changement de date seulement)
- 10 tests JVM verts (7 nouveaux : resolver, Hijri, countdown) ; vérifié sur émulateur en light + dark (RTL, tick, pastille)

### Fait (session 2, suite) — notifications de prière
- Package `notifications/` : `PrayerNotificationChannel` (canal `prayer_times_v1`, remplacé par `NotificationChannels` en session 7, IMPORTANCE_HIGH, son custom + vibration — le canal suit automatiquement le mode du téléphone : sonnerie/vibreur/silencieux), `PrayerAlarmScheduler` (une alarme exacte à la fois, `setExactAndAllowWhileIdle`, repli `setAlarmClock` si permission révoquée), `PrayerAlarmReceiver` (notifie puis replanifie la suivante — chaîne), `RescheduleReceiver` (BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED)
- `MiqaatApp` (Application) crée le canal ; `MainActivity` demande `POST_NOTIFICATIONS` (13+) et resynchronise la chaîne à chaque ouverture
- Manifest : `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` (maxSdk 32), `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` + les 2 receivers (`exported=false`)
- **Son provisoire** : carillon 2 notes généré (`res/raw/prayer_notification.wav`) — à remplacer par un enregistrement « حيّ على الصلاة، حيّ على الفلاح » fourni par l'utilisateur ; ⚠ au changement de son, bumper l'ID du canal en `prayer_times_v2` (Android fige les réglages d'un canal créé)
- Vérifié sur émulateur : alarme exacte enregistrée (`dumpsys alarm`, window=0, `policy_permission`), notification arabe OK (section sonore), **re-planification après reboot sans ouvrir l'app**
- Note test : le shell adb ne peut pas viser un receiver non exporté (Permission Denial) — utiliser `adb root` puis `am broadcast -n com.mohamed.miqaat/.notifications.PrayerAlarmReceiver --es prayer ASR`

### Fait (session 3) — son adhan + géolocalisation + cache Room (MVP terminé)
- **Son réel** : enregistrement « حيّ على الصلاة، حيّ على الفلاح » fourni par l'utilisateur (WAV 16 kHz mono ~31 s) dans `res/raw/prayer_notification.wav` ; canal bumpé en `prayer_times_v2`, l'ancien v1 est supprimé au lancement (`OLD_IDS`)
- **Room 2.8.4 + KSP 2.2.10-2.0.2** : `data/db/` (`MiqaatDatabase`, `CachedLocationEntity` singleton id=1, `LocationDao` avec `@Upsert`) ; ⚠ AGP 9 + KSP exige `android.disallowKotlinSourceSets=false` dans gradle.properties (message d'erreur AGP officiel)
- **Géolocalisation native sans Play Services** : `DeviceLocationDataSource` (LocationManager, fix ponctuel coarse, lastKnown < 30 min sinon `getCurrentLocation`/`requestSingleUpdate`, timeout 15 s), `CityNameResolver` (Geocoder locale ar, best-effort), `CachedLocationRepository` (mémoire → Room → défaut Skikda ; `refresh()` = fix + géocodage + upsert)
- **Câblage** : singletons lazy dans `MiqaatApp` (`database`, `locationRepository`, extension `Context.miqaatApp`) — pas de framework DI ; `HomeViewModel` reçoit le repository (créé via `viewModel { }` dans HomeScreen), cache horaires invalidé si date OU position change ; scheduler branché sur le même repository
- `MainActivity` : demande groupée POST_NOTIFICATIONS + ACCESS_COARSE_LOCATION, refresh position à chaque ouverture si permise puis replanification
- Vérifié sur émulateur : fix simulé Alger → ville « بير مراد رايس » géocodée en arabe, horaires décalés de ~+15 min (longitude), alarme replanifiée 16:41 ; **hors ligne total** (réseau coupé + permission révoquée + force-stop) → tout persiste depuis Room

### Fait (session 3, suite) — écran de réglages
- **DataStore Preferences 1.2.1** : `data/settings/SettingsRepository` (même patron que la position : instantané mémoire pour les lecteurs synchrones + Flow pour l'UI ; les setters rafraîchissent aussi le cache mémoire)
- **Domaine** : `CalculationSettings` (méthode, madhab, décalage Hijri borné ±2), `PrayerTimesCalculator.calculate(..., method, madhab)` via `parameters.copy(madhab)`, `HijriFormatter.format(date, offsetDays)`
- **UI** : `ui/settings/` (SettingsScreen + SettingsViewModel) — 11 méthodes de calcul en arabe (dialogue radio), madhab jumhur/hanafi, stepper Hijri ; navigation par simple bascule d'état dans MainActivity (`showSettings` + BackHandler, pas de librairie) ; engrenage sur l'accueil
- Tout changement de réglage **replanifie l'alarme** (callback `onSettingsChanged` → `PrayerAlarmScheduler.scheduleNext()`) ; le tick de l'accueil recalcule si (date, position, réglages) change
- ⚠ Le BOM Compose 2026 ne fournit plus `material-icons` avec material3 → icônes vectorielles custom (`ic_settings.xml`, `ic_arrow_back.xml` avec `autoMirrored` pour le RTL)
- 13 tests JVM verts (3 nouveaux : Asr hanafi > jumhur, Fajr égyptien < MWL, décalage Hijri exact) ; vérifié sur émulateur : madhab hanafi → Asr 16:41→17:49 et alarme système replanifiée à 17:49:00, retour jumhur OK

### Fait (session 4) — méthodes de calcul nationales + sélection automatique
- **`domain/model/MethodOption`** : enum maison remplaçant `CalculationMethod` dans tout le code applicatif — les 11 méthodes Adhan (mêmes `name` → compat DataStore) + **10 méthodes nationales** absentes de la librairie, construites via `CalculationMethod.OTHER.parameters.copy(...)` : Algérie 18/17, Tunisie 18/18, Maroc 19/17, Jordanie 18/18 + Maghrib +5 min, France UOIF 12/12, Russie 16/15, Indonésie 20/18, Malaisie 20/18, Portugal 18°/Isha 77 min + 3 min, Golfe 19,5°/Isha 90 min (paramètres de l'API AlAdhan `v1/methods`)
- **`domain/AutoMethodResolver`** : mapping pays ISO alpha-2 → méthode (DZ, TN, MA, LY→égyptienne, JO, SA, AE, KW, QA, BH/OM→Golfe, TR, RU, FR, PT, SG, MY, ID, PK/IN/BD, US/CA, GB…) ; extension `CalculationSettings.effectiveMethod(countryCode)` utilisée par `HomeViewModel` et `PrayerAlarmScheduler`
- **Mode auto activé par défaut** (`methodAuto: Boolean = true`, clé DataStore `method_auto`) ; choisir une méthode manuelle l'écrit **et** désactive l'auto dans le même `edit` ; l'entrée « تلقائي — <pays> » en tête du dialogue le réactive (le dernier choix manuel est conservé)
- **Pays de la position** : `GeoLocation.countryCode` + colonne Room (`version = 2` avec `Migration(1,2)` `ALTER TABLE`, pas de fallback destructif) ; `CityNameResolver` renvoie `ResolvedPlace(cityName, countryCode)` (`Address.countryCode`) ; repli 100 % hors ligne `android.icu.util.TimeZone.getRegion(zoneId)` dans `CachedLocationRepository`
- ⚠ Adhan n'a pas d'angle Maghrib → méthode de Téhéran non implémentable (IR → MWL) ; ⚠ `ishaInterval` s'applique au coucher **brut**, d'où l'ajustement `isha = +3` du Portugal en plus du `maghrib = +3`
- Libye et Oman : pas d'entrée dédiée (paramètres officiels non documentés de façon fiable) → rattachés au standard régional dans le mapping auto
- 27 tests JVM verts (11 nouveaux : angles/ajustements de chaque nationale, mapping pays, mode manuel) ; vérifié sur émulateur : migration Room 1→2 sans perte, « تلقائي — الجزائر » par défaut, Maroc manuel → Fajr 04:19→04:12, position TN → « تلقائي — تونس » et Fajr 03:43→03:50 / Isha 20:53→21:00, alarme replanifiée à chaque changement
- Note test : cet émulateur (snapshot restauré) ignore `adb emu geo fix` — son provider GPS reste `ProviderRequest[OFF]`. Pour simuler un pays : `settings put secure location_mode 0` (aucun fix → `refresh()` renvoie false) puis écrire directement dans Room via `run-as … sqlite3` (le quoting imbriqué passe par l'outil Bash, pas PowerShell)

### Fait (session 5) — boussole Qibla + multilingue ar/fr/en + docs
- **`domain/QiblaCalculator.kt`** (JVM pur) : azimut de la Qibla par le cap initial du grand cercle (`atan2(sin Δλ, cos φ₁·tan φ₂ − sin φ₁·cos Δλ)`), distance haversine à la Kaaba, et les utilitaires d'angles (`normalizeDegrees`, `shortestAngleDelta`, `isAlignedWithQibla` (tolérance 3°), `lerpDegrees` pour le lissage circulaire)
- **`data/compass/CompassDataSource.kt`** : `callbackFlow` sur les capteurs, `TYPE_ROTATION_VECTOR` → repli `TYPE_GEOMAGNETIC_ROTATION_VECTOR` → repli accéléromètre + magnétomètre ; `remapCoordinateSystem` selon la rotation de l'écran ; **`GeomagneticField`** (modèle WMM embarqué, 0 réseau) convertit le cap magnétique en cap **géographique** ; lissage 15 %/événement ; `.conflate()` ; capteurs libérés à l'annulation
- **`ui/qibla/`** : `QiblaScreen` (en-tête, cadran, message d'état unique, angle + distance, vibration à l'alignement), `QiblaCompass` (cadran 100 % Canvas : rose graduée tous les 15°, cardinaux restant droits, aiguille, marque Kaaba, repère fixe au sommet — toutes les couleurs viennent de `colorScheme` donc clair/sombre automatiques), `QiblaViewModel` (position figée à l'ouverture, `flatMapLatest` sur la rotation d'écran, `WhileSubscribed`)
- Sans magnétomètre : l'écran affiche quand même angle + distance ; `uses-feature compass required="false"`. Précision basse → message de calibration (mouvement en 8)
- **Navigation** : `MainActivity` passe d'un booléen à un `enum Screen { HOME, SETTINGS, QIBLA }` ; bouton Qibla en `TopStart` de l'accueil (`ic_qibla.xml`)
- **Multilingue** : `values-fr/` et `values-en/` créés et **traduits intégralement** (l'arabe reste dans `values/`, langue par défaut) ; `res/xml/locales_config.xml` + `android:localeConfig` → sélecteur de langue par app (Android 13+) ; `settings_back` renommé `action_back` (partagé)
- **`docs/`** créé (INDEX, dev-workflow, decisions D1→D13, file-map, i18n)
- 38 tests JVM verts (11 nouveaux : azimuts Skikda/Paris/Jakarta/New York, cas dégénéré plein nord, bornes 0-360, distance, écart d'angle, alignement, lissage, normalisation) ; `assembleDebug` OK. **Pas de test sur émulateur** (capteurs non simulables utilement) — à vérifier sur appareil réel
- ⚠ Dates et nom de ville encore formatés en `ar-DZ` en dur (`HomeViewModel`, `HijriFormatter`, `CityNameResolver`) : à rendre dépendants de la locale pour finir le multilingue

### Fait (session 6) — widget écran d'accueil
- **`widget/NextPrayerWidget.kt`** (`AppWidgetProvider`) + **`widget/NextPrayerWidgetViews.kt`** : widget 3×2 redimensionnable affichant ville · date hégirienne · prochaine prière (nom, heure, décompte) · les cinq horaires du jour avec la prochaine sur pastille ; clic → ouvre l'app. Mosquée de 46dp dans une colonne à part (hauteur pleine) : elle s'agrandit sans allonger la carte
- **Aspect translucide + mosaïque girih** : carte à ~78 % d'opacité (le fond d'écran transparaît), liseré vert et motif `widget_mosaic.xml` — étoiles à huit branches « khātam » (carré + carré tourné de 45°) sur une grille de 60 unités, losanges de remplissage aux croisements ; encarté de 14dp car une `layer-list` ne rogne pas les coins arrondis. ⚠ La taille posée vient du nombre de cellules (`targetCellWidth/Height`), pas de la hauteur du contenu ; un widget déjà posé garde son ancienne taille tant qu'on ne le repose pas

### Fait (session 6, suite) — choix de la langue dans l'app
- **`data/settings/AppLocale.kt`** : `AppLanguage` (système/ar/fr/en) + `wrap(context)` qui habille un `Context` via `createConfigurationContext` (langue **et** sens d'écriture). Décision D16 : pas de `LocaleManager` (API 33+) ni d'AppCompat — un seul chemin de code de l'API 26 à 36, zéro dépendance
- Stockage en **`SharedPreferences`** et non DataStore : `attachBaseContext` s'exécute avant tout et doit être synchrone
- Appliqué par `MainActivity.attachBaseContext` (changement de langue → `recreate()`), et par `AppLocale.wrap()` dans le widget, la notification de prière et la création du canal (idempotente, donc le nom du canal se met à jour dans les réglages système)
- Nouvelle ligne « لغة التطبيق » dans les réglages ; chaque langue est nommée dans sa propre langue (même valeur dans les trois `strings.xml`)
- Le défaut « langue du téléphone » n'habille rien : le sélecteur système d'Android 13+ reste fonctionnel ; un choix dans l'app prime
- **RemoteViews classiques, pas Glance** (décision D14) : pas de dépendance supplémentaire sur une chaîne de build déjà particulière. Comme le lanceur inflate la vue dans **son** processus, on ne lui transmet que du texte et des **identifiants de ressources** → palette `widget_*` dans `values/colors.xml` + `values-night/colors.xml` (recopiée de `Color.kt`), donc clair/sombre suivis automatiquement ; la sélection de la prochaine prière ne change qu'un `setBackgroundResource`, jamais une couleur calculée
- **Compte à rebours sans réveil** : `Chronometer` + `setChronometerCountDown` — il défile côté lanceur sur l'horloge monotone (`elapsedRealtime`), zéro mise à jour, zéro batterie
- **Rafraîchissement branché sur la chaîne d'alarmes** (décision D15) : `PrayerAlarmScheduler.scheduleNext()` se termine par `NextPrayerWidget.refresh()` — donc à l'heure de chaque prière, au reboot, au changement d'heure/fuseau, à l'ouverture de l'app, au rafraîchissement de position et à chaque réglage modifié. `updatePeriodMillis = 30 min` n'est qu'un filet de sécurité. L'appel est placé **après** la pose de l'alarme : la chaîne des notifications ne dépend jamais du widget
- **`res/drawable/ic_mosque.xml`** : mosquée minimaliste (coupole + épi, deux minarets, porte en arc et trois losanges en creux via `fillType="evenOdd"` — la « mosaïque »), couleur `@color/widget_accent` donc thème automatique ; sert aussi d'aperçu (`widget_preview.xml`) pour les lanceurs d'avant Android 12
- Textes `widget_label` / `widget_description` ajoutés dans les **trois** `strings.xml` ; `assembleDebug` OK. **Pas de test sur émulateur** (à poser à la main sur l'écran d'accueil)

### Fait (session 7) — rappels avant la prière
- **`domain/model/ReminderSettings.kt`** : rappel actif (par défaut **oui**) + délai (10 min par défaut). Choix fermés `LEAD_CHOICES = 10, 15, 20, 30, 45, 60` — ⚠ **jamais moins de 10 min** : en Doze, Android n'accorde qu'une alarme `setExactAndAllowWhileIdle` toutes les ~9 min, un rappel plus rapproché ferait reporter l'adhan qui le suit (décision D18). `sanitizeLead()` ramène toute valeur stockée au choix le plus proche
- **`domain/PrayerEventResolver.kt`** (JVM pur) : généralise la chaîne d'alarmes à des `PrayerEvent(prayer, kind, time)` avec `PrayerEventKind = REMINDER | ADHAN`. Il construit les évènements des deux jours (le shurūq n'en a pas) et rend le premier à venir → **toujours une seule alarme à la fois**, décision D17. `NextPrayerResolver` reste tel quel pour l'affichage
- **Deux canaux** : `NotificationChannels.kt` remplace `PrayerNotificationChannel.kt` et déclare `prayer_times_v2` (adhan) + **`prayer_reminder_v1`** (son `res/raw/prayer_reminder.mp3` fourni par l'utilisateur). Deux canaux pour que l'utilisateur puisse régler ou couper le rappel sans toucher à l'appel à la prière (D19)
- `PrayerAlarmScheduler` pose l'alarme du prochain **évènement** et transmet `EXTRA_KIND` ; `PrayerAlarmReceiver` affiche l'adhan (id = `prayer.ordinal`) ou le rappel (id = `100 + ordinal`, pour ne pas s'écraser), puis replanifie. `EXTRA_KIND` absent → traité comme `ADHAN` (alarme survivant à la mise à jour)
- `SettingsRepository` : deux clés DataStore (`reminder_enabled`, `reminder_lead_minutes`), `reminderFlow` + `currentReminder()` ; le cache mémoire est désormais alimenté à partir des `Preferences` brutes, donc les deux instantanés restent cohérents à chaque écriture. Tout changement replanifie l'alarme (callback déjà en place)
- **Réglages** : interrupteur « التذكير قبل الأذان » + ligne « مدّة التذكير » (masquée quand le rappel est coupé) ; nouveau composable `SettingSwitchRow` (toute la ligne bascule, le `Switch` n'est qu'un témoin)
- **`<plurals name="duration_minutes">`** dans les trois `strings.xml` — six formes en arabe (`zero/one/two/few/many/other`), deux en fr/en ; utilisé par l'écran (`pluralStringResource`) et par la notification (`getQuantityString`)
- 49 tests JVM verts (11 nouveaux : intercalage du rappel, rappel dépassé ignoré, rappel désactivé, enchaînement Isha → rappel du Fajr de demain, shurūq sans évènement, délai long, bornes et `sanitizeLead`) ; `assembleDebug` OK

### Fait (session 7, suite) — le son est joué par l'app (retour d'appareil, Android 10)
Deux symptômes remontés sur un appareil Android 10 : le rappel restait **muet**, et l'adhan **ne mettait pas la musique en pause**. Même racine : on déléguait le son au lecteur de notifications du système, qui ne demande jamais le focus audio (donc ne peut pas interrompre un lecteur) et dont plusieurs surcouches ignorent le son personnalisé d'un canal. Décision **D20** :
- **`notifications/PrayerSoundService.kt`** : service en avant-plan (type `shortService`) qui demande `AUDIOFOCUS_GAIN_TRANSIENT` — les lecteurs en cours se mettent en pause et **reprennent seuls** après — puis joue la ressource avec un `MediaPlayer`. Pas de `MediaPlayer` dans le receiver : son processus peut être tué dès `onReceive` terminé, ce qui couperait l'adhan de 31 s. Démarrer un service d'avant-plan depuis un receiver d'**alarme exacte** fait partie des cas exemptés
- **`notifications/PrayerNotifications.kt`** : point unique de construction (id, canal, son, contenu traduit), partagé par le receiver et le service
- **Canaux muets** (`setSound(null, null)`) sinon double son → IDs bumpés en **`prayer_times_v3`** et **`prayer_reminder_v2`** (les anciens sont supprimés). La **vibration reste au canal**, donc Android suit tout seul le mode du téléphone ; le service applique la même règle au son via `AudioManager.ringerMode` *(dépassé en session 12 : l'app reprend aussi la vibration — voir D38)*
- La notification est posée par le receiver **avant** le service, puis reprise en avant-plan et détachée (`STOP_FOREGROUND_DETACH`) : si le système refuse le service, l'utilisateur est prévenu quand même
- Manifest : `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SHORT_SERVICE`, `<service … foregroundServiceType="shortService">`
- ⚠ `ServiceCompat.startForeground` avec type exige core-ktx ≥ 1.12 (le projet est en 1.10.1) → branchement manuel sur `Build.VERSION_CODES.UPSIDE_DOWN_CAKE`
- **À vérifier sur appareil** : son du rappel puis de l'adhan, mise en pause effective d'un lecteur, et silence en mode vibreur/silencieux

### Fait (session 8) — calendrier mensuel + horaires de Ramadan
- **`domain/MonthGrid.kt`** (JVM pur) : `monthGridCells(month, firstDayOfWeek)` découpe un mois grégorien en cases alignées sur le premier jour de la semaine de la locale (`null` = case vide avant le 1er ou après le dernier jour ; jamais les jours des mois voisins, qui inviteraient à cliquer hors de la grille) ; `weekdaysFrom()` pour l'en-tête des sept colonnes
- **`domain/RamadanTimes.kt`** : `IMSAK_MINUTES_BEFORE_FAJR = 10` (valeur Umm al-Qura), `ramadanTimesOf(day)` → imsāk (Fajr − 10 min) et iftār (= Maghrib exactement), `fastingDuration` de l'imsāk à l'iftār — **décision D22** : ce sont les deux heures affichées, leur différence doit tomber juste
- **`HijriFormatter` étendu** : `toHijri()` (conversion brute avec décalage), `formatMonthYear()` (« رمضان 1448 »), et les extensions `HijrahDate.hijriDayOfMonth` / `.hijriMonth` / `.isRamadan` (+ `RAMADAN_MONTH = 9`)
- **`ui/calendar/`** : `CalendarScreen` (en-tête de mois avec flèches `autoMirrored`, bouton « اليوم » qui n'apparaît que si l'on s'est éloigné, jour ouvert, encart du jeûne), `MonthGrid` (grille cliquable : chaque case porte le quantième grégorien **et** hégirien ; sélection en `primary`, aujourd'hui en liseré, Ramadan en `tertiaryContainer` bleuté pour ne pas concurrencer le vert), `CalendarViewModel` + `CalendarUiState`
- **Réutilisation** : le jour ouvert affiche `PrayerList` de l'accueil (`PrayerRowUi` partagé), donc mêmes tons, même RTL, zéro duplication ; « passé » n'est marqué que pour aujourd'hui, un autre jour s'affiche à plat
- **Décision D21** : le calendrier n'a **pas** fait tomber D7 (pas de librairie de navigation) — il s'ouvre toujours sur aujourd'hui et garde mois + jour sélectionné dans son ViewModel, donc aucun argument ne traverse la navigation. Pas de ticker (rien ne défile), et les horaires ne sont calculés que pour le **jour ouvert**, pas pour les 42 cases ; grille en `Column` de `Row` et non `LazyVerticalGrid` (colonne déjà défilante)
- **Navigation** : `enum Screen` passe à quatre entrées ; les boutons Qibla et calendrier vivent dans un `Row` en `TopStart` de l'accueil (`ic_calendar.xml`, `ic_arrow_forward.xml` ajoutés)
- **Textes** : 12 clés nouvelles dans les **trois** `strings.xml` (`calendar_*`, `ramadan_*`, `duration_hours_minutes`) ; le texte d'aide de l'imsāk réutilise le `<plurals name="duration_minutes">` de la session 7, donc changer la constante suffira
- 61 tests JVM verts (12 nouveaux : multiple de 7, cases vides selon le premier jour de la semaine, mois qui tombe pile, jours présents une seule fois et dans l'ordre, ordre des colonnes, imsāk/iftār/durée, reconnaissance du mois de Ramadan et effet du décalage hijri, titre de mois) ; `assembleDebug` OK. **Pas de test sur émulateur**
- ⚠ En-têtes de mois et noms de jours formatés en `ar-DZ` en dur, comme l'accueil : cohérent avec l'existant, mais à reprendre avec le reste du multilingue

### Fait (session 8, suite) — Maghrib algérien : marge de 3 minutes (retour d'appareil)
- Écart signalé à Skikda contre une autre app. Diagnostic mené **à la seconde**, sur deux dates à quatre mois d'écart : les cinq autres moments coïncident exactement, seul le Maghrib est court de **3 minutes constantes** (6 août : brut 19:34:10, officiel 19:37 · 15 déc. : brut 17:17:02, officiel 17:20)
- C'est l'*iḥtiyāṭ* du ministère, absent de la spécification AlAdhan → `MethodOption.ALGERIA` porte désormais `maghribMinutes = 3` (**décision D23**). L'Isha algérienne étant calculée par angle et non par intervalle, elle n'en hérite pas — un test le verrouille
- Marge posée **pour l'Algérie seulement** : Tunisie et Maroc ont probablement la leur, non relevée
- **Faux positif écarté** : l'Asr paraissait décalé d'une minute le 6 août (brut 16:25:28, à 32 s de la bascule d'arrondi) mais coïncide le 15 décembre (brut 14:59:06). Les deux calculs sont d'accord à la seconde près — aucune correction
- ⚠ La position GPS n'y était pour rien : mesuré à **~4 secondes par kilomètre** (5 km = 13 s, 20 km = 53 s). Un déplacement dans la ville ne peut pas produire un écart d'une minute
- 63 tests JVM verts (2 nouveaux : Maghrib officiel aux deux dates, Isha non décalée) ; `assembleDebug` OK

### Fait (session 8, suite) — ajustement manuel des horaires
- **`domain/model/PrayerTimeAdjustments.kt`** : une minute de décalage par moment, bornée ±30, portée par `CalculationSettings` (**décision D24**). Complète D23 pour tous les pays et calendriers de mosquée qu'on n'a pas mesurés
- Branché sur le **`prayerAdjustments`** d'Adhan et non sur `methodAdjustments` : la librairie additionne les deux, donc un réglage manuel **se superpose** à la marge officielle sans l'effacer, et changer de méthode ne le perd pas
- ⚠ **Les entrées nulles ne sont jamais stockées** (clé DataStore supprimée) : deux réglages équivalents restent `==`, ce dont dépend le cache de `HomeViewModel`, dont la clé est le `CalculationSettings` entier
- L'ajustement traverse les **quatre** points de calcul — accueil, calendrier, `PrayerAlarmScheduler`, `NextPrayerWidgetViews` : l'alarme doit sonner à l'heure que l'écran affiche
- Une clé DataStore par moment (`adjust_fajr`…), setter + remise à zéro dans `SettingsRepository`
- **Réglages** : ligne « تعديل المواقيت يدويًّا » résumant l'état (« العصر +1 · المغرب +3 » ou « بدون تعديل »), ouvrant un dialogue à six curseurs ±1 min avec bouton de remise à zéro ; chaque pas replanifie l'alarme
- 6 clés nouvelles dans les trois `strings.xml` (`settings_adjustments_*`, `action_close`)
- ⚠ Deux pièges Compose rencontrés : un `if` autour du paramètre `dismissButton` d'`AlertDialog` lui fait perdre son contexte `@Composable` (le mettre **dans** la lambda), et `joinToString` n'est pas `inline` — pas de `stringResource` dans sa lambda, contrairement à `map`
- 70 tests JVM verts (7 nouveaux : décalage ciblé, valeur négative, cumul avec la marge algérienne, bornes, retour à zéro et égalité, résumé ordonné, lecture du stockage) ; `assembleDebug` OK

### Fait (session 9) — invocations et adhkār
- **`domain/model/Invocation.kt`** : une invocation (livrée ou écrite par l'utilisateur) et son moment — `InvocationSchedule.FixedTime(h, m)` ou `PrayerAnchor(prière, décalage)` borné à ±(120/240) min par pas de 5. `BuiltinInvocation` (MORNING, EVENING) porte des **ids fixes** (1, 2)
- **`domain/AlarmEventResolver.kt`** (JVM pur) : `ScheduledEvent` = `Prayer` **ou** `Invocation`. Deuxième généralisation de la chaîne après D17 — toujours **une seule alarme à la fois** (**décision D25**). Il porte surtout la **garde de 10 minutes** : le quota Doze (une alarme exacte par ~9 min, déjà responsable de D18) vaut pour toute l'app, donc une invocation posée 5 min avant le Fajr **reporterait l'adhan**. Règle : les évènements de prière ne bougent jamais, une invocation trop proche est repoussée à `bloqueur + 10 min`, en boucle. `PrayerEventResolver` n'est pas remplacé (sa production d'évènements d'un jour est devenue publique)
- **Room v3** : `InvocationEntity` + `InvocationDao` + `MIGRATION_2_3` ; ⚠ le `CREATE TABLE` de la migration doit être mot pour mot celui que Room génère. `data/invocations/InvocationRepository` : même patron que `SettingsRepository` (instantané mémoire pour le receiver, `Flow` pour l'écran), semis idempotent des deux entrées livrées (`OnConflictStrategy.IGNORE` sur les ids fixes → une désactivation survit)
- **Décision D26** : le **contenu** des invocations livrées vit dans les ressources, pas en base — titre traduit dans les trois langues, texte arabe déclaré une seule fois en `translatable="false"` (formule rituelle, comme `notification_call`). Elles sont désactivables mais **pas supprimables**, garanti côté base (`DELETE … AND builtinKey IS NULL`). Une invocation de l'utilisateur stocke, elle, son titre et son texte
- **Décision D27** : le canal `invocations_v1` garde le **son de notification du système** et `IMPORTANCE_DEFAULT` — exception assumée à D20, qui ne se justifiait que par la durée de l'adhan et le besoin de mettre la musique en pause. Donc pas de `PrayerSoundService` pour les adhkār, et l'utilisateur peut changer ce son depuis les réglages Android
- **`PrayerAlarmReceiver` n'a pas été renommé** malgré son périmètre élargi : l'alarme déjà posée par la version installée pointe sur ce nom de classe. Nouveau `EXTRA_INVOCATION` ; l'invocation est **relue** au déclenchement (elle a pu être coupée entre-temps)
- **`ui/invocations/`** : liste (moment résumé « بعد الفجر بـ30 دقيقة », interrupteur), lecture (interligne 36sp pour les diacritiques), éditeur en dialogue (titre, texte, onglets heure fixe / ancrage, `TimePicker` M3, stepper de 5 min). Tout l'état vit dans le ViewModel → **D7 tient toujours**, comme en D21. Cinquième entrée à `enum Screen`, icône chapelet `ic_invocation.xml` sur l'accueil
- `MainActivity` passe en **`singleTop`** + `onNewIntent` : la notification d'une invocation rouvre l'app dessus sans empiler une seconde copie
- 12 clés `invocation_*` + 4 `notification_invocation_*` dans les **trois** `strings.xml` ; les deux textes d'adhkār (آية الكرسي، المعوّذات، أصبحنا/أمسينا، سيّد الاستغفار، رضيت بالله، بسم الله الذي لا يضرّ، حسبي الله، سبحان الله وبحمده) dans `values/` seulement
- **79 tests JVM verts** (9 nouveaux : heure fixe, ancrage avant/après, invocation désactivée, invocation isolée inchangée, poussée après l'adhan, poussée en cascade, aucun adhan déplacé sur une journée entière, écart ≥ garde entre deux évènements consécutifs, passage au lendemain) ; `assembleDebug` OK
- **Vérifié sur émulateur** : migration Room 2→3 sans perte (position تونس/TN conservée, `user_version = 3`), les deux adhkār semés et activés, alarme posée sur l'**invocation** (16:42 = Asr 16:12 + 30, `window=0`, `exactAllowReason=policy_permission`), notification déclenchée à la main (`am broadcast … --el invocation 2`) avec son et texte déplié, appui dessus → l'invocation ouverte, liste/lecture/éditeur/`TimePicker` OK en **arabe RTL comme en anglais LTR**, bascule de langue à chaud OK
- ⚠ Corrigé après essai : le texte arabe s'affichait **calé à gauche** quand l'interface est en fr/en. `TextAlign.Start` ne suffit pas — Compose le résout contre `LocalLayoutDirection`, alors que le sens du paragraphe vient déjà du contenu. `InvocationDetail.alignmentOf()` reprend donc la même heuristique du **premier caractère fortement directionnel** pour l'alignement : un dhikr arabe reste à droite quelle que soit la langue de l'app
- ⚠ Piège Kotlin rencontré : une méthode et une fonction d'extension du même nom dans la même classe ont la **même signature JVM** (« Platform declaration clash ») — on garde l'extension et on l'appelle via `with(resolver) { … }`

### Fait (session 10) — identité visuelle : icône, écran de démarrage, son du rappel
- **Icône de l'app** : le logo fourni par l'utilisateur (SVG 1024, coupole de mosquée portant des aiguilles d'horloge + minaret) devient l'icône adaptative. `ic_launcher_foreground.xml` reprend les deux tracés **tels quels**, en blanc et pleins ; les aiguilles ne sont pas peintes, ce sont des **creux** (sous-tracé de sens inverse, remplissage `nonZero`) qui laissent voir le fond. `ic_launcher_background.xml` : dégradé diagonal du vert de la marque, figé (l'icône est de l'identité, elle ne suit pas le mode sombre)
- **Mise à l'échelle mesurée, pas devinée** : le cercle englobant du dessin a pour centre (505,3 ; 481,5) et rayon 476 unités — pas le centre de la boîte englobante, la composition étant lourde du bas. Le groupe le ramène à un rayon de **34dp** autour du centre du canevas de 108dp (zone sûre : 33). Vérifié en rendant l'icône sous les quatre masques de lanceur (cercle, squircle, arrondi, carré) : rien n'est rogné, ni le socle ni la pointe du minaret
- Les cinq `mipmap-*dpi/ic_launcher*.webp` (robot Android du gabarit) sont **supprimés** : avec `minSdk 26`, `mipmap-anydpi` l'emporte sur tous les appareils, ils n'étaient plus que du poids mort
- **Écran de démarrage en deux moitiés** (décision **D28**) : l'écran système d'Android 12+ (trois attributs dans `values-v31/themes.xml` — impossible à supprimer depuis cette version, seulement à habiller) puis **`ui/splash/SplashScreen.kt`** en Compose, qui ajoute le nom et la baseline que le système ne sait pas afficher. Pas de dépendance `core-splashscreen` : elle ne rétroporterait qu'une API dont la moitié utile est déjà en Compose
- Raccord invisible entre les deux : même vert (`splash_background`, sans variante nuit), et surtout **`android:windowBackground` vaut ce même vert** — sinon un éclair clair (ou sombre) s'intercale. `Theme.Miqaat` est donc scindé en `Base.Theme.Miqaat` (values/ et values-night/, qui portent le parent clair ou sombre) et `Theme.Miqaat` (values/ et values-v31/) : un style n'est jamais fusionné entre qualificateurs, toujours remplacé
- ⚠ Logo du splash Compose à **288dp** : la taille exacte à laquelle Android dessine `windowSplashScreenAnimatedIcon`. Mesuré à l'écran — à 180dp il rapetissait d'un tiers au moment du relais
- Le splash est **superposé** à l'accueil (`Box` + `AnimatedVisibility`, entrée `None`, sortie en fondu 450 ms) et non joué à sa place : l'accueil se compose et charge ses horaires pendant les 1,4 s. `rememberSaveable` pour qu'un changement de langue ne le rejoue pas. Les icônes des barres système sont forcées en clair tant qu'il est là, puis rendues au thème
- **Baseline** : « مواقيت الصلاة أينما كنت، دون إنترنت » / « Les horaires de prière où que vous soyez, sans Internet » / « Prayer times wherever you are, no Internet needed » — clé `app_tagline` dans les **trois** `strings.xml`
- **Son du rappel remplacé** par `prayer_approach_2.mp3` fourni par l'utilisateur (~5 s, 44,1 kHz). ⚠ **Aucun ID de canal à bumper** : depuis D20 les canaux de prière sont muets et le son vient de `PrayerSoundService` — la règle du bump ne vaut plus que pour un réglage du canal lui-même
- 79 tests JVM verts (inchangés), `assembleDebug` OK. **Vérifié sur émulateur (Android 17)** : icône posée dans le dock, écran système vert au logo blanc puis relais sans coupure vers le splash Compose (logo à la même taille, nom + baseline), en **anglais LTR comme en arabe RTL**, en clair **comme en sombre**, puis bascule sur l'accueil. Rappel déclenché à la main : le service obtient le focus audio (`AUDIOFOCUS_GAIN_TRANSIENT`), joue le nouveau MP3 jusqu'au bout et rend le focus, sans erreur `MediaPlayer`
- Note test : `am broadcast` à la main sur le receiver **ne peut pas** démarrer le service sonore (`ForegroundServiceStartNotAllowedException`) — l'exemption ne vaut que pour une alarme exacte réelle. Mettre l'app au premier plan d'abord
- **Vérifié sur appareil réel** (Redmi Note 8, Android 10 / MIUI — celui d'où venait le retour de la session 7) : installé, position سكيكدة trouvée, alarme exacte posée (`RTC_WAKEUP`, `window=0`), canaux `prayer_reminder_v2` et `prayer_times_v3` présents avec `mSound=null` (l'ancien `prayer_times_v2` marqué supprimé). **Le rappel a sonné sur une vraie alarme** et l'utilisateur a confirmé entendre le bon enregistrement ; le log audio le corrobore : `AudioTrack stop: 222336 frames delivered` = **5,04 s à 44,1 kHz**, soit le fichier joué en entier, focus audio pris puis rendu, aucune erreur `MediaPlayer`, et la chaîne a enchaîné sur l'alarme suivante
- Astuce de mesure : `AudioTrack ... frames delivered` ÷ fréquence d'échantillonnage donne la durée **réellement sortie**, ce qui distingue « le son n'est pas joué » de « le son est joué mais inaudible » — le reste du log ne prouve que l'intention
- **D20 validé de bout en bout sur ce même appareil**, avec un lecteur de Coran (`my.smartech.mp3quran`) en train de jouer. Séquence relevée à la milliseconde : `requestAudioFocus … req=2` → le lecteur reçoit **`onAudioFocusChange(-2)`** (LOSS_TRANSIENT) 3 ms plus tard et se met en pause → `AudioTrack stop: **496000 frames delivered**` = 496000 ÷ 16 000 Hz = **31,0 s exactement**, l'adhan entier → le lecteur reçoit **`onAudioFocusChange(1)`** (GAIN) et **reprend tout seul**. C'était la moitié de D20 jamais confirmée depuis la session 7 : la mise en pause de la musique, impossible en laissant le son au canal de notification
- Note bénigne observée : `NotificationService: Muting recently noisy … com.mohamed.miqaat|3` — Android étouffe la seconde émission de la notification (celle reprise en avant-plan par le service). Sans effet, le canal étant déjà muet ; à ne pas confondre avec un son perdu

### Fait (session 11) — première release signée + correction des insets
- **Signature des releases (décision D29)** : `app/build.gradle.kts` lit `keystore.properties` **s'il existe** ; sans lui, `assembleRelease` produit toujours un APK, non signé — le dépôt reste donc compilable par quiconque le clone, ce qui compte sous GPL. `*.jks`, `keystore.properties` gitignorés (vérifié à `git check-ignore` avant le commit), `keystore.properties.example` versionné. Signature **v2 + v3** : v1 ne sert qu'en dessous d'Android 7 (`minSdk 26`), v3 ouvre la rotation de clé — impossible à ajouter après publication
- Clé RSA 4096 créée par l'utilisateur (`keytool`, mots de passe jamais manipulés ici). ⚠ **Refaite une fois** : le premier certificat portait `CN=Unknown` (champs laissés vides). Corrigé en `CN=Mohamed Boughouas, O=Miqaat, C=DZ` — l'identité du certificat est définitive dès la première publication, la chaîne de mises à jour en dépend
- **`docs/release.md`** : procédure complète (clé, build, `apksigner verify`, empreinte SHA-256, tag, release GitHub) + les trois points à préparer si Google Play un jour (déclaration `USE_EXACT_ALARM`, politique de confidentialité, AAB)
- **APK release 10,00 Mo**, R8 laissé désactivé pour cette première version (aucune règle proguard écrite ni éprouvée ; la ponctualité des alarmes prime sur quelques centaines de kilooctets)
- **Bug d'insets corrigé sur six écrans (décision D30)** : `statusBarsPadding()` était déclaré **après** `verticalScroll()`, donc à l'intérieur du contenu qui défile — l'en-tête passait sous la barre de statut dès le premier glissement (invisible au repos, d'où un défaut intermittent). Et les marges basses, écrites `Spacer(Modifier.height(24.dp).navigationBarsPadding())`, n'ont **jamais** rien fait : `height()` à l'extérieur fixe la hauteur totale à 24dp. L'écran des réglages n'en avait aucune. Règle désormais : insets sur le conteneur, avant `verticalScroll` ; l'accueil reste l'exception (le héros peint derrière la barre de statut)
- **Stepper hégirien** : `width(72.dp)` en dur sur la valeur → « Aucun ajustement » se coupait au milieu d'un mot **dès l'échelle de police par défaut**. Remplacé par une valeur courte (`0`, `+1`, `−2`), même largeur (56dp) et même code couleur que le stepper des ajustements manuels ; `settings_hijri_no_offset` retirée des trois langues
- 79 tests JVM verts (inchangés) ; vérifié sur émulateur aux échelles de police **1,0 / 1,3 / 1,8** en navigation à trois boutons : contenu rogné sous la barre de statut au défilement, dégagé au-dessus des boutons du bas, stepper sur une seule ligne
- **Vérifié sur appareil réel** (Redmi Note 8) : release installée, empreinte du `base.apk` comparée à celle du build — identiques
- ⚠ **Piège de test rencontré** : le téléphone exécutait une build **debug** (`CN=Android Debug`, 15,4 Mo) installée par le bouton ▶ d'Android Studio, pas l'APK release. D'où un « rien n'a changé » trompeur. Debug et release portent le même `applicationId` : elles ne peuvent pas coexister et se remplacent silencieusement. **Toujours vérifier par l'empreinte** : `adb shell pm path <pkg>` puis `sha256sum` sur le `base.apk`, comparé au fichier compilé
- Notes MIUI : `adb install` est refusé (`INSTALL_FAILED_USER_RESTRICTED`) tant que « Installation via USB » n'est pas activée dans les options développeur ; et `adb shell input` est refusé (`INJECT_EVENTS`) dès qu'une boîte de dialogue système a le focus

### Fait (session 12) — fiabilité des notifications + mode d'alerte
Retour d'appareil : **aucune notification n'arrivait** sur le Redmi Note 8 (Android 10 / MIUI), et l'ouverture de l'app vers 14h faisait apparaître la notification « approche du Fajr » — l'alarme de 4h délivrée dix heures plus tard, au redémarrage du processus. Deux défauts distincts, deux chantiers.

**Chantier 1 — la délivrance**
- **`domain/AlarmFreshness.kt` (décision D31)** : le scheduler transmet `EXTRA_TRIGGER_AT`, le receiver le compare à l'heure réelle. Tolérances 20 min (adhan) / **5 min** (rappel) / 30 min (invocation). Les 5 min ne sont pas arbitraires : strictement sous `LEAD_CHOICES.min()` (10 min), sinon un rappel périmé s'afficherait **après** son adhan — un test verrouille l'inégalité. Extra absent (alarme d'une version antérieure) = frais ; déclenchement en avance = frais
- `PrayerAlarmReceiver` : tout le travail dans un `try` / `scheduleNext()` dans un **`finally`** — la chaîne se replanifie même sur évènement périmé, sans permission de notification, ou après exception. `runCatching` sur la pose d'alarme avec repli inexact (une surcouche peut refuser l'exacte malgré la permission). **Pas de `goAsync()`** : le droit de démarrer un service d'avant-plan tient à l'allowlist accordée *pendant* `onReceive`
- **`RescheduleReceiver` (D32)** : `exported="true"` + `MY_PACKAGE_REPLACED` + `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` + action maison du chien de garde. ⚠ `exported="false"` **fonctionnait** (vérifié session 2) — le vrai correctif ici est `MY_PACKAGE_REPLACED` : Android annule les alarmes d'un paquet remplacé, donc **chaque mise à jour tuait la chaîne** silencieusement, depuis toujours
- **Chien de garde (D33)** : seconde alarme `setInexactRepeating` semi-quotidienne (`requestCode 1003`) vers `RescheduleReceiver`. **WorkManager écarté** — dépendance de plus, travail déférable donc incapable de tenir la promesse de ponctualité, et gelé par MIUI comme le reste ; la ligne « AlarmManager + WorkManager » de la table de stack, jamais implémentée depuis la session 1, est **corrigée**. Le filet ne répare pas MIUI : il répare la rupture de chaîne
- **Écran « Fiabilité des notifications » (D34, D35)** : `domain/reliability/` (verdict, testable) + `data/reliability/` (`ReliabilityLog` en SharedPreferences, `ReliabilityInspector`, `OemAutostart`) + `ui/reliability/` (écran, ViewModel, bannière). Cinq contrôles : notifications, alarmes exactes (sans objet hors Android 12/12L, `USE_EXACT_ALARM` étant accordée d'office au-delà), batterie, démarrage automatique OEM, et **délivrance réelle** — seul détecteur automatique du gel, via l'horodatage du receiver. Bouton « notification de test », prochaine alerte, dernière reçue. Réévalué à chaque `ON_RESUME`
- **Règle anti-harcèlement** verrouillée par test : `UNKNOWN` ne déclenche **jamais** la bannière (l'autostart MIUI est illisible ; sinon tout possesseur de Xiaomi aurait un avertissement inextinguible). Bannière sur critique + certain seulement, report 14 jours, jamais de modale au lancement
- ⚠ `OemAutostart` exige **trois** précautions : bloc `<queries>` au manifeste (sans lui `resolveActivity` renvoie `null` sur Android 11+), `resolveActivity` (les noms changent selon la version de surcouche), et `try/catch` vers le texte manuel

**Chantier 2 — le mode d'alerte** (`domain/NotificationAlert.kt`, D36 → D39)
- `NotificationMode { FOLLOW_PHONE, ALWAYS_SOUND, ALWAYS_VIBRATE, SILENT }`, défaut « suivre le téléphone ». `AlertResolver` croise le mode et le `RingerState` → `AlertDecision(stream: AlertStream?, vibration: VibrationStyle)`. `stream` nullable et non doublé d'un booléen : l'état « je ne joue pas mais voici mon flux » devient inconstructible. Les **12 cases** de la matrice ont chacune leur assertion
- **Flux audio (D37)** : `USAGE_NOTIFICATION_RINGTONE` par défaut → volume de la **sonnerie d'appel** (ce que l'utilisateur demandait) ; `USAGE_ALARM` quand le son est forcé → non muté par le ringer mode. `ALWAYS_SOUND` n'emprunte le flux alarme que lorsqu'il le faut. DND : « alarmes seulement » laisse passer le forçage, « silence total » gagne toujours (contourner exigerait `ACCESS_NOTIFICATION_POLICY`, non demandée)
- **Vibration reprise par l'app (D38)** : les **trois** canaux passent muets *et* sans vibration → nouveaux IDs `prayer_times_v4`, `prayer_reminder_v3`, `invocations_v2`, anciens ajoutés à `OLD_IDS`. La vibration part du **receiver** et non du service : un `VibrationEffect` fini est confié au service système et survit à la mort du processus. Motifs courts (0,4 s / ~3 s), jamais indexés sur les 31 s de l'adhan. Attribut `USAGE_ALARM` indispensable, sinon le système la supprime comme une vibration de notification
- Effet de bord : en vibreur ou silencieux, **plus aucun service d'avant-plan n'est démarré**. `PrayerSoundService` → **`AlertSoundService`** (il sert les trois natures d'alerte ; renommage sans risque, aucune `PendingIntent` ne pointe sur un service, contrairement au receiver). `onTimeout()` ajouté (`shortService` limité à ~3 min sur Android 14+)
- **D27 renversée (D39)** : les adhkār suivent le mode d'alerte. Leur son reste `DEFAULT_NOTIFICATION_URI`, simplement joué par nous ; focus `..._MAY_DUCK` et non `TRANSIENT` (quelques secondes ne justifient pas d'interrompre une lecture). Perdu : le choix d'un son différent depuis Android. Gagné : un réglage unique pour tout ce que l'app émet
- **Troisième instantané** dans `SettingsRepository` (`NotificationSettings`, clé `notification_mode`) plutôt qu'une extension de `ReminderSettings` — celui-ci est une entrée des resolvers, y glisser un réglage de rendu ferait trimballer une donnée inutile à toute la planification. ⚠ `cache()` doit rafraîchir les **trois** instantanés : en oublier un le rendrait périmé pour le receiver, qui tourne souvent dans un processus neuf où aucun Flow n'a émis
- Réglages : ligne « نمط التنبيه / Mode d'alerte » (dialogue radio à quatre entrées, sous-titre) + ligne « Fiabilité des notifications » ; `SettingRow` gagne un `subtitle` optionnel
- **31 clés nouvelles** dans les trois `strings.xml` ; `MainActivity.Screen` passe à six entrées
- **108 tests JVM verts** (29 nouveaux : matrice 12 cases + invariants, garde de fraîcheur dont le verrou rappel < délai minimal, parsing du mode, verdict de fiabilité dont la règle anti-harcèlement) ; `assembleDebug` OK
- **Vérifié sur le Redmi Note 8** : release signée installée par-dessus la précédente (même clé, aucune donnée perdue), l'utilisateur confirme que les notifications arrivent. Restent à éprouver dans la durée le verdict `DELIVERY` sur une nuit entière et la matrice complète du mode d'alerte (voir « Prochaine étape »)

### Fait (session 13) — précision des horaires : l'arrondi et la marge officielle
Retour d'appareil : écarts d'une à deux minutes contre le calendrier officiel de Skikda, « sur certains moments seulement, sans logique apparente ». L'utilisateur soupçonnait la position GPS. Ce n'était pas ça.

- **Diagnostic (décision D40)** : Adhan calcule à la seconde puis **arrondit à la minute la plus proche** (`Rounding.NEAREST` par défaut, vérifié dans le bytecode de `CalendarUtil.roundedMinute` : `minute + round(sec/60)`). Un ministère ne fait jamais ça : il ajoute une marge de précaution (iḥtiyāṭ) puis **tronque**, pour que l'heure annoncée ne tombe jamais *avant* l'heure calculée. L'app pouvait donc annoncer jusqu'à 30 s trop tôt, et l'écart se voyait ou non selon les secondes du jour — d'où un défaut qui paraissait aléatoire. La position, elle, ne pèse que **~4 s/km** (mesuré en session 8) : il faudrait être à ~20 km du point de référence pour perdre une minute
- **Mesure sur un mois entier**, pas sur deux dates : calendrier officiel de la مديرية الشؤون الدينية والأوقاف — سكيكدة, Rabīʿ al-Awwal 1448 (14 août → 12 septembre 2026), 30 jours × 5 moments. Horaires bruts sortis à la seconde (`Rounding.NONE`, sans marge), puis ajustement d'un décalage constant par prière : chaque jour contraint le décalage à un intervalle de 60 s, l'intersection des 30 le donne à quelques secondes près. **Une intersection vide est une information** — c'est ce qui a révélé une erreur de transcription et la dérive de l'ʿAṣr
- **État avant correction, chiffré** : sur ces 30 jours, l'app tombait juste sur **1 ligne de Fajr, 1 de Ẓuhr, 0 d'ʿAṣr, 5 de Maghrib, 4 d'ʿIshāʾ**. Elle n'était pas « parfois décalée » : elle était presque systématiquement une minute en avance
- **`domain/model/TimeCalibration.kt`** (créé) : `MinuteRounding` (NEAREST/DOWN/UP) + un décalage **en secondes par moment**, appliqué **avant** l'arrondi (l'ordre compte : arrondir d'abord déplace le résultat d'une minute). `PrayerTimesCalculator` demande désormais `Rounding.NONE` à Adhan et tranche lui-même. Des secondes et non des minutes parce que la mesure le commande : en minutes entières le meilleur modèle ne reproduit que 25 à 29 lignes sur 30 selon le moment
- **`MethodOption.ALGERIA` calibrée** : Fajr +95 s, Ẓuhr +85 s, ʿAṣr +126 s, Maghrib +261 s, ʿIshāʾ +82 s, puis troncature → **143 cases justes sur 150**. Les cinq nombres se lisent en deux termes : une base d'environ 85 s commune (marge du ministère + point de référence de la ville) et, sur le seul Maghrib, **3 minutes de plus** — 261 ≈ 85 + 176, soit la marge de D23 confirmée sur trente jours au lieu de deux
- **Règle d'arbitrage : jamais en avance.** L'ʿAṣr et l'ʿIshāʾ dérivent d'une dizaine de secondes sur le mois, aucun décalage constant ne les rend exacts partout. On choisit le côté tardif sans exception — une minute de retard est sans conséquence, une minute d'avance fait prier avant l'heure. Coût : deux jours exacts sur l'ʿAṣr (126 s plutôt que 120)
- **Hypothèse écartée** : la dérive pouvait venir de coordonnées différentes (le Ẓuhr ne dépend que de la longitude, l'ʿAṣr et l'ʿIshāʾ aussi de la latitude). Un balayage ±0,30° au pas de 0,01° trouve des couples qui rendent les cinq décalages constants, mais tous demandent une latitude ~0,20° plus au sud — 22 km dans les terres, ce qui ne décrit pas le chef-lieu d'une wilaya côtière — et les marges y restent inégales d'un moment à l'autre. Écartée
- **D23 amendée** : ses 3 minutes de Maghrib sont confirmées, mais deux points de mesure ne pouvaient pas séparer la marge de l'arrondi, et le même diagnostic avait écarté à tort un écart sur l'ʿAṣr comme « faux positif » — il était réel. Le relevé de décembre 2026 (Maghrib 17:20) ne s'accorde pas à cette calibration ; il venait d'une comparaison « contre une autre app », pas du ministère, donc c'est l'image officielle qui fait foi. Un calendrier d'hiver officiel tranchera
- **`docs/prayer-times-accuracy.md`** (créé) : les trois sources d'écart, le protocole de mesure réutilisable pour tout pays, le relevé de Skikda et ce qui reste ouvert
- **Rien ne change pour les autres méthodes** : la calibration par défaut reproduit exactement le comportement d'Adhan, et un test le verrouille. **Aucun texte d'interface nouveau**, donc rien à traduire — la correction est entièrement interne
- **120 tests JVM verts** (12 nouveaux : 7 sur l'arrondi et l'ordre décalage/arrondi, 5 sur le calendrier officiel — jamais en avance, jamais plus d'une minute après, Fajr/Ẓuhr/Maghrib exacts les 30 jours, nombre de lignes justes figé pour l'ʿAṣr et l'ʿIshāʾ, place du shurūq) ; `assembleDebug` OK. **Pas de vérification sur appareil** (non demandée cette session)

### Fait (session 14) — écoute du Coran
Première fonctionnalité en réseau de l'app. L'idée n'était pas d'ajouter une n-ième application de récitation, mais d'exploiter ce que Miqaat est seule à savoir pendant qu'elle joue : **les horaires de prière et la date hégirienne**.

- **Source** : API publique **mp3quran.net v3**, sans clé ni inscription. Deux appels seulement (`/reciters`, `/suwar`), `HttpURLConnection` + `org.json` — **pas** de Retrofit ni d'OkHttp. Le parseur JSON → domaine est une fonction **pure prenant une `String`**, donc testable sur fixtures ; `org.json` est ajouté en `testImplementation` seulement (le framework Android lève « not mocked » en test JVM)
- **Trois pièges de l'API relevés à la mesure**, tous verrouillés par un test : ① les codes de langue ne sont pas ceux d'Android — l'arabe est `ar`, le français `fr`, mais **l'anglais est `eng`**, et envoyer `en` ne renvoie pas d'erreur, l'API retombe silencieusement sur l'arabe ; ② `surah_list` porte parfois une **virgule traînante** (la documentation officielle en montre un exemple) ; ③ **un moshaf n'a pas toujours les 114 sourates** (celui de Hazza Al-Balushi en compte 83) — les manquantes s'affichent atténuées et non cliquables plutôt que masquées
- ⚠ Les pages de documentation répondent **403** sans User-Agent de navigateur, et `/api/v3/docs` est un **404** : la vraie doc est sur `https://www.mp3quran.net/ar/api`. Les endpoints JSON, eux, répondent à n'importe quel client. La page `/ar/api/2` documente l'API « verset par verset », un service différent — pas une version de repli
- **D41 — `INTERNET` entre dans l'app, et l'offline-first se précise plutôt qu'il ne tombe** : aucune fonction **cœur** (horaires, alarmes, Qibla, calendrier, adhkār, widget) ne touche au réseau, et l'app reste entièrement utilisable sans jamais ouvrir l'écran du Coran. Toujours aucun SDK de tracking, un seul hôte contacté, aucune donnée envoyée. La baseline `app_tagline` (« دون إنترنت ») **reste vraie** : elle parle des horaires
- **D42 — Media3/ExoPlayer, en rupture assumée** avec la tradition zéro-dépendance (Glance D14, WorkManager D33, AppCompat, `core-splashscreen`, navigation D7). Il apporte quatre choses qu'on ne réécrit pas correctement à la main sur un flux HTTP distant : buffering et reprise après coupure, focus audio, notification média + écran verrouillé, file d'attente. Coût ~2,5 Mo. `media3-exoplayer` + `media3-session` **seulement** (pas `media3-ui`, en Views). ⚠ `QuranPlaybackService` est `exported="true"` : `MediaSessionService` l'**exige**, ce n'est pas une inattention à corriger
- **D43 — le lecteur cède la place à l'adhan, par deux chemins et non un seul.** Quand l'alerte a du son : **rien à écrire**, `AlertSoundService` demande déjà `AUDIOFOCUS_GAIN_TRANSIENT` (D20) et ExoPlayer construit avec `handleAudioFocus = true` se met en pause **puis reprend seul** — exactement le comportement mesuré à la milliseconde en session 10 avec un lecteur tiers, sauf que le lecteur est maintenant le nôtre. Quand l'alerte est **muette** (vibreur/silencieux : depuis D38 aucun service sonore n'est démarré, donc personne ne prend le focus), `PrayerAlarmReceiver` appelle explicitement `QuranPlaybackService.pauseForPrayer()`. La règle qui évite le double comportement : `if (decision.stream == null)`. Une **invocation ne met rien en pause** — quelques secondes de dhikr atténuent (`..._MAY_DUCK`, D39), interrompre serait disproportionné
- ⚠ La chaîne d'alarmes **n'apprend pas** l'existence du lecteur : ni `PrayerAlarmScheduler`, ni `AlarmEventResolver`, ni `AlertSoundService` ne sont touchés. Le receiver seul fait le lien, et **après** avoir posé la notification — la ponctualité de l'adhan ne dépend en rien du Coran
- **La sourate du moment** (`domain/QuranSuggestion.kt`, JVM pur) : al-Kahf le vendredi **du Fajr au Maghrib**, al-Mulk après l'Isha, Yā-Sīn entre Fajr et shurūq, al-Wāqiʿa entre Maghrib et Isha, ar-Raḥmān sinon. Les bornes sont les **horaires réels du jour**, jamais des heures d'horloge — c'est ce qui rend la première règle juste : la nuit du vendredi commence au Maghrib du jeudi, donc jeudi soir c'est al-Mulk qui gagne et al-Kahf ne prend le relais qu'au Fajr. Un test décale le Maghrib d'une heure et vérifie que la même heure d'horloge change de réponse
- **Room v3 → v4** : `quran_reciter`, `quran_moshaf`, `quran_surah`, `quran_favorite`. ⚠ Le piège de la session 9 (le `CREATE TABLE` mot pour mot) a été **écarté sans émulateur** : après un `compileDebugKotlin`, le `MiqaatDatabase_Impl.kt` généré par KSP contient le SQL exact, comparé caractère par caractère à celui de la migration — identique. Une clé primaire non auto-générée s'écrit `INTEGER NOT NULL … PRIMARY KEY(id)`, pas `INTEGER PRIMARY KEY`
- **Cache** : le catalogue n'existe que dans **une langue à la fois** (les noms ne sont que des translittérations), rechargé si vide, périmé (> 7 jours) ou d'une autre langue. Parcourir marche hors ligne dès le premier chargement ; **écouter demande le réseau**, et l'écran le dit. Un échec ne bloque que si l'on n'a rien à montrer. Les **favoris** ne sont pas du cache — c'est pour eux qu'une migration a été écrite plutôt que de laisser Room repartir de zéro
- **Second DataStore** (`quran`) et non une extension de `settings` : la position de lecture s'écrit à chaque pause, la mêler aux réglages ferait réémettre tous leurs `Flow` — donc **replanifier l'alarme** — à chaque mise en pause de la récitation
- **UI** : `ui/quran/` — récitateurs (recherche, favoris épinglés en tête, ~130 entrées), sourates du récitateur ouvert, carte de suggestion, lecteur complet (progression, ±10 s, précédent/suivant). Une seule `LazyColumn` partagée par l'en-tête et les listes (écrites en `LazyListScope`), tout l'état dans le ViewModel → **D7 tient toujours**, comme D21 et les adhkār. 7ᵉ entrée à `enum Screen`, 4ᵉ icône sur l'accueil (`ic_quran.xml`)
- **Le mini-lecteur en `bottomBar` de `MainActivity`** et non dans un écran : il survit au changement d'écran, donc on écoute une sourate en consultant les horaires. Invisible quand rien ne joue (ne compose rien, aucun blanc — patron de `ReliabilityBanner`). ⚠ Effet de bord corrigé au passage : la barre porte déjà `navigationBarsPadding`, donc `MainActivity` applique `consumeWindowInsets(innerPadding)` en plus de `padding(innerPadding)` — sans quoi chaque écran rajoutait la marge par-dessus et un blanc de la hauteur de la barre de navigation apparaissait dès qu'une sourate jouait. Même famille que D30
- ⚠ Le service est habillé par `AppLocale.wrap()` dans `attachBaseContext`, comme toute surface hors activité ; `onTaskRemoved` l'arrête si rien ne joue, sinon une notification fantôme survit à la fermeture
- **35 clés `quran_*`** dans les trois `strings.xml`. Les **noms des 114 sourates ne s'y trouvent pas** : ils viennent de l'API déjà traduits
- **151 tests JVM verts** (31 nouveaux : les cinq règles de suggestion et leur priorité, la bascule jeudi soir → vendredi matin, la preuve que les bornes suivent les horaires et non l'horloge, la construction de l'URL et de la file, le parsing sur fixtures officielles dont la virgule traînante et le JSON tronqué, et le verrou `ENGLISH → "eng"`) ; `assembleDebug` OK — **Media3 compile sur AGP 9**, validé dès la première étape avant d'écrire le reste
- **Aucune vérification sur appareil** cette session (non demandée) : voir « Prochaine étape »

### Fait (session 14, suite) — quatre retours d'appareil sur l'écoute du Coran
Release v1.2 installée sur le Redmi Note 8, quatre points remontés, tous corrigés.

- **Les noms restaient en arabe quelle que soit la langue.** Deux causes indépendantes. ① `https://mp3quran.net/api/v3` répond **301** vers `www.mp3quran.net` — `BASE_URL` vise désormais l'hôte canonique. ② Surtout : le `QuranViewModel` recevait le code de langue **à sa construction**, or un changement de langue appelle `recreate()` et **un ViewModel survit à la recréation de l'activité**. Le code capturé au premier affichage ne changeait donc jamais. C'est maintenant `QuranScreen` qui appelle `setLanguage()` dans un `LaunchedEffect(languageTag)`, et le ViewModel ne retient plus rien. Vérifié côté API : `/suwar?language=fr` rend bien « Prologue », « La génisse »
- **La 4ᵉ icône de l'accueil chevauchait le nom de la ville** — le défaut annoncé en fin de session, confirmé. Le héros réserve désormais `ACTION_ROW_HEIGHT` (56dp = 48 de l'`IconButton` + les 4dp de marge du `Row`) en haut, au lieu de 24dp. Corrigé côté héros et non en déplaçant l'icône : la marge tient à n'importe quelle échelle de police, un déplacement n'aurait fait que relocaliser le problème
- **La notification et l'écran verrouillé affichaient la pochette de mp3quran.** Ce n'était pas une pochette manquante : leurs MP3 portent une image **ID3 embarquée**, et `ExoPlayerImpl.buildUpdatedMediaMetadata()` complète les métadonnées du flux avec celles de l'élément de la file — l'élément gagne pour tout champ qu'il **renseigne**, et perd pour tout champ laissé vide. D'où un titre et un récitateur corrects, et une pochette étrangère. `quran/QuranArtwork` dessine le logo Miqaat sur le vert de la marque (à la volée depuis `ic_launcher_foreground`, donc une seule source pour le dessin) et le pose par `setArtworkData`. La petite icône de la barre d'état passe par `DefaultMediaNotificationProvider.setSmallIcon` — sinon c'est la note de musique générique de Media3
- **Listes enrichies** : sections « المفضّلة » / « كلّ القرّاء » avec compteur au lieu d'une simple concaténation, pastille d'initiale pour accrocher l'œil sur 130 lignes (pas de photo : l'API n'en fournit pas et en chercher ailleurs voudrait dire contacter un second hôte, contre D41), et mention « 83 سورة من 114 » sur les seuls enregistrements incomplets. Côté sourates : numéro dans une **rosace girih** dessinée au Canvas — le même khātam que la mosaïque du widget —, origine et **nombre de versets**
- **Le décompte des versets est en dur** (`Surah.AYAH_COUNTS`, décompte de Kūfa) : l'API ne le donne pas, et c'est une donnée immuable qui n'a pas à dépendre du réseau. ⚠ Une faute de frappe dans 114 nombres serait invisible à l'œil — un test vérifie que la somme fait **6236**
- Champ de recherche doté d'une loupe et d'une croix d'effacement (`ic_search.xml`, `ic_close.xml`)
- **155 tests JVM verts** (4 nouveaux sur le décompte des versets) ; `assembleDebug` OK

### Fait (session 15) — mise à jour de l'app depuis GitHub
L'app est distribuée hors Play Store : chaque version est un APK signé joint à un tag du dépôt, et rien ne prévenait l'utilisateur qu'une nouvelle existait. Elle lit désormais `/releases/latest` elle-même, l'annonce sur l'accueil, et sait télécharger puis faire installer l'APK. **Dispositif temporaire**, en attendant un compte Google Play développeur — ses critères de retrait sont écrits dès maintenant dans D44.

- **Aucune dépendance nouvelle** : `HttpURLConnection` + `org.json` (calque de `Mp3QuranApi`), `DownloadManager` et `FileProvider` (androidx.core, déjà là). `libs.versions.toml` n'est pas touché
- **`domain/update/AppVersion.kt`** (JVM pur) : compare le `tag_name` de la release au `versionName` du paquet **réellement installé** — lu au `packageManager` et non à `BuildConfig`, qui n'est d'ailleurs pas généré ici : après un sideload, la question est précisément « qu'est-ce qui tourne vraiment ? ». **Repli fermé** : dès qu'un côté est illisible, rien n'est proposé — la règle de D34 transposée. ⚠ Un `-` ou un `+` fait échouer la lecture **entière** et n'est pas un séparateur qu'on couperait, sinon `v1.3-rc1` se ferait passer pour `1.3` ; et la comparaison est numérique, jamais lexicographique (`1.10` > `1.9`, le bug classique du genre)
- **`domain/update/ReleaseInfo.kt`** : `UpdateVerdict.shouldShowOnHome` (cinq portes : opt-out, report, cache vide, version ignorée, pas plus récente) et le **veto du `versionCode`** — une ligne `versionCode: N` dans le corps de la release l'emporte sur le tag, parce qu'Android refuse tout `versionCode` non croissant et le refuse **après** le téléchargement, par un « Application non installée » que rien n'explique. Ligne absente → le tag décide seul, un oubli de rédaction n'éteint pas la détection
- **`data/update/GithubReleaseApi.kt`** : un GET, parseur en `object` pur donc testable sur fixtures. ⚠ GitHub répond **403 sans `User-Agent`** (le seul de ce cas dans l'app) ; quota anonyme 60 req/h, hors d'atteinte à une vérification par jour. Le parseur choisit l'asset `.apk` (préférant `miqaat-`), rejette toute URL non `https://`, revérifie `draft`/`prerelease`, et extrait des notes l'empreinte SHA-256 et le `versionCode`. ⚠ Le gabarit de `release.md` publie **deux** empreintes (APK puis certificat) : c'est la première étiquetée `SHA-256 :` qui gagne, la seconde étant annoncée par « SHA-256 du certificat », qui ne suit pas la forme `clé : valeur`
- **`data/update/UpdateLog.kt`** en **SharedPreferences** et non DataStore : la note d'accueil a besoin d'une lecture **synchrone** (le repository amorce son `StateFlow` dans son constructeur, donc rien ne clignote), et l'argument qui avait fait naître le second DataStore du Coran (« la position s'écrit à chaque pause ») joue ici en sens inverse — une écriture par jour. Tout est persisté, **notes de version comprises** : après une seule vérification réussie, la note et l'écran entier s'affichent hors ligne ; seul le téléchargement demande le réseau
- **`data/update/UpdateRepository.kt`** : la vérification ne part **que depuis l'activité** (`MainActivity.onCreate`) — jamais d'un receiver, jamais de la chaîne d'alarmes, jamais d'un travail différé (D33 tient). Au plus une fois par 24 h, `@Volatile inFlight` parce qu'un changement de langue appelle `recreate()`, et **`lastCheckAt` n'est écrit qu'en cas de succès** : un échec ne consomme pas le quota
- **`data/update/ApkInstaller.kt`** : `setDestinationInExternalFilesDir(DIRECTORY_DOWNLOADS)` n'exige **aucune permission de stockage** à aucun niveau d'API ; taille puis SHA-256 vérifiés ; `FileProvider` + `ACTION_VIEW` `application/vnd.android.package-archive` + `FLAG_GRANT_READ_URI_PERMISSION` (sans lui : « Analyse impossible ») ; garde `canRequestPackageInstalls()` puis cascade `||` vers l'écran des sources inconnues, comme celle de l'optimisation de batterie. ⚠ Le nom du fichier vient de **notre** tag filtré, jamais du `name` de l'asset distant
- **`PackageInstaller` en session écarté** (D44) : il exigerait un `PendingIntent` **mutable** (API 31+), un receiver `RECEIVER_NOT_EXPORTED` (API 34+) et une table de statuts, pour un seul avantage — l'écriture en flux — sans objet puisque `DownloadManager` a déjà posé le fichier. Sur une surcouche hostile, le chemin le plus banal est le plus sûr
- **Progression par sondage du curseur, aucun `BroadcastReceiver`** : `ACTION_DOWNLOAD_COMPLETE` ne donne que la fin (il faudrait sonder de toute façon), un receiver dynamique doit déclarer son exposition depuis Android 14, et hors écran la notification de `DownloadManager` fait le travail gratuitement. Boucle `delay(500)` dans `viewModelScope`, `download_id` persisté pour se raccrocher après une mort du processus
- **Nettoyage à l'ouverture suivante** (`cleanUpIfInstalled`) : après l'installation le processus est remplacé, plus rien de nous ne s'exécute. Ça ramasse au passage les fichiers d'une installation abandonnée. `RescheduleReceiver` n'est pas touché
- **Manifeste** : `REQUEST_INSTALL_PACKAGES` + le **premier `<provider>` du projet** (`FileProvider`, `${applicationId}.updates`) + `res/xml/file_paths.xml` (`external-files-path path="Download/"` — une erreur ici ne se voit qu'à l'installation, par un « Failed to find configured root »). **Aucun ajout au bloc `<queries>`** : le repli navigateur est un intent implicite passé à `startActivity`, hors du filtrage de visibilité d'Android 11 — c'est `resolveActivity` qui est filtré, et on n'en utilise pas. Le commentaire au-dessus d'`INTERNET` est réécrit
- **D41 amendée (D44)** : trois hôtes désormais (`api.github.com`, puis `github.com` et son CDN sur tape explicite) au lieu d'un. Rien n'est envoyé, aucun identifiant, aucun SDK, aucune fonction cœur ne touche au réseau, la baseline « دون إنترنت » reste vraie. Mais c'est le **premier appel réseau que l'app fait d'elle-même**, sans qu'on ait ouvert une fonctionnalité en réseau — d'où l'opt-out, que l'écoute du Coran n'avait pas besoin d'avoir
- **UI** : 8ᵉ entrée à `enum Screen` (**D7 tient** — une entrée, une branche de `when`, le `BackHandler` générique). Écran et non dialogue : notes multi-paragraphes, progression qui survit à un aller-retour vers un écran système, et une destination nécessaire **même quand tout est à jour**. La note d'accueil suit le patron de `ReliabilityBanner` (composable autonome, `if (!visible) return`), en `tertiaryContainer` et non `errorContainer` — une version disponible n'est pas une erreur —, et **après** celle de la fiabilité : l'avertissement dit que l'app échoue à son métier, la mise à jour n'est qu'un agrément. L'interrupteur d'opt-out vit sur l'écran de mise à jour et non dans les réglages, comme le témoin OEM vit sur l'écran de fiabilité (D35)
- **30 clés `update_*`** dans les **trois** `strings.xml` ; la taille du fichier passe par `Formatter.formatShortFileSize` (déjà localisé par Android, donc aucune clé à créer). ⚠ Le pourcentage s'écrit `%%`, sinon `String.format` lève
- **`docs/updates.md`** (créé) : le contrat du corps de release, ce que l'app fait et quand, la friction MIUI, la liste « la mise à jour ne s'affiche pas, que vérifier », et la façon d'éprouver la chaîne sans rien publier
- **183 tests JVM verts** (28 nouveaux : parsing du tag et pré-versions refusées, complément par zéros, comparaison numérique, repli fermé des deux côtés ; les cinq portes du verdict et le veto du `versionCode` ; la réponse GitHub sur fixtures — choix de l'asset parmi plusieurs, `.apk` absent, URL en clair rejetée, brouillon, JSON tronqué, les deux empreintes du gabarit, `versionCode` présent/absent/non numérique) ; `assembleDebug` OK
- **Aucune vérification sur appareil** cette session (non demandée) : voir « Prochaine étape »

### Fait (session 15, suite) — release v1.3 préparée
- `versionCode 6` / `versionName 1.3` ; APK release signé **12,86 Mo** (`miqaat-1.3.apk`), signature **v2 + v3**, certificat `1af97066…` — le **même** que la 1.2.1, donc l'installation par-dessus ne perd rien
- SHA-256 de l'APK : `B77660271601027FAA2593613B8A0BF435642FCFA219B379BC41AC6E2F19C35A`
- **`docs/release-notes-v1.3.md`** rédigé selon le gabarit, avec la ligne `versionCode: 6` **seule sur sa ligne** (une puce `- ` devant l'empêcherait de correspondre à l'ancre de début de ligne)
- **`ReleaseNotesContractTest`** (créé) : relit les **vrais** fichiers `docs/release-notes-v*.md` et vérifie que l'app y trouve l'empreinte de l'APK — et non celle du certificat, qui figure dans la même section — et un `versionCode` lisible. ⚠ Une note mal formée ne casse **rien de visible** : elle prive silencieusement de la vérification, ou laisse proposer une version qu'Android refusera. C'est le seul garde-fou, et **il faut ajouter chaque nouveau fichier de notes à son ensemble `contractual`**
- **185 tests JVM verts** (2 nouveaux) ; `assembleRelease` OK
- ⚠ Reste à faire à la main : `git tag -a v1.3`, `git push origin v1.3`, puis la release GitHub par le formulaire web (`gh` n'est pas installé sur cette machine)

### Prochaine étape
- **Publier la v1.3** : tag `v1.3`, release GitHub **ni brouillon ni pré-version** (sinon `/releases/latest` l'ignore et personne ne la voit), APK `miqaat-1.3.apk` joint, notes de `docs/release-notes-v1.3.md` collées telles quelles
- **Éprouver la mise à jour sur appareil** (voir `docs/updates.md`) : ① la note apparaît sur l'accueil au lancement suivant ; ② « plus tard » la fait taire 7 jours, « ignorer cette version » définitivement ; ③ le téléchargement affiche sa progression et survit à un aller-retour vers les réglages ; ④ l'écran des sources inconnues s'ouvre, et le texte manuel apparaît si MIUI le refuse ; ⑤ l'APK s'installe **par-dessus** sans rien perdre (même clé) ; ⑥ à l'ouverture suivante, l'APK est effacé et la note a disparu ; ⑦ mode avion → l'écran reste consultable (cache) et le téléchargement échoue proprement ; ⑧ le tout en arabe RTL **et** en français LTR, en clair **et** en sombre
- **Éprouver le repli navigateur** : couper `com.android.providers.downloads` (ou refuser les sources inconnues) et vérifier que « ouvrir la page de la version » aboutit
- **Vérifier que la vérification ne part pas plus d'une fois par jour** : `run-as com.mohamed.miqaat cat shared_prefs/update.xml` après plusieurs ouvertures — `last_check_at` ne doit bouger qu'une fois
- **Éprouver l'écoute du Coran sur appareil** : ① le catalogue se charge, un récitateur mis en favori remonte en tête ; ② une sourate se lance, les contrôles marchent depuis la notification **et** l'écran verrouillé ; ③ mode avion → message clair et liste toujours consultable (cache) ; ④ **D43 cas sonore** : lancer une sourate, déclencher la notification de test en mode sonnerie → pause, adhan, **reprise seule** ; ⑤ **D43 cas muet** : téléphone en vibreur, même test → pause **sans** reprise, mini-lecteur toujours visible ; ⑥ quitter l'app en lecture → la notification survit ; en pause → elle disparaît ; ⑦ le tout en arabe RTL **et** en français LTR, en clair **et** en sombre
- **Vérifier que la 4ᵉ icône de l'accueil passe** sur un écran de 360 dp à l'échelle de police 1,8 ; si c'est trop serré, l'entrée Coran passe au mini-lecteur et à une ligne dans les réglages
- **Vérifier la migration Room 3→4** sur un appareil qui porte déjà des données (position, invocations) : `user_version = 4` et rien de perdu
- **Vérifier la correction à l'écran** : à Skikda, méthode auto → Algérie, les horaires affichés doivent être ceux du calendrier papier ; contrôler que l'alarme système suit (`dumpsys alarm`), le widget et le calendrier mensuel aussi
- **Obtenir un calendrier officiel d'un mois d'hiver** pour Skikda : c'est la seule façon de savoir si la calibration tient sur deux saisons ou si elle est ajustée sur un mois d'été (voir `docs/prayer-times-accuracy.md`, « Ce qui reste ouvert »)
- **Vérifier sur le Redmi Note 8** (voir `docs/reliability.md`) : ① installer sans rien régler, laisser une nuit → rien n'arrive et `DELIVERY = à corriger` + bannière ; ② suivre les actions de l'écran (autostart, batterie sans restriction) ; ③ nouvelle nuit → les cinq adhans arrivent ; ④ **non-régression du symptôme** : après une nuit sans réglage MIUI, ouvrir l'app à 14h ne doit produire **aucune** notification « approche du Fajr » ; ⑤ `dumpsys package … | grep stopped`
- Éprouver le mode d'alerte sur appareil avec le bouton « notification de test » : 4 modes × 3 états de sonnerie × (adhan, rappel, invocation), en vérifiant **quel curseur de volume** agit (sonnerie vs alarme), puis les trois filtres DND
- ✅ **Release v1.1 publiée** (`versionCode 2`) : `v1.0` était déjà taguée et publiée sur le commit initial du 8 août, tout le reste — sessions 11 et 12 — attendait. `gh` n'est pas installé sur cette machine → la release GitHub passe par le formulaire web, les notes de version sont préparées dans `docs/release-notes-v1.1.md`
- Dette connue : la grille du calendrier rogne les quantièmes hégiriens **au-delà de l'échelle de police ~1,6** (à 1,3 tout tient). Même famille que D30 : une hauteur de case en dur
- Envisager un `applicationIdSuffix = ".debug"` pour que build debug et release cohabitent sur l'appareil de test — au prix d'alarmes, widget et notifications dédoublés
- Vérifier la boussole sur un appareil réel (l'émulateur ne simule pas utilement le magnétomètre)
- Vérifier le widget posé sur l'écran d'accueil (clair/sombre, RTL, bascule à l'heure d'une prière)
- ✅ Rappel **et** adhan vérifiés sur appareil (session 10), mise en pause de la musique comprise. Le comportement en vibreur et en silencieux relève désormais du mode d'alerte (session 12), à éprouver avec la notification de test
- Vérifier l'icône sur un lanceur tiers et en **icône thématisée** (Android 13+, `monochrome`) — l'émulateur n'a montré que le lanceur Pixel
- Vérifier le calendrier sur appareil : navigation entre mois, RTL, clair/sombre, encart Ramadan
- Vérifier l'ajustement manuel sur appareil : un pas doit décaler l'accueil, le calendrier, le widget **et** l'heure de l'alarme système
- Invocations : le gros est vérifié sur émulateur (voir ci-dessus). Restent la **création/suppression d'un du'ā** (saisie clavier non testée) et surtout la vérification que **la garde tient sur un vrai appareil en Doze** — poser une invocation 5 min avant le Fajr et contrôler à `dumpsys alarm` que l'adhan n'est pas reporté
- Relire le contenu des deux adhkār livrés (`invocation_morning_body` / `invocation_evening_body`) : sélection curée, à valider
- Relever la calibration officielle de la Tunisie et du Maroc (protocole de `docs/prayer-times-accuracy.md` : un calendrier officiel d'un mois entier, pas deux dates)
- Finir le multilingue : formats de date (accueil **et** calendrier) et géocodage selon la locale
- Plus tard : activation des notifications par prière · sélection manuelle de ville

> **Règle permanente : à la fin de chaque session de travail importante, mettre à jour cette section « État actuel ».**

## Conventions de code

- Kotlin idiomatique ; noms de classes/fonctions en anglais, textes UI en arabe via `strings.xml` (jamais de texte en dur dans les composables)
- `domain/` ne doit **jamais** importer d'API Android (testable en JVM pur)
- Un composable d'écran (`XxxScreen`) reçoit son état via le ViewModel ; les composables enfants reçoivent des données simples (pas le ViewModel)
- Compose : utiliser `start`/`end` (jamais `left`/`right`) pour que le RTL fonctionne automatiquement
- Dépendances déclarées dans `gradle/libs.versions.toml` (version catalog)

## Commandes utiles

```powershell
# Compiler l'APK debug
.\gradlew.bat assembleDebug

# Lancer les tests unitaires (JVM, rapides)
.\gradlew.bat :app:testDebugUnitTest

# Installer sur l'émulateur/appareil connecté
.\gradlew.bat installDebug
# ou : adb install app\build\outputs\apk\debug\app-debug.apk

# Nettoyer le build
.\gradlew.bat clean
```

APK debug généré dans `app/build/outputs/apk/debug/app-debug.apk`.

### Particularité : build lancé depuis Claude Code (sur cette machine)

L'environnement Claude Code bloque les sockets AF_UNIX que le JDK ≥ 16 utilise pour ses pipes NIO internes (erreur « Unable to establish loopback connection »). Contournement : pointer `jdk.net.unixdomain.tmpdir` vers un dossier inexistant pour forcer le repli sur le loopback TCP :

```powershell
$fix = "-Djdk.net.unixdomain.tmpdir=C:\claude-afunix-fallback-inexistant"
$env:GRADLE_OPTS = $fix
$env:JAVA_TOOL_OPTIONS = $fix   # indispensable : hérité par le daemon Gradle et le daemon Kotlin
.\gradlew.bat assembleDebug "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 $fix" "-Pkotlin.daemon.jvmargs=-Xmx1024m $fix"
```

Constaté en session 2 : sans `JAVA_TOOL_OPTIONS`, le `-Dorg.gradle.jvmargs` de la ligne de commande n'atteint pas toujours le daemon (« A new daemon was started but could not be connected to »). Si ce message apparaît : `.\gradlew.bat --stop` puis relancer avec `JAVA_TOOL_OPTIONS` posé.

Les builds depuis Android Studio ou un terminal normal ne sont **pas** concernés.
