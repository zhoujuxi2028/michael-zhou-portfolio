# GitHub Copilot Instructions for Michael Zhou Portfolio

## 语言偏好 (Language Preference)

**请优先使用中文回复所有问题、代码注释和文档。**

### 中文输出规范 (Chinese Output Standards)

1. **代码注释和文档** — 使用中文
   - 类、函数、变量的注释用中文
   - 测试用例的描述用中文
   - README、CLAUDE.md 等文档用中文

2. **技术术语保持英文** — 保持专业性
   - 库名称：`pytest`, `jest`, `k6`, `Grafana`
   - 技术概念：`AAA pattern`, `DRY`, `TDD`, `coverage`
   - API 名称：保持原样（如 `getByRole`, `fireEvent`）

3. **回复和解释** — 优先中文
   - 技术方案讲解用中文
   - 错误排查用中文
   - 学习内容用中文
   - 代码审查反馈用中文

### 双语并存原则 (Bilingual Coexistence)

- 核心代码逻辑保持现有风格（可中可英）
- 新增注释统一中文
- 代码变量名保持一致性（不混用）
- 必要时英中并注，例：
  ```python
  # 计算折扣 (calculate discount)
  def calc_discount():
      pass
  ```

---

## 项目上下文 (Project Context)

**Portfolio Type**: Personal Learning + Cloud Infrastructure

**Main Branch**: `main` (Default)

**Key Focus**: 
- GitHub Copilot CLI 深度学习 (M1-M15)
- Cloud Monitoring Platform
- Learning Documentation & Best Practices

**Key Technologies**: 
- Copilot CLI: Prompting, Code Generation, Workflow Integration
- Cloud: AWS, Monitoring, Infrastructure
- Learning: Documentation, Examples, Case Studies

---

## 开发规范摘要 (Development Standards Summary)

### 流程 (Process)
- 遵循 5 阶段开发流程（需求 → 设计 → 开发 → 测试 → 收尾）
- 每阶段完成后需暂停等待评审
- TDD：先写测试，再实现代码

### 代码质量 (Code Quality)
- **覆盖率目标**: statements ≥ 80%, branches ≥ 70%
- **Linting**: ESLint (Node.js) / Black + Flake8 (Python)
- **Commit规范**: `feat:`, `fix:`, `docs:`, `test:` 开头，附加 Co-authored-by Copilot trailer

### Git规范 (Git Conventions)
- 每 commit 包含 Co-authored-by 信息：
  ```
  Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
  ```

---

## M4-M6 学习计划相关 (M4-M6 Learning Curriculum)

**当前项目**: GitHub Copilot CLI 深度学习路径

**学习范围**: 
- 15 个核心学习模块 (M1-M15)
- 5 个学习阶段
- 30% 理论 + 70% 实战

**完成进度**:
- M4 (测试生成): ✅ 100% 完成 (79 个测试，100% 覆盖率)
- M5 (文档生成): ✅ 完成 (Swagger, Docstring)
- M6+ (高级集成): 进行中

**相关文件**:
- 学习中心：`docs/copilot-cli-journey/`
- M4 完成文档：`docs/copilot-cli-journey/modules/phase2/M4-test-generation.md`
- 实战代码示例：`docs/copilot-cli-journey/examples/`

---

## 快速命令参考 (Quick Commands)

```bash
# 学习中心
cd ~/michael-zhou-portfolio/docs/copilot-cli-journey

# 启动 Copilot CLI
copilot

# 查看学习路线图
cat docs/copilot-cli-journey/README.md

# 查看 M4 学习总结
cat docs/copilot-cli-journey/modules/phase2/M4-test-generation.md

# 查看实战代码示例
ls -la docs/copilot-cli-journey/examples/
```

---

## 需要帮助时 (When You Need Help)

- **学习问题**: 参考 `docs/copilot-cli-journey/README.md`
- **实战示例**: 查看 `docs/copilot-cli-journey/examples/` 目录
- **Copilot 用法**: 执行 `/help` 查看命令列表
- **M4 完成文档**: `docs/copilot-cli-journey/modules/phase2/M4-test-generation.md`

---

**Last Updated**: 2026-04-15  
**Language Priority**: 中文优先 (Chinese First)  
**Repository**: michael-zhou-portfolio (Personal Learning Center)
