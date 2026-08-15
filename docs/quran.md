# Écoute du Coran

Ajouté en session 14. Récitateurs, favoris, sourates, lecture en streaming — et
les deux choses que seule une app de prière peut offrir : **le lecteur cède la
place à l'adhan** (D43) et **la sourate du moment** est déduite des horaires
réels du jour.

Voir aussi [decisions.md](decisions.md) D41 (`INTERNET`), D42 (Media3), D43.

---

## L'API mp3quran.net v3

Base : `https://mp3quran.net/api/v3` — publique, **sans clé ni inscription**.

| Endpoint | Ce qu'il rend |
|---|---|
| `GET /reciters?language=<code>` | ~130 récitateurs, chacun avec ses `moshaf` |
| `GET /suwar?language=<code>` | les 114 sourates, nom traduit |
| `GET /languages` | la table des codes de langue |

L'app n'utilise que les deux premiers. Existent aussi, **délibérément non
utilisés** : `/riwayat`, `/moshaf`, `/radios`, `/tafasir`, `/video_types`, une API
« verset par verset » et une « Timing API » sur un autre hôte
(`api.mp3quran.net`, documentée sur `/ar/api/2`). La Timing API permettrait de
surligner le verset en cours — c'est la piste la plus intéressante si l'on veut
aller plus loin.

### Forme des réponses

```json
{"reciters":[{"id":1,"name":"إبراهيم الأخضر","letter":"إ",
  "moshaf":[{"id":1,"name":"حفص عن عاصم - مرتل",
             "server":"https://server6.mp3quran.net/akdr/",
             "surah_total":114,"moshaf_type":11,
             "surah_list":"1,2,3,…,114"}]}]}
```

```json
{"suwar":[{"id":1,"name":"الفاتحة ","start_page":1,"end_page":1,"makkia":1,"type":0}]}
```

### L'URL d'un enregistrement

    moshaf.server + numéro de sourate sur trois chiffres + ".mp3"
    https://server6.mp3quran.net/akdr/ + 001.mp3

C'est `domain/QuranAudio.kt`, et rien d'autre dans l'app ne construit d'URL.

### ⚠ L'hôte canonique porte le `www.`

`https://mp3quran.net/api/v3/…` répond **301** vers `https://www.mp3quran.net/…`.
`BASE_URL` vise donc directement l'hôte avec `www.` : un aller-retour de moins à
chaque appel, et une occasion de moins de perdre le paramètre `language` en route.

### Trois pièges relevés à la mesure

**① Les codes de langue ne sont pas ceux d'Android.** Vérifié sur
`/api/v3/languages` : l'arabe est `ar`, le français est `fr`, mais **l'anglais est
`eng`**. Envoyer `en` ne provoque aucune erreur — l'API retombe silencieusement
sur l'arabe, et le catalogue serait en arabe dans une application en anglais.
D'où la table explicite dans `Mp3QuranLanguage` et son test.

**② `surah_list` n'est pas toujours propre.** La documentation officielle montre
elle-même une valeur à virgule traînante (`"…,39,40,"`). Le parseur ignore les
jetons vides et les numéros hors 1–114.

**③ Un moshaf n'a pas toujours les 114 sourates.** L'exemple officiel de Hazza
Al-Balushi en compte 83. Les sourates manquantes s'affichent **atténuées et non
cliquables** plutôt que masquées : la numérotation reste continue, et l'on
comprend que c'est le récitateur qui manque, pas l'application.

### Où lire la documentation

`https://www.mp3quran.net/ar/api` (et `/eng/api`, `/fr/api`). ⚠ La page
`/api/v3/docs` **n'existe pas** (404), et les pages de documentation répondent
**403 sans User-Agent de navigateur**. Les endpoints JSON, eux, répondent à
n'importe quel client.

---

## Le cache

Le catalogue est copié dans Room (base v4 : `quran_reciter`, `quran_moshaf`,
`quran_surah`, `quran_favorite`). Parcourir marche donc hors ligne dès le premier
chargement réussi ; **écouter** demande toujours le réseau.

`QuranCatalogRepository.refreshIfNeeded()` recharge dans trois cas seulement :

1. rien en cache ;
2. cache dans une **autre langue** que celle affichée (les noms viennent traduits
   de l'API) ;
3. cache plus vieux que **sept jours** — mp3quran ajoute des récitateurs
   régulièrement, mais pas au point de justifier un appel à chaque ouverture.

Le catalogue n'existe **que dans une langue à la fois** : les noms ne sont que des
translittérations, garder trois copies ne vaudrait pas la complexité. Un
changement de langue le refait entièrement.

⚠ **La langue se relit à chaque composition, elle ne se capture jamais.** Le
`QuranViewModel` ne reçoit pas de code de langue à la construction : c'est
`QuranScreen` qui appelle `setLanguage()` dans un `LaunchedEffect(languageTag)`.
Raison : changer la langue appelle `recreate()`, mais un `ViewModel` **survit** à
la recréation de l'activité — c'est tout son intérêt. Un code capturé au premier
affichage ne changeait donc jamais, et le catalogue restait dans la langue du
premier chargement jusqu'à la mort du processus. C'était un défaut réel, remonté
depuis un appareil.

Un échec réseau ne bloque que si l'on n'a rien à montrer. Avec un cache en place,
l'écran reste utilisable et l'échec est silencieux.

⚠ Les **favoris** ne sont pas du cache : c'est de la donnée de l'utilisateur.
C'est pour eux qu'une migration Room a été écrite plutôt que de laisser la base
repartir de zéro.

---

## La lecture

`quran/QuranPlaybackService` est un `MediaSessionService` Media3 (D42). Il porte
un `ExoPlayer` construit avec :

- `setAudioAttributes(USAGE_MEDIA / CONTENT_TYPE_SPEECH, handleAudioFocus = true)`
  — c'est ce paramètre qui livre gratuitement la moitié de D43 ;
- `setHandleAudioBecomingNoisy(true)` — casque débranché, lecture arrêtée.

La file d'attente est la sourate choisie **et toutes les suivantes que ce
récitateur possède** : c'est ce qui donne un sens à « suivant » et fait enchaîner
la lecture. Le `mediaId` de chaque élément porte le numéro de sourate, ce qui
permet au service d'enregistrer la position sans tenir d'état parallèle.

`quran/QuranPlayerConnection` est le seul point de contact de l'interface : un
`MediaController`, un `StateFlow<QuranPlaybackUiState>`, quelques commandes.
L'interface ne touche jamais au service ni au lecteur.

⚠ Le service est habillé par `AppLocale.wrap()` dans `attachBaseContext` — comme
toute surface hors activité, sans quoi sa notification suivrait la langue du
téléphone et non celle choisie dans l'app.

⚠ `onTaskRemoved` arrête le service si rien ne joue, sinon une notification
fantôme survivrait à la fermeture de l'application.

### La notification média

⚠ **Sans pochette explicite, ce n'est pas une pochette vide qui s'affiche mais
celle de mp3quran** : leurs fichiers MP3 portent une image ID3 embarquée, et
`ExoPlayerImpl.buildUpdatedMediaMetadata()` complète les métadonnées du flux avec
celles de l'élément de la file — l'élément gagne pour tout champ qu'il renseigne,
et perd pour tout champ qu'il laisse vide. Le titre et le récitateur étaient donc
corrects, la pochette non.

`quran/QuranArtwork` dessine le logo Miqaat sur le vert de la marque et rend un
PNG, posé par `setArtworkData` sur chaque élément. Dessiné à la volée depuis
`ic_launcher_foreground` plutôt que livré en bitmap : le logo a déjà une source
unique, réutilisée par l'icône et l'écran de démarrage.

La petite icône de la barre d'état passe par
`DefaultMediaNotificationProvider.setSmallIcon(ic_quran_notification)` — sinon
c'est la note de musique générique de Media3, qui ne dit pas d'où vient le son.

### Le mini-lecteur

`ui/quran/QuranPlayerBar` est posé en `bottomBar` du `Scaffold` de `MainActivity`,
et non dans un écran : c'est ce qui lui permet de survivre au retour à l'accueil,
donc d'écouter une sourate en consultant les horaires. Il ne compose rien quand
rien ne joue — même patron que `ReliabilityBanner`.

⚠ Conséquence sur les insets : la barre porte déjà `navigationBarsPadding`, donc
`MainActivity` applique `consumeWindowInsets(innerPadding)` en plus de
`padding(innerPadding)`. Sans cela, chaque écran rajouterait la marge par-dessus,
et un blanc de la hauteur de la barre de navigation apparaîtrait dès qu'une
sourate joue. Même famille que D30.

---

## La sourate du moment

`domain/QuranSuggestion.kt`, JVM pur. Cinq règles, dans cet ordre de priorité :

| # | Quand | Sourate |
|---|---|---|
| 1 | vendredi, **du Fajr au Maghrib** | al-Kahf (18) |
| 2 | après l'**Isha**, jusqu'au Fajr | al-Mulk (67) |
| 3 | entre le **Fajr et le shurūq** | Yā-Sīn (36) |
| 4 | entre le **Maghrib et l'Isha** | al-Wāqiʿa (56) |
| 5 | le reste de la journée | ar-Raḥmān (55) |

Les bornes sont **les horaires réels du jour**, jamais des heures d'horloge.
C'est tout l'intérêt, et c'est ce qui rend la règle 1 correcte : la nuit du
vendredi commence au Maghrib du jeudi, donc jeudi soir c'est al-Mulk qui gagne, et
al-Kahf ne prend le relais qu'au Fajr — al-Kahf est la sourate de la *journée*.

La règle 2 couvre les heures d'après minuit par `now < fajr` : on est alors après
l'Isha de la veille.

La carte reste une **proposition** : on peut l'ignorer, ce n'est jamais une
modale. Elle est teintée en `tertiary` et non en `primary`, comme les jours de
Ramadan du calendrier — se distinguer du vert de la marque sans lui disputer
l'attention.

**Ajouter une règle** : `domain/QuranSuggestion.kt` (la règle et son `Reason`),
`ui/quran/QuranLabels.kt` (le `labelRes`), les **trois** `strings.xml`, et un
test dans `QuranSuggestionTest`.

---

## Ce qui reste ouvert

- **Téléchargement pour écoute hors ligne.** Écarté sciemment cette session : il
  demande la gestion du stockage, la reprise de téléchargement, la place occupée
  et un écran de gestion. À reprendre si le besoin se confirme à l'usage.
- **Les radios** (`/radios`) : quelques lignes de plus, mais un autre modèle de
  lecture (flux continu, pas de durée, pas de file).
- **La Timing API** : surligner le verset en cours pendant la récitation.
- **Minuterie d'arrêt** — et surtout sa variante propre à Miqaat, « arrêter à la
  prochaine prière », pour s'endormir sur al-Mulk sans que ça tourne toute la nuit.
- **Vérification sur appareil** : voir la section « Prochaine étape » de
  `CLAUDE.md`. Rien de la session 14 n'a encore tourné sur un vrai téléphone.
