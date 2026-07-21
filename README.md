# OtakuPulse Companion

Anime-Auswahl per Wischen — gegen die Entscheidungsschwäche vor dem Abend.

Karten nach **links** wegwischen (kein Interesse), nach **rechts** (auf die Watchlist)
oder nach **oben** (*Super-Swipe*: alle in der Party bekommen eine Push-Nachricht).
Wischen zwei Party-Mitglieder denselben Titel nach rechts, entsteht ein **Match** — die
gemeinsame Liste füllt sich also von allein mit „das wollen beide sehen".

Die App ergänzt [otakupulse.de](https://www.otakupulse.de) um genau die Nutzer-Features,
die dort bewusst fehlen: Watchlist, Gesehen-Markierungen, Simulcast-Kalender und
Benachrichtigungen bei neuen Folgen.

## Was drin ist

| Bereich | Funktion |
|---|---|
| **Entdecken** | Kartenstapel mit Filter für Sync (Ger Dub/Sub), Typ, Status, Genre und über 350 Tags |
| **Watchlist** | Vier Status-Reiter, Folgenfortschritt, Abzeichen bei neuer Folge |
| **Kalender** | Simulcast-Wochenansicht nach Tagen, „Heute" und „Morgen" benannt |
| **Party** | Beitritt per Code, Matches mit Cover, umbenennen · verlassen · löschen |
| **Meldungen** | Empfangene Super-Swipes und Folgen-Hinweise, bleiben nach dem Wegwischen erhalten |
| **Einstellungen** | Theme (System/Hell/Dunkel), Serveradresse |

## Aufbau

```
android/    Kotlin, Compose M3, Room, WorkManager, FCM
server/     Python, FastAPI, SQLAlchemy — Container hinter app.otakupulse.de
docs/       Design-Spec, Firebase-Anleitung, PDF-Erzeugung
dist/       Fertige APK zum Installieren
```

Die Anime-Daten kommen aus der bestehenden **OtakuPulse-Postgres** (~11.000 Titel mit
deutschen Beschreibungen, Tags nach Kategorie, Ger-Dub/Sub, Streaming-Anbieter). Das
Backend liest sie über eine eigene Rolle mit ausschließlich `SELECT`-Recht; die
Ausstrahlungstermine des Kalenders stehen dort ebenfalls schon und werden vom täglichen
Website-Sync frisch gehalten. AniList wird nur als Rückfallebene für fehlende Titel
angefragt.

### Warum es so gebaut ist

Ein paar Entscheidungen, die von außen überraschen könnten:

- **Kein Login.** Die Identität ist ein Geräte-Token, Partys betritt man per Code. Für
  einen geschlossenen Freundeskreis ist alles andere Verwaltungsaufwand ohne Gegenwert.
  Damit ein Gerätewechsel nicht alles kostet, sichert Androids Auto Backup das Token mit.
- **Der Kartenstapel ist selbst gebaut** (`pointerInput` + `detectDragGestures`, rund
  hundert Zeilen). Keine Abhängigkeit von einem Paket, das irgendwann nicht mehr
  gepflegt wird.
- **Mehrere Tags wirken als ODER, nicht UND.** Ein leerer Stapel ist der schlimmste
  Fehlerfall dieser App.
- **Ein Zufalls-Seed pro Sitzung.** Hält die Reihenfolge über nachgeladene Seiten stabil;
  ohne ihn tauchen Karten doppelt auf. Jeder Filterwechsel zieht einen neuen.
- **Swipes wirken zuerst lokal**, erst danach werden sie übertragen. Wischen muss auch
  im Funkloch funktionieren.
- **Löschen einer Party darf nur, wer sie angelegt hat.** Sonst könnte ein einzelnes
  Mitglied die gesammelten Matches aller anderen wegwerfen. Umbenennen darf jeder — es
  ist eine gemeinsame Liste, kein Besitz.

## Backend

Läuft als Container `otakupulse-companion` im Docker-Netz `otakupulse-net`, Host-Port
**3005**, öffentlich erreichbar unter **app.otakupulse.de** über den bestehenden
Cloudflare-Tunnel. Kein VPN, kein offener Port am Router.

```bash
cd /mnt/cache/appdata/otakupulse-companion/server
./manage.sh rebuild     # bauen und starten
./manage.sh logs        # Logs verfolgen
./manage.sh test        # Tests im Container
```

Der Unraid-Host hat **kein** compose-Plugin — `manage.sh` nutzt deshalb plain `docker run`,
wie die übrigen Dienste im Homelab.

### Endpunkte

Alles außer `/health` und der Geräte-Registrierung verlangt `Authorization: Bearer <token>`.

| Methode | Pfad | Zweck |
|---|---|---|
| `GET` | `/health` | Erreichbarkeit, einzige offene Route neben der Registrierung |
| `POST` | `/v1/devices` | Gerät registrieren, gibt das Token zurück |
| `PATCH` | `/v1/devices/me` | Anzeigename und FCM-Token aktualisieren |
| `GET` | `/v1/deck` | Swipe-Stapel; gewischte Titel fallen serverseitig heraus |
| `POST` | `/v1/swipes` | Swipes entgegennehmen, Matches anlegen, Super-Swipe verschicken |
| `GET` | `/v1/filters` | Genres und Tags für das Filterblatt |
| `GET` | `/v1/anime/{id}` | Einzelner Titel für die Detailansicht |
| `GET` | `/v1/airing` | Ausstrahlungstermine für Kalender und Folgen-Prüfung |
| `GET` `POST` | `/v1/parties`, `/v1/parties/join` | Partys auflisten, anlegen, beitreten |
| `PATCH` `DELETE` | `/v1/parties/{id}` | Umbenennen, löschen |
| `POST` | `/v1/parties/{id}/leave` | Verlassen |

Weil der Dienst öffentlich steht: Drosselung je Token und IP (240 Anfragen/Minute) sowie
streng bei der Registrierung (5 pro Stunde und IP — ohne Grenze könnte jeder beliebig
viele Tokens erzeugen und damit den Ausschluss bereits gewischter Titel umgehen),
gedeckelte Seitengröße,
keinerlei Schreibrechte auf die OtakuPulse-Daten, kein Admin-Bereich. Wer nicht Mitglied
einer Party ist, bekommt `404` statt `403` — sonst ließen sich fremde IDs durchprobieren.

## Android

Kein Emulator auf dem Host: es laufen nur Build und JVM-Unit-Tests, getestet wird auf
echter Hardware.

```bash
source /mnt/cache/appdata/android-build/env.sh
cd android
"$GRADLE" :app:assembleDebug
"$GRADLE" :app:testDebugUnitTest
```

Der **Debug-Build** spricht `http://192.168.0.161:3005` und bringt dafür ein eigenes
`src/debug/AndroidManifest.xml` mit `usesCleartextTraffic` mit — Android blockt Klartext
seit API 28 stillschweigend. Die Adresse ist zur Laufzeit umstellbar (Einstellungen →
Server), es braucht also keinen neuen Build, um zwischen WLAN, Tailscale und der
öffentlichen Domain zu wechseln.

Paketname `de.pattaku.otakupulse.app`, minSdk 26, targetSdk 35, manuelle
Abhängigkeitsverdrahtung über `AppContainer` — dieselbe Bauweise wie WorkTracker.

### Gestaltung

Farben, Formen und Typenskala folgen der Website: Zinc-Palette, Akzent Pink `#ff4d6d`
plus Violett `#8b5cf6`, `rounded-xl` als mittlerer Radius. Hell und dunkel sind
eigenständig gestaltet, nicht invertiert — im hellen Modus ist das Marken-Pink zu
`#d11f45` nachgedunkelt, weil es auf Weiß sonst nur 3,1∶1 erreicht.

Alle Animationen prüfen `ANIMATOR_DURATION_SCALE` und schalten sich ab, wenn das System
„Animationen entfernen" gesetzt hat.

## Tests

```bash
./manage.sh test                    # Backend, 51 Tests
"$GRADLE" :app:testDebugUnitTest    # App, 21 Tests
```

Geprüft wird das, was wirklich brechen kann und ohne Gerät prüfbar ist: der Bau der
Deck-Abfrage aus Filtern, die Match-Logik samt Nachtrag bei spätem Beitritt, wann ein
FCM-Token als tot gilt, das gleitende Zeitfenster der Drosselung, die Nachladelogik des
Stapels, die Tagesgruppierung des Kalenders über Zeitzonen hinweg und die Normalisierung
der Serveradresse.

## Einrichtung

Vollständige Firebase-Anleitung: [`docs/FIREBASE.md`](docs/FIREBASE.md).
Der abgestimmte Entwurf liegt unter [`docs/superpowers/specs/`](docs/superpowers/specs/).

Geheimnisse liegen außerhalb der Versionsverwaltung und sind in `.gitignore`
ausgeschlossen:

| Datei | Inhalt |
|---|---|
| `server/secrets.env` | Datenbank-Zugänge, FCM-Projekt-ID |
| `server/secrets/firebase-service-account.json` | Schlüssel zum Versenden von Push-Nachrichten |
| `android/app/google-services.json` | Firebase-Konfiguration der App |
| `android/keystore.properties`, `*.jks` | Release-Signatur (gehört nach `/mnt/cache/appdata/android-build/keystore/`) |

## Stolpersteine

Dinge, die schon einmal Zeit gekostet haben:

- **`/root` ist auf Unraid eine RAM-Disk.** Das Projekt liegt deshalb unter
  `/mnt/cache/appdata/otakupulse-companion`, `/root/otakupulse-app` ist nur ein Symlink.
- **MIUI/HyperOS meldet gelegentlich „App wurde nicht installiert".** Ein Geräte-Neustart
  hilft, nicht die Signatur.
- **`google-auth` erneuert seine Token über den `requests`-Transport.** Ohne das Paket
  scheitert jede Erneuerung, obwohl `google-auth` selbst installiert ist.
- **FCM meldet ein kaputtes Token mit `400`, nicht mit `404`.** Blind bei jedem `400`
  aufzuräumen wäre allerdings falsch — das kommt auch bei fehlerhaftem Nachrichtenaufbau
  und würde reihenweise gültige Tokens löschen. Deshalb entscheidet der Grund.
- **SQLite vergibt IDs nur bei `INTEGER PRIMARY KEY` automatisch, nicht bei `BIGINT`.**
  Ohne `with_variant` scheitern die Tests, obwohl der Betrieb auf Postgres läuft.
- **Ein Compose-Bildschirm außerhalb des `Scaffold` hat keinen Hintergrund.** Dann
  scheint der XML-Fensterhintergrund durch; deshalb umschließt ein `Surface` die App.
- **AniList lässt nur rund 30 Anfragen pro Minute zu.** Das Backend drosselt zentral und
  respektiert `Retry-After`.

## Stand

Fertig: Kartenstapel, Filter, Watchlist, Kalender, Partys mit Matches, Super-Swipe per
Push, Meldungsbereich, Theme-Umschalter.

Offen: Auto Backup und JSON-Export/Import.
