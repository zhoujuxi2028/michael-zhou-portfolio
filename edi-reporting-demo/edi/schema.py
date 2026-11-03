"""EDIFACT 报文 Schema 定义层

用**声明式配置**描述每种报文类型的结构：哪些段、是否必填、可重复次数、
以及限定符（qualifier）的业务含义。

新增一种报文类型（如 DESADV、INVOIC）只需在此**新增一份配置 + 注册映射**，
无需改动 parser 或 validator 的核心逻辑。
"""
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class SegmentSpec:
    """一个段的规格描述。

    tag: 段标识符（如 BGM、LIN）
    required: 是否必填
    max_occurrence: 最大出现次数（None 表示不限，LIN/QTY 等明细行可重复）
    description: 业务含义（用于日志/报错可读性）
    """

    tag: str
    required: bool = False
    max_occurrence: int | None = 1
    description: str = ""


@dataclass(frozen=True)
class MessageSchema:
    """一类报文（如 ORDERS）的完整规格。"""

    name: str
    segments: tuple[SegmentSpec, ...]
    # 限定符字典: {段 Tag: {限定符码: 业务含义}}
    qualifiers: dict[str, dict[str, str]] = field(default_factory=dict)

    def spec_for(self, tag: str) -> SegmentSpec | None:
        """按 Tag 查找段规格，未注册则返回 None。"""
        for spec in self.segments:
            if spec.tag == tag:
                return spec
        return None

    def known_tags(self) -> set[str]:
        return {s.tag for s in self.segments}


# ---------------------------------------------------------------------------
# 通用限定符（跨报文类型复用）
# ---------------------------------------------------------------------------
DTM_QUALIFIERS = {
    "137": "单据日期",
    "102": "交付日期",
    "2": "请求交付日期",
}

NAD_QUALIFIERS = {
    "BY": "买方 Buyer",
    "SU": "供应商 Supplier",
    "SE": "卖方 Seller",
}

QTY_QUALIFIERS = {
    "21": "订购数量 Ordered",
    "12": "发货数量 Despatch",
    "113": "收货数量 Received",
}

# ---------------------------------------------------------------------------
# ORDERS 订单报文
# ---------------------------------------------------------------------------
ORDERS_SCHEMA = MessageSchema(
    name="ORDERS",
    segments=(
        SegmentSpec("UNB", required=True, description="交换头(收发双方/时间戳)"),
        SegmentSpec("UNH", required=True, description="消息头(报文类型)"),
        SegmentSpec("BGM", required=True, description="报文开始(单据号)"),
        SegmentSpec("DTM", required=False, max_occurrence=None, description="日期/时间"),
        SegmentSpec("NAD", required=False, max_occurrence=None, description="参与方"),
        SegmentSpec("LIN", required=True, max_occurrence=None, description="商品行"),
        SegmentSpec("QTY", required=False, max_occurrence=None, description="数量"),
        SegmentSpec("UNS", required=False, description="明细/汇总分界"),
        SegmentSpec("UNT", required=True, description="消息尾(段计数)"),
        SegmentSpec("UNZ", required=True, description="交换尾(消息计数)"),
    ),
    qualifiers={
        "DTM": DTM_QUALIFIERS,
        "NAD": NAD_QUALIFIERS,
        "QTY": QTY_QUALIFIERS,
    },
)

# ---------------------------------------------------------------------------
# DESADV 发货通知（扩展示例：新增报文只需配置 + 注册）
# ---------------------------------------------------------------------------
DESADV_SCHEMA = MessageSchema(
    name="DESADV",
    segments=(
        SegmentSpec("UNB", required=True, description="交换头"),
        SegmentSpec("UNH", required=True, description="消息头"),
        SegmentSpec("BGM", required=True, description="发货通知单据号"),
        SegmentSpec("DTM", required=False, max_occurrence=None, description="日期"),
        SegmentSpec("NAD", required=False, max_occurrence=None, description="参与方"),
        SegmentSpec("CPS", required=False, max_occurrence=None, description="托运层级"),
        SegmentSpec("LIN", required=True, max_occurrence=None, description="商品行"),
        SegmentSpec("QTY", required=False, max_occurrence=None, description="数量"),
        SegmentSpec("UNT", required=True, description="消息尾"),
        SegmentSpec("UNZ", required=True, description="交换尾"),
    ),
    qualifiers={
        "DTM": DTM_QUALIFIERS,
        "NAD": NAD_QUALIFIERS,
        "QTY": QTY_QUALIFIERS,
    },
)

# ---------------------------------------------------------------------------
# INVOIC 发票（扩展示例）
# ---------------------------------------------------------------------------
INVOIC_SCHEMA = MessageSchema(
    name="INVOIC",
    segments=(
        SegmentSpec("UNB", required=True, description="交换头"),
        SegmentSpec("UNH", required=True, description="消息头"),
        SegmentSpec("BGM", required=True, description="发票号"),
        SegmentSpec("DTM", required=False, max_occurrence=None, description="日期"),
        SegmentSpec("NAD", required=False, max_occurrence=None, description="参与方"),
        SegmentSpec("LIN", required=True, max_occurrence=None, description="行项目"),
        SegmentSpec("MOA", required=False, max_occurrence=None, description="金额"),
        SegmentSpec("UNT", required=True, description="消息尾"),
        SegmentSpec("UNZ", required=True, description="交换尾"),
    ),
    qualifiers={
        "DTM": DTM_QUALIFIERS,
        "NAD": NAD_QUALIFIERS,
    },
)

# 报文类型注册表：UNH 段里的报文类型标识 -> Schema
SCHEMA_REGISTRY: dict[str, MessageSchema] = {
    "ORDERS": ORDERS_SCHEMA,
    "DESADV": DESADV_SCHEMA,
    "INVOIC": INVOIC_SCHEMA,
}


def get_schema(message_type: str) -> MessageSchema | None:
    """按报文类型标识（如 ORDERS）返回对应 Schema，未知类型返回 None。"""
    return SCHEMA_REGISTRY.get(message_type.upper())
