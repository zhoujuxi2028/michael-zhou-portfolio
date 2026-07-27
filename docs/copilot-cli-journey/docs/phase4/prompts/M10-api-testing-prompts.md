# M10 API 测试集成 Prompt 模板库

本模板库用于从 OpenAPI 规范生成测试、补全 Postman 脚本，以及集成 Newman CI/CD。

## 快速选择表

| 场景 | 推荐模板 | 目标 |
|------|---------|------|
| 从 API 规范生成 pytest 测试 | T1 Pytest 集成测试生成 | 覆盖成功/失败/认证三类场景 |
| 补全 Postman Tests 标签 | T2 Postman 测试脚本 | 生成标准 pm.test() 断言 |
| 生成 Newman CI 工作流 | T3 Newman GitHub Actions | 含报告上传和失败通知 |
| 分析 API 测试失败 | T4 测试失败根因 | 区分根因与环境问题 |

---

## T1：从 OpenAPI 规范生成 Pytest 集成测试

```text
根据以下 OpenAPI 3.0 端点，使用 Pytest + requests 生成集成测试：

{端点描述：路径、请求体、成功/失败响应}

要求：
- 使用 pytest fixture 管理 base_url 和 headers
- base_url 从 os.environ['API_BASE_URL'] 读取，不能硬编码
- 每个端点至少 3 个测试：成功场景 + 参数错误 + 认证失败
- token 从环境变量 API_TOKEN 读取
- 每个测试结束后清理创建的资源（或使用唯一 ID 隔离）
只输出代码
```

---

## T2：Postman 测试脚本生成

```text
请为以下 Postman 请求生成 Tests 标签中的测试脚本（JavaScript）：

请求：{METHOD} {路径}
预期成功响应：{JSON 结构}

要求：
1. 验证状态码 {期望状态码}
2. 验证响应时间 < {毫秒}ms
3. 验证 {关键字段} 存在且非空
4. {如果需要保存变量} 将 {字段} 保存到 Postman 环境变量 {变量名}
使用 pm.test() 格式，只输出脚本代码
```

---

## T3：Newman GitHub Actions 工作流

```text
请生成一个 GitHub Actions workflow 文件，实现：
1. 触发时机：{PR/push/schedule}
2. 安装 Newman（Postman CLI）
3. 运行 {collection路径} 测试集合
4. 环境文件：{environment文件路径}
5. API_TOKEN 从 GitHub secret 读取
6. 生成 HTML 报告并上传为 artifact
7. 测试失败时在 PR 评论中发送通知
只输出 YAML 文件内容
```

---

## T4：API 测试失败根因分析

```text
以上是 API 测试失败的输出。请：
1. 区分根因（API 本身的问题）和环境问题（URL/认证/网络配置）
2. 如果是根因，给出 2-3 个可能的修复方向
3. 如果是环境问题，给出具体的配置检查步骤
用中文简洁回答。
```

---

*最后更新：2026-07-28*
