"""Swipes und Matches — die Logik, an der die Party-Funktion hängt."""
from __future__ import annotations

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.models import Base, Device, Match, Party, PartyMember, Swipe, SwipeDirection
from app.swipes import record_swipe


@pytest.fixture()
def session() -> Session:
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with sessionmaker(bind=engine)() as s:
        yield s


def make_device(session: Session, name: str) -> Device:
    device = Device(token=f"token-{name}", display_name=name)
    session.add(device)
    session.commit()
    return device


def make_party(session: Session, *devices: Device) -> Party:
    party = Party(name="Testparty", join_code="ABC123", created_by=devices[0].id)
    session.add(party)
    session.flush()
    for d in devices:
        session.add(PartyMember(party_id=party.id, device_id=d.id))
    session.commit()
    return party


def test_swipe_wird_gespeichert(session):
    device = make_device(session, "patrick")
    record_swipe(session, device, anime_id=42, direction=SwipeDirection.RIGHT)
    stored = session.query(Swipe).one()
    assert stored.anime_id == 42
    assert stored.direction == SwipeDirection.RIGHT


def test_erneutes_wischen_ueberschreibt_statt_zu_doppeln(session):
    # Der Offline-Puffer der App kann denselben Swipe zweimal schicken.
    device = make_device(session, "patrick")
    record_swipe(session, device, 42, SwipeDirection.LEFT)
    record_swipe(session, device, 42, SwipeDirection.RIGHT)
    assert session.query(Swipe).count() == 1
    assert session.query(Swipe).one().direction == SwipeDirection.RIGHT


def test_ohne_party_entsteht_kein_match(session):
    device = make_device(session, "patrick")
    assert record_swipe(session, device, 42, SwipeDirection.RIGHT) == []


def test_match_entsteht_beim_zweiten_rechts_wisch(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    party = make_party(session, a, b)

    assert record_swipe(session, a, 42, SwipeDirection.RIGHT) == []
    matches = record_swipe(session, b, 42, SwipeDirection.RIGHT)

    assert [m.anime_id for m in matches] == [42]
    assert session.query(Match).count() == 1
    assert session.query(Match).one().party_id == party.id


def test_links_wischen_erzeugt_kein_match(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    make_party(session, a, b)

    record_swipe(session, a, 42, SwipeDirection.RIGHT)
    assert record_swipe(session, b, 42, SwipeDirection.LEFT) == []
    assert session.query(Match).count() == 0


def test_super_swipe_zaehlt_als_interesse_und_matcht(session):
    # Ein Super-Swipe ist ein Rechts-Wisch mit Ausrufezeichen — er muss ebenso matchen.
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    make_party(session, a, b)

    record_swipe(session, a, 42, SwipeDirection.RIGHT)
    matches = record_swipe(session, b, 42, SwipeDirection.SUPER)
    assert [m.anime_id for m in matches] == [42]


def test_match_entsteht_nur_einmal(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    c = make_device(session, "dritter")
    make_party(session, a, b, c)

    record_swipe(session, a, 42, SwipeDirection.RIGHT)
    record_swipe(session, b, 42, SwipeDirection.RIGHT)
    # Der Dritte wischt denselben Titel — es darf kein zweiter Match-Eintrag entstehen.
    assert record_swipe(session, c, 42, SwipeDirection.RIGHT) == []
    assert session.query(Match).count() == 1


def test_match_nur_innerhalb_derselben_party(session):
    a = make_device(session, "patrick")
    b = make_device(session, "fremder")
    make_party(session, a)  # b ist in keiner gemeinsamen Party

    record_swipe(session, a, 42, SwipeDirection.RIGHT)
    assert record_swipe(session, b, 42, SwipeDirection.RIGHT) == []
    assert session.query(Match).count() == 0


def test_match_in_mehreren_gemeinsamen_partys(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    p1 = make_party(session, a, b)
    p2 = Party(name="Zweite", join_code="XYZ789", created_by=a.id)
    session.add(p2)
    session.flush()
    session.add(PartyMember(party_id=p2.id, device_id=a.id))
    session.add(PartyMember(party_id=p2.id, device_id=b.id))
    session.commit()

    record_swipe(session, a, 42, SwipeDirection.RIGHT)
    matches = record_swipe(session, b, 42, SwipeDirection.RIGHT)
    assert {m.party_id for m in matches} == {p1.id, p2.id}
