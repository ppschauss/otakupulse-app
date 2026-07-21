"""Der Deck-Query ist das Herz der App — hier entscheidet sich, was auf dem Stapel landet."""
from __future__ import annotations

import pytest

from app.deck_query import DeckFilter, build_deck_query


def _sql(f: DeckFilter) -> str:
    return build_deck_query(f)[0]


def test_hentai_bleibt_immer_draussen():
    # Die Website blendet Hentai per noindex aus; im Stapel hat es erst recht nichts verloren.
    assert "is_hentai = false" in _sql(DeckFilter())


def test_karten_ohne_cover_kommen_nicht_in_den_stapel():
    # Eine Swipe-Karte ohne Bild ist wertlos.
    assert "cover_image_url IS NOT NULL" in _sql(DeckFilter())


def test_ohne_filter_keine_ueberfluessigen_bedingungen():
    sql, params = build_deck_query(DeckFilter())
    assert "genres" not in sql
    assert "anime_tags" not in sql
    assert "exclude_ids" not in params


def test_bereits_gewischte_titel_werden_ausgeschlossen():
    sql, params = build_deck_query(DeckFilter(exclude_ids=[5, 9]))
    assert "id <> ALL" in sql
    assert params["exclude_ids"] == [5, 9]


def test_genre_filter_trifft_ueber_die_relationstabelle():
    sql, params = build_deck_query(DeckFilter(genres=["isekai"]))
    assert "anime_rels" in sql and "genres" in sql
    assert params["genres"] == ["isekai"]


def test_tag_filter_trifft_ueber_anime_tags():
    sql, params = build_deck_query(DeckFilter(tags=["magie", "medical"]))
    assert "anime_tags" in sql
    assert params["tags"] == ["magie", "medical"]


def test_mehrere_tags_erweitern_den_stapel_statt_ihn_zu_verengen():
    # Bewusste Entscheidung: ODER, nicht UND. Ein leerer Stapel ist der schlimmste Fehlerfall.
    sql = _sql(DeckFilter(tags=["magie", "medical"]))
    assert sql.count("FROM anime_tags") == 1


def test_sprachfilter_fuer_ger_dub():
    sql, params = build_deck_query(DeckFilter(languages=["de-dub"]))
    assert "anime_available_languages" in sql
    assert params["languages"] == ["de-dub"]


def test_anbieterfilter():
    sql, params = build_deck_query(DeckFilter(providers=["crunchyroll"]))
    assert "anime_streaming_providers" in sql
    assert params["providers"] == ["crunchyroll"]


def test_mindestbewertung():
    sql, params = build_deck_query(DeckFilter(min_score=75))
    assert "average_score >=" in sql
    assert params["min_score"] == 75


def test_jahresbereich():
    sql, params = build_deck_query(DeckFilter(year_from=2015, year_to=2020))
    assert params["year_from"] == 2015
    assert params["year_to"] == 2020


def test_formatfilter_vergleicht_als_text_wegen_postgres_enum():
    # a.format ist ein Postgres-Enum; ohne ::text scheitert der Vergleich mit einem Text-Array.
    sql, params = build_deck_query(DeckFilter(formats=["TV", "MOVIE"]))
    assert "format::text" in sql
    assert params["formats"] == ["TV", "MOVIE"]


def test_zufallssortierung_ist_bei_gleichem_seed_stabil():
    # Sonst tauchen beim Nachladen der nächsten Seite Karten doppelt auf.
    a, pa = build_deck_query(DeckFilter(sort="random", seed="abc"))
    b, pb = build_deck_query(DeckFilter(sort="random", seed="abc"))
    assert a == b and pa == pb
    assert pa["seed"] == "abc"


def test_zufallssortierung_braucht_einen_seed():
    with pytest.raises(ValueError):
        build_deck_query(DeckFilter(sort="random", seed=None))


def test_standardsortierung_ist_nach_beliebtheit():
    assert "popularity DESC" in _sql(DeckFilter())


def test_seitengroesse_ist_gedeckelt():
    # Öffentlich erreichbar: niemand soll die Datenbank seitenweise abgreifen.
    _, params = build_deck_query(DeckFilter(limit=5000), max_page_size=40)
    assert params["limit"] == 40


def test_unbekannte_sortierung_wird_abgelehnt():
    with pytest.raises(ValueError):
        build_deck_query(DeckFilter(sort="; DROP TABLE anime --"))


def test_alle_werte_gehen_als_parameter_nicht_als_text_in_die_query():
    # Schutz vor SQL-Injection: im SQL darf kein Nutzerwert auftauchen.
    sql, _ = build_deck_query(
        DeckFilter(genres=["'; DROP TABLE anime --"], tags=["x"], seed="s", sort="random")
    )
    assert "DROP TABLE" not in sql
