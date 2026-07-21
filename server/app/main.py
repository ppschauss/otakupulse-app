"""OtakuPulse Companion — Backend unter app.otakupulse.de.

Steht öffentlich im Internet (Cloudflare-Tunnel → NPM → Container), damit die
App ohne VPN funktioniert. Deshalb: alles außer /health und der Geräte-
Registrierung verlangt ein Geräte-Token, und es gibt keinerlei Schreibzugriff
auf die OtakuPulse-Daten.
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone

from fastapi import Depends, FastAPI, HTTPException, Query, Request, status
from sqlalchemy import select, text
from sqlalchemy.orm import Session

from . import catalog
from .auth import current_device, new_token
from .db import companion_engine, get_session, get_settings
from .deck_query import DeckFilter
from .push import Pusher
from .ratelimit import ALLGEMEIN, REGISTRIERUNG, client_ip, pruefe
from .swipes import (
    backfill_matches_for_new_member,
    generate_join_code,
    party_members_to_notify,
    record_swipe,
)
from .models import Base, Device, Match, Party, PartyMember, Swipe, SwipeDirection

app = FastAPI(title="OtakuPulse Companion", docs_url=None, redoc_url=None, openapi_url=None)
pusher = Pusher(get_settings())


@app.on_event("startup")
def _startup() -> None:
    Base.metadata.create_all(companion_engine)


@app.get("/health")
def health() -> dict:
    with companion_engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    return {"status": "ok"}


@app.post("/v1/devices", status_code=201)
def register_device(
    payload: dict,
    request: Request,
    session: Session = Depends(get_session),
) -> dict:
    """Registriert ein Gerät und gibt das Token zurück — die einzige offene Route.

    Streng gedrosselt: ohne Grenze könnte jeder beliebig viele Tokens erzeugen und
    damit den Deck-Ausschluss gewischter Titel umgehen.
    """
    pruefe(REGISTRIERUNG, f"reg:{client_ip(request)}")
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


def _party_payload(session: Session, party: Party, device: Device) -> dict:
    mitglieder = session.execute(
        select(Device).join(PartyMember, PartyMember.device_id == Device.id)
        .where(PartyMember.party_id == party.id)
    ).scalars().all()
    match_ids = list(session.scalars(
        select(Match.anime_id).where(Match.party_id == party.id).order_by(Match.created_at.desc())
    ))
    return {
        "id": party.id,
        "name": party.name,
        "joinCode": party.join_code,
        "members": [
            {"id": m.id, "displayName": m.display_name, "isMe": m.id == device.id}
            for m in mitglieder
        ],
        "matches": catalog.fetch_by_ids(match_ids),
    }


@app.get("/v1/parties")
def list_parties(
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Nur die eigenen Partys — fremde sind nicht auflistbar."""
    ids = list(session.scalars(select(PartyMember.party_id).where(PartyMember.device_id == device.id)))
    if not ids:
        return {"parties": []}
    parties = session.scalars(select(Party).where(Party.id.in_(ids))).all()
    return {"parties": [_party_payload(session, p, device) for p in parties]}


@app.post("/v1/parties", status_code=201)
def create_party(
    payload: dict,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    name = (payload.get("name") or "").strip()[:60] or "Meine Party"
    party = Party(name=name, join_code=generate_join_code(session), created_by=device.id)
    session.add(party)
    session.flush()
    session.add(PartyMember(party_id=party.id, device_id=device.id))
    session.commit()
    return _party_payload(session, party, device)


@app.post("/v1/parties/join")
def join_party(
    payload: dict,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Beitritt per Code. Grossschreibung egal — den Code liest man sich vor."""
    code = (payload.get("joinCode") or "").strip().upper()
    party = session.scalar(select(Party).where(Party.join_code == code))
    if party is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Party-Code unbekannt")

    schon_dabei = session.scalar(
        select(PartyMember).where(
            PartyMember.party_id == party.id, PartyMember.device_id == device.id
        )
    )
    if schon_dabei is None:
        session.add(PartyMember(party_id=party.id, device_id=device.id))
        session.commit()
        # Wer später dazukommt, hat vielleicht schon passende Titel gewischt —
        # rückwirkend prüfen, sonst fehlen Matches ohne ersichtlichen Grund.
        backfill_matches_for_new_member(session, party, device)

    return _party_payload(session, party, device)


@app.patch("/v1/parties/{party_id}")
def rename_party(
    party_id: int,
    payload: dict,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Umbenennen darf jedes Mitglied — es ist eine gemeinsame Liste, kein Besitz."""
    party = _eigene_party(session, party_id, device)
    name = (payload.get("name") or "").strip()[:60]
    if name:
        party.name = name
        session.commit()
    return _party_payload(session, party, device)


@app.delete("/v1/parties/{party_id}")
def delete_party(
    party_id: int,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Löschen darf nur, wer die Party angelegt hat.

    Für alle anderen gibt es „verlassen": sonst könnte ein einzelnes Mitglied
    die gesammelten Matches aller anderen wegwerfen.
    """
    party = _eigene_party(session, party_id, device)
    if party.created_by != device.id:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN,
            "Nur wer die Party angelegt hat, kann sie löschen — du kannst sie verlassen.",
        )
    session.delete(party)
    session.commit()
    return {"deleted": party_id}


def _eigene_party(session: Session, party_id: int, device: Device) -> Party:
    """Party nur herausgeben, wenn das Gerät Mitglied ist — sonst 404."""
    mitglied = session.scalar(
        select(PartyMember).where(
            PartyMember.party_id == party_id, PartyMember.device_id == device.id
        )
    )
    party = session.get(Party, party_id) if mitglied else None
    if party is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Party nicht gefunden")
    return party


@app.post("/v1/parties/{party_id}/leave")
def leave_party(
    party_id: int,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    session.query(PartyMember).filter(
        PartyMember.party_id == party_id, PartyMember.device_id == device.id
    ).delete()
    session.commit()
    return {"left": party_id}


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

        if direction is SwipeDirection.SUPER:
            _melde_super_swipe(session, device, anime_id, item.get("partyIds"))

    return {"accepted": accepted, "matches": matches}


def _melde_super_swipe(
    session: Session,
    device: Device,
    anime_id: int,
    party_ids: list | None = None,
) -> None:
    """Schickt den Super-Swipe an die anderen Mitglieder.

    Ohne Angabe gehen alle gemeinsamen Partys — die App schickt die Auswahl mit,
    sobald man in mehr als einer ist.
    """
    empfaenger = party_members_to_notify(session, device, party_ids)
    if not empfaenger:
        return
    karten = catalog.fetch_by_ids([anime_id])
    titel = karten[0]["title"] if karten else "ein Anime"
    pusher.send(
        session,
        empfaenger,
        titel=f"{device.display_name} schwärmt von etwas",
        text=f"{titel} — unbedingt anschauen!",
        daten={"animeId": anime_id, "type": "super_swipe"},
    )


@app.get("/v1/anime/{anime_id}")
def anime_detail(
    anime_id: int,
    device: Device = Depends(current_device),
) -> dict:
    """Einzelner Titel — für die Detailansicht aus der Watchlist heraus."""
    karten = catalog.fetch_by_ids([anime_id])
    if not karten:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Unbekannter Titel")
    return karten[0]


@app.get("/v1/airing")
def airing(
    days: int = 7,
    back: int = 0,
    onlyMine: bool = False,
    device: Device = Depends(current_device),
    session: Session = Depends(get_session),
) -> dict:
    """Ausstrahlungstermine — für den Wochenkalender und die Folgen-Prüfung.

    `back` blickt zurück: die App fragt so nach Folgen, die seit dem letzten
    Abgleich erschienen sind. `onlyMine` beschränkt auf gewischte Titel.
    """
    jetzt = datetime.now(timezone.utc)
    von = jetzt - timedelta(days=max(0, min(back, 30)))
    bis = jetzt + timedelta(days=max(1, min(days, 31)))

    ids = None
    if onlyMine:
        ids = list(
            session.scalars(
                select(Swipe.anime_id).where(
                    Swipe.device_id == device.id,
                    Swipe.direction.in_([SwipeDirection.RIGHT, SwipeDirection.SUPER]),
                )
            )
        )
        if not ids:
            return {"airing": []}

    return {"airing": catalog.fetch_airing(von, bis, ids)}


@app.get("/v1/filters")
def filters(device: Device = Depends(current_device)) -> dict:
    """Auswahlmöglichkeiten für das Filterblatt der App."""
    return {"genres": catalog.fetch_genres(), "tags": catalog.fetch_tags()}
