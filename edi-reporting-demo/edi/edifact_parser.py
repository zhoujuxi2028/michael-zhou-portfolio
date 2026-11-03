#!/usr/bin/env python3
"""EDIFACT ORDERS 报文解析器 Demo
用于 DSV 114361 Senior IT Specialist 面试准备。
解析 EDIFACT 纯文本报文：按段分隔符切段 -> 按元素分隔符切元素 -> 映射常见段。

用法:
    python edifact_parser.py [file]
"""
import sys

SEGMENT_TERMINATOR = "'"
ELEMENT_SEPARATOR = "+"
COMPONENT_SEPARATOR = ":"

SEGMENT_MEANING = {
    "UNA": "服务串通知(分隔符声明)",
    "UNB": "交换头(收发双方/时间戳)",
    "UNH": "消息头(报文类型)",
    "BGM": "报文开始(单据号)",
    "DTM": "日期/时间",
    "NAD": "名称与地址(参与方)",
    "LIN": "行项目(商品行)",
    "QTY": "数量",
    "UNS": "段分隔(明细/汇总分界)",
    "UNT": "消息尾(段计数)",
    "UNZ": "交换尾(消息计数)",
}

# DTM 限定符含义 (EDIFACT 2005 日期限定符)
DTM_QUALIFIER = {
    "137": "单据日期",
    "102": "交付日期",
    "2": "请求交付日期",
}

# NAD 参与方限定符
NAD_QUALIFIER = {
    "BY": "买方 Buyer",
    "SU": "供应商 Supplier",
    "SE": "卖方 Seller",
}

# QTY 数量限定符
QTY_QUALIFIER = {
    "21": "订购数量 Ordered",
    "12": "发货数量 Despatch",
    "113": "收货数量 Received",
}


def parse_segment(segment: str):
    """返回 (tag, [elements...])，元素内的成分已按 : 拆分。"""
    parts = segment.split(ELEMENT_SEPARATOR)
    tag = parts[0].strip()
    elements = [p.split(COMPONENT_SEPARATOR) for p in parts[1:]]
    return tag, elements


def parse_edi(text: str):
    """切掉 UNA 行，按段终止符切段。"""
    segments = []
    for raw in text.split(SEGMENT_TERMINATOR):
        seg = raw.strip()
        if not seg:
            continue
        if seg.startswith("UNA"):
            continue
        segments.append(seg)
    return segments


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "sample_orders.edi"
    with open(path, encoding="utf-8") as f:
        text = f.read()

    print(f"=== 解析报文: {path} ===\n")
    for seg in parse_edi(text):
        tag, elements = parse_segment(seg)
        meaning = SEGMENT_MEANING.get(tag, "")
        print(f"[{tag}] {meaning}")

        if tag == "UNB":
            print(f"   发送方: {elements[1][0]}  接收方: {elements[2][0]}  时间: {elements[3]}")
        elif tag == "BGM":
            print(f"   单据号: {elements[1][0]}")
        elif tag == "DTM":
            q = elements[0][0] if elements and elements[0] else ""
            val = elements[0][1] if elements and len(elements[0]) > 1 else ""
            print(f"   {DTM_QUALIFIER.get(q, q)}: {val}")
        elif tag == "NAD":
            q = elements[0][0] if elements and elements[0] else ""
            print(f"   {NAD_QUALIFIER.get(q, q)}: {elements[1][0] if len(elements) > 1 else ''}")
        elif tag == "LIN":
            print(f"   行号: {elements[0][0]}  商品: {elements[2][0] if len(elements) > 2 else ''}")
        elif tag == "QTY":
            q = elements[0][0] if elements and elements[0] else ""
            qty = elements[0][1] if elements and len(elements[0]) > 1 else ""
            unit = elements[0][2] if elements and len(elements[0]) > 2 else ""
            print(f"   {QTY_QUALIFIER.get(q, q)}: {qty} {unit}")
        print()

    print("=== 解析完成 ===\n")
    print("关键洞察: EDI = 纯文本 + 分隔符 + 段Tag，物流核心链路 = 订单(ORDERS) -> 发货通知(DESADV/ASN) -> 收货确认")


if __name__ == "__main__":
    main()
