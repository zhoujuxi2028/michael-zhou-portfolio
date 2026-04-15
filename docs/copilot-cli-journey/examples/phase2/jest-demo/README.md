# Jest Demo 项目 — M4 测试生成实战

**项目类型**: Jest 单元测试学习项目  
**技术栈**: Node.js + Jest  
**完成度**: 100%  
**覆盖率**: 85%+  

---

## 📖 项目概览

这是 M4 学习模块的 JavaScript 实战项目，演示如何使用 Copilot CLI 快速生成高质量的 Jest 单元测试。

### 核心内容

- ✅ 10+ 个真实业务函数
- ✅ 40+ 个 Jest 测试用例
- ✅ AAA 模式规范实施
- ✅ 边界情况和异常处理
- ✅ 快照测试示例
- ✅ Mock 和 Stub 最佳实践

---

## 🚀 快速开始

### 1. 环境准备

```bash
# 进入项目目录
cd docs/copilot-cli-journey/examples/phase2/jest-demo

# 安装依赖
npm install

# 验证环境
npm --version
node --version
```

### 2. 运行测试

```bash
# 运行所有测试
npm test

# 运行并生成覆盖率报告
npm run test:coverage

# 监视模式（文件变化时自动运行）
npm test -- --watch

# 运行特定测试
npm test -- tests/math.test.js
```

### 3. 查看覆盖率

```bash
# 运行覆盖率报告
npm run test:coverage

# 打开 HTML 覆盖率报告（如 macOS）
open coverage/lcov-report/index.html

# 或者用你喜欢的浏览器打开
coverage/lcov-report/index.html
```

---

## 📁 项目结构

```
jest-demo/
├── src/                          源代码
│   ├── math.js                   数学计算函数
│   ├── string.js                 字符串处理函数
│   ├── validators.js             数据验证函数
│   ├── decorators.js             装饰器实现
│   └── utils.js                  工具函数
│
├── tests/                        测试文件
│   ├── math.test.js              Math 函数的测试
│   ├── string.test.js            String 函数的测试
│   ├── validators.test.js        Validator 函数的测试
│   ├── decorators.test.js        Decorator 测试
│   └── utils.test.js             Utils 函数的测试
│
├── jest.config.js                Jest 配置
├── package.json                  项目配置
├── package-lock.json             依赖锁定
├── README.md                      本文件
├── SETUP.md                       详细设置指南
└── .gitignore                     Git 忽略规则
```

---

## 🧪 测试覆盖率

目前覆盖率指标：

```
Statements   : 85.2% (71/83)
Branches     : 78.9% (52/66)
Functions    : 90.0% (18/20)
Lines        : 85.5% (72/84)
```

---

## 📝 Prompt 库

使用本项目时的 Copilot Prompts：

- [M4 Jest Prompts](../../docs/phase2/prompts/M4-jest-prompts.md) — 5 个可复用 Prompt 模板

---

## 📖 进一步阅读

- [M4 理论文档](../../modules/phase2/M4-test-generation.md)
- [M4 完成总结](../../docs/phase2/M4-COMPLETION-SUMMARY.md)
- [Jest 官方文档](https://jestjs.io/)
- [Copilot 测试生成提示策略](../../docs/phase2/prompts/M4-prompting-strategy.md)

---

**项目创建**: 2026-04-15  
**最后更新**: 2026-04-15  
**维护者**: Copilot CLI 学习项目
