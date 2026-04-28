# M4: 测试代码生成最佳实践

## 概览

Copilot CLI 最直接的 QA 应用场景之一是**从零快速生成测试代码**。无论是 Pytest 单元测试、Jest 前端测试还是 Playwright E2E 测试，掌握正确的提示方式能让你的测试编写速度提升 3-5 倍。本模块专注于如何写出有效的测试生成提示，以及如何快速提升测试覆盖率。

---

## 核心概念 (理论 ~ 30-40%)

### 概念 1: 测试类型与 Copilot 的适配

不同类型的测试，Copilot CLI 表现各异：

| 测试类型 | 适合用 Copilot 生成？ | 关键提示技巧 |
|----------|---------------------|------------|
| **单元测试** | ✅ 非常适合 | 提供函数签名 + 入参类型 + 预期行为 |
| **集成测试** | ✅ 适合 | 提供 API 规范或接口文档 |
| **E2E 测试** | ✅ 适合 | 提供用户操作流程描述 |
| **性能测试** | 🟡 部分适合 | 提供并发场景和目标指标 |
| **安全测试** | 🟡 部分适合 | 提供攻击场景描述 |

### 概念 2: 高质量测试提示的五要素

有效的测试生成提示需要包含：

1. **框架声明** — 明确告诉 Copilot 用哪个测试框架（Pytest / Jest / Mocha）
2. **被测代码** — 提供完整的函数签名或类定义
3. **测试数量** — 指定具体数字（如"生成 10 个测试"），避免过于简短
4. **测试分类** — 明确需要 happy path、edge case、error handling
5. **格式约束** — 指定不需要注释/说明，只要代码

**示例提示（Pytest）**：
```
使用 Pytest 为以下函数生成 8 个测试用例：

def calculate_discount(price: float, rate: float) -> float:
    ...

要求：
- 3 个 happy path（正常折扣计算）
- 3 个边界值（0%、100%、负数）
- 2 个异常处理（非数字、None 输入）
只输出代码，不需要说明。
```

### 概念 3: Jest vs Pytest 的核心差异

| 特性 | Jest（JavaScript） | Pytest（Python） |
|------|-------------------|-----------------|
| 测试描述风格 | `describe / it / expect` (BDD) | 函数 `def test_...` |
| 断言链 | `expect(x).toBe(y)` | `assert x == y` |
| 覆盖率 | `--coverage`（内置） | `pytest-cov`（外部） |
| Fixture | `beforeEach / beforeAll` | `@pytest.fixture` |
| 参数化 | `test.each` | `@pytest.mark.parametrize` |
| 最适合 | 前端 / JavaScript / TypeScript | 后端 / Python / 数据科学 |

---

## 实战应用 (70% 以上)

### 场景 1: 从函数代码生成单元测试

**问题描述**

你有一个 Python 工具函数，需要快速为它生成覆盖全面的单元测试，但手动写会花 30 分钟。

**被测代码示例**

```python
# string_utils.py
def normalize_email(email: str) -> str:
    """将 email 地址标准化：去除首尾空白，转为小写。"""
    if not email or not isinstance(email, str):
        raise ValueError("email 必须是非空字符串")
    stripped = email.strip()
    if '@' not in stripped:
        raise ValueError(f"无效的 email 格式：{stripped}")
    return stripped.lower()
```

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
使用 Pytest 为以下函数生成 12 个测试用例：

def normalize_email(email: str) -> str:
    if not email or not isinstance(email, str):
        raise ValueError("email 必须是非空字符串")
    stripped = email.strip()
    if '@' not in stripped:
        raise ValueError(f"无效的 email 格式：{stripped}")
    return stripped.lower()

要求：
- 4 个 happy path（大写、空格、混合大小写、正常 email）
- 4 个边界值（只有 @、多个 @、只有空格）
- 4 个异常（None、空字符串、数字、无 @ 符号）
遵循 AAA 模式（Arrange / Act / Assert）
只输出 Python 代码，不需要注释说明
EOF
```

**结果与验证**

Copilot 会生成类似如下的测试文件：

```python
import pytest
from string_utils import normalize_email

def test_normalize_email_uppercase():
    assert normalize_email("USER@EXAMPLE.COM") == "user@example.com"

def test_normalize_email_with_spaces():
    assert normalize_email("  user@example.com  ") == "user@example.com"

# ... 更多测试

def test_normalize_email_raises_on_none():
    with pytest.raises(ValueError):
        normalize_email(None)
```

运行验证：
```bash
pytest tests/test_string_utils.py -v --tb=short
```

**常见陷阱与对策**

- ❌ 陷阱 1：生成的测试导入路径不正确
  - ✅ 对策：在提示中说明"被测文件路径为 `src/utils/string_utils.py`，测试文件放在 `tests/`"
- ❌ 陷阱 2：Copilot 只生成 happy path，遗漏异常测试
  - ✅ 对策：在提示中明确要求"包含 X 个异常处理测试，使用 `pytest.raises`"

---

### 场景 2: 从 API 规范生成集成测试

**问题描述**

你有一份 REST API 接口文档（或 OpenAPI 规范），需要快速生成集成测试覆盖所有端点。

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
使用 Pytest + requests 生成以下 API 端点的集成测试：

API Base URL: https://api.example.com/v1
Authorization: Bearer token（从环境变量 API_TOKEN 读取）

端点列表：
- GET  /users/{id}    —— 获取用户，返回 {id, name, email}
- POST /users         —— 创建用户，body: {name, email}，返回 201
- PUT  /users/{id}    —— 更新用户，返回 200
- DELETE /users/{id}  —— 删除用户，返回 204

要求：
- 每个端点至少 2 个测试（成功 + 失败）
- 使用 pytest fixture 管理 token 和 base_url
- 失败场景：404 / 401 / 400
只输出代码
EOF
```

**结果与验证**

生成后，验证步骤：
1. 运行 `pytest tests/integration/ -v --tb=short`
2. 检查每个端点是否都被覆盖
3. 确认 fixture 配置正确

**常见陷阱与对策**

- ❌ 陷阱 1：生成的测试直接硬编码了 token，有安全风险
  - ✅ 对策：在提示中明确"token 从 `os.environ['API_TOKEN']` 读取，不能硬编码"
- ❌ 陷阱 2：测试间有依赖关系（如 POST 的结果影响 GET）
  - ✅ 对策：要求"每个测试独立，不依赖其他测试的状态"

---

### 场景 3: 提升现有测试的覆盖率

**问题描述**

CI 显示代码覆盖率只有 60%，你需要快速找到未覆盖的分支并补充测试。

**Copilot CLI 解决方案**

```bash
# Step 1: 生成覆盖率报告
pytest --cov=src --cov-report=term-missing

# Step 2: 把未覆盖代码告诉 Copilot
cat << 'EOF' | gh copilot suggest
以下代码的这些行没有测试覆盖（根据 pytest-cov 报告）：

文件：src/auth/validator.py，未覆盖行：45-52, 67-71

def validate_token(token: str) -> dict:
    if not token:
        raise ValueError("token 不能为空")       # 行 45
    if len(token) < 32:
        raise ValueError("token 格式无效")        # 行 47
    try:
        payload = jwt.decode(token, SECRET_KEY)
        if payload.get("exp") < time.time():      # 行 52
            raise TokenExpiredError()
    except jwt.DecodeError:
        raise InvalidTokenError()                 # 行 67

请只为未覆盖的这些行生成对应的 Pytest 测试用例，使用 pytest.raises 处理异常。
EOF
```

**结果与验证**

```bash
# 补充测试后重新运行
pytest --cov=src --cov-report=term-missing
# 目标：从 60% 提升到 80%+
```

**常见陷阱与对策**

- ❌ 陷阱 1：Copilot 生成的测试通过了但覆盖率没提升
  - ✅ 对策：检查测试是否真的触发了目标代码路径，确认导入是否正确

---

## 最佳实践速查表

| 任务 | 推荐提示关键词 | 注意事项 |
|------|--------------|---------|
| 生成单元测试 | "使用 Pytest / Jest 生成 N 个测试" | 提供完整函数代码 |
| 覆盖异常场景 | "包含 X 个异常处理测试，使用 pytest.raises" | 明确异常类型 |
| 生成集成测试 | "使用 requests，fixture 管理 token" | 确保环境变量安全 |
| 提升覆盖率 | "只为行 X-Y 生成测试" | 先跑 coverage 再补充 |
| 生成 Jest 测试 | "使用 describe/it/expect，包含 mock" | 说明 mock 哪些模块 |

---

## 常见错误与调试

| 问题 | 症状 | 原因 | 解决方案 |
|------|------|------|---------|
| 生成代码无法运行 | ImportError / SyntaxError | 路径或框架版本不对 | 在提示中说明具体框架版本 |
| 测试覆盖率没提升 | 测试通过但 coverage 不变 | 测试没触发目标路径 | 检查被测函数是否正确调用 |
| 生成测试重复 | 多个测试逻辑完全相同 | 提示不够具体 | 明确列出每个测试应覆盖的场景 |
| 测试假阳性 | 测试通过但逻辑错误 | Copilot 误解了预期行为 | 在提示中添加具体的输入/输出例子 |

---

## 与其他模块的关系

- **前置模块**：M2（提示工程基础 — 学会如何写出准确的提示）、M3（CLI 生态 — 了解文件输入方式）
- **相关模块**：M5（文档生成 — 同时生成测试和文档）、M9（调试 — 改进低质量的测试建议）
- **后续模块**：M10（API 测试项目集成）、M11（E2E 测试集成）

---

## 进阶延伸

- **参数化测试**：让 Copilot 生成 `@pytest.mark.parametrize` 版本，覆盖更多边界值
- **测试数据工厂**：使用 `factory_boy`（Python）或 `faker.js`（JS）生成测试数据，让 Copilot 帮你写工厂类
- **突变测试（Mutation Testing）**：配合 `mutmut`（Python）或 `stryker`（JS）验证测试质量
- **BDD 风格**：让 Copilot 生成 `Given / When / Then` 格式的测试描述

---

## 参考资源

- [Pytest 官方文档](https://docs.pytest.org/)
- [Jest 官方文档](https://jestjs.io/)
- [pytest-cov 插件](https://pytest-cov.readthedocs.io/)
- [GitHub Copilot CLI 命令参考](https://docs.github.com/copilot/using-github-copilot/using-copilot-in-the-terminal)

---

## 反思与迭代

完成本模块学习后，请记录：

- ✅ **学到的最有用的技巧**：哪种测试类型用 Copilot 生成最省时？
- 🤔 **遇到的主要困难**：生成的测试是否总能直接运行？遇到了哪些错误？
- 💡 **改进的空间**：如何进一步优化你的测试生成提示？

---

**下一步**：[M5: 文档和注释生成工作流](./M5-doc-generation.md)

*最后更新：2026-04-28*
