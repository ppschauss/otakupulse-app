"""Partys, Beitrittscodes und wer wen benachrichtigt bekommt."""
from __future__ import annotations

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.models import Base, Device, Party, PartyMember
from app.swipes import generate_join_code, party_members_to_notify


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
    party = Party(name="Testparty", join_code=generate_join_code(session), created_by=devices[0].id)
    session.add(party)
    session.flush()
    for d in devices:
        session.add(PartyMember(party_id=party.id, device_id=d.id))
    session.commit()
    return party


def test_beitrittscode_ist_sechsstellig(session):
    code = generate_join_code(session)
    assert len(code) == 6


def test_beitrittscode_meidet_verwechselbare_zeichen(session):
    # 0/O und 1/I verwechselt man beim Vorlesen am Telefon.
    for _ in range(50):
        assert not set(generate_join_code(session)) & set("01OI")


def test_beitrittscode_ist_eindeutig(session):
    codes = set()
    for i in range(30):
        code = generate_join_code(session)
        codes.add(code)
        session.add(Party(name=f"P{i}", join_code=code, created_by=None))
        session.commit()
    assert len(codes) == 30


def test_super_swipe_erreicht_alle_anderen(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    c = make_device(session, "dritter")
    make_party(session, a, b, c)

    empfaenger = party_members_to_notify(session, a)
    assert {d.display_name for d in empfaenger} == {"freund", "dritter"}


def test_absender_benachrichtigt_sich_nicht_selbst(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    make_party(session, a, b)
    assert a.id not in {d.id for d in party_members_to_notify(session, a)}


def test_ohne_party_gibt_es_keine_empfaenger(session):
    a = make_device(session, "einzelgaenger")
    assert party_members_to_notify(session, a) == []


def test_alleine_in_der_party_gibt_es_keine_empfaenger(session):
    a = make_device(session, "patrick")
    make_party(session, a)
    assert party_members_to_notify(session, a) == []


def test_jedes_mitglied_nur_einmal_trotz_mehrerer_gemeinsamer_partys(session):
    # Sonst bekäme derselbe Freund zwei Benachrichtigungen für denselben Super-Swipe.
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    make_party(session, a, b)
    make_party(session, a, b)

    empfaenger = party_members_to_notify(session, a)
    assert len(empfaenger) == 1


def test_spaeterer_beitritt_traegt_matches_nach(session):
    """Der Neue hat den Titel schon gewischt, bevor er dabei war."""
    from app.models import SwipeDirection
    from app.swipes import backfill_matches_for_new_member, record_swipe

    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    party = make_party(session, a)

    record_swipe(session, a, 42, SwipeDirection.RIGHT)   # a ist drin, wischt
    record_swipe(session, b, 42, SwipeDirection.RIGHT)   # b noch nicht dabei

    session.add(PartyMember(party_id=party.id, device_id=b.id))
    session.commit()
    nachgetragen = backfill_matches_for_new_member(session, party, b)

    assert [m.anime_id for m in nachgetragen] == [42]


def test_nachtrag_erzeugt_keine_doppelten_matches(session):
    from app.models import Match, SwipeDirection
    from app.swipes import backfill_matches_for_new_member, record_swipe

    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    party = make_party(session, a, b)

    record_swipe(session, a, 42, SwipeDirection.RIGHT)
    record_swipe(session, b, 42, SwipeDirection.RIGHT)   # Match entsteht hier schon
    assert backfill_matches_for_new_member(session, party, b) == []
    assert session.query(Match).count() == 1


def test_super_swipe_laesst_sich_auf_eine_party_beschraenken(session):
    a = make_device(session, "patrick")
    b = make_device(session, "freund")
    c = make_device(session, "kollege")
    p1 = make_party(session, a, b)
    make_party(session, a, c)

    empfaenger = party_members_to_notify(session, a, nur_partys=[p1.id])
    assert {d.display_name for d in empfaenger} == {"freund"}


def test_fremde_party_id_in_der_auswahl_wird_verworfen(session):
    # Sonst könnte man über die Auswahl in Partys funken, in denen man nicht ist.
    a = make_device(session, "patrick")
    b = make_device(session, "fremder")
    make_party(session, a)
    fremde = make_party(session, b)

    assert party_members_to_notify(session, a, nur_partys=[fremde.id]) == []
