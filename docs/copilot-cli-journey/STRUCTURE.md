# 文件夹结构说明

本文档用于说明 `docs/copilot-cli-journey/` 内各类学习材料分别放在哪里、各自承担什么作用。

---

## 1. 顶层目录的作用

```text
docs/copilot-cli-journey/
├── README.md
├── INDEX.md
├── STRUCTURE.md
├── template.md
├── modules/
├── docs/
├── examples/
├── LEARNING-BRANCH-GUIDE.md
├── M5-COMPLETION-SUMMARY.md
├── M5-DEEPDIVE-COMPLETE.md
├── M5-TECH-STACK-COMPARISON.md
├── M5-TECH1-FastAPI.md
├── M5-TECH2-gRPC.md
├── M5-TECH3-GraphQL.md
├── M5-python-google-docstring-prompts.md
└── M5-python-docstring-example.py
```

---

## 2. 四类核心材料

### A. 导航类文件

| 文件 | 作用 |
|------|------|
| `README.md` | 整套学习资料的总入口 |
| `INDEX.md` | 按目标、角色、时间选择学习路径 |
| `STRUCTURE.md` | 解释文件夹怎么组织 |
| `template.md` | 后续新增模块时的统一模板 |

---

### B. 模块类文件：`modules/`

这是正式学习内容的主体，按 5 个 Phase 组织。

| 目录 | 内容定位 | 当前状态 |
|------|----------|----------|
| `modules/phase1/` | 基础认知、提示工程、CLI 生态 | ✅ 内容较完整 |
| `modules/phase2/` | 测试生成、文档生成、代码审查 | 🟡 模块骨架已建 |
| `modules/phase3/` | 上下文管理、工作流集成、调试 | 🟡 以占位为主 |
| `modules/phase4/` | API / E2E / 性能测试项目集成 | 🟡 以占位为主 |
| `modules/phase5/` | Workspace、团队规范、知识总结 | 🟡 以占位为主 |

---

### C. 阶段支持文件：`docs/`

这里放学习过程中的“配套材料”，不是主教程正文。

常见内容包括：
- 阶段完成报告
- 评审请求
- 评审反馈
- 自检清单
- 阶段 README

**当前情况**

| 目录 | 状态 | 说明 |
|------|------|------|
| `docs/phase1/` | ✅ 较完整 | 已有总结、清单、反馈、评审资料 |
| `docs/phase2/` ~ `docs/phase5/` | 🟡 占位 | 目录已建立，后续待补充 |

---

### D. 示例与补充资料：`examples/` 和 `M5-*`

#### `examples/`
适合放：
- 小型代码示例
- 演示脚本
- 学习实验产物

当前已按阶段分目录，方便后续扩展。

#### `M5-*`
这是当前目录里最明显的“专题增强资料”，主要围绕 M5 文档生成主题展开，已经超出普通模块页的深度。

| 文件 | 作用 |
|------|------|
| `M5-COMPLETION-SUMMARY.md` | M5 主体完成总结 |
| `M5-DEEPDIVE-COMPLETE.md` | M5 深化学习总览 |
| `M5-TECH-STACK-COMPARISON.md` | 多技术栈文档方案对比 |
| `M5-TECH1-FastAPI.md` | FastAPI 文档生成专题 |
| `M5-TECH2-gRPC.md` | gRPC / Protobuf 文档专题 |
| `M5-TECH3-GraphQL.md` | GraphQL 文档专题 |
| `M5-python-google-docstring-prompts.md` | Python Docstring Prompt 模板 |
| `M5-python-docstring-example.py` | Python Docstring 示例代码 |

---

## 3. 推荐理解方式

可以把整个文件夹理解成三层：

```text
第 1 层：README / INDEX / STRUCTURE
  → 解决“去哪里看”

第 2 层：modules/
  → 解决“真正学什么”

第 3 层：docs/ + examples/ + M5 专题
  → 解决“如何补充理解、复盘和实践”
```

---

## 4. 当前最清晰的阅读入口

### 想看整体
→ [README.md](./README.md)

### 想选路径
→ [INDEX.md](./INDEX.md)

### 想直接进入学习
→ [modules/phase1/](./modules/phase1/)

### 想先看已成熟专题
→ [M5-COMPLETION-SUMMARY.md](./M5-COMPLETION-SUMMARY.md)

---

## 5. 后续整理建议

1. 将 Phase 2-Phase 5 的占位模块逐步补全
2. 把根目录下零散的专题成果，逐步回链到对应模块页
3. 统一旧文档中的历史路径描述，避免与当前目录名不一致
4. 为 `examples/` 增加“示例用途说明”和“对应模块链接”

---

*最后更新：2026-04-28*
