"""Swipes aufnehmen und daraus Matches ableiten."""
from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.orm import Session

from .models import Device, Match, Party, PartyMember, Swipe, SwipeDirection

# Beide Richtungen bedeuten Interesse — ein Super-Swipe ist ein Rechts-Wisch mit Ausrufezeichen.
INTERESTED = (SwipeDirection.RIGHT, SwipeDirection.SUPER)


def record_swipe(
    session: Session,
    device: Device,
    anime_id: int,
    direction: SwipeDirection,
) -> list[Match]:
    """Speichert den Swipe und legt fällige Matches an.

    Gibt die *neu* entstandenen Matches zurück — daran hängt die Benachrichtigung.
    Idempotent: derselbe Swipe darf mehrfach ankommen (der Offline-Puffer der App
    schickt bei Verbindungsabbruch erneut), ohne Doppel oder Doppel-Matches.
    """
    existing = session.scalar(
        select(Swipe).where(Swipe.device_id == device.id, Swipe.anime_id == anime_id)
    )
    if existing is None:
        session.add(Swipe(device_id=device.id, anime_id=anime_id, direction=direction))
    else:
        existing.direction = direction
    session.commit()

    if direction not in INTERESTED:
        return []
    return _create_matches(session, device, anime_id)


def _create_matches(session: Session, device: Device, anime_id: int) -> list[Match]:
    """Ein Match entsteht, sobald ein zweites Mitglied derselben Party Interesse zeigt."""
    party_ids = list(
        session.scalars(select(PartyMember.party_id).where(PartyMember.device_id == device.id))
    )
    if not party_ids:
        return []

    created: list[Match] = []
    for party_id in party_ids:
        # Gibt es diesen Match schon, ist nichts zu tun — auch wenn ein Dritter nachzieht.
        if session.scalar(
            select(Match).where(Match.party_id == party_id, Match.anime_id == anime_id)
        ):
            continue

        others = [
            m for m in session.scalars(
                select(PartyMember.device_id).where(PartyMember.party_id == party_id)
            ) if m != device.id
        ]
        if not others:
            continue

        someone_else_wants_it = session.scalar(
            select(Swipe).where(
                Swipe.device_id.in_(others),
                Swipe.anime_id == anime_id,
                Swipe.direction.in_(INTERESTED),
            )
        )
        if someone_else_wants_it is None:
            continue

        match = Match(party_id=party_id, anime_id=anime_id)
        session.add(match)
        created.append(match)

    if created:
        session.commit()
    return created


def party_members_to_notify(session: Session, device: Device) -> list[Device]:
    """Alle Party-Mitglieder außer dem Absender — Empfänger eines Super-Swipes."""
    party_ids = list(
        session.scalars(select(PartyMember.party_id).where(PartyMember.device_id == device.id))
    )
    if not party_ids:
        return []

    device_ids = {
        d for d in session.scalars(
            select(PartyMember.device_id).where(PartyMember.party_id.in_(party_ids))
        ) if d != device.id
    }
    if not device_ids:
        return []
    return list(session.scalars(select(Device).where(Device.id.in_(device_ids))))


def generate_join_code(session: Session) -> str:
    """Kurzer, gut vorlesbarer Code. Ohne 0/O und 1/I, die verwechselt man am Telefon."""
    import secrets

    alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    for _ in range(20):
        code = "".join(secrets.choice(alphabet) for _ in range(6))
        if session.scalar(select(Party).where(Party.join_code == code)) is None:
            return code
    raise RuntimeError("Kein freier Party-Code gefunden")
