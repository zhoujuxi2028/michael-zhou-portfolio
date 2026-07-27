# M8 工作流集成 Prompt 模板库

本模板库用于将 Copilot CLI 集成到 Shell 脚本、Git Hook 和 CI/CD 工作流中。

## 快速选择表

| 场景 | 推荐模板 | 目标 |
|------|---------|------|
| 自动生成 commit message | T1 Commit Message 辅助 | 从 git diff 生成 Conventional Commits 格式 |
| 分析测试失败日志 | T2 测试失败摘要 | 提取根因和修复优先级 |
| 生成 GitHub Actions 工作流 | T3 CI/CD 工作流生成 | 按需求生成完整 YAML |
| 生成 Git Hook 脚本 | T4 Git Hook 脚本 | 自动化特定 Git 事件 |

---

## T1：Commit Message 辅助

适用于：将 staged 的 diff 转为规范的 commit message。

```text
根据以上 Git diff 生成一行 Conventional Commits 格式的 commit message。
格式：<type>(<scope>): <description>
type 只能是：feat, fix, docs, test, refactor, chore
只输出一行 commit message，不包含换行符，不需要说明。
```

**使用方式**：
```bash
git diff --staged | gh copilot suggest "$(cat 上面的模板)"
```

**已知局限**：diff 超过 500 行时 scope 可能不准确，建议先 `git add` 单个文件再运行。

---

## T2：测试失败摘要

适用于：pytest/jest 输出过长，需要快速定位根因。

```text
以上是测试失败的日志。请：
1. 找出最根本的错误原因（不是 Traceback 的最后一行，而是真正的根因）
2. 排除由根因引起的次生错误（列出即可，不需要分析）
3. 给出 2-3 个修复方向，按可能性从高到低排序
用中文简洁回答，不需要代码示例。
```

**使用方式**：
```bash
pytest --tb=short 2>&1 | tail -50 | gh copilot suggest "$(cat 上面的模板)"
```

---

## T3：GitHub Actions 工作流生成

适用于：生成满足特定需求的 CI/CD YAML 文件。

```text
请生成一个 GitHub Actions workflow 文件，实现以下需求：
{需求描述，逐条列出}

要求：
- 触发条件：{on: push/pull_request/schedule 等}
- 运行环境：{ubuntu-latest/specific version}
- Secrets 从 GitHub secrets 读取（不能硬编码）
- 失败时发送通知（{PR 评论/Slack/邮件}）
只输出 YAML 文件内容
```

---

## T4：Git Hook 脚本生成

适用于：在特定 Git 事件（pre-commit、post-merge 等）触发自动化任务。

```text
请生成一个 {hook类型} Git Hook 脚本（Bash），实现：
{功能描述}

要求：
- 检查失败时退出码非 0（阻止 Git 操作）
- 输出清晰的错误信息（❌ 失败 / ✅ 成功 格式）
- 兼容 macOS 和 Linux
- 脚本放入 .git/hooks/{hook名} 后需 chmod +x
```

---

*最后更新：2026-07-28*
