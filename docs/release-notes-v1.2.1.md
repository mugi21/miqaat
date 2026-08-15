# Notes de version v1.2.1 — à coller dans le formulaire GitHub

Titre de la release : **Miqaat 1.2.1 — مِيقات**

---

Correctifs de l'écoute du Coran, à partir des premiers retours d'usage de la 1.2.

## Corrigé

- **Les noms des récitateurs et des sourates restaient en arabe** même après
  avoir changé la langue de l'application. Ils suivent désormais la langue
  choisie ; le catalogue se recharge automatiquement au changement.
- **Le bouton du Coran chevauchait le nom de la ville** sur l'écran principal.
  Le contenu de l'en-tête passe maintenant sous la rangée de boutons.
- **La notification et l'écran verrouillé affichaient l'image d'une autre
  application.** Elle venait des fichiers audio eux-mêmes ; Miqaat pose
  désormais son propre logo, et sa propre icône dans la barre d'état.

## Amélioré

- **Liste des récitateurs** : les favoris ont leur section, avec le nombre
  d'entrées ; chaque ligne porte une pastille d'initiale et la rīwāya. Les
  récitateurs dont l'enregistrement est incomplet l'indiquent (« 83 sourates
  sur 114 »).
- **Liste des sourates** : le numéro est présenté dans une rosace à huit
  branches — le même motif que la mosaïque du widget — et chaque ligne indique
  l'origine et le **nombre de versets** (« Mecquoise · 110 versets »).
- La recherche a une loupe et un bouton d'effacement.

## Installation

Android **8.0 (API 26)** minimum. S'installe par-dessus la 1.2 sans rien perdre.

- Fichier : `miqaat-1.2.1.apk`
- SHA-256 : `0CF16634505D16C98B282806E77D2221996F2DEA410916D32A82EA7500C85BE4`
- Signature v2 + v3, certificat `CN=Mohamed Boughouas, O=Miqaat, C=DZ`
  (SHA-256 du certificat : `1af97066f2706edbc7d5704bace12929f74253dedaeeacf75743b04f3ba3510d`)

```powershell
Get-FileHash miqaat-1.2.1.apk -Algorithm SHA256
```

Licence GPL v3.
