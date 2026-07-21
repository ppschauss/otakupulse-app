# OtakuPulse Companion

Anime-Discovery per Swipe — Android-App plus Backend. Karten nach links (kein Interesse),
nach rechts (auf die Watchlist) oder nach oben (*Super-Swipe*: alle in der Party bekommen
eine Push-Benachrichtigung). Wischen zwei Party-Mitglieder denselben Titel nach rechts,
entsteht ein **Match**.

Ergänzt die Website [otakupulse.de](https://www.otakupulse.de) um genau die Nutzer-Features,
die dort bewusst fehlen: Watchlist, Gesehen-Markierungen, Simulcast-Kalender und
Benachrichtigungen bei neuen Folgen.

## Aufbau

| Ordner | Inhalt |
|---|---|
| `android/` | Native App — Kotlin, Compose M3, Room, WorkManager, FCM |
| `server/` | Backend — Python/FastAPI, läuft als Container unter `app.otakupulse.de` |
| `docs/` | Design-Spec und Betriebsnotizen |

Die Anime-Daten kommen aus der bestehenden OtakuPulse-Postgres (deutsche Beschreibungen,
Tags, Ger-Dub/Sub, Streaming-Anbieter); fehlende Titel lädt das Backend live von AniList nach.

## Entwicklung

**Backend**

```bash
cd /mnt/cache/appdata/otakupulse-companion
./manage.sh rebuild     # bauen und starten
./manage.sh logs        # Logs verfolgen
```

**Android** — der Unraid-Host hat keinen Emulator, es laufen nur Build und JVM-Unit-Tests.
Getestet wird auf echter Hardware.

```bash
source /mnt/cache/appdata/android-build/env.sh
cd android
"$GRADLE" :app:assembleDebug
"$GRADLE" :app:testDebugUnitTest
```

## Geheimnisse

`secrets.env`, `google-services.json`, der Firebase-Service-Account-Schlüssel und der
Release-Keystore liegen **außerhalb** des Repos beziehungsweise sind in `.gitignore`
ausgeschlossen. Der Keystore gehört nach `/mnt/cache/appdata/android-build/keystore/`.
