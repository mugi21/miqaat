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
