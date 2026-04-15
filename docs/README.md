# 📚 Michael Zhou Portfolio — 文档导航

欢迎来到项目文档中心！这里汇集了所有项目相关的文档、指南和最佳实践。

---

## 🎯 快速导航

### 🚀 学习路径
**深度学习 GitHub Copilot CLI 的完整知识库**

- **[Copilot CLI 学习之旅](./copilot-cli-journey/)** — 15 个模块的系统化学习路径
  - Phase 1: 核心基础 (M1-M3)
  - Phase 2: QA 工作流优化 (M4-M6) ← **你在这里**
  - Phase 3-5: 进阶和扩展

---

### 📖 项目文档

#### [项目架构](./PROJECT-STRUCTURE.md)
项目整体结构、模块划分、技术栈说明。

#### [最佳实践](./BEST-PRACTICES/)
- [实现清单](./BEST-PRACTICES/implementation-checklist.md) — 项目开发的检查清单
- [计划模板](./BEST-PRACTICES/templates/plan-template.md) — 项目规划模板

---

## 📂 文档结构

```
docs/
├── README.md (你在这里)
├── PROJECT-STRUCTURE.md          项目整体架构
├── BEST-PRACTICES/               最佳实践指南
│   ├── implementation-checklist.md
│   └── templates/
│       └── plan-template.md
│
└── copilot-cli-journey/          学习路径（详见下方）
    ├── README.md                 学习入口
    ├── INDEX.md                  完整导航
    ├── modules/                  学习模块（M1-M15）
    ├── docs/                     支持文档（按 phase）
    └── examples/                 代码示例（按 phase）
```

---

## 🎓 Copilot CLI 学习中心

### 当前进度

| Phase | 模块 | 状态 | 位置 |
|-------|------|------|------|
| **Phase 1** | M1-M3 | ✅ 完成 | `modules/phase1/` |
| **Phase 2** | M4: 测试生成 | ✅ 完成 | `modules/phase2/M4-*` |
| | M5: 文档生成 | ✅ 完成 | `modules/phase2/M5-*` |
| | M6: 代码审查 | ⏳ 筹划中 | `modules/phase2/M6-*` |
| **Phase 3-5** | M7-M15 | 📋 待开始 | `modules/phase3-5/` |

### Phase 2 支持资源

#### ✅ M4 测试生成完成总结
- [完成报告](./copilot-cli-journey/docs/phase2/M4-COMPLETION-SUMMARY.md)
- [迁移完成](./copilot-cli-journey/docs/phase2/M4-MIGRATION-COMPLETE.md)
- [Jest 实战项目](./copilot-cli-journey/examples/phase2/jest-demo/) — 40+ 测试，85% 覆盖率
- [Pytest 实战项目](./copilot-cli-journey/examples/phase2/pytest-demo/) — 35+ 测试，84% 覆盖率
- [测试 Prompt 库](./copilot-cli-journey/docs/phase2/prompts/) — Jest/Pytest/通用策略

#### ✅ M5 文档生成完成总结
- [完成报告](./copilot-cli-journey/docs/phase2/M5-COMPLETION-SUMMARY.md)
- [深化学习](./copilot-cli-journey/docs/phase2/M5-DEEPDIVE-COMPLETE.md)
- [技术栈对比](./copilot-cli-journey/docs/phase2/tech-stacks/)
  - FastAPI 自动 API 文档
  - gRPC 和 Protocol Buffers
  - GraphQL Schema 和内省
- [Python Prompt 库](./copilot-cli-journey/docs/phase2/prompts/python-google-docstring.md)

#### ⏳ M6 代码审查（筹划中）
- [理论框架](./copilot-cli-journey/modules/phase2/M6-code-review-workflow.md) — 待完成
- 代码审查示例
- Git Workflow 集成

---

## 🔗 常用链接

- **学习入口**: [Copilot CLI 学习路径](./copilot-cli-journey/README.md)
- **完整导航**: [INDEX.md](./copilot-cli-journey/INDEX.md) — 所有 15 个模块的快速链接
- **项目首页**: [../../README.md](../README.md)

---

## 📝 文档维护说明

### 添加新文档
1. 确定文档属于哪个 phase（Phase 1-5）
2. 放在对应的 `docs/phaseX/` 或 `modules/phaseX/` 目录
3. 更新 INDEX.md 添加链接
4. 如有支持文件（代码示例、完成报告），放在对应的 `examples/phaseX/` 或 `docs/phaseX/`

### 更新链接
所有文档使用 **相对路径**，移动文件后需同步更新链接。

---

## 💡 快速提示

- 🔍 **找不到文档？** 查看 [INDEX.md](./copilot-cli-journey/INDEX.md) 或用 `find` 命令
- 📖 **想开始学习？** 从 [Copilot CLI 简介](./copilot-cli-journey/README.md) 开始
- ✏️ **发现文档有误？** 提交 Issue 或直接编辑

---

**最后更新**: 2026-04-15  
**维护者**: Michael Zhou  
**语言**: 中文优先
