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
| **AlarmManager (alarmes exactes) + WorkManager** | AlarmManager `setExactAndAllowWhileIdle` pour la ponctualité même en Doze ; WorkManager pour replanifier (reboot, changement de jour) |
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

### v1.2+
Thème sombre/clair · tracker de prières · événements du calendrier islamique · horaires de jeûne Ramadan

### Plus tard
Multilingue (FR/AR/EN) · sons d'adhan personnalisés · statistiques

## Contraintes techniques permanentes

- Permissions : `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, `POST_NOTIFICATIONS` (Android 13+), localisation — à demander proprement avec explications
- `BOOT_COMPLETED` → replanifier toutes les alarmes après reboot
- Doze mode : utiliser `setExactAndAllowWhileIdle` / `setAlarmClock` ; les notifications ne doivent JAMAIS être en retard
- Aucune dépendance réseau pour les fonctionnalités cœur ; aucun SDK tiers de tracking

## Documentation

Depuis la session 5, `docs/` complète ce fichier — voir [docs/INDEX.md](docs/INDEX.md) :
`dev-workflow.md` (build, rituel, conventions), `decisions.md` (choix d'architecture
et leurs raisons), `file-map.md` (carte des fichiers), `i18n.md` (multilingue).
`CLAUDE.md` reste la mémoire vivante : vision, stack, roadmap, État actuel.

## État actuel

**Dernière mise à jour : 2026-08-08 (fin de session 10)**

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
- **Canaux muets** (`setSound(null, null)`) sinon double son → IDs bumpés en **`prayer_times_v3`** et **`prayer_reminder_v2`** (les anciens sont supprimés). La **vibration reste au canal**, donc Android suit tout seul le mode du téléphone ; le service applique la même règle au son via `AudioManager.ringerMode`
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

### Prochaine étape
- Vérifier la boussole sur un appareil réel (l'émulateur ne simule pas utilement le magnétomètre)
- Vérifier le widget posé sur l'écran d'accueil (clair/sombre, RTL, bascule à l'heure d'une prière)
- ✅ Rappel **et** adhan vérifiés sur appareil (session 10), mise en pause de la musique comprise. Reste à contrôler le comportement en **mode vibreur et en silencieux** (le service applique lui-même la règle du `ringerMode`, jamais éprouvé)
- Vérifier que les alarmes tiennent **app fermée** sur ce Redmi (MIUI tue agressivement l'arrière-plan) : démarrage automatique activé + batterie « aucune restriction », puis un adhan attendu sans rouvrir l'app
- Vérifier l'icône sur un lanceur tiers et en **icône thématisée** (Android 13+, `monochrome`) — l'émulateur n'a montré que le lanceur Pixel
- Vérifier le calendrier sur appareil : navigation entre mois, RTL, clair/sombre, encart Ramadan
- Vérifier l'ajustement manuel sur appareil : un pas doit décaler l'accueil, le calendrier, le widget **et** l'heure de l'alarme système
- Invocations : le gros est vérifié sur émulateur (voir ci-dessus). Restent la **création/suppression d'un du'ā** (saisie clavier non testée) et surtout la vérification que **la garde tient sur un vrai appareil en Doze** — poser une invocation 5 min avant le Fajr et contrôler à `dumpsys alarm` que l'adhan n'est pas reporté
- Relire le contenu des deux adhkār livrés (`invocation_morning_body` / `invocation_evening_body`) : sélection curée, à valider
- Relever la marge officielle du Maghrib pour la Tunisie et le Maroc (même protocole que D23 : deux dates éloignées)
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
