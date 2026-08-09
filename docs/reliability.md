# Fiabilité : pourquoi l'adhan n'arrive pas, et comment le réparer

Une alarme exacte parfaitement posée ne sert à rien si le système refuse de la
délivrer. C'est le cas le plus fréquent en pratique, et il est **invisible** :
aucune erreur, aucun log, simplement rien. Ce fichier décrit les cinq verrous que
l'application sait diagnostiquer, et la procédure sur les appareils qui posent
problème. Décisions : D31 à D35.

## Le symptôme qui a tout déclenché

Redmi Note 8, Android 10 / MIUI, session 12. Aucune notification pendant la nuit ;
en ouvrant l'app vers 14h, l'utilisateur reçoit « le Fajr approche ». L'alarme de
4h avait bien été posée — elle n'a été **délivrée** qu'au moment où le processus a
redémarré, dix heures plus tard.

Deux problèmes distincts, deux corrections distinctes :
- l'alerte périmée s'affichait → garde de fraîcheur (D31), corrigée dans le code ;
- l'application était gelée → **rien dans le code ne peut le corriger**, seul
  l'utilisateur le peut. D'où cet écran.

## Les cinq contrôles

| Contrôle | Lecture | Action proposée | État possible |
|---|---|---|---|
| Notifications | `areNotificationsEnabled()` | `ACTION_APP_NOTIFICATION_SETTINGS` | OK / à corriger |
| Alarmes exactes | `canScheduleExactAlarms()` | `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` | sans objet hors Android 12/12L |
| Batterie | `isIgnoringBatteryOptimizations()` | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | OK / à corriger |
| Démarrage automatique | **illisible** | écran de la surcouche | inconnu, ou OK si l'utilisateur l'a déclaré |
| Délivrance | `ReliabilityLog.lastFiredAt()` | aide surcouche | à corriger si rien depuis 24 h |

**Alarmes exactes — pourquoi « sans objet » au-delà d'Android 12L :** en dessous
d'Android 12, aucune permission n'est requise ; à partir d'Android 13,
`USE_EXACT_ALARM` est accordée d'office aux applications dont l'alarme est la
fonction principale, et n'est pas révocable. Le contrôle ne vaut donc que pour deux
versions.

**Délivrance — le seul détecteur automatique du gel.** Le receiver horodate chaque
déclenchement. Une application installée depuis plus de 24 h qui n'a jamais rien
délivré est nécessairement empêchée de s'exécuter : cinq prières par jour ne
peuvent pas toutes se manquer. La condition sur la date d'installation évite
d'accuser à tort une application posée il y a dix minutes.

## La règle anti-harcèlement

La bannière d'accueil n'apparaît que si un contrôle **critique** (notifications,
alarmes exactes, délivrance) est en « à corriger », c'est-à-dire sur du certain.

Un état **inconnu n'alarme jamais** : l'état du démarrage automatique n'étant pas
lisible, en faire une alerte condamnerait tout possesseur de Xiaomi à un
avertissement permanent qu'aucune action ne pourrait éteindre. « Plus tard » fait
taire la bannière quatorze jours ; l'écran reste accessible depuis les réglages.

Jamais de dialogue modal au lancement.

## Les écrans de démarrage automatique par fabricant

`data/reliability/OemAutostart.kt`. Composants essayés dans l'ordre — les noms
changent d'une version de surcouche à l'autre.

| Fabricant | Composants |
|---|---|
| Xiaomi / Redmi / Poco | `com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity`, puis `com.miui.securitycenter/com.miui.powercenter.PowerSettings` |
| Huawei / Honor | `…/startupmgr.ui.StartupNormalAppListActivity`, `…/appcontrol.activity.StartupAppControlActivity`, `…/optimize.process.ProtectActivity` |
| Oppo / Realme | `com.coloros.safecenter/…permission.startup.StartupAppListActivity`, `…startupapp.StartupAppListActivity`, `com.oppo.safe/…StartUpAppListActivity` |
| Vivo / iQOO | `com.vivo.permissionmanager/….BgStartUpManagerActivity`, `com.iqoo.secure/….AddWhiteListActivity` |
| Samsung | `com.samsung.android.lool/…sm.ui.battery.BatteryActivity` |
| OnePlus | `com.oneplus.security/….ChainLaunchAppListActivity` |

Sur MIUI, les **deux** écrans comptent : le démarrage automatique et l'économiseur
de batterie sont deux verrous distincts, l'un sans l'autre ne suffit pas.

> ⚠ **Ajouter un fabricant, c'est deux fichiers.** La table, **et** le bloc
> `<queries>` du `AndroidManifest.xml`. Sans la seconde, `resolveActivity` renvoie
> `null` sur Android 11+ (filtrage de visibilité des paquets) et l'entrée est
> silencieusement ignorée. C'est l'oubli le plus probable.

## Procédure MIUI, pas à pas

1. Réglages → Applications → Gérer les applications → Miqaat
2. **Démarrage automatique** : activé
3. **Économiseur de batterie** : « Aucune restriction »
4. **Notifications** : autorisées, et le canal « Adhan » non désactivé
5. Verrouiller l'app dans les applications récentes (icône cadenas) — MIUI la tue
   moins volontiers

Puis, dans Miqaat : Réglages → Fiabilité des notifications → « C'est fait » sur le
démarrage automatique, et **notification de test** pour vérifier le son.

## Vérifications adb

```bash
# L'app est-elle en « stopped state » ? Dans ce cas, même BOOT_COMPLETED ne passe pas.
adb shell dumpsys package com.mohamed.miqaat | grep -i stopped

# Exclusion de l'optimisation de batterie
adb shell dumpsys deviceidle whitelist | grep miqaat

# Alarmes posées (l'exacte + le chien de garde)
adb shell dumpsys alarm | grep -A 25 com.mohamed.miqaat

# Doze
adb shell dumpsys deviceidle force-idle && adb shell dumpsys deviceidle step
adb shell dumpsys deviceidle unforce

# Révoquer la permission d'alarme exacte (Android 12/12L)
adb shell cmd appops set com.mohamed.miqaat SCHEDULE_EXACT_ALARM ignore   # puis allow
```

⚠ Sur MIUI, `adb install` est refusé (`INSTALL_FAILED_USER_RESTRICTED`) tant que
« Installation via USB » n'est pas activée dans les options développeur, et
`adb shell input` est refusé dès qu'une boîte de dialogue système a le focus.

⚠ Toujours vérifier **quelle build tourne** avant de conclure : `adb shell pm path
<pkg>` puis `sha256sum` sur le `base.apk`, comparé au fichier compilé. Une build
debug installée par Android Studio remplace silencieusement la release.
