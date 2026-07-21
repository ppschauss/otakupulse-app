# Firebase einrichten (für Super-Swipe-Benachrichtigungen)

Nur dafür nötig: Wenn jemand in der Party eine Karte nach oben wischt, sollen die
anderen sofort eine Benachrichtigung bekommen — auch bei geschlossener App. Das geht
auf Android ausschließlich über Firebase Cloud Messaging (FCM).

Alles andere (Partys, Matches, Watchlist, Kalender) läuft ohne Firebase.

Kosten: keine. FCM ist im kostenlosen Spark-Tarif enthalten, es wird keine Kreditkarte
verlangt.

## 1. Projekt anlegen

1. <https://console.firebase.google.com> → **Projekt hinzufügen**
2. Name: `otakupulse-companion` (beliebig)
3. Google Analytics: **aus** — wird nicht gebraucht und spart die Einwilligungsfragen

## 2. Android-App registrieren

Im Projekt auf das Android-Symbol klicken:

| Feld | Wert |
|---|---|
| Paketname | `de.pattaku.otakupulse.app` |
| App-Name | OtakuPulse Companion |
| SHA-1 | leer lassen — nur für Google-Anmeldung nötig, die gibt es hier nicht |

**Der Paketname muss exakt stimmen**, sonst weist FCM die Registrierung des Geräts ab.

Danach `google-services.json` herunterladen und ablegen unter:

```
/mnt/cache/appdata/otakupulse-companion/android/app/google-services.json
```

Die Datei ist in `.gitignore` ausgeschlossen und landet nicht auf GitHub.

## 3. Service-Account-Schlüssel fürs Backend

Das Backend muss die Nachrichten verschicken dürfen:

1. In der Firebase-Konsole: **Zahnrad → Projekteinstellungen → Dienstkonten**
2. **Neuen privaten Schlüssel generieren** → JSON-Datei wird heruntergeladen
3. Ablegen unter:

```
/mnt/cache/appdata/otakupulse-companion/server/secrets/firebase-service-account.json
```

Der Ordner `secrets/` ist bereits als schreibgeschütztes Volume in den Container
eingebunden (`/secrets`), und der Pfad steht schon in `secrets.env`.

## 4. Projekt-ID eintragen

In `server/secrets.env` die Zeile ergänzen — die ID steht in der heruntergeladenen
JSON-Datei unter `project_id`:

```
FCM_PROJECT_ID=otakupulse-companion-1234
```

Danach:

```bash
cd /mnt/cache/appdata/otakupulse-companion/server
./manage.sh rebuild
```

## 5. Prüfen

Zwei Geräte in dieselbe Party, auf dem einen eine Karte nach oben wischen — auf dem
anderen muss die Benachrichtigung erscheinen, auch wenn die App dort geschlossen ist.

## Sicherheit

Beide Dateien sind Geheimnisse und gitignored. Der Service-Account-Schlüssel erlaubt
das Versenden von Nachrichten an alle Geräte des Projekts — er gehört nicht in ein
Repo, auch nicht in ein privates.
