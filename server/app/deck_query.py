"""Baut die SQL-Abfrage für den Swipe-Stapel gegen die OtakuPulse-Datenbank.

Bewusst eine reine Funktion ohne Datenbankzugriff: so lässt sich die Filterlogik
testen, ohne Postgres zu starten. Alle Nutzerwerte gehen als gebundene Parameter
in die Abfrage, niemals als Text.
"""
from __future__ import annotations

from dataclasses import dataclass, field

# Spalten, die eine Swipe-Karte und die Detailansicht brauchen.
SELECT_COLUMNS = """
    a.id,
    a.anilist_id,
    a.slug,
    a.title_romaji,
    a.title_english,
    a.title_german,
    a.description_de,
    a.description_source,
    a.cover_image_url,
    a.banner_image_url,
    a.format::text   AS format,
    a.status::text   AS status,
    a.episodes,
    a.season::text   AS season,
    a.season_year,
    a.average_score,
    a.popularity
"""

SORTS = {
    "popularity": "a.popularity DESC NULLS LAST, a.id",
    "score": "a.average_score DESC NULLS LAST, a.id",
    "newest": "a.season_year DESC NULLS LAST, a.id",
    # Stabil bei gleichem Seed — sonst tauchen Karten beim Nachladen doppelt auf.
    "random": "hashtext(a.id::text || :seed), a.id",
}


@dataclass
class DeckFilter:
    genres: list[str] = field(default_factory=list)
    tags: list[str] = field(default_factory=list)
    providers: list[str] = field(default_factory=list)
    languages: list[str] = field(default_factory=list)
    formats: list[str] = field(default_factory=list)
    statuses: list[str] = field(default_factory=list)
    year_from: int | None = None
    year_to: int | None = None
    min_score: int | None = None
    exclude_ids: list[int] = field(default_factory=list)
    sort: str = "popularity"
    seed: str | None = None
    limit: int = 20
    offset: int = 0


def build_deck_query(f: DeckFilter, max_page_size: int = 40) -> tuple[str, dict]:
    """Liefert (SQL, Parameter) für eine Seite des Swipe-Stapels."""
    if f.sort not in SORTS:
        raise ValueError(f"Unbekannte Sortierung: {f.sort!r}")
    if f.sort == "random" and not f.seed:
        raise ValueError("Zufallssortierung braucht einen Seed, sonst wiederholen sich Karten")

    where = [
        # Hentai bleibt draußen — die Website setzt es auf noindex, im Stapel hat es nichts verloren.
        "a.is_hentai = false",
        # Eine Karte ohne Bild ist als Swipe-Karte wertlos.
        "a.cover_image_url IS NOT NULL",
    ]
    params: dict = {}

    if f.genres:
        where.append(
            "EXISTS (SELECT 1 FROM anime_rels r JOIN genres g ON g.id = r.genres_id"
            " WHERE r.parent_id = a.id AND g.slug = ANY(:genres))"
        )
        params["genres"] = f.genres

    if f.tags:
        # ODER, nicht UND: mehrere Tags erweitern den Stapel. Ein leerer Stapel ist der
        # schlimmste Fehlerfall dieser App.
        where.append(
            "EXISTS (SELECT 1 FROM anime_tags at JOIN tags t ON t.id = at.tag_id"
            " WHERE at._parent_id = a.id AND t.slug = ANY(:tags))"
        )
        params["tags"] = f.tags

    if f.providers:
        where.append(
            "EXISTS (SELECT 1 FROM anime_streaming_providers sp"
            " WHERE sp.parent_id = a.id AND sp.value::text = ANY(:providers))"
        )
        params["providers"] = f.providers

    if f.languages:
        where.append(
            "EXISTS (SELECT 1 FROM anime_available_languages al"
            " WHERE al.parent_id = a.id AND al.value::text = ANY(:languages))"
        )
        params["languages"] = f.languages

    if f.formats:
        # format ist ein Postgres-Enum — ohne ::text scheitert der Vergleich mit einem Text-Array.
        where.append("a.format::text = ANY(:formats)")
        params["formats"] = f.formats

    if f.statuses:
        where.append("a.status::text = ANY(:statuses)")
        params["statuses"] = f.statuses

    if f.year_from is not None:
        where.append("a.season_year >= :year_from")
        params["year_from"] = f.year_from

    if f.year_to is not None:
        where.append("a.season_year <= :year_to")
        params["year_to"] = f.year_to

    if f.min_score is not None:
        where.append("a.average_score >= :min_score")
        params["min_score"] = f.min_score

    if f.exclude_ids:
        where.append("a.id <> ALL(:exclude_ids)")
        params["exclude_ids"] = f.exclude_ids

    if f.sort == "random":
        params["seed"] = f.seed

    # Seitengröße deckeln: der Dienst steht öffentlich, niemand soll den Katalog abgreifen.
    params["limit"] = max(1, min(f.limit, max_page_size))
    params["offset"] = max(0, f.offset)

    sql = (
        f"SELECT {SELECT_COLUMNS}\n"
        "FROM anime a\n"
        "WHERE " + "\n  AND ".join(where) + "\n"
        f"ORDER BY {SORTS[f.sort]}\n"
        "LIMIT :limit OFFSET :offset"
    )
    return sql, params
