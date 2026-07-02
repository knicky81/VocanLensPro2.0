# VocabLens Pro – zuerst lesen

Wichtig:
Der Ordner `.github` ist absichtlich vorhanden. Manche Smartphones und Dateimanager verstecken Ordner, die mit einem Punkt beginnen.

Wenn du `.github` nicht siehst, ist er meistens trotzdem im ZIP enthalten.

## Variante am Rechner, empfohlen

1. ZIP am Rechner entpacken.
2. Auf GitHub ein neues Repository erstellen.
3. Alle Inhalte des entpackten Ordners hochladen:
   - `.github`
   - `app`
   - `build.gradle.kts`
   - `gradle.properties`
   - `settings.gradle.kts`
   - `README.md`
4. In GitHub auf `Actions` gehen.
5. `Build APK` auswählen.
6. `Run workflow` klicken.
7. Nach dem grünen Build unten bei `Artifacts` die APK herunterladen.

## Falls `.github` beim Hochladen fehlt

Im Projekt liegt zusätzlich:
`GITHUB_ACTIONS_VISIBLE_COPY/build-apk.yml`

Dann in GitHub:
1. Ordner `.github` erstellen
2. darin Ordner `workflows` erstellen
3. Datei `build-apk.yml` dort hineinlegen

Der Pfad muss am Ende exakt so sein:
`.github/workflows/build-apk.yml`

## APK auf dem Handy installieren

Nach dem Build:
1. Artifact `VocabLensPro-debug-apk` herunterladen
2. ZIP entpacken
3. `app-debug.apk` aufs Xiaomi schicken
4. öffnen und Installation erlauben
