"""Geräte-Token statt Konten: ein Bearer-Token identifiziert das Gerät."""
from __future__ import annotations

import secrets

from fastapi import Depends, Header, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from .db import get_session
from .models import Device, utcnow

TOKEN_BYTES = 32  # 256 Bit — nicht ratbar, auch wenn der Dienst öffentlich steht


def new_token() -> str:
    return secrets.token_urlsafe(TOKEN_BYTES)[:64]


def current_device(
    authorization: str | None = Header(default=None),
    session: Session = Depends(get_session),
) -> Device:
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Geräte-Token fehlt")

    token = authorization[7:].strip()
    device = session.scalar(select(Device).where(Device.token == token))
    if device is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Geräte-Token ungültig")

    device.last_seen = utcnow()
    session.commit()
    return device
