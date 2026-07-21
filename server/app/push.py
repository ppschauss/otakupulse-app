"""Push-Versand über Firebase Cloud Messaging (HTTP v1)."""
from __future__ import annotations

import logging

import httpx
from sqlalchemy.orm import Session

from .config import Settings
from .models import Device

log = logging.getLogger(__name__)

SCOPE = "https://www.googleapis.com/auth/firebase.messaging"


class Pusher:
    """Verschickt Benachrichtigungen; ohne Schlüssel tut er still nichts.

    Bewusst kein harter Fehler bei fehlender Konfiguration: Partys, Matches und
    Watchlist sollen auch ohne Firebase vollständig funktionieren.
    """

    def __init__(self, settings: Settings) -> None:
        self._project = settings.fcm_project_id
        self._key_file = settings.fcm_credentials_file
        self._credentials = None

    @property
    def configured(self) -> bool:
        return bool(self._project and self._key_file)

    def _token(self) -> str | None:
        if not self.configured:
            return None
        try:
            from google.auth.transport.requests import Request
            from google.oauth2 import service_account

            if self._credentials is None:
                self._credentials = service_account.Credentials.from_service_account_file(
                    self._key_file, scopes=[SCOPE]
                )
            if not self._credentials.valid:
                self._credentials.refresh(Request())
            return self._credentials.token
        except Exception:
            log.exception("FCM-Zugangstoken konnte nicht geholt werden")
            return None

    def send(
        self,
        session: Session,
        empfaenger: list[Device],
        titel: str,
        text: str,
        daten: dict | None = None,
    ) -> int:
        """Verschickt an alle Empfänger mit hinterlegtem FCM-Token.

        Räumt dabei tote Tokens auf: ein 404 oder 403 von FCM heisst, die App wurde
        deinstalliert oder das Token ist abgelaufen. Bleibt es stehen, scheitert
        jeder künftige Versand an derselben Stelle erneut.
        """
        ziele = [d for d in empfaenger if d.fcm_token]
        if not ziele:
            return 0

        access_token = self._token()
        if access_token is None:
            log.info("Push übersprungen — Firebase ist nicht eingerichtet")
            return 0

        url = f"https://fcm.googleapis.com/v1/projects/{self._project}/messages:send"
        headers = {"Authorization": f"Bearer {access_token}"}
        zugestellt = 0
        tot: list[Device] = []

        with httpx.Client(timeout=15) as client:
            for device in ziele:
                payload = {
                    "message": {
                        "token": device.fcm_token,
                        "notification": {"title": titel, "body": text},
                        "data": {k: str(v) for k, v in (daten or {}).items()},
                        "android": {"priority": "HIGH"},
                    }
                }
                try:
                    antwort = client.post(url, json=payload, headers=headers)
                except httpx.HTTPError:
                    log.warning("FCM nicht erreichbar für Gerät %s", device.id)
                    continue

                if antwort.status_code == 200:
                    zugestellt += 1
                elif antwort.status_code in (403, 404):
                    tot.append(device)
                else:
                    log.warning("FCM antwortete %s: %s", antwort.status_code, antwort.text[:200])

        if tot:
            for device in tot:
                device.fcm_token = None
            session.commit()
            log.info("%d tote FCM-Tokens entfernt", len(tot))

        return zugestellt
