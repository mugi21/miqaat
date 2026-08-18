# Mise à jour de l'app depuis GitHub

Comment Miqaat sait qu'une nouvelle version existe, et comment elle l'installe.
Dispositif **temporaire**, en attendant une publication sur Google Play — voir D44
pour les raisons et les critères de retrait.

Pour publier une version, voir [release.md](release.md). Ce fichier-ci décrit ce que
l'app attend d'une release, et quoi faire quand ça coince.

---

## Le contrat d'une release

L'app lit `https://api.github.com/repos/mugi21/miqaat/releases/latest`. Quatre
points, du plus important au moins :

1. **Ni brouillon ni pré-version.** `/releases/latest` les ignore, et le parseur
   les rejette une seconde fois. Une release restée en brouillon n'est vue de
   personne — c'est le premier réflexe quand « la mise à jour ne s'affiche pas ».
2. **Le tag est purement numérique**, préfixé `v` ou non : `v1.3`, `1.3.1`. Un tag
   qui contient un `-` ou un `+` (`v1.3-rc1`, `v1.3+build7`) est refusé **en
   entier**, pas coupé — sinon une pré-version se ferait passer pour la version
   finale. Et le `versionName` du `build.gradle.kts` doit correspondre au tag,
   comme l'exige déjà `release.md` : c'est de leur comparaison que tout dépend.
3. **L'APK est joint, nommé `miqaat-<version>.apk`.** S'il y a plusieurs `.apk`,
   celui préfixé `miqaat-` gagne. Aucun `.apk` : la version est quand même
   annoncée, mais seul le bouton « ouvrir la page de la version » est offert.
   Toute URL d'asset qui n'est pas en `https://` est ignorée.
4. **Le corps de la release** est affiché tel quel (markdown brut, non rendu) et
   sert à deux extractions automatiques :

   | Ce qui est cherché | Forme attendue | Si absent |
   |---|---|---|
   | Empreinte de l'APK | une ligne `SHA-256 : <64 hex>` (backticks tolérés, casse indifférente) | pas de vérification d'empreinte, mais rien n'est bloqué |
   | Code de version | une ligne seule `versionCode: 6` | le tag décide seul |

   ⚠ Le gabarit de `release.md` publie **deux** empreintes : celle de l'APK, puis
   celle du certificat. C'est la première étiquetée `SHA-256 :` qui est lue —
   la seconde est annoncée par « SHA-256 du certificat : », qui ne suit pas la
   forme `clé : valeur` et n'est donc pas confondue. Ne pas inverser cet ordre.

   ⚠ La ligne `versionCode:` est **optionnelle mais recommandée**. Elle oppose un
   veto quand le code publié n'est pas supérieur à celui installé, ce qui épargne à
   l'utilisateur de télécharger l'APK entier pour se voir opposer un « Application
   non installée » que rien n'explique — Android refuse tout `versionCode` non
   croissant, et il le refuse après le téléchargement.

---

## Ce que l'app fait, et quand

| Moment | Ce qui se passe |
|---|---|
| Ouverture de l'app | `UpdateRepository.refreshIfDue()` — au plus une fois par 24 h, et seulement si la vérification automatique est active |
| Vérification réussie | tag, titre, **notes complètes**, URL et taille de l'APK, empreinte et `versionCode` sont écrits en `SharedPreferences` |
| Vérification échouée | rien n'est écrit : le quota n'est pas consommé, le prochain lancement réessaiera |
| Accueil | la note s'affiche si une version plus récente existe, n'a pas été ignorée, et que « plus tard » n'est pas en cours (7 jours) |
| Écran de mise à jour | vérification manuelle, téléchargement, installation, opt-out |
| Ouverture suivante | `cleanUpIfInstalled()` efface l'APK devenu inutile |

Après **une seule** vérification réussie, la note d'accueil et l'écran entier —
notes de version comprises — s'affichent hors ligne. Seul le téléchargement demande
le réseau.

L'utilisateur peut tout couper depuis l'écran de mise à jour. L'interrupteur y vit
plutôt que dans les réglages parce que la phrase qu'il gouverne — « l'app ne
contactera plus github.com » — ne tient pas dans le sous-titre d'une ligne.

---

## L'installation, et la friction MIUI

Le chemin nominal : `DownloadManager` écrit l'APK dans le dossier privé de l'app
(aucune permission de stockage), la taille et l'empreinte sont vérifiées, puis un
`ACTION_VIEW` sur un `content://` de `FileProvider` ouvre l'installateur système.

Android exige au passage l'autorisation **« installer des applications inconnues »**,
par application, donnée une seule fois. L'écran l'explique et propose le bouton qui
ouvre le réglage. Si aucun écran système ne répond, les instructions écrites
prennent le relais :

> Réglages ← Applications ← Miqaat ← Installer des applications inconnues

Sur le Redmi Note 8 (Android 10 / MIUI), trois particularités à connaître :

- l'installateur MIUI impose un compte à rebours de plusieurs secondes et une
  « vérification » qui passe par le réseau ; certains builds réclament un compte
  Xiaomi ;
- après l'octroi des sources inconnues, MIUI **tue et relance** l'app sur certains
  builds — c'est pour cela que l'identifiant du téléchargement est persisté et que
  l'écran s'y raccroche au retour ;
- `com.android.providers.downloads` peut avoir été désactivé, auquel cas le
  téléchargement échoue d'emblée.

Dans les trois cas, le bouton **« ouvrir la page de la version »** reste offert et
n'est jamais conditionnel : sur ces appareils, c'est parfois le seul chemin qui
aboutit.

---

## « La mise à jour ne s'affiche pas » — que vérifier

1. La release est-elle **publiée** (ni brouillon, ni pré-version) ?
2. Le tag est-il purement numérique et **supérieur** au `versionName` installé ?
   Attention : `1.2` est antérieure à `1.2.1`, et la comparaison est numérique
   (`1.10` est postérieure à `1.9`).
3. Le corps contient-il une ligne `versionCode:` **inférieure ou égale** à celle
   installée ? Elle oppose un veto, quoi que dise le tag.
4. L'utilisateur a-t-il tapé « ignorer cette version » (le tag est alors mémorisé)
   ou « plus tard » (7 jours de silence) ?
5. La vérification automatique est-elle restée active ?
6. Moins de 24 h depuis la dernière vérification **réussie** ? Le bouton
   « vérifier maintenant » de l'écran ignore ce délai.
7. Version installée illisible ? Un `versionName` vide ou non numérique ferme le
   repli : rien n'est jamais proposé sur une comparaison douteuse.

Côté journal : `GithubReleaseApi`, `ApkInstaller` et `UpdateRepository` écrivent en
`Log.w`/`Log.i` sous leurs propres étiquettes, et l'état persistant se lit à
`run-as com.mohamed.miqaat cat shared_prefs/update.xml`.

---

## Éprouver la chaîne sans rien publier

Abaisser temporairement `versionName` à `"1.2.0"` dans `app/build.gradle.kts` et
réinstaller : la comparaison porte sur le **paquet installé**, donc la release
`v1.2.1` déjà en ligne suffit à faire jouer toute la chaîne, téléchargement et
installation compris.

⚠ L'essai doit partir d'un APK **signé release**, pas d'un build debug : une clé de
signature différente rend l'installation par-dessus impossible, et l'on passerait la
soirée à déboguer la mauvaise chose (piège déjà rencontré en session 11).

---

## Le jour de Google Play

Tout ce dispositif disparaît. À supprimer : `Screen.UPDATE`, `ui/update/`,
`data/update/`, `domain/update/`, le `<provider>` du manifeste,
`REQUEST_INSTALL_PACKAGES`, `res/xml/file_paths.xml`, les clés `update_*` des trois
`strings.xml`, ce fichier — et l'amendement de D41 revient à sa formulation d'origine.
