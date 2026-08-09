# Carte des fichiers

Package racine : `com.mohamed.miqaat` (`app/src/main/java/com/mohamed/miqaat/`).

## Entrée de l'application

| Fichier | Rôle |
|---|---|
| `MiqaatApp.kt` | `Application` : singletons `by lazy` (base Room, repositories), création du canal de notification. `Context.miqaatApp` donne accès à tout depuis n'importe où, receivers compris. |
| `MainActivity.kt` | Unique activité : demande les permissions, resynchronise la chaîne d'alarmes, superpose l'écran de démarrage, et bascule entre les écrans (`enum Screen`). |

## `domain/` — logique métier pure (aucune API Android, testable en JVM)

| Fichier | Rôle |
|---|---|
| `PrayerTimesCalculator.kt` | Enveloppe d'Adhan : coordonnées + date + méthode + madhab → `DailyPrayerTimes`. |
| `NextPrayerResolver.kt` | Prochaine prière, jamais null (après l'Isha → Fajr du lendemain). Sert à l'**affichage**. |
| **`PrayerEventResolver.kt`** | Évènements de prière : rappel avant l'adhan ou adhan (`PrayerEvent`, `PrayerEventKind`). Voir D17. |
| **`AlarmEventResolver.kt`** | Prochain évènement de la chaîne, prières **et** invocations (`ScheduledEvent`) ; porte la garde de dix minutes qui protège l'adhan. Voir D25 et [invocations.md](invocations.md). |
| `HijriFormatter.kt` | Date hégirienne (Umm al-Qura) avec décalage manuel : jour complet, mois + année, conversion brute (`toHijri`) et les extensions `hijriDayOfMonth` / `hijriMonth` / `isRamadan`. |
| **`MonthGrid.kt`** | Découpe un mois grégorien en cases de grille alignées sur le premier jour de la semaine (`monthGridCells`, `weekdaysFrom`). |
| **`RamadanTimes.kt`** | Imsāk et iftār déduits du Fajr et du Maghrib, durée du jeûne (`IMSAK_MINUTES_BEFORE_FAJR`). Voir D22. |
| **`AlarmFreshness.kt`** | Tolérances de retard par nature d'évènement : une alerte délivrée trop tard ne s'affiche pas. Voir D31. |
| **`NotificationAlert.kt`** | `AlertResolver` : mode d'alerte × état du téléphone → `AlertDecision` (flux audio, style de vibration). Voir D36. |
| **`reliability/ReliabilityCheck.kt`** | Les cinq contrôles de fiabilité, leur état, et `ReliabilityVerdict` — dont la règle « un état inconnu n'alarme jamais ». Voir D34. |
| `CountdownFormatter.kt` | `formatCountdown(Duration)`. |
| `AutoMethodResolver.kt` | Code pays ISO → méthode de calcul ; `CalculationSettings.effectiveMethod()`. |
| **`QiblaCalculator.kt`** | Azimut de la Qibla (grand cercle) et distance à la Kaaba (haversine) + utilitaires d'angles (`normalizeDegrees`, `shortestAngleDelta`, `isAlignedWithQibla`, `lerpDegrees`). |
| `model/PrayerName.kt`, `model/DailyPrayerTimes.kt` | Les six moments du jour. |
| `model/MethodOption.kt` | Les 21 méthodes de calcul (11 d'Adhan + 10 nationales reconstruites). |
| `model/CalculationSettings.kt` | Méthode, mode auto, madhab, décalage hégirien, ajustements manuels. |
| **`model/PrayerTimeAdjustments.kt`** | Minutes ajoutées manuellement à chaque moment (±30). S'additionnent à la marge de la méthode ; les zéros ne sont jamais stockés. Voir D24. |
| **`model/ReminderSettings.kt`** | Rappel avant l'adhan : actif ou non, délai (`LEAD_CHOICES`, minimum 10 min — voir D18). |
| **`model/NotificationMode.kt`** | Suivre le téléphone, toujours sonner, toujours vibrer, toujours silencieux. |
| **`model/NotificationSettings.kt`** | Réglages de **rendu** des alertes, tenus à part de la planification. |
| **`model/Invocation.kt`** | Une invocation (livrée ou écrite) et son moment (`InvocationSchedule` : heure fixe ou ancrage à une prière). Voir D26. |

## `data/` — sources de données

| Fichier | Rôle |
|---|---|
| `location/LocationRepository.kt` | Interface + `GeoLocation` + `FixedLocationRepository` (Skikda, previews/tests). |
| `location/CachedLocationRepository.kt` | Cascade mémoire → Room → défaut ; `refresh()` = fix + géocodage + upsert. |
| `location/DeviceLocationDataSource.kt` | Fix ponctuel via `LocationManager` (sans Play Services). |
| `location/CityNameResolver.kt` | `Geocoder` → `ResolvedPlace(cityName, countryCode)`, best-effort. |
| `settings/SettingsRepository.kt` | DataStore Preferences : instantané mémoire pour les lecteurs synchrones + `Flow` pour l'UI. |
| **`settings/AppLocale.kt`** | `AppLanguage` + langue choisie dans l'app : stockage `SharedPreferences` (lecture synchrone) et `wrap(context)` pour habiller un contexte. Voir [i18n.md](i18n.md). |
| `db/` | Room : `MiqaatDatabase` (v3), `CachedLocationEntity` (singleton id=1), `LocationDao`, **`InvocationEntity` + `InvocationDao`**. |
| **`invocations/InvocationRepository.kt`** | Les invocations : instantané mémoire + `Flow`, semis idempotent des deux entrées livrées (ids fixes). Voir [invocations.md](invocations.md). |
| **`reliability/ReliabilityLog.kt`** | `SharedPreferences` : dernière alerte réellement délivrée, report de la bannière, étape OEM acquittée. Seul moyen de détecter le gel de l'app par une surcouche. |
| **`reliability/ReliabilityInspector.kt`** | Interroge les cinq verrous (notifications, alarmes exactes, batterie, démarrage auto, délivrance) et ouvre l'écran système qui corrige chacun. |
| **`reliability/OemAutostart.kt`** | Table `Build.MANUFACTURER` → écrans de démarrage automatique des surcouches. ⚠ Exige le bloc `<queries>` du manifeste. |
| **`compass/CompassDataSource.kt`** | Capteurs → `Flow<CompassReading>` : choix du capteur et replis, permutation des axes selon la rotation de l'écran, déclinaison magnétique (`GeomagneticField`, hors ligne), lissage circulaire. |

## `notifications/`

| Fichier | Rôle |
|---|---|
| **`NotificationChannels.kt`** | Les trois canaux — `prayer_times_v4`, `prayer_reminder_v3`, `invocations_v2` — **tous muets et sans vibration** : l'app joue le son (D20) et vibre (D38) elle-même. ⚠ Tout changement de réglage d'un canal impose de bumper son ID **et** d'ajouter l'ancien à `OLD_IDS`. |
| **`PrayerNotifications.kt`** | Point unique de construction : identifiant, canal, son et contenu traduit d'un évènement de prière. Partagé par le receiver et le service. |
| **`InvocationNotifications.kt`** | La notification d'une invocation : texte déplié, appui → l'invocation ouverte dans l'app. Suit le mode d'alerte depuis D39. |
| **`AlertSoundService.kt`** | Service d'avant-plan : focus audio (`AUDIOFOCUS_GAIN_TRANSIENT`, met la musique en pause) + `MediaPlayer`, sur le flux décidé par le receiver. `onTimeout()` pour le `shortService` d'Android 14+. Voir D20 et D37. |
| **`AlertVibrator.kt`** | La vibration, en motifs finis. Appelé **depuis le receiver** : l'effet confié au service système survit à la mort du processus. Voir D38. |
| **`RingerReader.kt`** | Mode sonnerie + filtre « Ne pas déranger » → `RingerState`. Seul point de lecture Android de l'état sonore. |
| `PrayerAlarmScheduler.kt` | Une alarme exacte à la fois (`setExactAndAllowWhileIdle`, replis `setAlarmClock` puis inexact), posée sur le prochain **évènement** ; `nextEvent()` l'expose sans rien programmer ; pose aussi le chien de garde (D33). |
| `PrayerAlarmReceiver.kt` | Vérifie la fraîcheur (D31), pose la notification, décide de l'alerte (D36), vibre, lance le son, puis planifie l'évènement suivant dans un `finally`. ⚠ **Ne pas renommer** : les alarmes déjà posées pointent sur ce nom de classe. |
| `RescheduleReceiver.kt` | `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, changement de permission d'alarme exacte, et le chien de garde. `exported="true"` — voir D32. |

## `widget/` — widget d'écran d'accueil

| Fichier | Rôle |
|---|---|
| **`NextPrayerWidget.kt`** | `AppWidgetProvider` : `onUpdate` + `refresh(context)`, appelé par `PrayerAlarmScheduler` (voir D15). |
| **`NextPrayerWidgetViews.kt`** | Construit les `RemoteViews` : mêmes sources que l'accueil, rendues en texte + identifiants de ressources uniquement. |

## `ui/`

| Fichier | Rôle |
|---|---|
| `theme/` | Palettes claire et sombre figées, typographie IBM Plex Sans Arabic, et les trois couleurs de marque de l'écran de démarrage (hors schéma Material). |
| **`splash/SplashScreen.kt`** | Écran de démarrage Compose : logo, nom, baseline. Prend le relais de l'écran système d'Android 12+ et en tient lieu en dessous (D28). |
| `PrayerLabels.kt` | `PrayerName.labelRes` — partagé écran et notifications. |
| **`InvocationLabels.kt`** | Titre et texte d'une invocation livrée (ressources) ou saisie (base) : `displayTitle` / `displayBody`. |
| `home/HomeScreen.kt` | Écran d'accueil ; les deux boutons du haut ouvrent la Qibla (start) et les réglages (end). |
| `home/HeroSection.kt`, `home/PrayerList.kt` | En-tête dégradé et liste des six moments. |
| `home/HomeViewModel.kt` | Tick 1 s aligné sur l'horloge, arrêté en arrière-plan ; recalcul si (date, position, réglages) change. |
| `settings/SettingsScreen.kt`, `settings/SettingsViewModel.kt` | Méthode, madhab, décalage hégirien, rappel avant l'adhan, **mode d'alerte**, accès à la fiabilité, langue ; tout changement replanifie l'alarme. |
| **`reliability/ReliabilityScreen.kt`** | Les cinq contrôles avec leur bouton de correction, la carte du démarrage automatique, prochaine alerte / dernière reçue, et une notification de test. |
| **`reliability/ReliabilityViewModel.kt`**, **`reliability/ReliabilityUiState.kt`** | Diagnostic réévalué à chaque `ON_RESUME` ; contexte de l'application, jamais celui de l'activité. |
| **`reliability/ReliabilityBanner.kt`** | L'avertissement d'accueil, affiché seulement sur du critique et du certain, avec report de 14 jours. Voir D34. |
| **`qibla/QiblaScreen.kt`** | Écran Qibla : en-tête, cadran, message d'état, angle et distance, vibration à l'alignement. |
| **`qibla/QiblaCompass.kt`** | Le cadran, entièrement dessiné au Canvas avec les couleurs du thème. |
| **`qibla/QiblaViewModel.kt`**, **`qibla/QiblaUiState.kt`** | Position figée à l'ouverture + flux des capteurs ; libérés dès que l'écran disparaît. |
| **`calendar/CalendarScreen.kt`** | Écran calendrier : en-tête de mois, grille, jour ouvert (réutilise `PrayerList`), encart du jeûne. |
| **`calendar/MonthGrid.kt`** | La grille elle-même : en-tête des sept colonnes et cases cliquables (sélection, aujourd'hui, Ramadan). |
| **`calendar/CalendarViewModel.kt`**, **`calendar/CalendarUiState.kt`** | Mois affiché + jour sélectionné ; sans ticker, les horaires ne sont calculés que pour le jour ouvert. Voir D21. |
| **`invocations/InvocationsScreen.kt`** | Liste des adhkār (moment résumé, interrupteur), et aiguillage vers la lecture ou l'éditeur. |
| **`invocations/InvocationDetail.kt`** | Lecture d'une invocation : interligne large pour les diacritiques. |
| **`invocations/InvocationEditor.kt`** | Dialogue de création/modification : titre, texte, heure fixe (`TimePicker` M3) ou ancrage à une prière. |
| **`invocations/InvocationsViewModel.kt`**, **`invocations/InvocationsUiState.kt`** | Liste + invocation lue + brouillon d'édition, tout dans le ViewModel (donc D7 tient toujours). Chaque écriture replanifie l'alarme. |

## Ressources

| Chemin | Rôle |
|---|---|
| `res/values/strings.xml` | Arabe (langue par défaut). |
| `res/values-fr/`, `res/values-en/` | Français, anglais — voir [i18n.md](i18n.md). |
| `res/xml/locales_config.xml` | Langues exposées au sélecteur d'Android 13+. |
| `res/xml/widget_next_prayer_info.xml` | Descripteur du widget (taille 4×2, aperçu, période de secours). |
| `res/layout/widget_next_prayer.xml` | Mise en page du widget (RemoteViews : ville, prochaine prière, `Chronometer`, cinq créneaux `widget_slot_1..5`). |
| `res/values/colors.xml`, `values-night/colors.xml` | `splash_background` (vert de marque, **sans variante nuit**) + **palette `widget_*`** (à garder synchronisée avec `Color.kt`). |
| `res/values/themes.xml`, `values-night/themes.xml`, `values-v31/themes.xml` | `Base.Theme.Miqaat` porte le parent clair/sombre et le `windowBackground` ; `Theme.Miqaat` en hérite, et sa variante v31 ajoute les attributs de l'**écran de démarrage système** (D28). |
| `res/mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml` | Icône adaptative (fond + avant-plan + `monochrome`). Aucun bitmap de secours : `minSdk 26`, tous les appareils lisent l'adaptative. |
| `res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml` | **Le logo Miqaat** — coupole horlogère et minaret, blancs et pleins, sur le vert de la marque. L'avant-plan sert aussi de logo à l'écran de démarrage (système **et** Compose) : une seule source pour le dessin. |
| `res/drawable/ic_settings.xml`, `ic_arrow_back.xml`, `ic_arrow_forward.xml`, `ic_qibla.xml`, `ic_calendar.xml`, `ic_invocation.xml` (chapelet) | Icônes vectorielles maison (le BOM Compose 2026 ne fournit plus `material-icons`). Les deux flèches sont `autoMirrored` : en RTL, « mois précédent » pointe bien à droite. |
| `res/drawable/ic_mosque.xml` | Mosquée minimaliste (coupole, minarets, mosaïque en creux `evenOdd`) — icône du widget. |
| `res/drawable/widget_background.xml`, `widget_mosaic.xml`, `widget_slot_next.xml`, `widget_preview.xml` | Carte translucide du widget, treillis de mosaïque, pastille de la prochaine prière, aperçu du sélecteur. |
| `res/raw/prayer_notification.wav` | Appel à la prière du canal de notification. |
| `res/raw/prayer_reminder.mp3` | Son du rappel avant l'adhan. |
| `res/font/` | IBM Plex Sans Arabic (SIL OFL), trois graisses. |

## Tests (`app/src/test/`)

Un fichier par unité de domaine : horaires, resolver, Hijri, countdown, méthodes,
mapping pays, Qibla, évènements d'alarme et réglages du rappel, grille de mois et
horaires de Ramadan, **matrice du mode d'alerte, garde de fraîcheur et verdict de
fiabilité**. Tous en JVM pur, aucun émulateur nécessaire.

## Où toucher pour…

| Évolution | Fichiers |
|---|---|
| Ajouter une méthode de calcul nationale | `model/MethodOption.kt` + `AutoMethodResolver.kt` + les trois `strings.xml` + `SettingsScreen.selectableMethods` |
| Corriger la marge officielle d'un pays | `model/MethodOption.kt` (`maghribMinutes`…), **après mesure sur deux dates éloignées** — voir D23 |
| Ajouter une entrée au calcul des horaires | `PrayerTimesCalculator.calculate` + ses **quatre** appelants : accueil, calendrier, `PrayerAlarmScheduler`, `NextPrayerWidgetViews` |
| Ajouter un écran | `MainActivity.Screen` + un dossier `ui/<écran>/` |
| Changer le logo ou l'écran de démarrage | `res/drawable/ic_launcher_foreground.xml` (le dessin, réutilisé partout) · `splash_background` + `SplashGradient*` **ensemble** · `ui/splash/SplashScreen.kt` pour le texte · `SPLASH_DURATION_MS` pour la durée |
| Changer le son d'un évènement | `res/raw/` + `PrayerNotifications.soundOf` (les canaux sont muets, aucun ID à bumper) |
| Ajouter un évènement à la chaîne d'alarmes | `ScheduledEvent` + `AlarmEventResolver` + le branchement de `PrayerAlarmReceiver` |
| Ajouter une invocation livrée, changer la garde | voir [invocations.md](invocations.md) |
| Changer la tolérance d'alignement Qibla | `isAlignedWithQibla` (`domain/QiblaCalculator.kt`) |
| Changer la marge de l'imsāk | `IMSAK_MINUTES_BEFORE_FAJR` (`domain/RamadanTimes.kt`) — le texte d'aide suit tout seul (plurals) |
| Changer l'aspect d'une case du calendrier | `ui/calendar/MonthGrid.kt` ; le contenu d'une case vient de `CalendarDayUi` |
| Modifier le widget | `res/layout/widget_next_prayer.xml` + `widget/NextPrayerWidgetViews.kt` ; une couleur nouvelle → **les deux** `colors.xml` |
| Changer le comportement sonore ou vibratoire | `domain/NotificationAlert.kt` (la matrice) + `AlertVibrator` (les motifs) + `AlertSoundService` (les flux) — voir [notifications.md](notifications.md) |
| Ajouter un fabricant à la liste du démarrage automatique | `data/reliability/OemAutostart.kt` **et** le bloc `<queries>` du manifeste — l'oubli le plus probable |
| Changer une tolérance de retard | `domain/AlarmFreshness.kt` ; celle du rappel doit rester **sous** `LEAD_CHOICES.min()` |
| Ajouter un texte | les **trois** `strings.xml` |
| Ajouter une surface hors activité (widget, notification…) | l'entourer de `AppLocale.wrap()`, sinon elle ignore la langue choisie dans l'app |
