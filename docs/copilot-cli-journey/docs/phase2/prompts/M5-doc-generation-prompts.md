# M5 Copilot 文档生成提示工程完全指南

> 掌握如何用精准的提示词指导 Copilot 生成高质量文档、Docstring、API 规范和项目文档

---

## 什么是好的文档生成提示？

好的提示具有以下特征：

1. **明确文档类型** — Docstring / API / README / Test Documentation
2. **明确风格规范** — Google / NumPy / ReStructuredText / Markdown
3. **具体场景** — 代码意图、参数类型、返回值
4. **输出格式** — 明确格式和长度要求
5. **可验证性** — 包含示例和边界情况
6. **无歧义** — 给出具体的输入-输出例子
7. **业务上下文** — 说明为什么这个函数/API 重要

---

## 核心提示模板结构

```
【第 1 部分：上下文说明】
I need to generate [Doc Type] for [Code Element].

【第 2 部分：代码和上下文信息】
Code/Function: [代码段或函数签名]
Purpose: [功能目的，1-2 句话]
Parameters: [参数列表及类型]
Returns: [返回值类型和含义]
Framework/Technology: [Python/FastAPI/Pytest/etc]

【第 3 部分：具体需求】
Requirements:
- Language: [中文/English/Bilingual]
- Style: [Google/NumPy/ReStructuredText/etc]
- Length: [简洁/详细/示例数量]
- Include: [错误处理/示例/业务背景]

【第 4 部分：输出格式约束】
Format requirements:
- [Format specification]
- [Length constraints]
- [Examples if needed]

Output ONLY the documentation, no explanations.
```

---

## Prompt 库：快速参考表

| 场景 | 推荐 Prompt | 风格 | 行数 |
|------|-----------|------|------|
| Docstring (单个函数) | T1 | Google | 20-40 |
| Docstring (类方法) | T2 | Google | 30-50 |
| Docstring 多语言 | T3 | Google | 40-60 |
| API 文档 (FastAPI) | T4 | OpenAPI | 50-100 |
| API 文档 (Flask) | T5 | OpenAPI | 50-100 |
| README 快速版 | T6 | Markdown | 100-200 |
| README 完整版 | T7 | Markdown | 300-500 |
| 测试文档 | T8 | Google | 30-50 |
| 变更日志 | T9 | Markdown | 100-200 |

---

## 模板 T1: 函数 Docstring 生成（Google 风格）

### 使用场景
生成单个 Python 函数的完整 Docstring，包括参数、返回值、异常处理。

### Prompt 模板
```
Generate a Google-style Python docstring for the following function.

Function signature:
def calculate_discount(price: float, rate: float, apply_tax: bool = False) -> float:

Requirements:
1. 中文描述（30-50 字）说明函数的目的
2. Args 部分需要详细说明每个参数：
   - 参数名和类型
   - 参数约束（如范围、格式要求）
   - 默认值说明（如果有）
3. Returns 部分说明返回值的类型和含义
4. Raises 部分列出可能的异常及触发条件
5. Examples 部分提供 2-3 个实际使用示例
   - 至少包括 1 个正常情况
   - 至少包括 1 个异常情况
6. 避免过度详细，保持简洁

Output format:
"""
<完整 docstring，包括三个引号>
"""

Output ONLY the docstring, no code or explanations.
```

### 预期输出
```python
"""计算打折后的价格，支持税费计算。

根据给定的折扣率计算商品的最终价格。支持可选的
税费计算，自动识别百分比率（0-1）和分数率（1-100）。

Args:
    price: 原始价格，必须为正数（大于 0）。类型：float。
    rate: 折扣率，范围 0.0-1.0 表示百分比。类型：float。
    apply_tax: 是否在打折后应用 10% 的税费。默认值为 False。

Returns:
    打折后的价格（float），如果 apply_tax=True，已包含税费。
    返回值保留两位小数。

Raises:
    ValueError: 如果 price ≤ 0 或 rate 不在 0-1 范围内。
    TypeError: 如果参数不是数字类型。

Examples:
    >>> calculate_discount(100, 0.2)
    80.0
    >>> calculate_discount(100, 0.2, apply_tax=True)
    88.0
    >>> calculate_discount(-50, 0.2)
    ValueError: price must be positive
"""
```

### 最佳实践
- 参数说明 ≤ 30 字
- 异常说明明确条件
- 示例包括正常和异常情况

---

## 模板 T2: 类和方法 Docstring 生成

### 使用场景
生成 Python 类及其方法的完整 Docstring。

### Prompt 模板
```
Generate comprehensive Google-style docstrings for the following Python class and its methods.

Class definition:
class OrderProcessor:
    def __init__(self, db_connection, payment_service):
        self.db = db_connection
        self.payment_service = payment_service
    
    def process_order(self, order_id: int, items: List[dict]) -> dict:
        # 处理订单，返回处理结果
        pass
    
    def apply_discount(self, order_amount: float, coupon_code: str) -> float:
        # 应用优惠券
        pass

Requirements:
1. 为 OrderProcessor 类添加总体描述（说明类的职责和使用场景）
2. 为 __init__ 方法生成 Docstring（说明初始化参数的含义）
3. 为每个业务方法生成完整 Docstring：
   - 说明方法的目的和业务意义
   - 参数说明（包括约束）
   - 返回值说明
   - 异常说明
   - 使用示例（2 个）
4. 中英文并注（中文为主，英文术语保留）
5. 避免重复说明，保持简洁

Output format:
"""
<完整的类 docstring>
"""

Then output:
"""
<__init__ docstring>
"""

Then output each method's docstring.

Output ONLY docstrings, no code or explanations.
```

### 最佳实践
- 类 Docstring 说明职责、使用场景和依赖
- 方法 Docstring 说明业务意义
- 参数说明包括业务约束（如 order_id > 0）

---

## 模板 T3: 多语言 Docstring 生成（中英文并注）

### 使用场景
为开源项目或多语言项目生成中英文并注的 Docstring。

### Prompt 模板
```
Generate a bilingual (Chinese + English) Google-style docstring for:

Function:
def send_email(recipient: str, subject: str, body: str, 
               attachments: List[str] = None) -> bool:

Requirements:
1. 主语言为中文，关键术语用英文标注（如 [EN: email service]）
2. Args 部分每个参数先用中文描述，然后用 [EN: description] 标注
3. Returns 部分说明返回值含义
4. Raises 部分列出异常（SMTPException, ValidationError 等）
5. Examples 部分提供 2 个示例，第一个中文注释，第二个英文注释
6. 行长不超过 100 字符（便于代码编辑器显示）

Example format:
"""
发送电子邮件 [EN: Send an email message].

简要描述...

Args:
    recipient: 收件人邮箱地址 [EN: Recipient email address].
    ...
"""

Output ONLY the docstring.
```

### 最佳实践
- 保持行长 < 100 字符
- 关键术语用 [EN: ...] 标注
- 示例中文和英文交替出现

---

## 模板 T4: FastAPI 路由 API 文档生成

### 使用场景
从 FastAPI 路由快速生成完整的 OpenAPI/Swagger 文档。

### Prompt 模板
```
Generate comprehensive OpenAPI/Swagger documentation for the following FastAPI endpoint.

Endpoint code:
@app.post("/api/products")
async def create_product(
    name: str,
    price: float,
    category: str,
    description: Optional[str] = None,
    tags: List[str] = None
):
    # 创建新产品
    pass

Requirements:
1. 为端点生成详细的 summary 和 description
2. 为每个参数生成 Swagger 规范：
   - 参数名、位置（query/path/body）
   - 参数类型、格式、约束（如 minimum, maximum）
   - 参数说明（中文）
   - 默认值和是否必需
3. 生成所有可能的响应 (200, 400, 422, 500)：
   - 状态码说明
   - 响应体 Schema 定义
   - 示例数据（真实的、合理的值）
4. 格式：YAML 或 JSON（请指定选择）
5. Include 标准的 requestBody 定义

Output format:
/api/products:
  post:
    summary: ...
    description: ...
    ...

Output ONLY the OpenAPI definition, no code or explanations.
```

### 预期输出
```yaml
/api/products:
  post:
    summary: 创建新的产品
    description: 向系统中添加一个新产品。需要提供产品名、价格和分类信息。
    requestBody:
      required: true
      content:
        application/json:
          schema:
            type: object
            properties:
              name:
                type: string
                description: 产品名称，长度 1-255 字符
                minLength: 1
                maxLength: 255
              price:
                type: number
                description: 产品价格，必须为正数
                minimum: 0.01
                format: decimal
              category:
                type: string
                description: 产品分类（如 electronics, clothing, food）
              description:
                type: string
                description: 产品描述，可选，长度 ≤ 1000 字符
              tags:
                type: array
                items:
                  type: string
                description: 产品标签列表（最多 10 个）
            required:
              - name
              - price
              - category
    responses:
      200:
        description: 产品创建成功
        content:
          application/json:
            schema:
              type: object
              properties:
                id:
                  type: integer
                  description: 新创建产品的 ID
                name:
                  type: string
                price:
                  type: number
            example:
              id: 12345
              name: "iPhone 15"
              price: 5999.00
      400:
        description: 请求参数无效（如价格为负数）
      422:
        description: 请求体格式不符合 Schema 要求
      500:
        description: 服务器内部错误
```

### 最佳实践
- 参数约束明确（minimum, maximum, format）
- 示例数据要真实合理
- 响应体包括所有可能的错误

---

## 模板 T5: Flask 路由 API 文档生成

### 使用场景
从 Flask 路由生成 API 文档（OpenAPI/Swagger）。

### Prompt 模板
```
Generate OpenAPI/Swagger documentation for the following Flask endpoint.

Endpoint code:
@app.route('/api/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    """获取用户信息"""
    user = User.query.get(user_id)
    if not user:
        return {'error': 'User not found'}, 404
    return {'id': user.id, 'name': user.name, 'email': user.email}, 200

Requirements:
1. 生成 summary 和 description（从代码和注释提取）
2. 路径参数说明：user_id 是正整数，用户 ID
3. 查询参数（如有）：字段过滤、排序、分页等
4. 生成所有可能的响应（200, 400, 404, 500）
5. 响应体 Schema 包含：
   - 字段名和类型
   - 字段说明
   - 示例值
6. Include error response schema

Output format: YAML with complete OpenAPI spec.
Output ONLY the endpoint definition.
```

### 最佳实践
- 从代码注释和错误处理推断响应码
- 包含参数约束（路径参数、查询参数）
- 错误响应包括原因说明

---

## 模板 T6: README 快速版生成

### 使用场景
为新项目快速生成 README 框架，包括介绍、安装、快速开始。

### Prompt 模板
```
Generate a concise README.md for a new Python project.

Project information:
- Name: [Project Name]
- Purpose: [简要说明，1-2 句]
- Main features: [3-5 个主要特性]
- Dependencies: [主要依赖列表]
- Target audience: [目标用户（开发者/数据分析师/etc）]

README structure:
1. Title and description (50 字以内)
2. Key features (3-5 条，每条 ≤ 15 字)
3. Installation (pip/conda 命令)
4. Quick start (2-3 个实际使用示例)
5. Configuration (关键配置说明)
6. Common issues (2-3 个常见问题)
7. License

Requirements:
- Language: 中文 with code examples in English
- Length: ≤ 300 行
- Include: code blocks with proper syntax highlighting
- No verbose explanations; keep it concise

Output format: Markdown
Output ONLY the README content.
```

### 预期长度
100-200 行

### 最佳实践
- 快速版只包括必需信息
- 代码示例可以直接复制使用
- 常见问题针对初次使用者

---

## 模板 T7: README 完整版生成

### 使用场景
为成熟项目生成完整的 README，包括架构、配置、故障排查、贡献指南。

### Prompt 模板
```
Generate a comprehensive README.md for a production Python project.

Project information:
- Name: [Project Name]
- Description: [详细说明，2-3 句]
- Features: [5-10 个特性]
- Architecture: [简要架构说明]
- Tech stack: [使用的主要技术]

README structure:
1. Title + badge (stars, CI/CD status)
2. Overview (200-300 字)
3. Features (bullet list with descriptions)
4. Architecture diagram (ASCII or description)
5. Installation:
   - Prerequisites
   - Step-by-step instructions (pip/conda/docker)
6. Configuration:
   - Environment variables
   - Config file examples
   - Common configurations
7. Usage:
   - Basic usage with examples
   - Advanced usage patterns
   - CLI commands
8. API Documentation
   - Link to detailed API docs
   - Key endpoints summary
9. Examples:
   - Real-world use cases (2-3)
   - Code snippets
10. Testing:
    - How to run tests
    - Test coverage
11. Troubleshooting:
    - Common issues and solutions (5-10)
12. Contributing:
    - How to contribute
    - Development setup
    - Code style guidelines
13. Performance
    - Benchmarks (if applicable)
14. License + links

Requirements:
- Language: 中文 with code/API in English
- Length: 400-600 行
- Include: actual command examples, not templates
- Badges and links must be valid
- Include Table of Contents with jump links

Output ONLY the README content in Markdown.
```

### 预期长度
300-500 行

### 最佳实践
- 包含目录和内部链接
- 架构说明包括核心组件
- 故障排查覆盖常见问题
- 包含开发设置和测试说明

---

## 模板 T8: 测试用例文档生成

### 使用场景
为 Pytest 测试函数补充文档，说明测试目的、场景、预期结果。

### Prompt 模板
```
Add comprehensive docstrings to the following Pytest test functions.

Test functions:
def test_validate_email_with_valid_input():
    result = validate_email('user@example.com')
    assert result is True

def test_validate_email_with_invalid_format():
    result = validate_email('invalid-email')
    assert result is False

def test_validate_email_with_subdomain():
    result = validate_email('user@mail.company.co.uk', allow_subdomains=True)
    assert result is True

Requirements for each docstring:
1. 简单的一行说明：测试的目的
2. Test scenario: 输入条件、测试参数
3. Expected result: 预期返回值或异常
4. Edge cases covered: 边界情况说明（如果有）
5. Business significance: 为什么这个测试重要（1-2 句）

Format: Google-style docstring

Example structure:
"""
验证邮箱格式检查 (Validate email format check).

Test scenario: 有效的电子邮件地址 (Valid email address).
Input: 'user@example.com', no special flags
Expected: 返回 True

Edge cases: 标准的一级域名，无子域名。
Business significance: 确保系统接受标准邮箱格式，支持普通用户。
"""

Output each docstring separately.
Output ONLY the docstrings, no code.
```

### 最佳实践
- 第一行快速说明测试目的
- 场景说明包括输入值
- 业务意义说明为什么这个测试重要

---

## 模板 T9: 变更日志 (CHANGELOG) 生成

### 使用场景
从 Git 提交或功能列表生成结构化的 CHANGELOG。

### Prompt 模板
```
Generate a CHANGELOG.md entry for version X.Y.Z of [Project Name].

Changes in this release:
- [Feature 1]: 完整描述
- [Feature 2]: 完整描述
- [Bug Fix 1]: 完整描述
- [Bug Fix 2]: 完整描述

Requirements:
1. 组织为 Added / Changed / Fixed / Removed / Deprecated sections
2. 每项使用 bullet point
3. Include links to related issues (如 #123) or PRs (如 PR #456)
4. 每项 ≤ 50 字
5. Language: 中文，links in English
6. Include upgrade notes if breaking changes

Format:
## [X.Y.Z] - YYYY-MM-DD

### Added
- [Feature with link]

### Fixed
- [Bug with link]

...

Output ONLY the CHANGELOG section.
```

### 最佳实践
- 分类清晰（Added, Fixed, Changed）
- 每项简洁明确
- 包含链接到相关 Issue/PR

---

## 高级模式：错误恢复

### 场景 1: Copilot 生成的 Docstring 过度详细

**问题**
```python
# Copilot 生成的冗长 Docstring
"""
This function calculates the discount based on the provided parameters.
The function takes a price as input, which should be a floating point number.
It also takes a rate, which represents the discount rate as a decimal number
between 0 and 1. The function returns the discounted price.
...
"""
```

**改进提示**
```
修改 Prompt：
"生成简洁的 Docstring（≤ 30 行）。
Args 部分每个参数只需要 2-3 行说明（参数名、类型、约束）。
避免重复或冗余描述。
示例 ≤ 5 行。"
```

### 场景 2: API 文档缺少错误响应

**问题**
```yaml
responses:
  200:
    description: Success
# 缺少 400, 404, 500 等错误响应
```

**改进提示**
```
修改 Prompt：
"必须包含所有可能的 HTTP 状态码：
- 200: 成功情况
- 400: 请求参数无效
- 404: 资源不存在
- 500: 服务器错误

每个响应包括 description 和 schema 定义。"
```

### 场景 3: README 缺少代码示例

**问题**
readme 中的 Quick Start 没有具体命令

**改进提示**
```
修改 Prompt：
"每个步骤包括实际的、可复制的命令。
不要使用 [PROJECT_NAME] 这样的占位符，
使用真实的项目名称。
代码块必须用三个反引号标记。"
```

---

## 快速参考：常用提示词关键字

| 关键字 | 用途 | 示例 |
|-------|------|------|
| `Output ONLY` | 明确只要输出内容 | Output ONLY the docstring |
| `Exactly` | 明确数量/格式 | Generate exactly 3 examples |
| `Requirements:` | 列出必需项 | Requirements: 1. ... 2. ... |
| `Format:` | 指定输出格式 | Format: Google style |
| `Include:` | 必须包含的内容 | Include error handling |
| `Avoid:` | 避免的内容 | Avoid verbose descriptions |
| `Max/Min` | 长度限制 | Max 50 words |
| `Example` | 给出参考 | Example: `user@example.com` |
| `Language:` | 指定语言 | Language: Chinese with English terms |
| `No explanations` | 不要多余的解释 | No explanations or comments |

---

## 评估生成文档的质量

使用以下检查清单评估 Copilot 生成的文档：

### Docstring 质量检查
- [ ] 函数目的清晰（≤ 30 字）
- [ ] 所有参数都有说明（包括类型、约束）
- [ ] 返回值类型和含义明确
- [ ] 异常列表完整
- [ ] 示例可以直接运行
- [ ] 长度适中（≤ 40 行）

### API 文档质量检查
- [ ] Summary 清晰（≤ 15 字）
- [ ] 所有参数都在 OpenAPI 中定义
- [ ] 响应码完整（200, 400, 404, 500）
- [ ] Schema 包括字段类型和说明
- [ ] 示例数据真实合理
- [ ] 错误响应包括原因说明

### README 质量检查
- [ ] 项目描述清晰
- [ ] 快速开始可以直接执行
- [ ] 代码示例有语法高亮
- [ ] 常见问题覆盖用户最可能遇到的问题
- [ ] 所有链接有效
- [ ] 长度适中（快速版 ≤ 300 行，完整版 ≤ 600 行）

---

## 总结

文档生成的最佳实践：

1. **明确需求** — 使用结构化的提示模板
2. **具体场景** — 给出实际的代码和参数
3. **验证输出** — 检查示例是否可运行、链接是否有效
4. **迭代改进** — 如果第一版不满意，调整提示并重试
5. **保持一致** — 使用相同的风格和结构

---

**最后更新**: 2026-04-15  
**关联模块**: M5-doc-generation.md  
**下一步**: M6 代码审查工作流

