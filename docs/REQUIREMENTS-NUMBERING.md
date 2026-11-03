# 需求编号规范 (Requirement Numbering Convention)

> 本仓库（michael-zhou-portfolio）的统一需求编号规范。所有新增需求必须遵循本规范。
>
> 状态：✅ 现行 | 最后更新：2026-11-03

---

## 1. 目的

仓库是多项目 portfolio，历史上存在多套互不统一的编号（`REQ-001`、`REQ-MX-NN`、`REQ-EDI-01`）。本规范：

1. 统一编号格式，消除歧义与冲突
2. 通过项目前缀实现「编号即定位」——看编号就知道属于哪个项目
3. 建立全局索引（[REQUIREMENTS-INDEX.md](./REQUIREMENTS-INDEX.md)）集中登记

---

## 2. 编号格式

```
REQ-{PROJECT}-{SEQ}
```

| 部分 | 说明 | 示例 |
|------|------|------|
| `REQ` | 固定前缀，表示 Requirement | `REQ` |
| `PROJECT` | 项目前缀（见下表），2-4 位大写字母 | `EDI` |
| `SEQ` | 项目内序号，至少 2 位数字，从 `01` 递增 | `01` |

完整示例：`REQ-EDI-01` = edi-reporting-demo 项目的第 1 条需求。

---

## 3. 项目前缀表

| 前缀 | 项目路径 | 说明 |
|------|---------|------|
| `CMP` | `cloud-monitoring-platform/` | IoT 云监控平台 |
| `EDI` | `edi-reporting-demo/` | EDI 报文解析 + Jasper 报表 |
| `CC` | `docs/copilot-cli-journey/` | Copilot CLI 学习路径 |
| `CA` | `docs/claude-api-journey/` | Claude API 学习路径 |
| `AND` | `android-app/`（规划中） | Android 应用 |
| `DRV` | `linux-driver/`（规划中） | Linux 内核驱动 |

> 新增项目时：在本表登记前缀，并在 [REQUIREMENTS-INDEX.md](./REQUIREMENTS-INDEX.md) 增加对应小节。

---

## 4. 规则

1. **编号唯一**：`PROJECT + SEQ` 组合在仓库内唯一，不得复用。
2. **前缀定项目**：新需求一律使用所属项目前缀，不得使用无前缀的 `REQ-001` 这种裸编号。
3. **序号递增**：项目内按时间顺序 `01`、`02`… 递增，不跳跃、不预留空号。
4. **模块子编号（可选）**：学习类项目可用 `REQ-{PROJECT}-M{模块}-{SEQ}`，如 `REQ-CC-M8-01`（M8 模块第 1 条）。
5. **一处登记**：每条需求创建后同步登记到全局索引。

---

## 5. 历史编号映射（兼容）

历史上已存在的编号**不强制改写**，但统一视为「旧格式」，在新文档中一律使用新编号，并通过下表对照：

| 旧编号 | 所属项目 | 新编号（规范） |
|--------|---------|---------------|
| `REQ-001` | cloud-monitoring-platform | `REQ-CMP-001` |
| `REQ-MX-NN`（M0-M15） | copilot-cli-journey | `REQ-CC-MX-NN` |
| `REQ-EDI-01` | edi-reporting-demo | 已是规范格式，无需映射 |

> 完整对照见 [REQUIREMENTS-INDEX.md](./REQUIREMENTS-INDEX.md)。

---

## 6. 全局索引

所有需求编号集中登记在：

- [**REQUIREMENTS-INDEX.md**](./REQUIREMENTS-INDEX.md) — 按项目分组的全局需求索引

每个子项目可维护自己的 `REQUIREMENTS.md`（如 `docs/copilot-cli-journey/REQUIREMENTS.md`），但必须同时回填到全局索引。
