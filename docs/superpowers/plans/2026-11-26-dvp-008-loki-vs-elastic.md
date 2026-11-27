# REQ-DVP-008 Loki vs Elastic 对比 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 `docs/guides/LOGGING-LOKI-VS-ELASTIC.md`（架构 / 索引模型 / 资源 / 查询 / 运维 / 成本 / 选型建议），Loki 侧数据来自本项目实测，Elastic 侧来自官方来源（注明访问日期）。

**Architecture:** 在 k3d `portfolio-dev` 上临时安装 Loki + Promtail（不装 Prometheus），跑一次 newman Job，用 Loki HTTP API 实测 5 个查询（命中数 + 执行时间）、标签 / stream 数、内存、磁盘与保留配置；写入对比文档后卸载全部临时 release 与命名空间。Elastic 侧不部署：架构 / 索引 / 运维 / 许可证引用官方文档，实测类单元格写 `未实测（可选部署未执行）`。

**Tech Stack:** Loki chart 6.6.2 / Promtail chart 6.15.5（现有 values，`helm --repo` 安装）、k3d、kubectl、curl、jq、Markdown。

---

## 背景与约束

| 项 | 事实 | 来源 |
|----|------|------|
| Loki | SingleBinary，requests 100m/128Mi，limits 200m/256Mi，PVC 5Gi，tsdb v13，`auth_enabled: false`，**未配置 retention**（#97） | `monitoring/loki/loki-values.yaml` |
| Promtail | DaemonSet（容忍 control-plane → 3 节点 3 个 pod），每 pod requests 64Mi / limits 128Mi；chart 开启 ServiceMonitor | `monitoring/loki/promtail-values.yaml` |
| Promtail 生命周期 | "Promtail is end of life (EOL) as of March 2, 2026"，后续为 Grafana Alloy（#98） | grafana.com/docs/loki/latest/send-data/promtail/（访问 2026-11-27） |
| 集群现状 | `portfolio-dev` 未装 monitoring 栈；现有 portfolio-demo（ns `portfolio`）、external-secrets、traefik —— 本 REQ 不得改动 | 用户提供 |
| newman Job | release `qa-portfolio` 下 Job 名 `qa-portfolio-newman`，容器名 `newman`，镜像 `qa-newman-demo:latest`（`pullPolicy: Never`） | `helm/qa-portfolio/values.yaml`、`templates/job-newman.yaml` |
| 仪表盘 LogQL | `{namespace="qa-portfolio", job_name=~"newman.*"}` 匹配不到日志（#96）→ 本计划改用 `container="newman"`；Task 1 收集标签证据 | `monitoring/dashboards/test-failure-investigation.json` |
| 许可证 | ES / Kibana 源码：SSPL 1.0 / AGPLv3 / ELv2；默认发行版 ELv2；AGPLv3 于 8.16 GA 前加入（FAQ 更新 2024-08-29）；FAQ 称 Beats 为 Apache 2.0 | elastic.co/pricing/faq/licensing（访问 2026-11-27） |
| Loki 许可证 | Grafana Labs 将 Loki 改为 AGPLv3（生效版本待核实） | grafana.com/licensing/（访问 2026-11-27） |

## 范围

| 在范围 | 不在范围 |
|--------|----------|
| 对比文档（FR-DVP-008-01） | **可选部署（FR-DVP-008-02）本 REQ 不做**：用户决定（2026-11-27），原因为内存（OrbStack 8 GiB，ES + Kibana 约 2–2.5 GiB）与网络（镜像约 1.7 GB，拉取慢）限制；可作为后续 REQ |
| Task 1 临时安装 Loki + Promtail 做实测，结束后全部卸载 | ECK、Elastic 进 k3d、多节点、ILM 调优、替换 Loki |
| README 指南表登记 | 仪表盘 `job_name` 修复（#96）、Loki retention 配置（#97）、Promtail → Alloy 迁移（#98） |

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `devops-platform-demo/docs/guides/LOGGING-LOKI-VS-ELASTIC.md` | 新建 | 对比文档 |
| `devops-platform-demo/README.md` | 修改 | Documentation 表登记 + 日志链路处链接 |

## 占位约定

| 标记 | 含义 |
|------|------|
| `未实测（可选部署未执行）` | 仅用于 Elastic 实测类单元格（实测内存、同批日志磁盘 / 行数、启动耗时、命中数、字段数） |
| `TODO-verify` | 无法从官方来源核实的许可证 / 订阅事实；Task 2 Step 2 尽量核实，仍不能核实则保留并在 PR 描述列出 |

除以上两种外，文档不得含其他占位。Elastic 的非实测单元格（架构、配置默认值、限制、许可证）必须引用官方来源并注明访问日期。

---

### Task 1: Loki 实测（临时安装 Loki + Promtail，不装 Prometheus）

**Files:** 无仓库改动；记录写入 `/tmp/dvp008-loki.txt`（不提交）

> 前置：`docker stats --no-stream` 确认没有 REQ-009 / REQ-010 的 e2e 在跑；Loki + Promtail 合计 limits ≈ 640Mi。所有命令显式 `--kube-context` / `--context k3d-portfolio-dev`，工作目录 `devops-platform-demo/`。不修改本机 helm repo 配置（与 `scripts/install-eso.sh` 相同，用 `--repo`）。

- [x] **Step 1: 集群就绪 + 基线**

```bash
k3d cluster start portfolio-dev
kubectl --context k3d-portfolio-dev get nodes
helm --kube-context k3d-portfolio-dev list -A | tee /tmp/dvp008-baseline-helm.txt
kubectl --context k3d-portfolio-dev get ns -o name | sort | tee /tmp/dvp008-baseline-ns.txt
```
Expected: 3 节点 `Ready`；`list -A` 中无 `loki` / `promtail` / `dvp008-logs`；`get ns` 中**没有** `monitoring` 和 `qa-portfolio`（Step 7 会删除这两个命名空间 —— 若任一已存在，停下询问用户）。

- [x] **Step 2: 安装 Loki + Promtail（关闭 ServiceMonitor —— 没有 Prometheus Operator CRD）**

```bash
helm --kube-context k3d-portfolio-dev upgrade --install loki loki \
  --repo https://grafana.github.io/helm-charts --version 6.6.2 \
  -f monitoring/loki/loki-values.yaml --set monitoring.serviceMonitor.enabled=false \
  -n monitoring --create-namespace --wait --timeout 5m
helm --kube-context k3d-portfolio-dev upgrade --install promtail promtail \
  --repo https://grafana.github.io/helm-charts --version 6.15.5 \
  -f monitoring/loki/promtail-values.yaml --set serviceMonitor.enabled=false \
  -n monitoring --wait --timeout 3m
kubectl --context k3d-portfolio-dev get pods -n monitoring -o wide
```
Expected: `loki-0` 1/1 Running；`promtail-*` 3 个 Running（每节点一个）。记录 `loki-0` 从创建到 Ready 的秒数：
`kubectl --context k3d-portfolio-dev get pod loki-0 -n monitoring -o jsonpath='{.metadata.creationTimestamp} {.status.conditions[?(@.type=="Ready")].lastTransitionTime}{"\n"}'`

- [x] **Step 3: 跑 newman Job（独立 release，不影响其他 release）**

```bash
docker image inspect qa-newman-demo:latest >/dev/null || docker build -t qa-newman-demo:latest -f Dockerfile.newman .
k3d image import qa-newman-demo:latest -c portfolio-dev
helm --kube-context k3d-portfolio-dev upgrade --install dvp008-logs helm/qa-portfolio \
  -n qa-portfolio --create-namespace \
  --set cypress.enabled=false --set ingress.enabled=false --set ingressClass.enabled=false
kubectl --context k3d-portfolio-dev get jobs -n qa-portfolio
kubectl --context k3d-portfolio-dev wait --for=condition=complete job/dvp008-logs-qa-portfolio-newman -n qa-portfolio --timeout=300s
```
Expected: Job 名以 `get jobs` 输出为准（若不是 `dvp008-logs-qa-portfolio-newman`，后续命令替换）；`condition met`。若因集群无外网（jsonplaceholder）而 failed，日志仍可用于对比 —— 记录为 failed 并继续（失败日志让 Q2 有命中）。Job 默认 TTL 3600s，Step 4–6 须在 1 小时内完成。

- [x] **Step 4: 端口转发 + 标签实测（#96 证据）**

```bash
kubectl --context k3d-portfolio-dev -n monitoring port-forward svc/loki 3100:3100 >/tmp/dvp008-pf.log 2>&1 &
PF_PID=$!
LOKI=http://127.0.0.1:3100
until curl -fsS "$LOKI/ready" >/dev/null 2>&1; do sleep 2; done
START="$(( $(date +%s) - 86400 ))000000000"
{
curl -fsS "$LOKI/loki/api/v1/labels" | jq -c .data
curl -fsS -G "$LOKI/loki/api/v1/label/job_name/values" --data-urlencode "start=$START" | jq -c .data
curl -fsS -G "$LOKI/loki/api/v1/series" --data-urlencode 'match[]={namespace=~".+"}' --data-urlencode "start=$START" | jq '.data | length'
curl -fsS -G "$LOKI/loki/api/v1/query" --data-urlencode 'query=sum(count_over_time({namespace="qa-portfolio", job_name=~"newman.*"}[24h]))' | jq -c .data.result
} | tee -a /tmp/dvp008-loki.txt
kubectl --context k3d-portfolio-dev logs -n qa-portfolio job/dvp008-logs-qa-portfolio-newman | head -20
```
Expected: 标签含 `namespace`、`pod`、`container`、`node_name`（记录完整列表）；`job_name` 值列表与仪表盘查询结果（预期为空 / `[]`）—— 原样贴到 [#96](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/96) 评论；stream 总数（整数）；newman 输出中请求行形如 `GET https://... [200 OK, 7.94kB, 245ms]`（若格式不同，调整 Q5 正则并同步到文档）。

- [x] **Step 5: 5 个查询实测（命中数 + Loki 执行时间）**

```bash
q() { # q <label> <logql-metric-query>
  curl -fsS -G "$LOKI/loki/api/v1/query" --data-urlencode "query=$2" \
    | jq -r --arg l "$1" '"\($l) hits=\(.data.result[0].value[1] // "0") execTime=\(.data.stats.summary.execTime)s"'
}
{
q Q1 'sum(count_over_time({namespace="qa-portfolio", container="newman"}[24h]))'
q Q2 'sum(count_over_time({namespace="qa-portfolio", container="newman"} |~ "AssertionError|FAILED|Error:" [24h]))'
q Q3 'sum by (pod) (count_over_time({namespace="qa-portfolio", container="newman"} |~ "AssertionError|FAILED|Error:" [5m]))'
q Q4 'sum(count_over_time({namespace=~".+"} |= "timeout" [24h]))'
q Q5 'sum(count_over_time({namespace="qa-portfolio", container="newman"} | regexp `(?P<ms>\d+)ms` | ms > 500 [24h]))'
q ALL 'sum(count_over_time({namespace=~".+"}[24h]))'
} | tee -a /tmp/dvp008-loki.txt
```
Expected: 每行 `<label> hits=<整数> execTime=<秒>s`，命令 exit 0（任一 LogQL 语法错误 → curl 400 → 失败，修正后重跑）。Q1 > 0。Q3 为瞬时查询，0 命中可接受（记录"窗口外"）。

- [x] **Step 6: 资源与保留策略实测**

```bash
{
kubectl --context k3d-portfolio-dev top pod -n monitoring
kubectl --context k3d-portfolio-dev exec -n monitoring loki-0 -c loki -- du -sk /var/loki
curl -fsS "$LOKI/config" | grep -E 'retention_period|retention_enabled|max_query_length'
for n in server-0 agent-0 agent-1; do docker exec "k3d-portfolio-dev-$n" crictl images | grep -E 'grafana/(loki|promtail)'; done
} | tee -a /tmp/dvp008-loki.txt
kill "$PF_PID"
```
Expected: `top` 输出 loki-0 与 3 个 promtail 的 MiB；`du` 为 KiB（若镜像无 `du`，改用 `docker exec k3d-portfolio-dev-<loki 所在节点> du -sk /var/lib/rancher/k3s/storage/<pvc 目录>`）；`retention_enabled: false` / `retention_period: 0s`（证明当前无限保留至 PVC 满，#97）；Loki / Promtail 镜像大小。

- [x] **Step 7: 清理 —— 卸载三个 release，删除两个命名空间**

```bash
helm --kube-context k3d-portfolio-dev uninstall promtail loki -n monitoring --wait
helm --kube-context k3d-portfolio-dev uninstall dvp008-logs -n qa-portfolio --wait
kubectl --context k3d-portfolio-dev delete namespace monitoring qa-portfolio --wait
helm --kube-context k3d-portfolio-dev list -A
kubectl --context k3d-portfolio-dev get ns
diff /tmp/dvp008-baseline-helm.txt <(helm --kube-context k3d-portfolio-dev list -A) && echo HELM_BASELINE_OK
diff /tmp/dvp008-baseline-ns.txt <(kubectl --context k3d-portfolio-dev get ns -o name | sort) && echo NS_BASELINE_OK
```
Expected: `list -A` 无 `loki` / `promtail` / `dvp008-logs`；`HELM_BASELINE_OK`（与 Step 1 完全一致，即 portfolio-demo、external-secrets、traefik 的 release 未被改动）；`get ns` 无 `monitoring` / `qa-portfolio`；`NS_BASELINE_OK`（`portfolio`、`external-secrets`、`kube-system` 等仍在）。任一 diff 有输出 → 停下报告用户。`k3d image import` 的镜像留在节点上（不影响其他 release）。

- [x] **Step 8:** 无提交（实测记录在 `/tmp/dvp008-loki.txt`，Task 2 写入文档）。

### Task 2: 编写对比文档

**Files:** Create `devops-platform-demo/docs/guides/LOGGING-LOKI-VS-ELASTIC.md`

- [x] **Step 1: 按下列结构写文档**（中文正文，表格优先；`<T1:...>` 表示填入 Task 1 记录值，提交时必须是实际数字）

**标题与引言：** `# Loki vs Elastic 日志栈对比（REQ-DVP-008）`；引言 2 行：本项目日志栈为 Loki + Promtail（REQ-DVP-002）；Elastic 未在本项目部署（FR-DVP-008-02 未执行，原因：内存 / 网络限制），Elastic 列引用官方来源。数据来源图例表：

| 标记 | 含义 |
|------|------|
| 实测 | 本项目 k3d `portfolio-dev` 测得（日期） |
| 配置 | 仓库文件中的值（给出路径） |
| 官方 | 官方文档链接（访问日期） |
| 未实测（可选部署未执行） | Elastic 未部署，无实测值 |

**§0 速览**（一张表）：

| 维度 | Loki + Promtail | Elastic（ES + Kibana + Filebeat） |
|------|-----------------|-----------------------------------|
| 索引 | 只索引标签，日志行压缩存 chunk（官方） | 倒排索引（全文）+ 字段索引 / doc values（官方） |
| 查询语言 | LogQL（官方） | KQL（Kibana）/ ES\|QL / Query DSL（官方） |
| 本项目 requests / limits | Loki 128Mi/256Mi + Promtail 64Mi/128Mi × 3 = 320Mi/640Mi（配置） | 官方单节点 Docker 文档的最低内存建议（官方，Task 2 Step 2 核实具体值） |
| 实测内存 | `<T1: loki-0 + 3×promtail MiB>`（实测） | 未实测（可选部署未执行） |
| 许可证 | AGPLv3（官方） | 源码 SSPL / AGPLv3 / ELv2，默认发行版 ELv2（官方） |
| 本项目结论 | 继续使用（K8s / CI 日志） | 安全分析 / 全文检索 / 企业已有 Elastic 时选用 |

**§1 架构**：两个 ASCII 图：
- Loki（本项目实际链路）：`/var/log/pods (k3d 节点) → Promtail DaemonSet (cri stage) → Loki SingleBinary (tsdb 索引 + filesystem chunk, PVC 5Gi) → Grafana Explore`
- Elastic（官方典型链路，未部署）：`/var/log/pods → Filebeat / Elastic Agent DaemonSet → Elasticsearch (data stream, shard) → Kibana Discover`

| 组件 | Loki 栈 | Elastic 栈 |
|------|---------|-----------|
| 采集 | Promtail（EOL 2026-03-02，后继 Alloy，见 #98，官方） | Filebeat / Elastic Agent（官方） |
| 存储 | 标签索引（tsdb）+ 对象存储 / 文件系统 chunk | Lucene 段，按 shard 分布，`_source` + 倒排 + doc values |
| 部署形态 | SingleBinary → SimpleScalable（read/write/backend）→ 微服务 | 单节点 → 多节点（master / data / ingest 角色） |
| 扩展单元 | 无状态读写组件 + 共享对象存储 | shard + replica，节点加入后 rebalance |
| UI | Grafana（与 Prometheus 指标同一界面） | Kibana |
| 本项目部署 | Helm（`monitoring/loki/`，k3d 内） | 未部署 |

**§2 索引模型**：

| 项 | Loki | Elastic |
|----|------|---------|
| 被索引的内容 | 仅标签（本项目：`<T1: 标签列表>`） | 每个字段 + `message` 全文（分词） |
| 查询路径 | 标签选 stream → 拉 chunk → 行过滤（grep 式） | 倒排索引直接定位文档 |
| 本项目 stream / 字段数 | `<T1: series 数>` stream（实测） | 未实测（可选部署未执行） |
| 高基数 | 标签值多 → stream 数爆炸、chunk 碎片化；官方建议标签保持低基数（官方：Loki labels 文档） | 字段多 → mapping 膨胀（`index.mapping.total_fields.limit` 默认 1000，官方）；高基数聚合耗堆内存 |
| 高基数字段的 Loki 方案 | structured metadata（Loki 3.x，官方，TODO-verify 版本） | — |
| schema 变更 | `schema_config` 按日期切换，旧数据不迁移（配置：`from: 2024-01-01`, v13） | mapping 已有字段不可改，需 reindex（官方） |
| 匹配语义 | 行过滤为字节 / 正则匹配，区分大小写 | `text` 字段按分词匹配，默认不区分大小写 → 同一"关键词"命中数可能不同（见 §4 Q2） |
| 仪表盘标签陷阱 | `job_name` 标签无日志匹配（实测 `<T1>`，见 #96） | — |

**§3 资源**：

| 组件 | requests | limits | 实测内存 | 启动到就绪 | 镜像大小 | 来源 |
|------|----------|--------|----------|-----------|----------|------|
| Loki | 100m / 128Mi | 200m / 256Mi | `<T1>` MiB | `<T1>` s | `<T1>` | 配置 + 实测 |
| Promtail ×3 | 50m / 64Mi 每个 | 100m / 128Mi 每个 | `<T1>` MiB（合计） | — | `<T1>` | 配置 + 实测 |
| Elasticsearch | 官方 heap / 内存建议（核实后填） | — | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 官方 |
| Kibana | 官方建议（核实后填，查不到写 TODO-verify） | — | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 未实测（可选部署未执行） | 官方 |

| 同一批日志（Task 1 窗口） | Loki | Elastic |
|----------------------------|------|---------|
| 日志行数 | `<T1: ALL>`（newman `<T1: Q1>`） | 未实测（可选部署未执行） |
| 磁盘 | `<T1: du>` KiB | 未实测（可选部署未执行） |
| 字节 / 行 | 计算值 | 未实测（可选部署未执行） |

**§4 查询**（同一需求对照；Loki 列填 Task 1 实测；KQL / ES|QL 写法依据官方语法文档）：

| # | 需求 | LogQL | Loki 命中 / execTime | KQL（或 ES\|QL） | Elastic 命中 | 差异说明 |
|---|------|-------|----------------------|------------------|-------------|----------|
| Q1 | newman Job 全部日志 | `{namespace="qa-portfolio", container="newman"}` | `<T1>` | `kubernetes.namespace: "qa-portfolio" and kubernetes.container.name: "newman"` | 未实测（可选部署未执行） | 字段名按 Filebeat `add_kubernetes_metadata` 官方字段（核实） |
| Q2 | newman 错误行 | `{namespace="qa-portfolio", container="newman"} \|~ "AssertionError\|FAILED\|Error:"` | `<T1>` | `kubernetes.namespace: "qa-portfolio" and kubernetes.container.name: "newman" and message: ("AssertionError" or "FAILED" or "Error:")` | 未实测（可选部署未执行） | LogQL 正则区分大小写；KQL 分词后不区分大小写 |
| Q3 | 每 5 分钟错误数（按 pod） | `sum by (pod) (count_over_time({...} \|~ "..." [5m]))` | `<T1>` | KQL 只能过滤 → ES\|QL：`FROM filebeat-* \| WHERE kubernetes.container.name == "newman" AND message RLIKE ".*(AssertionError\|FAILED\|Error:).*" \| STATS c = COUNT(*) BY b = BUCKET(@timestamp, 5 minutes), kubernetes.pod.name` | 未实测（可选部署未执行） | LogQL 内置指标化；Kibana 需 Lens / ES\|QL |
| Q4 | 全命名空间搜 `timeout` | `{namespace=~".+"} \|= "timeout"` | `<T1>` | `message: "timeout"` | 未实测（可选部署未执行） | Loki 需扫全部 stream 的 chunk；ES 走倒排索引 |
| Q5 | 慢请求 > 500ms | `{namespace="qa-portfolio", container="newman"} \| regexp "(?P<ms>\\d+)ms" \| ms > 500` | `<T1>` | KQL 不能查询时抽取 → ES\|QL：`FROM filebeat-* \| WHERE kubernetes.container.name == "newman" \| GROK message "%{NUMBER:ms:int}ms" \| WHERE ms > 500 \| STATS c = COUNT(*)`；或 ingest pipeline / runtime field | 未实测（可选部署未执行） | Loki 查询时解析（schema-on-read）；ES 推荐写入时解析 |

表下：LogQL 计数写法（`sum(count_over_time(<query>[24h]))`）与 Task 1 Step 5 命令（复制）；注明 KQL / ES|QL 未在本项目执行（语法依据官方文档）。

**§5 运维**：

| 项 | Loki | Elastic |
|----|------|---------|
| 保留 | compactor `retention_enabled` + `limits_config.retention_period`；**本项目当前未配置**（实测 `/config`：`<T1>`）→ 保留到 PVC 5Gi 满，见 #97 | ILM 策略（rollover → delete）；Filebeat 默认 data stream 绑定内置 ILM 策略（官方） |
| 扩容 | 读写组件水平扩，存储在对象存储 | 加节点 + shard rebalance；主 shard 数需预规划（不可直接改，需 split / reindex，官方） |
| 备份 | 依赖对象存储自身（版本化 / 复制） | snapshot / restore 到快照仓库（官方） |
| 升级 | 二进制无状态；schema 按日期滚动 | 滚动升级；跨主版本需索引兼容（官方升级文档） |
| 采集器 | Promtail 已 EOL（2026-03-02）→ 迁移 Alloy，见 #98 | Filebeat / Elastic Agent（Fleet） |
| 安全 | 本项目 `auth_enabled: false`；多租户靠 `X-Scope-OrgID` | 8.x 起默认开启安全（TLS + 认证，官方，核实） |

**§6 成本**：

| 项 | Loki | Elastic | 来源 |
|----|------|---------|------|
| 存储放大 | 字节/行 `<T1 计算>` | 未实测（可选部署未执行） | 实测 |
| 存储介质 | 对象存储（S3 / GCS / Azure Blob）为主 | 块存储为主；searchable snapshots 可用对象存储（订阅级别 TODO-verify） | 官方 |
| 计算 | 320Mi requests（本项目） | 官方最低建议（§3） | 配置 / 官方 |
| 许可证 — Loki | AGPLv3（生效版本 TODO-verify） | — | grafana.com/licensing/（访问 2026-11-27） |
| 许可证 — 采集器 | Promtail：随 Loki 仓库（TODO-verify）；Alloy：TODO-verify | Beats：FAQ 列为 Apache 2.0；默认二进制发行版许可 TODO-verify | elastic.co/pricing/faq/licensing（访问 2026-11-27） |
| 许可证 — ES / Kibana | — | 源码 SSPL 1.0 / AGPLv3 / ELv2 三选一；默认发行版 ELv2；AGPLv3 自 8.16 起（FAQ 2024-08-29 更新） | 同上 |
| 付费功能 | Grafana Cloud / Enterprise Logs（可选） | ML、高级安全等按订阅级别（TODO-verify，elastic.co/subscriptions） | 官方 |
| 说明 | 仅列条款与链接，不作法律结论；对外提供托管服务时需法务评估 AGPL 网络条款 / ELv2 限制 | | |

**§7 选型建议**：

| 场景 | 推荐 | 理由 |
|------|------|------|
| K8s 运维 / CI Job 日志（本项目） | Loki | 实测 `<T1>` MiB；与 Prometheus 标签、Grafana 统一 |
| 安全分析 / SIEM | Elastic | 全文 + 字段索引、Elastic Security |
| 业务日志全文检索、字段未知的排查 | Elastic | 倒排索引，Q4 类查询不扫全量 |
| 企业已运行 Elastic（如 SCOM / Elastic 环境） | Elastic | 复用现有集群与 ILM / 权限体系，Filebeat 接入即可 |
| 日志量大、查询少、成本敏感 | Loki + 对象存储 | 只索引标签，存储便宜 |
| 高基数字段过滤（trace_id、user_id） | Elastic；或 Loki structured metadata | 见 §2 |

**附录 A：测量方法**：Task 1 Step 2–6 命令（安装、标签、5 个查询、资源、保留）+ 清理命令；注明测量日期与 chart 版本。

**附录 B：参考**：每条链接带访问日期（Loki architecture / labels / retention / LogQL、Promtail EOL、Grafana licensing、Elastic licensing FAQ、ILM、KQL、ES|QL、single-node Docker 内存要求、mapping limit、Filebeat kubernetes 字段）。

**附录 C：相关 issue**：#96（仪表盘 `job_name`）、#97（Loki retention）、#98（Promtail → Alloy）。

- [x] **Step 2: 核实官方来源**：对附录 B 每个链接用 WebFetch 打开，确认引用的句子 / 默认值存在；能核实的 `TODO-verify` 与"核实"标注改为事实 + 访问日期；不能核实的保留 `TODO-verify`。
- [x] **Step 3: 自检**

```bash
cd devops-platform-demo
f=docs/guides/LOGGING-LOKI-VS-ELASTIC.md
grep -n '<T1\|核实后填\|（核实）' "$f"            # Expected: 无输出
grep -n -i 'TBD\|TODO[^-]\|interview\|面试' "$f"     # Expected: 无输出
grep -c '未实测（可选部署未执行）' "$f"               # Expected: > 0（仅 Elastic 实测单元格）
grep -n 'TODO-verify' "$f"                           # Expected: 仅 §2 / §6 许可证与订阅行
grep -c '#96\|#97\|#98' "$f"                          # Expected: ≥ 3
```
- [x] **Step 4:** Commit `docs(dvp): add Loki vs Elastic logging comparison guide`

### Task 3: 登记文档

**Files:** Modify `devops-platform-demo/README.md`（Documentation 表，约第 489 行 `SECRETS-RBAC.md` 行之后；第 292 行日志链路）

- [x] **Step 1:** 插入 `| [LOGGING-LOKI-VS-ELASTIC.md](docs/guides/LOGGING-LOKI-VS-ELASTIC.md) | Loki vs Elastic 日志栈对比（REQ-DVP-008） |`
- [x] **Step 2:** 在 `Pod logs ──→ Promtail ──→ Loki ──→ Grafana log explorer` 所在代码块之后加一行：`> 与 Elastic 的取舍见 [LOGGING-LOKI-VS-ELASTIC.md](docs/guides/LOGGING-LOKI-VS-ELASTIC.md)`
- [x] **Step 3:** 链接检查：`grep -o '](docs/guides/LOGGING-LOKI-VS-ELASTIC.md)' README.md | wc -l` → `2`；`test -f docs/guides/LOGGING-LOKI-VS-ELASTIC.md && echo OK`
- [x] **Step 4:** Commit `docs(dvp): link logging comparison guide from README`

---

## 需求追踪

| 需求 / 验收 | 任务 |
|-------------|------|
| FR-DVP-008-01 架构、索引模型、资源占用、成本、LogQL vs KQL | Task 1（Loki 实测）、Task 2（§1 架构、§2 索引、§3 资源、§4 查询、§6 成本；另含 §5 运维、§7 选型）、Task 3（登记） |
| FR-DVP-008-02 可选部署 | 不做（用户决定 2026-11-27，见范围表）；验收项标注"不在本 REQ" |
| 验收：对比文档完成 | Task 2 Step 3 自检 |
| 设计 §6：所有对比项有来源（实测 / 链接） | Task 2 Step 1 图例 + Step 2 核实 |
| 设计 §6：LogQL 查询在本项目 Loki 可执行 | Task 1 Step 5 |
| 设计 §7：许可证引用官方页面并注明日期 | Task 2 Step 2、§6 |
| #96 标签证据 | Task 1 Step 4 |

## Stage 4 / 5

| 阶段 | 内容 | 状态 |
|------|------|------|
| 4 | 文档自检（Task 2 Step 3）、README 链接检查；push `feature/dvp-008-logging-comparison`，开 PR，CI 全绿 | ✅ PR #105 |
| 5 | REQUIREMENTS 章节：FR-DVP-008-02 标"不在本 REQ（2026-11-28 用户决定）"、勾文档验收、总表状态 Done；`docs/REQUIREMENTS-INDEX.md:102` 状态；更新 memory | ✅ |

> 实施偏差：Promtail 已由 #98 迁移为 Grafana Alloy；Alloy 镜像拉取受限，查询实测使用 Loki Push API 注入日志（见文档附录 A）。
