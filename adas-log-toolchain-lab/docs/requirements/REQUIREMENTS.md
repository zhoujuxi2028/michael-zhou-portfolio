# adas-log-toolchain-lab — Requirements

> 需求编号：`REQ-ADAS-01` ｜ Issue：[#69](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/69) ｜ 设计：[design spec](../../../docs/superpowers/specs/2026-11-20-adas-log-toolchain-design.md)
>
> 细项编号 `FR-ADAS-*` / `NFR-ADAS-*`，均归属 `REQ-ADAS-01`。
>
> **声明**：simulation lab，全部使用合成数据，不代表实车、测试车或量产项目经验。

## 1. 背景与目标

补齐开发者作品集中「数据 / 工具链开发」方向的证据：用 TypeScript 编写数据脚本（日志解析、异常提取、测试用例生成、传感器数据处理、数据采集），并配套数据分析与看板。现有作品集中 TypeScript 主要用于 UI 自动化，缺少数据处理类工具的实践。

## 2. REQ 级需求

| REQ ID | 标题 | 类型 | 优先级 | 可行性 | 依赖 | 状态 |
|--------|------|------|--------|--------|------|------|
| `REQ-ADAS-01` | ADAS 日志工具链：TS 解析 + 用例生成 + 数据采集 + 场景回放 + 分析看板 | DEMO | P1 | ✅ 可行（2026-11-20） | — | 🔄 Phase 2–4 集成中（0.2.0） |

## 3. 功能需求

| ID | 描述 | 模块 | 优先级 | Phase |
|----|------|------|--------|-------|
| FR-ADAS-001 | 用固定 seed 生成可复现的合成 ACC / APA / HMI 日志（JSON Lines）及真值标签 | `generator` | P0 | 1 |
| FR-ADAS-002 | 流式解析日志，按规则提取异常（含 HMI 日志异常），输出 JSON 异常报告 | `parser` | P0 | 1 |
| FR-ADAS-003 | ACC / APA / HMI 各 2 条异常规则 | `rules` | P0 | 1 |
| FR-ADAS-004 | 按规则阈值自动生成边界值测试用例 | `casegen boundary` | P0 | 1 |
| FR-ADAS-005 | 模拟车载数据记录器：环形缓冲、触发录制、片段切分、片段索引、采集质量检查 | `collector` | P1 | 2 |
| FR-ADAS-006 | 从异常报告生成回放清单和可运行的回归测试文件 | `casegen regression` | P1 | 2 |
| FR-ADAS-007 | 按回放清单批量重放场景（含泊车场景）并汇总结果 | `replay` | P1 | 2 |
| FR-ADAS-008 | LKA 事件与 2 条规则（转向灯开启仍纠偏；车道线丢失未提示） | `rules` | P1 | 2 |
| FR-ADAS-009 | Pandas 统计分析，导出 JSON | `analysis` | P2 | 3 |
| FR-ADAS-010 | TypeScript + ECharts 静态看板，发布 GitHub Pages | `dashboard` | P2 | 3 |
| FR-ADAS-011 | 合成的 Jira 导出工单 JSON → 脚本任务 + 检查脚本 | `ticket` | P3 | 4 |
| FR-ADAS-012 | 使用手册与开发规范 | docs | P3 | 4 |

## 4. 非功能需求

| ID | 类别 | 要求 | Phase |
|----|------|------|-------|
| NFR-ADAS-001 | 语言 | 代码注释、CLI 输出、异常描述、报告、CHANGELOG 用英文；README 中英双语 | 1 |
| NFR-ADAS-002 | 版本管理 | 工具 semver + 规则集版本，写入所有输出；CHANGELOG | 1 |
| NFR-ADAS-003 | CI | GitHub Actions `adas` job；`.gitlab-ci.yml` 示例（未在 GitLab 运行） | 1 |
| NFR-ADAS-004 | 质量 | TypeScript strict；行覆盖率 ≥ 90%，规则 100%；真值比对精确率 / 召回率 100% | 1 |
| NFR-ADAS-005 | 性能 | 10 万事件 < 5 s，内存不随文件大小线性增长 | 1 |
| NFR-ADAS-006 | 健壮性 | 坏行、未知事件、乱序不中断运行，写入数据质量报告 | 1 |

## 5. 范围

| Phase | 内容 |
|-------|------|
| 1 | `generator` + `parser` + `casegen boundary` + 英文 / 版本管理 / GitLab CI 示例 |
| 2 | `collector` + `casegen regression` + `replay` + LKA |
| 3 | `analysis` + `dashboard` |
| 4 | `ticket`（Jira 工单 → 脚本）+ 使用手册 + 开发规范 |

**不做：** 真实车辆总线采集与云端上传、原始传感器数据（点云 / 图像）、连接真实 Jira 服务、AUTOSAR Adaptive、底层 C/C++。

**后续可能：** PySpark 大规模日志处理示例（Phase 3 完成后评估）。

## 6. 验收标准

### Phase 1

- [x] `generator` 用固定 seed 生成可复现的合成日志
- [x] `parser` 为 TypeScript（strict）CLI，流式读取，输出 JSON 异常报告
- [x] ACC / APA / HMI 各 2 条异常规则
- [x] `casegen boundary`：每个判定条件生成 阈值 −ε / 阈值 / 阈值 +ε 用例，并全部通过
- [x] 单元测试覆盖规则与解析，CI 通过
- [x] README.md（英文）+ README.zh.md（中文），写明运行方式与"合成数据"声明
- [x] 代码注释、CLI 输出、异常描述为英文；`CHANGELOG.md` 记录 0.1.0
- [x] `anomalies.json` 的 `meta` 包含工具版本与规则集版本
- [x] `.gitlab-ci.yml` 示例通过 YAML 校验

### Phase 2

- [x] `collector`：模拟实时数据流，环形缓冲 + 触发（规则命中 / 手动）录制前后 N 秒片段
- [x] 片段元数据（触发原因、时间范围、事件数、SHA-256）与片段索引
- [x] 采集质量检查：丢帧率、重复事件、时间戳间隔异常
- [x] `casegen regression`：生成回放清单和可直接运行的 Vitest 回归测试
- [x] `replay` 按回放清单批量执行并汇总结果
- [x] LKA-01 / LKA-02 规则及其边界值用例
- [ ] 根目录 README「招聘方速查」中"数据 / 工具链开发"一行更新为本项目（待做：合并后的收尾步骤）

### Phase 3

- [x] `analysis`：Python 3.13 + Pandas 统计，导出 `summary.json`，pytest 通过
- [ ] `dashboard`：静态页面构建成功并发布 GitHub Pages（构建成功；未发布：需仓库所有者先在 Settings -> Pages 开启（来源 GitHub Actions），再设置仓库变量 `ADAS_PAGES_ENABLED=true`，部署 job 才会运行）

### Phase 4

- [x] `ticket`：解析合成的 Jira 导出 JSON（component → 模块，label → 规则），生成脚本任务与检查脚本
- [x] 使用手册与开发规范（`docs/user-guide*.md`、`docs/dev-guide*.md`，中英双语）

## 7. 需求确认记录（2026-11-20）

| 问题 | 决定 |
|------|------|
| 节奏 | 按完整范围分期推进，不压缩工期 |
| 日志格式 | JSON Lines |
| 看板 | Pandas → JSON → TypeScript + ECharts 静态页面（GitHub Pages） |
| 补充 1 | 新增 TS 生成测试用例（`casegen`）与数据采集工具链（`collector`） |
| 补充 2 | 新增英文交付、版本管理、GitLab CI 示例（Phase 1）；LKA（Phase 2）；Jira 工单 → 脚本（Phase 4） |

## 8. 可行性评估（2026-11-20）

**结论：可行。** 全栈已在本机临时目录实测跑通（测后删除），无阻塞项。

| 检查项 | 结果 | 结论 |
|--------|------|------|
| Node / npm | v26.13.1 / 11.20.0 | ✅ |
| TypeScript strict + zod 4 + Vitest 5 | `tsc --strict` 通过，Vitest 1 passed | ✅ |
| commander | 15.1.0 可安装 | ✅ |
| Vite 8 + ECharts 6 静态构建 | 柱状图页面构建成功 | ✅ |
| Python + Pandas | python3.13 venv + Pandas 3.1.1 正常（系统 3.15 未装 Pandas） | ✅ 用 3.13 |
| CI | 沿用 `ci.yml` 每项目一个 job 的模式 | ✅ |
| GitHub Pages | 仓库公开，可开启，当前未开 | 待所有者开启（Phase 3 计划；见 FR-ADAS-010 备注） |
| 磁盘 | 剩余约 16 GB；Vite + ECharts 依赖约 140 MB | ⚠️ 够用 |

| # | 风险 | 应对 |
|---|------|------|
| R1 | 分支落后 main | 已从最新 main 重建（2026-11-20） |
| R2 | TypeScript 7 为大版本，周边工具可能滞后 | 实测可用；ESLint 不兼容则固定 TS 5.x |
| R3 | 系统 Python 3.15 过新 | `analysis/` 与 CI 固定 3.13 |
| R4 | 磁盘空间紧张 | 依赖只装在项目内，venv 放 `/tmp` |

## 9. 评审记录

| 日期 | 阶段 | 结论 |
|------|------|------|
| 2026-11-20 | Stage 1 需求（含可行性） | ✅ 通过 |
| 2026-11-20 | Stage 2 设计文档 | ✅ 子代理评审 2 轮通过 + 用户评审通过 |
| 2026-11-20 | Stage 2 实施计划 | ✅ 子代理评审 2 轮通过 + 用户评审通过；执行方式：子代理逐任务 |
| 2026-11-20 | Stage 3 开发（Phase 1 T1–T14） | ✅ 3 轮代码评审修复完成 + TDD 证据补充（`docs/tdd.md`）+ 用户评审通过 |
| 2026-11-20 | Stage 4 测试（Phase 1） | ✅ PR #75 CI 4 个 job 全绿 + 用户评审通过；合并后打 tag `adas-lab-v0.1.0` |
| 2026-11-20 | Phase 2–4 开发（并行子代理 + TDD）| 集成中 |
