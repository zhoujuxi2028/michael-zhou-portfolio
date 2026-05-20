# Copilot CLI 功能实验室

本文件夹用于记录 Copilot CLI 的各项功能实验，包括 /explain, /tests, /diff, /code 等。

## 实验目录结构

- `code-generation/` — /code 功能测试（如何让 Copilot 生成特定代码）
- `explanation/` — /explain 功能测试（代码解释、流程分析）
- `diff-analysis/` — /diff 功能测试（对比分析）
- `test-generation/` — /tests 功能测试（单元测试/集成测试生成）

## 实验工作流

1. 选择实验主题（如"生成登录表单验证函数"）
2. 准备示例代码或上下文
3. 使用 `copilot <command>` 执行 Copilot 操作
4. 将对话和输出保存到 EXPERIMENT_LOG.md
5. 总结学习要点和最佳实践
6. 提交到分支（commit message 包含功能名称）

## 快速命令

- 使用 Copilot /explain 解释代码: `copilot /explain < code.js`
- 使用 Copilot /tests 生成测试: `copilot /tests < function.py`
- 使用 Copilot /diff 分析变更: `copilot /diff < changes.patch`
- 查看 Copilot CLI 帮助: `copilot /help`

## 学习进度

- [ ] /explain — 代码解释和流程分析
- [ ] /tests — 单元测试和集成测试生成
- [ ] /diff — 变更对比和影响分析
- [ ] /code — 代码生成和补全
- [ ] /refactor — 代码重构建议
- [ ] 其他功能 — TBD

---

## 📊 实验成果与学习追踪

详细的学习进度、实验日志和完成报告见主学习中心：

**查看学习进度：**
- 📄 [Phase 1 完成报告](../../docs/copilot-cli-journey/docs/phase1/PHASE1-COMPLETION-REPORT.md) — 阶段总结、质量指标、后续计划

**本分支的角色：**
- 本文件夹（experiments/）专注于**代码示例和实验素材**
- 学习日志和进度跟踪统一在 [copilot-cli-journey](../../docs/copilot-cli-journey/) 中维护
- 这样保持了学习中心的信息一致性和实验分支的简洁性

---

更多详情见 ../../docs/copilot-cli-journey/
