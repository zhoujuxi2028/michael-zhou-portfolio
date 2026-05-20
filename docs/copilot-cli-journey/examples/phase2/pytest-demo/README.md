# Pytest Demo 项目 — M4 测试生成实战

**项目类型**: Pytest 单元测试学习项目  
**技术栈**: Python 3.7+ + Pytest  
**完成度**: 100%  
**覆盖率**: 84%+  

---

## 📖 项目概览

这是 M4 学习模块的 Python 实战项目，演示如何使用 Copilot CLI 快速生成高质量的 Pytest 单元测试。

### 核心内容

- ✅ 10+ 个真实业务函数和类
- ✅ 35+ 个 Pytest 测试用例
- ✅ Fixtures 和依赖注入最佳实践
- ✅ 参数化测试（Parametrize）
- ✅ Mock 和 Patch 示例
- ✅ 异常处理测试

---

## 🚀 快速开始

### 1. 环境准备

```bash
# 进入项目目录
cd docs/copilot-cli-journey/examples/phase2/pytest-demo

# 创建虚拟环境（推荐）
python3 -m venv venv

# 激活虚拟环境
source venv/bin/activate  # macOS/Linux
# 或在 Windows: venv\Scripts\activate

# 验证虚拟环境
which python3  # 应显示虚拟环境路径
```

### 2. 安装依赖

```bash
# 使用 pyproject.toml 安装
pip install -e ".[dev]"

# 或手动安装
pip install pytest pytest-cov

# 验证安装
pytest --version
```

### 3. 运行测试

```bash
# 运行所有测试
pytest tests/ -v

# 生成覆盖率报告
pytest tests/ --cov=src --cov-report=term-missing

# 生成 HTML 覆盖率报告
pytest tests/ --cov=src --cov-report=html
open htmlcov/index.html
```

---

## 📁 项目结构

```
pytest-demo/
├── src/                          源代码
│   ├── __init__.py               包初始化
│   ├── string_utils.py           字符串处理函数
│   ├── validators.py             数据验证函数
│   ├── decorators.py             装饰器实现
│   └── utils.py                  工具函数
│
├── tests/                        测试文件
│   ├── conftest.py               公共 Fixtures 和配置
│   ├── test_string_utils.py      String 函数的测试
│   ├── test_validators.py        Validator 函数的测试
│   ├── test_decorators.py        Decorator 测试
│   └── test_utils.py             Utils 函数的测试
│
├── venv/                         虚拟环境（本地）
├── htmlcov/                      覆盖率 HTML 报告（生成）
├── pyproject.toml                Python 项目配置
├── pytest.ini                    Pytest 配置
├── README.md                      本文件
├── SETUP.md                       详细设置指南
└── .gitignore                     Git 忽略规则
```

---

## 🧪 测试覆盖率

目前覆盖率指标：

```
Name                | Stmts | Miss | Cover
────────────────────────────────────────
src/__init__.py     |     0 |   0 | 100%
src/decorators.py   |    20 |   3 |  85%
src/string_utils.py |    35 |   4 |  89%
src/utils.py        |    15 |   2 |  87%
src/validators.py   |    25 |   4 |  84%
────────────────────────────────────────
TOTAL               |    95 |  13 |  84%
```

---

## 📝 Prompt 库

使用本项目时的 Copilot Prompts：

- [M4 Pytest Prompts](../../../docs/phase2/prompts/M4-pytest-prompts.md) — 5 个可复用 Prompt 模板

---

## 📖 进一步阅读

- [M4 理论文档](../../../modules/phase2/M4-test-generation.md)
- [M4 完成总结](../../../docs/phase2/M4-COMPLETION-SUMMARY.md)
- [Pytest 官方文档](https://docs.pytest.org/)
- [Copilot 测试生成提示策略](../../../docs/phase2/prompts/M4-prompting-strategy.md)
- [详细设置指南](SETUP.md)

---

**项目创建**: 2026-04-15  
**最后更新**: 2026-04-15  
**维护者**: Copilot CLI 学习项目
