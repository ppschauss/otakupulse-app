"""OtakuPulse Companion — Backend unter app.otakupulse.de.

Steht öffentlich im Internet (Cloudflare-Tunnel → NPM → Container), damit die
App ohne VPN funktioniert. Deshalb: alles außer /health und der Geräte-
Registrierung verlangt ein Geräte-Token, und es gibt keinerlei Schreibzugriff
auf die OtakuPulse-Daten.
"""
from __future__ import annotations

from fastapi import Depends, FastAPI, Query
from sqlalchemy import select, text
from sqlalchemy.orm import Session

from . import catalog
from .auth import current_device, new_token
from .db import companion_engine, get_session, get_settings
from .deck_query import DeckFilter
from .swipes import record_swipe
from .models import Base, Device, Swipe, SwipeDirection

app = FastAPI(title="OtakuPulse Companion", docs_url=None, redoc_url=None, openapi_url=None)


@app.on_event("startup")
def _startup() -> None:
    Base.metadata.create_all(companion_engine)


@app.get("/health")
def health() -> dict:
    with companion_engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    return {"status": "ok"}


@app.post("/v1/devices", status_code=201)
def register_device(payload: dict, session: Session = Depends(get_session)) -> dict:
    """Registriert ein Gerät und gibt das Token zurück — die einzige offene Route."""
    name = (payload.get("displayName") or "").strip()[:60] or "Unbenannt"
    device = Device(token=new_token(), display_name=name, fcm_token=payload.get("fcmToken"))
    session.add(device)
    session.commit()
    return {"deviceId": device.id, "token": device.token, "displayName": device.display_name}


@app.patch("/v1/devices/me")
def update_device(
    payload: dict,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    if "displayName" in payload:
        device.display_name = (payload["displayName"] or "").strip()[:60] or device.display_name
    if "fcmToken" in payload:
        device.fcm_token = payload["fcmToken"]
    session.merge(device)
    session.commit()
    return {"deviceId": device.id, "displayName": device.display_name}


@app.get("/v1/deck")
def deck(
    genres: list[str] = Query(default=[]),
    tags: list[str] = Query(default=[]),
    providers: list[str] = Query(default=[]),
    languages: list[str] = Query(default=[]),
    formats: list[str] = Query(default=[]),
    statuses: list[str] = Query(default=[]),
    yearFrom: int | None = None,
    yearTo: int | None = None,
    minScore: int | None = None,
    sort: str = "popularity",
    seed: str | None = None,
    limit: int = 20,
    offset: int = 0,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Der Swipe-Stapel. Bereits gewischte Titel fallen serverseitig heraus."""
    swiped = list(session.scalars(select(Swipe.anime_id).where(Swipe.device_id == device.id)))
    f = DeckFilter(
        genres=genres,
        tags=tags,
        providers=providers,
        languages=languages,
        formats=formats,
        statuses=statuses,
        year_from=yearFrom,
        year_to=yearTo,
        min_score=minScore,
        exclude_ids=swiped,
        sort=sort,
        seed=seed,
        limit=limit,
        offset=offset,
    )
    cards = catalog.fetch_deck(f, max_page_size=get_settings().deck_max_page_size)
    return {"cards": cards, "count": len(cards)}


@app.post("/v1/swipes")
def post_swipes(
    payload: dict,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Nimmt einen Stapel Swipes entgegen.

    Die App puffert Swipes lokal und schickt sie gebündelt — Wischen funktioniert
    dadurch auch ohne Netz. Wiederholte Zustellung ist unschädlich.
    """
    items = payload.get("swipes") or []
    matches: list[dict] = []
    accepted = 0

    for item in items:
        try:
            anime_id = int(item["animeId"])
            direction = SwipeDirection(item["direction"])
        except (KeyError, ValueError, TypeError):
            continue  # Kaputte Einzelposten überspringen, den Rest trotzdem annehmen
        for match in record_swipe(session, device, anime_id, direction):
            matches.append({"partyId": match.party_id, "animeId": match.anime_id})
        accepted += 1

    return {"accepted": accepted, "matches": matches}


@app.get("/v1/filters")
def filters(device: Device = Depends(current_device)) -> dict:
    """Auswahlmöglichkeiten für das Filterblatt der App."""
    return {"genres": catalog.fetch_genres(), "tags": catalog.fetch_tags()}
