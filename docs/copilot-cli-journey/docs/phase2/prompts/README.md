# Phase 2 Prompt 模板库

> GitHub Copilot CLI 深度学习路径 Phase 2 的所有 Prompt 模板和最佳实践

---

## 📚 模板库概览

| 模块 | 标题 | 模板数 | 行数 | 说明 |
|------|------|--------|------|------|
| **M4** | 测试代码生成 | 12 | 1,000+ | Jest、Pytest、通用策略 |
| **M5** | 文档生成 | 9 | 734 | Docstring、API、README、测试文档 |
| **M6** | 代码审查 | - | - | 待启动 |

---

## 🎯 M4 测试生成 Prompt 模板

### 文件列表
1. **M4-jest-prompts.md** (5 个 Jest 测试模板)
   - T1: 简单函数测试
   - T2: 异步函数测试
   - T3: 错误处理测试
   - T4: 集成测试
   - T5: 覆盖率优化

2. **M4-pytest-prompts.md** (5 个 Pytest 测试模板)
   - T1: 简单函数测试
   - T2: Fixture 使用
   - T3: 参数化测试
   - T4: Mock 和补丁
   - T5: 异常测试

3. **M4-prompting-strategy.md** (通用测试生成策略)
   - 核心提示模板结构
   - 常见错误及改进
   - 快速参考表
   - 质量评估清单

---

## 🎯 M5 文档生成 Prompt 模板

### 文件列表
**M5-doc-generation-prompts.md** (9 个文档生成模板)

| 模板 | 名称 | 适用场景 | 风格 |
|------|------|----------|------|
| **T1** | 函数 Docstring | 单个函数 | Google |
| **T2** | 类和方法 Docstring | Python 类 | Google |
| **T3** | 多语言 Docstring | 中英文并注 | Google |
| **T4** | FastAPI 路由 | REST API | OpenAPI |
| **T5** | Flask 路由 | REST API | OpenAPI |
| **T6** | README 快速版 | 项目介绍 | Markdown |
| **T7** | README 完整版 | 完整文档 | Markdown |
| **T8** | 测试文档 | 测试说明 | Google |
| **T9** | 变更日志 | CHANGELOG | Markdown |

### 特色
- 9 个实用模板，即插即用
- 包含预期输出示例
- 高级模式：错误恢复策略
- 快速参考表和质量检查清单

---

## 📁 完整文件结构

```
prompts/
├── README.md                          ← 本文件
├── M4-jest-prompts.md                 ✅ Jest 测试模板
├── M4-pytest-prompts.md               ✅ Pytest 测试模板
├── M4-prompting-strategy.md           ✅ 通用测试策略
├── M5-doc-generation-prompts.md       ✅ 文档生成模板（9 个）
└── M6-code-review-prompts.md          ⏳ 代码审查模板（筹划中）
```

---

## 🚀 快速开始

### 选择你的任务

**需要生成测试代码？**
→ 查看 `M4-*` 系列文件

**需要生成文档？**
→ 查看 `M5-doc-generation-prompts.md`

**需要进行代码审查？**
→ 待 M6 启动

### 使用 Prompt 模板

1. **选择合适的模板** — 根据你的需求选择 T1-T9
2. **定制 Prompt** — 将模板中的 [占位符] 替换为你的具体代码
3. **在 Copilot CLI 中执行** — 使用 `copilot /generate` 或 `copilot /explain`
4. **验证输出** — 检查生成结果是否满足要求
5. **迭代改进** — 如果需要，调整 Prompt 并重试

---

## 📖 推荐阅读顺序

### 初学者
1. `M4-prompting-strategy.md` — 理解通用的 Prompt 结构
2. `M4-jest-prompts.md` 或 `M4-pytest-prompts.md` — 选择你用的框架
3. `M5-doc-generation-prompts.md` — 学习文档生成

### 进阶使用者
1. 快速浏览所有 Prompt 模板的"常见错误"部分
2. 学习"高级模式"和"错误恢复"部分
3. 参考快速参考表快速查找

---

## 📊 Prompt 统计

| 指标 | M4 | M5 | 总计 |
|------|-----|-----|------|
| 模板数量 | 12 | 9 | 21 |
| 总行数 | 1,000+ | 734 | 1,734+ |
| 包含示例 | 是 | 是 | - |
| 包含错误恢复 | 是 | 是 | - |
| 包含快速参考 | 是 | 是 | - |

---

## 💡 最佳实践

### DO ✅
- ✅ 在 Prompt 中明确指定输出格式
- ✅ 提供具体的代码示例，不要泛泛而谈
- ✅ 列出所有的边界情况或特殊场景
- ✅ 验证 Copilot 的输出，不要无脑接受
- ✅ 遇到问题时查看"常见错误"部分

### DON'T ❌
- ❌ 给出过于宽泛的 Prompt（如"写测试"）
- ❌ 用占位符代替具体代码
- ❌ 一个 Prompt 生成过多的代码
- ❌ 忽视生成结果的质量
- ❌ 对第一次失败放弃

---

## 🔗 相关资源

### 理论学习
- [M4 理论文档](../../../modules/phase2/M4-test-generation.md)
- [M5 理论文档](../../../modules/phase2/M5-doc-generation.md)

### 实战项目
- [Jest 实战项目](../../../examples/phase2/jest-demo/)
- [Pytest 实战项目](../../../examples/phase2/pytest-demo/)
- [M5 Python 文档演示](../../../modules/phase2/m5/examples/python-docstring-example.py)

### 外部参考
- [Google 风格指南](https://google.github.io/styleguide/pyguide.html)
- [Jest 文档](https://jestjs.io/)
- [Pytest 文档](https://docs.pytest.org/)
- [OpenAPI 规范](https://swagger.io/specification/)

---

**最后更新**: 2026-04-15  
**版本**: 1.0  
**状态**: M4 ✅ + M5 ✅ + M6 ⏳
