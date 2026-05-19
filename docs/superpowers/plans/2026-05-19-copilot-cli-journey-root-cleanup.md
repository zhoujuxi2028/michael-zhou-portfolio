# Copilot CLI Journey Root Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `docs/copilot-cli-journey/` root concise by keeping only stable entry files and moving process/status documents into Phase 2 support archives.

**Architecture:** Treat the root directory as the learning center entry point, with only `README.md`, `INDEX.md`, `STRUCTURE.md`, `template.md`, and the three main subdirectories. Move temporary process records to `docs/phase2/archive/` and update root navigation so every visible link points to the current structure.

**Tech Stack:** Markdown documentation, Git file moves, shell validation commands, existing repository docs structure.

---

## File Structure

| Path | Action | Responsibility |
|------|--------|----------------|
| `docs/copilot-cli-journey/BRANCH-STATUS.md` | Move | Historical branch status record, no longer root-level navigation |
| `docs/copilot-cli-journey/TRANSITION-PROGRESS.md` | Move | Historical M4-M5 transition progress record |
| `docs/copilot-cli-journey/docs/phase2/archive/BRANCH-STATUS.md` | Create by move | Archived Phase 2 branch status record |
| `docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md` | Create by move | Archived Phase 2 transition progress record |
| `docs/copilot-cli-journey/README.md` | Modify | Root learning-center overview and current links |
| `docs/copilot-cli-journey/STRUCTURE.md` | Modify | Current directory structure and responsibilities |
| `docs/copilot-cli-journey/docs/phase2/README.md` | Modify | Add archive entry so moved files remain discoverable |
| `docs/copilot-cli-journey/docs/phase2/archive/README.md` | Create | Explain what the archive contains |

---

### Task 1: Move root process documents into Phase 2 archive

**Files:**
- Move: `docs/copilot-cli-journey/BRANCH-STATUS.md` → `docs/copilot-cli-journey/docs/phase2/archive/BRANCH-STATUS.md`
- Move: `docs/copilot-cli-journey/TRANSITION-PROGRESS.md` → `docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md`
- Create: `docs/copilot-cli-journey/docs/phase2/archive/README.md`

- [ ] **Step 1: Create the archive directory**

```bash
mkdir -p docs/copilot-cli-journey/docs/phase2/archive
```

Expected: command exits with code 0.

- [ ] **Step 2: Move the branch status document**

```bash
git mv docs/copilot-cli-journey/BRANCH-STATUS.md docs/copilot-cli-journey/docs/phase2/archive/BRANCH-STATUS.md
```

Expected: command exits with code 0.

- [ ] **Step 3: Move the transition progress document**

```bash
git mv docs/copilot-cli-journey/TRANSITION-PROGRESS.md docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md
```

Expected: command exits with code 0.

- [ ] **Step 4: Add archive README**

Create `docs/copilot-cli-journey/docs/phase2/archive/README.md` with this content:

```markdown
# Phase 2 归档资料

本目录保存 Phase 2 学习过程中产生的阶段性记录，主要用于追溯历史决策和迁移过程。

| 文件 | 用途 |
|------|------|
| `BRANCH-STATUS.md` | M4-M5 过渡期间的 Git 分支状态记录 |
| `TRANSITION-PROGRESS.md` | M4-M5 资料迁移和阶段推进记录 |

日常学习入口请优先查看：

- [`../../README.md`](../README.md) — Phase 2 支持文档导航
- [`../../../README.md`](../../../README.md) — Copilot CLI 学习中心入口
```

- [ ] **Step 5: Validate moved files exist**

```bash
test -f docs/copilot-cli-journey/docs/phase2/archive/BRANCH-STATUS.md
test -f docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md
test -f docs/copilot-cli-journey/docs/phase2/archive/README.md
```

Expected: all commands exit with code 0.

- [ ] **Step 6: Commit Task 1**

```bash
git add docs/copilot-cli-journey/docs/phase2/archive docs/copilot-cli-journey/BRANCH-STATUS.md docs/copilot-cli-journey/TRANSITION-PROGRESS.md
git commit -m "docs: archive copilot journey process records" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: commit succeeds. Ignore unrelated untracked files outside `docs/copilot-cli-journey/`.

---

### Task 2: Update root README for concise navigation

**Files:**
- Modify: `docs/copilot-cli-journey/README.md`

- [ ] **Step 1: Replace the learning branch guide link**

In `docs/copilot-cli-journey/README.md`, replace this line:

```markdown
  - 📖 [使用指南](./LEARNING-BRANCH-GUIDE.md)
```

with:

```markdown
  - 📖 当前分支资料已归档到 [Phase 2 归档资料](./docs/phase2/archive/)
```

- [ ] **Step 2: Fix the project structure block root path**

In `docs/copilot-cli-journey/README.md`, replace the structure block header:

```markdown
docs/learning/
```

with:

```markdown
docs/copilot-cli-journey/
```

- [ ] **Step 3: Remove root-level process files from the structure block**

Ensure the root-level structure section lists only these root files before directories:

```markdown
docs/copilot-cli-journey/
├── README.md                    ← 学习中心主入口
├── INDEX.md                     ← 导航指南
├── STRUCTURE.md                 ← 当前目录结构说明
├── template.md                  ← 每个模块的标准模板
│
├── 📁 modules/                  ← 学习模块（按阶段分类）
├── 📁 docs/                     ← 支持文档、报告、清单和归档资料
└── 📁 examples/                 ← 代码示例和练习项目
```

Keep the detailed phase/module list that follows, but make sure it remains under the correct `modules/`, `docs/`, and `examples/` headings.

- [ ] **Step 4: Fix the quick start M1 link**

In `docs/copilot-cli-journey/README.md`, replace:

```markdown
1. **选择学习模块**：从 [M1 基础](./M1-copilot-cli-basics.md) 开始
```

with:

```markdown
1. **选择学习模块**：从 [M1 基础](./modules/phase1/M1-copilot-cli-basics.md) 开始
```

- [ ] **Step 5: Add a short root responsibility note**

Near the `## 🗂️ 项目结构` heading in `docs/copilot-cli-journey/README.md`, add:

```markdown
根目录只保留学习入口、导航、结构说明和模块模板；阶段报告、迁移记录和过程资料统一放在 `docs/` 子目录。
```

- [ ] **Step 6: Validate README no longer points to missing root files**

```bash
rg "LEARNING-BRANCH-GUIDE|\\./M1-copilot-cli-basics\\.md|docs/learning" docs/copilot-cli-journey/README.md
```

Expected: no matches.

- [ ] **Step 7: Commit Task 2**

```bash
git add docs/copilot-cli-journey/README.md
git commit -m "docs: simplify copilot journey root README" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: commit succeeds.

---

### Task 3: Update STRUCTURE to match the actual root layout

**Files:**
- Modify: `docs/copilot-cli-journey/STRUCTURE.md`

- [ ] **Step 1: Replace the old root path**

In `docs/copilot-cli-journey/STRUCTURE.md`, replace:

```markdown
docs/learning/
```

with:

```markdown
docs/copilot-cli-journey/
```

- [ ] **Step 2: Rewrite the root files section**

In the structure tree, ensure the root file list is:

```markdown
├── 📄 README.md                     ← 学习中心主入口
├── 📄 INDEX.md                      ← 学习路径导航
├── 📄 STRUCTURE.md                  ← 当前结构说明
├── 📄 template.md                   ← 模块标准模板
│
```

Do not include `BRANCH-STATUS.md` or `TRANSITION-PROGRESS.md` in the root file list.

- [ ] **Step 3: Add archive location under Phase 2 docs**

Under the `docs/phase2/` section in `STRUCTURE.md`, include:

```markdown
│  │  ├── archive/                   阶段性状态和迁移记录归档
│  │  │  ├── BRANCH-STATUS.md
│  │  │  └── TRANSITION-PROGRESS.md
```

- [ ] **Step 4: Update the final timestamp note**

Replace the final note with:

```markdown
*最后更新：2026-05-19* — 根目录瘦身，阶段性过程资料归档到 `docs/phase2/archive/`
```

- [ ] **Step 5: Validate STRUCTURE matches root files**

```bash
find docs/copilot-cli-journey -maxdepth 1 -mindepth 1 -print | sort
```

Expected output includes only:

```text
docs/copilot-cli-journey/INDEX.md
docs/copilot-cli-journey/README.md
docs/copilot-cli-journey/STRUCTURE.md
docs/copilot-cli-journey/docs
docs/copilot-cli-journey/examples
docs/copilot-cli-journey/modules
docs/copilot-cli-journey/template.md
```

- [ ] **Step 6: Commit Task 3**

```bash
git add docs/copilot-cli-journey/STRUCTURE.md
git commit -m "docs: update copilot journey structure guide" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: commit succeeds.

---

### Task 4: Update Phase 2 navigation and moved-file links

**Files:**
- Modify: `docs/copilot-cli-journey/docs/phase2/README.md`
- Modify: `docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md`

- [ ] **Step 1: Add archive section to Phase 2 README**

In `docs/copilot-cli-journey/docs/phase2/README.md`, add this section near the existing resource/navigation sections:

```markdown
## 🗄️ 归档资料

阶段性状态和迁移记录已集中到归档目录，避免根目录混杂过程资料：

- [Phase 2 归档资料](./archive/)
- [分支状态记录](./archive/BRANCH-STATUS.md)
- [M4-M5 转换进度](./archive/TRANSITION-PROGRESS.md)
```

- [ ] **Step 2: Fix relative links inside moved TRANSITION-PROGRESS**

In `docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md`, replace the related document links:

```markdown
- [BRANCH-STATUS.md](./BRANCH-STATUS.md) - Phase 1 documentation
- [M4-RESOURCE-INDEX.md](./M4-RESOURCE-INDEX.md) - Phase 2 navigation
- [STRUCTURE.md](./STRUCTURE.md) - Project structure status
```

with:

```markdown
- [BRANCH-STATUS.md](./BRANCH-STATUS.md) - Phase 1 documentation
- [M4-RESOURCE-INDEX.md](../M4-RESOURCE-INDEX.md) - Phase 2 navigation
- [STRUCTURE.md](../../../STRUCTURE.md) - Project structure status
```

- [ ] **Step 3: Validate archive links**

```bash
test -f docs/copilot-cli-journey/docs/phase2/archive/BRANCH-STATUS.md
test -f docs/copilot-cli-journey/docs/phase2/M4-RESOURCE-INDEX.md
test -f docs/copilot-cli-journey/STRUCTURE.md
rg "\\]\\(\\./M4-RESOURCE-INDEX\\.md\\)|\\]\\(\\./STRUCTURE\\.md\\)" docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md
```

Expected: the three `test` commands pass, and `rg` returns no matches.

- [ ] **Step 4: Commit Task 4**

```bash
git add docs/copilot-cli-journey/docs/phase2/README.md docs/copilot-cli-journey/docs/phase2/archive/TRANSITION-PROGRESS.md
git commit -m "docs: link copilot journey phase archives" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: commit succeeds.

---

### Task 5: Run final documentation validation

**Files:**
- Validate: `docs/copilot-cli-journey/`

- [ ] **Step 1: Confirm root directory is concise**

```bash
find docs/copilot-cli-journey -maxdepth 1 -mindepth 1 -print | sort
```

Expected output:

```text
docs/copilot-cli-journey/INDEX.md
docs/copilot-cli-journey/README.md
docs/copilot-cli-journey/STRUCTURE.md
docs/copilot-cli-journey/docs
docs/copilot-cli-journey/examples
docs/copilot-cli-journey/modules
docs/copilot-cli-journey/template.md
```

- [ ] **Step 2: Check root entry docs for old paths and missing moved-file references**

```bash
rg "docs/learning|LEARNING-BRANCH-GUIDE|\\./M1-copilot-cli-basics\\.md|\\./BRANCH-STATUS\\.md|\\./TRANSITION-PROGRESS\\.md" docs/copilot-cli-journey/README.md docs/copilot-cli-journey/STRUCTURE.md docs/copilot-cli-journey/INDEX.md
```

Expected: no matches.

- [ ] **Step 3: Check moved archive documents are discoverable**

```bash
rg "archive/BRANCH-STATUS|archive/TRANSITION-PROGRESS|Phase 2 归档资料" docs/copilot-cli-journey/README.md docs/copilot-cli-journey/STRUCTURE.md docs/copilot-cli-journey/docs/phase2/README.md docs/copilot-cli-journey/docs/phase2/archive/README.md
```

Expected: matches appear in `STRUCTURE.md`, `docs/phase2/README.md`, and `docs/phase2/archive/README.md`.

- [ ] **Step 4: Review git status only for intended files**

```bash
git --no-pager status --short
```

Expected: no modified files from this cleanup remain. Pre-existing unrelated untracked files may still appear; leave them untouched.

- [ ] **Step 5: Final commit if validation required additional fixes**

Only run this if Step 1-3 exposed additional link or wording fixes:

```bash
git add docs/copilot-cli-journey
git commit -m "docs: finalize copilot journey root cleanup" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: commit succeeds if there were additional fixes; otherwise skip this step.
