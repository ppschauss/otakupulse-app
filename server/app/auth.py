"""Geräte-Token statt Konten: ein Bearer-Token identifiziert das Gerät."""
from __future__ import annotations

import secrets

from fastapi import Depends, Header, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from .db import get_session
from .models import Device, utcnow
from .ratelimit import ALLGEMEIN, client_ip, pruefe

TOKEN_BYTES = 32  # 256 Bit — nicht ratbar, auch wenn der Dienst öffentlich steht


def new_token() -> str:
    return secrets.token_urlsafe(TOKEN_BYTES)[:64]


def current_device(
    request: Request,
    authorization: str | None = Header(default=None),
    session: Session = Depends(get_session),
) -> Device:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Geräte-Token fehlt")

    token = authorization[7:].strip()
    device = session.scalar(select(Device).where(Device.token == token))
    if device is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Geräte-Token ungültig")

    # Je Token drosseln, nicht je IP: mehrere Geräte teilen sich oft eine IP,
    # und ein verlorenes Token soll die anderen nicht mitreissen.
    pruefe(ALLGEMEIN, f"tok:{device.id}")
    pruefe(ALLGEMEIN, f"ip:{client_ip(request)}")

    device.last_seen = utcnow()
    session.commit()
    return device
