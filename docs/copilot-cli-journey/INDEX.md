# Copilot CLI 学习导航

这是 `docs/copilot-cli-journey/` 的快速索引，用来回答三个问题：

1. 我现在应该学哪个模块？
2. 我应该去哪里找模块、案例、报告和 Prompt？
3. 根目录 4 个入口文件分别怎么用？

---

## 1. 根目录 4 个文件怎么用

| 文件 | 用途 | 适合什么时候打开 |
|------|------|------------------|
| [README.md](./README.md) | 学习中心主入口，查看 M1-M15 总路线和快速开始 | 第一次进入学习中心，或想了解全局规划 |
| [INDEX.md](./INDEX.md) | 学习路径导航，按目标、时间、角色选择下一步 | 不确定先学什么，或想快速定位资料 |
| [STRUCTURE.md](./STRUCTURE.md) | 目录结构地图，说明模块、支持文档和示例放在哪里 | 找不到文件、报告、归档资料或示例代码时 |
| [template.md](./template.md) | 学习模块写作模板，统一新模块的内容结构 | 新增或重写 M 模块时 |

一句话记忆：**README 看全局，INDEX 选路径，STRUCTURE 找资料，template 写模块。**

---

## 2. 推荐学习方式

### 新手入门路径

适合目标：先熟悉 Copilot CLI 的基本用法。

1. [M1: Copilot CLI 基础](./modules/phase1/M1-copilot-cli-basics.md)
2. [M2: 提示工程基础](./modules/phase1/M2-prompting-fundamentals.md)
3. [M3: Copilot CLI 生态初探](./modules/phase1/M3-cli-ecosystem.md)
4. 练习：[Phase 1 示例](./examples/phase1/)

### QA 工作流路径

适合目标：把 Copilot CLI 用到测试、文档和审查工作中。

1. [M4: 测试代码生成最佳实践](./modules/phase2/M4-test-generation.md)
2. [M5: 文档和注释生成工作流](./modules/phase2/M5-doc-generation.md)
3. [M6: 代码审查加速工作流](./modules/phase2/M6-code-review-workflow.md)
4. 复盘：[Phase 2 支持文档中心](./docs/phase2/)
5. 练习：[Phase 2 示例](./examples/phase2/)

### 高阶能力路径

适合目标：提升多文件上下文、工作流集成和调试能力。

1. [M7: 上下文管理与多文件交互](./modules/phase3/M7-context-management.md)
2. [M8: 自定义工作流与脚本集成](./modules/phase3/M8-workflow-integration.md)
3. [M9: 调试与故障排查](./modules/phase3/M9-debugging.md)

### 项目集成路径

适合目标：把 Copilot CLI 应用到真实 QA 项目或作品集项目中。

1. [M10: API 测试项目集成](./modules/phase4/M10-api-testing-integration.md)
2. [M11: E2E 测试项目集成](./modules/phase4/M11-e2e-testing-integration.md)
3. [M12: 性能/稳定性测试集成](./modules/phase4/M12-perf-testing-integration.md)

### 总结扩展路径

适合目标：沉淀团队规范和个人知识库。

1. [M13: Copilot Workspace 探索](./modules/phase5/M13-copilot-workspace.md)
2. [M14: 团队工作流标准化](./modules/phase5/M14-team-standards.md)
3. [M15: 个人知识库总结与迭代](./modules/phase5/M15-knowledge-summary.md)

---

## 3. 按目标快速选择

| 你的目标 | 推荐路径 | 重点产出 |
|----------|----------|----------|
| 快速上手 Copilot CLI | M1 → M2 → M3 | 基础命令、提示词、Git/Shell 工作流 |
| 提升测试生成效率 | M1 → M2 → M4 | Jest/Pytest 测试生成和覆盖率提升 |
| 自动化文档和注释 | M1 → M2 → M5 | Docstring、API 文档、注释规范 |
| 加速代码审查 | M1 → M2 → M6 | Review checklist、PR 描述、质量检查 |
| 提升复杂任务能力 | M7 → M8 → M9 | 多文件上下文、脚本集成、调试策略 |
| 做作品集实战案例 | M10 → M11 → M12 | API/E2E/性能测试集成案例 |
| 形成长期方法论 | M13 → M14 → M15 | 团队规范、知识库、长期复盘 |

---

## 4. 按角色选择

| 角色 | 建议优先学习 | 可暂缓 |
|------|--------------|--------|
| 功能测试工程师 QA | M1 → M2 → M3 → M4 → M5 → M6 → M11 | M12-M15 |
| 测试开发工程师 SDET | M1 → M2 → M4 → M7 → M8 → M9 → M10 → M11 → M12 | M13-M15 |
| 后端开发/API 测试 | M1 → M2 → M5 → M6 → M10 | M4、M11、M12 |
| DevOps/CI-CD 工程师 | M1 → M2 → M3 → M8 → M9 → M14 | M4-M7 |
| 个人知识库维护者 | M1 → M5 → M14 → M15 | M10-M12 |

---

## 5. 模块总览

| Phase | 模块 | 状态 | 入口 |
|-------|------|------|------|
| Phase 1 核心基础 | M1 | ✅ 已完成 | [Copilot CLI 基础](./modules/phase1/M1-copilot-cli-basics.md) |
| Phase 1 核心基础 | M2 | ✅ 已完成 | [提示工程基础](./modules/phase1/M2-prompting-fundamentals.md) |
| Phase 1 核心基础 | M3 | ✅ 已完成 | [Copilot CLI 生态初探](./modules/phase1/M3-cli-ecosystem.md) |
| Phase 2 QA 工作流 | M4 | ✅ 已完成 | [测试代码生成最佳实践](./modules/phase2/M4-test-generation.md) |
| Phase 2 QA 工作流 | M5 | ✅ 已完成 | [文档和注释生成工作流](./modules/phase2/M5-doc-generation.md) |
| Phase 2 QA 工作流 | M6 | ✅ 已完成 | [代码审查加速工作流](./modules/phase2/M6-code-review-workflow.md) |
| Phase 3 高阶提示工程 | M7 | 🔨 待深化 | [上下文管理与多文件交互](./modules/phase3/M7-context-management.md) |
| Phase 3 高阶提示工程 | M8 | 🔨 待深化 | [自定义工作流与脚本集成](./modules/phase3/M8-workflow-integration.md) |
| Phase 3 高阶提示工程 | M9 | 🔨 待深化 | [调试与故障排查](./modules/phase3/M9-debugging.md) |
| Phase 4 项目集成案例 | M10 | 📋 占位 | [API 测试项目集成](./modules/phase4/M10-api-testing-integration.md) |
| Phase 4 项目集成案例 | M11 | 📋 占位 | [E2E 测试项目集成](./modules/phase4/M11-e2e-testing-integration.md) |
| Phase 4 项目集成案例 | M12 | 📋 占位 | [性能/稳定性测试集成](./modules/phase4/M12-perf-testing-integration.md) |
| Phase 5 进阶与扩展 | M13 | 📋 占位 | [Copilot Workspace 探索](./modules/phase5/M13-copilot-workspace.md) |
| Phase 5 进阶与扩展 | M14 | 📋 占位 | [团队工作流标准化](./modules/phase5/M14-team-standards.md) |
| Phase 5 进阶与扩展 | M15 | 📋 占位 | [个人知识库总结与迭代](./modules/phase5/M15-knowledge-summary.md) |

标记说明：✅ 已完成并可复习；🔨 有骨架或初稿，适合继续深化；📋 占位或规划中。

---

## 6. 资源入口

| 资源类型 | 入口 | 用途 |
|----------|------|------|
| 正式学习模块 | [modules/](./modules/) | M1-M15 的主学习内容 |
| 支持文档 | [docs/](./docs/) | 完成报告、Review 记录、Prompt 库、阶段归档 |
| 可运行示例 | [examples/](./examples/) | Jest、Pytest、文档生成等实践项目 |
| Phase 1 支持资料 | [docs/phase1/](./docs/phase1/) | Phase 1 完成报告和评审资料 |
| Phase 2 支持资料 | [docs/phase2/](./docs/phase2/) | M4-M6 报告、Prompt、技术栈和归档资料 |
| Phase 2 归档资料 | [docs/phase2/archive/](./docs/phase2/archive/) | 分支状态和 M4-M5 转换记录 |
| 模块模板 | [template.md](./template.md) | 新建或重写模块时复制使用 |

---

## 7. 建议学习节奏

| 时间 | 建议任务 | 目标 |
|------|----------|------|
| 第 1 周 | M1-M2 | 掌握基础命令和提示词写法 |
| 第 2 周 | M3 + 一个 Phase 1 示例 | 熟悉 Git/Shell/文件操作场景 |
| 第 3-4 周 | M4-M6 | 完成测试、文档、代码审查三类 QA 工作流 |
| 第 5-6 周 | M7-M9 | 练习复杂上下文、工作流集成和调试 |
| 第 7-8 周 | M10-M12 | 选择一个真实项目做集成案例 |
| 第 9 周后 | M13-M15 | 总结个人方法论和团队规范 |

如果时间有限，优先走：**M1 → M2 → M4 → M5 → M6**。

---

## 8. 每个模块怎么学

1. 先读模块的 **概览** 和 **核心概念**。
2. 跳到 **实战应用**，选择一个场景照着执行。
3. 到 `examples/` 找对应案例运行。
4. 把可复用 Prompt 或命令记录到阶段支持文档。
5. 完成后回到本文件，更新自己的学习进度。

---

*最后更新：2026-05-20*
