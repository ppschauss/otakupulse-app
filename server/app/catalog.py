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


def fetch_detail(anime_id: int) -> dict | None:
    """Vollständige Detailansicht: Karte plus Genres, Tags, Studios, Sprachen, Anbieter, Verwandtes.

    Alles in einem Rutsch statt in mehreren Abfragen — die Detailansicht öffnet sich
    beim Antippen einer Karte, da zählt jede Millisekunde.
    Charaktere, Stab und Sprecher bleiben bewusst draußen: bei ~29.000 Charakteren
    und 28.000 Personen wäre das eine eigene Seite, keine Ergänzung.
    """
    karten = fetch_by_ids([anime_id])
    if not karten:
        return None
    detail = karten[0]

    with catalog_engine.connect() as conn:
        detail["genres"] = [
            r[0] for r in conn.execute(
                text(
                    "SELECT g.name FROM anime_rels r JOIN genres g ON g.id = r.genres_id"
                    " WHERE r.parent_id = :id ORDER BY r.\"order\""
                ),
                {"id": anime_id},
            )
        ]

        # Nach Rang sortiert: die zutreffendsten Tags zuerst, das ist die
        # Reihenfolge, die AniList selbst verwendet.
        detail["tags"] = [
            {"name": r[0], "category": r[1], "rank": int(r[2]) if r[2] is not None else None}
            for r in conn.execute(
                text(
                    "SELECT t.name, t.category::text, at.rank"
                    " FROM anime_tags at JOIN tags t ON t.id = at.tag_id"
                    " WHERE at._parent_id = :id AND t.is_adult = false"
                    " ORDER BY at.rank DESC NULLS LAST LIMIT 20"
                ),
                {"id": anime_id},
            )
        ]

        detail["studios"] = [
            {"name": r[0], "isAnimation": bool(r[1])}
            for r in conn.execute(
                text(
                    "SELECT s.name, s.is_animation_studio FROM anime_rels r"
                    " JOIN studios s ON s.id = r.studios_id"
                    " WHERE r.parent_id = :id ORDER BY s.is_animation_studio DESC NULLS LAST"
                ),
                {"id": anime_id},
            )
        ]

        detail["languages"] = [
            r[0] for r in conn.execute(
                text(
                    "SELECT value::text FROM anime_available_languages"
                    " WHERE parent_id = :id ORDER BY \"order\""
                ),
                {"id": anime_id},
            )
        ]

        detail["providers"] = [
            r[0] for r in conn.execute(
                text(
                    "SELECT value::text FROM anime_streaming_providers"
                    " WHERE parent_id = :id ORDER BY \"order\""
                ),
                {"id": anime_id},
            )
        ]

        detail["links"] = [
            {"platform": r[0], "url": r[1]}
            for r in conn.execute(
                text(
                    "SELECT platform, url FROM anime_external_links"
                    " WHERE _parent_id = :id ORDER BY _order"
                ),
                {"id": anime_id},
            )
        ]

        # Verwandte Titel: Vorgänger und Fortsetzungen zuerst, das ist die
        # Frage, die man vor dem Anschauen wirklich hat.
        verwandt = [
            {"animeId": r[0], "relation": r[1]}
            for r in conn.execute(
                text(
                    "SELECT ar.anime_id, ar.relation_type::text FROM anime_relations ar"
                    " WHERE ar._parent_id = :id AND ar.anime_id IS NOT NULL"
                    " ORDER BY CASE ar.relation_type::text"
                    "   WHEN 'PREQUEL' THEN 0 WHEN 'SEQUEL' THEN 1 ELSE 2 END"
                    " LIMIT 12"
                ),
                {"id": anime_id},
            )
        ]

    if verwandt:
        karten_map = {k["id"]: k for k in fetch_by_ids([v["animeId"] for v in verwandt])}
        detail["related"] = [
            {**karten_map[v["animeId"]], "relation": v["relation"]}
            for v in verwandt
            if v["animeId"] in karten_map
        ]
    else:
        detail["related"] = []

    detail["duration"] = _dauer(anime_id)
    return detail


def _dauer(anime_id: int) -> int | None:
    with catalog_engine.connect() as conn:
        row = conn.execute(
            text("SELECT duration, popularity, source FROM anime WHERE id = :id"),
            {"id": anime_id},
        ).fetchone()
    return int(row[0]) if row and row[0] is not None else None
