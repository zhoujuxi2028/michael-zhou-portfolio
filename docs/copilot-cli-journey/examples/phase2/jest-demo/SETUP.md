# Jest Demo 项目 — 详细设置指南

## 📋 前提条件

确保你的系统已安装：
- **Node.js**: v14.0.0 或更高版本
- **npm**: v6.0.0 或更高版本

检查版本：

```bash
node --version    # 应显示 v14.0.0 或更高
npm --version     # 应显示 6.0.0 或更高
```

---

## 🔧 一步步安装

### Step 1: 克隆或下载项目

```bash
# 使用 Git（推荐）
git clone https://github.com/your-repo/michael-zhou-portfolio.git
cd docs/copilot-cli-journey/examples/phase2/jest-demo

# 或直接导航到项目目录
cd /path/to/jest-demo
```

### Step 2: 安装依赖

```bash
# 使用 npm 安装所有依赖
npm install

# 验证安装成功
npm list | head -20
```

**预期输出** — 会看到以下主要依赖：
```
├── jest@29.x.x
└── (其他依赖...)
```

### Step 3: 验证环境

```bash
# 验证 Jest 已正确安装
npm test -- --version

# 预期输出：Jest 29.x.x, Globals 24.x.x, ...
```

---

## ▶️ 运行测试

### 快速运行

```bash
# 运行所有测试（一次性）
npm test

# 预期：所有测试通过（绿色 ✓）
```

### 生成覆盖率报告

```bash
# 生成覆盖率并输出到控制台
npm run test:coverage

# 预期输出：
# ─────────────────────────────────────────────────
# File      | % Stmts | % Branch | % Funcs | % Lines
# ─────────────────────────────────────────────────
# All files |   85.2% |   78.9%  |  90.0%  |  85.5%
```

### 监视模式（开发中使用）

```bash
# 在监视模式中运行测试（文件变化时自动重运行）
npm test -- --watch

# 快捷键：
# a - 运行所有测试
# o - 只运行改动文件相关的测试
# q - 退出监视模式
```

### 运行特定测试

```bash
# 运行某个特定的测试文件
npm test tests/math.test.js

# 运行包含特定名称的测试
npm test -- --testNamePattern="should add"

# 运行某个 describe 块下的所有测试
npm test -- --testNamePattern="Math operations"
```

---

## 📊 生成 HTML 覆盖率报告

```bash
# 生成 HTML 格式的覆盖率报告
npm run test:coverage

# 打开 HTML 报告（macOS）
open coverage/lcov-report/index.html

# 或用浏览器手动打开
# coverage/lcov-report/index.html

# 查看每个文件的覆盖率详情
# 点击文件名可看具体的被覆盖和未覆盖代码行
```

---

## 🔍 Jest 命令速查

| 命令 | 说明 |
|------|------|
| `npm test` | 运行所有测试 |
| `npm test -- --watch` | 监视模式 |
| `npm test -- --coverage` | 生成覆盖率报告 |
| `npm test tests/file.test.js` | 运行特定文件 |
| `npm test -- --testNamePattern="pattern"` | 按名称过滤测试 |
| `npm test -- --bail` | 首次失败后停止 |
| `npm test -- --verbose` | 详细输出 |

---

## 📁 项目文件说明

### 源代码文件

| 文件 | 说明 | 行数 |
|------|------|------|
| `src/math.js` | 数学计算函数（add, subtract, multiply, divide, power） | ~30 |
| `src/string.js` | 字符串处理函数 | ~25 |
| `src/validators.js` | 数据验证函数 | ~20 |
| `src/decorators.js` | 装饰器实现 | ~35 |
| `src/utils.js` | 工具函数 | ~20 |

### 测试文件

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `tests/math.test.js` | 15 | 数学函数的单元测试 |
| `tests/string.test.js` | 12 | 字符串函数的单元测试 |
| `tests/validators.test.js` | 10 | 验证函数的单元测试 |
| `tests/decorators.test.js` | 8 | 装饰器的功能测试 |
| `tests/utils.test.js` | 5 | 工具函数的测试 |

**总计**: 40+ 个测试用例

---

## ⚙️ Jest 配置文件说明

`jest.config.js` 中的关键配置：

```javascript
{
  testEnvironment: 'node',        // 使用 Node.js 环境
  testMatch: ['**/tests/**/*.test.js'],  // 测试文件匹配模式
  collectCoverageFrom: [          // 收集覆盖率的源文件
    'src/**/*.js',
    '!src/**/*.json'
  ],
  coverageThreshold: {            // 覆盖率阈值
    global: {
      statements: 80,
      branches: 75,
      functions: 80,
      lines: 80
    }
  }
}
```

---

## 🐛 常见问题和解决方案

### 问题 1：`npm: command not found`

**解决**: 安装 Node.js

```bash
# 使用 Homebrew（macOS）
brew install node

# 或从 https://nodejs.org/ 下载安装程序
```

### 问题 2：`jest: command not found`

**解决**: 重新安装依赖

```bash
# 清除 node_modules
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

### 问题 3：测试执行超时

**解决**: 增加超时时间

```bash
npm test -- --testTimeout=10000
```

### 问题 4：覆盖率低于阈值

**检查**: 查看未覆盖的代码

```bash
npm run test:coverage
# 打开 coverage/lcov-report/index.html 查看详情
```

---

## 🎯 学习建议

1. **第一步** — 理解测试结构：
   - 打开 `tests/math.test.js` 查看 Jest 测试基本结构
   - 识别 AAA 模式（Arrange-Act-Assert）

2. **第二步** — 使用 Copilot 生成更多测试：
   - 参考 [M4 Jest Prompts](../../docs/phase2/prompts/M4-jest-prompts.md)
   - 尝试为新函数生成测试

3. **第三步** — 理解覆盖率指标：
   - 运行 `npm run test:coverage`
   - 打开 HTML 报告理解不同类型的覆盖率

4. **第四步** — 实践高级特性：
   - 学习 Mock 和 Spy（见 `tests/utils.test.js`）
   - 学习快照测试（见 `tests/decorators.test.js`）

---

## 📚 相关资源

- [Jest 官方文档](https://jestjs.io/docs/getting-started)
- [Jest 测试技巧](https://jestjs.io/docs/tutorial-react#tips)
- [M4 理论文档](../../modules/phase2/M4-test-generation.md)
- [M4 完成总结](../../docs/phase2/M4-COMPLETION-SUMMARY.md)

---

## ✅ 验证检查清单

运行以下命令验证环境正确设置：

```bash
# 1. 检查 Node.js 版本
node --version

# 2. 检查 npm 版本
npm --version

# 3. 进入项目目录
cd docs/copilot-cli-journey/examples/phase2/jest-demo

# 4. 安装依赖
npm install

# 5. 运行测试
npm test

# 6. 生成覆盖率
npm run test:coverage

# 所有命令都成功 ✓
```

如果所有命令都成功，恭喜！环境设置完毕，可以开始学习了。

---

**创建日期**: 2026-04-15  
**最后更新**: 2026-04-15
