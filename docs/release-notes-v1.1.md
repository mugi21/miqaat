# Notes de version v1.1 — à coller dans le formulaire GitHub

Titre de la release : **Miqaat 1.1 — مِيقات**

---

مواقيت الصلاة أينما كنت، دون إنترنت
_Les horaires de prière où que vous soyez, sans Internet._

Cette version est consacrée à une seule chose : **que la notification arrive, à
l'heure, même application fermée**. Plus un nouveau réglage du mode d'alerte.

## Corrigé

- **Les notifications n'arrivaient pas application fermée** sur les appareils dont
  la surcouche gèle les applications en arrière-plan (Xiaomi/MIUI, Huawei, Oppo,
  Vivo…). L'application ne pouvait rien y faire seule : elle sait désormais le
  **détecter** et vous emmener au bon réglage. Voir le nouvel écran ci-dessous.
- **Une alerte en retard ne s'affiche plus.** On pouvait recevoir « le Fajr
  approche » à 14 h, quand une alarme bloquée depuis l'aube était enfin délivrée.
  Passé un délai raisonnable, l'alerte est désormais ignorée.
- **Une mise à jour de l'application interrompait la chaîne d'alarmes** jusqu'à sa
  prochaine ouverture. Elle se replanifie maintenant toute seule.
- Une seconde alarme de contrôle répare la chaîne si un évènement se perd.

## Nouveau — écran « Fiabilité des notifications »

Dans les réglages. Il vérifie cinq points (notifications autorisées, alarmes
exactes, optimisation de la batterie, démarrage automatique du constructeur, et
si une alerte a réellement été reçue récemment), propose un bouton pour corriger
chacun, et affiche l'heure de la prochaine alerte ainsi que celle de la dernière
reçue. Un bouton « notification de test » permet d'entendre le résultat sans
attendre une prière.

Un avertissement apparaît sur l'accueil uniquement si quelque chose de sérieux et
de certain empêche les notifications d'arriver.

## Nouveau — mode d'alerte

Quatre choix, dans les réglages, valables pour l'adhan, le rappel **et** les
adhkār :

| Mode | Effet |
|---|---|
| Suivre le mode du téléphone (défaut) | sonnerie → son, vibreur → vibration, silencieux → rien |
| Toujours sonner | son même en vibreur ou en silencieux |
| Toujours vibrer | vibration seule |
| Toujours silencieux | notification visuelle uniquement |

Le son sort désormais au volume de la **sonnerie d'appel** et non à celui des
notifications. En mode « toujours sonner », il emprunte le volume des alarmes —
seul moyen de se faire entendre sur un téléphone en silencieux. Le filtre « Ne pas
déranger / silence total » d'Android reste prioritaire sur tout.

> ⚠ **À la mise à jour**, les canaux de notification changent d'identifiant : si
> vous aviez personnalisé leur son ou leur vibration dans les réglages Android,
> ces réglages reviennent à zéro. Le nouveau mode d'alerte les remplace.

## Toujours vrai

Gratuite, sans publicité, sans achat intégré, sans aucun SDK de suivi. Aucune
donnée ne quitte l'appareil ; la position ne sert qu'aux calculs, faits localement.
La permission Internet n'est pas requise pour les fonctions principales.

Cinq prières et le shurūq avec compte à rebours · boussole Qibla hors ligne ·
calendriers grégorien et hégirien avec horaires de Ramadan · invocations et
adhkār · widget d'écran d'accueil · 21 méthodes de calcul choisies
automatiquement selon le pays · arabe (RTL), français, anglais.

## Installation

Android **8.0 (API 26)** minimum. S'installe par-dessus la 1.0 sans rien perdre
(même clé de signature).

- Fichier : `miqaat-1.1.apk`
- SHA-256 : `D15BDCBAB8FA0062CC42BD960E9BE83AC18330096F77E719AA4E1D15333112CA`
- Signature v2 + v3, certificat `CN=Mohamed Boughouas, O=Miqaat, C=DZ`
  (SHA-256 du certificat : `1af97066f2706edbc7d5704bace12929f74253dedaeeacf75743b04f3ba3510d`)

Vérification de l'empreinte après téléchargement :

```powershell
Get-FileHash miqaat-1.1.apk -Algorithm SHA256
```

Licence GPL v3.
