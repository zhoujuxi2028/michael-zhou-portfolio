# M4 Copilot 测试生成提示工程完全指南

> 掌握如何用精准的提示词指导 Copilot 生成高质量、可维护的测试代码

---

## 什么是好的测试生成提示？

好的提示具有以下特征：

1. **明确框架** - 指定 Jest 或 Pytest，甚至版本号
2. **明确参数类型** - 说明输入参数的数据类型
3. **具体场景** - 列出要测试的场景（Happy path, Edge case, Error）
4. **输出格式** - 明确期望的测试代码格式
5. **测试数量** - 指定生成多少个测试
6. **可验证性** - 给具体的输入-输出例子，而非笼统的"边界条件"
7. **无歧义** - 不留解释空间，给具体例子

---

## 核心提示模板结构

```
【第 1 部分：上下文说明】
I need to generate [Framework] unit tests for [function/class name].

【第 2 部分：详细的功能描述】
Function signature: [完整签名]
Purpose: [函数的目的，1-2 句话]
Parameter types: [明确参数类型，如 a: number, b: number]
Return type: [返回值类型]
Framework: [Jest/Pytest/Other]

【第 3 部分：测试场景列表 - 具体而非抽象】
Generate exactly [N] test cases:
1. [具体例子] → 输入: X, 预期输出: Y
2. [具体例子] → 输入: X, 预期输出: Y
3. [边界情况] → 输入: X, 预期输出: Y 或抛出 ErrorType
...

【第 4 部分：输出格式约束】
Code style:
- Use [describe/it] for grouping (Jest) or [test_*] (Pytest)
- Assertion format: [expect().toBe() or assert]
- Each test case: 2-3 lines max
- No comments, no explanations

Output ONLY code, no prose or explanations.
```

---

## 常见提示错误及改进

### ❌ 错误 1: 太宽泛，没有具体场景

**坏提示**
```
"为 add 函数生成 Jest 测试，包括边界条件"
```

问题：
- "边界条件"定义不明确
- 测试数量不确定（可能 3 个，也可能 10 个）
- 不知道是否包括错误处理

**✅ 改进后的提示**
```
Generate exactly 5 Jest unit tests for add(a, b) function.

Function: add(a, b) - Adds two numbers and returns their sum
Parameters: a (number), b (number)

Test cases:
1. Happy path: add(2, 3) → 5
2. Negative: add(-5, 3) → -2
3. Zero: add(0, 0) → 0
4. Float: add(2.5, 3.5) → 6
5. Error: add('a', 2) → throws TypeError

Use Jest describe/it/expect. Output ONLY code.
```

---

### ❌ 错误 2: 参数类型不明确

**坏提示**
```
"测试 processUser 函数"
```

问题：
- 不知道 processUser 的签名
- 参数是对象？字符串？数组？
- 返回值类型未知

**✅ 改进后的提示**
```
Generate 4 Pytest test cases for processUser(user_data).

Function: processUser(user_data: dict) → bool
Purpose: Validates and processes user data

Parameter types:
- user_data: dict with keys {name: str, email: str, age: int}

Test cases:
1. Valid user: {'name': 'John', 'email': 'john@example.com', 'age': 25} → True
2. Missing field: {'name': 'John', 'age': 25} → raises KeyError
3. Invalid email: {'name': 'John', 'email': 'invalid', 'age': 25} → False
4. Age out of range: {'name': 'John', 'email': 'john@example.com', 'age': 150} → False

Use pytest syntax with assert statements. Output code only.
```

---

### ❌ 错误 3: 异步处理不明确（Jest）

**坏提示**
```
"生成一个测试 fetchData 的 Jest 测试"
```

问题：
- 不知道是异步还是同步
- 不知道是返回 Promise 还是使用 callback

**✅ 改进后的提示**
```
Generate 3 Jest test cases for fetchData(url: string) async function.

Function: fetchData(url: string) → Promise<object>
Purpose: Fetches JSON data from URL and returns parsed object

Test cases:
1. Success: fetchData('https://api.example.com/data') 
   → resolves with {status: 'ok', data: [...]}
2. Network error: fetchData('invalid-url') 
   → rejects with Error('Network error')
3. Invalid JSON: fetchData('https://api.example.com/bad') 
   → rejects with SyntaxError

Use Jest async/await syntax.
Use expect().resolves for success cases.
Use expect().rejects for error cases.
Output code only.
```

---

### ❌ 错误 4: Mock/Fixture 需求不明确

**坏提示**
```
"测试 getUserProfile，这个函数调用数据库"
```

问题：
- 没有说明是否需要 mock 数据库
- 没有说明是否需要 fixture

**✅ 改进后的提示**
```
Generate 3 Pytest test cases for getUserProfile(user_id: int).

Function: getUserProfile(user_id: int) → dict
Purpose: Retrieves user profile from database

Important: Mock the database call using @patch decorator.
Mock should return {'id': 1, 'name': 'John', 'email': 'john@example.com'}

Test cases:
1. Valid user: getUserProfile(1) → returns mocked profile dict
2. Invalid ID: getUserProfile(999) → raises ValueError('User not found')
3. Database error: Mock raises Exception('DB connection failed') 
   → function should raise RuntimeError

Use @patch('module.database.query') decorator.
Use assert for validations.
Output code only.
```

---

## 高级提示模式

### 模式 1: 参数化测试（Pytest）

**提示模板**
```
Generate 1 parametrized Pytest test case using @pytest.mark.parametrize for is_palindrome(s: str).

Function: is_palindrome(s: str) → bool

Use @pytest.mark.parametrize with these input/output pairs:
1. 'racecar' → True
2. 'hello' → False
3. '' → True
4. 'a' → True
5. 'ABA' (case-insensitive) → True

Use a single test_is_palindrome function with parametrize decorator.
Output code only.
```

---

### 模式 2: 测试分组与 Fixture（Jest）

**提示模板**
```
Generate 6 Jest test cases for Calculator class with setup/teardown.

Class: Calculator
Methods: add(a, b), subtract(a, b), multiply(a, b), divide(a, b)

Create a test suite using describe/beforeEach/afterEach:
- beforeEach: instantiate Calculator and initialize it with 0
- Test each method with 1-2 cases
- afterEach: reset calculator

Assertions:
- add(2, 3) → 5
- subtract(10, 5) → 5
- multiply(3, 4) → 12
- divide(10, 2) → 5
- divide(10, 0) → throws Error

Output Jest code with beforeEach/afterEach hooks. Code only.
```

---

### 模式 3: 错误恢复与调试

当 Copilot 生成的测试失败时，使用**调试提示**：

**提示模板**
```
The generated test for splitString(s: str, delimiter: str) is failing.

Test was: 
def test_split():
    assert splitString('a,b,c', ',') == ['a', 'b', 'c']

Error: AssertionError - expected ['a', 'b', 'c'] but got ['a', 'b', 'c', '']

Analyze the issue and generate a corrected test that accounts for trailing delimiters.
Provide 2 fixed test cases that handle:
1. Normal case: 'a,b,c' → ['a', 'b', 'c']
2. Trailing delimiter: 'a,b,c,' → ['a', 'b', 'c']

Output code only.
```

---

## 最佳实践速查表

| 场景 | 提示要点 | 示例 |
|------|--------|------|
| **简单函数** | 明确参数类型 + 3-5 个具体例子 | `add(2, 3) → 5` |
| **异步函数** | 明确 Promise/async + resolve/reject | `await fetchData() → Promise<data>` |
| **类方法** | 明确类实例化 + 方法调用 | `calculator.add(2, 3)` |
| **Mock/Stub** | 明确 mock 哪个依赖 + 预期返回值 | `@patch('db.query') returns {...}` |
| **参数化** | 明确使用 @pytest.mark.parametrize 或 test.each | `[[2,3,5], [1,1,2]]` |
| **错误处理** | 明确异常类型 + 触发条件 | `input='invalid' → TypeError` |

---

## 提示词反思清单

使用这个清单检查你的提示是否足够精准：

- [ ] 明确了框架吗？(Jest/Pytest/Other)
- [ ] 明确了参数类型吗？(string/number/object/etc)
- [ ] 明确了返回值类型吗？
- [ ] 给了 3+ 个具体的输入-输出例子吗？
- [ ] 包括了边界条件和错误处理吗？
- [ ] 测试数量明确了吗？
- [ ] 如果涉及异步，是否明确了 Promise/async 用法？
- [ ] 如果涉及 mock，是否明确了 mock 哪个部分？
- [ ] 输出格式要求明确了吗？
- [ ] 是否说了"Output ONLY code, no prose"？

如果有任何一个打不上勾，重新写提示。

---

## 实战案例：从坏提示改进到好提示

**初版（坏）**
```
为 formatDate 函数生成测试
```

**问题**: 太简洁，没有信息

**第二版（还是不够）**
```
为 formatDate(date, format) 生成 Jest 测试，包括各种日期格式
```

**问题**: format 参数的值未明确，"各种日期格式"太模糊

**第三版（基本可用）**
```
Generate 4 Jest test cases for formatDate(date: Date, format: string).
Test cases should include:
1. 'yyyy-MM-dd' format
2. 'dd/MM/yyyy' format
3. Invalid format
4. Null date

Output Jest code only.
```

**问题**: 还是缺少具体的输入-输出

**最终版（精准）** ✅
```
Generate 4 Jest test cases for formatDate(date: Date, format: string) → string.

Parameters:
- date: JavaScript Date object
- format: string with pattern like 'yyyy-MM-dd', 'MM/dd/yyyy', 'dd MMM yyyy'

Test cases - EXACT EXAMPLES:
1. formatDate(new Date('2024-01-15'), 'yyyy-MM-dd') → '2024-01-15'
2. formatDate(new Date('2024-01-15'), 'dd/MM/yyyy') → '15/01/2024'
3. formatDate(new Date('2024-12-25'), 'dd MMM yyyy') → '25 Dec 2024'
4. formatDate(null, 'yyyy-MM-dd') → throws TypeError('Date is required')

Use Jest describe/it/expect().toBe() and expect().toThrow().
Each test: 2-3 lines max.
Code only, no explanations.
```

---

## 常见 Copilot 生成失败及恢复方案

### 情景 1: 生成的测试不完整（缺少断言）

**症状**: 测试通过但没有实际检验
```javascript
it('should add two numbers', () => {
  add(2, 3);  // 缺少 expect!
});
```

**恢复提示**:
```
The test above is incomplete - it's missing assertions.
Rewrite this test with at least 3 expect() statements:
- Verify return value is 5
- Verify arguments are not modified
- Verify it works with negative numbers
```

---

### 情景 2: Mock 配置错误

**症状**: 测试通过但没有真正 mock 依赖
```python
def test_fetch_user():
    # 真的调用了网络 API！
    result = fetch_user(1)
```

**恢复提示**:
```
This test is calling the real API instead of mocking it.
Rewrite using @patch('requests.get') to mock the HTTP call.
The mock should return {'id': 1, 'name': 'John'}.
Write 2 test cases:
1. Successful response
2. Network error
```

---

### 情景 3: 异步处理错误（Jest）

**症状**: 异步测试在运行完成前就结束了
```javascript
it('should fetch data', () => {
  fetchData().then(data => {
    expect(data).toBeDefined();
  });
  // Jest 不等待 Promise！
});
```

**恢复提示**:
```
The test is not properly waiting for the Promise to resolve.
Rewrite using async/await syntax with expect().resolves:

it('should fetch data', async () => {
  const data = await fetchData();
  expect(data).toBeDefined();
});

Also test the error case using expect().rejects.
```

---

## 实践建议

1. **每次生成测试后，检查**：
   - [ ] 所有测试都有 expect 语句吗？
   - [ ] Mock 是否正确配置？
   - [ ] 异步函数是否使用了正确的等待机制？
   - [ ] 覆盖率是否达到 80%+？

2. **保存高质量的提示**：
   - 为常见场景（CRUD、API、工具函数）建立提示库
   - 复用这些提示，微调参数即可

3. **逐步迭代**：
   - 不要期望第一次生成完美的测试
   - 生成后检查并用调试提示修正
   - 最后验证测试确实有效

---

*最后更新: 2026-04-15*
