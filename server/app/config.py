"""Konfiguration aus Umgebungsvariablen (secrets.env)."""
from __future__ import annotations

import os
from dataclasses import dataclass


def _require(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Umgebungsvariable {name} fehlt")
    return value


@dataclass(frozen=True)
class Settings:
    # Lesender Zugriff auf die OtakuPulse-Datenbank (eigene Rolle, nur SELECT)
    catalog_dsn: str
    # Eigene Datenbank für Geräte, Partys, Swipes
    companion_dsn: str
    # Firebase-Service-Account-Schlüssel (JSON-Datei) für Push
    fcm_credentials_file: str | None
    fcm_project_id: str | None
    # Wie viele Karten eine Deck-Seite höchstens liefert
    deck_max_page_size: int = 40

    @staticmethod
    def from_env() -> "Settings":
        return Settings(
            catalog_dsn=_require("CATALOG_DSN"),
            companion_dsn=_require("COMPANION_DSN"),
            fcm_credentials_file=os.environ.get("FCM_CREDENTIALS_FILE") or None,
            fcm_project_id=os.environ.get("FCM_PROJECT_ID") or None,
            deck_max_page_size=int(os.environ.get("DECK_MAX_PAGE_SIZE", "40")),
        )
