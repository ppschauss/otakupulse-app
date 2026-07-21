"""Wann ein FCM-Token als tot gilt — hier entscheidet sich, ob gültige Tokens überleben."""
from __future__ import annotations

from app.push import _token_ist_tot

# Echte Antwort von FCM auf ein kaputtes Token (gekürzt).
KAPUTTES_TOKEN = '{"error":{"message":"The registration token is not a valid FCM registration token"}}'
FEHLERHAFTE_NACHRICHT = '{"error":{"message":"Invalid JSON payload received. Unknown name \\"foo\\""}}'


def test_deinstallierte_app_gilt_als_tot():
    assert _token_ist_tot(404, "") is True


def test_falsches_projekt_gilt_als_tot():
    assert _token_ist_tot(403, "") is True


def test_kaputtes_token_gilt_als_tot():
    assert _token_ist_tot(400, KAPUTTES_TOKEN) is True


def test_fehlerhafte_nachricht_loescht_keine_tokens():
    # Sonst würde ein Fehler im Nachrichtenaufbau reihenweise gültige Tokens löschen.
    assert _token_ist_tot(400, FEHLERHAFTE_NACHRICHT) is False


def test_serverfehler_gilt_nicht_als_tot():
    # 500 und 503 sind vorübergehend — später klappt es wieder.
    assert _token_ist_tot(500, "") is False
    assert _token_ist_tot(503, "") is False


def test_grossschreibung_egal():
    assert _token_ist_tot(400, '{"message":"The Registration Token is invalid"}') is True
