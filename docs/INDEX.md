# Index de la documentation Miqaat

Point d'entrée de `docs/`. `CLAUDE.md` (racine) reste la mémoire vivante du projet :
vision, stack, roadmap et **État actuel** mis à jour à chaque fin de session.

| Fichier | Contenu |
|---|---|
| [dev-workflow.md](dev-workflow.md) | Comment on travaille : build, tests, rituel de session, conventions de code et de langue |
| [decisions.md](decisions.md) | Décisions d'architecture notables, avec leur raison et leurs conséquences |
| [file-map.md](file-map.md) | Carte des fichiers : où vit quoi, et quoi toucher pour telle évolution |
| [i18n.md](i18n.md) | Multilingue : arabe par défaut, français, anglais — règles et pièges |
| [prayer-times-accuracy.md](prayer-times-accuracy.md) | Coller à un calendrier officiel : arrondi, marge de précaution, protocole de mesure |
| [notifications.md](notifications.md) | La chaîne d'alarmes, les canaux et leurs identifiants, le mode d'alerte, la garde de fraîcheur |
| [reliability.md](reliability.md) | Pourquoi l'adhan n'arrive pas : les cinq verrous, les surcouches constructeur, la procédure MIUI |
| [invocations.md](invocations.md) | Adhkār : invocations livrées et créées, moment du rappel, garde de dix minutes |
| [quran.md](quran.md) | Écoute du Coran : l'API mp3quran et ses pièges, le cache, le lecteur, la sourate du moment |
| [release.md](release.md) | Publier une version : clé de signature, build release, tag et release GitHub |

## Règles de tenue

- Un choix d'architecture notable → une entrée dans `decisions.md`.
- Un fichier important créé → une ligne dans `file-map.md` (et ici s'il s'agit d'un doc).
- Un sujet qui grossit trop dans un fichier existant → nouveau fichier dédié, référencé ici.
