# M10: API 测试项目集成

## 概览

API 测试是 QA 工作中最高频的场景之一。本模块专注于如何在**真实的 API 测试项目中**应用 Copilot CLI，重点覆盖：Postman/Newman 集成、自动生成测试脚本、以及将 API 测试融入 CI/CD 流水线。通过本模块的实战案例，你将学会在已有 API 测试基础设施上快速叠加 AI 辅助能力。

---

## 核心概念 (理论 ~ 30-40%)

### 概念 1: API 测试工具链概览

常见的 API 测试工具和 Copilot 的配合方式：

| 工具 | 用途 | Copilot 的作用 |
|------|------|--------------|
| **Postman** | GUI 构建和管理 API 测试 | 生成测试脚本（Pre-request / Tests 部分） |
| **Newman** | 命令行运行 Postman Collection | 生成 Newman 配置和报告脚本 |
| **pytest + requests** | Python API 测试 | 生成测试函数和 fixture |
| **Supertest** | Node.js API 测试 | 生成 Jest + Supertest 测试 |
| **k6** | 性能和负载测试 | 生成 k6 测试脚本（M12 深入讲解） |

### 概念 2: API 测试的三个核心维度

有效的 API 测试需要覆盖：

```
功能测试
  ├─ 正常请求（各类 HTTP 方法）
  ├─ 边界值（空值、最大值、特殊字符）
  └─ 错误场景（无权限、不存在、格式错误）

契约测试
  ├─ 响应 Schema 验证
  ├─ 字段类型和必填项检查
  └─ 版本兼容性

安全测试
  ├─ 认证和授权测试
  ├─ 注入攻击防御
  └─ 敏感信息泄露检查
```

### 概念 3: Copilot 在 API 测试中的最佳切入点

| 切入场景 | 效果 | 说明 |
|---------|------|------|
| 从 OpenAPI 规范生成测试 | ⭐⭐⭐⭐⭐ | 有规范文档时效果最好 |
| 从现有测试生成类似测试 | ⭐⭐⭐⭐ | 保持风格一致 |
| 为新端点快速补充测试 | ⭐⭐⭐⭐ | 提供端点描述即可 |
| 分析测试失败日志 | ⭐⭐⭐⭐ | 快速定位根本原因 |

---

## 实战应用 (70% 以上)

### 场景 1: 从 OpenAPI 规范生成 Pytest 集成测试

**问题描述**

后端团队提供了一份 OpenAPI 3.0 规范文件，你需要为所有端点快速生成集成测试，并确保覆盖成功和失败两种场景。

**Copilot CLI 解决方案**

```bash
# 提供 OpenAPI 规范的关键部分
cat << 'EOF' | gh copilot suggest
根据以下 OpenAPI 3.0 规范，使用 Pytest + requests 生成集成测试：

POST /api/orders
  请求体: { "product_id": int, "quantity": int, "user_id": int }
  成功响应 201: { "order_id": int, "status": "pending", "total_price": float }
  失败响应 400: { "error": "参数无效" }
  失败响应 404: { "error": "商品不存在" }
  失败响应 401: 未认证

GET /api/orders/{id}
  路径参数: id (int)
  成功响应 200: { "order_id": int, "status": string, "items": array }
  失败响应 404: { "error": "订单不存在" }

要求：
- 使用 pytest fixture 管理 base_url 和 headers
- 每个端点至少 3 个测试（成功 + 参数错误 + 认证失败）
- token 从环境变量 API_TOKEN 读取
- 使用 assert response.status_code == X 验证状态码
- 使用 assert "order_id" in response.json() 验证响应结构
只输出代码
EOF
```

> ⚠️ 注意：生成后需要检查 fixture 结构是否与项目的 `conftest.py` 一致，包括 fixture 的 scope（function/module/session）、放置路径，以及是否需要在 `conftest.py` 中注册。

**生成后的文件结构建议**

```
tests/api/
├── conftest.py        # fixtures: base_url, auth_headers
├── test_orders.py     # 订单相关测试
└── test_products.py   # 商品相关测试
```

**常见陷阱与对策**

- ❌ 陷阱 1：生成的测试用硬编码的 URL
  - ✅ 对策：要求"base_url 从 `os.environ['API_BASE_URL']` 读取，不能硬编码"
- ❌ 陷阱 2：测试没有清理创建的数据，导致测试间污染
  - ✅ 对策：要求"每个测试结束后清理创建的资源，或使用唯一 ID 隔离数据"

---

### 场景 2: 为 Postman Collection 生成测试脚本

**问题描述**

你已经有了一个 Postman Collection，但里面的"Tests"标签都是空的。需要为每个请求快速生成标准化的测试断言。

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
请为以下 Postman 请求生成 Tests 标签中的测试脚本（JavaScript）：

请求：POST /api/users/login
请求体：{"username": "test@example.com", "password": "password123"}
预期成功响应：
{
  "token": "eyJhbGci...",
  "user": {"id": 1, "name": "Test User", "role": "admin"},
  "expires_in": 3600
}

要求：
1. 验证状态码 200
2. 验证响应时间 < 2000ms
3. 验证 token 字段存在且非空
4. 验证 user.role 是 "admin"
5. 将 token 保存到 Postman 环境变量 AUTH_TOKEN
使用 pm.test() 格式，只输出脚本代码
EOF
```

**生成结果示例**

```javascript
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});

pm.test("Response time < 2000ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

pm.test("Token is present and non-empty", () => {
    const body = pm.response.json();
    pm.expect(body).to.have.property('token');
    pm.expect(body.token).to.be.a('string').and.not.empty;
});

pm.test("User role is admin", () => {
    const body = pm.response.json();
    pm.expect(body.user.role).to.equal('admin');
});

// 保存 token 到环境变量
const body = pm.response.json();
pm.environment.set("AUTH_TOKEN", body.token);
```

---

### 场景 3: Newman + CI/CD 自动化运行

**问题描述**

你需要在 GitHub Actions 中自动运行 API 测试，并在失败时生成可读的报告。

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
请生成一个 GitHub Actions workflow 文件，实现：
1. 在 PR 时触发
2. 安装 Newman（Postman CLI 工具）
3. 运行 postman/collections/api-tests.json 中的测试集合
4. 使用环境文件 postman/environments/staging.json
5. API_TOKEN 从 GitHub secret 读取
6. 生成 HTML 报告并上传为 artifact
7. 失败时在 PR 评论中发送通知

只输出 YAML 文件内容
EOF
```

---

## 最佳实践速查表

| 任务 | 推荐方式 | 关键要点 |
|------|---------|---------|
| 从 API 规范生成测试 | 提供端点路径 + 请求/响应格式 | 列出所有失败场景 |
| Postman 测试脚本 | 提供请求描述 + 预期结构 | 要求保存关键变量到环境 |
| Newman CI 集成 | 提供运行环境要求 | 包含报告生成和 secret 管理 |
| 分析 API 测试失败 | 传入失败日志 | 要求区分根因和次生错误 |

---

## 常见错误与调试

| 问题 | 症状 | 原因 | 解决方案 |
|------|------|------|---------|
| 生成的测试端口不对 | `Connection refused` | 测试环境配置不对 | 在提示中说明测试环境 URL |
| 测试间有状态依赖 | 某些测试单独运行成功但整体失败 | 测试污染 | 要求每个测试独立，使用唯一 ID |
| Newman 找不到 Collection | `File not found` | 路径配置不对 | 在提示中提供正确的相对路径 |

---

## 与其他模块的关系

- **前置模块**：M4（测试代码生成 — API 测试的基础模式）、M8（工作流集成 — CI/CD 基础）
- **相关模块**：M9（调试 — 分析 API 测试失败）、M11（E2E 测试 — 在 E2E 测试中验证 API）
- **后续模块**：M11（E2E 测试项目集成）

---

## 进阶延伸

- **契约测试**：使用 Pact 框架，让 Copilot 帮你生成消费者/提供者双端测试
- **API Mock 生成**：用 Copilot 为测试环境生成 mock server 配置（WireMock / MSW）
- **测试数据管理**：让 Copilot 生成测试数据工厂脚本，确保测试数据一致性

---

## 参考资源

- [Newman CLI 文档](https://learning.postman.com/docs/collections/using-newman-cli/command-line-integration-with-newman/)
- [pytest-httpretty 或 responses 库](https://github.com/getsentry/responses)（API Mock）
- [OpenAPI 规范](https://swagger.io/specification/)

---

## 反思与迭代

完成本模块学习后，请记录：

- ✅ **学到的最有用的技巧**：Copilot 在 API 测试中最省时的应用场景是？
- 🤔 **遇到的主要困难**：生成的测试是否需要大量修改才能运行？
- 💡 **改进的空间**：如何把 API 测试生成标准化，让新 API 上线时能立刻有测试覆盖？

---

**下一步**：[M11: E2E 测试项目集成](./M11-e2e-testing-integration.md)

*最后更新：2026-04-28*
