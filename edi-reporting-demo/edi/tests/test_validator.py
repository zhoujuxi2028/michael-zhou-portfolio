"""validator.py 单元测试：schema 驱动校验 + 错误分级。"""
from __future__ import annotations

from parser import parse_message
from schema import ORDERS_SCHEMA, get_schema
from validator import Severity, validate


class TestValidMessage:
    def test_orders_passes(self, orders_text):
        msg = parse_message(orders_text)
        result = validate(msg, ORDERS_SCHEMA)
        assert result.is_valid
        assert result.errors == []


class TestMissingRequiredSegment:
    def test_missing_bgm_is_error(self, orders_missing_bgm):
        msg = parse_message(orders_missing_bgm)
        result = validate(msg, ORDERS_SCHEMA)
        assert not result.is_valid
        codes = [i.code for i in result.errors]
        assert "MISSING_REQUIRED" in codes
        missing = [i for i in result.errors if i.code == "MISSING_REQUIRED"]
        assert any(i.tag == "BGM" for i in missing)


class TestUnknownSegment:
    def test_unknown_segment_is_warning(self, orders_unknown_segment):
        msg = parse_message(orders_unknown_segment)
        result = validate(msg, ORDERS_SCHEMA)
        # 未知段是 WARNING，不影响整体通过
        assert result.is_valid
        assert any(i.code == "UNKNOWN_SEGMENT" for i in result.warnings)
        # 但定位信息要准确
        warn = next(i for i in result.warnings if i.code == "UNKNOWN_SEGMENT")
        assert warn.tag == "XYZ"


class TestSchemaRegistry:
    def test_get_orders(self):
        assert get_schema("ORDERS") is ORDERS_SCHEMA

    def test_get_desadv(self):
        s = get_schema("DESADV")
        assert s is not None
        assert s.name == "DESADV"

    def test_get_invoic(self):
        s = get_schema("INVOIC")
        assert s is not None
        assert s.name == "INVOIC"

    def test_unknown_type_returns_none(self):
        assert get_schema("NONEXISTENT") is None

    def test_case_insensitive(self):
        assert get_schema("orders") is ORDERS_SCHEMA


class TestSeverity:
    def test_severity_values(self):
        assert Severity.ERROR.value == "ERROR"
        assert Severity.WARNING.value == "WARNING"
