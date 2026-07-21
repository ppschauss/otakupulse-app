"""Eigene Tabellen des Companion-Backends (Geräte, Partys, Swipes, Matches)."""
from __future__ import annotations

import enum
from datetime import datetime, timezone

from sqlalchemy import (
    BigInteger,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


# Swipes und Matches können viele werden, deshalb BIGINT. SQLite vergibt IDs aber nur bei
# INTEGER PRIMARY KEY automatisch — ohne diese Variante scheitern die Tests, obwohl der
# Produktivbetrieb auf Postgres läuft.
BigId = BigInteger().with_variant(Integer, "sqlite")


class Base(DeclarativeBase):
    pass


class SwipeDirection(str, enum.Enum):
    LEFT = "LEFT"    # kein Interesse
    RIGHT = "RIGHT"  # auf die Watchlist
    SUPER = "SUPER"  # Super-Swipe: Push an die Party


class Device(Base):
    """Ein Gerät ist die Identität — es gibt bewusst keine Konten."""

    __tablename__ = "device"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    token: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    display_name: Mapped[str] = mapped_column(String(60))
    fcm_token: Mapped[str | None] = mapped_column(String(400), default=None)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    last_seen: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    memberships: Mapped[list["PartyMember"]] = relationship(back_populates="device")


class Party(Base):
    __tablename__ = "party"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(60))
    join_code: Mapped[str] = mapped_column(String(12), unique=True, index=True)
    created_by: Mapped[int] = mapped_column(ForeignKey("device.id", ondelete="SET NULL"), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    members: Mapped[list["PartyMember"]] = relationship(
        back_populates="party", cascade="all, delete-orphan"
    )


class PartyMember(Base):
    __tablename__ = "party_member"
    __table_args__ = (UniqueConstraint("party_id", "device_id", name="uq_party_member"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    party_id: Mapped[int] = mapped_column(ForeignKey("party.id", ondelete="CASCADE"), index=True)
    device_id: Mapped[int] = mapped_column(ForeignKey("device.id", ondelete="CASCADE"), index=True)
    joined_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)

    party: Mapped[Party] = relationship(back_populates="members")
    device: Mapped[Device] = relationship(back_populates="memberships")


class Swipe(Base):
    """Ein Gerät wischt jeden Anime höchstens einmal — erneutes Wischen überschreibt."""

    __tablename__ = "swipe"
    __table_args__ = (UniqueConstraint("device_id", "anime_id", name="uq_swipe_device_anime"),)

    id: Mapped[int] = mapped_column(BigId, primary_key=True)
    device_id: Mapped[int] = mapped_column(ForeignKey("device.id", ondelete="CASCADE"), index=True)
    anime_id: Mapped[int] = mapped_column(Integer, index=True)
    direction: Mapped[SwipeDirection] = mapped_column(Enum(SwipeDirection, name="swipe_direction"))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class Match(Base):
    """Entsteht, sobald zwei Mitglieder derselben Party denselben Titel nach rechts wischen."""

    __tablename__ = "match"
    __table_args__ = (UniqueConstraint("party_id", "anime_id", name="uq_match_party_anime"),)

    id: Mapped[int] = mapped_column(BigId, primary_key=True)
    party_id: Mapped[int] = mapped_column(ForeignKey("party.id", ondelete="CASCADE"), index=True)
    anime_id: Mapped[int] = mapped_column(Integer, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
