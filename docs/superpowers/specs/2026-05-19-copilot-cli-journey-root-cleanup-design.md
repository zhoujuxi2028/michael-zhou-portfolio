# Copilot CLI Journey 根目录整理设计

## 背景

`docs/copilot-cli-journey/` 是 Copilot CLI 深度学习路径的入口目录。当前根目录同时放置入口文档、导航文档、模板、结构说明、分支状态和迁移进度记录，导致读者难以判断哪些文件是日常入口，哪些只是阶段性过程资料。

## 目标

本次整理采用“适度整理”方案：让根目录只保留稳定入口与通用模板，把阶段性状态和迁移记录移动到阶段支持资料目录，并修复因历史迁移产生的旧路径和失效链接。

整理后的根目录职责如下：

| 路径 | 职责 |
|------|------|
| `README.md` | 学习中心主入口、路线图总览 |
| `INDEX.md` | 按目标、时间、角色选择学习路径 |
| `STRUCTURE.md` | 当前目录结构和文件职责说明 |
| `template.md` | 学习模块写作模板 |
| `modules/` | M1-M15 正式学习模块 |
| `docs/` | 阶段支持文档、报告、清单、归档记录 |
| `examples/` | 可运行示例和练习项目 |

## 整理范围

1. 将根目录中的阶段性过程文件迁移到 Phase 2 支持文档归档区：
   - `BRANCH-STATUS.md`
   - `TRANSITION-PROGRESS.md`
2. 在 `docs/phase2/` 下建立清晰的状态/归档位置，例如 `docs/phase2/archive/`。
3. 更新根目录文档中的旧路径描述：
   - 将 `docs/learning/` 修正为 `docs/copilot-cli-journey/`。
   - 修复 README 中直接指向根目录模块的失效链接。
   - 修复 `LEARNING-BRANCH-GUIDE.md` 等不存在或位置不明的链接，若找不到目标则移除或改为现有替代入口。
4. 更新迁移记录内部链接，确保移动后仍能指向正确文件。
5. 保持正式模块、示例代码和阶段支持文档内容不做语义重写，只处理目录归位、导航和链接准确性。

## 非目标

- 不重命名 `modules/`、`docs/`、`examples/` 三个主目录。
- 不深度重构所有阶段资料。
- 不修改示例项目依赖或测试逻辑。
- 不整理 `docs/copilot-cli-journey/` 以外的资料。

## 成功标准

- 根目录只保留稳定入口文件、主目录和模板。
- 根目录 README 能准确说明当前结构，并链接到可访问文件。
- `STRUCTURE.md` 与实际目录一致。
- 阶段性状态和迁移记录能在 `docs/phase2/archive/` 中找到。
- 关键 Markdown 链接不再指向明显不存在的根目录文件。

## 验证方式

- 使用 `find docs/copilot-cli-journey -maxdepth 1` 检查根目录是否瘦身。
- 使用文本搜索检查旧路径 `docs/learning/` 和移动文件链接是否仍出现在根入口文档中。
- 使用脚本或命令检查 Markdown 相对链接是否存在明显断链。
