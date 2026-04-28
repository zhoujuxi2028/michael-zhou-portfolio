# M14: 团队工作流标准化

## 概览

个人熟练使用 Copilot CLI 只是第一步，让整个团队高效、一致地使用 Copilot 才能产生乘数效应。本模块专注于**如何将 Copilot 的使用方式制度化**，包括编写团队 Copilot 使用规范、建立可复用的 Prompt 模板库、设计 onboarding 流程，以及制定质量保障机制，确保 AI 生成的代码符合团队标准。

---

## 核心概念 (理论 ~ 30-40%)

### 概念 1: 团队标准化的三个层级

| 层级 | 内容 | 产物 |
|------|------|------|
| **个人习惯** | 每个人自己的最佳实践 | 个人 Prompt 库 |
| **团队规范** | 约定好的使用方式 | 团队 Copilot 指南 |
| **系统集成** | 融入 CI/CD 和工具链 | 自动化脚本和 Actions |

标准化的目标：让**新成员在 1 周内**就能像资深成员一样高效使用 Copilot。

### 概念 2: 需要标准化的关键场景

| 场景 | 为什么需要标准化 | 标准化内容 |
|------|----------------|----------|
| **测试生成** | 框架选择和质量要求不同 | 指定框架、覆盖率目标 |
| **文档生成** | Docstring 风格不一致 | 统一 Google 风格或 JSDoc |
| **PR 描述** | 质量参差不齐 | 标准化 PR 模板 |
| **代码审查** | 关注点不统一 | 审查 Checklist |
| **Commit Message** | 格式混乱 | Conventional Commits |

### 概念 3: Prompt 模板库的设计原则

好的团队 Prompt 模板需要：

1. **参数化** — 用占位符标注可变部分（如 `{function_name}`、`{framework}`）
2. **有示例** — 提供 good/bad 对比，让新成员理解期望效果
3. **有版本** — Prompt 模板也要版本控制，记录优化历史
4. **易查找** — 按场景分类，加标签，便于搜索

---

## 实战应用 (70% 以上)

### 场景 1: 编写团队 Copilot 使用规范文档

**问题描述**

你的团队开始推广 Copilot CLI，但每个人用法不一致，生成的代码质量参差不齐，需要一份统一的使用规范。

**Copilot CLI 辅助生成规范**

```bash
cat << 'EOF' | gh copilot suggest
请为一个 QA 工程师团队生成 GitHub Copilot CLI 使用规范文档，团队背景：
- 5 人 QA 团队
- 主要技术栈：Python（后端测试）+ JavaScript（前端测试）
- 使用框架：Pytest、Jest、Playwright
- CI/CD：GitHub Actions
- 代码规范：Google Python 风格，Airbnb JS 风格

规范文档需要包含：
1. 适用场景（明确什么时候用 Copilot，什么时候不用）
2. 安全红线（不允许把哪些信息发给 Copilot）
3. 质量检查（Copilot 生成的代码必须经过什么验证）
4. 主要使用场景的标准 Prompt（测试生成、文档生成、PR 描述）
5. 反模式（明确禁止的使用方式）

用 Markdown 格式，适合放入 Wiki 或 README
EOF
```

**规范文档关键章节示例**

```markdown
## 安全红线

以下信息绝对不能提供给 Copilot：
- ❌ 生产环境的 API Token 或密码
- ❌ 含有真实用户数据的日志（需要脱敏后再分析）
- ❌ 公司内部系统的具体架构细节
- ❌ 未公开的产品路线图

## 代码质量要求

所有 Copilot 生成的代码必须满足：
1. 通过所有现有测试（`pytest` 或 `npm test`）
2. 通过 linter 检查（`flake8` 或 `eslint`）
3. 经过人工 Review，确认逻辑正确
4. 函数覆盖率 ≥ 80%（新增代码）
```

**常见陷阱与对策**

- ❌ 陷阱 1：规范太严格，团队不愿遵守
  - ✅ 对策：规范要说明"为什么"，而不只是"必须"，让团队理解价值
- ❌ 陷阱 2：规范只有文字，没有示例
  - ✅ 对策：每条规范都配一个 good/bad 对比示例

---

### 场景 2: 建立可复用的 Prompt 模板库

**问题描述**

团队中每个人都在重复写类似的提示，效率低且质量不稳定。需要建立一个集中的 Prompt 模板库。

**库结构设计**

```
.copilot-prompts/
├── README.md          # 使用说明
├── testing/
│   ├── pytest-unit-test.md
│   ├── pytest-integration-test.md
│   └── jest-component-test.md
├── docs/
│   ├── google-docstring.md
│   └── swagger-jsdoc.md
├── git/
│   ├── pr-description.md
│   └── commit-message.md
└── review/
    ├── security-review.md
    └── general-review.md
```

**示例模板文件（pytest-unit-test.md）**

```markdown
# Pytest 单元测试生成模板

## 使用场景
为 Python 函数生成单元测试，适用于工具类、数据处理、业务逻辑函数。

## 模板

```
使用 Pytest 为以下 Python 函数生成 {TEST_COUNT} 个单元测试：

{FUNCTION_CODE}

要求：
- {HAPPY_PATH_COUNT} 个 happy path（正常输入）
- {EDGE_CASE_COUNT} 个边界值（极值、空值、最大值）
- {ERROR_COUNT} 个异常处理（使用 pytest.raises）
- 遵循 AAA 模式（Arrange / Act / Assert）
- 不需要注释，只输出代码
```

## 示例

好的提示：
[提示示例]

不好的提示：
[反例说明为什么这个提示效果不好]
```

**使用方式**

```bash
# 创建别名简化使用
alias copilot-unit-test='cat .copilot-prompts/testing/pytest-unit-test.md'

# 使用模板
(copilot-unit-test && cat src/utils.py) | gh copilot suggest
```

---

### 场景 3: 设计 Copilot Onboarding 流程

**问题描述**

新成员加入团队，需要在 1 周内掌握团队的 Copilot 使用方式，而不是从零摸索。

**Copilot CLI 辅助生成 Onboarding 材料**

```bash
cat << 'EOF' | gh copilot suggest
为 QA 工程师新成员设计一个 5 天的 GitHub Copilot CLI Onboarding 计划：

背景：
- 成员背景：有 2 年测试经验，Python 中级水平，GitHub 基础
- 团队工具链：Pytest, Jest, Playwright, GitHub Actions
- 学习目标：1 周后能独立高效使用 Copilot 完成日常 QA 任务

计划要求：
- 每天 1-2 个具体任务（实践为主，理论为辅）
- 每个任务有清晰的验收标准
- 第 5 天完成一个综合小项目作为验收
- 包含需要阅读的关键文档链接
EOF
```

**Onboarding 计划输出示例**

```
Day 1: 安装配置 + 基础命令
  - 任务：安装 gh CLI，配置 Copilot 扩展，完成认证
  - 验收：能运行 `gh copilot explain "ls -la"` 并看到结果

Day 2: 测试生成实战
  - 任务：为团队代码库中的 3 个函数生成单元测试并运行
  - 验收：生成的测试能直接通过 pytest，覆盖率 ≥ 80%

...（Day 3-5）
```

---

## 最佳实践速查表

| 标准化项目 | 关键内容 | 维护方式 |
|----------|---------|---------|
| 使用规范 | 适用场景 + 安全红线 + 质量要求 | Wiki / README，季度评审 |
| Prompt 库 | 按场景分类，参数化模板 | Git 仓库，PR 评审 |
| Onboarding | 5 天实践任务 + 验收标准 | 每季度更新一次 |
| CI 集成 | PR 自动生成摘要 + 测试分析 | GitHub Actions 配置 |

---

## 常见错误与调试

| 问题 | 症状 | 原因 | 解决方案 |
|------|------|------|---------|
| 规范推广失败 | 团队继续各行其是 | 规范复杂或好处不明 | 先做小范围试点，用数据说话 |
| Prompt 库无人使用 | 库有内容但大家不用 | 发现成本高 | 集成到 IDE / 脚本，降低使用门槛 |
| 新成员 onboarding 效果差 | 1 周后仍不会用 | 任务太复杂或缺乏指导 | 增加导师制，1v1 陪练 |

---

## 与其他模块的关系

- **前置模块**：M8（工作流集成 — CI/CD 集成是标准化的基础）、M13（Workspace — 团队级别工具）
- **相关模块**：所有模块 — 团队标准化是对前面所有模块的综合应用
- **后续模块**：M15（个人知识库总结 — 标准化之后沉淀个人洞察）

---

## 进阶延伸

- **Copilot 使用度量**：用 GitHub Copilot API 获取团队使用数据，量化效率提升
- **规范自动化执行**：把规范中的检查项做成 linter 规则或 CI 检查，强制执行
- **跨团队标准共享**：把本团队的最佳实践贡献给组织级别的规范库

---

## 参考资源

- [GitHub Copilot 企业版管理指南](https://docs.github.com/copilot/managing-copilot)
- [Conventional Commits 规范](https://www.conventionalcommits.org/)
- [Google Engineering Practices](https://google.github.io/eng-practices/)

---

## 反思与迭代

完成本模块学习后，请记录：

- ✅ **学到的最有用的技巧**：团队标准化中哪个环节最关键？
- 🤔 **遇到的主要困难**：推广 Copilot 使用规范时遇到了哪些阻力？
- 💡 **改进的空间**：6 个月后回顾，哪些规范需要修订？

---

**下一步**：[M15: 个人知识库总结与迭代](./M15-knowledge-summary.md)

*最后更新：2026-04-28*
