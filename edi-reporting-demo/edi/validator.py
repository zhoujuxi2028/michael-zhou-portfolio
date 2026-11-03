"""EDIFACT 报文校验层 (validator)

基于 MessageSchema 对已解析的 Message 做**结构校验**，并做错误分级。

校验维度（由浅入深）：
1. 结构层：未知段 Tag、段出现次数超限
2. 语法层：必填段缺失
3. 业务层：必填元素/字段缺失

错误分级（Severity）：
- ERROR   : 硬错误，无法继续，需隔离/人工介入
- WARNING : 可容忍，记录后继续（如未知但非必填的段）

校验结果携带精确定位（段序号 + Tag + 字段位置），保证可追溯。
"""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum

from parser import Message
from schema import MessageSchema


class Severity(str, Enum):
    ERROR = "ERROR"
    WARNING = "WARNING"


@dataclass(frozen=True)
class Issue:
    """单条校验问题，带精确定位。"""

    severity: Severity
    code: str
    message: str
    segment_index: int | None = None  # 段序号（从 0 起）
    tag: str | None = None


@dataclass
class ValidationResult:
    """校验结果汇总。"""

    message_name: str
    issues: list[Issue] = field(default_factory=list)

    @property
    def is_valid(self) -> bool:
        """无 ERROR 即视为通过。"""
        return not any(i.severity == Severity.ERROR for i in self.issues)

    @property
    def errors(self) -> list[Issue]:
        return [i for i in self.issues if i.severity == Severity.ERROR]

    @property
    def warnings(self) -> list[Issue]:
        return [i for i in self.issues if i.severity == Severity.WARNING]

    def __str__(self) -> str:
        if not self.issues:
            return f"[{self.message_name}] ✅ 校验通过"
        lines = [f"[{self.message_name}] 校验完成: {len(self.errors)} 错误 / {len(self.warnings)} 警告"]
        for i in self.issues:
            loc = f"段{i.segment_index} [{i.tag}]" if i.segment_index is not None else "-"
            lines.append(f"  [{i.severity.value}] {i.code} @ {loc}: {i.message}")
        return "\n".join(lines)


def validate(message: Message, schema: MessageSchema) -> ValidationResult:
    """对已解析的 Message 做 schema 驱动的结构校验。"""
    result = ValidationResult(message_name=schema.name)
    known = schema.known_tags()

    for idx, seg in enumerate(message.segments):
        spec = schema.spec_for(seg.tag)

        # 1) 结构层：未知段 Tag
        if spec is None:
            result.issues.append(
                Issue(
                    severity=Severity.WARNING,
                    code="UNKNOWN_SEGMENT",
                    message=f"未知段 Tag，不在 {schema.name} schema 中，已忽略",
                    segment_index=idx,
                    tag=seg.tag,
                )
            )
            continue

        # 2) 结构层：段出现次数超限（仅对限次数的段做）
        if spec.max_occurrence is not None:
            occurrences = sum(1 for s in message.segments if s.tag == spec.tag)
            if occurrences > spec.max_occurrence:
                result.issues.append(
                    Issue(
                        severity=Severity.ERROR,
                        code="SEGMENT_OCCURRENCE",
                        message=(
                            f"段 [{spec.tag}] 出现 {occurrences} 次，"
                            f"超过 schema 上限 {spec.max_occurrence}"
                        ),
                        segment_index=idx,
                        tag=spec.tag,
                    )
                )

    # 3) 语法层：必填段缺失
    for spec in schema.segments:
        if spec.required and spec.tag not in message.segment_tags():
            result.issues.append(
                Issue(
                    severity=Severity.ERROR,
                    code="MISSING_REQUIRED",
                    message=f"必填段 [{spec.tag}] ({spec.description}) 缺失",
                    tag=spec.tag,
                )
            )

    return result
