# Publier une release

Cible actuelle : **release GitHub** (APK signé joint à un tag). Pas de Play Store
pour l'instant — voir « Si un jour Google Play » en bas de page.

## 1. La clé de signature (une fois pour toutes)

Android n'accepte une mise à jour que si elle est signée par **la même clé** que
la version installée. Perdre le `.jks`, c'est ne plus jamais pouvoir mettre à jour
l'application pour ceux qui l'ont installée : la sauvegarder hors de la machine
fait partie du travail.

```powershell
keytool -genkeypair -v -keystore miqaat-release.jks -alias miqaat `
  -keyalg RSA -keysize 4096 -validity 10000
```

Puis copier `keystore.properties.example` en `keystore.properties` et le compléter.
Les deux fichiers (`*.jks`, `keystore.properties`) sont **gitignorés** ; aucun mot de
passe ne doit entrer dans le dépôt.

Le module lit ce fichier s'il existe (voir `app/build.gradle.kts`). S'il est absent,
`assembleRelease` produit quand même un APK, mais **non signé** : le projet reste
compilable par quiconque le clone.

## 2. Avant de taguer

```powershell
.\gradlew.bat :app:testDebugUnitTest   # doit être vert
.\gradlew.bat :app:assembleRelease
```

- `versionCode` **doit** augmenter à chaque release publiée (`app/build.gradle.kts`) ;
  `versionName` est ce que l'utilisateur lit, et doit correspondre au tag.
- Vérifier l'APK sur un appareil réel avant publication — la build release n'est pas
  la build debug : `debuggable = false`, et la signature change.

Sortie : `app/build/outputs/apk/release/app-release.apk`. Renommer à la copie en
`miqaat-<version>.apk` pour que le fichier téléchargé se comprenne seul.

Contrôle de la signature :

```powershell
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

## 3. Le tag et la release

```powershell
git tag -a v1.0 -m "Miqaat 1.0"
git push origin v1.0
```

Puis, sur GitHub → *Releases* → *Draft a new release* : choisir le tag, joindre
l'APK renommé, coller les notes de version. (Ou `gh release create v1.0 miqaat-1.0.apk`
si le CLI GitHub est installé.)

Les notes doivent dire, au minimum : ce que fait l'application, ce qu'elle
n'envoie nulle part, Android minimum requis (**8.0 / API 26**), et l'empreinte
SHA-256 de l'APK — c'est la seule façon pour quelqu'un de vérifier que le fichier
téléchargé est bien le nôtre :

```powershell
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

## Si un jour Google Play

Trois points à préparer, dans cet ordre de risque :

1. **`USE_EXACT_ALARM`** : Play la réserve aux applications dont l'alarme est la
   fonction principale (réveil, agenda). Une application de prière peut passer,
   mais la déclaration doit être argumentée — et un refus imposerait de retomber
   sur `SCHEDULE_EXACT_ALARM` avec demande à l'utilisateur.
2. **Politique de confidentialité** hébergée à une URL publique, obligatoire même
   quand l'application ne collecte rien (c'est justement ce qu'elle doit dire).
3. Un **AAB** (`bundleRelease`) et non un APK, avec Play App Signing : la clé
   ci-dessus devient alors la clé d'*upload*.
