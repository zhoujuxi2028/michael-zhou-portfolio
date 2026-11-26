# REQ-DVP-008 — Loki vs Elastic 对比 Design

**Date:** 2026-11-26
**Author:** michael_zhou
**Status:** ✅ Approved（2026-11-26）
**Requirement:** [`REQ-DVP-008`](../../../devops-platform-demo/docs/requirements/REQUIREMENTS.md#req-dvp-008--elastic-集成--loki-vs-elastic-对比)

---

## 1. 背景与目标

项目日志栈为 Loki + Promtail（REQ-002）。企业环境常见 Elastic（ELK / Elastic Stack），需要说明两者取舍，并可选地演示 Elastic 检索同一批日志。

**目标：**

- 一份基于本项目实测数据的 Loki vs Elastic 对比文档
- （可选）按需启动单节点 Elastic，Kibana 检索与 Loki 相同来源的日志

**非目标：**

- Elastic 常驻 k3d、与 monitoring 全栈并行运行
- ECK operator、多节点集群、ILM 调优
- 替换 Loki

## 2. 可行性依据（2026-11-26）

| 项 | 结果 |
|----|------|
| 现有 Loki | SingleBinary，requests 128Mi；Promtail 64Mi（`monitoring/loki/*-values.yaml`） |
| Prometheus | limits 1Gi |
| OrbStack | 上限 8 GiB；宿主 16 GB，空闲约 5 GB |
| Elastic 资源（已知需求） | Elasticsearch 单节点 heap ≥ 512m，容器约 1–1.5 GiB；Kibana 约 0.7–1 GiB；Filebeat ~100 MiB |
| 镜像 | Elasticsearch + Kibana 约 1.7 GB；本机 Docker Hub 拉取慢（postgres 镜像 > 4 分钟未完成） |
| 未实测 | Elastic 实际内存 / 启动时间（Stage 1 再测） |

**结论：** 文档 100%；部署 50% → 部署降为可选，且不与其他栈并行。

## 3. 决策记录

| # | 决策 | 备选 | 理由 |
|---|------|------|------|
| D1 | 交付以对比文档为主 | 部署为主 | 内存与网络限制；对比价值在于取舍分析 |
| D2 | 可选演示用 `docker-compose.elastic.yml`（ES + Kibana + Filebeat），不进 k3d | ECK on k3d | ECK operator + CRD 额外开销；compose 启停简单、与集群隔离 |
| D3 | Filebeat 采集 k3d 节点容器日志（`/var/log/containers` 经 docker 挂载） | 应用直写 ES | 与 Promtail 同源，对比公平 |
| D4 | ES 单节点 `discovery.type=single-node`、heap 512m、关闭安全仅限本地 | 开启 TLS / 认证 | 本地 demo；文档注明生产必须开启 |
| D5 | 对比数据用同一时间窗、同一查询（如 newman Job 错误日志）分别测 LogQL / KQL | 只引用官方资料 | 体现实测，而非复述文档 |

## 4. 对比文档结构

`devops-platform-demo/docs/guides/LOGGING-LOKI-VS-ELASTIC.md`

| 章节 | 内容 |
|------|------|
| 架构 | Loki（标签索引 + 对象存储 chunk）vs ES（倒排索引 + shard） |
| 索引模型 | 只索引标签 vs 全文 / 字段索引；高基数标签的影响 |
| 资源 | 本项目实测内存 / 磁盘（可选部署时补实测值） |
| 查询 | 同一需求的 LogQL 与 KQL 写法对照表 |
| 运维 | 保留策略（Loki retention vs ILM）、扩容、备份 |
| 成本 | 存储放大、计算资源、许可证（Elastic License / AGPL） |
| 选型建议 | 场景 → 推荐（K8s 运维日志 / 安全分析 / 业务全文检索） |

## 5. 可选演示

| 步骤 | 说明 |
|------|------|
| 前置 | 停掉 monitoring 全栈 / 其他 demo，确保 OrbStack 空闲 ≥ 3 GiB |
| 启动 | `docker compose -f docker-compose.elastic.yml up -d` |
| 采集 | 运行 newman / cypress Job 产生日志 |
| 验证 | Kibana Discover 检索与 Grafana Explore（Loki）相同的日志行 |
| 清理 | `docker compose -f docker-compose.elastic.yml down -v` |

## 6. 测试 / 验收

| 项 | 验证 |
|----|------|
| 文档 | 所有对比项有来源（实测 / 链接）；LogQL 查询在本项目 Loki 可执行 |
| compose（可选） | `docker compose config` 通过；ES `_cluster/health` 为 green / yellow；Kibana 检索到目标日志 |
| CI | compose 文件仅做语法校验，不在 CI 运行 Elastic |

## 7. 风险

| 风险 | 缓解 |
|------|------|
| 内存不足导致 OrbStack 停机（REQ-006 期间发生过） | 前置检查脚本：空闲内存不足即退出 |
| 镜像拉取慢 | 提前拉取；或仅交付文档 |
| 许可证表述不准确 | 引用 Elastic 官方许可证页面，注明日期 |
