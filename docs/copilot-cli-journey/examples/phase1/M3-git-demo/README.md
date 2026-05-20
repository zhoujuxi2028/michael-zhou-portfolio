# M3 Git 工作流集成演示项目

> 通过实际示例展示如何使用 Copilot CLI 生成标准化的 Git 消息和 PR 描述

## 项目概览

这个演示项目展示了 Copilot 在实际 Git 工作流中的应用：

1. **Commit Message 生成** - 使用 Copilot 生成标准化的 commit message
2. **Branch 名称建议** - 生成符合规范的分支名称
3. **PR 描述生成** - 基于 diff 自动生成 PR 描述
4. **Changelog 生成** - 从 commit 历史生成版本更新日志

## 快速开始

### 前置条件

- Git 已安装
- `gh` CLI 工具已安装
- Copilot CLI 已配置

### 项目结构

```
M3-git-demo/
├── README.md (此文件)
├── SETUP.md (详细的演示步骤)
├── commits-for-practice/ (用于练习的示例提交列表)
│   ├── scenario-1-feature.txt
│   ├── scenario-2-bugfix.txt
│   ├── scenario-3-refactor.txt
│   └── scenario-4-docs.txt
└── templates/
    ├── commit-msg-template.txt
    ├── pr-description-template.txt
    └── branch-naming-template.txt
```

## 学习场景

### 场景 1: 生成 Feature 的 Commit Message

**问题**: 新增了用户认证功能，修改了 5 个文件，需要一个清晰的 commit message

**文件内容示例** (`commits-for-practice/scenario-1-feature.txt`):
```
Modified files:
- src/auth/login.js (新增)
- src/auth/utils.js (新增)
- src/middleware/authenticate.js (新增)
- tests/auth.test.js (新增)
- package.json (更新依赖)

Changes:
+ Implemented JWT-based authentication
+ Added login endpoint with password hashing
+ Added middleware to protect routes
+ 15 new test cases covering auth flow
+ Updated package.json with bcrypt and jsonwebtoken dependencies
```

**Copilot 解决方案**:
```bash
# Step 1: 复制上述 diff 内容
cat commits-for-practice/scenario-1-feature.txt

# Step 2: 使用 Copilot 生成 commit message
cat commits-for-practice/scenario-1-feature.txt | gh copilot suggest

# Step 3: Copilot 生成的结果
feat(auth): implement JWT-based user authentication

- Added login endpoint with bcrypt password hashing
- Implemented JWT token generation and validation
- Added authentication middleware for protected routes
- Added 15 comprehensive test cases
- Updated dependencies (bcrypt, jsonwebtoken)
```

### 场景 2: 生成 Bugfix 的 Commit Message

**问题**: 修复了一个用户登录的并发问题

**文件内容示例** (`commits-for-practice/scenario-2-bugfix.txt`):
```
Modified files:
- src/auth/session-manager.js

Bug description:
- Issue: Race condition when multiple login requests processed simultaneously
- Root cause: Session storage not properly locked during write
- Solution: Added Redis-based session locking mechanism
- Impact: Prevents duplicate session creation
```

**Copilot 生成的结果**:
```
fix(auth): prevent race condition in concurrent login requests

- Added Redis-based session locking during authentication
- Ensures atomic session creation for simultaneous requests
- Prevents duplicate session creation and data corruption
```

### 场景 3: 生成 Refactor 的 Commit Message

**问题**: 重构了代码以提高可维护性

**文件内容示例** (`commits-for-practice/scenario-3-refactor.txt`):
```
Modified files:
- src/utils/validators.js
- src/utils/formatters.js
- src/utils/helpers.js

Changes:
- Extracted 12 utility functions from 3 files into a new utility module
- Improved code reusability by 40%
- No functional changes, all tests still pass
- Reduced code duplication from 15% to 8%
```

**Copilot 生成的结果**:
```
refactor(utils): consolidate utility functions for better reusability

- Extracted 12 common utility functions into dedicated modules
- Reduced code duplication from 15% to 8%
- Improved maintainability without changing functionality
- All existing tests pass
```

### 场景 4: 生成文档更新的 Commit Message

**问题**: 更新了 API 文档

**文件内容示例** (`commits-for-practice/scenario-4-docs.txt`):
```
Modified files:
- docs/API.md
- docs/SETUP.md
- README.md

Changes:
- Added OpenAPI/Swagger schema for all endpoints
- Updated installation guide with Docker setup
- Added authentication flow diagram
```

**Copilot 生成的结果**:
```
docs: update API documentation with Swagger schema and setup guide

- Added OpenAPI 3.0 schema for all REST endpoints
- Updated SETUP.md with Docker containerization steps
- Added authentication flow diagram to README
```

## 高级场景: PR 描述生成

### 场景: 基于多个 Commits 生成 PR 描述

**模拟的 Commits** (从某分支比较 main):
```
$ git log --oneline main..feature-branch
abc1234 fix: handle edge case in password validation
def5678 feat: add social login integration (OAuth2)
ghi9012 test: add 8 new test cases for OAuth flow
jkl3456 docs: update authentication documentation
```

**使用 Copilot 生成 PR 描述**:
```bash
# Step 1: 获取分支间的 commit 日志
git log --oneline main..HEAD

# Step 2: 执行 Copilot 建议
git log --oneline main..HEAD | gh copilot suggest

# Step 3: 使用 PR 描述模板提示
# (参考 docs/phase1/prompts/M3-prompts.md - 模板 2)

# Step 4: Copilot 生成的完整 PR 描述
```

**预期 PR 描述**:
```markdown
## Description
Adds OAuth2-based social login functionality with improved password validation.
This PR enhances user authentication flexibility and security.

## Changes
- Implemented OAuth2 social login (Google, GitHub)
- Improved password validation with edge case handling
- Added 8 comprehensive test cases for OAuth flow
- Updated authentication documentation
- Includes migration guide for existing users

## Testing
- All 25+ authentication tests passing
- Tested with Google and GitHub OAuth providers
- Verified edge cases (invalid tokens, expired sessions)
- Manual testing on staging environment

## Checklist
- [x] Code reviewed
- [x] All tests passing
- [x] Documentation updated
- [x] Backwards compatible
```

## 模板文件

### templates/commit-msg-template.txt
```
<type>(<scope>): <subject>

<body>

<footer>
```

**说明**:
- type: feat, fix, docs, test, refactor, perf, chore
- scope: component or feature affected
- subject: 描述改动，不超过 50 字符
- body: 详细说明（可选）
- footer: 关联 Issue（可选）

### templates/pr-description-template.txt
```
## Description
[改动的简要说明]

## Changes
- [主要改动 1]
- [主要改动 2]
- [主要改动 3]

## Testing
[测试说明]

## Checklist
- [ ] Code reviewed
- [ ] Tests pass
- [ ] Docs updated
```

### templates/branch-naming-template.txt
```
<type>/<description>

类型（type）:
- feature: 新功能
- bugfix: 问题修复
- refactor: 代码重构
- docs: 文档更新

示例:
- feature/oauth-login
- bugfix/session-race-condition
- refactor/utility-consolidation
```

## 使用这个项目学习

### 学习路径

1. **基础**: 阅读 `SETUP.md` 了解详细步骤
2. **实践**: 按照 `commits-for-practice/` 中的场景练习
3. **进阶**: 创建自己的提示词，生成高质量的 commit message
4. **验证**: 将生成的结果应用到真实项目中

### 实践建议

1. **复制 scenario 文件内容**，粘贴到 Copilot 提示
2. **比较 Copilot 生成的结果**和本项目中的"预期结果"
3. **调整提示词**，优化生成结果
4. **建立你自己的 Commit Message 库**

## 进阶技巧

### 技巧 1: 集成到 Git Hooks

创建 `.git/hooks/prepare-commit-msg` 脚本：
```bash
#!/bin/bash
# 使用 Copilot 自动生成 commit message
git diff --cached | gh copilot suggest > .commit-msg-suggestion
echo "Suggested commit message:"
cat .commit-msg-suggestion
```

### 技巧 2: 自定义 Prompt 库

基于本项目的模板，为你的项目创建定制化的 Prompt 库。

---

## 参考资源

- [M3 理论文档](../../../modules/phase1/M3-cli-ecosystem.md)
- [M3 Prompt 模板库](../../../docs/phase1/prompts/M3-prompts.md)
- [Conventional Commits 规范](https://www.conventionalcommits.org/)

---

**下一步**: 完成本项目后，进入 [M4: 测试代码生成最佳实践](../../../modules/phase2/M4-test-generation.md)

*最后更新: 2026-04-15*
