# Loki vs Elastic 日志栈对比（REQ-DVP-008）

本项目日志栈为 Loki + Grafana Alloy（REQ-DVP-002 引入，REQ-DVP-010 后 Alloy 替代 EOL 的 Promtail）；Elastic 未在本项目部署（FR-DVP-008-02 未执行，原因：内存与网络限制），Elastic 列引用官方来源。

| 标记 | 含义 |
|------|------|
| 实测 | 本项目 k3d `portfolio-dev` 测得（2026-11-28） |
| 配置 | 仓库文件中的值（给出路径） |
| 官方 | 官方文档链接（访问 2026-11-28） |
| 未实测（可选部署未执行） | Elastic 未部署，无实测值 |

## §0 速览

| 维度 | Loki + Alloy | Elastic（ES + Kibana + Filebeat） |
|------|--------------|-----------------------------------|
| 索引 | 只索引标签，日志行压缩存 chunk（官方） | 倒排索引（全文）+ 字段索引 / doc values（官方） |
| 查询语言 | LogQL（官方） | KQL（Kibana）/ ES\|QL / Query DSL（官方） |
| 本项目 requests / limits | Loki 128Mi/256Mi + Alloy 64Mi/128Mi × 3 = 320Mi/640Mi（配置） | 未实测（可选部署未执行） |
| 实测内存 | Loki 116Mi（实测；Alloy 未计入，见附录 A） | 未实测（可选部署未执行） |
| 许可证 | AGPLv3（官方） | 源码 SSPL 1.0 / AGPLv3 / ELv2；默认发行版 ELv2（官方） |
| 本项目结论 | 继续使用（K8s / CI 日志） | 安全分析 / 全文检索 / 企业已有 Elastic 时选用 |

## §1 架构

Loki（本项目实际链路）：

```
/var/log/pods (k3d 节点)
  → Alloy DaemonSet (Kubernetes API 读取 pod 日志)
  → Loki SingleBinary (tsdb 索引 + filesystem chunk, PVC 5Gi)
  → Grafana Explore
```

Elastic（官方典型链路，未部署）：

```
/var/log/pods
  → Filebeat / Elastic Agent DaemonSet
  → Elasticsearch (data stream, shard)
  → Kibana Discover
```

| 组件 | Loki 栈 | Elastic 栈 |
|------|---------|-----------|
| 采集 | Grafana Alloy（官方；Promtail 已于 2026-03-02 EOL） | Filebeat / Elastic Agent（官方） |
| 存储 | 标签索引（tsdb）+ 对象存储 / 文件系统 chunk | Lucene 段，按 shard 分布，`_source` + 倒排 + doc values |
| 部署形态 | SingleBinary → SimpleScalable（read/write/backend）→ 微服务 | 单节点 → 多节点（master / data / ingest 角色） |
| 扩展单元 | 无状态读写组件 + 共享对象存储 | shard + replica，节点加入后 rebalance |
| UI | Grafana（与 Prometheus 指标同一界面） | Kibana |
| 本项目部署 | Helm（`monitoring/loki/`，k3d 内） | 未部署 |

## §2 索引模型

| 项 | Loki | Elastic |
|----|------|---------|
| 被索引的内容 | 仅标签（实测：`container`、`namespace`、`node`、`pod`、`service_name`） | 每个字段 + `message` 全文（分词） |
| 查询路径 | 标签选 stream → 拉 chunk → 行过滤（grep 式） | 倒排索引直接定位文档 |
| 本项目 stream 数 | 1 个 stream（实测） | 未实测（可选部署未执行） |
| 高基数 | 标签值多 → stream 数爆炸、chunk 碎片化；官方建议标签保持低基数 | 字段多 → mapping 膨胀（`index.mapping.total_fields.limit` 默认 1000，官方）；高基数聚合耗堆内存 |
| 高基数字段方案 | structured metadata（Loki 3.x，官方） | — |
| schema 变更 | `schema_config` 按日期切换，旧数据不迁移（配置：`from: 2024-01-01`, v13） | mapping 已有字段不可改，需 reindex（官方） |
| 匹配语义 | 行过滤为字节 / 正则匹配，区分大小写 | `text` 字段按分词匹配，默认不区分大小写 → 同一“关键词”命中数可能不同（见 §4 Q2） |
| 标签陷阱 | `job_name` 不是 Loki 标签 → 旧仪表盘查询为空（实测，见 #96） | — |

## §3 资源

| 组件 | requests | limits | 实测内存 | 启动到就绪 | 镜像大小 | 来源 |
|------|----------|--------|----------|-----------|----------|------|
| Loki | 100m / 128Mi | 200m / 256Mi | 116Mi | 58s | 80.4MB | 配置 + 实测 |
| Alloy ×3 | 50m / 64Mi 每个 | 100m / 128Mi 每个 | 未实测（镜像拉取受限） | — | — | 配置 |
| Elasticsearch | 未实测（可选部署未执行） | — | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 官方 |
| Kibana | 未实测（可选部署未执行） | — | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 官方 |

| 同一批日志（实测窗口） | Loki | Elastic |
|------------------------|------|---------|
| 日志行数 | 265（其中 newman 265） | 未实测（可选部署未执行） |
| 磁盘 | 52 KiB | 未实测（可选部署未执行） |
| 字节 / 行（含索引与 WAL） | ≈ 201 B | 未实测（可选部署未执行） |

## §4 查询

LogQL 计数写法：`sum(count_over_time(<query>[24h]))`；Loki 返回 `execTime` 为服务端执行时间。

| # | 需求 | LogQL | Loki 命中 / execTime | KQL（或 ES\|QL） | Elastic 命中 | 差异说明 |
|---|------|-------|----------------------|------------------|-------------|----------|
| Q1 | newman 全部日志 | `{namespace="qa-portfolio", container="newman"}` | 265 / 0.446s | `kubernetes.namespace: "qa-portfolio" and kubernetes.container.name: "newman"` | 未实测（可选部署未执行） | 字段名按 Filebeat `add_kubernetes_metadata` 官方字段 |
| Q2 | newman 错误行 | `{namespace="qa-portfolio", container="newman"} \|~ "AssertionError\|FAILED\|Error:"` | 14 / 0.409s | `kubernetes.namespace: "qa-portfolio" and kubernetes.container.name: "newman" and message: ("AssertionError" or "FAILED" or "Error:")` | 未实测（可选部署未执行） | LogQL 正则区分大小写；KQL 分词后不区分大小写 |
| Q3 | 每 5 分钟错误数（按 pod） | `sum by (pod) (count_over_time({...} \|~ "..." [5m]))` | 14 / 0.017s | ES\|QL：`FROM filebeat-* \| WHERE kubernetes.container.name == "newman" AND message RLIKE ".*(AssertionError\|FAILED\|Error:).*" \| STATS c = COUNT(*) BY b = BUCKET(@timestamp, 5 minutes), kubernetes.pod.name` | 未实测（可选部署未执行） | LogQL 内置指标化；Kibana 需 Lens / ES\|QL |
| Q4 | 全命名空间搜 `timeout` | `{namespace=~".+"} \|= "timeout"` | 4 / 0.615s | `message: "timeout"` | 未实测（可选部署未执行） | Loki 需扫全部 stream 的 chunk；ES 走倒排索引 |
| Q5 | 慢请求 > 500ms | `{namespace="qa-portfolio", container="newman"} \| regexp \`(?P<ms>[0-9]+)ms\` \| ms > 500` | 50 / 0.613s | ES\|QL：`FROM filebeat-* \| WHERE kubernetes.container.name == "newman" \| GROK message "%{NUMBER:ms:int}ms" \| WHERE ms > 500 \| STATS c = COUNT(*)` | 未实测（可选部署未执行） | Loki 查询时解析（schema-on-read）；ES 推荐写入时解析 |
| ALL | 全部命名空间日志行数 | `sum(count_over_time({namespace=~".+"}[24h]))` | 265 / 0.521s | `FROM filebeat-* \| STATS c = COUNT(*)` | 未实测（可选部署未执行） | — |

> KQL / ES\|QL 未在本项目执行，语法依据官方文档。

## §5 运维

| 项 | Loki | Elastic |
|----|------|---------|
| 保留 | compactor `retention_enabled: true` + `limits_config.retention_period: 168h`（本项目已配置，#97） | ILM 策略（rollover → delete）；Filebeat 默认 data stream 绑定内置 ILM 策略（官方） |
| 扩容 | 读写组件水平扩，存储在对象存储 | 加节点 + shard rebalance；主 shard 数需预规划（不可直接改，需 split / reindex，官方） |
| 备份 | 依赖对象存储自身（版本化 / 复制） | snapshot / restore 到快照仓库（官方） |
| 升级 | 二进制无状态；schema 按日期滚动 | 滚动升级；跨主版本需索引兼容（官方升级文档） |
| 采集器 | Alloy（Promtail 已 EOL，见 #98） | Filebeat / Elastic Agent（Fleet） |
| 安全 | 本项目 `auth_enabled: false`；多租户靠 `X-Scope-OrgID` | 8.x 起默认开启安全（TLS + 认证，官方） |

## §6 成本

| 项 | Loki | Elastic | 来源 |
|----|------|---------|------|
| 存储放大 | ≈ 201 B/行（实测，含索引与 WAL） | 未实测（可选部署未执行） | 实测 |
| 存储介质 | 对象存储（S3 / GCS / Azure Blob）为主 | 块存储为主；searchable snapshots 可用对象存储（订阅级别） | 官方 |
| 计算 | 320Mi requests（本项目） | 未实测（可选部署未执行） | 配置 / 官方 |
| 许可证 — Loki | AGPLv3 | — | grafana.com/licensing/（访问 2026-11-28） |
| 许可证 — 采集器 | Alloy：Apache 2.0（官方） | Beats：Apache 2.0 | 官方 |
| 许可证 — ES / Kibana | — | 源码 SSPL 1.0 / AGPLv3 / ELv2 三选一；默认发行版 ELv2；AGPLv3 自 8.16 起（官方 FAQ，2024-08-29 更新） | elastic.co/pricing/faq/licensing（访问 2026-11-28） |
| 付费功能 | Grafana Cloud / Enterprise Logs（可选） | ML、高级安全等按订阅级别 | 官方 |

> 仅列条款与链接，不作法律结论；对外提供托管服务时需法务评估 AGPL 网络条款 / ELv2 限制。

## §7 选型建议

| 场景 | 推荐 | 理由 |
|------|------|------|
| K8s 运维 / CI Job 日志（本项目） | Loki | 实测 116Mi；与 Prometheus 标签、Grafana 统一 |
| 安全分析 / SIEM | Elastic | 全文 + 字段索引、Elastic Security |
| 业务日志全文检索、字段未知的排查 | Elastic | 倒排索引，Q4 类查询不扫全量 |
| 企业已运行 Elastic | Elastic | 复用现有集群与 ILM / 权限体系，Filebeat 接入即可 |
| 日志量大、查询少、成本敏感 | Loki + 对象存储 | 只索引标签，存储便宜 |
| 高基数字段过滤（trace_id、user_id） | Elastic；或 Loki structured metadata | 见 §2 |

## 附录 A：测量方法

| 项 | 值 |
|----|----|
| 日期 | 2026-11-28 |
| 集群 | k3d `portfolio-dev`（1 server + 2 agents） |
| Loki | chart 6.6.2 / app 3.0.0，SingleBinary，filesystem，PVC 5Gi |
| 日志来源 | 合成 newman 风格 workload（200 + 50 行，含 500–1400ms 慢请求与错误行） |
| 采集方式 | 集群无外网且 Alloy 镜像拉取受限，本次通过 Loki Push API 注入日志；Alloy 配置已入库（`monitoring/loki/alloy-values.yaml`），镜像就绪后可用 `deploy-loki.sh` 复测 |
| 保留配置 | `/config` 返回 `retention_enabled: true`、`retention_period: 1w`、`max_query_length: 30d1h` |

```bash
# 安装
helm upgrade --install loki /tmp/loki-6.6.2.tgz -n monitoring --create-namespace \
  -f monitoring/loki/loki-values.yaml --set monitoring.serviceMonitor.enabled=false
# 端口转发 + 推送 + 查询
kubectl -n monitoring port-forward svc/loki 3100:3100
curl -X POST http://127.0.0.1:3100/loki/api/v1/push -H 'Content-Type: application/json' --data @payload.json
curl -G http://127.0.0.1:3100/loki/api/v1/query --data-urlencode 'query=sum(count_over_time({namespace="qa-portfolio", container="newman"}[24h]))'
```

## 附录 B：参考

| 主题 | 链接 | 访问日期 |
|------|------|----------|
| Loki architecture | https://grafana.com/docs/loki/latest/get-started/architecture/ | 2026-11-28 |
| Loki labels | https://grafana.com/docs/loki/latest/get-started/labels/ | 2026-11-28 |
| LogQL | https://grafana.com/docs/loki/latest/query/ | 2026-11-28 |
| Loki retention | https://grafana.com/docs/loki/latest/operations/storage/retention/ | 2026-11-28 |
| Promtail EOL | https://grafana.com/docs/loki/latest/send-data/promtail/ | 2026-11-28 |
| Grafana licensing | https://grafana.com/licensing/ | 2026-11-28 |
| Elastic licensing FAQ | https://www.elastic.co/pricing/faq/licensing | 2026-11-28 |
| Elastic ILM | https://www.elastic.co/guide/en/elasticsearch/reference/current/index-lifecycle-management.html | 2026-11-28 |
| KQL | https://www.elastic.co/guide/en/kibana/current/kuery-query.html | 2026-11-28 |
| ES\|QL | https://www.elastic.co/guide/en/elasticsearch/reference/current/esql.html | 2026-11-28 |
| Filebeat Kubernetes fields | https://www.elastic.co/guide/en/beats/filebeat/current/add-kubernetes-metadata.html | 2026-11-28 |

## 附录 C：相关 issue

| Issue | 内容 | 状态 |
|-------|------|------|
| #96 | 仪表盘 `job_name` 过滤无日志 | 已修复（PR #103） |
| #97 | Loki 未配置 retention | 已修复（PR #103） |
| #98 | Promtail EOL → Alloy 迁移 | 已修复（PR #103） |
