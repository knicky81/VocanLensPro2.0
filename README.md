# VocabLens Pro – GitHub Ready

Diese Version ist dafür vorbereitet, direkt über GitHub Actions eine APK zu bauen.

## Funktionen

- Kamera-Import
- Galerie-Import
- OCR mit Google ML Kit
- automatische 3-Spalten-Erkennung:
  Englisch | Beispielsatz | Deutsch
- Editor zum Prüfen und Korrigieren der erkannten Vokabeln
- Listen speichern/laden
- Lernrichtung EN → DE und DE → EN
- Zufallsabfrage: 10 / 20 / 30 / alle
- Fehlerwörter bevorzugen
- Nur falsche Wörter lernen
- tolerante Antwortprüfung
- Text-to-Speech für englische Wörter
- einfache Statistik

## GitHub APK-Build

1. ZIP entpacken.
2. Inhalt in ein neues GitHub-Repository hochladen.
3. In GitHub auf `Actions` klicken.
4. Workflow `Build APK` auswählen.
5. `Run workflow` klicken.
6. Nach dem Lauf unten bei `Artifacts` die APK herunterladen.

Die APK heißt im Artifact:
`app-debug.apk`

## Hinweis für Buchfotos

Wenn eine Seite sehr viel anderen Text enthält, fotografiere möglichst nah an der Vokabelliste oder schneide den Vokabelbereich vorher im Handy zu. Danach importierst du das zugeschnittene Bild in VocabLens Pro.
