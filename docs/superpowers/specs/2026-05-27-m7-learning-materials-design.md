# M7 学习材料最小收尾设计

## 背景

`docs/copilot-cli-journey/` 中的 M7 主模块、Prompt 模板库和实战示例已经存在，内容覆盖上下文地图、范围收敛、多文件计划、diff 审查和长会话压缩。当前任务不做深度扩写，而是完成最小收尾，让 M7 在学习中心中呈现为已完成、可导航、可复查的模块。

## 目标

1. 将 M7 从“进行中/待扩展”状态同步为已完成或明确完成。
2. 补齐必要的完成记录，说明 M7 已包含哪些学习材料。
3. 保持现有内容结构，不重写主模块，不扩展到 M8/M9。
4. 验证链接、占位词和状态描述没有明显不一致。

## 范围

### 必须修改

- `docs/copilot-cli-journey/docs/phase3/README.md`：同步 M7 模块状态和支持资料说明。
- `docs/copilot-cli-journey/README.md` 或 `INDEX.md`：如仍把 Phase 3/M7 描述为待扩展，需要改成更准确的状态。
- `docs/copilot-cli-journey/docs/phase3/M7-COMPLETION-SUMMARY.md`：新增简短完成总结，记录完成范围、材料清单和后续建议。

### 可能修改

- `docs/copilot-cli-journey/examples/phase3/README.md`：如果导航缺少 M7 示例入口，则补齐。
- `docs/copilot-cli-journey/modules/phase3/M7-context-management.md`：仅在发现状态、日期或明显链接问题时做最小修正。

### 不应修改

- Phase 1、Phase 2 已完成材料。
- M8、M9 正文内容。
- 与 M7 无关的示例代码、配置或脚本。

## 设计方案

采用“最小收尾”方案：

1. 先检查 M7 相关文档和导航中的状态描述。
2. 将 M7 的状态从“进行中”更新为“已完成”，但保留 M8/M9 的后续状态。
3. 新增一份简短完成总结，列出主模块、Prompt 库、实战示例和导航状态。
4. 运行针对性文本检查，确认没有 M7 相关占位词、错误路径或明显状态冲突。

该方案避免重写已有 M7 主体内容，改动范围小，适合当前“minimal_finish”要求。

## 验证标准

- M7 在 Phase 3 导航中显示为完成。
- M7 完成总结链接到主模块、Prompt 库和示例目录。
- 没有引入 Phase 1/Phase 2/M8/M9 的无关改动。
- `M7` 相关文档中不存在明显 `TODO`、`TBD` 或占位模板文本。

## 风险与处理

- **状态过度声明**：只声明 M7 完成，不声明整个 Phase 3 完成。
- **导航遗漏**：检查 README、Phase 3 README 和示例 README 是否需要同步。
- **范围膨胀**：不补写深度练习，不新增复杂案例，把扩展内容留给后续迭代。
