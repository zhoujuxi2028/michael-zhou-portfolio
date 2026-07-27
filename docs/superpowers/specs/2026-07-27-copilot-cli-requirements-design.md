# Spec: Copilot CLI 学习路径需求清单 + GitHub Issues 追踪

**日期**：2026-07-27  
**状态**：已审批  

---

## 背景

`docs/copilot-cli-journey/` 包含 15 个学习模块（M1–M15），分 5 个 Phase。Phase 1–2 已完成，M7 完成实战验证，M8–M15 仍处于"初稿完成"或"待实践"状态。需要建立一套编号体系和追踪机制，把所有未完成需求系统化管理。

---

## 目标

1. 为所有未完成需求分配唯一编号（格式：`REQ-MX-NN`）
2. 在本地 `REQUIREMENTS.md` 维护单一需求清单
3. 为每条需求创建对应 GitHub Issue，双向链接
4. 标签统一为 `copilot-cli-journey`

---

## 需求编号规则

格式：`REQ-M{模块号}-{序号两位数}`

- `REQ-M8-01`：M8 模块的第 1 条需求
- `REQ-M0-01`：跨模块/跨 Phase 横切需求（模块号用 `M0`）

---

## 需求清单（13 条）

| 编号 | 描述 | Phase | 优先级 |
|------|------|-------|--------|
| REQ-M5-01  | M5-doc-generation.md 末尾添加「学习成果」section，汇总 m5/summaries/ 关键结论，使模块页成为完整独立入口 | Phase 2 | 中 |
| REQ-M8-01  | M8 工作流集成 — 实战验证记录 | Phase 3 | 高 |
| REQ-M9-01  | M9 调试与脚本 — 实战验证记录 | Phase 3 | 高 |
| REQ-M0-02  | Phase 3 支持文档 — M8/M9 验证后补充 Prompt 库和完成总结到 docs/phase3/ | Phase 3 | 低 |
| REQ-M10-01 | M10 API测试集成 — 实战验证 + 真实项目案例 | Phase 4 | 中 |
| REQ-M11-01 | M11 E2E测试集成 — 实战验证 + 真实项目案例 | Phase 4 | 中 |
| REQ-M12-01 | M12 性能测试集成 — 实战验证 + 真实项目案例 | Phase 4 | 中 |
| REQ-M0-03  | Phase 4 支持文档（docs/phase4/）+ 示例补充，参照 Phase 1-3 模式建立完成总结/Prompt 库/示例 | Phase 4 | 低 |
| REQ-M13-01 | M13 Copilot Workspace — 实践沉淀 | Phase 5 | 中 |
| REQ-M14-01 | M14 团队标准 — 实践沉淀 | Phase 5 | 中 |
| REQ-M15-01 | M15 知识库总结 — 实践沉淀 | Phase 5 | 中 |
| REQ-M0-04  | Phase 5 支持文档（docs/phase5/）+ 示例补充，参照 Phase 1-3 模式建立完成总结/Prompt 库/示例 | Phase 5 | 低 |
| REQ-M0-01  | 统一旧路径引用（docs/learning/ → 当前路径） | 横切 | 低 |

---

## 本地追踪文档

**路径**：`docs/copilot-cli-journey/REQUIREMENTS.md`

**结构**：

```markdown
| 编号 | 描述 | 状态 | GitHub Issue | Phase | 优先级 |
```

状态图标：🔴 待开始 | 🟡 进行中 | ✅ 已完成

---

## GitHub Issues 规范

- **仓库**：`zhoujuxi2028/michael-zhou-portfolio`
- **Label**：`copilot-cli-journey`（新建，颜色 `#0075ca`）
- **Title 格式**：`[REQ-M8-01] M8 工作流集成 — 实战验证记录`
- **Body 模板**：
  ```
  **Phase**: Phase X | **Priority**: 高/中/低

  ## 需求描述
  <一句话描述>

  ## 完成标准
  - [ ] <可验证条目>

  ## 追踪文档
  [REQUIREMENTS.md](https://github.com/zhoujuxi2028/michael-zhou-portfolio/blob/main/docs/copilot-cli-journey/REQUIREMENTS.md)
  ```
- Issue 创建后，编号回填到 `REQUIREMENTS.md` 的 Issue 列

---

## 执行步骤

1. 创建 GitHub Label `copilot-cli-journey`
2. 创建 13 条 GitHub Issues
3. 写入 `docs/copilot-cli-journey/REQUIREMENTS.md`（含 Issue 编号）
4. Commit + Push
