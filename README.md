# Miqaat — مِيقات

> مواقيت الصلاة أينما كنت، دون إنترنت
> *Les horaires de prière où que vous soyez, sans Internet.*

Application Android native de temps de prière (Salat), **100 % gratuite**, **sans publicité**,
**sans achat intégré** et **sans aucun SDK de tracking ou d'analytics**.

Tous les calculs sont **astronomiques et locaux** : après la première localisation, l'application
n'a plus jamais besoin du réseau.

---

## Fonctionnalités

- **Les cinq prières + le shurūq**, avec la prochaine mise en évidence et un compte à rebours
- **Notifications à l'heure exacte** — fiables application fermée, téléphone en Doze, et
  replanifiées automatiquement après un redémarrage
- **Rappel avant l'adhan** (10 à 60 min), sur son propre canal de notification réglable à part
- **Boussole Qibla** : azimut par le cap initial du grand cercle, déclinaison magnétique corrigée
  hors ligne (modèle WMM embarqué), distance à la Kaaba
- **Calendrier mensuel** grégorien + hégirien, horaires de n'importe quel jour, et encart
  **imsāk / iftār** pendant le Ramadan
- **Invocations et adhkār** : deux sélections livrées (matin, soir) plus les vôtres, rappelées à
  une heure fixe ou relativement à une prière
- **Widget d'écran d'accueil** translucide, motif girih, compte à rebours sans réveil du processeur
- **Méthode de calcul automatique selon le pays** (21 méthodes, dont 10 méthodes nationales
  absentes des librairies courantes), madhab, décalage hégirien, et **ajustement manuel** de
  chaque horaire à la minute
- **Arabe (RTL), français et anglais** — l'arabe est la langue par défaut ; la langue se choisit
  dans l'application, indépendamment du téléphone

## Permissions demandées, et pourquoi

| Permission | Usage |
|---|---|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Sonner l'adhan **à la minute**, même en veille profonde |
| `POST_NOTIFICATIONS` | Afficher l'appel à la prière (Android 13+) |
| `ACCESS_COARSE_LOCATION` | Calculer les horaires pour votre position — la position **ne quitte jamais l'appareil** |
| `RECEIVE_BOOT_COMPLETED` | Replanifier les alarmes après un redémarrage |
| `FOREGROUND_SERVICE` | Jouer l'adhan en demandant le focus audio, pour mettre la musique en pause |

Aucune permission Internet n'est requise par le cœur de l'application.

## Stack technique

- **Kotlin** + **Jetpack Compose** / Material 3 (Android natif, pas de framework cross-platform :
  la ponctualité des alarmes en dépend)
- [**Adhan**](https://github.com/batoulapps/adhan-kotlin) `com.batoulapps.adhan:adhan2` — calcul
  astronomique 100 % local
- **AlarmManager** (`setExactAndAllowWhileIdle`) pour la ponctualité, **WorkManager** pour la
  replanification
- **Room** (cache de position, invocations) et **DataStore** (réglages)
- **MVVM** : ViewModel + StateFlow
- `minSdk 26` · `targetSdk 36` · AGP 9.2.1 · Kotlin 2.2.10 · Compose BOM 2026.02.01

## Architecture

```
com.mohamed.miqaat
  domain/          Logique métier pure — aucune API Android, testable en JVM
    model/         PrayerName, DailyPrayerTimes, MethodOption, Invocation…
  data/            Sources de données
    location/      Position (GPS natif, sans Play Services) + cache Room
    settings/      DataStore + langue de l'application
    compass/       Capteurs de la boussole
    invocations/   Adhkār
  ui/              Compose + ViewModels
    home/ settings/ qibla/ calendar/ invocations/ splash/ theme/
  notifications/   Alarmes exactes, receivers, canaux, service sonore
  widget/          Widget d'écran d'accueil (RemoteViews)
```

Le détail vit dans [`docs/`](docs/INDEX.md) — notamment
[`decisions.md`](docs/decisions.md) (les choix d'architecture et leurs raisons) et
[`file-map.md`](docs/file-map.md) (où vit quoi). [`CLAUDE.md`](CLAUDE.md) tient la mémoire du
projet : vision, roadmap et état d'avancement.

## Compiler

```bash
git clone https://github.com/mugi21/miqaat.git
cd miqaat
```

Ouvrir dans Android Studio (qui génère `local.properties`), ou en ligne de commande :

```bash
./gradlew assembleDebug          # APK debug → app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest # 79 tests JVM
./gradlew installDebug           # installer sur l'appareil connecté
```

Sous Windows, `gradlew.bat`. Voir [`docs/dev-workflow.md`](docs/dev-workflow.md) pour les détails.

## Licence

Copyright (C) 2026 Mohamed Boughouas

Ce programme est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon les termes
de la **GNU General Public License version 3** telle que publiée par la Free Software Foundation.

Il est distribué dans l'espoir qu'il sera utile, mais **SANS AUCUNE GARANTIE**. Voir le fichier
[LICENSE](LICENSE) pour le texte complet.

### Crédits

- Police **IBM Plex Sans Arabic** — SIL Open Font License 1.1
- Librairie **Adhan** (batoulapps) pour le calcul astronomique
- Paramètres des méthodes nationales : documentation publique de l'API AlAdhan
- Enregistrements audio (`prayer_notification.wav`, `prayer_reminder.mp3`) : réalisés pour ce projet
