# M11 E2E 测试集成 Prompt 模板库

本模板库用于生成 Playwright/Cypress E2E 测试、补全测试骨架，以及生成 Page Object Model 类。

## 快速选择表

| 场景 | 推荐模板 | 目标 |
|------|---------|------|
| 从用户故事生成 Playwright 测试 | T1 用户流程转测试 | 含 POM 结构和等待逻辑 |
| 补全 Cypress 测试骨架 | T2 骨架补全 | 保持现有结构，填充 TODO |
| 生成 Page Object Model 类 | T3 POM 类生成 | 含 TypeScript 类型注解 |

---

## T1：用户流程转 Playwright 测试

```text
使用 Playwright (TypeScript) 为以下用户流程生成 E2E 测试：

URL: {baseURL}
用户流程（按步骤编号）：
{逐步描述操作和验证点}

要求：
- 使用 Page Object Model 结构（为每个主要页面创建对应的 Page 类）
- 选择器优先级：getByRole > getByTestId > getByText，避免 CSS 选择器
- 每个点击或导航后添加等待：await expect(element).toBeVisible() 或 await page.waitForURL()
- 使用 test.beforeEach 处理登录等前置步骤
- 在关键验证点添加截图：await page.screenshot({ path: '{步骤名}.png' })
```

---

## T2：Cypress 测试骨架补全

```text
以下是已有的 Cypress 测试骨架，请补全每个 TODO：

{粘贴含 TODO 注释的 describe/it 代码}

补全约束：
- 基础 URL: {URL}
- 账号信息: {用户名/密码}
- 关键页面路径: {路径列表}
- 选择器使用 cy.get('[data-testid="..."]')（根据功能推断合理的 testId）
- {如有自定义命令} 使用 cy.{命令名}() 自定义命令（假设已在 commands.js 中定义）
只补全 TODO 的部分，保持现有的 describe/it 结构不变
```

---

## T3：Page Object Model 类生成

```text
请为以下页面生成 Playwright TypeScript Page Object 类：

页面：{页面名称}
主要元素：
{逐条描述元素：类型、标识符（placeholder/id/text/role）、用途}

要求：
- 类名：{PageName}Page
- 使用 Playwright 的 Locator（不是 ElementHandle）
- 方法：{列出需要的方法名，如 fill{字段}(), submit(), getError(), waitForSuccess()}
- 完整的 TypeScript 类型注解
- constructor 接收 page: Page 参数
```

---

*最后更新：2026-07-28*
