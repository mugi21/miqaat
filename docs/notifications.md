# Notifications : la chaîne, les canaux, le mode d'alerte

Tout ce qui sort de l'application — l'adhan, le rappel qui le précède, les
adhkār — passe par le même circuit. Ce fichier le décrit d'un bout à l'autre.
Les décisions correspondantes vivent dans [decisions.md](decisions.md) :
D3, D15, D17, D18, D20, D25, D31 à D39.

## La chaîne d'alarmes

**Une seule alarme à la fois** (D3). Chaque déclenchement replanifie la suivante.

```
PrayerAlarmScheduler.scheduleNext()
  ├─ nextEvent()            position + réglages + horaires du jour et du lendemain
  │                          → AlarmEventResolver → prochain ScheduledEvent
  ├─ setExactAndAllowWhileIdle(RTC_WAKEUP, …)   requestCode 1001
  │     replis : setAlarmClock (permission révoquée) puis set() (surcouche récalcitrante)
  ├─ setInexactRepeating(… INTERVAL_HALF_DAY)   requestCode 1003 — le chien de garde (D33)
  └─ NextPrayerWidget.refresh()                 toujours en dernier (D15)

PrayerAlarmReceiver.onReceive()
  try {
      si notifications autorisées :
          garde de fraîcheur (D31) → périmé ? on n'affiche rien
          notification posée
          AlertResolver.resolve(mode, ringerState)      (D36)
          AlertVibrator.vibrate(…)                      depuis le receiver (D38)
          AlertSoundService.start(…) si un son est décidé
      ReliabilityLog.recordFired()
  } finally {
      scheduleNext()        la chaîne ne se rompt jamais
  }
```

**Ce qui la resynchronise** (`RescheduleReceiver`, D32) : redémarrage, changement
d'heure ou de fuseau, **mise à jour de l'application**, changement de la permission
d'alarme exacte, chien de garde. Plus l'ouverture de l'app, un changement de
réglage, de position, ou d'invocation.

⚠ `PrayerAlarmReceiver` **ne se renomme pas** : les alarmes déjà posées par la
version installée pointent sur ce nom de classe. Ajouter un extra est sûr ; changer
le nom ou le `requestCode` orpheline l'alarme en vol.

## Les canaux

| Canal | ID actuel | Importance | Son | Vibration |
|---|---|---|---|---|
| Adhan | `prayer_times_v4` | HIGH | canal muet, joué par le service | canal muet, émise par le receiver |
| Rappel | `prayer_reminder_v3` | HIGH | idem | idem |
| Adhkār | `invocations_v2` | DEFAULT | idem | idem |

Historique des identifiants, tous listés dans `OLD_IDS` et supprimés au lancement :
`prayer_times_v1` → `v2` (son réel de l'utilisateur) → `v3` (canal rendu muet, D20)
→ `v4` (vibration retirée, D38) · `prayer_reminder_v1` → `v2` (D20) → `v3` (D38) ·
`invocations_v1` → `v2` (D39).

### Checklist « je touche à un canal »

1. Android **fige** les réglages d'un canal à sa création. Un changement de son,
   de vibration ou d'importance n'a d'effet que sur un **nouvel identifiant**.
2. Bumper l'identifiant dans `NotificationChannels`.
3. Ajouter l'ancien à `OLD_IDS`, sinon il reste en canal fantôme chez l'utilisateur.
4. Prévenir dans la note de version : le bump efface les personnalisations que
   l'utilisateur avait faites sur ce canal.
5. Changer seulement le **fichier son** ne demande rien de tout cela : les canaux
   sont muets, le son vient de `PrayerNotifications.soundOf`.

## Le mode d'alerte

Réglé dans les réglages (`settings_notification_mode`), stocké sous
`notification_mode`, appliqué aux **trois** natures d'alerte.

| mode ＼ téléphone | sonnerie | vibreur | silencieux |
|---|---|---|---|
| Suivre le téléphone (défaut) | flux sonnerie + impulsion | — + série | — + rien |
| Toujours sonner | flux sonnerie + impulsion | **flux alarme** + impulsion | **flux alarme** + impulsion |
| Toujours vibrer | — + série | — + série | — + série |
| Toujours silencieux | — + rien | — + rien | — + rien |

- **Flux sonnerie** = `USAGE_NOTIFICATION_RINGTONE` → `STREAM_RING`, le volume de
  la sonnerie d'appel. Muet quand le téléphone l'est.
- **Flux alarme** = `USAGE_ALARM` → `STREAM_ALARM`, que le mode sonnerie ne coupe
  pas. Souvent proche du volume maximal : c'est le sens de « toujours sonner ».
- La notification est **toujours** posée, même en mode silencieux. Seule l'alerte
  sonore et vibratoire change.

### Ne pas déranger

| Filtre | Ce qui passe |
|---|---|
| Prioritaire | comme d'habitude, si le canal est autorisé |
| Alarmes seulement | « toujours sonner » (flux alarme) ; pas « suivre le téléphone » |
| Silence total | **rien**, flux alarme compris |

Contourner le silence total demanderait `ACCESS_NOTIFICATION_POLICY`, permission
lourde qu'on ne demande pas (D37). `RingerReader` traite « alarmes seulement » et
« silence total » comme silencieux.

## La garde de fraîcheur

| Évènement | Tolérance |
|---|---|
| Adhan | 20 min |
| Rappel | **5 min** — doit rester sous `LEAD_CHOICES.min()` (10 min) |
| Invocation | 30 min |

Au-delà, la notification n'est **pas** affichée mais la chaîne se replanifie. Un
`EXTRA_TRIGGER_AT` absent (alarme d'une version antérieure) vaut « frais ».

## Points de vérification adb

```bash
adb shell dumpsys alarm | grep -A 25 com.mohamed.miqaat
adb shell dumpsys notification --noredact | grep -E "prayer_times|prayer_reminder|invocations"
adb shell dumpsys audio | grep -i "ringer mode"
adb shell cmd notification set_dnd priority   # puis alarms, none, off

# Replanification après mise à jour (le receiver est exporté, pas besoin de root)
adb shell am broadcast -a android.intent.action.MY_PACKAGE_REPLACED \
  -n com.mohamed.miqaat/.notifications.RescheduleReceiver

# Garde de fraîcheur : émettre un évènement daté de trois heures (adb root requis,
# PrayerAlarmReceiver n'est pas exporté)
adb root
adb shell am broadcast -n com.mohamed.miqaat/.notifications.PrayerAlarmReceiver \
  --es prayer FAJR --es kind REMINDER --el trigger_at <epoch_ms>
# attendu : aucune notification, un log « périmé », une alarme replanifiée
```

⚠ `am broadcast` à la main ne peut pas démarrer le service sonore
(`ForegroundServiceStartNotAllowedException`) : l'exemption ne vaut que pour une
vraie alarme exacte. Mettre l'app au premier plan d'abord, ou utiliser le bouton
« notification de test » de l'écran de fiabilité.

Astuce de mesure : `AudioTrack … frames delivered` ÷ fréquence d'échantillonnage
donne la durée **réellement sortie**, ce qui distingue « le son n'est pas joué » de
« le son est joué mais inaudible ».
