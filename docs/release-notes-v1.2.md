# Notes de version v1.2 — à coller dans le formulaire GitHub

Titre de la release : **Miqaat 1.2 — مِيقات**

---

مواقيت الصلاة أينما كنت، دون إنترنت
_Les horaires de prière où que vous soyez, sans Internet._

Cette version ajoute **l'écoute du Coran**, et deux comportements qu'une
application de récitation seule ne peut pas offrir : le lecteur s'arrête de
lui-même à l'heure de la prière, et la sourate proposée dépend du moment réel de
votre journée.

## Nouveau — écouter le Coran

Quatrième icône en haut de l'accueil. Environ 130 récitateurs, les 114 sourates,
lecture en continu.

- **Favoris** : marquez un récitateur pour le retrouver en tête de liste, et une
  sourate pour la retrouver plus vite.
- **Contrôles complets** depuis la notification et l'écran verrouillé : lecture,
  pause, sourate précédente et suivante, avance et recul de dix secondes.
- **Mini-lecteur en bas de l'écran** : il reste visible quand vous revenez à
  l'accueil, donc vous pouvez consulter les horaires sans couper la récitation.
- Le récitateur choisi enchaîne les sourates suivantes tout seul.

## Nouveau — le lecteur cède la place à l'adhan

À l'heure de la prière comme au rappel, la récitation **se met en pause d'elle-même**.

- Si l'alerte a du son, elle **reprend seule** une fois l'appel à la prière terminé.
- Si votre téléphone est en vibreur ou en silencieux, elle s'arrête simplement —
  une tape sur le mini-lecteur la relance.
- Une invocation (adhkār) n'interrompt rien : elle ne fait que baisser le volume
  quelques secondes.

## Nouveau — la sourate du moment

Une carte en haut de l'écran propose une sourate selon **les horaires réels de
votre journée**, et non selon l'heure qu'il est :

| Quand | Sourate |
|---|---|
| Le vendredi, du Fajr au Maghrib | al-Kahf |
| Après l'Isha, jusqu'au Fajr | al-Mulk |
| Entre le Fajr et le shurūq | Yā-Sīn |
| Entre le Maghrib et l'Isha | al-Wāqiʿa |
| Le reste de la journée | ar-Raḥmān |

C'est ce qui rend la première juste : la nuit du vendredi commence au Maghrib du
jeudi, donc le jeudi soir c'est al-Mulk qui est proposée, et al-Kahf prend le
relais au Fajr.

## Ce qui change côté vie privée

L'application demande désormais la permission **Internet**. Elle sert
**uniquement** à l'écoute du Coran :

- aucune fonction principale n'y touche — horaires, notifications, Qibla,
  calendrier, adhkār et widget restent **entièrement hors ligne** ;
- un seul serveur est contacté, **mp3quran.net**, sans compte ni clé ;
- rien n'est envoyé : aucune donnée, aucun identifiant, aucune statistique ;
- toujours **aucun SDK** de suivi, d'analyse ou de publicité.

La liste des récitateurs est enregistrée sur l'appareil au premier chargement :
la parcourir fonctionne ensuite hors ligne. **Écouter demande une connexion** —
c'est de la diffusion en direct, rien n'est téléchargé sur le téléphone.

## Toujours vrai

Gratuite, sans publicité, sans achat intégré. La position ne sert qu'aux calculs,
faits localement sur l'appareil.

Cinq prières et le shurūq avec compte à rebours · boussole Qibla hors ligne ·
calendriers grégorien et hégirien avec horaires de Ramadan · invocations et
adhkār · widget d'écran d'accueil · 21 méthodes de calcul choisies
automatiquement selon le pays · mode d'alerte et écran de fiabilité des
notifications · arabe (RTL), français, anglais.

## Installation

Android **8.0 (API 26)** minimum. S'installe par-dessus la 1.1 sans rien perdre
(même clé de signature) : position, réglages, invocations et favoris sont conservés.

- Fichier : `miqaat-1.2.apk`
- SHA-256 : `97E2DB0330A3F762F73A00C67272DD2CCF4A8A12A2478CFE29649AD9C90ABEAB`
- Signature v2 + v3, certificat `CN=Mohamed Boughouas, O=Miqaat, C=DZ`
  (SHA-256 du certificat : `1af97066f2706edbc7d5704bace12929f74253dedaeeacf75743b04f3ba3510d`)

Vérification de l'empreinte après téléchargement :

```powershell
Get-FileHash miqaat-1.2.apk -Algorithm SHA256
```

Licence GPL v3.
