# feature/copilot-learning 使用指南

## 快速开始

```bash
# 切换到学习分支
git checkout feature/copilot-learning

# 查看实验列表
ls -la experiments/

# 查看实验日志
cat docs/copilot-features-log.md
```

## 如何贡献实验

1. **选择实验主题** — 选择 experiments/ 中的某个功能（如 explanation）
2. **准备示例代码** — 在对应文件夹中创建示例文件
3. **运行 Copilot 操作** — 执行 `copilot /explain`, `/tests` 等命令
4. **记录结果** — 将输出和观察写入 `docs/copilot-features-log.md`
5. **提交** — Git commit 并 push 到 feature/copilot-learning

### 示例 Commit Message

```
feat(copilot-learning): add /explain experiment for async functions

- Document Copilot /explain output for async/await patterns
- Record learning insights on error handling
- Add example: fetchUserWithPosts() explanation
```

## 与 main 分支的关系

- **feature/copilot-learning** 是长期存在的学习分支
- 定期更新实验日志和发现，**不与 main 合并**（作为参考分支）
- 每个实验完成后提交到本分支
- 关键成果可摘取（如示例代码）并集成到 copilot-cli-journey 文档中

## 实验成果反馈

当实验有重要发现时：
1. 在 copilot-cli-journey 的对应模块中记录学习要点
2. 更新 M5-M15 中的 Copilot 相关章节
3. 创建 GitHub Issue 追踪重要发现或功能改进建议

---

## 常见 Copilot 命令速查

| 命令 | 用途 | 示例 |
|------|------|------|
| `/explain` | 解释代码逻辑 | `copilot /explain < complex-function.js` |
| `/tests` | 生成测试用例 | `copilot /tests < userService.py` |
| `/diff` | 分析变更影响 | `copilot /diff < changes.patch` |
| `/code` | 生成代码实现 | `copilot /code "write a login validator"` |
| `/refactor` | 代码重构建议 | `copilot /refactor < legacy-code.js` |
| `/help` | 显示帮助 | `copilot /help` |

---

更多信息见 `README.md`
