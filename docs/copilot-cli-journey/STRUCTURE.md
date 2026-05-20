# 文件夹结构说明

## 📂 新的组织结构

```
docs/copilot-cli-journey/
├── 📄 README.md                     ← 学习中心主入口
├── 📄 INDEX.md                      ← 学习路径导航
├── 📄 STRUCTURE.md                  ← 当前结构说明
├── 📄 template.md                   ← 模块标准模板
│
├── 📁 modules/                      ← 所有学习模块（按阶段分类）
│  ├── phase1/                       ✅ 完成
│  │  ├── M1-copilot-cli-basics.md
│  │  ├── M2-prompting-fundamentals.md
│  │  └── M3-cli-ecosystem.md
│  ├── phase2/                       ✅ M4 完成 / 🔨 M5-M6 进行中
│  │  ├── M4-test-generation.md      ✅ COMPLETE (41 tests total: Jest 95%+, Pytest 82-85%+)
│  │  ├── M5-doc-generation.md       👷 IN PREPARATION
│  │  └── M6-code-review-workflow.md 📋 PLANNED
│  ├── phase3/                       🔨 占位符
│  │  ├── M7-context-management.md
│  │  ├── M8-workflow-integration.md
│  │  └── M9-debugging.md
│  ├── phase4/                       🔨 占位符
│  │  ├── M10-api-testing-integration.md
│  │  ├── M11-e2e-testing-integration.md
│  │  └── M12-perf-testing-integration.md
│  └── phase5/                       🔨 占位符
│     ├── M13-copilot-workspace.md
│     ├── M14-team-standards.md
│     └── M15-knowledge-summary.md
│
├── 📁 docs/                         ← 支持文档（报告、清单等）
│  ├── phase1/                       ✅ 完成
│  │  ├── README.md
│  │  ├── PHASE1-COMPLETION-REPORT.md
│  │  ├── PHASE1-FINAL-SUMMARY.md
│  │  ├── PHASE1-REVIEW-REQUEST.md
│  │  ├── PHASE1-REVIEW-FEEDBACK-LOG.md
│  │  └── PRE-REVIEW-CHECKLIST.md
│  ├── phase2/                       ✅ M4 完成 / 🔨 M5-M6 进行中
│  │  ├── M4-RESOURCE-INDEX.md       新文件：M4 资源导航索引
│  │  ├── M4-COMPLETION-SUMMARY.md
│  │  ├── M4-MIGRATION-COMPLETE.md
│  │  ├── archive/                   阶段性状态和迁移记录归档
│  │  │  ├── BRANCH-STATUS.md
│  │  │  └── TRANSITION-PROGRESS.md
│  │  ├── prompts/                   Copilot 提示词库
│  │  └── ...其他文档
│  ├── phase3/                       📋 占位符
│  ├── phase4/                       📋 占位符
│  └── phase5/                       📋 占位符
│
└── 📁 examples/                     ← 代码示例和脚本
   ├── phase1/                       📝 待补充
   ├── phase2/                       ✅ M4 完成
   │  ├── jest-demo/                21 tests, 95%+ coverage
   │  └── pytest-demo/              20 tests, 82-85%+ coverage
   ├── phase3/                       📝 占位符
   ├── phase4/                       📝 占位符
   └── phase5/                       📝 占位符
```

---

## 说明

| 标记 | 含义 |
|------|------|
| ✅ | 已完成并有内容 |
| 🔨 | 占位符，骨架已建立，待填充内容 |
| 📋 | 占位符文件，目录结构已建立 |
| 📝 | 可选内容区域 |

---

## 快速导航

### 🚀 我想学习 Phase 1
→ [modules/phase1/README](./modules/phase1/) 或直接打开 M1-M3

### 📚 我想查看支持文档
→ [docs/phase1/](./docs/phase1/) （评审清单、完成报告等）

### 🗄️ 我想查看 Phase 2 归档资料
→ [Phase 2 归档资料](./docs/phase2/archive/)（含 [分支状态记录](./docs/phase2/archive/BRANCH-STATUS.md) 和 [M4-M5 转换进度](./docs/phase2/archive/TRANSITION-PROGRESS.md)）

### 💻 我想看代码示例
→ [examples/phase1/](./examples/phase1/)

---

*最后更新：2026-05-19* — 根目录瘦身，阶段性过程资料归档到 `docs/phase2/archive/`
