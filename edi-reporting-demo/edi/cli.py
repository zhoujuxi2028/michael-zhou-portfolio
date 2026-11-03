#!/usr/bin/env python3
"""EDI 解析命令行入口

组合 parser -> schema -> validator 三层，解析并校验 EDIFACT 报文。

用法:
    python cli.py                     # 解析 fixtures/sample_orders.edi
    python cli.py <file>              # 解析指定文件
    python cli.py <file> --type DESADV  # 指定报文类型（默认自动识别 ORDERS）
"""
from __future__ import annotations

import sys
from pathlib import Path

from parser import Message, parse_message, parse_segment
from schema import get_schema
from validator import validate


def _detect_message_type(message: Message) -> str | None:
    """从 UNH 段提取报文类型标识（UNH 第 2 个元素第 1 个成分，如 ORDERS）。"""
    for seg in message.segments:
        if seg.tag == "UNH":
            elem = seg.element(1)
            if elem:
                return elem[0]
    return None


def _render(message: Message, schema) -> None:
    """打印解析结果（复用 schema 的限定符做语义翻译）。"""
    print(f"=== 解析报文: {schema.name} ===\n")
    for seg in message.segments:
        spec = schema.spec_for(seg.tag)
        desc = spec.description if spec else "(未知段)"
        print(f"[{seg.tag}] {desc}")

        qualifiers = schema.qualifiers.get(seg.tag, {})

        if seg.tag == "UNB":
            print(f"   发送方: {seg.component(1, 0)}  接收方: {seg.component(2, 0)}  时间: {'/'.join(seg.element(3))}")
        elif seg.tag == "BGM":
            print(f"   单据号: {seg.component(1, 0)}")
        elif seg.tag == "DTM":
            q = seg.component(0, 0)
            val = seg.component(0, 1)
            print(f"   {qualifiers.get(q, q)}: {val}")
        elif seg.tag == "NAD":
            q = seg.component(0, 0)
            print(f"   {qualifiers.get(q, q)}: {seg.component(1, 0)}")
        elif seg.tag == "LIN":
            print(f"   行号: {seg.component(0, 0)}  商品: {seg.component(2, 0)}")
        elif seg.tag == "QTY":
            q = seg.component(0, 0)
            qty = seg.component(0, 1)
            unit = seg.component(0, 2)
            print(f"   {qualifiers.get(q, q)}: {qty} {unit}")
        print()

    print("=== 解析完成 ===\n")
    print("关键洞察: EDI = 纯文本 + 分隔符 + 段Tag，物流核心链路 = 订单(ORDERS) -> 发货通知(DESADV) -> 收货确认")


def main() -> int:
    args = sys.argv[1:]
    path = "fixtures/sample_orders.edi"
    forced_type = None

    if "--type" in args:
        i = args.index("--type")
        forced_type = args[i + 1]
        args = args[:i] + args[i + 2:]
    if args:
        path = args[0]

    if not Path(path).exists():
        print(f"文件不存在: {path}", file=sys.stderr)
        return 1

    text = Path(path).read_text(encoding="utf-8")
    message = parse_message(text)

    msg_type = forced_type or _detect_message_type(message)
    schema = get_schema(msg_type or "")
    if schema is None:
        print(f"无法识别报文类型: {msg_type!r}（UNH 段缺失或未注册）", file=sys.stderr)
        return 1

    result = validate(message, schema)
    _render(message, schema)
    print("\n" + str(result))
    return 0 if result.is_valid else 1


if __name__ == "__main__":
    raise SystemExit(main())
