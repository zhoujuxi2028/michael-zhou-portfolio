# 全局需求索引 (Global Requirements Index)

> 本仓库所有需求的**单一登记入口**，按项目分组。编号规范见 [REQUIREMENTS-NUMBERING.md](./REQUIREMENTS-NUMBERING.md)。
>
> 状态：🟡 进行中 | 最后更新：2026-11-03

---

## 统计

| 项目前缀 | 项目 | 需求数 |
|---------|------|-------|
| `CMP` | cloud-monitoring-platform | 1 |
| `EDI` | edi-reporting-demo | 1 |
| `CC` | copilot-cli-journey | 13 |
| `CA` | claude-api-journey | 0 |
| `AND` / `DRV` | （规划中） | 0 |
| **合计** | | **15** |

---

## CMP — cloud-monitoring-platform

| 新编号 | 旧编号 | 描述 | 状态 |
|--------|--------|------|------|
| `REQ-CMP-001` | `REQ-001` | 电信平台设备信息查询接口 | ✅ |

> 详见 `cloud-monitoring-platform/prototype/docs/requirements/requirements.md`

---

## EDI — edi-reporting-demo

| 新编号 | 旧编号 | 描述 | 状态 |
|--------|--------|------|------|
| `REQ-EDI-01` | — | EDI 解析器重构为 parser + schema + validator 分层架构 + pytest 契约测试 | ✅ |

---

## CC — copilot-cli-journey

> 详细清单见 `docs/copilot-cli-journey/REQUIREMENTS.md`（与 GitHub Issues 双向链接）

| 新编号 | 旧编号 | 描述 | 状态 |
|--------|--------|------|------|
| `REQ-CC-M5-01` | `REQ-M5-01` | M5 文档生成 — 学习成果 section | ✅ |
| `REQ-CC-M8-01` | `REQ-M8-01` | M8 工作流集成 — 实战验证 | ✅ |
| `REQ-CC-M9-01` | `REQ-M9-01` | M9 调试与脚本 — 实战验证 | ✅ |
| `REQ-CC-M0-02` | `REQ-M0-02` | Phase 3 支持文档 | ✅ |
| `REQ-CC-M10-01` | `REQ-M10-01` | M10 API 测试集成 | ✅ |
| `REQ-CC-M11-01` | `REQ-M11-01` | M11 E2E 测试集成 | ✅ |
| `REQ-CC-M12-01` | `REQ-M12-01` | M12 性能测试集成 | ✅ |
| `REQ-CC-M0-03` | `REQ-M0-03` | Phase 4 支持文档 | ✅ |
| `REQ-CC-M13-01` | `REQ-M13-01` | M13 Copilot Workspace | ✅ |
| `REQ-CC-M14-01` | `REQ-M14-01` | M14 团队标准 | ✅ |
| `REQ-CC-M15-01` | `REQ-M15-01` | M15 知识库总结 | ✅ |
| `REQ-CC-M0-04` | `REQ-M0-04` | Phase 5 支持文档 | ✅ |
| `REQ-CC-M0-01` | `REQ-M0-01` | 统一旧路径引用 | ✅ |

---

## CA — claude-api-journey

暂无正式编号需求。

---

## 新增需求登记流程

1. 确定所属项目前缀（见编号规范 §3）
2. 分配下一个可用序号
3. 在对应项目小节追加一行
4. 更新「统计」表合计
