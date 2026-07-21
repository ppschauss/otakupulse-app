# OtakuPulse Companion

**Anime-Auswahl per Wischen — gegen die halbe Stunde Scrollen, bevor überhaupt etwas läuft.**

Das Problem, das diese App löst, kennt jeder: Man hat Lust auf Anime, öffnet eine
Datenbank, scrollt durch Listen, liest drei Beschreibungen an, findet nichts, macht was
anderes. Die App dreht das um. Statt einer Liste bekommt man **eine Karte**. Ja oder
nein, weiter. Nach zwanzig Karten hat man drei Titel gemerkt und weiß, was man heute
Abend schaut.

Zu zweit oder zu dritt wird daraus ein Spiel: Wischen zwei Leute in derselben Party
denselben Titel nach rechts, entsteht ein **Match** — und die Frage „was gucken wir?"
beantwortet sich von allein.

Die App ergänzt [otakupulse.de](https://www.otakupulse.de) um genau das, was der Website
bewusst fehlt: persönliche Listen, Fortschritt, Erinnerungen.

---

## Die Funktionen im Einzelnen

### Entdecken — der Kartenstapel

Der Hauptbildschirm zeigt eine Karte: Cover, Titel, Jahr, Format, Folgenzahl, Bewertung
und die ersten Zeilen der deutschen Beschreibung. Vier Gesten:

| Geste | Bedeutung |
|---|---|
| **← links** | Kein Interesse. Der Titel taucht nie wieder auf. |
| **→ rechts** | Auf die Watchlist, Status „geplant". |
| **↑ oben** | *Super-Swipe* — alle in deiner Party bekommen eine Push-Nachricht. |
| **Tippen** | Detailansicht. |

Die Karte folgt dem Finger, kippt leicht mit und färbt sich ein: grün für „merken",
rot für „nein", violett für den Super-Swipe. Ab einem Viertel der Bildschirmbreite löst
sie aus. Wer lieber tippt, findet dieselben drei Aktionen als Knöpfe darunter.

Weggewischte Titel schließt der **Server** aus dem nächsten Stapel aus, nicht die App.
Damit bleibt der Ausschluss auch dann bestehen, wenn die App neu installiert wird.

### Filter — was überhaupt im Stapel landet

Über das Filtersymbol oben rechts, mit einem Abzeichen, wie viele Bereiche gerade
eingeschränkt sind:

- **Sync** — Deutsche Synchro (1.270 Titel), Deutscher Sub (2.831), Englische Synchro,
  Englischer Sub, Japanisch. Genau der Filter, der sonst nirgends sauber funktioniert.
- **Typ** — Serie, Kurzserie, Film, OVA, ONA, Special.
- **Status** — Läuft gerade, Abgeschlossen, Angekündigt.
- **Genre** — alle 19 als antippbare Chips.
- **Tag** — über 350 Stück, deshalb mit Suchfeld statt Liste. Gewählte Tags bleiben
  immer sichtbar, auch wenn das Suchfeld leer ist; sonst könnte man sie nicht mehr
  abwählen.

Mehrere Tags wirken als **ODER**, nicht UND. „Isekai" plus „Magie" erweitert den Stapel,
statt ihn auf die Schnittmenge einzudampfen — ein leerer Stapel ist der schlimmste
Fehlerfall dieser App. Die Einstellung bleibt über Neustarts erhalten.

### Detailansicht

Beim Antippen einer Karte, ebenso aus Watchlist und Meldungen. Zeigt alles, was zur
Entscheidung beiträgt:

Banner mit Titel und Bewertung · Format, Folgenzahl, Folgenlänge, Status · Season und
Jahr · vollständige deutsche Beschreibung · **Genres** · **Verfügbar als** (Ger Dub,
Ger Sub, Eng Dub …) · **Wo streamen** (Crunchyroll, Netflix, RTL+ …) · **Studios**
(Animationsstudios hervorgehoben, Produktion und Vertrieb kleiner darunter) · die
**zwanzig treffendsten Tags** mit Übereinstimmung in Prozent · **verwandte Titel**
(Vorgänger und Fortsetzungen zuerst, direkt antippbar) · **offizielle Links**.

Charaktere, Stab und Sprecher bleiben bewusst draußen — bei rund 29.000 Charakteren und
28.000 Personen wäre das eine eigene Anwendung, keine Ergänzung.

### Watchlist

Vier Reiter mit Anzahl: **Am Schauen · Geplant · Fertig · Abgebrochen**. Je Titel Cover,
Fortschritt („Folge 7/12"), Statuswechsel per Knopf und ein `+1` zum Abhaken der
gerade gesehenen Folge. Ist die letzte Folge abgehakt, wandert der Titel automatisch
auf „Fertig".

Ein rotes **„neu"** neben dem Titel bedeutet: seit deinem letzten Blick ist eine Folge
erschienen.

Die Watchlist liegt vollständig auf dem Gerät und funktioniert **ohne Netz** — Titel,
Cover und Folgenzahl werden mitgespeichert, nicht nur die ID.

### Kalender

Die Simulcast-Woche, nach Tagen gruppiert. Heute und morgen heißen „Heute" und „Morgen",
alles danach trägt Wochentag und Datum. Je Folge Uhrzeit, Cover, Titel und Folgennummer.

Die Termine stammen aus der OtakuPulse-Datenbank und werden vom täglichen Website-Sync
frisch gehalten. Aktuell liegen dort rund 930 künftige Ausstrahlungen über 121 Titel.

### Party

Eine Party anlegen, den **sechsstelligen Code** vorlesen, Freunde treten damit bei. Auf
der Party-Karte stehen Name, Code, Mitglieder und alle **Matches mit Cover**.

Ein Match entsteht, sobald zwei Mitglieder denselben Titel nach rechts wischen. Wer
später beitritt, bekommt seine Matches **rückwirkend** nachgetragen — sonst entstünden
sie nie, weil niemand denselben Titel zweimal wischt.

Über das Menü auf der Karte: **Umbenennen** (darf jedes Mitglied — es ist eine gemeinsame
Liste, kein Besitz), **Verlassen**, **Löschen** (nur wer sie angelegt hat; sonst könnte
einer die gesammelten Matches aller anderen wegwerfen).

Bist du in **mehreren** Partys, fragt der Super-Swipe nach, an welche er geht. Bei genau
einer Party geht er direkt raus — die Frage wäre dort nur ein zusätzlicher Klick.

### Meldungen

Android-Benachrichtigungen sind flüchtig: einmal weggewischt, sind sie weg. Wer nachts
einen Super-Swipe bekommt, soll morgens noch sehen können, worum es ging.

Deshalb sammelt die App empfangene Super-Swipes und Folgen-Hinweise in einer eigenen
Liste, mit Zeitstempel und Symbol. Ungelesene erscheinen als Zahl an der
Navigationsleiste; Antippen öffnet den Anime.

### Einstellungen

**Darstellung** — System, Hell oder Dunkel.

**Sicherung** — Watchlist als JSON-Datei exportieren und wieder einlesen, über den
System-Dateidialog. Der Import ist ein **Zusammenführen**, kein Ersetzen: wer
zwischenzeitlich weitergewischt hat, verliert nichts. Bei Titeln, die es beidseits gibt,
gewinnt der höhere Fortschritt — man hat eher mehr geschaut als weniger. Das
Geräte-Token wandert mit in die Datei, sonst wäre man nach einem Gerätewechsel aus allen
Partys verschwunden; es wird beim Import aber nur übernommen, wenn noch keines vorhanden
ist.

Zusätzlich sichert Androids **Auto Backup** die Datenbank ohne Zutun in dein Google Drive.

---

## Aufbau

```
android/    Kotlin, Compose M3, Room, WorkManager, FCM
server/     Python, FastAPI, SQLAlchemy — Container hinter app.otakupulse.de
docs/       Design-Spec, Firebase-Anleitung, PDF-Erzeugung
dist/       Fertige APK zum Installieren
```

Die Anime-Daten kommen aus der bestehenden **OtakuPulse-Postgres**: rund 11.000 Titel mit
deutschen Beschreibungen, Tags nach Kategorie, Ger-Dub/Sub-Angaben und Streaming-Anbietern.
Das Backend liest sie über eine eigene Rolle mit ausschließlich `SELECT`-Recht; auch die
Ausstrahlungstermine des Kalenders stehen dort schon. AniList wird nur als Rückfallebene
für fehlende Titel angefragt.

### Warum es so gebaut ist

Entscheidungen, die von außen überraschen könnten:

- **Kein Login, keine Konten.** Die Identität ist ein Geräte-Token, Partys betritt man per
  Code. Für einen geschlossenen Freundeskreis wäre alles andere Verwaltungsaufwand ohne
  Gegenwert. Damit ein Gerätewechsel nicht alles kostet, wandert das Token in die
  Sicherung und wird von Auto Backup mitgenommen.
- **Nur eine Serveradresse.** Früher war sie in der App einstellbar (LAN, Tailscale,
  öffentlich). Das war eine Einstellung, die man falsch stellen konnte, für einen
  Vorteil, den der Cloudflare-Tunnel ohnehin liefert. Jetzt fest `app.otakupulse.de`,
  verschlüsselt, von überall.
- **Der Kartenstapel ist selbst gebaut** (`pointerInput` + `detectDragGestures`, rund
  hundert Zeilen), statt eine Fremdbibliothek einzubinden, die irgendwann nicht mehr
  gepflegt wird.
- **Ein Zufalls-Seed pro Sitzung** hält die Reihenfolge über nachgeladene Seiten stabil.
  Ohne ihn tauchen beim Nachladen Karten doppelt auf. Jeder Filterwechsel zieht einen neuen.
- **Swipes wirken zuerst lokal**, erst danach werden sie übertragen. Wischen muss auch im
  Funkloch funktionieren; ein Hintergrund-Job schiebt den Puffer nach und räumt nur weg,
  was der Server wirklich bestätigt hat.
- **Kalender und „neu"-Abzeichen speisen sich aus derselben Prüfung.** Getrennte Wege
  würden über kurz oder lang auseinanderlaufen.

---

## Backend

Läuft als Container `otakupulse-companion` im Docker-Netz `otakupulse-net`, Host-Port
**3005**, öffentlich unter **app.otakupulse.de** über den bestehenden Cloudflare-Tunnel.
Kein VPN, kein offener Port am Router.

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
| `GET` | `/health` | Erreichbarkeit; einzige offene Route neben der Registrierung |
| `POST` | `/v1/devices` | Gerät registrieren, gibt das Token zurück |
| `PATCH` | `/v1/devices/me` | Anzeigename und FCM-Token aktualisieren |
| `GET` | `/v1/deck` | Swipe-Stapel; gewischte Titel fallen serverseitig heraus |
| `POST` | `/v1/swipes` | Swipes entgegennehmen, Matches anlegen, Super-Swipe verschicken |
| `GET` | `/v1/filters` | Genres und Tags für das Filterblatt |
| `GET` | `/v1/anime/{id}` | Vollständige Detailansicht in einer Antwort |
| `GET` | `/v1/airing` | Ausstrahlungstermine für Kalender und Folgen-Prüfung |
| `GET` `POST` | `/v1/parties`, `/v1/parties/join` | Partys auflisten, anlegen, beitreten |
| `PATCH` `DELETE` | `/v1/parties/{id}` | Umbenennen, löschen |
| `POST` | `/v1/parties/{id}/leave` | Verlassen |

### Absicherung

Der Dienst steht öffentlich im Internet, deshalb:

- **Drosselung** mit gleitendem Zeitfenster: 240 Anfragen pro Minute je Token *und* je IP
  (getrennt gezählt, weil sich mehrere Geräte oft eine IP teilen), und **5 Registrierungen
  pro Stunde und IP**. Ohne die letzte Grenze könnte jeder beliebig viele Tokens erzeugen
  und damit den Ausschluss bereits gewischter Titel umgehen.
- **Gedeckelte Seitengröße**, damit die App kein bequemer Abzug der SEO-Datenbank wird.
- **Keinerlei Schreibrechte** auf die OtakuPulse-Daten, kein Admin-Bereich.
- Wer nicht Mitglied einer Party ist, bekommt **`404` statt `403`** — sonst ließen sich
  fremde IDs durchprobieren.
- Fremde Party-IDs in der Super-Swipe-Auswahl werden still verworfen.

---

## Android

Kein Emulator auf dem Host: es laufen nur Build und JVM-Unit-Tests, getestet wird auf
echter Hardware.

```bash
source /mnt/cache/appdata/android-build/env.sh
cd android
"$GRADLE" :app:assembleDebug
"$GRADLE" :app:testDebugUnitTest
```

Paketname `de.pattaku.otakupulse.app`, minSdk 26, targetSdk 35, manuelle
Abhängigkeitsverdrahtung über `AppContainer` — dieselbe Bauweise wie WorkTracker.
Fünf Navigationsziele plus Einstellungen in der Kopfleiste; Material erlaubt drei bis
fünf Ziele, sechs wären ein Verstoß.

### Gestaltung

Farben, Formen und Typenskala folgen der Website: Zinc-Palette, Akzent Pink `#ff4d6d`
plus Violett `#8b5cf6`, `rounded-xl` als mittlerer Radius. Hell und dunkel sind
eigenständig gestaltet, nicht invertiert — im hellen Modus ist das Marken-Pink zu
`#d11f45` nachgedunkelt, weil es auf Weiß sonst nur 3,1∶1 erreicht und als Textfarbe
durchfiele.

Beim Start läuft ein kurzer Auftakt: ein Puls-Ring im Markenverlauf, dann fährt der
Schriftzug ein. Rund 1,4 Sekunden, nur beim Kaltstart. Ladezustände zeigen entweder drei
pulsierende Punkte oder — im Kartenstapel — eine schimmernde Platzhalterfläche in
Kartenform; ein Kreisel mitten im Inhalt verschweigt, was gleich kommt.

Alle Animationen prüfen `ANIMATOR_DURATION_SCALE` und schalten sich ab, wenn das System
„Animationen entfernen" gesetzt hat.

---

## Tests

```bash
./manage.sh test                    # Backend, 51 Tests
"$GRADLE" :app:testDebugUnitTest    # App, 15 Tests
```

Geprüft wird, was wirklich brechen kann und ohne Gerät prüfbar ist: der Bau der
Deck-Abfrage aus Filtern samt Schutz vor eingeschleustem SQL, die Match-Logik
einschließlich Nachtrag bei spätem Beitritt, wann ein FCM-Token als tot gilt, das
gleitende Zeitfenster der Drosselung, die Nachladelogik des Stapels und die
Tagesgruppierung des Kalenders über Zeitzonen hinweg.

---

## Einrichtung

Firebase (nur für Super-Swipe-Benachrichtigungen nötig):
[`docs/FIREBASE.md`](docs/FIREBASE.md). Der abgestimmte Entwurf liegt unter
[`docs/superpowers/specs/`](docs/superpowers/specs/).

Geheimnisse sind in `.gitignore` ausgeschlossen:

| Datei | Inhalt |
|---|---|
| `server/secrets.env` | Datenbank-Zugänge, FCM-Projekt-ID |
| `server/secrets/firebase-service-account.json` | Schlüssel zum Versenden von Push-Nachrichten |
| `android/app/google-services.json` | Firebase-Konfiguration der App |
| `android/keystore.properties`, `*.jks` | Release-Signatur (gehört nach `/mnt/cache/appdata/android-build/keystore/`) |

---

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
  scheint der XML-Fensterhintergrund durch — beim Plattform-Theme ein flaches `#303030`.
  Deshalb umschließt ein `Surface` die gesamte App.
- **Room-Migrationen niemals zerstörend.** Die Watchlist ist die einzige Sammlung, die
  der Nutzer selbst aufgebaut hat, und sie liegt nirgendwo sonst.
- **AniList lässt nur rund 30 Anfragen pro Minute zu.** Das Backend drosselt zentral und
  respektiert `Retry-After`.

---

## Stand

Vollständig: Kartenstapel mit Filtern, Detailansicht, Watchlist, Kalender, Partys mit
Matches, Super-Swipe per Push, Meldungsbereich, Theme-Umschalter, Sicherung per Datei.
