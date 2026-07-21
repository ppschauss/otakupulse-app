"""Drosselung — der Dienst steht öffentlich, hier hängt die Missbrauchsgrenze."""
from __future__ import annotations

from app.ratelimit import Fenster


def test_unter_der_grenze_geht_alles_durch():
    f = Fenster(grenze=3, sekunden=60)
    assert [f.erlaubt("a", jetzt=0) for _ in range(3)] == [True, True, True]


def test_ueber_der_grenze_wird_abgewiesen():
    f = Fenster(grenze=3, sekunden=60)
    for _ in range(3):
        f.erlaubt("a", jetzt=0)
    assert f.erlaubt("a", jetzt=0) is False


def test_schluessel_zaehlen_getrennt():
    # Sonst sperrt ein einzelner Vielnutzer alle anderen aus.
    f = Fenster(grenze=1, sekunden=60)
    assert f.erlaubt("a", jetzt=0) is True
    assert f.erlaubt("b", jetzt=0) is True


def test_nach_ablauf_des_fensters_wieder_frei():
    f = Fenster(grenze=2, sekunden=60)
    f.erlaubt("a", jetzt=0)
    f.erlaubt("a", jetzt=10)
    assert f.erlaubt("a", jetzt=30) is False
    # Der erste Eintrag ist jetzt älter als 60 s und zählt nicht mehr mit.
    assert f.erlaubt("a", jetzt=61) is True


def test_fenster_gleitet_statt_zu_springen():
    # Ein starres Fenster würde bei Sekunde 10 zurücksetzen und die doppelte Menge
    # an der Grenze durchlassen. Beim gleitenden Fenster zählen beide Einträge weiter.
    f = Fenster(grenze=2, sekunden=10)
    f.erlaubt("a", jetzt=9.0)
    f.erlaubt("a", jetzt=9.5)
    assert f.erlaubt("a", jetzt=10.5) is False   # beide liegen noch im Fenster
    assert f.erlaubt("a", jetzt=19.1) is True    # erst jetzt ist der von 9,0 raus


def test_aufraeumen_entfernt_alte_schluessel():
    # Ohne das würde die Tabelle mit jedem gesehenen Schlüssel wachsen.
    f = Fenster(grenze=5, sekunden=10)
    f.erlaubt("alt", jetzt=0)
    f.erlaubt("neu", jetzt=100)
    f.aufraeumen(jetzt=100)
    assert "alt" not in f._treffer
    assert "neu" in f._treffer
