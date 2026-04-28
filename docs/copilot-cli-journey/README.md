# GitHub Copilot CLI 学习资料总览

这里是仓库中 **GitHub Copilot CLI 深度学习路径** 的主入口，目标是把分散的学习材料整理成一套“**先看什么、再学什么、遇到问题去哪里找**”都很清楚的知识库。

---

## 1. 这套资料适合谁

- 想系统学习 **GitHub Copilot CLI** 的开发者或 QA
- 想把 Copilot 用到 **测试生成、文档生成、代码审查、工作流集成** 中的人
- 想把零散笔记沉淀为 **可复用学习路径** 的个人学习者

---

## 2. 先看这 3 个入口

### 如果你第一次进入这个文件夹
1. 先看本文件：了解整体结构和当前完成度
2. 再看 [INDEX.md](./INDEX.md)：按目标选择学习路径
3. 最后进入 `modules/`：开始具体模块学习

### 如果你只想快速开始
- 从 [M1：Copilot CLI 基础](./modules/phase1/M1-copilot-cli-basics.md) 开始
- 然后看 [M2：提示工程基础](./modules/phase1/M2-prompting-fundamentals.md)
- 再看 [M3：Copilot CLI 生态初探](./modules/phase1/M3-cli-ecosystem.md)

### 如果你想看已经比较完整的成果
- [Phase 1 模块](./modules/phase1/)
- [M5 完成总结](./M5-COMPLETION-SUMMARY.md)
- [M5 深化学习总结](./M5-DEEPDIVE-COMPLETE.md)

---

## 3. 当前资料完成度

| 区域 | 状态 | 说明 |
|------|------|------|
| Phase 1（M1-M3） | ✅ 已完成 | 基础概念、提示工程、CLI 生态已形成完整学习内容 |
| Phase 2（M4-M6） | 🟡 部分完成 | 模块首页仍是占位，但 M5 已补充完成总结和专题资料 |
| Phase 3（M7-M9） | 🟡 待扩展 | 已有模块骨架，便于后续继续填充 |
| Phase 4（M10-M12） | 🟡 待扩展 | 已有项目集成主题，但正文仍需完善 |
| Phase 5（M13-M15） | 🟡 待扩展 | 已建立长期演进方向和占位结构 |
| 支持文档 `docs/` | 🟡 Phase 1 较完整 | 评审、总结、清单类资料主要集中在 Phase 1 |
| 示例资料 `examples/` | 🟡 以占位为主 | 已按阶段分目录，后续可持续补充 |

---

## 4. 学习材料怎么分层看

### A. 主学习内容：`modules/`
这是最重要的部分，按 Phase 划分 15 个模块。

- `phase1/`：基础入门
- `phase2/`：测试、文档、审查工作流
- `phase3/`：上下文、脚本、调试
- `phase4/`：真实项目集成
- `phase5/`：团队规范与知识沉淀

### B. 支持资料：`docs/`
这里放的是学习过程中的辅助文档，例如：

- 阶段完成报告
- 评审请求
- 自检清单
- 阶段总结

### C. 示例与脚本：`examples/`
这里放可运行示例、演示代码或阶段性的实验结果，适合“边看边练”。

### D. 专题补充资料：当前根目录下的 `M5-*`
这些文件是对某些模块的深入展开，尤其是 M5 文档生成主题，已经形成了较多可直接复用的材料，例如：

- [M5 完成总结](./M5-COMPLETION-SUMMARY.md)
- [M5 深化学习总结](./M5-DEEPDIVE-COMPLETE.md)
- [M5 FastAPI 专题](./M5-TECH1-FastAPI.md)
- [M5 gRPC 专题](./M5-TECH2-gRPC.md)
- [M5 GraphQL 专题](./M5-TECH3-GraphQL.md)
- [M5 Python Docstring Prompt 集](./M5-python-google-docstring-prompts.md)

---

## 5. 推荐阅读顺序

### 路线 1：从零开始的标准路径
`M1 → M2 → M3 → M4 → M5 → M6`

适合第一次系统学习 Copilot CLI 的读者。

### 路线 2：直接聚焦“文档生成”
1. [M2：提示工程基础](./modules/phase1/M2-prompting-fundamentals.md)
2. [M5：文档和注释生成工作流（模块页）](./modules/phase2/M5-doc-generation.md)
3. [M5 完成总结](./M5-COMPLETION-SUMMARY.md)
4. [M5 深化学习总结](./M5-DEEPDIVE-COMPLETE.md)

### 路线 3：只看已经较成熟的资料
1. Phase 1 全部模块
2. Phase 1 支持文档
3. M5 相关专题文件

---

## 6. 每类文件分别解决什么问题

| 你现在的需求 | 优先看哪里 |
|--------------|-----------|
| 我想知道这套资料讲什么 | 本文件 `README.md` |
| 我不知道该从哪里开始 | [INDEX.md](./INDEX.md) |
| 我想按模块系统学习 | `modules/` |
| 我想看阶段总结和过程记录 | `docs/phase*/` |
| 我想找具体示例或实验材料 | `examples/` |
| 我想深入看 M5 文档生成专题 | `M5-*` 文件 |

---

## 7. 文件夹快速地图

```text
docs/copilot-cli-journey/
├── README.md                    # 学习资料总入口
├── INDEX.md                     # 学习路径导航
├── STRUCTURE.md                 # 文件夹结构说明
├── template.md                  # 模块模板
├── modules/                     # 15 个学习模块
├── docs/                        # 阶段报告、清单、总结
├── examples/                    # 示例和脚本
├── M5-COMPLETION-SUMMARY.md     # M5 完成总结
├── M5-DEEPDIVE-COMPLETE.md      # M5 深化学习总结
└── M5-TECH*.md                  # M5 专题资料
```

---

## 8. 这套资料当前最值得继续完善的地方

1. 把 Phase 2-Phase 5 的模块正文从“占位”补成“可学习内容”
2. 把已经完成的 M5 成果回填到模块页中，减少信息分散
3. 为各 Phase 增补对应的支持文档与示例
4. 统一历史文档中的旧路径表述，避免 `docs/learning/` 与当前目录混淆

---

## 9. 下一步建议

- 想继续学习：去看 [INDEX.md](./INDEX.md)
- 想快速进入实质内容：去看 [modules/phase1/](./modules/phase1/)
- 想优先看最有成果的专题：去看 [M5 完成总结](./M5-COMPLETION-SUMMARY.md)

---

*最后更新：2026-04-28*
