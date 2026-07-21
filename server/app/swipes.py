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


def backfill_matches_for_new_member(session: Session, party: Party, device: Device) -> list[Match]:
    """Legt Matches an, die durch einen späteren Beitritt fällig werden.

    Der Neue hat oft schon Titel gewischt, die andere in der Party ebenfalls wollen.
    Ohne diesen Nachtrag entstünden diese Matches nie — sie kämen erst zustande,
    wenn jemand denselben Titel noch einmal wischt, was nicht passiert.
    """
    others = [
        m for m in session.scalars(
            select(PartyMember.device_id).where(PartyMember.party_id == party.id)
        ) if m != device.id
    ]
    if not others:
        return []

    meine = set(
        session.scalars(
            select(Swipe.anime_id).where(
                Swipe.device_id == device.id, Swipe.direction.in_(INTERESTED)
            )
        )
    )
    if not meine:
        return []

    ihre = set(
        session.scalars(
            select(Swipe.anime_id).where(
                Swipe.device_id.in_(others),
                Swipe.direction.in_(INTERESTED),
                Swipe.anime_id.in_(meine),
            )
        )
    )
    vorhanden = set(
        session.scalars(select(Match.anime_id).where(Match.party_id == party.id))
    )

    created = [Match(party_id=party.id, anime_id=a) for a in sorted(ihre - vorhanden)]
    if created:
        session.add_all(created)
        session.commit()
    return created


def party_members_to_notify(
    session: Session,
    device: Device,
    nur_partys: list | None = None,
) -> list[Device]:
    """Alle Party-Mitglieder außer dem Absender — Empfänger eines Super-Swipes.

    `nur_partys` schränkt auf ausgewählte Partys ein. Fremde IDs werden dabei
    still verworfen, damit niemand über die Auswahl in fremde Partys funkt.
    """
    party_ids = list(
        session.scalars(select(PartyMember.party_id).where(PartyMember.device_id == device.id))
    )
    if nur_partys:
        erlaubt = {int(p) for p in nur_partys if str(p).lstrip("-").isdigit()}
        party_ids = [p for p in party_ids if p in erlaubt]
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
