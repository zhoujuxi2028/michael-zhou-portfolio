# M8: 自定义工作流与脚本集成

## 概览

Copilot CLI 真正发挥威力的时候，是当它被集成到你的**日常工作流**中，而不是每次手动运行命令。本模块专注于如何编写辅助脚本、配置 shell 别名，以及将 Copilot 融入 CI/CD 流水线，让 AI 辅助变成自动化工作流的一部分。

---

## 核心概念 (理论 ~ 30-40%)

### 概念 1: Copilot 在工作流中的三种集成方式

| 集成方式 | 场景 | 触发时机 |
|---------|------|---------|
| **Shell 别名 / 函数** | 个人日常提效 | 手动触发，替代常用命令 |
| **辅助脚本** | 团队共享的自动化任务 | 手动或 Hook 触发 |
| **CI/CD 集成** | 自动化流水线 | 代码提交 / PR 触发 |

### 概念 2: Shell 脚本中使用 Copilot 的模式

```bash
# 模式 1：管道模式（最常用）
cat file.py | gh copilot suggest "分析这个文件..."

# 模式 2：Here Document（适合多行提示）
gh copilot suggest << 'EOF'
请根据以下规范生成代码...
EOF

# 模式 3：命令输出作为输入（适合分析命令结果）
pytest --tb=short 2>&1 | gh copilot suggest "分析以上测试失败原因，给出修复建议"

# 模式 4：环境变量注入上下文（适合脚本复用）
echo "项目：$PROJECT_NAME，语言：Python，框架：FastAPI" | cat - src/main.py | gh copilot suggest
```

### 概念 3: 工作流集成的安全注意事项

在 CI/CD 中使用 Copilot 时，需要特别注意：

| 风险点 | 说明 | 对策 |
|--------|------|------|
| **Token 暴露** | GitHub Token 不能硬编码 | 使用 CI secrets |
| **代码泄露** | 不向 Copilot 发送机密信息 | 过滤敏感字段后再发送 |
| **速率限制** | 大规模 CI 可能触发限流 | 设置合理的请求间隔 |
| **非确定性输出** | Copilot 结果不是固定的 | CI 中只用于辅助，不用于关键判断 |

---

## 实战应用 (70% 以上)

### 场景 1: 创建 Commit Message 辅助脚本

**问题描述**

每次提交时手动想 commit message 很烦，希望把 diff 传给 Copilot，自动生成符合 Conventional Commits 规范的 message。

**Copilot CLI 解决方案**

创建脚本 `scripts/smart-commit.sh`：

```bash
#!/bin/bash
# smart-commit.sh：用 Copilot 生成 commit message，然后交互式确认

set -e

# 检查是否有 staged 改动
if git diff --staged --quiet; then
  echo "❌ 没有 staged 的改动，请先 git add 文件"
  exit 1
fi

echo "🤖 正在分析改动并生成 commit message..."

# 获取 Copilot 建议（加入基本错误处理）
SUGGESTED_MSG=$(git diff --staged | gh copilot suggest \
  "根据以上 Git diff 生成一行 Conventional Commits 格式的 commit message。
  格式：<type>(<scope>): <description>
  type 只能是：feat, fix, docs, test, refactor, chore
  只输出一行 commit message，不需要说明。") || {
  echo "❌ Copilot 生成失败，请手动输入 commit message"
  exit 1
}

echo ""
echo "💡 建议的 commit message:"
echo "   $SUGGESTED_MSG"
echo ""

# 交互式确认
read -p "使用此 message？(y/n/e 编辑) " choice
case $choice in
  y|Y)
    git commit -m "$SUGGESTED_MSG"
    echo "✅ 提交成功"
    ;;
  e|E)
    read -p "请输入 commit message: " CUSTOM_MSG
    git commit -m "$CUSTOM_MSG"
    ;;
  *)
    echo "已取消提交"
    exit 0
    ;;
esac
```

**使用方式**

```bash
chmod +x scripts/smart-commit.sh
git add src/auth/validator.py tests/test_validator.py
./scripts/smart-commit.sh
```

**常见陷阱与对策**

- ❌ 陷阱 1：Copilot 生成了多行 message，破坏了脚本逻辑
  - ✅ 对策：在提示中明确"只输出一行，不包含换行符"

---

### 场景 2: Git Hook 自动生成测试报告摘要

**问题描述**

每次运行测试后，希望自动分析失败日志并生成人类可读的摘要，方便快速定位问题。

**Copilot CLI 解决方案**

创建 `.git/hooks/post-test`（或添加到 Makefile）：

```bash
#!/bin/bash
# 运行测试并用 Copilot 分析失败原因

echo "🧪 运行测试..."
PYTEST_OUTPUT=$(pytest --tb=short 2>&1)
EXIT_CODE=$?

echo "$PYTEST_OUTPUT"

# 如果有失败，用 Copilot 分析
if [ $EXIT_CODE -ne 0 ]; then
  echo ""
  echo "🤖 Copilot 分析失败原因："
  echo "$PYTEST_OUTPUT" | tail -50 | gh copilot suggest \
    "以上是 pytest 测试失败的日志。请：
    1. 总结失败了几个测试
    2. 列出主要失败原因（分类，不是逐条）
    3. 给出优先修复顺序的建议
    用简洁的中文回答，不需要代码示例。"
fi

exit $EXIT_CODE
```

**Makefile 集成**

```makefile
test:
	pytest --tb=short 2>&1 | tee /tmp/test-output.txt; \
	if [ $$? -ne 0 ]; then \
		cat /tmp/test-output.txt | tail -50 | gh copilot suggest "分析失败原因"; \
	fi
```

---

### 场景 3: GitHub Actions 中生成 PR 摘要

**问题描述**

每次 PR 合并后，希望自动在 PR 评论里生成一个变更摘要，方便 QA 和产品了解改动内容。

**GitHub Actions 工作流**

```yaml
# .github/workflows/pr-summary.yml
name: AI PR Summary

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  generate-summary:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Install GitHub CLI
        run: sudo apt-get install gh -y

      - name: Generate PR Summary
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          # 获取 PR 的 diff
          git diff origin/main...HEAD > /tmp/pr.diff
          
          # 用 Copilot 生成摘要（注意：需要 Copilot 订阅）
          SUMMARY=$(cat /tmp/pr.diff | gh copilot suggest \
            "根据 Git diff 生成 PR 摘要，用中文，包含：
            主要变更（bullet list）、影响范围、测试建议。
            控制在 200 字以内。")
          
          # 在 PR 中发表评论
          gh pr comment ${{ github.event.pull_request.number }} \
            --body "## 🤖 AI 变更摘要\n\n$SUMMARY\n\n*由 GitHub Copilot CLI 自动生成*"
```

---

## 最佳实践速查表

| 任务 | 方式 | 关键考虑 |
|------|------|---------|
| 个人提效 | Shell 别名或简短函数 | 放入 `.bashrc` / `.zshrc` |
| 团队共享 | 放入 `scripts/` 目录，加入 README | 版本控制 |
| CI/CD 集成 | GitHub Actions / Jenkins 步骤 | 使用 secrets 保存 token |
| Git Hook | `.git/hooks/` 中的脚本 | 注意退出码的处理 |

---

## 常见错误与调试

| 问题 | 症状 | 原因 | 解决方案 |
|------|------|------|---------|
| CI 中 Copilot 不工作 | `gh: command not found` | gh CLI 未安装 | 添加安装步骤 |
| Token 认证失败 | `401 Unauthorized` in CI | GitHub token 权限不足 | 确认 token 有 Copilot 权限 |
| 脚本输出不稳定 | 每次生成的内容不同 | Copilot 是非确定的 | 只用于辅助展示，不用于关键判断 |
| 管道断裂 | `Broken pipe` 错误 | 某个命令提前退出 | 检查管道中每个命令的退出码 |

---

## 与其他模块的关系

- **前置模块**：M3（CLI 生态 — Git 基础集成）、M7（上下文管理 — 多文件上下文在脚本中的应用）
- **相关模块**：M6（代码审查 — 把审查集成到 CI）、M9（调试 — 自动分析失败日志）
- **后续模块**：M9（调试与故障排查）

---

## 进阶延伸

- **自定义 Copilot 命令别名**：用 `alias cpr="git diff HEAD~1 | gh copilot suggest '生成 PR 描述'"` 简化命令
- **多项目脚本共享**：把通用脚本发布为 dotfiles 仓库，在新项目中复用
- **Webhook 触发**：配合 GitHub Webhooks，在特定事件（如 issue 创建）时触发 Copilot 分析

---

## 参考资源

- [GitHub Actions 官方文档](https://docs.github.com/actions)
- [Git Hooks 指南](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)
- [Bash 脚本最佳实践](https://google.github.io/styleguide/shellguide.html)

---

## 反思与迭代

完成本模块学习后，请记录：

- ✅ **学到的最有用的技巧**：哪个集成方式对你的日常工作提升最大？
- 🤔 **遇到的主要困难**：CI 集成中遇到了哪些权限或配置问题？
- 💡 **改进的空间**：有哪些重复性的工作可以进一步用 Copilot 脚本自动化？

---

**下一步**：[M9: 调试与故障排查](./M9-debugging.md)

*最后更新：2026-04-28*
