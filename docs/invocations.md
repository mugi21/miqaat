# Invocations et adhkār

Écran « الأدعية والأذكار » : deux invocations livrées avec l'application
(أذكار الصباح / أذكار المساء), lisibles, avec un rappel réglable — et autant
d'invocations écrites par l'utilisateur qu'il le souhaite.

Décisions liées : [D25](decisions.md) (chaîne d'alarmes et garde),
[D26](decisions.md) (contenu en ressources), [D27](decisions.md) (canal sonore).

## Deux natures d'invocation

| | Livrée (`builtin != null`) | Écrite par l'utilisateur |
|---|---|---|
| Titre | ressource, traduit fr/ar/en | saisi, stocké en base |
| Texte | ressource, arabe, `translatable="false"` | saisi, stocké en base |
| Moment | réglable | réglable |
| Désactivable | oui | oui |
| Supprimable | **non** (`DELETE … AND builtinKey IS NULL`) | oui |

Le semis vit dans `InvocationRepository.ensureSeeded()` : un
`OnConflictStrategy.IGNORE` sur les **ids fixes** de `BuiltinInvocation`
(MORNING = 1, EVENING = 2). Il tourne à chaque première lecture, sans jamais
écraser un réglage — c'est ce qui permet à une désactivation de survivre.

**Ajouter une invocation livrée :** une entrée dans `BuiltinInvocation` (avec son
id fixe), son `seedEntity()`, ses deux clés dans `ui/InvocationLabels.kt`, et les
textes dans `strings.xml`. Aucune migration de base.

## Le moment du rappel

`InvocationSchedule` a deux formes :

- `FixedTime(hour, minute)` — une heure d'horloge, tous les jours ;
- `PrayerAnchor(prayer, offsetMinutes)` — un décalage par rapport à une prière,
  borné à `[-120, +240]` et réglé par pas de 5 minutes.

Les deux invocations livrées sont **ancrées** par défaut (matin = Fajr + 30 min,
soir = Asr + 30 min) : l'heure suit alors les saisons toute seule, ce qu'une
heure d'horloge ne fait pas.

## La garde de dix minutes

C'est la règle la plus importante du dossier, et la seule surprise possible pour
l'utilisateur.

Android n'accorde à l'application qu'une alarme exacte toutes les ~9 minutes en
Doze — quota déjà responsable du minimum de 10 minutes du rappel avant l'adhan
(D18). Il vaut pour toute l'app, donc une invocation trop proche d'une prière
ferait **reporter l'adhan**.

`AlarmEventResolver.applyGuard` écarte donc les invocations, jamais les prières :
une invocation à moins de `GUARD_MINUTES` d'un évènement déjà placé est repoussée
à `celui-ci + 10 min`. Concrètement, un dhikr réglé « 5 min avant l'Asr » sonnera
10 minutes **après** l'adhan. L'écran l'annonce en une ligne
(`invocation_guard_hint`).

Changer la valeur : `AlarmEventResolver.GUARD_MINUTES` — et rien d'autre, les
tests la lisent depuis la constante.

## Identifiants réservés

| Plage | Usage |
|---|---|
| 0..5 | notifications d'adhan (`PrayerName.ordinal`) |
| 100..105 | notifications de rappel avant l'adhan |
| 1000 + id | notifications d'invocation |
| 3000 + id | `requestCode` du `PendingIntent` d'ouverture d'une invocation |

## Chemin d'une notification d'invocation

1. `PrayerAlarmScheduler.scheduleNext()` demande le prochain `ScheduledEvent` à
   `AlarmEventResolver` et met `EXTRA_INVOCATION` dans l'intent si c'en est une.
2. `PrayerAlarmReceiver` relit l'invocation (elle a pu être supprimée ou coupée
   entre la pose de l'alarme et son déclenchement), vérifie sa fraîcheur (D31),
   pose la notification, puis applique le **mode d'alerte** comme pour une prière
   — vibration et son via `AlertSoundService` (D39, qui renverse D27) — et
   replanifie.
3. Un appui ouvre `MainActivity` (`singleTop`, donc `onNewIntent`) sur l'écran
   des invocations, directement sur la bonne entrée.

## Où toucher pour…

| Évolution | Fichiers |
|---|---|
| Ajouter une invocation livrée | `BuiltinInvocation` + `seedEntity()` + `ui/InvocationLabels.kt` + les trois `strings.xml` |
| Changer la garde | `AlarmEventResolver.GUARD_MINUTES` |
| Changer les bornes du décalage | `InvocationSchedule.OFFSET_MIN` / `OFFSET_MAX` / `OFFSET_STEP` |
| Changer le son du rappel d'adhkār | `AlertSoundService.resolveTarget` (aujourd'hui `DEFAULT_NOTIFICATION_URI`) ; le canal `invocations_v2` est muet, aucun ID à bumper |
| Ajouter un champ à une invocation | `InvocationEntity` + une migration Room + `InvocationRepository` |
