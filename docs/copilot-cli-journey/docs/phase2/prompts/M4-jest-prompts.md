# M4 Jest 测试生成 Prompts

## Prompt 1: 基础函数单元测试

```
I need to generate Jest unit tests for the [FUNCTION_NAME] function from src/calculator.js.

Context:
- Function: export function [FUNCTION_NAME](a, b) { ... }
- Purpose: [描述函数目的]
- Framework: Jest
- Test count: 4 tests

Test cases to generate:
1. Happy path: [FUNCTION_NAME]([input1], [input2]) returns [expected1]
2. Edge case: [FUNCTION_NAME]([input2], [input3]) returns [expected2]
3. Boundary: [FUNCTION_NAME]([input3], [input4]) returns [expected3]
4. Error: [FUNCTION_NAME]([invalid], [input]) throws [ERROR_TYPE]

Use Jest syntax:
- describe('[FUNCTION_NAME]', () => { ... })
- it('should ...', () => { ... })
- expect(...).toBe(...) or expect(...).toThrow(...)

Output ONLY the test code block, no explanations.
```

## Prompt 2: 边界条件测试

```
Generate 3 Jest tests for boundary conditions of [FUNCTION_NAME].

Test edge cases:
1. Zero/empty values
2. Negative numbers
3. Very large numbers

Use describe/it/expect syntax.
Output ONLY code.
```

## Prompt 3: 使用 test.each 的参数化测试

```
Generate a Jest parametrized test for [FUNCTION_NAME] using test.each().

Test these input/output pairs:
- [[input1], expected1]
- [[input2], expected2]
- [[input3], expected3]
- [[input4], expected4]

Use: test.each([...])('should ...', ...)

Output ONLY the test code.
```
