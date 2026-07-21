"""Lesender Zugriff auf den OtakuPulse-Katalog."""
from __future__ import annotations

from typing import Any

from sqlalchemy import text

from .db import catalog_engine
from .deck_query import SELECT_COLUMNS, DeckFilter, build_deck_query


def _card(row: Any) -> dict:
    """Formt eine Datenbankzeile zur Swipe-Karte.

    Titel-Vorrang deutsch → englisch → romaji, denn die App ist deutschsprachig.
    Beschreibung deutsch mit Rückfall auf die Originalsprache.
    """
    m = row._mapping
    return {
        "id": m["id"],
        "anilistId": int(m["anilist_id"]) if m["anilist_id"] is not None else None,
        "slug": m["slug"],
        "title": m["title_german"] or m["title_english"] or m["title_romaji"],
        "titleRomaji": m["title_romaji"],
        "description": m["description_de"] or m["description_source"],
        "coverImageUrl": m["cover_image_url"],
        "bannerImageUrl": m["banner_image_url"],
        "format": m["format"],
        "status": m["status"],
        "episodes": int(m["episodes"]) if m["episodes"] is not None else None,
        "season": m["season"],
        "seasonYear": int(m["season_year"]) if m["season_year"] is not None else None,
        "averageScore": int(m["average_score"]) if m["average_score"] is not None else None,
    }


def fetch_deck(f: DeckFilter, max_page_size: int) -> list[dict]:
    sql, params = build_deck_query(f, max_page_size=max_page_size)
    with catalog_engine.connect() as conn:
        rows = conn.execute(text(sql), params).fetchall()
    return [_card(r) for r in rows]


def fetch_genres() -> list[dict]:
    sql = "SELECT slug, name FROM genres ORDER BY name"
    with catalog_engine.connect() as conn:
        return [dict(r._mapping) for r in conn.execute(text(sql))]


def fetch_tags() -> list[dict]:
    """Tags nach Kategorie gruppierbar — genau wie die Filter auf der Website."""
    sql = (
        "SELECT slug, name, category::text AS category FROM tags"
        " WHERE is_adult = false AND category IS NOT NULL"
        " ORDER BY category, name"
    )
    with catalog_engine.connect() as conn:
        return [dict(r._mapping) for r in conn.execute(text(sql))]


def fetch_by_ids(ids: list[int]) -> list[dict]:
    """Karten zu bekannten IDs — für Match-Listen, die sonst nur Zahlen zeigen würden."""
    if not ids:
        return []
    sql = f"SELECT {SELECT_COLUMNS} FROM anime a WHERE a.id = ANY(:ids)"
    with catalog_engine.connect() as conn:
        rows = conn.execute(text(sql), {"ids": ids}).fetchall()
    nach_id = {r._mapping["id"]: _card(r) for r in rows}
    return [nach_id[i] for i in ids if i in nach_id]


AIRING_SQL = """
SELECT
    e.anime_id,
    e.number       AS episode,
    e.air_date,
    a.slug,
    a.title_german,
    a.title_english,
    a.title_romaji,
    a.cover_image_url,
    a.format::text AS format
FROM episodes e
JOIN anime a ON a.id = e.anime_id
WHERE e.air_date >= :von
  AND e.air_date < :bis
  AND a.is_hentai = false
  {nur_ids}
ORDER BY e.air_date, a.id
LIMIT 500
"""


def fetch_airing(von, bis, anime_ids: list[int] | None = None) -> list[dict]:
    """Ausstrahlungstermine aus der OtakuPulse-Datenbank.

    Die Termine stehen dort bereits und werden vom täglichen Sync der Website
    frisch gehalten — AniList muss dafür nicht angefragt werden.
    """
    params: dict = {"von": von, "bis": bis}
    nur_ids = ""
    if anime_ids:
        nur_ids = "AND e.anime_id = ANY(:ids)"
        params["ids"] = anime_ids

    with catalog_engine.connect() as conn:
        rows = conn.execute(text(AIRING_SQL.format(nur_ids=nur_ids)), params).fetchall()

    ergebnis = []
    for r in rows:
        m = r._mapping
        ergebnis.append({
            "animeId": m["anime_id"],
            "episode": int(m["episode"]) if m["episode"] is not None else None,
            "airingAt": m["air_date"].isoformat() if m["air_date"] else None,
            "slug": m["slug"],
            "title": m["title_german"] or m["title_english"] or m["title_romaji"],
            "coverImageUrl": m["cover_image_url"],
            "format": m["format"],
        })
    return ergebnis
