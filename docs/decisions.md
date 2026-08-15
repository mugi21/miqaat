# Décisions d'architecture

Ordre chronologique. Chaque entrée : le choix, la raison, et ce qu'il implique.

## D1 — Android natif (Kotlin) plutôt que Flutter

La fiabilité des alarmes exactes et des notifications en arrière-plan est le cœur
du produit ; c'est justement ce que les frameworks cross-platform gèrent mal.
**Conséquence :** pas de version iOS sans réécriture — assumé.

## D2 — Adhan (`com.batoulapps.adhan:adhan2`) pour les horaires

Calcul astronomique 100 % local, aucun appel réseau, librairie de référence.
**Conséquence :** les méthodes absentes de la librairie doivent être reconstruites
à la main (voir D8) ; l'angle Maghrib n'existe pas → méthode de Téhéran non implémentable.

## D3 — AlarmManager exact + WorkManager, pas de service en avant-plan

`setExactAndAllowWhileIdle` pour la ponctualité en Doze, repli `setAlarmClock` si la
permission d'alarme exacte est révoquée. Une seule alarme à la fois : le receiver
notifie **puis** planifie la suivante (chaîne). `BOOT_COMPLETED` / `TIME_SET` /
`TIMEZONE_CHANGED` relancent la chaîne.
**Conséquence :** rien à maintenir en mémoire, mais toute rupture de la chaîne est
fatale → `MainActivity` resynchronise à chaque ouverture (filet de sécurité).

## D4 — Thème vert émeraude figé, `dynamicColor = false`

Identité de marque et lisibilité contrôlée en clair comme en sombre, plutôt que
les couleurs dynamiques du téléphone.
**Conséquence :** toute UI nouvelle doit puiser dans `colorScheme` — y compris les
dessins Canvas, qui n'héritent de rien automatiquement.

## D5 — Géolocalisation via `LocationManager`, sans Google Play Services

L'app doit rester installable hors Play Store et sans dépendance propriétaire.
Fix ponctuel « coarse », `lastKnown` si récent (< 30 min), sinon `getCurrentLocation`.
**Conséquence :** moins précis et plus lent que le fused provider — acceptable, la
précision « ville » suffit aux horaires comme à la Qibla.

## D6 — Cache Room + repli en cascade pour la position

`mémoire → Room → défaut Skikda`. Le pays vient du `Geocoder` quand il répond,
sinon de `android.icu.util.TimeZone.getRegion(zoneId)` (100 % hors ligne).
**Conséquence :** l'app est pleinement fonctionnelle réseau coupé et permission révoquée.

## D7 — Pas de librairie de navigation

Quatre écrans à plat depuis l'accueil (accueil, réglages, Qibla, calendrier) : un
`enum Screen` dans `rememberSaveable` + `BackHandler` suffisent.
**Conséquence :** à revoir dès qu'un écran aura des arguments ou une pile profonde.
Le calendrier, qu'on croyait déclencheur, ne l'a pas été — voir D21.

## D8 — `MethodOption` maison plutôt que `CalculationMethod` d'Adhan

Dix méthodes nationales manquantes à la librairie, reconstruites via
`CalculationMethod.OTHER.parameters.copy(...)`. Les `name` des entrées communes sont
conservés → compatibilité avec ce qui est déjà écrit dans DataStore.
**Conséquence :** ne jamais renommer une entrée de l'enum sans migration.

## D9 — Sélection automatique de la méthode selon le pays

`AutoMethodResolver` mappe le code pays ISO alpha-2 vers la méthode de son ministère.
Mode auto **activé par défaut** ; un choix manuel l'écrit et désactive l'auto dans le
même `edit`, en conservant le dernier choix manuel.
**Conséquence :** une méthode nationale nouvelle se déclare à deux endroits
(`MethodOption` + le mapping).

## D10 — Qibla : géométrie dans `domain/`, capteurs dans `data/`

`QiblaCalculator` (cap de grand cercle + haversine) est du JVM pur, testé sans
émulateur. `CompassDataSource` isole tout ce qui est Android : capteurs, permutation
des axes selon la rotation de l'écran, déclinaison magnétique.
**Conséquence :** la formule est vérifiable par des tests ; les capteurs restent
remplaçables (un jour : lissage différent, ou capteur de rotation seul).

## D11 — Qibla : nord **géographique**, via `GeomagneticField`

Le capteur donne un cap magnétique ; viser la Kaaba demande le nord géographique.
`android.hardware.GeomagneticField` embarque le modèle WMM dans le système → la
déclinaison est calculée **sans réseau**, à partir de la position déjà en cache.
**Conséquence :** l'écran Qibla marche hors ligne au même titre que les horaires.
Écart typique corrigé : de 1° à 15° selon le lieu — non négligeable.

## D12 — Qibla : `TYPE_ROTATION_VECTOR` d'abord, deux replis

Ordre : vecteur de rotation (fusionné, le plus stable) → vecteur de rotation
géomagnétique → accéléromètre + magnétomètre bruts. Sans magnétomètre du tout,
l'écran affiche l'angle et la distance mais pas la visée.
**Conséquence :** `uses-feature compass` est déclaré `required="false"` — l'app reste
installable sur les tablettes sans boussole.

## D13 — Trois langues dès maintenant (ar par défaut, fr, en)

L'arabe vit dans `res/values/` (défaut du système de ressources), le français et
l'anglais dans `values-fr/` et `values-en/`. `res/xml/locales_config.xml` permet le
choix de langue par application (Android 13+).
**Conséquence :** toute chaîne nouvelle doit être ajoutée dans les **trois** fichiers,
sous peine de retomber silencieusement en arabe. Voir [i18n.md](i18n.md).

## D14 — Widget en `RemoteViews` classiques, pas en Glance

Glance aurait permis d'écrire le widget en Compose, mais il ajoute une dépendance
(et son propre compilateur de composition) pour une seule vue statique, sur une
chaîne de build déjà particulière (AGP 9, KSP, workaround AF_UNIX). Un
`AppWidgetProvider` + une mise en page XML couvrent exactement le besoin.
**Conséquence :** le widget se style en XML, pas avec `MaterialTheme`. Comme le
lanceur inflate la vue **dans son propre processus**, on ne lui transmet que du
texte et des **identifiants de ressources** : les couleurs vivent dans
`values/colors.xml` + `values-night/colors.xml` (préfixe `widget_`, valeurs
recopiées de `Color.kt`), jamais résolues côté app — sinon le mode sombre
suivrait notre processus et non le lanceur.
**Aspect :** carte translucide (alpha ≈ 78 %) laissant transparaître le fond
d'écran, liseré vert et treillis de losanges (`widget_mosaic.xml`) encarté de
14dp — une `layer-list` ne sait pas rogner les coins arrondis, l'encart évite
donc que les traits dépassent. La taille posée dépend du nombre de cellules
(`targetCellWidth/Height`, 3×2), pas de la hauteur du contenu.

## D15 — Le widget se rafraîchit sur la chaîne d'alarmes, jamais tout seul

`PrayerAlarmScheduler.scheduleNext()` se termine par `NextPrayerWidget.refresh()`.
Or ce point est déjà appelé à chaque évènement qui rend les horaires obsolètes :
heure d'une prière, reboot, changement d'heure ou de fuseau, ouverture de l'app,
rafraîchissement de la position, modification des réglages. Un seul point de
resynchronisation, donc aucun cas oublié — et le widget ne coûte pas un réveil
supplémentaire. Le compte à rebours, lui, est tenu par un `Chronometer`
(`setChronometerCountDown`) qui défile **côté lanceur**, sans aucune mise à jour.
**Conséquence :** `updatePeriodMillis` (30 min) n'est qu'un filet de sécurité ;
et l'appel au widget est placé **après** la pose de l'alarme, pour que la chaîne
des notifications ne puisse jamais dépendre de lui.

## D16 — Langue choisie dans l'app : contextes habillés, pas `LocaleManager`

`LocaleManager` (le per-app language d'Android) ne existe qu'à partir de l'API 33,
et son rétroportage passe par `AppCompatDelegate`, donc par AppCompat — que
l'app n'utilise pas (Compose sur `ComponentActivity`). Choix : appliquer la
langue nous-mêmes en habillant les `Context` (`createConfigurationContext`), un
seul chemin de code de l'API 26 à 36 et zéro dépendance.
**Conséquences :**
- le tag de langue est stocké en `SharedPreferences` et non dans DataStore :
  `attachBaseContext` s'exécute avant tout et doit être **synchrone** ;
- changer la langue **recrée l'activité** (`recreate()`), seul moyen de relancer
  `attachBaseContext` ;
- tout ce qui lit une ressource hors activité doit appeler `AppLocale.wrap()` —
  aujourd'hui le widget, la notification de prière et le nom du canal ;
- le défaut « langue du téléphone » n'habille rien, donc le sélecteur système
  d'Android 13+ reste pleinement fonctionnel ; un choix dans l'app prime.
Voir [i18n.md](i18n.md).

## D17 — Le rappel avant l'adhan est un évènement de la **même** chaîne d'alarmes

Deux chaînes parallèles (une pour les adhans, une pour les rappels) auraient doublé
les points de rupture possibles — et D3 dit que toute rupture est fatale. Choix :
généraliser la chaîne. `PrayerEventResolver` produit la suite des évènements des
deux jours — pour chaque prière, un `REMINDER` puis un `ADHAN` — et rend le premier
à venir. `PrayerAlarmScheduler` continue de poser **une seule alarme à la fois**, et
le receiver lit un `EXTRA_KIND` pour savoir laquelle des deux notifications afficher.
`NextPrayerResolver` reste inchangé : l'affichage ne connaît que les prières.
**Conséquences :**
- un rappel dont l'adhan approche déjà (téléphone rallumé à 12h55 pour un Dhuhr à
  13h00) est simplement sauté — il n'est plus « à venir » ;
- un `EXTRA_KIND` absent est traité comme `ADHAN` : une alarme posée par une version
  antérieure et qui survit à la mise à jour reste correcte.

## D18 — Délai du rappel : liste fermée, **jamais moins de 10 minutes**

En Doze, Android n'accorde à une application qu'une alarme
`setExactAndAllowWhileIdle` toutes les ~9 minutes. Un rappel à 5 minutes ferait
donc reporter l'adhan qui le suit — précisément ce que l'app s'interdit. Les choix
sont donc 10, 15, 20, 30, 45 et 60 minutes (`ReminderSettings.LEAD_CHOICES`), et
`sanitizeLead` ramène toute valeur lue du stockage au choix le plus proche.
**Conséquence :** proposer un rappel plus court imposerait de passer à
`setAlarmClock` (exempt de quota), au prix de l'icône de réveil permanente dans la
barre de statut — refusé pour cinq prières par jour.

## D19 — Un canal de notification par nature d'alerte

`NotificationChannels` (qui remplace `PrayerNotificationChannel`) déclare
`prayer_times_v2` et `prayer_reminder_v1`. Deux canaux parce qu'ils n'ont ni le même
son ni la même urgence, et surtout parce qu'Android laisse alors l'utilisateur
régler ou couper le rappel **sans toucher** à l'appel à la prière.
**Conséquence :** la règle du bump d'ID au changement de son vaut pour chacun,
indépendamment.

## D20 — Le son est joué par l'app, plus par le canal de notification

Constaté sur appareil (Android 10) : le rappel restait muet, et surtout **la
musique en cours continuait par-dessus l'adhan**. Ce second point n'est pas un
bug mais une propriété du système — le lecteur de notifications ne demande
jamais le **focus audio**, il ne peut donc pas mettre un lecteur en pause. Quant
au premier, plusieurs surcouches ignorent le son personnalisé d'un canal.

Choix : `PrayerSoundService` (service en **avant-plan**) demande
`AUDIOFOCUS_GAIN_TRANSIENT` — les autres lecteurs reçoivent
`AUDIOFOCUS_LOSS_TRANSIENT`, se mettent en pause, et **reprennent seuls** après —
puis joue la ressource avec un `MediaPlayer`.

Pourquoi un service et non un `MediaPlayer` dans le receiver : le processus d'un
`BroadcastReceiver` peut être tué dès `onReceive` terminé, ce qui couperait un
adhan de 31 s au bout de quelques secondes. Le type `shortService` (Android 14+)
décrit exactement ce besoin et ne demande aucune permission sensible ; démarrer
le service depuis un receiver d'**alarme exacte** fait partie des cas exemptés de
l'interdiction de lancer un service d'avant-plan depuis l'arrière-plan.

**Conséquences :**
- les canaux passent en **muet** (`setSound(null, null)`), sinon tout serait
  entendu deux fois → IDs bumpés en `prayer_times_v3` et `prayer_reminder_v2` ;
- la **vibration reste au canal**, donc Android continue de suivre le mode du
  téléphone tout seul ; le service applique la même règle au son en lisant
  `AudioManager.ringerMode` (silencieux ou vibreur → pas de son) ;
- l'utilisateur ne peut plus remplacer le son depuis les réglages système du
  canal — ce sera un réglage de l'app le jour où les adhans personnalisés arriveront ;
- la notification est posée **par le receiver**, puis reprise par le service comme
  notification d'avant-plan et enfin détachée (`STOP_FOREGROUND_DETACH`) : si le
  système refuse le service, l'utilisateur est prévenu quand même — seul le son manque.

## D21 — Le calendrier n'a pas fait tomber D7 : son état lui appartient

D7 annonçait le calendrier mensuel comme le déclencheur d'une vraie navigation.
Il ne l'a pas été : l'écran s'ouvre **toujours sur aujourd'hui** et gère lui-même
le mois affiché et le jour sélectionné, dans son ViewModel. Aucun argument ne
traverse donc la navigation, et `enum Screen` suffit toujours.
**Conséquences :**
- `CalendarViewModel` n'a **pas de ticker** (contrairement à l'accueil) : rien ne
  défile, l'état se reconstruit seulement au changement de mois ou de jour ;
- les horaires ne sont calculés que pour le **jour ouvert**, jamais pour les 42
  cases de la grille — seule la conversion hégirienne, très peu coûteuse, y tourne ;
- la grille est un `Column` de `Row` et non un `LazyVerticalGrid` : six lignes au
  plus, dans une colonne déjà défilante, où un conteneur défilant imbriqué serait
  inutile et source d'ennuis de mesure.

## D22 — Imsāk = Fajr − 10 min, et la durée du jeûne court de l'imsāk à l'iftār

Le jeûne commence en droit à l'aube véritable (le Fajr) ; l'imsāk est l'usage
répandu de s'arrêter un peu avant, par précaution. Dix minutes est la valeur
d'Umm al-Qura et de la plupart des calendriers du Maghreb
(`IMSAK_MINUTES_BEFORE_FAJR`). L'iftār, lui, est exactement le Maghrib : aucun
calcul astronomique supplémentaire n'est nécessaire, tout se déduit des horaires
du jour déjà calculés.

La **durée affichée** court de l'imsāk à l'iftār, et non du Fajr au Maghrib : ce
sont les deux heures que l'écran montre, et un utilisateur qui les soustrait doit
retrouver le nombre affiché. La différence est de dix minutes, et l'écart
canonique reste lisible puisque le Fajr figure juste au-dessus dans la liste.
**Conséquence :** rendre le délai d'imsāk réglable un jour ne touchera qu'une
constante ; l'encart du Ramadan ne s'affiche que si le jour tombe au mois 9.

## D23 — Le Maghrib algérien porte une marge de 3 minutes

Signalé depuis un appareil : nos horaires de Skikda tombaient 3 minutes avant
ceux du calendrier officiel sur le seul Maghrib. Vérifié en calculant les heures
**à la seconde** et en les comparant à deux dates éloignées de quatre mois :

| Date | Coucher brut | Nous (avant) | Officiel | Écart |
|---|---|---|---|---|
| 6 août 2026 | 19:34:10 | 19:34 | 19:37 | +3 min |
| 15 déc. 2026 | 17:17:02 | 17:17 | 17:20 | +3 min |

Les cinq autres moments coïncidaient exactement aux deux dates. La marge est donc
**constante**, et non un artefact d'arrondi ou de saison : c'est l'*iḥtiyāṭ* que le
ministère ajoute au coucher calculé. Elle ne figure pas dans la spécification
AlAdhan de la méthode algérienne, d'où son absence jusqu'ici.
`MethodOption.ALGERIA` porte désormais `maghribMinutes = 3`.

**Conséquences :**
- l'Isha algérienne est calculée par **angle** (17°) et non par intervalle depuis
  le coucher : elle n'hérite pas de l'ajustement, contrairement au cas portugais
  (D8) — un test le verrouille explicitement ;
- le correctif déplace l'heure de l'adhan du Maghrib **et** l'heure d'iftar de
  l'écran calendrier (D22) : c'est précisément l'enjeu, rompre le jeûne trois
  minutes trop tôt est une vraie faute ;
- la marge est posée **pour l'Algérie seulement**. La Tunisie et le Maroc ont
  probablement la leur, mais elle n'a pas été relevée : on ne recopie pas une
  valeur d'un pays à l'autre sans mesure.

**Suite :** les pays dont la marge n'a pas été mesurée sont couverts par le réglage
manuel de D24.

**Ce qui n'était pas un écart :** l'Asr semblait décalé d'une minute le 6 août
(16:25 contre 16:26). Le calcul brut donne 16:25:28, soit 32 secondes de la
bascule — un écart de quelques secondes entre deux implémentations suffit à faire
basculer l'arrondi. Le 15 décembre, où l'Asr brut tombe à :06, les deux apps
s'accordent. Aucune correction : les deux calculs sont d'accord à la seconde près.

## D24 — Ajustement manuel par prière, en plus de la marge de la méthode

D23 a corrigé l'Algérie parce qu'on avait de quoi mesurer. Les autres pays, et
surtout les calendriers de mosquée, ne se mesureront pas un par un : l'utilisateur
doit pouvoir recaler lui-même. `PrayerTimeAdjustments` porte donc une minute de
décalage par moment, bornée à ±30 min.

Adhan distingue deux jeux d'ajustements et les **additionne** :
`methodAdjustments` (la marge officielle, ex. les 3 min algériennes) et
`prayerAdjustments` (le réglage de l'utilisateur). On se branche sur le second,
donc un ajustement manuel **se superpose** à la marge de la méthode sans l'effacer
— et changer de méthode ne fait pas perdre le réglage manuel.

**Conséquences :**
- **les entrées nulles ne sont jamais stockées** (ni en mémoire, ni dans DataStore,
  où la clé est supprimée) : deux réglages équivalents sont donc `==`, ce qui est
  indispensable au cache de `HomeViewModel`, dont la clé est le `CalculationSettings`
  entier. Un `{FAJR: 0}` stocké aurait suffi à le faire recalculer sans fin ;
- l'ajustement traverse les **quatre** points de calcul — accueil, calendrier,
  planificateur d'alarme, widget. C'est la partie qui compte : si l'alarme ne
  suivait pas l'écran, l'adhan sonnerait à une heure que l'app n'affiche nulle part ;
- une clé DataStore par moment (`adjust_fajr`, `adjust_sunrise`…) plutôt qu'une
  chaîne sérialisée : lisible, et un moment nouveau ne casserait rien ;
- le réglage vit dans un dialogue à six lignes plutôt qu'en six lignes de plus sur
  l'écran, qui en compte déjà sept. Chaque pas s'applique immédiatement et
  replanifie l'alarme, d'où un bouton « fermer » et non « valider ».

## D25 — Les invocations rejoignent la **même** chaîne d'alarmes, avec une garde

Deux chaînes parallèles — une pour les prières, une pour les adhkār — auraient
doublé les points de rupture, ce que D3 interdit et que D17 avait déjà tranché
pour le rappel avant l'adhan. On généralise donc une seconde fois :
`AlarmEventResolver` produit un `ScheduledEvent` (prière **ou** invocation) et la
chaîne continue de poser **une seule alarme à la fois**.

Mais le quota Doze de D18 — une alarme `setExactAndAllowWhileIdle` par ~9 minutes
— s'applique à **toute** l'application, pas à une catégorie d'alarme. Une
invocation posée cinq minutes avant le Fajr ferait donc reporter l'adhan, ce que
l'app s'interdit depuis le premier jour. D'où la garde, appliquée dans le domaine
plutôt que dans l'UI (elle est ainsi testable, et l'utilisateur garde le droit de
choisir l'heure qu'il veut) :

> Les évènements de prière ne bougent **jamais**. Une invocation qui tombe à
> moins de `GUARD_MINUTES` (10) d'un évènement déjà placé est repoussée à
> `celui-ci + garde`, et le contrôle reprend avec le bloqueur suivant.

Un seul passage suffit : les bloqueurs sont parcourus dans l'ordre croissant et
une invocation ne se déplace que vers l'avant, donc un bloqueur déjà dépassé ne
peut pas redevenir gênant.

**Conséquences :**
- une invocation peut sonner plus tard que demandé (au pire de dix minutes en
  dix minutes) ; un adhan, jamais — c'est le sens de l'échange, et l'écran le
  dit en une ligne (`invocation_guard_hint`) ;
- les invocations sont **désactivées à la source** : une invocation coupée ne
  produit aucun évènement, elle ne consomme donc rien ;
- `PrayerEventResolver` n'est pas remplacé : sa production d'évènements d'un jour
  est simplement devenue publique, et `resolveNext` reste en place ;
- `PrayerAlarmReceiver` **n'a pas été renommé** malgré son périmètre élargi :
  l'alarme déjà posée par la version installée pointe sur ce nom de classe et ne
  survivrait pas à un changement. Le nom est devenu un peu étroit ; la chaîne
  intacte vaut mieux qu'un nom juste.

## D26 — Le contenu des invocations livrées vit dans les ressources, pas en base

Un dhikr authentique ne s'édite pas, et son texte arabe ne se traduit pas plus
que l'appel à la prière (règle i18n n°5). Les deux invocations livrées
(`BuiltinInvocation.MORNING` et `EVENING`) ne stockent donc en base que leur
**état** — activée ou non, à quel moment — et une clé `builtinKey` ; leur titre
et leur texte viennent de `strings.xml`, le titre traduit dans les trois langues,
le texte déclaré une seule fois en `translatable="false"`.

Une invocation écrite par l'utilisateur, elle, stocke son titre et son texte
littéraux : c'est son contenu à lui, il n'a rien à faire dans les ressources.

**Conséquences :**
- les invocations livrées sont **désactivables mais pas supprimables** — la
  clause `AND builtinKey IS NULL` du `DELETE` le garantit côté base, pas
  seulement côté UI ;
- leur semis est idempotent grâce à des **ids fixes** (1 et 2) et un
  `OnConflictStrategy.IGNORE` : une désactivation survit à chaque démarrage ;
- ajouter une invocation livrée = une entrée d'enum + deux clés de ressources,
  sans migration de base.

## D27 — Le canal des invocations garde le son du système (exception à D20)

D20 a rendu les canaux de prière muets pour jouer l'adhan nous-mêmes : c'était le
seul moyen d'obtenir le **focus audio**, donc de mettre en pause la musique en
cours, et de contourner les surcouches qui ignorent le son personnalisé d'un
canal. Rien de tout cela ne vaut pour un dhikr : ni les trente secondes d'un
adhan, ni le droit d'interrompre ce que l'utilisateur écoute.

Le canal `invocations_v1` garde donc le **son de notification par défaut** du
téléphone et `IMPORTANCE_DEFAULT`, et `PrayerSoundService` n'est pas sollicité.

**Conséquences :**
- l'utilisateur peut changer ou couper ce son depuis les réglages Android, ce
  qui n'est justement plus possible pour l'adhan depuis D20 ;
- la règle du bump d'ID au changement de son vaut pour lui comme pour les
  autres : toucher à son son ou à son importance imposera `invocations_v2` ;
- `buildChannel` prend désormais l'importance et le son en paramètres, avec les
  valeurs de D20 par défaut — le cas muet reste le cas normal.

## D28 — Écran de démarrage : le thème pour l'écran système, Compose pour le reste

Depuis Android 12, le système affiche **toujours** un écran de démarrage au
lancement : on ne peut pas le supprimer, seulement l'habiller. Mais il ne sait
montrer qu'une icône — ni le nom de l'app, ni sa baseline.

L'écran de démarrage est donc en deux moitiés qui se recouvrent :

1. **L'écran système**, habillé par trois attributs de thème dans
   `values-v31/themes.xml` (fond vert de la marque, avant-plan de l'icône
   adaptative en guise de logo — l'icône complète serait verte sur vert).
2. **`ui/splash/SplashScreen`**, un composable superposé à l'accueil pendant
   1,4 s puis estompé, qui ajoute le nom et la baseline. C'est aussi le seul
   écran de démarrage en dessous d'Android 12, où le système n'en fait aucun.

Le raccord tient à une seule chose : `android:windowBackground` vaut lui aussi
`@color/splash_background`, dans les deux modes. Sans cela un éclair clair (ou
sombre) s'intercale entre l'écran système et le nôtre. Ce fond n'est plus jamais
visible ensuite, Compose peignant par-dessus.

**Pas de dépendance `androidx.core:core-splashscreen`** : elle sert à
rétroporter l'API 31 sur les versions antérieures, or ici la moitié
rétroportable est déjà écrite en Compose, et il faut de toute façon un
composable pour le texte. Trois attributs XML coûtent moins qu'une librairie.

**Conséquences :**
- `Theme.Miqaat` est scindé en `Base.Theme.Miqaat` (values/ et values-night/,
  qui portent le parent clair ou sombre) et `Theme.Miqaat` (values/ pour la
  version simple, values-v31/ pour celle qui ajoute les attributs de démarrage) —
  un style n'étant jamais fusionné entre qualificateurs, mais toujours remplacé ;
- l'écran de démarrage est vert dans les deux modes, comme l'icône du lanceur :
  c'est de l'identité, pas de l'interface (même raison que `dynamicColor = false`).
  `splash_background` n'a donc pas de variante `values-night` ;
- pendant qu'il est affiché, les icônes des barres système sont forcées en clair,
  puis rendues au thème — `enableEdgeToEdge()` les choisit sur le mode nuit, ce
  qui les rendrait illisibles sur le vert en mode clair ;
- le splash est **superposé** à l'accueil, pas joué à sa place : l'accueil se
  compose et charge ses horaires pendant ce temps, et n'apparaît pas vide ;
- `rememberSaveable` : un changement de langue recrée l'activité, il ne doit pas
  rejouer le démarrage.

> **D20 — confirmé sur appareil (session 10).** Redmi Note 8, Android 10, avec un
> lecteur de Coran en cours de lecture. `requestAudioFocus … req=2` →
> `onAudioFocusChange(-2)` reçu par le lecteur 3 ms plus tard (il se met en pause)
> → `AudioTrack stop: 496000 frames delivered`, soit 496000 ÷ 16 000 Hz = **31,0 s**,
> l'adhan entier → `onAudioFocusChange(1)`, le lecteur **reprend seul**.
> C'est exactement ce qu'un son laissé au canal de notification ne sait pas faire.

## D29 — La clé de signature vit hors du dépôt, et la release se compile sans elle

`app/build.gradle.kts` lit `keystore.properties` **s'il existe** et n'ouvre un
`signingConfigs` que dans ce cas. Conséquences voulues :

- aucun secret ne traverse le dépôt (`*.jks` et `keystore.properties` sont
  gitignorés, un `keystore.properties.example` documente les clés attendues) ;
- `assembleRelease` réussit quand même sans la clé, en produisant un APK non
  signé : quiconque clone le projet peut le compiler, ce qui compte pour un
  logiciel sous GPL. Un `signingConfig` déclaré en dur ferait échouer leur build ;
- signature en **v2 + v3**. v1 (JAR) ne sert qu'en dessous d'Android 7, or
  `minSdk` vaut 26 ; v3 ouvre la rotation de clé, impossible à ajouter après coup.

L'identité du certificat (`CN`, `O`, `C`) a été fixée **avant** la première
publication : après, la changer romprait la chaîne de mises à jour, Android
n'acceptant une mise à jour que signée par la même clé.

La procédure complète vit dans [release.md](release.md).

## D30 — Les insets système s'appliquent au conteneur, avant `verticalScroll`

Une marge posée **après** `verticalScroll` dans la chaîne de modificateurs fait
partie du contenu qui défile : elle remonte avec lui, et l'en-tête finit sous la
barre de statut au premier glissement. Le défaut est invisible au repos, donc
intermittent à l'usage — cinq écrans en souffraient.

Symétriquement, `Modifier.height(24.dp).navigationBarsPadding()` sur un `Spacer`
ne fait **rien** : `height()` étant à l'extérieur fixe la hauteur totale à 24dp,
et la marge intérieure n'a aucun contenu à décaler.

Règle : `.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(…)`.
L'accueil est la seule exception — il omet `statusBarsPadding`, le dégradé du
héros se prolongeant volontairement derrière la barre de statut, et `HeroSection`
posant sa propre marge.

Corollaire vérifié au passage : une dimension en dur ne suit pas l'échelle de
police. Un `width(72.dp)` sur la valeur du stepper hégirien coupait
« Aucun ajustement » au milieu d'un mot dès l'échelle par défaut — d'où une valeur
courte (`0`, `+1`) dans cet emplacement.

## D31 — Une alerte en retard ne s'affiche jamais

L'alarme du rappel du Fajr, posée à 4h, a été délivrée à 14h sur un Redmi Note 8 :
la surcouche avait gelé l'application, et la diffusion n'est sortie qu'au moment
où l'utilisateur l'a rouverte. Recevoir « le Fajr approche » à 14h est pire qu'une
notification manquée — c'est une notification **fausse**, et elle décrédibilise
toutes les autres.

Le scheduler transmet donc `EXTRA_TRIGGER_AT` (l'instant visé), et le receiver le
compare à l'heure réelle avant d'afficher quoi que ce soit. Les tolérances
(`domain/AlarmFreshness.kt`) valent 20 min pour l'adhan, **5 min** pour le rappel,
30 min pour une invocation.

Les cinq minutes du rappel ne sont pas arbitraires : elles doivent rester
**strictement inférieures** au délai de rappel le plus court (`LEAD_CHOICES`
commence à 10 min), sans quoi un rappel périmé pourrait s'afficher *après* l'adhan
qu'il annonçait. Un test verrouille cette inégalité.

**Conséquences :**
- l'extra absent (alarme posée par une version antérieure) vaut « frais » : on
  affiche, comme avant. Aucune alarme en vol n'est perdue à la mise à jour ;
- un déclenchement **en avance** (horloge reculée) est frais aussi — l'alerte
  n'est pas périmée, elle est prématurée, et la chaîne la reposera ;
- le filtrage ne porte que sur l'**affichage** : `scheduleNext()` est désormais
  dans un `finally`, donc la chaîne se replanifie même sur un évènement ignoré,
  même sans permission de notification, même après une exception.

## D32 — `RescheduleReceiver` exporté, et quatre actions de plus

`exported="false"` **fonctionnait** : la session 2 avait vérifié la
replanification après reboot avec cette valeur. Le passage à `true` ne corrige
donc rien — il est gratuit (les actions écoutées sont des *protected broadcasts*,
qu'aucune application tierce ne peut forger, et le `when` ignore tout le reste) et
il rend le receiver joignable depuis `adb` **sans root**, ce que la note de test
de la session 2 signalait justement comme impossible.

Ce qui corrigeait vraiment quelque chose, c'est l'ajout de
`ACTION_MY_PACKAGE_REPLACED` : Android annule les alarmes d'un paquet remplacé,
donc **chaque mise à jour de l'application tuait la chaîne** jusqu'à la prochaine
ouverture — silencieusement, depuis toujours.

`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` est ajoutée sans en attendre
grand-chose : elle ne concerne qu'Android 12/12L, `USE_EXACT_ALARM` étant accordée
d'office et non révocable au-delà.

⚠ Ne pas confondre avec `ACTION_PACKAGE_REPLACED`, qui exigerait un
`<data android:scheme="package"/>` et se déclencherait pour **toutes** les
applications du téléphone.

## D33 — Un chien de garde, pas WorkManager

La table de la stack annonçait « AlarmManager + WorkManager pour replanifier »
depuis la session 1. WorkManager n'a jamais été implémenté, et ne le sera pas :
c'est une dépendance de plus (avec son `ContentProvider` d'initialisation et sa
base), pour du travail **déférable** — donc structurellement incapable de tenir la
seule promesse de l'application — et gelé par les surcouches exactement comme le
reste. La table est corrigée plutôt qu'honorée.

À la place, une **seconde alarme**, inexacte, semi-quotidienne
(`setInexactRepeating`, `requestCode 1003`), qui ne notifie rien : elle rappelle
`scheduleNext()`. Inexacte volontairement — lui donner l'exactitude consommerait
le quota Doze de la vraie alarme, ce que D18 et D25 s'interdisent.

Elle répare la rupture de chaîne (une diffusion perdue, une exception imprévue),
faiblesse structurelle de D3 « une seule alarme à la fois ». Elle ne répare
**pas** le gel de l'application par une surcouche : rien de ce qu'on programme
n'est alors délivré. C'est l'objet de D34.

Alternative écartée : poser les trois prochains évènements sur trois `requestCode`
distincts. Plus robuste, mais cela multiplierait par trois les alarmes exactes et
casserait le raisonnement de quota Doze de D18 et D25.

⚠ Ne **pas** brancher `NextPrayerWidget.onUpdate` sur `scheduleNext()` comme filet
supplémentaire : `scheduleNext()` se termine par `NextPrayerWidget.refresh()`,
d'où une boucle infinie.

## D34 — L'écran de fiabilité, et la règle qui l'empêche de harceler

Aucune API ne dit à une application qu'elle a été gelée, ni ne permet de demander
le démarrage automatique. Le seul remède est de faire régler l'appareil par
l'utilisateur — donc de le lui expliquer et de l'y emmener. Cinq contrôles :
notifications autorisées, alarmes exactes, exclusion de l'optimisation de
batterie, démarrage automatique de la surcouche, et **délivrance réelle**.

Ce dernier est le seul détecteur automatique du gel : `ReliabilityLog` horodate
chaque déclenchement du receiver, et une application installée depuis plus de 24 h
qui n'a jamais rien délivré est forcément empêchée de s'exécuter. Sans cette
trace, « la surcouche nous gèle » et « la chaîne s'est rompue » ont exactement le
même symptôme et deux remèdes différents.

**La règle anti-harcèlement**, verrouillée par un test : `CheckState.UNKNOWN` ne
déclenche **jamais** la bannière d'accueil. L'état du démarrage automatique n'est
pas lisible ; s'en servir afficherait à tout possesseur de Xiaomi un avertissement
permanent qu'aucune action ne pourrait éteindre. On n'alarme que sur du **certain
et du critique**, jamais par une modale au lancement, et « plus tard » fait taire
la bannière quatorze jours. L'écran reste accessible depuis les réglages.

**Conséquences :**
- un contrôle `NOT_APPLICABLE` (alarmes exactes hors Android 12/12L, fabricant sans
  écran connu) n'a pas de ligne du tout : mieux vaut une liste courte et vraie ;
- l'écran affiche l'heure de la **prochaine alerte programmée** (via
  `PrayerAlarmScheduler.nextEvent()`, extrait pour l'occasion) et celle de la
  dernière reçue : deux faits valent mieux qu'un message rassurant ;
- un bouton « notification de test » déclenche une vraie alerte, seul moyen
  d'éprouver le mode d'alerte sans attendre une prière ;
- la table des composants OEM (`data/reliability/OemAutostart.kt`) exige **trois**
  précautions : le bloc `<queries>` du manifeste (sans lui, `resolveActivity`
  renvoie `null` sur Android 11+, filtrage de visibilité des paquets),
  `resolveActivity` pour choisir parmi des noms qui changent d'une version de
  surcouche à l'autre, et un `try/catch` qui bascule sur les instructions écrites.

## D35 — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` est assumée

Cette permission fait rejeter une application sur Google Play sauf cas listés.
Miqaat est distribuée hors Play (GPL, releases GitHub) : la politique ne s'y
applique pas, et l'exclusion de l'optimisation de batterie est la deuxième cause
de notifications manquées après le démarrage automatique.

Si une publication Play était envisagée un jour, il faudrait basculer sur
`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (la liste générale, sans demande
ciblée) — déjà en place comme repli.

## D36 — La décision d'alerte est abstraite, et vit dans le domaine

Le réglage « mode d'alerte » croise deux données : ce que l'utilisateur a choisi
et l'état du téléphone. La table de vérité qui en résulte est la partie qui a le
plus de chances d'être fausse — donc celle qu'il faut tester. `AlertResolver`
(`domain/NotificationAlert.kt`) ne connaît qu'un `NotificationMode` et un
`RingerState`, et rend un `AlertDecision` : un flux audio éventuel et un style de
vibration. La lecture d'`AudioManager` et de `NotificationManager` reste côté
Android (`RingerReader`), la matrice est en JVM pur, et ses douze cases ont chacune
leur assertion.

| mode ＼ téléphone | sonnerie | vibreur | silencieux |
|---|---|---|---|
| **Suivre le téléphone** | sonnerie + impulsion | — + série | — + rien |
| **Toujours sonner** | sonnerie + impulsion | alarme + impulsion | alarme + impulsion |
| **Toujours vibrer** | — + série | — + série | — + série |
| **Toujours silencieux** | — + rien | — + rien | — + rien |

`AlertDecision.stream` est **nullable** plutôt que doublé d'un booléen
`playSound` : l'état incohérent « je ne joue pas, mais voici mon flux » devient
impossible à construire.

« Toujours sonner » n'emprunte le flux « alarme » que lorsqu'il le faut : sur un
téléphone qui sonne, la sonnerie suffit et respecte le volume que l'utilisateur a
réglé pour elle.

Le réglage vit dans un **troisième** instantané de `SettingsRepository`
(`NotificationSettings`) et non dans `ReminderSettings` : celui-ci est un
paramètre d'entrée de `PrayerEventResolver` et `AlarmEventResolver`, et y glisser
une donnée de rendu obligerait toute la planification — et sa vingtaine de tests —
à la transporter sans jamais s'en servir.

⚠ `SettingsRepository.cache()` doit rafraîchir les **trois** instantanés. En
oublier un le rendrait périmé pour les lecteurs synchrones, c'est-à-dire
précisément pour le receiver d'alarme, qui tourne souvent dans un processus
fraîchement démarré où aucun `Flow` n'a jamais émis. Bug invisible en test manuel,
visible seulement application fermée.

## D37 — Deux flux audio : la sonnerie d'appel par défaut, l'alarme pour forcer

Jusqu'ici le son sortait en `USAGE_NOTIFICATION`, donc au volume des
notifications. L'utilisateur attend le volume de la **sonnerie d'appel** :
`USAGE_NOTIFICATION_RINGTONE` (flux `STREAM_RING`).

Pour « toujours sonner », il faut un flux que le mode sonnerie ne coupe pas :
`USAGE_ALARM` (`STREAM_ALARM`). C'est le seul moyen d'être entendu sur un téléphone
en vibreur ou en silencieux — et cela joue au volume des alarmes, souvent proche
du maximum. C'est le sens du réglage, et le sous-titre le dit.

**Conséquences « Ne pas déranger », assumées :**
- filtre « alarmes seulement » : « toujours sonner » passe, « suivre le
  téléphone » non. Cohérent ;
- filtre « silence total » : **rien ne passe, même le flux alarme**. Contourner
  demanderait `setBypassDnd(true)` sur le canal, lui-même ignoré tant que
  l'utilisateur n'a pas accordé `ACCESS_NOTIFICATION_POLICY` — permission lourde
  et mal comprise, qu'on ne demandera pas. Le système gagne ;
- `RingerReader` traite donc les deux filtres comme « silencieux », et
  `getCurrentInterruptionFilter()` n'exige aucune permission, contrairement à
  `getNotificationPolicy()`.

Le type de gain de focus est inchangé : `AUDIOFOCUS_GAIN_TRANSIENT` pour l'adhan
et le rappel — c'est lui qui met la musique en pause, D20 reste intact, mesure des
31,0 s comprise. Une invocation, qui ne dure que quelques secondes, se contente de
`..._MAY_DUCK` : l'interrompre serait disproportionné.

## D38 — L'application reprend la vibration ; les trois canaux deviennent muets

D20 avait retiré le **son** des canaux tout en laissant la **vibration** au canal,
« pour qu'Android suive tout seul le mode du téléphone ». C'est exactement ce qui
rendait le nouveau réglage impossible : tant que le canal décide, personne ne peut
forcer quoi que ce soit.

Les trois canaux passent donc à `setSound(null, null)` **et**
`enableVibration(false)`, ce qui impose de nouveaux identifiants —
`prayer_times_v4`, `prayer_reminder_v3`, `invocations_v2` — Android figeant les
réglages d'un canal à sa création. Les anciens rejoignent `OLD_IDS` et sont
supprimés au lancement, sans quoi ils resteraient en canaux fantômes dans les
réglages système. ⚠ Le bump efface au passage les personnalisations que
l'utilisateur aurait faites sur ces canaux : c'est inévitable, à mentionner dans
la note de version.

**La vibration part du receiver, jamais du service sonore.** Un `VibrationEffect`
fini est confié au service système : il se poursuit même quand notre processus
meurt. Le déclencher depuis un service qu'on arrête dès la fin du son le couperait
au milieu.

Les motifs sont **finis et courts** (≈ 0,4 s, ≈ 3 s pour l'adhan en série), jamais
indexés sur la durée du son : trente et une secondes de vibration continue
seraient agressives et videraient la batterie pour rien. Et l'attribut
`USAGE_ALARM` est indispensable — sans lui, le système traite la vibration comme
celle d'une notification et la supprime dès que l'utilisateur a coupé les
vibrations de notification, ce qui viderait « toujours vibrer » de son sens.

Effet de bord bienvenu : en mode vibreur ou silencieux, **plus aucun service
d'avant-plan n'est démarré**. Auparavant on en lançait un pour découvrir, une fois
dedans, qu'il n'y avait rien à jouer. `PrayerSoundService` est renommé
`AlertSoundService` (il sert les trois natures d'alerte) — renommage sans risque,
contrairement au receiver : aucune `PendingIntent` ne pointe sur un service.

Ajout au passage de `onTimeout()` : un `shortService` non arrêté sous ~3 minutes
lève une `ForegroundServiceDidNotStopInTimeException` fatale sur Android 14+.
L'adhan dure 31 s, la marge est large, mais un `MediaPlayer` bloqué ne doit pas
faire tomber l'application.

## D39 — Les adhkār suivent le mode d'alerte (D27 est renversée)

D27 laissait au canal des invocations le son de notification du système, pour que
l'utilisateur puisse le régler depuis Android. Le réglage « mode d'alerte » rend
cette exception incohérente : qui demande « toujours silencieux » n'attend pas
qu'un dhikr sonne quand même.

Les invocations passent donc par le même chemin que les prières — canal muet
(`invocations_v2`), décision d'alerte, vibration depuis le receiver, son joué par
`AlertSoundService`. Faute d'enregistrement livré, ce son reste
`Settings.System.DEFAULT_NOTIFICATION_URI` : c'est bien le son de notification du
téléphone qu'on entend, simplement joué par nous.

Ce qui est perdu : le choix d'un son *différent* pour les adhkār depuis les
réglages Android. Ce qui est gagné : un réglage unique qui vaut pour tout ce que
l'application émet — c'est ce que l'utilisateur a demandé, et c'est plus facile à
expliquer qu'une exception. Ce qui reste de D27 : l'importance `DEFAULT` du canal,
un dhikr n'ayant ni la durée ni l'urgence d'un adhan.

## D40 — L'arrondi à la minute appartient à l'app, et la marge officielle se mesure en secondes

Adhan calcule à la seconde puis arrondit **à la minute la plus proche**. Un calendrier
de ministère ne fait jamais ça : il ajoute une marge de précaution puis tronque, pour
que l'heure annoncée ne tombe jamais *avant* l'heure calculée. L'app pouvait donc
annoncer jusqu'à trente secondes trop tôt, et l'écart se voyait ou non selon les
secondes du jour — d'où un défaut qui paraissait aléatoire.

`PrayerTimesCalculator` demande désormais `Rounding.NONE` à la librairie et tranche
lui-même, via la `calibration` de la méthode (`domain/model/TimeCalibration.kt`) :
un décalage **par moment, en secondes**, puis un arrondi choisi. L'ordre compte —
l'arrondi doit voir la marge, l'inverse déplacerait le résultat d'une minute.

Trois raisons de ne pas laisser faire Adhan : sa politique d'arrondi est unique pour
les six moments, alors que le shurūq (fin du Fajr) doit être tronqué quand les prières
sont retardées ; ses `PrayerAdjustments` ne s'expriment qu'en minutes entières ; et
travailler sur les secondes brutes rend l'écart **mesurable**, donc vérifiable par un test.

Des secondes et non des minutes, parce que la mesure le commande : sur les trente jours
du calendrier officiel de Skikda, un décalage en minutes entières ne reproduit que 25 à
29 lignes sur 30 selon le moment, là où le décalage en secondes les reproduit toutes. La
méthode de mesure et les valeurs relevées sont dans
[prayer-times-accuracy.md](prayer-times-accuracy.md).

**Rien ne change pour les autres méthodes** : la calibration par défaut reproduit
exactement le comportement d'Adhan. Une méthode ne s'en écarte qu'après mesure sur un
mois entier.

### D23 est amendée

D23 avait relevé les 3 minutes du Maghrib algérien sur **deux dates**, en comparant
« contre une autre app ». Les trois minutes sont confirmées — on les retrouve dans les
261 secondes du Maghrib, contre ~85 pour les autres moments. Mais deux points de mesure
ne pouvaient pas séparer la marge de l'arrondi, et le même diagnostic avait écarté à
tort un écart sur l'ʿAṣr comme « faux positif » : il était réel. Une marge officielle se
mesure sur un mois, pas sur deux dates.
