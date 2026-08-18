# Notes de version v1.3 — à coller dans le formulaire GitHub

Titre de la release : **Miqaat 1.3 — مِيقات**

---

Miqaat sait maintenant se mettre à jour toute seule.

## Nouveau — mise à jour depuis l'application

Jusqu'ici, il fallait penser à revenir sur cette page pour savoir qu'une nouvelle
version existait. Désormais :

- **Une note apparaît sur l'écran d'accueil** quand une version plus récente est
  publiée. « Plus tard » la fait taire une semaine, « ignorer cette version »
  définitivement — la suivante repassera.
- **Un écran dédié** (accessible aussi depuis les réglages) affiche la version
  installée, la version disponible et ses nouveautés, puis télécharge et installe
  l'APK sans quitter l'application. La progression survit à un aller-retour vers
  les réglages du téléphone.
- Le fichier téléchargé est **vérifié** (taille et empreinte SHA-256) avant d'être
  proposé à l'installation. Et comme toujours, Android refusera de l'installer
  s'il n'est pas signé par la même clé que la version en place.
- **Un bouton « ouvrir la page de la version »** est toujours offert : sur certains
  téléphones — Xiaomi en particulier — l'installateur du système est capricieux, et
  c'est le chemin de secours.

## Vie privée

C'est la première fois que Miqaat contacte un serveur **de sa propre initiative**,
alors autant être précis :

- Elle lit la liste des versions publiées sur `github.com`, **une fois par jour au
  plus**, et seulement quand vous ouvrez l'application.
- Elle **n'envoie rien** : aucune donnée, aucun identifiant, aucun compte, aucun
  cookie, toujours aucun SDK de publicité ou de statistiques.
- **Vous pouvez tout couper** depuis l'écran de mise à jour, en un interrupteur.
- Le téléchargement ne démarre que si vous le demandez.

## Toujours vrai

Les horaires de prière, les notifications, la Qibla, le calendrier, les adhkār et
le widget **ne touchent jamais au réseau** : tout est calculé sur l'appareil. Vous
pouvez rester en mode avion indéfiniment, l'adhan arrivera à l'heure.

## Installation

Android **8.0 (API 26)** minimum. S'installe par-dessus la 1.2.1 sans rien perdre.

- Fichier : `miqaat-1.3.apk`
- SHA-256 : `B77660271601027FAA2593613B8A0BF435642FCFA219B379BC41AC6E2F19C35A`
- Signature v2 + v3, certificat `CN=Mohamed Boughouas, O=Miqaat, C=DZ`
  (SHA-256 du certificat : `1af97066f2706edbc7d5704bace12929f74253dedaeeacf75743b04f3ba3510d`)

```powershell
Get-FileHash miqaat-1.3.apk -Algorithm SHA256
```

> ⚠ **À la première mise à jour depuis l'application**, Android demandera
> l'autorisation d'installer des applications depuis Miqaat. Elle se donne une
> seule fois, et l'écran vous y conduit.

versionCode: 6

Licence GPL v3.
