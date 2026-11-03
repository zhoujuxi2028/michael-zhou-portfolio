"""pytest 公共 fixtures：提供样例报文与解析结果。"""
from __future__ import annotations

from pathlib import Path

import pytest

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures"


def _load(name: str) -> str:
    return (FIXTURES / name).read_text(encoding="utf-8")


@pytest.fixture(scope="session")
def orders_text() -> str:
    return _load("sample_orders.edi")


@pytest.fixture(scope="session")
def orders_missing_bgm() -> str:
    return _load("sample_orders_missing_bgm.edi")


@pytest.fixture(scope="session")
def orders_unknown_segment() -> str:
    return _load("sample_orders_unknown_segment.edi")
