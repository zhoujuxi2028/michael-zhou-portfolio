"""EDIFACT 通用解析层 (parser)

只负责把 EDIFACT 纯文本切成结构化对象，**不包含任何业务语义**。
所有业务知识（段含义、限定符、必填性）都在 schema.py 与 validator.py 中。

设计原则：
- 纯函数、无副作用、可测试
- 只处理字符流 -> 数据结构 的机械转换
- 不依赖任何第三方库
"""
from __future__ import annotations

from dataclasses import dataclass, field

# EDIFACT 默认分隔符（可由 UNA 段覆盖）
SEGMENT_TERMINATOR = "'"
ELEMENT_SEPARATOR = "+"
COMPONENT_SEPARATOR = ":"


@dataclass(frozen=True)
class Segment:
    """一个 EDIFACT 段。

    tag: 段标识符（如 BGM、LIN）
    elements: 元素列表；每个元素是一个成分列表
        elements[i][j] 表示第 i 个元素（从 0 起）的第 j 个成分
    例: "BGM+220+PO-2026-0001+9'"
        -> tag="BGM", elements=[["220"], ["PO-2026-0001"], ["9"]]
    """

    tag: str
    elements: list[list[str]]

    def element(self, idx: int) -> list[str]:
        """返回第 idx 个元素（成分列表），越界返回空列表。"""
        if 0 <= idx < len(self.elements):
            return self.elements[idx]
        return []

    def component(self, element_idx: int, comp_idx: int) -> str:
        """返回第 element_idx 个元素的第 comp_idx 个成分，越界返回空串。"""
        elem = self.element(element_idx)
        if 0 <= comp_idx < len(elem):
            return elem[comp_idx]
        return ""


@dataclass
class Message:
    """一条完整的 EDIFACT 消息。

    segments: 按顺序排列的段列表（不含 UNA）
    raw: 原始文本（用于排障/重放）
    """

    segments: list[Segment] = field(default_factory=list)
    raw: str = ""

    def segment_tags(self) -> list[str]:
        """返回所有段的 Tag 顺序，便于计数与顺序校验。"""
        return [s.tag for s in self.segments]


def _split_una(text: str) -> tuple[str | None, str]:
    """分离 UNA 服务串通知（若存在）与正文。

    UNA 段声明实际使用的分隔符（如 "UNA:+.? '"）。
    本实现遵循 EDIFACT 标准默认分隔符，UNA 仅做跳过处理，
    若需支持自定义分隔符可在此扩展。
    """
    for raw in text.split(SEGMENT_TERMINATOR):
        seg = raw.strip()
        if not seg:
            continue
        if seg.startswith("UNA"):
            return seg, text
    return None, text


def parse_segment(segment: str) -> Segment:
    """把单个段字符串解析为 Segment 对象。"""
    parts = segment.split(ELEMENT_SEPARATOR)
    tag = parts[0].strip()
    elements = [p.split(COMPONENT_SEPARATOR) for p in parts[1:]]
    return Segment(tag=tag, elements=elements)


def parse_message(text: str) -> Message:
    """把完整 EDIFACT 报文文本解析为 Message 对象。

    按段终止符切段 -> 逐段解析 -> 跳过空段与 UNA 段。
    """
    segments: list[Segment] = []
    for raw in text.split(SEGMENT_TERMINATOR):
        seg = raw.strip()
        if not seg:
            continue
        if seg.startswith("UNA"):
            continue
        segments.append(parse_segment(seg))
    return Message(segments=segments, raw=text)
