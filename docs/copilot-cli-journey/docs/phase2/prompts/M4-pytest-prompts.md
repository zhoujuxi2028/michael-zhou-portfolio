# M4 Pytest 测试生成 Prompts

## Prompt 1: 基础函数单元测试

```
I need to generate Pytest unit tests for the [FUNCTION_NAME] function from src/string_utils.py.

Context:
- Function: def [FUNCTION_NAME](s: str) -> [RETURN_TYPE]: ...
- Purpose: [描述函数目的]
- Framework: Pytest
- Test count: 4 tests

Test cases to generate:
1. Happy path: [FUNCTION_NAME]('[input1]') returns [expected1]
2. Edge case: [FUNCTION_NAME]('[input2]') returns [expected2]
3. Boundary: [FUNCTION_NAME]('[input3]') returns [expected3]
4. Error: [FUNCTION_NAME]([invalid]) raises [ERROR_TYPE]

Use Pytest syntax:
- def test_[description](): ...
- assert [FUNCTION_NAME](...) == expected
- pytest.raises([ERROR]) for exceptions

Use fixtures from conftest.py where applicable.

Output ONLY the test code block, no explanations.
```

## Prompt 2: 使用 Fixture 的测试

```
Generate 2 Pytest tests for [FUNCTION_NAME] using fixtures from conftest.py.

Available fixtures:
- sample_string: 'hello'
- palindrome_string: 'racecar'
- mixed_case_string: 'Hello World'
- duplicate_string: 'aabbcc'

Use these fixtures in test functions.
Output ONLY code.
```

## Prompt 3: 参数化测试

```
Generate Pytest parametrized tests for [FUNCTION_NAME] using @pytest.mark.parametrize.

Test these cases:
- ([input1], expected1)
- ([input2], expected2)
- ([input3], expected3)
- ([input4], expected4)

Use: @pytest.mark.parametrize(...)

Output ONLY code.
```
