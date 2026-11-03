"""契约测试 (Contract Tests)

用**黄金样本 (golden file)** 锁定解析结果，防止重构或扩展时
破坏既有报文类型的解析行为。

契约约定：对同一份 ORDERS 报文，以下关键字段的解析值必须保持不变。
新增报文类型（DESADV/INVOIC）时，跑本套用例确认 ORDERS 未被破坏。
"""
from __future__ import annotations

from parser import parse_message


def _extract_orders_contract(orders_text: str) -> dict:
    """从 ORDERS 报文提取关键业务字段，作为契约基准。"""
    msg = parse_message(orders_text)
    contract = {
        "sender": None,
        "receiver": None,
        "order_number": None,
        "buyer": None,
        "supplier": None,
        "line_items": [],  # [(line_no, sku)]
        "quantities": [],  # [(qualifier, qty, unit)]
    }
    for seg in msg.segments:
        if seg.tag == "UNB":
            contract["sender"] = seg.component(1, 0)
            contract["receiver"] = seg.component(2, 0)
        elif seg.tag == "BGM":
            contract["order_number"] = seg.component(1, 0)
        elif seg.tag == "NAD":
            q = seg.component(0, 0)
            if q == "BY":
                contract["buyer"] = seg.component(1, 0)
            elif q == "SU":
                contract["supplier"] = seg.component(1, 0)
        elif seg.tag == "LIN":
            contract["line_items"].append((seg.component(0, 0), seg.component(2, 0)))
        elif seg.tag == "QTY":
            contract["quantities"].append(
                (seg.component(0, 0), seg.component(0, 1), seg.component(0, 2))
            )
    return contract


class TestOrdersContract:
    """ORDERS 报文的黄金契约：解析值必须稳定。"""

    def test_header_fields(self, orders_text):
        c = _extract_orders_contract(orders_text)
        assert c["sender"] == "SUPPLIER01"
        assert c["receiver"] == "BUYER01"

    def test_order_number(self, orders_text):
        c = _extract_orders_contract(orders_text)
        assert c["order_number"] == "PO-2026-0001"

    def test_parties(self, orders_text):
        c = _extract_orders_contract(orders_text)
        assert c["buyer"] == "BUYER123"
        assert c["supplier"] == "WAREHOUSE456"

    def test_line_items(self, orders_text):
        c = _extract_orders_contract(orders_text)
        assert c["line_items"] == [("1", "SKU1001"), ("2", "SKU1002")]

    def test_quantities(self, orders_text):
        c = _extract_orders_contract(orders_text)
        assert c["quantities"] == [
            ("21", "100", "PCE"),
            ("21", "50", "PCE"),
        ]
