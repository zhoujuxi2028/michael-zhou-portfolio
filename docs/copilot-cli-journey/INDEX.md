# 学习路径导航

这份导航只解决一件事：**帮你快速找到最适合自己的学习入口**。

---

## 1. 按目标选择

### 目标 A：先把 Copilot CLI 用起来
**推荐顺序**

1. [M1：Copilot CLI 基础](./modules/phase1/M1-copilot-cli-basics.md)
2. [M2：提示工程基础](./modules/phase1/M2-prompting-fundamentals.md)
3. [M3：Copilot CLI 生态初探](./modules/phase1/M3-cli-ecosystem.md)

**你会得到**
- 知道 CLI 能做什么
- 会写基本 Prompt
- 能把 Copilot 接入常见命令行工作流

---

### 目标 B：重点学习“测试 / 文档 / 审查”
**推荐顺序**

1. Phase 1 全部（M1-M3）
2. [M4：测试代码生成最佳实践](./modules/phase2/M4-test-generation.md)
3. [M5：文档和注释生成工作流](./modules/phase2/M5-doc-generation.md)
4. [M6：代码审查加速](./modules/phase2/M6-code-review-workflow.md)

**补充必看**
- [M5 完成总结](./M5-COMPLETION-SUMMARY.md)
- [M5 深化学习总结](./M5-DEEPDIVE-COMPLETE.md)

**适合**
- QA
- SDET
- 需要提升日常交付效率的开发者

---

### 目标 C：先看已经比较成熟的内容
**推荐顺序**

1. [Phase 1 模块](./modules/phase1/)
2. [Phase 1 支持文档](./docs/phase1/README.md)
3. [M5 完成总结](./M5-COMPLETION-SUMMARY.md)
4. [M5 FastAPI / gRPC / GraphQL 专题](./M5-TECH-STACK-COMPARISON.md)

**适合**
- 想先看“成型内容”，再决定是否深入的人

---

## 2. 按当前完成度选择

| 你想看什么 | 建议入口 |
|------------|----------|
| 完整模块 | `modules/phase1/` |
| 完整阶段支持资料 | `docs/phase1/` |
| 完整专题总结 | `M5-COMPLETION-SUMMARY.md`、`M5-DEEPDIVE-COMPLETE.md` |
| 技术专题对比 | `M5-TECH-STACK-COMPARISON.md`、`M5-TECH1/2/3` |
| 占位中的后续规划 | `modules/phase2/` ~ `modules/phase5/` |

---

## 3. 按角色选择

### QA / 功能测试工程师
**建议路径**

`M1 → M2 → M3 → M4 → M5 → M10 → M11`

**重点原因**
- M4 对应测试生成
- M5 对应文档产出
- M10/M11 对应 API 和 E2E 集成场景

---

### SDET / 测试开发工程师
**建议路径**

`M1 → M2 → M3 → M4 → M7 → M8 → M9 → M10 → M11 → M12`

**重点原因**
- 既要学 Prompt，也要学上下文管理与调试
- 更关注自动化、脚本集成与项目落地

---

### 开发者 / 技术负责人
**建议路径**

`M1 → M2 → M5 → M6 → M8 → M14 → M15`

**重点原因**
- 更适合关注文档、审查、流程规范与知识沉淀

---

## 4. 按时间选择

### 只有半天
- 看 [README.md](./README.md)
- 看 [M1](./modules/phase1/M1-copilot-cli-basics.md)
- 快速浏览 [INDEX.md](./INDEX.md)

### 只有 1 周
- 完成 `M1 + M2 + M3`
- 再从 `M4 / M5 / M6` 中选一个与你工作最相关的主题

### 有 2-4 周
- 完成 Phase 1
- 深入 Phase 2
- 结合 M5 专题资料做一次小型实践

### 想长期建设个人知识库
- 按 `M1 → M15` 的顺序逐步完善
- 同步沉淀支持文档、示例、专题总结

---

## 5. 关键资料索引

### 基础必读
- [README.md](./README.md)
- [STRUCTURE.md](./STRUCTURE.md)
- [template.md](./template.md)

### 已较完整的学习内容
- [M1：Copilot CLI 基础](./modules/phase1/M1-copilot-cli-basics.md)
- [M2：提示工程基础](./modules/phase1/M2-prompting-fundamentals.md)
- [M3：Copilot CLI 生态初探](./modules/phase1/M3-cli-ecosystem.md)
- [Phase 1 支持文档](./docs/phase1/README.md)

### 文档生成专题（M5）
- [M5 完成总结](./M5-COMPLETION-SUMMARY.md)
- [M5 深化学习总结](./M5-DEEPDIVE-COMPLETE.md)
- [M5 技术栈对比](./M5-TECH-STACK-COMPARISON.md)
- [M5 FastAPI 专题](./M5-TECH1-FastAPI.md)
- [M5 gRPC 专题](./M5-TECH2-gRPC.md)
- [M5 GraphQL 专题](./M5-TECH3-GraphQL.md)
- [M5 Python Docstring Prompt 集](./M5-python-google-docstring-prompts.md)

---

## 6. 一句话建议

- **第一次看**：先读 `README`
- **想找路线**：看 `INDEX`
- **想真正开始学**：从 `modules/phase1/M1` 开始
- **想看成熟成果**：优先看 `M5-*`

---

*最后更新：2026-04-28*
