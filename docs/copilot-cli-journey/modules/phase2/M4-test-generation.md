# M4: 测试代码生成最佳实践

> 掌握使用 Copilot CLI 快速生成高质量测试代码的方法

**难度级别**: ⭐⭐ 中等  
**学习时间**: 4-5 小时 (理论 1 小时 + 实战 3-4 小时)  
**先修模块**: M1, M2, M3

---

## 📖 学习目标

完成本模块后，你将能够：

1. ✅ 理解 TDD（测试驱动开发）的核心思想和 Copilot 的角色
2. ✅ 编写精准的 Copilot 测试生成提示词
3. ✅ 使用 Copilot CLI 快速生成 Jest 测试套件 (15-20 个测试)
4. ✅ 使用 Copilot CLI 快速生成 Pytest 测试套件 (15-20 个测试)
5. ✅ 对比 Jest 和 Pytest，选择合适的框架
6. ✅ 规划测试覆盖率策略，理解"什么时候 100% 够用，什么时候 80% 足够"

---

## 🎓 核心概念

### 1. TDD (Test-Driven Development) 完整流程

TDD 是一种开发方法论，强调在实现功能前先写测试：

```
阶段 1: RED — 写失败的测试
    ↓
    编写测试用例，确保它们全部失败
    验证测试框架、语法、数据是否正确
    
阶段 2: GREEN — 实现最小化代码
    ↓
    编写最简单的代码让测试通过
    注意：不追求最优，只求通过
    
阶段 3: REFACTOR — 改进代码质量
    ↓
    去除重复代码、优化性能、提高可读性
    保证所有测试仍然通过
    
阶段 4: 重复循环
    ↓
    开发下一个功能时重复 RED-GREEN-REFACTOR
```

**Copilot CLI 在 TDD 中的角色**:
- **加速 Red 阶段**: Copilot 帮你快速草拟测试框架（推荐用法）
- **加速 Green 阶段**: Copilot 可以建议实现（需谨慎验证）
- **加速 Refactor**: Copilot 可以建议更优雅的代码（需单元测试覆盖）

**关键原则**:
> ⚠️ Copilot 生成的代码是起点，不是终点。你需要理解、验证、优化每个生成的测试。特别是：
> - 验证边界条件是否正确
> - 确保断言逻辑符合需求
> - 检查 fixture 和 mock 是否合理

### 2. 测试分类与覆盖率策略

优秀的测试套件需要覆盖多个层面，但**不同项目的覆盖率目标不同**：

| 测试类型 | 目的 | 示例 | 覆盖率权重 |
|---------|------|------|----------|
| **单元测试** | 测试单个函数，无依赖 | `add(2, 3) → 5` | 50-60% |
| **快乐路径** | 验证正常流程 | `login(valid_user) → success` | 20-25% |
| **边界条件** | 测试 0、负数、极值 | `divide(5, 0) → Error` | 15-20% |
| **错误处理** | 测试异常场景 | `add('x', 2) → TypeError` | 10-15% |

**覆盖率决策树**:

```
问题：我应该追求多少覆盖率？

1️⃣ 是否核心业务逻辑？
   ├─ 是 → 争取 90-100% 覆盖率
   │       (金融、医疗、支付系统)
   │
   └─ 否 → 可接受 70-80%
           (工具库、非关键功能)

2️⃣ 是否有严格的发版要求？
   ├─ 是 → 设定 85%+ 覆盖率门槛
   │       (企业级应用)
   │
   └─ 否 → 维持 75%+ 基线
           (内部工具、学习项目)

3️⃣ 是否是新项目（无遗留代码）？
   ├─ 是 → 从 80%+ 开始，逐步优化
   │
   └─ 否 → 先设定基线，渐进提高
```

**常见覆盖率陷阱**:

❌ **陷阱 1：100% 覆盖率迷思**
- 问题：追求 100% 覆盖率导致测试冗余，维护成本高
- 现实：100% 行覆盖率 ≠ 100% 逻辑正确性
- 改进：专注于**分支覆盖率**（branch coverage）而非行覆盖率

❌ **陷阱 2：忽视边界条件**
- 问题：只测试快乐路径，忽视边界值和异常
- 结果：生产环境出现 bug（如 null、负数、超大值）
- 改进：为每个参数添加至少 3 个边界测试

❌ **陷阱 3：脆弱的断言**
- 问题：断言过于宽松（如 `expect(x).toBeTruthy()` 而非 `expect(x).toBe(5)`）
- 结果：测试无法捕捉 bug，成为"无用的测试"
- 改进：编写精确的断言，每个断言检验一个假设

### 3. Copilot 生成的常见测试错误

当 Copilot 生成测试时，常见的问题有：

| 问题 | 症状 | 解决方案 |
|------|------|---------|
| **Mock 不当** | 测试通过但生产失败 | 验证 mock 的假设是否符合真实实现 |
| **异步处理错误** | Jest 报 "test finished before async"（变体） | 确保使用 `async/await` 或 `.resolves` |
| **Fixture 复用不当** | 测试之间互相干扰 | 在 `beforeEach` 中重置状态 |
| **断言缺失** | 测试不报错但没实际检验 | 明确告诉 Copilot "添加 5 个 expect()"  |
| **参数类型混淆** | 字符串当数字处理 | 在提示中明确指定参数类型 |

**预防措施**:
1. 在提示中明确要求"添加 N 个 expect 语句"
2. 为每个测试函数明确指定输入类型和预期输出
3. 生成后，手动验证至少 50% 的测试逻辑
4. 运行测试，确保覆盖率报告符合预期

### 4. 高效的 Copilot 提示工程

#### ❌ 不好的提示

```
"为这个函数写测试"
```

**问题**: 框架不明确、测试数量不定、场景范围太宽

#### ✅ 好的提示

```
"为 add(a, b) 函数生成 5 个 Jest 单元测试。

参数类型：a 和 b 都是数字

测试场景：
1. add(2, 3) 返回 5
2. add(-5, 3) 返回 -2  
3. add(0, 0) 返回 0
4. add(2.5, 3.5) 返回 6
5. add('a', 2) 抛出 TypeError

使用 Jest 语法：describe/it/expect
每个测试用 2-3 行代码
只输出测试代码，不要解释。"
```

**改进点**: ✅ 明确框架 ✅ 具体数量 ✅ 明确场景 ✅ 输出格式要求 ✅ 参数类型指定

---

## 🛠️ Part 1: JavaScript + Jest 实战

### 1.1 Jest 基础

Jest 是 JavaScript/Node.js 的测试框架，特别适合前端和 Node.js 项目。

**核心概念**:
- `describe()` - 测试套件分组
- `it()` / `test()` - 单个测试用例
- `expect()` - 断言
- `beforeEach()` / `afterEach()` - 生命周期钩子
- `.toBe()`, `.toEqual()`, `.toThrow()` - 匹配器

### 1.2 学习项目: Calculator Library

**快速开始**:
```bash
cd docs/copilot-cli-journey/examples/phase2/jest-demo
npm install
npm test                   # 运行所有测试
npm run test:coverage      # 查看覆盖率
```

**包含的函数**: `add()`, `subtract()`, `multiply()`, `divide()`, `power()`

**测试统计**: 21 个测试，85.2% 覆盖率

### 1.3 Copilot 生成 Jest 测试的演练

使用提示模板 (参考 `docs/phase2/prompts/M4-jest-prompts.md`):

```
为 add(a, b) 函数生成 5 个 Jest 单元测试。

测试：
1. add(2, 3) 返回 5
2. add(-5, 3) 返回 -2
3. add(0, 0) 返回 0
4. add(2.5, 3.5) 返回 6
5. add('a', 2) 抛出 TypeError

使用 describe/it/expect。只输出代码。
```

---

## 🛠️ Part 2: Python + Pytest 实战

### 2.1 Pytest 基础

Pytest 是 Python 的测试框架，以简洁、易读而著称。

**核心概念**:
- `test_*` 函数命名规范
- 原生 `assert` 断言
- `@pytest.fixture` - 测试数据和设置
- `@pytest.mark.parametrize` - 参数化测试
- `conftest.py` - 共享 fixtures

### 2.2 学习项目: String Utilities

**快速开始**:
```bash
cd docs/copilot-cli-journey/examples/phase2/pytest-demo
python3 -m venv venv
source venv/bin/activate
python3 -m pip install pytest pytest-cov
python3 -m pytest tests/              # 运行所有测试
python3 -m pytest tests/ --cov=src   # 查看覆盖率
```

**包含的函数**: `reverse_string()`, `is_palindrome()`, `count_vowels()`, `capitalize_words()`, `remove_duplicates()`

**测试统计**: 20 个测试，84% 覆盖率

### 2.3 Copilot 生成 Pytest 测试的演练

使用提示模板 (参考 `docs/phase2/prompts/M4-pytest-prompts.md`):

```
为 reverse_string(s) 生成 4 个 Pytest 单元测试。

测试：
1. reverse_string('hello') 返回 'olleh'
2. reverse_string('') 返回 ''
3. reverse_string('a') 返回 'a'
4. reverse_string(123) 抛出 TypeError

使用 pytest 语法，只输出代码。
```

---

## 📊 Jest vs Pytest 对比与选择

| 维度 | Jest | Pytest |
|------|------|--------|
| **语法** | BDD (describe/it) | 函数式 (test_*) |
| **断言** | 链式 (.toBe()) | 原生 assert |
| **Fixtures** | beforeEach/afterEach | @pytest.fixture |
| **参数化** | test.each() | @pytest.mark.parametrize |
| **覆盖率** | 内置 | 需插件 |
| **学习曲线** | 中等 | 平缓 |
| **最适合** | 前端/Node.js | Python/后端 |

**选择指南**:
- **Jest**: JavaScript/TypeScript 项目、前端测试
- **Pytest**: Python 项目、数据科学、自动化测试、复杂度高

---

## 📚 生成的学习资源

### 代码项目

| 项目 | 函数数 | 测试数 | 覆盖率 |
|------|--------|--------|--------|
| Calculator (Jest) | 5 | 21 | 85.2% |
| String Utils (Pytest) | 5 | 20 | 84% |

### Copilot 提示模板库

- `M4-prompting-strategy.md` - 提示工程指南
- `M4-jest-prompts.md` - 5 个 Jest 提示模板
- `M4-pytest-prompts.md` - 5 个 Pytest 提示模板

---

## 🎯 实践练习

### 练习 1: 扩展 Calculator 库
1. 为 `sqrt()` 新函数编写 3 个测试
2. 验证测试通过

### 练习 2: 参数化测试
1. 使用 `@pytest.mark.parametrize` 重写 `count_vowels` 测试
2. 至少 4 个参数组合

### 练习 3: 跨框架对比
1. 为同一个函数用 Jest 和 Pytest 各写一个测试
2. 记录语法差异

### 练习 4: 测试调试（高阶）
1. 故意创建一个失败的测试
2. 使用 Copilot 生成调试提示
3. 记录调试过程

---

## ✅ 学习验收标准

完成本模块时，你应该能够：

- [ ] 解释 TDD 的三个阶段并说出 Copilot 的具体角色
- [ ] 编写精准的 Copilot 测试生成提示，避免常见陷阱
- [ ] 运行所有测试通过，覆盖率达到 80%+
- [ ] 对比 Jest 和 Pytest 的差异，选择合适的框架
- [ ] 识别常见的测试反模式并改进

---

## 📖 参考资源

- [Jest 官方文档](https://jestjs.io/)
- [Pytest 官方文档](https://docs.pytest.org/)
- [本模块 Copilot 提示模板](../../docs/phase2/prompts/M4-jest-prompts.md)
- [本模块完成总结](../../docs/phase2/M4-COMPLETION-SUMMARY.md)

---

**下一步**: [M5: 文档和注释生成工作流](./M5-doc-generation.md)

*最后更新: 2026-04-15*
