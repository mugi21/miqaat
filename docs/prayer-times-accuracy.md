# Précision des horaires : coller à un calendrier officiel

Pourquoi l'app peut afficher une minute d'écart avec le calendrier d'un ministère,
comment on mesure l'écart, et comment on le corrige sans le deviner.

## Les trois sources d'écart, par ordre d'importance

### 1. La convention d'arrondi — la vraie cause

Le calcul astronomique tombe à la seconde. Adhan tranche en **arrondissant à la
minute la plus proche** (`Rounding.NEAREST`, vérifié dans `CalendarUtil.roundedMinute` :
`minute + round(seconde / 60)`). Un ministère ne fait jamais ça : il ajoute une marge de
précaution (*iḥtiyāṭ*) puis tronque, de sorte que l'heure annoncée ne tombe **jamais**
avant l'heure calculée. Prier un peu après le moment est sans conséquence, prier avant
invalide la prière — l'asymétrie est le motif de la règle.

Conséquence : l'app pouvait annoncer jusqu'à 30 secondes **trop tôt**, et l'écart
apparaissait ou non selon les secondes du jour. D'où le symptôme signalé — « une minute
d'écart, mais seulement sur certains moments, sans logique apparente ».

### 2. La marge de précaution du ministère

Elle ne figure dans aucune spécification (ni AlAdhan, ni Adhan) et se relève sur le
calendrier officiel. Elle est **spécifique au moment** : en Algérie, le Maghrib porte
trois minutes que les autres moments n'ont pas.

### 3. La position — marginale

Mesuré en session 8 : **~4 secondes par kilomètre**. Cinq kilomètres coûtent 13 secondes,
vingt kilomètres 53 secondes. Autrement dit, se déplacer dans une ville ne peut pas
produire une minute d'écart ; il faut être à ~20 km du point de référence de la wilaya
pour en atteindre une. Ce n'est donc jamais la première explication à examiner.

## Protocole de mesure

Une marge ne se devine pas sur deux dates : il faut un **mois entier**, sinon la marge et
l'arrondi restent indiscernables (les deux produisent des écarts d'une minute).

1. **Se procurer un calendrier officiel** d'un mois complet pour la ville, en image ou en
   texte — un document du ministère ou de sa direction de wilaya, pas une autre application.
2. **Sortir les horaires bruts à la seconde** : un test jetable qui appelle Adhan avec
   `Rounding.NONE` et sans aucune marge, pour les mêmes coordonnées, le même fuseau et le
   même madhab, sur les mêmes jours.
3. **Ajuster un décalage constant par moment.** Le modèle est
   `officiel = tronquer_à_la_minute(brut + C)`, une seule inconnue `C` en secondes par
   moment. Chaque jour contraint `C` à un intervalle de 60 secondes
   (`C ∈ [officiel − brut, officiel − brut + 60)`) ; l'intersection des 30 intervalles donne
   `C` à quelques secondes près. **Une intersection vide est une information** : soit la
   transcription du calendrier contient une erreur, soit le ministère n'utilise pas un
   décalage constant.
4. **Reporter la valeur retenue** dans la `calibration` de la méthode
   (`domain/model/MethodOption.kt`) et **figer les 30 jours dans un test**.

Le script d'ajustement n'est pas versionné : il ne sert qu'une fois par pays, et le test
des 30 jours est ce qui doit survivre.

## Algérie — mesure de référence

Source : **مديرية الشؤون الدينية والأوقاف — سكيكدة**, calendrier de la wilaya de Skikda,
Rabīʿ al-Awwal 1448 (14 août → 12 septembre 2026), 30 jours, cinq moments.
Coordonnées de calcul : 36,8665 N / 6,9063 E, `Africa/Algiers`, madhab jumhūr, angles 18°/17°.

| Moment | Décalage retenu | Jours exacts | Écart résiduel |
|---|---|---|---|
| Fajr | **95 s** | 30 / 30 | — |
| Ẓuhr | **85 s** | 30 / 30 | — |
| ʿAṣr | **126 s** | 25 / 30 | 5 jours à +1 min |
| Maghrib | **261 s** | 30 / 30 | — |
| ʿIshāʾ | **82 s** | 28 / 30 | 2 jours à +1 min |

**143 cases sur 150**, et surtout **aucune en avance** sur l'heure officielle.

Ces cinq nombres se lisent en deux termes :

- une **base d'environ 85 secondes** commune à tous les moments — la minute de précaution
  du ministère, plus l'écart entre nos coordonnées et son point de référence pour la ville ;
- sur le seul Maghrib, **trois minutes de plus** (261 ≈ 85 + 176) : c'est la marge déjà
  repérée en session 8 et consignée en D23, cette fois confirmée sur trente jours au lieu de deux.

Le **shurūq n'a pas de colonne** dans le calendrier officiel. Faute de mesure, il garde un
décalage nul, donc tronqué : il marque la *fin* du Fajr, et l'annoncer un peu tôt est le
côté prudent.

### La règle d'arbitrage : jamais en avance

L'ʿAṣr et l'ʿIshāʾ ne se laissent pas décrire par un décalage constant — sur le mois, il
leur faudrait dériver d'une dizaine de secondes. Aucune valeur ne rend donc les trente
jours exacts, et il faut choisir de quel côté tombe le reste.

**On choisit le côté tardif, sans exception.** C'est la raison d'être de l'iḥtiyāṭ : une
minute de retard est sans conséquence, une minute d'avance fait prier avant l'heure. Pour
l'ʿAṣr, cela coûte deux jours exacts (126 s au lieu de 120 s, 25 lignes justes au lieu de
27) et évite d'annoncer le 11 septembre à 16:03 quand le calendrier imprime 16:04.

Trois tests figent cet arbitrage dans `AlgeriaOfficialCalendarTest` : jamais avant
l'officiel, jamais plus d'une minute après, et le nombre exact de lignes justes — pour
qu'une retouche de la calibration ne dégrade rien en silence.

### Une hypothèse écartée : le point de référence de la wilaya

La dérive de l'ʿAṣr et de l'ʿIshāʾ pouvait venir de coordonnées différentes : le Ẓuhr ne
dépend que de la longitude, l'ʿAṣr et l'ʿIshāʾ aussi de la latitude. Une recherche par
balayage (±0,30° en latitude et longitude, au pas de 0,01°) trouve bien des couples qui
rendent les cinq décalages constants — mais tous demandent une latitude environ 0,20° plus
au sud, soit ~22 km à l'intérieur des terres, ce qui ne décrit pas le chef-lieu d'une
wilaya côtière. Et même à ces coordonnées, les marges restent inégales d'un moment à
l'autre : l'hypothèse n'explique rien qu'elle ne suppose. Elle est écartée.

### État avant correction

Sur ces mêmes 30 jours, l'app tombait juste sur **1 ligne pour le Fajr, 1 pour le Ẓuhr,
0 pour l'ʿAṣr, 5 pour le Maghrib et 4 pour l'ʿIshāʾ**. Elle n'était pas « parfois d'une
minute à côté » : elle était presque systématiquement une minute en avance, et l'utilisateur
ne remarquait que les cas les plus visibles.

### Ce qui reste ouvert

- **Une seule saison mesurée.** La calibration est ajustée sur un mois d'été. Un relevé de
  session 8 pour le 15 décembre 2026 (Maghrib officiel 17:20) ne s'y accorde pas — mais
  `CLAUDE.md` indique que cette comparaison était menée « contre une autre app », donc
  probablement pas contre le calendrier du ministère. Un second calendrier officiel, d'un
  mois d'hiver, tranchera. En attendant, c'est l'image officielle de trente lignes qui fait foi.
- **La base est mesurée à Skikda.** Sa part « point de référence de la ville » (≈ 25 s) n'a
  pas de raison de valoir ailleurs en Algérie ; la part dominante — la marge de précaution —
  si. L'erreur résiduelle attendue dans une autre ville reste sous la minute.
- **Tunisie et Maroc** n'ont jamais été mesurés : leurs méthodes gardent la calibration par
  défaut (celle d'Adhan). Même protocole, un calendrier officiel d'un mois suffit.

## Ce que l'utilisateur peut corriger lui-même

`PrayerTimeAdjustments` (D24) reste disponible : des minutes entières, par moment, bornées à
±30, qui **s'ajoutent** à la calibration de la méthode sans l'effacer. C'est le bon outil
pour suivre le calendrier d'une mosquée qui s'écarte du national — pas pour rattraper une
convention d'arrondi, laquelle relève de la méthode.
