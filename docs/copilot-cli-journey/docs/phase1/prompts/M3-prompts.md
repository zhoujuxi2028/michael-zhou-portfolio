# M3 Copilot CLI 生态探索 - 提示模板库

> 为 Git 集成、Shell 命令生成和文件操作任务提供高质量的 Copilot 提示模板

---

## 前置知识

使用这些提示前，建议先阅读：
- `modules/phase1/M3-cli-ecosystem.md` - M3 理论
- `M3-prompting-strategy.md` - M3 提示工程指南（如果存在）

---

## 提示模板 1: 生成标准化 Commit Message

**场景**: 你已经修改了多个文件，需要一个规范的 commit message

**使用条件**:
- 已通过 `git add` 暂存改动
- 项目遵循 Conventional Commits 规范（feat/fix/docs/test 等）

**提示词**:
```
Analyze the following git diff and generate a Conventional Commit message.

Requirements:
- Format: <type>(<scope>): <subject>
- Types: feat, fix, docs, test, refactor, perf, chore
- Scope: component or feature affected (optional)
- Subject: clear, imperative mood, no period, max 50 chars

Git diff:
```

**使用流程**:
```bash
# 1. 执行命令
git diff --cached | gh copilot suggest

# 2. 粘贴上述提示
# (提示词会引导 Copilot 分析 diff)

# 3. Copilot 会返回类似结果
feat(api): add user authentication endpoint

- Implements JWT-based authentication
- Adds /api/auth/login endpoint
- Includes password hashing with bcrypt

# 4. 复制并使用
git commit -m "feat(api): add user authentication endpoint"
```

**预期结果示例**:
```
feat(tests): add comprehensive unit tests for calculator

- Added 15 test cases covering all functions
- Achieved 95% code coverage
- Tests include edge cases and error scenarios
```

---

## 提示模板 2: 生成 PR 描述

**场景**: 你开发完一个功能分支，需要创建一个清晰的 PR 描述

**使用条件**:
- 已将改动推送到远程分支
- 知道 main/master 分支名称

**提示词**:
```
Analyze the git commits between main and this branch, then generate a professional PR description.

Format:
## Description
[What does this PR do? 2-3 sentences]

## Changes
- [List 3-5 key changes]

## Testing
[What testing was done?]

## Checklist
- [ ] Code reviewed
- [ ] Tests pass
- [ ] Docs updated

Git log (from main to HEAD):
```

**使用流程**:
```bash
# 1. 获取分支间的 commit
git log --oneline main..HEAD

# 2. 执行命令
git log --oneline main..HEAD | gh copilot suggest

# 3. 粘贴上述提示
# (Copilot 会基于 commit 生成 PR 描述)

# 4. 复制到 GitHub PR 页面
```

**预期结果示例**:
```
## Description
Implements user authentication system with JWT tokens and session management, 
including login/logout endpoints and middleware for protected routes.

## Changes
- Added JWT token generation and validation
- Implemented login/logout endpoints
- Added authentication middleware
- Added user model and database schema
- Added comprehensive unit tests

## Testing
- All 25 unit tests passing
- Manual testing of login/logout flows
- Tested with invalid credentials

## Checklist
- [x] Code reviewed
- [x] Tests pass
- [x] Docs updated
```

---

## 提示模板 3: 生成 Shell 命令 - 文件查找与过滤

**场景**: 需要查找并过滤特定的文件

**使用条件**:
- 了解 find/grep/awk 基础概念
- 想要避免编写复杂的 shell 命令

**提示词**:
```
Generate a shell command to [具体任务].

Requirements:
- Task: [明确的需求]
- Search criteria: [搜索条件]
- Output format: [期望的输出格式]
- System: macOS / Linux / both
- Exclude: [需要排除的项]

Example output:
[给一个期望的输出例子]

Provide ONLY the command, no explanation.
```

**具体示例 1: 查找最近修改的 Python 文件**
```
Generate a shell command to find all Python test files modified in the last 7 days.

Requirements:
- Task: Find test files (test_*.py) modified in last 7 days
- Search criteria: *.py files with "test_" prefix
- Output format: filename and modification time
- System: macOS
- Exclude: __pycache__, venv, .git

Example output:
tests/test_user.py (3 days ago)
tests/test_api.py (5 days ago)

Provide ONLY the command, no explanation.
```

**预期结果**:
```bash
find . -name "test_*.py" -type f -mtime -7 -exec ls -lh {} \; | awk '{print $9, $6, $7, $8}'
```

**具体示例 2: 统计代码中的 TODO/FIXME 注释**
```
Generate a shell command to find and count TODO and FIXME comments in Python code.

Requirements:
- Task: Find all TODO and FIXME comments
- Search criteria: Lines containing "TODO" or "FIXME"
- Output format: filepath, line number, comment text
- System: macOS
- Exclude: __pycache__, venv, .git, node_modules

Example output:
src/api.py:42: TODO: Add error handling
src/models.py:105: FIXME: Optimize database query

Provide ONLY the command, no explanation.
```

**预期结果**:
```bash
grep -rn "TODO\|FIXME" --include="*.py" --exclude-dir={__pycache__,venv,.git} . | awk -F: '{print $1":"$2": "$3}'
```

---

## 提示模板 4: 生成 Shell 命令 - 批量文件操作

**场景**: 需要对多个文件进行相同的操作（更新、转换、重命名等）

**使用条件**:
- 文件列表已确定
- 操作是可以重复应用的（如添加前缀、替换文本等）

**提示词**:
```
Generate a shell script to [具体任务]

Requirements:
- File pattern: [文件匹配模式]
- Operation: [具体的操作]
- Backup: [是否需要备份]
- Validation: [如何验证操作成功]
- System: macOS / Linux

Input files:
[列举 2-3 个示例文件]

Expected output after operation:
[列举 2-3 个期望的结果]

Provide complete shell script with error handling and logging.
Output ONLY code, no explanations.
```

**具体示例 1: 为所有 PNG 文件添加时间戳前缀**
```
Generate a shell script to rename all PNG files with timestamp prefix.

Requirements:
- File pattern: *.png in current directory
- Operation: Add timestamp prefix (YYYYMMDD-HHMMSS_filename.png)
- Backup: Create .backup folder with originals
- Validation: Verify renamed file exists and original matches backup
- System: macOS

Input files:
logo.png
icon.png
background.png

Expected output after operation:
20240415-120000_logo.png
20240415-120001_icon.png
20240415-120002_background.png

Provide complete shell script with error handling and logging.
Output ONLY code, no explanations.
```

**具体示例 2: 批量更新文件头注释**
```
Generate a shell script to update copyright year in all JavaScript files.

Requirements:
- File pattern: *.js in src/ and tests/ directories
- Operation: Replace year in copyright line from 2023 to 2024
- Backup: Create backup with .bak extension
- Validation: Show diff before final commit, ask for confirmation
- System: Linux

Current header:
// Copyright 2023 MyCompany

Expected header:
// Copyright 2024 MyCompany

Provide complete shell script with confirmation step.
Output ONLY code, no explanations.
```

---

## 提示模板 5: 生成 Git Hook 脚本

**场景**: 想要在 commit、push 等 Git 事件前自动运行某些操作

**使用条件**:
- 了解 Git hooks 概念（pre-commit, commit-msg, pre-push）
- 想要自动化某个开发流程步骤

**提示词**:
```
Generate a [hook-type] Git hook script for [目的].

Requirements:
- Hook type: [pre-commit / commit-msg / pre-push / post-merge]
- Purpose: [具体的目的]
- Language: bash / python
- Actions: [需要执行的操作列表]
- Failure handling: [如果检查失败是否阻止操作]

Example validation:
[给出 2-3 个验证的例子]

Provide complete script with error messages and exit codes.
Output ONLY code, no explanations.
```

**具体示例 1: Pre-commit Hook - 检查 Linting**
```
Generate a pre-commit Git hook script to verify code quality.

Requirements:
- Hook type: pre-commit
- Purpose: Run linting and tests before commit
- Language: bash
- Actions:
  1. Run ESLint on staged JavaScript files
  2. Run Prettier format check
  3. Run unit tests
- Failure handling: Block commit if any check fails

Example validation:
- ESLint error: "Unexpected var" → Show error and block
- Prettier mismatch: "File not formatted" → Show diff and block
- Test failure: "5 tests failed" → Show summary and block

Provide complete script with colored output and error messages.
Output ONLY code, no explanations.
```

**具体示例 2: Commit-msg Hook - 验证 Conventional Commits**
```
Generate a commit-msg Git hook script to enforce Conventional Commits.

Requirements:
- Hook type: commit-msg
- Purpose: Validate commit message format (feat/fix/docs/etc)
- Language: bash
- Actions:
  1. Extract commit message from file
  2. Validate format: <type>(<scope>): <subject>
  3. Enforce types: feat, fix, docs, test, refactor
- Failure handling: Block commit if format invalid

Example validation:
- Valid: "feat(api): add user authentication" → Allow
- Invalid: "add new feature" → Show error and block
- Invalid type: "update(auth): fix bug" → Show error and block

Provide complete script with helpful error messages.
Output ONLY code, no explanations.
```

---

## 提示模板 6: 生成复杂的多步脚本

**场景**: 需要执行一个复杂的多步操作，涉及多个命令和条件判断

**使用条件**:
- 操作流程已明确规划
- 想要一个可维护、可复用的脚本

**提示词**:
```
Generate a production-grade shell script for [任务名称].

Context:
[背景说明 2-3 句话]

Requirements:
- Purpose: [具体目的]
- Input: [输入参数或文件]
- Output: [输出结果]
- System compatibility: [macOS / Linux / both]
- Error handling: [如何处理错误]
- Logging: [是否记录日志]

Step-by-step flow:
1. [第一步]
2. [第二步]
3. [第三步]
...

Example usage:
./script.sh [参数示例]

Provide complete, production-ready script with:
- Input validation
- Error handling with meaningful messages
- Logging to file
- Progress indicators
- Final summary

Output ONLY code, no explanations.
```

**具体示例: 自动化构建和部署流程**
```
Generate a production-grade shell script for automated build and deployment.

Context:
We need to automate the build, test, and deployment process to staging environment.
The script should validate prerequisites, run tests, build artifacts, and deploy.

Requirements:
- Purpose: Build, test, and deploy Node.js application
- Input: Environment name (staging/production)
- Output: Deployment status report
- System compatibility: Linux (CI/CD server)
- Error handling: Stop on any error, rollback if deploy fails
- Logging: Log to deploy.log file

Step-by-step flow:
1. Validate Node.js and npm versions
2. Install dependencies (npm install)
3. Run linting (npm run lint)
4. Run tests (npm run test)
5. Build artifacts (npm run build)
6. Upload to server
7. Restart service
8. Verify deployment

Example usage:
./deploy.sh staging
./deploy.sh production

Provide complete, production-ready script with error handling and rollback.
Output ONLY code, no explanations.
```

---

## 快速参考表

| 需求 | 提示模板 | 预期输出 |
|------|--------|--------|
| Commit Message | 模板 1 | 标准化的 commit message |
| PR 描述 | 模板 2 | 结构化的 PR 描述 |
| 文件查找 | 模板 3 | Shell 命令 |
| 批量操作 | 模板 4 | 完整的脚本 |
| Git 自动化 | 模板 5 | Hook 脚本 |
| 复杂流程 | 模板 6 | 生产级脚本 |

---

## 使用建议

1. **复制完整提示词** - 不要改动模板中的结构
2. **补充具体信息** - 在 `[...]` 处填入你的具体需求
3. **验证生成结果** - 在实际应用前，先用 dry-run 或测试环境验证
4. **保存好的结果** - 建立你自己的提示库，复用高质量的提示

---

## 常见问题

**Q: 生成的命令在我的系统上不工作？**  
A: 在提示词中明确指定系统（macOS/Linux），某些工具在不同系统有差异（如 sed）

**Q: 如何让 Copilot 生成更简单的命令？**  
A: 在提示词末尾添加 "Prefer simple, readable commands over one-liners"

**Q: 脚本太复杂，难以维护？**  
A: 让 Copilot 添加注释："Add inline comments explaining each major step"

---

*最后更新: 2026-04-15*
