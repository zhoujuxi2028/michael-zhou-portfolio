# M6: 代码审查加速工作流 - Prompt 库

> 📚 **Prompt 库内容**: 8 个精心设计的代码审查 Prompt 模板，覆盖性能检查、安全审查、可读性审查、PR 生成、重构建议、GitHub 集成等场景。

---

## 📋 快速导航

| 模板 | 场景 | 核心功能 |
|------|------|--------|
| **T1** | 性能问题检查 | N+1 查询、循环复杂度、缓存优化 |
| **T2** | 安全漏洞审查 | SQL 注入、XSS、权限控制、加密 |
| **T3** | 可读性审查 | 变量命名、复杂度、代码规范 |
| **T4** | PR 描述生成 | 变更摘要、范围、测试、破坏性变更 |
| **T5** | 破坏性变更识别 | API 变更、配置变更、迁移说明 |
| **T6** | 代码优化建议 | 性能、内存、算法改进 |
| **T7** | 重构方案生成 | 设计模式、依赖关系、可维护性 |
| **T8** | GitHub Actions 集成 | 自动审查脚本、CI/CD 配置 |

---

## 📖 使用指南

### 如何选择合适的 Prompt？

1. **快速诊断** → 使用 T1-T3（单维度检查）
2. **生成 PR 描述** → 使用 T4（流程级）
3. **深度分析** → 组合使用 T6-T7（优化 + 重构）
4. **自动化集成** → 使用 T8（CI/CD 流程）

### Prompt 使用流程

```
1. 复制完整的 Prompt 文本（包括 {{变量}} 部分）
2. 将 {{变量}} 替换为实际的代码/路径
3. 在 Copilot 中粘贴并执行
4. 查看输出，迭代改进 Prompt（如需要）
5. 记录成功的 Prompt 变体供后续复用
```

---

## 🎯 Prompt 库详解

### T1: 性能问题检查 Prompt

**应用场景**: 检查代码中的性能瓶颈、N+1 查询、不必要的计算

**使用时机**: PR 提交后初审、性能测试前

```text
你是一位资深性能优化工程师，专注于识别代码中的性能问题。

分析以下代码片段，检查以下性能问题：
1. N+1 查询问题（循环内执行数据库查询）
2. 时间复杂度过高（不必要的 O(n²) 或以上）
3. 内存泄漏风险（未释放的资源）
4. 不必要的深拷贝或全量加载
5. 可以缓存但没有缓存的计算

代码片段：
{{CODE_SNIPPET}}

请输出以下内容：
1. [问题识别] 列出找到的性能问题（用 🔴 标记严重，🟡 标记中等，🟢 标记轻微）
2. [根本原因] 对每个问题解释为什么是问题（性能影响、资源消耗）
3. [改进方案] 提供具体的改进代码和优化思路
4. [预期收益] 量化改进的性能提升（时间/内存/请求数）
5. [参考链接] 关联到相关最佳实践文档

输出格式使用 Markdown，包含代码块。
```

**预期输出**:
```markdown
## 性能审查报告

### 🔴 严重问题: N+1 查询

**位置**: 第 15-20 行的 for 循环

**原因**: 每次循环都执行一次数据库查询，共执行 N 次

**改进方案**:
```python
# ❌ 原代码
for user_id in user_ids:
    user = db.query(User).filter(User.id == user_id).first()

# ✅ 改进后
users = db.query(User).filter(User.id.in_(user_ids)).all()
```

**预期收益**: 将数据库查询从 N 次降低到 1 次（性能提升 100 倍）
```

**最佳实践**:
- 提供具体的改进代码，不要只说"可以优化"
- 量化性能改进（百分比或倍数）
- 标记问题严重程度

**常见错误及改进**:
- ❌ "这个循环效率低"
- ✅ "当前是 O(n²)，建议改为 O(n log n)，性能提升 100 倍"

---

### T2: 安全漏洞审查 Prompt

**应用场景**: 检查代码中的安全风险、输入验证、认证授权问题

**使用时机**: 涉及用户输入、认证、敏感数据的 PR

```text
你是一位安全审查专家，专注于识别代码中的安全漏洞。

分析以下代码片段，重点检查：
1. SQL 注入风险（是否使用参数化查询）
2. XSS 漏洞（是否正确转义输出）
3. 认证和授权缺陷（是否有权限检查）
4. 硬编码敏感信息（密钥、密码、Token）
5. 不安全的密码学操作（MD5、自定义加密等）
6. 路径遍历漏洞（目录遍历）
7. 依赖版本安全性（已知漏洞）

代码片段：
{{CODE_SNIPPET}}

基于 OWASP Top 10 标准，输出：
1. [漏洞清单] 列出所有发现的安全漏洞（按严重级别排序）
2. [风险评估] 评估每个漏洞的影响范围和严重程度
3. [修复方案] 提供安全的改进代码
4. [加固建议] 提供额外的安全加固措施
5. [验证方法] 如何验证修复是否有效

安全意见应该具体、可执行、有参考。
```

**预期输出**:
```markdown
## 安全审查报告

### 🔴 严重漏洞: SQL 注入

**代码位置**: query.py 第 42 行

**漏洞描述**: 用户输入直接拼接到 SQL 查询中

❌ 问题代码:
```python
user_input = request.args.get('name')
query = f"SELECT * FROM users WHERE name = '{user_input}'"
```

✅ 修复方案:
```python
user_input = request.args.get('name')
query = "SELECT * FROM users WHERE name = %s"
cursor.execute(query, (user_input,))
```

**风险级别**: CRITICAL (CVSS 9.8)
```

**最佳实践**:
- 参考 OWASP 标准
- 提供安全的替代实现
- 指出具体的攻击场景

**常见错误及改进**:
- ❌ "这里可能有 SQL 注入"
- ✅ "CRITICAL: SQL 注入风险，攻击者可通过 name 参数执行任意 SQL，建议改为参数化查询"

---

### T3: 可读性审查 Prompt

**应用场景**: 检查变量命名、代码复杂度、注释完整性、代码风格一致性

**使用时机**: 日常代码审查、代码复杂度高的 PR

```text
你是代码质量和可读性专家。

分析以下代码片段的可读性，检查：
1. 变量名和函数名是否清晰有意义
2. 代码复杂度是否过高（嵌套层级、条件分支）
3. 是否需要注释来解释复杂逻辑
4. 是否遵循命名规范（驼峰、蛇形、常量大写）
5. 类型提示是否完整（Python type hints, TypeScript types）
6. 函数功能是否单一明确
7. 重复代码是否可以提取

代码片段：
{{CODE_SNIPPET}}

输出：
1. [命名评分] 评估变量名和函数名的清晰度（1-10 分）
2. [复杂度分析] 评估圈复杂度和嵌套深度，给出改进建议
3. [文档完整性] 检查是否有必要的 Docstring 和注释
4. [规范检查] 检查是否遵循项目编码规范
5. [改进建议] 具体的代码改进方案
6. [总体评分] 可读性总体评分（1-10 分）

重点突出具体的改进代码，而不仅仅是建议。
```

**预期输出**:
```markdown
## 可读性审查报告

### 命名清晰度: 6/10 ⚠️

**问题**: 函数名 `f()` 和变量 `a`, `b`, `c` 缺乏语义

❌ 原代码:
```python
def f(a, b):
    c = a + b
    d = c * 2
    return d
```

✅ 改进:
```python
def calculate_double_sum(num1: int, num2: int) -> int:
    """计算两个数之和后的 2 倍值"""
    total = num1 + num2
    result = total * 2
    return result
```

### 整体评分: 6/10
```

**最佳实践**:
- 给出清晰的改进代码
- 解释为什么这样改更好

**常见错误及改进**:
- ❌ "变量名太短"
- ✅ "变量名 `a`, `b`, `c` 缺乏语义，建议改为 `first_number`, `second_number`, `sum_result`"

---

### T4: PR 描述自动生成 Prompt

**应用场景**: 基于 git diff 自动生成完整的 Pull Request 描述

**使用时机**: PR 提交时自动执行

```text
你是项目文档和沟通专家。基于以下代码变更生成一份完整、清晰的 PR 描述。

Git diff:
{{GIT_DIFF}}

生成一份包含以下内容的 PR 描述（使用 Markdown 格式）：

## 📝 变更描述
简明扼要地说明这个 PR 的目的和解决的问题

## 🔍 变更范围
列出涉及的主要文件和模块

## ✅ 测试
说明如何测试这个改动（包括测试命令）

## 📋 检查清单
- [ ] 代码通过 Linter 检查
- [ ] 所有测试通过
- [ ] 添加了必要的注释和文档
- [ ] 没有引入新的警告或错误

## ⚠️ 破坏性变更
如有任何破坏性变更（API 变更、配置变更等），说明升级说明

## 🔗 相关 Issues
如有相关 issue，附上链接

PR 描述应该能让审查人快速理解改动的目的、范围和影响。
```

**预期输出**:
```markdown
## 📝 变更描述

修复用户认证模块的 Token 刷新问题。原有问题：旧 Token 在刷新时立即过期，导致短窗口内的请求失败。

## 🔍 变更范围

- `src/auth/token-manager.ts`: 修复 Token 刷新逻辑，增加 30 秒宽限期
- `src/auth/jwt-handler.ts`: 优化 JWT 验证性能（从 O(n) 改为 O(1)）
- `tests/auth/`: 新增 5 个测试用例覆盖 Token 刷新场景

## ✅ 测试

运行: `npm run test:auth` ✓ 所有测试通过（18 个）
```

**最佳实践**:
- 从 diff 准确提炼出改动的本质
- 提供清晰的测试步骤

---

### T5: 破坏性变更识别 Prompt

**应用场景**: 识别是否有 API 破坏性变更、配置变更、需要迁移说明

**使用时机**: 涉及 API、配置、数据结构的 PR

```text
你是版本管理和兼容性专家。

分析以下代码变更，识别是否存在破坏性变更（breaking changes）：

代码变更:
{{GIT_DIFF}}

检查以下方面：
1. 公共 API 签名变更（参数、返回类型）
2. 配置文件结构变更（是否需要迁移）
3. 数据库 Schema 变更（是否需要迁移脚本）
4. 默认行为变更
5. 依赖版本升级（是否有不兼容）

输出：
1. [破坏性变更] 是否存在破坏性变更（Yes/No）
2. [具体变更] 如有，列出所有破坏性变更
3. [影响范围] 评估有多少用户/项目会受到影响
4. [迁移指南] 提供升级和迁移说明
5. [向后兼容方案] 如果可能，提供保持向后兼容的改进方案

如无破坏性变更，明确标记为 ✅ 完全向后兼容
```

**预期输出**:
```markdown
## 破坏性变更分析

### ❌ 检测到破坏性变更

**变更 1: API 签名变更**
```
// 原有 API
export function fetchUsers(page: number, limit: number): Promise<User[]>

// 新 API
export function fetchUsers(options: FetchOptions): Promise<PaginatedResponse>
```

**影响**: 所有直接调用 `fetchUsers` 的代码都需要更新

**迁移指南**:
1. 将位置参数改为 options 对象
2. 返回值结构变更，需要访问 `response.data` 获取用户列表

**建议**: 保留旧 API 作为 deprecated 的包装器
```

---

### T6: 代码优化建议 Prompt

**应用场景**: 提出代码优化建议（性能、内存、算法改进）

**使用时机**: 代码审查、性能优化阶段

```text
你是一位代码优化专家。

分析以下代码片段，提出优化建议：

{{CODE_SNIPPET}}

对代码进行优化分析，检查：
1. 算法复杂度是否最优
2. 数据结构选择是否合理
3. 是否有重复计算可以缓存
4. 是否有可以并行化的操作
5. 是否有不必要的内存分配
6. 编译器优化友好程度

输出：
1. [当前性能] 分析当前代码的时间和空间复杂度
2. [优化方案] 列出 2-3 个优化方案，从简单到复杂排序
3. [性能对比] 对每个方案进行性能分析和对比
4. [推荐方案] 根据代码特点推荐最平衡的方案
5. [实现代码] 提供优化后的代码
6. [成本效益] 说明优化的收益和学习成本

每个优化方案应该有具体的改进代码，而不是概念性建议。
```

**预期输出**:
```markdown
## 代码优化建议

### 当前性能分析

- **时间复杂度**: O(n²) 
- **空间复杂度**: O(n)
- **关键瓶颈**: 双重循环查找重复项

### 优化方案 1: 使用 Set (推荐)

**改进代码**:
```python
def find_duplicates(numbers):
    seen = set()
    duplicates = set()
    for num in numbers:
        if num in seen:
            duplicates.add(num)
        seen.add(num)
    return duplicates
```

**性能**: O(n) 时间，O(n) 空间
**收益**: 速度提升 100 倍
```

---

### T7: 重构建议 Prompt

**应用场景**: 提出架构优化、设计模式改进、代码重构建议

**使用时机**: 代码复杂度高、需要长期维护的代码

```text
你是软件架构和重构专家。

分析以下代码，提出重构建议：

{{CODE_SNIPPET}}

分析代码的架构问题：
1. 是否违反 SOLID 原则（特别是单一职责）
2. 是否有适合应用的设计模式
3. 耦合度是否过高
4. 是否需要提取为单独的类或模块
5. 依赖关系是否清晰

输出：
1. [当前设计评估] 评估代码的架构质量和可维护性
2. [问题分析] 分析具体的设计问题
3. [设计模式建议] 建议应用的设计模式及原因
4. [重构方案] 提供具体的重构代码
5. [改进指标] 量化改进（如耦合度从 0.8 → 0.3）
6. [迭代步骤] 如果是大规模重构，给出分步实施计划

重构建议应该是可实施的，而不是理想化的，考虑到实际项目约束。
```

**预期输出**:
```markdown
## 重构建议

### 当前设计评估

代码存在以下问题：
- 类过大（500+ 行）
- 职责混乱（既处理数据，又处理业务逻辑和 UI）
- 测试困难（高耦合）

### 建议的重构方向

**使用分层架构 (Model-View-ViewModel)**

```
重构前:
```
class UserManager {
    // 数据访问
    // 业务逻辑
    // UI 更新
    // 所有代码混在一起
}
```

重构后:
```
- Model: UserService (数据和业务逻辑)
- ViewModel: UserViewModel (数据转换)
- View: UserComponent (UI 展示)
```
```

---

### T8: GitHub Actions 集成 Prompt

**应用场景**: 生成 GitHub Actions 脚本实现自动代码审查

**使用时机**: 设置 CI/CD 流程、自动化代码检查

```text
你是 GitHub Actions 和 CI/CD 专家。

生成一个 GitHub Actions 工作流，实现自动化代码审查。

项目信息：
- 语言: {{LANGUAGE}} (如 Python, TypeScript, Java 等)
- 框架: {{FRAMEWORK}}
- 测试框架: {{TEST_FRAMEWORK}}
- 代码检查工具: {{LINT_TOOL}} (如 ESLint, Black, Pylint 等)

生成一个完整的 `.github/workflows/code-review.yml` 配置文件，包含：

1. PR 提交时自动触发
2. 代码风格检查（Linting）
3. 单元测试运行和覆盖率检查
4. 安全审查（如 bandit for Python, npm audit for Node.js）
5. 代码重复检查
6. 输出清晰的检查报告
7. 失败时阻止 PR 合并

配置文件应该：
- 简洁但完整
- 有清晰的注释
- 失败时提供有用的错误信息
- 支持快速本地调试

输出完整的 YAML 配置文件，可直接复制使用。
```

**预期输出**:
```yaml
name: Code Review

on:
  pull_request:
    branches: [main, develop]

jobs:
  review:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: ['3.9', '3.10', '3.11']

    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0  # 获取完整历史用于对比

      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: ${{ matrix.python-version }}

      - name: Install dependencies
        run: |
          pip install -r requirements-dev.txt

      - name: Run Linter (Black)
        run: black --check src/

      - name: Run Type Checker (Mypy)
        run: mypy src/

      - name: Run Tests
        run: |
          pytest --cov=src --cov-report=xml tests/

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          file: ./coverage.xml

      - name: Run Security Check (Bandit)
        run: bandit -r src/ -f json -o bandit-report.json || true

      - name: Comment PR with Results
        if: always()
        uses: actions/github-script@v6
        with:
          script: |
            // 脚本：解析审查结果，添加评论到 PR
```

---

## 📊 Prompt 使用频率建议

| Prompt | 使用频率 | 最佳时机 |
|--------|---------|--------|
| T1 (性能) | 每个 PR | 代码审查初期 |
| T2 (安全) | 每个涉及输入/认证的 PR | 代码审查初期 |
| T3 (可读性) | 每个 PR | 代码审查中期 |
| T4 (PR 描述) | 每个 PR | 提交 PR 时 |
| T5 (破坏性变更) | 有 API/配置变更时 | 提交 PR 前 |
| T6 (优化) | 性能相关 PR、定期审查 | 代码优化阶段 |
| T7 (重构) | 需要大规模改动时 | 架构评审时 |
| T8 (GitHub Actions) | 一次性设置 | CI/CD 初始化 |

---

## 🎓 学习建议

### 初级用法（快速上手）
1. 从 T1-T3 开始，理解审查的核心维度
2. 在项目中实际应用 2-3 个 Prompt
3. 观察 Copilot 的输出，调整 Prompt 以符合项目需求

### 中级用法（深度应用）
1. 学习组合使用多个 Prompt（如先用 T1 查性能，再用 T6 优化）
2. 为项目定制 Prompt（添加项目特定的检查项）
3. 集成到 CI/CD 流程（使用 T8）

### 高级用法（完全自动化）
1. 使用 T4 自动生成 PR 描述
2. 使用 T8 在每个 PR 提交时自动审查
3. 建立反馈循环，根据审查结果持续改进 Prompt

---

## 💡 Prompt 优化技巧

### 技巧 1: 明确指定输出格式

✅ 好的例子:
```
输出格式:
1. [问题 ID]: 问题描述
2. [严重程度]: Critical/High/Medium/Low
3. [改进代码]: 具体的改进代码块
```

### 技巧 2: 提供上下文信息

✅ 好的例子:
```
这是一个 React 前端应用，使用 TypeScript + Redux 架构。
代码片段来自用户认证模块...
```

### 技巧 3: 指定输出长度和深度

✅ 好的例子:
```
提供 3-5 个最关键的优化建议（按优先级排序）
避免过于深入的优化建议，优先考虑易于实施的方案
```

### 技巧 4: 设定成功标准

✅ 好的例子:
```
好的审查意见应该满足：
1. 具体指出问题位置（文件名 + 行号）
2. 提供可直接使用的改进代码
3. 解释改进的必要性和好处
```

---

## 🔄 与团队协作

### 分享 Prompt 模板

```markdown
## 项目代码审查 Prompt

我们团队使用以下 Prompt 进行自动化代码审查：

### T1: 性能检查
[粘贴 T1 的 Prompt...]

### 如何使用
1. 复制 Prompt
2. 替换 {{CODE_SNIPPET}}
3. 粘贴到 Copilot
4. 查看输出
```

### 反馈和改进

- 记录哪些 Prompt 最有效
- 收集团队的改进建议
- 定期更新 Prompt 库
- 添加项目特定的检查项

---

## 📚 参考资源

- [Google 代码审查指南](https://google.github.io/styleguide/review/)
- [GitHub PR 最佳实践](https://github.com/features/code-review)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [设计模式参考](https://refactoring.guru/design-patterns)

---

## 📝 快速参考卡

### Prompt 选择流程图

```
需要审查代码?
├─ 性能相关? → 使用 T1
├─ 安全相关? → 使用 T2
├─ 可读性相关? → 使用 T3
├─ 需要生成 PR 描述? → 使用 T4
├─ 有 API 变更? → 使用 T5
├─ 需要优化建议? → 使用 T6
├─ 需要重构方案? → 使用 T7
└─ 需要 CI/CD 集成? → 使用 T8
```

---

**最后更新**: 2026-04-16  
**模块**: M6 代码审查加速工作流  
**Prompt 库版本**: 1.0  
**总 Prompt 数**: 8 个  
**推荐组合**: T1+T2+T3 (完整审查) | T4+T5 (PR 生成) | T8 (自动化)

