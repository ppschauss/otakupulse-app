"""Zwei Datenbankverbindungen: der OtakuPulse-Katalog (nur lesen) und die eigene DB."""
from __future__ import annotations

from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from .config import Settings

_settings = Settings.from_env()

# Katalog: fremde Datenbank, eigene Postgres-Rolle mit ausschließlich SELECT-Recht.
# Zusätzlich auf Ebene der Verbindung schreibgeschützt — doppelt hält besser.
catalog_engine = create_engine(
    _settings.catalog_dsn,
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=5,
    execution_options={"postgresql_readonly": True},
)

# Eigene Datenbank: Geräte, Partys, Swipes, Matches, AniList-Cache.
companion_engine = create_engine(_settings.companion_dsn, pool_pre_ping=True, pool_size=5)

CompanionSession = sessionmaker(bind=companion_engine, expire_on_commit=False)


def get_settings() -> Settings:
    return _settings


def get_session() -> Iterator[Session]:
    with CompanionSession() as session:
        yield session
