"""Einfache Drosselung für den öffentlich erreichbaren Dienst.

Bewusst im Arbeitsspeicher statt in Redis: der Dienst läuft als einzelne Instanz,
und ein Neustart setzt die Zähler zurück — das ist hier kein Schaden. Dieselbe
Überlegung wie beim Kontaktformular von OtakuPulse.
"""
from __future__ import annotations

import time
from collections import defaultdict, deque

from fastapi import HTTPException, Request, status


class Fenster:
    """Zählt Anfragen je Schlüssel in einem gleitenden Zeitfenster."""

    def __init__(self, grenze: int, sekunden: int) -> None:
        self.grenze = grenze
        self.sekunden = sekunden
        self._treffer: dict[str, deque[float]] = defaultdict(deque)

    def erlaubt(self, schluessel: str, jetzt: float | None = None) -> bool:
        jetzt = time.monotonic() if jetzt is None else jetzt
        eintraege = self._treffer[schluessel]

        # Abgelaufene Einträge vorne entfernen — die Schlange bleibt dadurch kurz.
        grenzwert = jetzt - self.sekunden
        while eintraege and eintraege[0] <= grenzwert:
            eintraege.popleft()

        if len(eintraege) >= self.grenze:
            return False
        eintraege.append(jetzt)
        return True

    def aufraeumen(self, jetzt: float | None = None) -> None:
        """Verhindert, dass die Tabelle mit alten Schlüsseln vollläuft."""
        jetzt = time.monotonic() if jetzt is None else jetzt
        grenzwert = jetzt - self.sekunden
        for schluessel in [k for k, v in self._treffer.items() if not v or v[-1] <= grenzwert]:
            del self._treffer[schluessel]


# Registrierung ist die einzige offene Schreibroute — hier am strengsten.
REGISTRIERUNG = Fenster(grenze=5, sekunden=3600)
# Normale Nutzung: großzügig genug, dass Wischen und Nachladen nie anstoßen.
ALLGEMEIN = Fenster(grenze=240, sekunden=60)


def client_ip(request: Request) -> str:
    """Echte IP hinter Cloudflare. Ohne diesen Header sähen wir nur den Tunnel."""
    for header in ("cf-connecting-ip", "x-forwarded-for"):
        wert = request.headers.get(header)
        if wert:
            return wert.split(",")[0].strip()
    return request.client.host if request.client else "unbekannt"


def pruefe(fenster: Fenster, schluessel: str) -> None:
    if not fenster.erlaubt(schluessel):
        raise HTTPException(
            status.HTTP_429_TOO_MANY_REQUESTS,
            "Zu viele Anfragen — bitte kurz warten.",
        )
