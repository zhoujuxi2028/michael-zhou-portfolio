# Pytest Demo 项目 — 详细设置指南

## 📋 前提条件

确保你的系统已安装：
- **Python**: 3.7 或更高版本
- **pip**: Python 包管理器（通常随 Python 一起安装）

检查版本：

```bash
python3 --version    # 应显示 Python 3.7 或更高
pip3 --version       # 应显示 pip 20.0 或更高
```

---

## 🔧 一步步安装

### Step 1: 克隆或下载项目

```bash
# 使用 Git（推荐）
git clone https://github.com/your-repo/michael-zhou-portfolio.git
cd docs/copilot-cli-journey/examples/phase2/pytest-demo

# 或直接导航到项目目录
cd /path/to/pytest-demo
```

### Step 2: 创建虚拟环境

虚拟环境隔离项目依赖，避免与系统 Python 冲突。

```bash
# 创建虚拟环境（名称为 venv）
python3 -m venv venv

# 激活虚拟环境
# 在 macOS/Linux：
source venv/bin/activate

# 在 Windows：
# venv\Scripts\activate

# 验证激活成功
# 命令行前应显示 (venv) 前缀
```

**验证虚拟环境**:

```bash
# 确认 Python 路径指向 venv
which python3  # 应显示 /path/to/venv/bin/python3

# 或检查 pip 版本
pip --version  # 应包含虚拟环境路径
```

### Step 3: 安装依赖

```bash
# 升级 pip（推荐）
pip install --upgrade pip

# 使用 pyproject.toml 安装依赖
pip install -e ".[dev]"

# 或手动安装所需包
pip install pytest pytest-cov

# 验证安装成功
pip list | grep pytest
```

**预期输出**:
```
pytest              29.x.x
pytest-cov          4.x.x
```

### Step 4: 验证环境

```bash
# 验证 Pytest 已正确安装
pytest --version

# 预期输出：pytest X.X.X
```

---

## ▶️ 运行测试

### 快速运行

```bash
# 进入项目目录并激活虚拟环境
cd docs/copilot-cli-journey/examples/phase2/pytest-demo
source venv/bin/activate  # 如需要

# 运行所有测试
pytest

# 或使用 python -m 执行（推荐）
python -m pytest tests/ -v

# 预期：所有测试通过（绿色 ✓）
```

### 生成覆盖率报告

```bash
# 生成覆盖率报告并输出到控制台
pytest tests/ --cov=src --cov-report=term-missing

# 预期输出：
# ─────────────────────────────────────────────────────
# Name         | Stmts | Miss | Cover | Missing
# ─────────────────────────────────────────────────────
# src/...      |   100 |   15 |  85%  | 10, 20-25
```

### 生成 HTML 覆盖率报告

```bash
# 生成 HTML 格式的详细报告
pytest tests/ --cov=src --cov-report=html

# 打开 HTML 报告（macOS）
open htmlcov/index.html

# 或手动用浏览器打开
# htmlcov/index.html
```

### 监视模式（可选，需要 pytest-watch）

```bash
# 安装 pytest-watch（可选）
pip install pytest-watch

# 使用 ptw 监视模式
ptw -- --cov=src tests/

# 快捷键：
# c - 清屏
# r - 运行所有测试
# q - 退出
```

---

## 🔍 Pytest 命令速查

| 命令 | 说明 |
|------|------|
| `pytest` | 运行所有测试 |
| `pytest tests/test_xxx.py` | 运行特定文件 |
| `pytest tests/test_xxx.py::test_function` | 运行特定测试函数 |
| `pytest -v` | 详细输出 |
| `pytest -s` | 显示 print 输出 |
| `pytest --cov=src` | 生成覆盖率报告 |
| `pytest --cov=src --cov-report=html` | HTML 覆盖率报告 |
| `pytest -k "test_name"` | 按名称过滤测试 |
| `pytest --maxfail=1` | 首次失败后停止 |
| `pytest -x` | 首次失败后停止（简写） |

---

## 📁 项目文件说明

### 源代码文件

| 文件 | 说明 | 行数 |
|------|------|------|
| `src/__init__.py` | 包初始化 | - |
| `src/string_utils.py` | 字符串处理函数 | ~50 |
| `src/validators.py` | 数据验证函数 | ~40 |
| `src/decorators.py` | 装饰器实现 | ~35 |
| `src/utils.py` | 工具函数 | ~30 |

### 测试文件

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `tests/conftest.py` | - | 公共 Fixtures 和配置 |
| `tests/test_string_utils.py` | 15 | 字符串函数的测试 |
| `tests/test_validators.py` | 10 | 验证函数的测试 |
| `tests/test_decorators.py` | 8 | 装饰器的功能测试 |
| `tests/test_utils.py` | 5 | 工具函数的测试 |

**总计**: 35+ 个测试用例

---

## ⚙️ Pytest 配置文件说明

### pytest.ini

```ini
[pytest]
testpaths = tests              # 测试目录
python_files = test_*.py       # 测试文件匹配
python_classes = Test*         # 测试类匹配
python_functions = test_       # 测试函数匹配
addopts = -v --tb=short        # 默认选项：详细模式，短错误追踪
```

### pyproject.toml

```toml
[tool.pytest.ini_options]
testpaths = ["tests"]
python_files = ["test_*.py"]
```

---

## 🔄 虚拟环境管理

### 激活虚拟环境

```bash
# 在 macOS/Linux
source venv/bin/activate

# 在 Windows
venv\Scripts\activate

# 在 PowerShell（Windows）
venv\Scripts\Activate.ps1
```

### 停用虚拟环境

```bash
# 在任何系统上
deactivate
```

### 删除虚拟环境

```bash
# 如需完全重置
rm -rf venv

# 然后重新创建
python3 -m venv venv
source venv/bin/activate
pip install -e ".[dev]"
```

---

## 🐛 常见问题和解决方案

### 问题 1：`python3: command not found`

**解决**: 安装 Python

```bash
# 使用 Homebrew（macOS）
brew install python3

# 或从 https://www.python.org/ 下载安装程序
```

### 问题 2：`pip: command not found`

**解决**: 确保虚拟环境已激活

```bash
# 检查虚拟环境是否激活
# 命令行前应显示 (venv) 前缀

# 如果未激活，重新激活
source venv/bin/activate
```

### 问题 3：`ModuleNotFoundError: No module named 'pytest'`

**解决**: 重新安装依赖

```bash
# 确保虚拟环境已激活
source venv/bin/activate

# 重新安装
pip install pytest pytest-cov
```

### 问题 4：`Permission denied` 错误

**解决**: 检查文件权限

```bash
# 添加执行权限
chmod +x venv/bin/activate

# 或重新创建虚拟环境
rm -rf venv
python3 -m venv venv
source venv/bin/activate
```

### 问题 5：测试执行超时

**解决**: 增加超时时间

```bash
pytest tests/ -v --timeout=10
```

---

## 🎯 学习建议

1. **第一步** — 理解虚拟环境：
   - 理解为什么需要虚拟环境
   - 学会激活和停用虚拟环境

2. **第二步** — 理解测试结构：
   - 打开 `tests/conftest.py` 了解 Fixtures
   - 打开 `tests/test_string_utils.py` 查看测试基本结构

3. **第三步** — 使用 Copilot 生成更多测试：
   - 参考 [M4 Pytest Prompts](../../docs/phase2/prompts/M4-pytest-prompts.md)
   - 尝试为新函数生成测试

4. **第四步** — 理解覆盖率指标：
   - 运行 `pytest --cov=src --cov-report=html`
   - 打开 HTML 报告理解不同类型的覆盖率

5. **第五步** — 实践高级特性：
   - 学习 Fixtures（见 `tests/conftest.py`）
   - 学习参数化测试（见 `tests/test_validators.py`）
   - 学习 Mock（见 `tests/test_utils.py`）

---

## 📚 相关资源

- [Pytest 官方文档](https://docs.pytest.org/)
- [Python 虚拟环境指南](https://docs.python.org/3/tutorial/venv.html)
- [M4 理论文档](../../modules/phase2/M4-test-generation.md)
- [M4 完成总结](../../docs/phase2/M4-COMPLETION-SUMMARY.md)
- [M4 Pytest Prompts](../../docs/phase2/prompts/M4-pytest-prompts.md)

---

## ✅ 验证检查清单

运行以下命令验证环境正确设置：

```bash
# 1. 检查 Python 版本
python3 --version

# 2. 进入项目目录
cd docs/copilot-cli-journey/examples/phase2/pytest-demo

# 3. 创建虚拟环境
python3 -m venv venv

# 4. 激活虚拟环境
source venv/bin/activate

# 5. 安装依赖
pip install -e ".[dev]"

# 6. 验证 pytest 安装
pytest --version

# 7. 运行测试
pytest tests/ -v

# 8. 生成覆盖率
pytest tests/ --cov=src --cov-report=html

# 所有命令都成功 ✓
```

如果所有命令都成功，恭喜！环境设置完毕，可以开始学习了。

---

**创建日期**: 2026-04-15  
**最后更新**: 2026-04-15
