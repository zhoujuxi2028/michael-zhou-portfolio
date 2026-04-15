# 文档编写规范

本文档定义了项目的文档管理原则、检查清单和最佳实践。所有新文档必须遵循这些规范。

---

## 🎯 核心原则

### 原则 1: 单一信息来源 (SSOT)

每条信息在整个项目中应该只有 **一个权威来源**。所有其他地方通过链接引用，而不是复制。

**结构:**
```
权威来源（维护单点）
    ↓
    ├─ 链接源 1（参考，不重复）
    └─ 链接源 2（参考，不重复）
```

**示例:**
- ✅ M4 学习资源只在 `docs/copilot-cli-journey/docs/phase2/` 中维护
- ✅ 其他地方通过链接指向这个位置
- ❌ 不在 `docs/learning/` 中重复维护同样的资源

### 原则 2: 避免重复维护

同一信息绝不应该在两处以上维护，会导致：
- 信息不同步
- 新用户混淆
- 维护负担加重

**正确做法:**
```markdown
详见 [M4 完成总结](../../copilot-cli-journey/docs/phase2/M4-COMPLETION-SUMMARY.md)
```

---

## 📋 创建新文档的检查清单

在创建任何新文档前，必须完成以下检查：

### Q1: 现有系统中是否已有类似文件？
- [ ] 搜索 `grep -r "keyword" docs/`
- [ ] 使用 GitHub 搜索功能
- [ ] 检查 INDEX.md 或相关 README.md
- **如果找到类似文件** → 转到 Q3

### Q2: 新文档的唯一职责是什么？
- [ ] 能否用一句话说明这个文档的职责？
- [ ] 与其他文档有没有职责重叠？
- [ ] 是否符合项目的目录结构规范？

### Q3: 我是复制还是链接？
- [ ] 如果信息已在其他地方维护 → **创建链接**
- [ ] 如果是完全新的信息 → **创建新文件**
- [ ] 如果两者都不是 → **先与 reviewer 讨论职责分工**

### Q4: 有没有更新导航？
- [ ] 新文档是否添加到相关目录的 `README.md`？
- [ ] 相关的权威来源是否需要链接更新？
- [ ] 是否需要更新 INDEX.md？

---

## 📝 文件命名约定

遵循以下约定使文件名清晰且易于查找：

| 文件类型 | 命名格式 | 示例 |
|---------|---------|------|
| 计划文件 | `YYYY-MM-DD-<feature-name>.md` | `2026-04-15-m4-migration.md` |
| 完成报告 | `<feature>-COMPLETION-SUMMARY.md` | `M4-COMPLETION-SUMMARY.md` |
| 迁移/事项报告 | `<feature>-<action>-COMPLETE.md` | `M4-MIGRATION-COMPLETE.md` |
| 学习记录 | `<TOPIC>-LEARNING.md` | `M4-TEST-GENERATION.md` |
| 指南文档 | `<topic>-guide.md` | `testing-guide.md` |
| 检查清单 | `<task>-checklist.md` | `documentation-checklist.md` |
| 事后分析 | `postmortem-<YYYY-QN>.md` | `postmortem-2026-Q1.md` |

---

## 🔍 PR 审查检查清单

当审查包含文档的 PR 时，reviewer 必须检查：

- [ ] **是否添加了新的 `.md` 文档？**
  - 文档名称是否符合命名约定？

- [ ] **是否遵循了 SSOT 原则？**
  - 是否在两处维护同一信息？
  - 是否可以用链接替代复制？

- [ ] **是否有重复维护的风险？**
  - 新文档的职责是否与现有文档明确分离？
  - 是否存在信息同步问题的风险？

- [ ] **是否更新了相关导航？**
  - README.md 是否包含指向新文档的链接？
  - INDEX.md 或其他索引是否需要更新？
  - 相关的权威来源是否需要链接？

- [ ] **是否有职责冲突？**
  - 发现冲突必须在 merge 前解决

---

## 📂 项目结构规范

当前项目的标准结构：

```
docs/
├── README.md                    中心导航
├── BEST-PRACTICES/              最佳实践和规范
│   ├── documentation-standards.md   ← 本文件
│   ├── implementation-checklist.md
│   └── templates/
│
└── copilot-cli-journey/         学习中心（唯一的学习资源来源）
    ├── README.md                入口
    ├── modules/                 学习理论
    ├── docs/                    支持文档（按 phase）
    └── examples/                代码示例（按 phase）
```

**重要:** 所有学习资源都应该在 `copilot-cli-journey/` 下，按 phase 组织。

---

## 💡 常见问题

### Q: 什么时候应该创建新文件？
A: 当有新的、独立的主题需要详细说明时。检查是否已有相关文档，如果没有，按照检查清单创建。

### Q: 什么时候应该使用链接而不是复制？
A: 当信息已在其他地方维护时，始终使用链接。这确保信息的唯一性和同步性。

### Q: 如果我不确定职责分工怎么办？
A: 在创建文档前先向 reviewer 咨询。避免创建重复或职责重叠的文档。

### Q: 文档过时了怎么办？
A: 
1. 更新原始文档
2. 确保所有链接仍然有效
3. 删除过时的副本（如果存在）
4. 在 PR 中说明变更

---

## 📚 参考资源

- [项目文档入口](../README.md)
- [Copilot CLI 学习中心](../copilot-cli-journey/)
- [实现清单](./implementation-checklist.md)

---

**最后更新**: 2026-04-15  
**版本**: 1.0  
**状态**: 有效
