# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a multi-project portfolio repository. Each subdirectory is an independent project with its own CLAUDE.md for project-specific guidance.

## Projects

- **cloud-monitoring-platform/** - IoT cloud monitoring system (Spring Boot + Vue.js + uni-app)
- **edi-reporting-demo/** - EDI message parsing + Jasper report template (Python + EDIFACT + Jasper Reports + SQLite)
- **android-app/** - Android application *(planned)*
- **linux-driver/** - Linux kernel driver *(planned)*

## General Conventions

- Each project has its own `CLAUDE.md` with project-specific build commands and architecture details
- Vendor references use anonymized names (Vendor A, Vendor B, Vendor C) for information security
- Navigate to the specific project directory before running any build/dev commands
- Requirements use the repository-level numbering convention `REQ-{PROJECT}-{SEQ}` — see [docs/REQUIREMENTS-NUMBERING.md](docs/REQUIREMENTS-NUMBERING.md) and register every new requirement in [docs/REQUIREMENTS-INDEX.md](docs/REQUIREMENTS-INDEX.md)

## Python Usage

- This macOS environment is externally managed (PEP 668), do NOT use `pip install` directly
- Always use a virtual environment (`python3 -m venv`) for any Python dependencies
- Recommended venv location: `/tmp/<purpose>_env` for temporary/one-off scripts
## Claude Code Guidelines
- **Be concise**: Keep responses and file content brief and to the point
- **No fluff**: Avoid unnecessary explanations, verbose comments, or filler text
- **Tables over prose**: Use tables/lists instead of paragraphs when possible

## ⚠️ Branch Discipline

**CRITICAL: Never work on `main` branch for development, testing, or verification.**

| Phase | 工作内容 | 分支要求 |
|-------|---------|----------|
| 1-5 (需求→收尾) | 代码编写、测试、验证 | ✅ 使用 `feature/*` 分支 |
| 收尾 (PR 合并后) | 文档同步、Wiki 更新 | ✅ main 已合并，可直接更新 |

**Rule**: 
- Stage 3 (开发) → Stage 4 (测试) → Stage 5 (收尾 PR) 都必须在 feature 分支上
- Main 分支仅用于：(1) 接收 PR merge，(2) 生产部署
- 误操作示例：在 main 上 run tests, commit fixes, git merge —abort ❌

**历史教训**: 2026-04-15 在 main 上触发 merge，导致需要回滚到 feature/performance-testing 重新开始 Stage 4 verification

## Worktree Convention

- 默认 worktree 位置：`~/.config/superpowers/worktrees/<repo-name>/`
- 本仓库推荐路径：`~/.config/superpowers/worktrees/michael-zhou-qa-portfolio/`
- 需要隔离开发、测试、设计验证时，优先使用全局 worktree，而不是在仓库内新建 `.worktrees/`

## Development Process（开发流程）

每个新功能/项目遵循 5 阶段流程，**每阶段结束必须暂停等待人工评审**，通过后才能进入下一阶段。

| 阶段 | 活动 | 交付物 | 评审要点 |
|------|------|--------|----------|
| 1. 需求 | Issue 分析、scope 确认、可行性评估 | 需求描述 + 可行性评估 | scope 是否合理、本机环境是否支持 |
| 2. 设计 | 实施计划、架构设计、Plan Review | 实施计划文档 | 架构合理、任务拆分清晰、reviewer 问题已修复 |
| 3. 开发 | TDD 编码、逐步提交 | 代码 + 单元测试 | 代码质量、测试覆盖、commit 规范 |
| 4. 测试 | 本地自测、lint、CI 验证 | 全部测试通过 | lint 通过、所有测试 PASS、CI 绿灯 |
| 5. 收尾 | PR 创建、文档更新、root 注册 | PR merged + 文档同步 | README/CLAUDE.md 更新、Wiki 同步 |

**规则：**
- **每阶段开始时，先对照 checklist 列出本阶段所有交付物**，再逐项完成
- Claude 在每个阶段完成后必须**报告状态并等待确认**，不得自行跳到下一阶段
- 如果评审发现问题，在当前阶段修复后重新评审
- 阶段可根据任务规模简化（小 bugfix 可合并阶段），但需用户同意