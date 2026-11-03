"""parser.py 单元测试：纯解析逻辑的正确性。"""
from __future__ import annotations

import pytest

from parser import Message, parse_message, parse_segment


class TestParseSegment:
    def test_basic_segment(self):
        seg = parse_segment("BGM+220+PO-2026-0001+9")
        assert seg.tag == "BGM"
        assert seg.elements == [["220"], ["PO-2026-0001"], ["9"]]

    def test_components_split_by_colon(self):
        seg = parse_segment("UNB+UNOA:3+SUPPLIER01:14")
        assert seg.elements[0] == ["UNOA", "3"]
        assert seg.element(0) == ["UNOA", "3"]

    def test_component_accessor(self):
        seg = parse_segment("DTM+137:20261101:102")
        assert seg.component(0, 0) == "137"
        assert seg.component(0, 1) == "20261101"
        assert seg.component(0, 2) == "102"

    def test_out_of_range_returns_empty(self):
        seg = parse_segment("BGM+220")
        assert seg.element(5) == []
        assert seg.component(0, 9) == ""


class TestParseMessage:
    def test_skips_una_and_empty(self, orders_text):
        msg = parse_message(orders_text)
        tags = msg.segment_tags()
        assert "UNA" not in tags
        assert "" not in tags

    def test_segment_order_preserved(self, orders_text):
        msg = parse_message(orders_text)
        assert msg.segment_tags()[0] == "UNB"
        assert msg.segment_tags()[-1] == "UNZ"

    def test_raw_preserved(self, orders_text):
        msg = parse_message(orders_text)
        assert msg.raw == orders_text

    def test_segment_count(self, orders_text):
        msg = parse_message(orders_text)
        # UNB UNH BGM DTM NAD NAD LIN QTY LIN QTY UNS UNT UNZ = 13
        assert len(msg.segments) == 13

    def test_returns_message_type(self, orders_text):
        assert isinstance(parse_message(orders_text), Message)
