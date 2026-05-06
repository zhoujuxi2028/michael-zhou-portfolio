# M5: 文档和注释生成工作流

## 概览

好的代码文档能减少沟通成本、降低维护风险。Copilot CLI 可以帮你快速为现有代码补充 Docstring、生成 API 文档、甚至自动化 Swagger/OpenAPI 规范。本模块专注于**从代码生成文档**的完整工作流，让你在不手写一行注释的情况下，产出规范的技术文档。

> 💡 本模块的补充材料已统一整理到 [M5 学习资料导航](./m5/README.md)，建议配合 [完成总结](./m5/summaries/completion-summary.md) 和 [深化学习总结](./m5/summaries/deep-dive.md) 一起阅读。

---

## 核心概念 (理论 ~ 30-40%)

### 概念 1: 文档生成的四个层级

| 层级 | 内容 | 工具 | 适用场景 |
|------|------|------|---------|
| **代码注释** | 函数级 Docstring | Copilot CLI | 日常开发 |
| **模块文档** | README、模块说明 | Copilot CLI | 项目维护 |
| **API 文档** | Swagger/OpenAPI | swagger-jsdoc / FastAPI | 后端 API |
| **规范文档** | 架构说明、设计决策 | Copilot 辅助 | 大型项目 |

### 概念 2: 主流 Docstring 标准对比

| 标准                           | 适用语言                    | 格式风格                             | 代表工具             |
| ---------------------------- | ----------------------- | -------------------------------- | ---------------- |
| **JSDoc**                    | JavaScript / TypeScript | `@param`, `@returns`, `@example` | TypeDoc, VS Code |
| **Google 风格**                | Python                  | `Args:`, `Returns:`, `Raises:`   | Sphinx           |
| **NumPy 风格**                 | Python（科学计算）            | `Parameters\n----------`         | NumPy, pandas    |
| **Sphinx（reStructuredText）** | Python                  | `:param name:`, `:rtype:`        | Sphinx           |

**建议**：Python 项目用 Google 风格，JavaScript 项目用 JSDoc — 两者可读性强且 Copilot 生成质量最高。

### 概念 3: 高质量 Docstring 提示的五步法

1. **一句话总结** — 命令式动词，清楚表达方法作用（如"计算订单折扣金额"）
2. **详细描述** — 背景、协议、适用场景
3. **Args** — 参数名、类型、说明（复杂类型展示结构）
4. **Returns** — 返回值类型和结构（成功/失败两种场景）
5. **Raises + Examples** — 异常情况和真实使用示例

---

## 实战应用 (70% 以上)

### 场景 1: 为 Python 函数生成 Google 风格 Docstring

**问题描述**

你有一批没有文档的 Python 方法，IDE 里悬浮提示完全空白，团队成员每次使用都要看源码。

**被测代码示例**

```python
def authenticate_user(username: str, password: str, mfa_code: str = None) -> dict:
    user = db.find_user(username)
    if not user or not check_password(password, user.password_hash):
        raise AuthenticationError("用户名或密码错误")
    if user.mfa_enabled and not verify_mfa(user, mfa_code):
        raise MFARequiredError("需要多因素认证")
    token = generate_token(user)
    return {"token": token, "expires_in": 3600}
```

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
请为以下 Python 函数生成 Google 风格 Docstring：

def authenticate_user(username: str, password: str, mfa_code: str = None) -> dict:
    user = db.find_user(username)
    if not user or not check_password(password, user.password_hash):
        raise AuthenticationError("用户名或密码错误")
    if user.mfa_enabled and not verify_mfa(user, mfa_code):
        raise MFARequiredError("需要多因素认证")
    token = generate_token(user)
    return {"token": token, "expires_in": 3600}

要求：
- 中文单行总结
- Args 部分包含类型和说明
- Returns 部分展示返回 dict 的结构（字段说明）
- Raises 部分列出两种异常及触发条件
- Examples 部分展示成功和失败用例
只输出 Docstring，不需要重写函数
EOF
```

**结果与验证**

生成的 Docstring 示例：
```python
"""验证用户登录凭证并返回访问令牌。

根据用户名和密码进行身份验证，支持多因素认证（MFA）。
成功后返回 JWT 令牌，失败则抛出对应异常。

Args:
    username (str): 用户账户名。
    password (str): 明文密码（函数内部会进行 hash 比对）。
    mfa_code (str, optional): MFA 验证码，当账户启用 MFA 时必填。

Returns:
    dict: 包含以下字段：
        - token (str): JWT 访问令牌。
        - expires_in (int): 令牌有效期（单位：秒）。

Raises:
    AuthenticationError: 用户名或密码不匹配时抛出。
    MFARequiredError: 账户启用了 MFA 但未提供或验证码错误时抛出。

Examples:
    >>> result = authenticate_user("admin", "secret123")
    >>> result["expires_in"]
    3600

    >>> authenticate_user("admin", "wrong_password")
    # 抛出 AuthenticationError
"""
```

**常见陷阱与对策**

- ❌ 陷阱 1：生成的 Docstring 说明太笼统，没有参数类型信息
  - ✅ 对策：在提示中明确"Args 部分必须包含类型注释，格式为 `name (type): 说明`"
- ❌ 陷阱 2：复杂返回值没有展开字段说明
  - ✅ 对策：要求"Returns dict 时，展示所有 key 的含义和类型"

---

### 场景 2: 为 JavaScript REST API 生成 Swagger 注解

**问题描述**

你的 Express API 没有任何文档，新加入的前端同学完全不知道接口格式。需要快速为端点添加 Swagger 注解，生成可交互的 API 文档。

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
请为以下 Express 路由生成 swagger-jsdoc 格式的 @swagger JSDoc 注解：

router.post('/api/users', async (req, res) => {
  const { name, email, role } = req.body;
  const user = await User.create({ name, email, role });
  res.status(201).json({ id: user.id, name, email });
});

要求：
- 使用 @swagger 标签
- 包含 summary、description、requestBody（JSON schema）
- 包含 responses：201（成功，展示返回结构）、400（参数错误）、409（邮箱重复）
- requestBody 的 schema 包含 name、email、role 三个字段
只输出 JSDoc 注释块，不重写函数
EOF
```

**结果与验证**

添加注解后，启动服务访问 `/api-docs/` 验证文档是否正确渲染，并尝试"Try it out"功能。

---

### 场景 3: 批量为模块生成 README

**问题描述**

项目中有多个子模块没有 README，需要快速为每个模块生成基础说明文档。

**Copilot CLI 解决方案**

```bash
# 提供模块入口文件，让 Copilot 分析并生成 README
cat src/auth/index.py | gh copilot suggest "为这个 Python 模块生成 README，
包含：模块简介、主要功能列表、安装依赖、快速使用示例（3 个场景）、配置项说明。
使用中文，Markdown 格式。"
```

---

## 最佳实践速查表

| 任务 | 提示关键词 | 注意事项 |
|------|-----------|---------|
| Python Docstring | "Google 风格，包含 Args/Returns/Raises/Examples" | 提供完整函数代码 |
| JavaScript JSDoc | "@swagger 格式，包含 requestBody 和 responses" | 说明状态码和 Schema |
| 模块 README | "包含简介、安装、用法、配置" | 提供入口文件 |
| 批量补充注释 | "为所有公开函数生成单行注释" | 小文件逐个处理 |

---

## 常见错误与调试

| 问题 | 症状 | 原因 | 解决方案 |
|------|------|------|---------|
| Docstring 格式不规范 | 缩进不对，IDE 解析失败 | 提示未指定格式 | 明确"Google 风格"或"JSDoc"风格 |
| Swagger 文档渲染不出来 | 路径映射错误 | 注解路径与实际路由不匹配 | 让 Copilot 根据实际路由生成 |
| 生成内容过于简洁 | 描述只有一句话 | 提示不够具体 | 列出期望包含的每个部分 |

---

## 与其他模块的关系

- **前置模块**：M2（提示工程基础 — 编写精准的文档生成提示）、M4（测试生成 — 可同时为测试函数生成 Docstring）
- **相关模块**：M6（代码审查 — 审查时检查文档是否完整）
- **后续模块**：M6（代码审查加速）

---

## 进阶延伸

- **文档版本控制**：将 OpenAPI 规范文件纳入 Git，配合 Swagger Diff 工具检测 breaking changes
- **自动化集成**：CI/CD 中自动更新文档（配合 M8 工作流集成）
- **多语言文档**：让 Copilot 生成中英双语 Docstring（`# 计算折扣 (calculate discount)`）
- **深度扩展**：参考本模块专题资料 [FastAPI](./m5/tech-stacks/fastapi.md)、[gRPC](./m5/tech-stacks/grpc.md)、[GraphQL](./m5/tech-stacks/graphql.md)

---

## 参考资源

- [Google Python Style Guide — Docstrings](https://google.github.io/styleguide/pyguide.html#38-comments-and-docstrings)
- [JSDoc 官方文档](https://jsdoc.app/)
- [swagger-jsdoc GitHub](https://github.com/Surnet/swagger-jsdoc)
- [FastAPI 自动文档生成](https://fastapi.tiangolo.com/tutorial/first-steps/)
- [本项目 M5 完成总结](./m5/summaries/completion-summary.md)

---

## 反思与迭代

完成本模块学习后，请记录：

- ✅ **学到的最有用的技巧**：哪种文档类型用 Copilot 生成效率最高？
- 🤔 **遇到的主要困难**：生成的文档是否准确反映了代码的实际行为？
- 💡 **改进的空间**：如何把文档生成集成到日常 commit 流程中？

---

**下一步**：[M6: 代码审查加速](./M6-code-review-workflow.md)

*最后更新：2026-05-06*
