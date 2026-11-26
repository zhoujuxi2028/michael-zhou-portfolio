# REQ-DVP-008 Loki vs Elastic 对比 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 `docs/guides/LOGGING-LOKI-VS-ELASTIC.md`（架构 / 索引模型 / 资源 / 查询 / 运维 / 成本 / 选型建议），内容来自本项目 Loki 实测 + 官方来源；可选交付 `docker-compose.elastic.yml`（单节点 ES + Kibana + Filebeat，读取与 Promtail 同源的 k3d pod 日志），带内存前置检查，CI 只做 `docker compose config`。

**Architecture:** 路径 A（必做，不运行 Elastic）：在 k3d 上临时装 Loki + Promtail（不装 Prometheus），跑一次 newman Job，用 Loki HTTP API 实测 5 个查询与资源，写对比文档；Elastic 实测值用 `⏳B` 占位。路径 B（可选）：Filebeat 在 compose 中以只读方式挂载 k3d 节点容器的 `/var/log` 匿名卷（`/k3d/<node>/pods/...`），失败则回退为 `docker cp` 快照卷；`monitoring/elastic/preflight.sh` 检查镜像已拉取、monitoring 命名空间为空、Docker VM 与宿主空闲内存 ≥ 3 GiB 后才 `up`；跑完回填 `⏳B` 并 `down -v`。

**Tech Stack:** Loki chart 6.6.2 / Promtail chart 6.15.5（现有）、Elasticsearch / Kibana / Filebeat **9.5.7**（`docker.elastic.co`，Stage 3 Task 4 核实）、Docker Compose v2（本机 v5.5.0）、bash（本机 3.2 / CI 5 兼容）、jq、curl、shellcheck、actionlint。

---

## 背景与约束

| 项 | 事实 | 来源 |
|----|------|------|
| Loki | SingleBinary，requests 100m/128Mi，limits 200m/256Mi，PVC 5Gi，tsdb v13，`auth_enabled: false`，**未配置 retention** | `monitoring/loki/loki-values.yaml` |
| Promtail | DaemonSet（容忍 control-plane → 3 节点 3 个 pod），每 pod requests 64Mi / limits 128Mi；chart 开启 ServiceMonitor | `monitoring/loki/promtail-values.yaml` |
| Promtail 生命周期 | "Promtail is end of life (EOL) as of March 2, 2026"，后续为 Grafana Alloy | grafana.com/docs/loki/latest/send-data/promtail/（访问 2026-11-27） |
| 集群现状 | `portfolio-dev` 未装 monitoring 栈；只有 portfolio-demo、ESO、traefik | 用户提供 |
| newman Job | release `qa-portfolio` 下 Job 名 `qa-portfolio-newman`，容器名 `newman`，`ttlSecondsAfterFinished: 3600`，镜像 `qa-newman-demo:latest`（`pullPolicy: Never`） | `helm/qa-portfolio/values.yaml`、`templates/job-newman.yaml` |
| 仪表盘 LogQL | `{namespace="qa-portfolio", job_name=~"newman.*"}`；Job 名以 `qa-portfolio-` 开头，`job_name` 标签可能不存在或不匹配 → 本计划改用 `container="newman"`（Task 1 实测确认） | `monitoring/dashboards/test-failure-investigation.json` |
| 内存 | OrbStack 上限 8 GiB；宿主 16 GB 空闲约 5 GB；OrbStack 曾在内存压力下崩溃 | 设计 §2 |
| 并行 | REQ-009 / REQ-010 并行开发；路径 B 期间**不得**运行其 e2e，也不得运行 monitoring 栈 | 设计 §1 非目标 |
| 镜像 | ES + Kibana 约 1.7 GB，拉取慢；preflight 不隐式拉取 | 设计 §2 |
| 许可证 | ES / Kibana 源码：SSPL 1.0 / AGPLv3 / ELv2；默认发行版 ELv2；AGPLv3 于 8.16 GA 前加入（FAQ 更新 2024-08-29）；FAQ 称 Beats 为 Apache 2.0 | elastic.co/pricing/faq/licensing（访问 2026-11-27） |
| Loki 许可证 | Grafana Labs 将 Loki 改为 AGPLv3（生效版本待核实） | grafana.com/licensing/（访问 2026-11-27） |
| 版本 | 下载页 9.5.7（2026-11-23）；9.6 已发布但下载页未上架；2026-11-18 安全公告涉及 8.19.26 / 9.4.10 / 9.5.7（Task 4 核实 9.5.7 是否为修复版） | elastic.co/downloads/elasticsearch、discuss.elastic.co 安全公告（访问 2026-11-27） |

## 范围

| 在范围 | 不在范围 |
|--------|----------|
| 对比文档（路径 A，必做） | ECK、Elastic 进 k3d、多节点、ILM 调优 |
| compose + filebeat.yml + preflight + up/verify/down 脚本（路径 B，可选） | 替换 Loki；Promtail → Alloy 迁移（只记为后续项） |
| PR CI：`docker compose -f docker-compose.elastic.yml config --quiet` | CI 中启动 Elastic |
| README 指南表登记 | 修复仪表盘 `job_name` 查询（记为后续项） |

## 文件结构

| 文件 | 操作 | 路径 | 职责 |
|------|------|------|------|
| `devops-platform-demo/docs/guides/LOGGING-LOKI-VS-ELASTIC.md` | 新建 | A | 对比文档 |
| `devops-platform-demo/README.md` | 修改 | A | Documentation 表登记 |
| `devops-platform-demo/tests/test-elastic-preflight.sh` | 新建 | B | preflight 离线测试（PATH stub） |
| `devops-platform-demo/monitoring/elastic/preflight.sh` | 新建 | B | 镜像 / monitoring / 内存 / 节点卷检查，写 `.env` |
| `devops-platform-demo/monitoring/elastic/filebeat.yml` | 新建 | B | filestream + CRI 解析 + 路径 dissect |
| `devops-platform-demo/docker-compose.elastic.yml` | 新建 | B | ES + Kibana + Filebeat |
| `devops-platform-demo/monitoring/elastic/elastic-up.sh` / `verify-elastic.sh` / `elastic-down.sh` | 新建 | B | 启动 / 验证 / 清理 |
| `.github/workflows/dvp-pr.yml` | 修改 | B | Unit Tests job 加 compose 语法检查 |

> `monitoring/elastic/.env`（preflight 生成）已被 `devops-platform-demo/.gitignore:20` 的 `.env` 规则忽略（`git check-ignore -v` 已确认）。

## 占位约定

| 标记 | 含义 | 何时消除 |
|------|------|----------|
| `⏳B` | Elastic 实测值（内存、磁盘、启动时间、命中数、镜像大小等） | Task 9 回填；若用户决定不做 B → 统一替换为 `未实测（可选部署未执行）` 并保留配置值 |
| `TODO-verify` | 无法从官方来源核实的许可证 / 订阅事实 | Task 2 Step 2 尽量核实；仍无法核实则保留并在 PR 描述列出 |

除以上两种外，文档不得含其他占位。

---

## 路径 A — 文档（必做，不运行 Elastic）

### Task 1: Loki 实测（临时安装 Loki + Promtail，不装 Prometheus）

**Files:** 无仓库改动；记录写入 `/tmp/dvp008-loki.txt`（不提交）

> 前置：`docker stats --no-stream` 确认没有 REQ-009 / REQ-010 的 e2e 在跑；Loki + Promtail 合计 limits ≈ 640Mi，可与 portfolio-demo 共存。所有命令显式 `--kube-context` / `--context k3d-portfolio-dev`，工作目录 `devops-platform-demo/`。

- [ ] **Step 1: 集群就绪**

```bash
k3d cluster start portfolio-dev
kubectl --context k3d-portfolio-dev get nodes
helm --kube-context k3d-portfolio-dev list -A
```
Expected: 3 节点 `Ready`；`list -A` 中无 `loki` / `promtail` / `dvp008-logs`（若 `monitoring` 命名空间已有 release，先停下询问用户）。

- [ ] **Step 2: 安装 Loki + Promtail（关闭 ServiceMonitor —— 没有 Prometheus Operator CRD）**

```bash
helm repo list | grep -q '^grafana' || helm repo add grafana https://grafana.github.io/helm-charts
helm repo update grafana
helm --kube-context k3d-portfolio-dev upgrade --install loki grafana/loki --version 6.6.2 \
  -f monitoring/loki/loki-values.yaml --set monitoring.serviceMonitor.enabled=false \
  -n monitoring --create-namespace --wait --timeout 5m
helm --kube-context k3d-portfolio-dev upgrade --install promtail grafana/promtail --version 6.15.5 \
  -f monitoring/loki/promtail-values.yaml --set serviceMonitor.enabled=false \
  -n monitoring --wait --timeout 3m
kubectl --context k3d-portfolio-dev get pods -n monitoring -o wide
```
Expected: `loki-0` 1/1 Running；`promtail-*` 3 个 Running（每节点一个）。记录 `loki-0` 从创建到 Ready 的秒数：
`kubectl --context k3d-portfolio-dev get pod loki-0 -n monitoring -o jsonpath='{.metadata.creationTimestamp} {.status.conditions[?(@.type=="Ready")].lastTransitionTime}{"\n"}'`

- [ ] **Step 3: 跑 newman Job（独立 release，日志保留 24h 供路径 B 复用）**

```bash
docker image inspect qa-newman-demo:latest >/dev/null || docker build -t qa-newman-demo:latest -f Dockerfile.newman .
k3d image import qa-newman-demo:latest -c portfolio-dev
helm --kube-context k3d-portfolio-dev upgrade --install dvp008-logs helm/qa-portfolio \
  -n qa-portfolio --create-namespace \
  --set cypress.enabled=false --set ingress.enabled=false --set ingressClass.enabled=false \
  --set newman.ttlSecondsAfterFinished=86400
kubectl --context k3d-portfolio-dev get jobs -n qa-portfolio
kubectl --context k3d-portfolio-dev wait --for=condition=complete job/dvp008-logs-qa-portfolio-newman -n qa-portfolio --timeout=300s
```
Expected: Job 名以 `get jobs` 输出为准（若不是 `dvp008-logs-qa-portfolio-newman`，后续命令替换）；`condition met`。若因集群无外网（jsonplaceholder）而 failed，日志仍可用于对比 —— 记录为 failed 并继续（失败日志正好让 Q2 有命中）。

- [ ] **Step 4: 端口转发 + 标签实测**

```bash
kubectl --context k3d-portfolio-dev -n monitoring port-forward svc/loki 3100:3100 >/tmp/dvp008-pf.log 2>&1 &
PF_PID=$!
LOKI=http://127.0.0.1:3100
until curl -fsS "$LOKI/ready" >/dev/null 2>&1; do sleep 2; done
START="$(( $(date +%s) - 86400 ))000000000"
curl -fsS "$LOKI/loki/api/v1/labels" | jq -c .data
curl -fsS -G "$LOKI/loki/api/v1/label/job_name/values" --data-urlencode "start=$START" | jq -c .data
curl -fsS -G "$LOKI/loki/api/v1/series" --data-urlencode 'match[]={namespace=~".+"}' --data-urlencode "start=$START" | jq '.data | length'
kubectl --context k3d-portfolio-dev logs -n qa-portfolio job/dvp008-logs-qa-portfolio-newman | head -20
```
Expected: 标签含 `namespace`、`pod`、`container`、`node_name`（记录完整列表）；`job_name` 值列表（记录是否为空 → 仪表盘问题证据）；stream 总数（整数）；newman 输出中请求行形如 `GET https://... [200 OK, 7.94kB, 245ms]`（若格式不同，调整 Q5 正则并同步到文档）。

- [ ] **Step 5: 5 个查询实测（命中数 + Loki 执行时间）**

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
} | tee -a /tmp/dvp008-loki.txt
```
Expected: 每行 `Qn hits=<整数> execTime=<秒>s`，命令 exit 0（任一 LogQL 语法错误 → curl 400 → 失败，修正后重跑）。Q1 > 0。Q3 为瞬时查询，0 命中可接受（记录"窗口外"）。

- [ ] **Step 6: 资源与保留策略实测**

```bash
{
kubectl --context k3d-portfolio-dev top pod -n monitoring
kubectl --context k3d-portfolio-dev exec -n monitoring loki-0 -c loki -- du -sk /var/loki
curl -fsS "$LOKI/config" | grep -E 'retention_period|retention_enabled|max_query_length'
for n in server-0 agent-0 agent-1; do docker exec "k3d-portfolio-dev-$n" crictl images | grep -E 'grafana/(loki|promtail)'; done
} | tee -a /tmp/dvp008-loki.txt
kill "$PF_PID"
```
Expected: `top` 输出 loki-0 与 3 个 promtail 的 MiB；`du` 为 KiB（若镜像无 `du`，改用 `docker exec k3d-portfolio-dev-<loki 所在节点> du -sk /var/lib/rancher/k3s/storage/` 下对应 PVC 目录）；`retention_enabled: false` / `retention_period: 0s`（证明当前无限保留至 PVC 满）；Loki / Promtail 镜像大小。

- [ ] **Step 7: 卸载 Loki + Promtail（路径 B 要求 monitoring 为空），保留 newman Job 日志**

```bash
helm --kube-context k3d-portfolio-dev uninstall promtail loki -n monitoring
kubectl --context k3d-portfolio-dev delete namespace monitoring --wait
kubectl --context k3d-portfolio-dev get pods -n qa-portfolio
```
Expected: monitoring 命名空间删除；newman pod 仍为 `Completed`（或 `Error`）—— 日志文件在节点 `/var/log/pods` 中保留到 TTL（24h）。若不做路径 B：`helm --kube-context k3d-portfolio-dev uninstall dvp008-logs -n qa-portfolio`。

- [ ] **Step 8:** 无提交（实测记录在 `/tmp/dvp008-loki.txt`，Task 2 写入文档）。

### Task 2: 编写对比文档

**Files:** Create `devops-platform-demo/docs/guides/LOGGING-LOKI-VS-ELASTIC.md`

- [ ] **Step 1: 按下列结构写文档**（中文正文，表格优先；`<T1:...>` 表示填入 Task 1 记录值，提交时必须是实际数字）

**标题与引言：** `# Loki vs Elastic 日志栈对比（REQ-DVP-008）`；引言 2 行：本项目日志栈为 Loki + Promtail（REQ-DVP-002）；数据来源图例表：

| 标记 | 含义 |
|------|------|
| 实测 | 本项目 k3d `portfolio-dev` 测得（日期） |
| 配置 | 仓库文件中的值（给出路径） |
| 官方 | 官方文档链接（访问日期） |
| `⏳B` | 待可选部署实测 |

**§0 速览**（一张表，7 行）：

| 维度 | Loki + Promtail | Elastic（ES + Kibana + Filebeat） |
|------|-----------------|-----------------------------------|
| 索引 | 只索引标签，日志行压缩存 chunk | 倒排索引（全文）+ 字段索引 / doc values |
| 查询语言 | LogQL | KQL（Kibana）/ ES\|QL / Query DSL |
| 本项目 requests / limits | Loki 128Mi/256Mi + Promtail 64Mi/128Mi × 3 = 320Mi/640Mi（配置） | ES heap 512m / `mem_limit` 1536m + Kibana 1g + Filebeat 256m（配置，compose） |
| 实测内存 | `<T1: loki-0 + 3×promtail MiB>`（实测） | `⏳B` |
| 同一批日志磁盘 | `<T1: du KiB>`（实测） | `⏳B` |
| 许可证 | AGPLv3（官方） | 源码 SSPL / AGPLv3 / ELv2，默认发行版 ELv2（官方） |
| 本项目结论 | 继续使用（K8s / CI 日志） | 安全分析 / 全文检索 / 企业已有 Elastic 时选用 |

**§1 架构**：两个 ASCII 图（本项目实际链路）：
- Loki：`/var/log/pods (k3d 节点) → Promtail DaemonSet (cri stage) → Loki SingleBinary (tsdb 索引 + filesystem chunk, PVC 5Gi) → Grafana Explore`
- Elastic：`/var/log/pods (k3d 节点 /var/log 卷, 只读) → Filebeat (compose, filestream + cri parser) → Elasticsearch 单节点 (data stream filebeat-*) → Kibana Discover`

| 组件 | Loki 栈 | Elastic 栈 |
|------|---------|-----------|
| 采集 | Promtail（EOL 2026-03-02，后继 Alloy，官方） | Filebeat / Elastic Agent |
| 存储 | 标签索引（tsdb）+ 对象存储 / 文件系统 chunk | Lucene 段，按 shard 分布，`_source` + 倒排 + doc values |
| 部署形态 | SingleBinary → SimpleScalable（read/write/backend）→ 微服务 | 单节点 → 多节点（master / data / ingest 角色） |
| 扩展单元 | 无状态读写组件 + 共享对象存储 | shard + replica，节点加入后 rebalance |
| UI | Grafana（与 Prometheus 指标同一界面） | Kibana |
| 本项目部署 | Helm（`monitoring/loki/`，k3d 内） | compose（`docker-compose.elastic.yml`，k3d 外，可选） |

**§2 索引模型**：

| 项 | Loki | Elastic |
|----|------|---------|
| 被索引的内容 | 仅标签（本项目：`<T1: 标签列表>`） | 每个字段 + `message` 全文（分词） |
| 查询路径 | 标签选 stream → 拉 chunk → 行过滤（grep 式） | 倒排索引直接定位文档 |
| 本项目 stream / 字段数 | `<T1: series 数>` stream（实测） | `⏳B` 字段（`GET filebeat-*/_mapping` 计数） |
| 高基数 | 标签值多 → stream 数爆炸、chunk 碎片化；官方建议标签保持低基数（官方：Loki labels 文档） | 字段多 → mapping 膨胀（`index.mapping.total_fields.limit` 默认 1000，官方）；高基数聚合耗堆内存 |
| 高基数字段的 Loki 方案 | structured metadata（Loki 3.x，官方，TODO-verify 版本） | — |
| schema 变更 | `schema_config` 按日期切换，旧数据不迁移（配置：`from: 2024-01-01`, v13） | mapping 已有字段不可改，需 reindex |
| 匹配语义 | 行过滤为字节/正则匹配，区分大小写 | `text` 字段按分词匹配，默认不区分大小写 → 同一"关键词"命中数可能不同（见 §4 Q2） |

**§3 资源**：

| 组件 | requests | limits | 实测内存 | 启动到就绪 | 镜像大小 | 来源 |
|------|----------|--------|----------|-----------|----------|------|
| Loki | 100m / 128Mi | 200m / 256Mi | `<T1>` MiB | `<T1>` s | `<T1>` | 配置 + 实测 |
| Promtail ×3 | 50m / 64Mi 每个 | 100m / 128Mi 每个 | `<T1>` MiB（合计） | — | `<T1>` | 配置 + 实测 |
| Elasticsearch | heap 512m | `mem_limit` 1536m | `⏳B` | `⏳B` | `⏳B` | compose + 实测 |
| Kibana | — | `mem_limit` 1g | `⏳B` | `⏳B` | `⏳B` | compose + 实测 |
| Filebeat | — | `mem_limit` 256m | `⏳B` | — | `⏳B` | compose + 实测 |

| 同一批日志（newman Job + 全部命名空间，24h 窗口） | Loki | Elastic |
|----------------------------------------------|------|---------|
| 日志行数 | `<T1: Q1 + 全量>` | `⏳B`（`_count`） |
| 磁盘 | `<T1: du>` KiB | `⏳B`（data stream `store_size`） |
| 字节 / 行 | 计算值 | `⏳B` |

注：资源表下加一行说明 —— 两栈不可同时运行（OrbStack 8 GiB），分两个时间窗测量，日志源为节点上同一批 `/var/log/pods` 文件。

**§4 查询**（同一需求对照；Loki 列填 Task 1 实测；KQL 在 Kibana Discover 时间范围 Last 24 hours 执行）：

| # | 需求 | LogQL | Loki 命中 / execTime | KQL（或 ES\|QL） | Elastic 命中 | 差异说明 |
|---|------|-------|----------------------|------------------|-------------|----------|
| Q1 | newman Job 全部日志 | `{namespace="qa-portfolio", container="newman"}` | `<T1>` | `k8s.namespace: "qa-portfolio" and k8s.container: "newman"` | `⏳B` | 同源应相等 |
| Q2 | newman 错误行 | `{namespace="qa-portfolio", container="newman"} \|~ "AssertionError\|FAILED\|Error:"` | `<T1>` | `k8s.namespace: "qa-portfolio" and k8s.container: "newman" and message: ("AssertionError" or "FAILED" or "Error:")` | `⏳B` | LogQL 正则区分大小写；KQL 分词后不区分大小写 |
| Q3 | 每 5 分钟错误数（按 pod） | `sum by (pod) (count_over_time({...} \|~ "..." [5m]))` | `<T1>` | KQL 只能过滤 → ES\|QL：`FROM filebeat-* \| WHERE k8s.container == "newman" AND message RLIKE ".*(AssertionError\|FAILED\|Error:).*" \| STATS c = COUNT(*) BY b = BUCKET(@timestamp, 5 minutes), k8s.pod` | `⏳B` | LogQL 内置指标化；Kibana 需 Lens / ES\|QL |
| Q4 | 全命名空间搜 `timeout` | `{namespace=~".+"} \|= "timeout"` | `<T1>` | `message: "timeout"` | `⏳B` | Loki 需扫全部 stream 的 chunk；ES 走倒排索引 |
| Q5 | 慢请求 > 500ms | `{namespace="qa-portfolio", container="newman"} \| regexp "(?P<ms>\\d+)ms" \| ms > 500` | `<T1>` | KQL 不能查询时抽取 → ES\|QL：`FROM filebeat-* \| WHERE k8s.container == "newman" \| GROK message "%{NUMBER:ms:int}ms" \| WHERE ms > 500 \| STATS c = COUNT(*)`；或 ingest pipeline / runtime field | `⏳B` | Loki 查询时解析（schema-on-read）；ES 推荐写入时解析 |

表下：LogQL 计数写法（`sum(count_over_time(<query>[24h]))`）与 Task 1 Step 5 命令（复制）；Elastic 计数命令见附录 A。

**§5 运维**：

| 项 | Loki | Elastic |
|----|------|---------|
| 保留 | compactor `retention_enabled` + `limits_config.retention_period`；**本项目未配置**（实测 `/config`：`<T1>`）→ 保留到 PVC 5Gi 满 | ILM 策略（rollover → delete）；Filebeat 默认 data stream 绑定 `filebeat` 策略（`⏳B`：`GET _ilm/policy/filebeat` 实际内容） |
| 扩容 | 读写组件水平扩，存储在对象存储 | 加节点 + shard rebalance；主 shard 数需预规划（不可直接改，需 split / reindex） |
| 备份 | 依赖对象存储自身（版本化 / 复制） | snapshot / restore 到快照仓库 |
| 升级 | 二进制无状态；schema 按日期滚动 | 滚动升级；跨主版本需索引兼容（官方升级文档） |
| 采集器 | Promtail EOL → 迁移 Alloy（本项目后续项） | Filebeat / Elastic Agent（Fleet） |
| 安全 | 本项目 `auth_enabled: false`；多租户靠 `X-Scope-OrgID` | 本项目演示关闭 `xpack.security`；**生产必须开启 TLS + 认证** |
| 本项目实测维护操作 | 安装 / 卸载各 1 条 helm 命令 | `elastic-up.sh` / `elastic-down.sh`（`⏳B`：启动耗时） |

**§6 成本**：

| 项 | Loki | Elastic | 来源 |
|----|------|---------|------|
| 存储放大 | 字节/行 `<T1 计算>` | `⏳B` | 实测 |
| 存储介质 | 对象存储（S3 / GCS / Azure Blob）为主 | 块存储为主；searchable snapshots 可用对象存储（订阅级别 TODO-verify） | 官方 |
| 计算 | 320Mi requests（本项目） | ≥ 2.75 GiB limits（本项目 compose） | 配置 |
| 许可证 — Loki | AGPLv3（生效版本 TODO-verify） | — | grafana.com/licensing/（访问 2026-11-27） |
| 许可证 — 采集器 | Promtail：随 Loki 仓库（TODO-verify）；Alloy：TODO-verify | Beats：FAQ 列为 Apache 2.0；默认二进制发行版许可 TODO-verify | elastic.co/pricing/faq/licensing（访问 2026-11-27） |
| 许可证 — ES / Kibana | — | 源码 SSPL 1.0 / AGPLv3 / ELv2 三选一；默认发行版 ELv2；AGPLv3 自 8.16 起（FAQ 2024-08-29 更新） | 同上 |
| 付费功能 | Grafana Cloud / Enterprise Logs（可选） | ML、高级安全等按订阅级别（TODO-verify，elastic.co/subscriptions） | 官方 |
| 说明 | 仅列条款与链接，不作法律结论；对外提供托管服务时需法务评估 AGPL 网络条款 / ELv2 限制 | | |

**§7 选型建议**：

| 场景 | 推荐 | 理由 |
|------|------|------|
| K8s 运维 / CI Job 日志（本项目） | Loki | 资源约为 Elastic 的 `<计算>`；与 Prometheus 标签、Grafana 统一 |
| 安全分析 / SIEM | Elastic | 全文 + 字段索引、Elastic Security |
| 业务日志全文检索、字段未知的排查 | Elastic | 倒排索引，Q4 类查询不扫全量 |
| 企业已运行 Elastic（如 SCOM / Elastic 环境） | Elastic | 复用现有集群与 ILM / 权限体系，Filebeat 接入即可 |
| 日志量大、查询少、成本敏感 | Loki + 对象存储 | 只索引标签，存储便宜 |
| 高基数字段过滤（trace_id、user_id） | Elastic；或 Loki structured metadata | 见 §2 |

**附录 A：可选 Elastic 演示**（路径 B 完成后补全；Task 2 先写结构与命令）：前置条件表（停 monitoring、无其他 e2e、镜像已拉、空闲 ≥ 3 GiB）、`preflight.sh` 检查项表、启动 / 验证 / 清理命令、live vs snapshot 模式说明、Task 4 spike 结果表（`⏳B`）。

**附录 B：参考**：每条链接带访问日期（Loki architecture / labels / retention / LogQL、Promtail EOL、Grafana licensing、Elastic licensing FAQ、ILM、KQL、ES|QL、single-node Docker、mapping limit）。

- [ ] **Step 2: 核实官方来源**：对附录 B 每个链接用 WebFetch 打开，确认引用的句子存在；能核实的 `TODO-verify` 改为事实 + 访问日期；不能核实的保留。
- [ ] **Step 3: 自检**

```bash
cd devops-platform-demo
f=docs/guides/LOGGING-LOKI-VS-ELASTIC.md
grep -n '<T1' "$f"                      # Expected: 无输出
grep -n -i 'TBD\|TODO[^-]\|interview\|面试' "$f"   # Expected: 无输出
grep -c '⏳B' "$f"                       # Expected: > 0（仅 Elastic 实测单元格）
grep -n 'TODO-verify' "$f"               # Expected: 仅 §2 / §6 许可证与订阅行
```
- [ ] **Step 4:** Commit `docs(dvp): add Loki vs Elastic logging comparison guide`

### Task 3: 登记文档

**Files:** Modify `devops-platform-demo/README.md`（Documentation 表，约第 489 行 `SECRETS-RBAC.md` 行之后）

- [ ] **Step 1:** 插入 `| [LOGGING-LOKI-VS-ELASTIC.md](docs/guides/LOGGING-LOKI-VS-ELASTIC.md) | Loki vs Elastic 日志栈对比（REQ-DVP-008） |`
- [ ] **Step 2:** 在 README 第 292 行日志链路（`Pod logs ──→ Promtail ──→ Loki ──→ Grafana log explorer`）下方加一行：`> 与 Elastic 的取舍见 [LOGGING-LOKI-VS-ELASTIC.md](docs/guides/LOGGING-LOKI-VS-ELASTIC.md)`
- [ ] **Step 3:** 链接检查：`grep -o '](docs/guides/LOGGING-LOKI-VS-ELASTIC.md)' README.md | wc -l` → `2`；`test -f docs/guides/LOGGING-LOKI-VS-ELASTIC.md`
- [ ] **Step 4:** Commit `docs(dvp): link logging comparison guide from README`

> **检查点：** 路径 A 完成 → 报告用户，确认是否执行路径 B。

---

## 路径 B — 可选 Elastic 演示

> **运行约束（Task 4、9）：** 运行前确认 REQ-009 / REQ-010 e2e 与 monitoring 栈均未运行；Task 5–8 只写代码与离线测试，不启动 Elastic。

### Task 4: Spike — 日志源可行性 + 镜像

**Files:** 无仓库改动；结果写入 `/tmp/dvp008-spike.txt`，Task 9 写入文档附录 A

- [ ] **Step 1: 节点 `/var/log` 是否为 Docker 卷**（rancher/k3s 镜像声明 `VOLUME /var/log`，需实测）

```bash
for n in server-0 agent-0 agent-1; do
  docker inspect -f '{{range .Mounts}}{{.Type}} {{.Name}} -> {{.Destination}}{{"\n"}}{{end}}' "k3d-portfolio-dev-$n" | grep ' -> /var/log$'
done | tee -a /tmp/dvp008-spike.txt
```
Expected: 3 行 `volume <64 位 hex> -> /var/log`。

- [ ] **Step 2: 从 k3d 外只读访问卷内 pod 日志**（用本地已有镜像，不拉新镜像）

```bash
vol="$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/var/log"}}{{.Name}}{{end}}{{end}}' k3d-portfolio-dev-agent-0)"
docker run --rm --user 0 -v "$vol:/k3d:ro" --entrypoint sh qa-newman-demo:latest -c \
  'ls /k3d/pods | head -5; f=$(find /k3d/pods -path "*/qa-portfolio_*" -name "*.log" | head -1); echo "$f"; head -c 300 "$f"' \
  | tee -a /tmp/dvp008-spike.txt
```
Expected: 目录名 `<namespace>_<pod>_<uid>`；文件路径 `/k3d/pods/qa-portfolio_<pod>_<uid>/<container>/0.log`；内容为 CRI 格式 `2026-11-..Z stdout F ...`。若 newman pod 不在 agent-0，换节点。

- [ ] **Step 3: 回退方案可用性**：`docker cp k3d-portfolio-dev-agent-0:/var/log/pods - | tar -tf - | head -5` → 输出 `pods/...` 条目。
- [ ] **Step 4: 决策**：Step 1+2 通过 → `live` 模式；否则 `snapshot` 模式（静态快照，文档注明"非实时"）。两者都失败 → 停下报告用户（再回退：`kubectl logs` 导出文件，需改 filebeat 路径与 dissect，另行评审）。
- [ ] **Step 5: 版本与镜像**

```bash
for img in elasticsearch/elasticsearch kibana/kibana beats/filebeat; do
  docker manifest inspect "docker.elastic.co/$img:9.5.7" >/dev/null && echo "ok $img:9.5.7"
done
docker run --rm --entrypoint cat qa-newman-demo:latest /proc/sys/vm/max_map_count
```
Expected: 3 行 `ok`（任一不存在 → 改用 9.5.x 最新补丁并全局替换 `9.5.7`）；同时查 discuss.elastic.co 安全公告确认所选版本为修复版。`max_map_count` 记录（单节点 discovery 不强制 bootstrap check，ES 启动日志再确认）。

- [ ] **Step 6: 后台拉取（不阻塞 Task 5–8）**：`time docker pull docker.elastic.co/elasticsearch/elasticsearch:9.5.7`（kibana、filebeat 同），完成后 `docker image ls --format '{{.Repository}}:{{.Tag}} {{.Size}}' | grep docker.elastic.co` 记录大小与耗时。
- [ ] **Step 7:** 无提交。

### Task 5: preflight 测试（先红）

**Files:** Create `devops-platform-demo/tests/test-elastic-preflight.sh`

- [ ] **Step 1: 写测试**

```bash
#!/usr/bin/env bash
# Offline tests for monitoring/elastic/preflight.sh using PATH stubs (REQ-DVP-008).
# Run: bash tests/test-elastic-preflight.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREFLIGHT="$ROOT/monitoring/elastic/preflight.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
passed=0
failed=0
mkdir -p "$WORK/bin"

cat > "$WORK/bin/docker" <<'EOF'
#!/usr/bin/env bash
case "$1" in
  info) exit "${STUB_DOCKER_INFO_RC:-0}" ;;
  compose) printf '%s\n' docker.elastic.co/elasticsearch/elasticsearch:9.5.7 \
             docker.elastic.co/kibana/kibana:9.5.7 docker.elastic.co/beats/filebeat:9.5.7 ;;
  image) case "$3" in *"${STUB_MISSING_IMAGE:-none}"*) exit 1 ;; esac ;;
  run) printf 'MemTotal: 8388608 kB\nMemAvailable: %s kB\n' "${STUB_VM_AVAIL_KB:-4194304}" ;;
  inspect)
    case "$*" in
      *State.Running*) echo "${STUB_NODE_RUNNING:-true}" ;;
      *) for last in "$@"; do :; done; echo "vol-$last" ;;
    esac ;;
  volume|cp) : ;;
  *) echo "stub docker: unexpected $*" >&2; exit 99 ;;
esac
EOF

cat > "$WORK/bin/kubectl" <<'EOF'
#!/usr/bin/env bash
case "$*" in
  *"get ns"*) exit 0 ;;
  *"get pods -n monitoring"*) [ -z "${STUB_MONITORING_PODS:-}" ] || echo "$STUB_MONITORING_PODS" ;;
  *) echo "stub kubectl: unexpected $*" >&2; exit 99 ;;
esac
EOF

cat > "$WORK/bin/uname" <<'EOF'
#!/usr/bin/env bash
echo "${STUB_UNAME:-Linux}"
EOF

cat > "$WORK/bin/sysctl" <<'EOF'
#!/usr/bin/env bash
echo 16384
EOF

cat > "$WORK/bin/vm_stat" <<'EOF'
#!/usr/bin/env bash
cat <<OUT
Mach Virtual Memory Statistics: (page size of 16384 bytes)
Pages free:                               ${STUB_HOST_FREE_PAGES:-200000}.
Pages active:                             500000.
Pages inactive:                           ${STUB_HOST_INACTIVE_PAGES:-50000}.
Pages speculative:                        1000.
OUT
EOF
chmod +x "$WORK/bin/"*

pf() { env PATH="$WORK/bin:$PATH" ELASTIC_ENV_FILE="$WORK/out.env" "$@"; }

expect() { # expect <name> <want_rc> <want_substring> <cmd...>
  local name="$1" want_rc="$2" want="$3" out rc=0
  shift 3
  rm -f "$WORK/out.env"
  out="$("$@" 2>&1)" || rc=$?
  if [ "$rc" = "$want_rc" ] && printf '%s' "$out" | grep -q -- "$want"; then
    echo "PASS $name"; passed=$((passed + 1))
  else
    echo "FAIL $name (rc=$rc, want $want_rc / '$want'): $out"; failed=$((failed + 1))
  fi
}

check() { # check <name> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "PASS $1"; passed=$((passed + 1))
  else
    echo "FAIL $1 (want '$2', got '$3')"; failed=$((failed + 1))
  fi
}

expect "live happy path"          0 "PREFLIGHT PASS"   pf bash "$PREFLIGHT" live
check  "env has 3 node volumes"   3 "$(grep -c '^K3D_LOG_VOL_' "$WORK/out.env")"
check  "server-0 volume resolved" "K3D_LOG_VOL_SERVER_0=vol-k3d-portfolio-dev-server-0" "$(grep '^K3D_LOG_VOL_SERVER_0=' "$WORK/out.env")"
expect "snapshot happy path"      0 "PREFLIGHT PASS"   pf bash "$PREFLIGHT" snapshot
check  "snapshot volume name"     "K3D_LOG_VOL_AGENT_1=qa-elastic-snap-agent-1" "$(grep '^K3D_LOG_VOL_AGENT_1=' "$WORK/out.env")"
expect "invalid mode"             1 "mode must be"     pf bash "$PREFLIGHT" bogus
expect "docker down"              1 "not reachable"    pf STUB_DOCKER_INFO_RC=1 bash "$PREFLIGHT" live
expect "image missing"            1 "not present"      pf STUB_MISSING_IMAGE=kibana bash "$PREFLIGHT" live
expect "monitoring stack running" 1 "monitoring"       pf STUB_MONITORING_PODS=pod/loki-0 bash "$PREFLIGHT" live
expect "VM memory too low"        1 "MemAvailable"     pf STUB_VM_AVAIL_KB=1048576 bash "$PREFLIGHT" live
check  "no env file on failure"   "absent" "$([ -e "$WORK/out.env" ] && echo present || echo absent)"
expect "macOS host memory low"    1 "host"             pf STUB_UNAME=Darwin STUB_HOST_FREE_PAGES=50000 STUB_HOST_INACTIVE_PAGES=10000 bash "$PREFLIGHT" live
expect "macOS host memory ok"     0 "PREFLIGHT PASS"   pf STUB_UNAME=Darwin bash "$PREFLIGHT" live
expect "k3d node stopped"         1 "not running"      pf STUB_NODE_RUNNING=false bash "$PREFLIGHT" live

echo "---"
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]
```

> 默认宿主值：(200000 + 50000 + 1000) × 16384 B = 3921 MiB ≥ 3072；低值用例 61000 页 = 953 MiB。

- [ ] **Step 2:** `bash tests/test-elastic-preflight.sh` → 全部 FAIL（preflight 不存在，`bash: ...preflight.sh: No such file`），最后 `passed=0`（`check` 行因 out.env 不存在也 FAIL），exit 1
- [ ] **Step 3:** `shellcheck tests/test-elastic-preflight.sh` 通过；用 `/bin/bash tests/test-elastic-preflight.sh` 再跑一次确认 bash 3.2 语法可解析
- [ ] **Step 4:** Commit `test(dvp): add offline tests for Elastic demo preflight`

### Task 6: preflight 脚本（变绿）

**Files:** Create `devops-platform-demo/monitoring/elastic/preflight.sh`

- [ ] **Step 1: 写脚本**

```bash
#!/usr/bin/env bash
# Preflight for the optional Elastic demo (REQ-DVP-008).
# Refuses to continue unless images are pulled, the monitoring stack is not running and
# enough memory is free in the Docker VM (and on the macOS host). Writes the k3d node
# /var/log volume names to monitoring/elastic/.env for docker-compose.elastic.yml.
# Usage: bash monitoring/elastic/preflight.sh [live|snapshot]
#   live     mount each k3d node's /var/log volume read-only (tails new lines)
#   snapshot copy /var/log/pods of each node into qa-elastic-snap-<node> volumes (static)
set -euo pipefail

MODE="${1:-live}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_FILE="$ROOT/docker-compose.elastic.yml"
ENV_FILE="${ELASTIC_ENV_FILE:-$ROOT/monitoring/elastic/.env}"
MIN_FREE_MIB="${ELASTIC_MIN_FREE_MIB:-3072}"
CLUSTER="${K3D_CLUSTER:-portfolio-dev}"
KUBE_CONTEXT="k3d-$CLUSTER"
NODES="server-0 agent-0 agent-1"
TMP_ENV="$ENV_FILE.tmp"
trap 'rm -f "$TMP_ENV"' EXIT

fail() { echo "PREFLIGHT FAIL: $*" >&2; exit 1; }
ok() { echo "ok   $*"; }

case "$MODE" in
  live|snapshot) ;;
  *) fail "mode must be live or snapshot (got '$MODE')" ;;
esac

# 1. Docker reachable
docker info >/dev/null 2>&1 || fail "docker daemon not reachable (start OrbStack)"
ok "docker reachable"

# 2. Images pulled beforehand (pulls are slow here; never pull implicitly)
images="$(docker compose -f "$COMPOSE_FILE" config --images)"
for img in $images; do
  docker image inspect "$img" >/dev/null 2>&1 \
    || fail "image $img not present; run: docker compose -f docker-compose.elastic.yml pull"
done
es_image="$(printf '%s\n' "$images" | grep '/elasticsearch/' | head -n 1)"
[ -n "$es_image" ] || fail "no elasticsearch image in $COMPOSE_FILE"
ok "images present"

# 3. Monitoring stack must not run alongside (OrbStack crashed under memory pressure before)
kubectl --context "$KUBE_CONTEXT" get ns >/dev/null || fail "cluster $KUBE_CONTEXT unreachable (k3d cluster start $CLUSTER)"
monitoring_pods="$(kubectl --context "$KUBE_CONTEXT" get pods -n monitoring --no-headers -o name)"
[ -z "$monitoring_pods" ] || fail "monitoring namespace has pods; uninstall loki/promtail/prometheus first"
ok "monitoring namespace empty"

# 4. Free memory inside the Docker VM (containers see the VM's /proc/meminfo)
vm_free_mib="$(docker run --rm --entrypoint cat "$es_image" /proc/meminfo \
  | awk '/^MemAvailable:/ {printf "%d", $2 / 1024}')"
[ -n "$vm_free_mib" ] || fail "could not read MemAvailable from the Docker VM"
[ "$vm_free_mib" -ge "$MIN_FREE_MIB" ] || fail "Docker VM MemAvailable ${vm_free_mib}MiB < ${MIN_FREE_MIB}MiB"
ok "Docker VM MemAvailable ${vm_free_mib}MiB"

# 5. macOS host: OrbStack grows the VM on demand, so the host must have room too
if [ "$(uname -s)" = "Darwin" ]; then
  page_size="$(sysctl -n hw.pagesize)"
  host_free_mib="$(vm_stat | awk -v ps="$page_size" \
    '/^Pages (free|inactive|speculative):/ {gsub(/\./, "", $NF); sum += $NF} END {printf "%d", sum * ps / 1048576}')"
  [ "$host_free_mib" -ge "$MIN_FREE_MIB" ] || fail "host free+inactive ${host_free_mib}MiB < ${MIN_FREE_MIB}MiB"
  ok "host free+inactive ${host_free_mib}MiB"
fi

# 6. Resolve (live) or create (snapshot) one log volume per k3d node
echo "# generated by monitoring/elastic/preflight.sh ($MODE); do not commit" > "$TMP_ENV"
for node in $NODES; do
  container="k3d-$CLUSTER-$node"
  [ "$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null)" = "true" ] \
    || fail "$container not running (k3d cluster start $CLUSTER)"
  if [ "$MODE" = "live" ]; then
    vol="$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/var/log"}}{{.Name}}{{end}}{{end}}' "$container")"
    [ -n "$vol" ] || fail "$container has no /var/log volume; rerun with: snapshot"
  else
    vol="qa-elastic-snap-$node"
    docker volume create "$vol" >/dev/null
    docker cp "$container:/var/log/pods" - \
      | docker run --rm -i --user 0 -v "$vol:/dst" --entrypoint tar "$es_image" -x -C /dst
  fi
  echo "K3D_LOG_VOL_$(echo "$node" | tr 'a-z-' 'A-Z_')=$vol" >> "$TMP_ENV"
done
mv "$TMP_ENV" "$ENV_FILE"
ok "wrote $ENV_FILE ($MODE)"
echo "PREFLIGHT PASS"
```

- [ ] **Step 2:** `bash tests/test-elastic-preflight.sh` → `passed=14 failed=0`；`/bin/bash tests/test-elastic-preflight.sh` 同样通过（bash 3.2）
- [ ] **Step 3:** `shellcheck monitoring/elastic/preflight.sh` 通过（无 `|| true`、无 `2>/dev/null` 吞掉的必需检查 —— 唯一的 `2>/dev/null` 在 `docker info` / `image inspect` / `State.Running`，其失败都转成 `fail`）
- [ ] **Step 4:** Commit `feat(dvp): add memory-aware preflight for Elastic demo`

### Task 7: compose + Filebeat 配置 + CI 语法检查

**Files:** Create `devops-platform-demo/docker-compose.elastic.yml`、`devops-platform-demo/monitoring/elastic/filebeat.yml`；Modify `.github/workflows/dvp-pr.yml`（Unit Tests job，`Alertmanager values tests` step 之后，约第 113 行）

- [ ] **Step 1: `docker-compose.elastic.yml`**

```yaml
# Optional Elastic demo (REQ-DVP-008): single-node Elasticsearch + Kibana + Filebeat.
# Reads the same k3d pod logs as Promtail (/var/log/pods inside the k3d node containers).
#
#   Start:  bash monitoring/elastic/elastic-up.sh [live|snapshot]   (runs preflight first)
#   Verify: bash monitoring/elastic/verify-elastic.sh
#   Stop:   bash monitoring/elastic/elastic-down.sh
#
# Local only: xpack.security is disabled; production must enable TLS + authentication.
# Never run together with the monitoring stack or other e2e demos (OrbStack 8 GiB limit).
# CI only runs `docker compose config`; Elastic is never started in CI.
# Keep all three image versions identical.

name: qa-portfolio-elastic

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.5.7
    container_name: qa-elastic-es
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - xpack.security.enabled=false
      - xpack.ml.enabled=false
      - cluster.routing.allocation.disk.threshold_enabled=false
    mem_limit: 1536m
    ports:
      - "127.0.0.1:9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -fs 'http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=5s' >/dev/null"]
      interval: 10s
      timeout: 10s
      retries: 30
      start_period: 30s

  kibana:
    image: docker.elastic.co/kibana/kibana:9.5.7
    container_name: qa-elastic-kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    mem_limit: 1g
    ports:
      - "127.0.0.1:5601:5601"
    depends_on:
      elasticsearch:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -fs http://localhost:5601/api/status | grep -q '\"level\":\"available\"'"]
      interval: 10s
      timeout: 10s
      retries: 30
      start_period: 60s

  filebeat:
    image: docker.elastic.co/beats/filebeat:9.5.7
    container_name: qa-elastic-filebeat
    user: root  # pod log files inside k3d nodes are root-owned
    command: ["filebeat", "-e", "--strict.perms=false"]
    mem_limit: 256m
    depends_on:
      elasticsearch:
        condition: service_healthy
    volumes:
      - ./monitoring/elastic/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - filebeat-data:/usr/share/filebeat/data
      - k3d-log-server-0:/k3d/server-0:ro
      - k3d-log-agent-0:/k3d/agent-0:ro
      - k3d-log-agent-1:/k3d/agent-1:ro

volumes:
  es-data:
  filebeat-data:
  # External: compose never creates or deletes these (down -v leaves k3d node volumes intact).
  # Names come from monitoring/elastic/.env written by preflight.sh.
  k3d-log-server-0:
    external: true
    name: ${K3D_LOG_VOL_SERVER_0:-run-elastic-preflight-first}
  k3d-log-agent-0:
    external: true
    name: ${K3D_LOG_VOL_AGENT_0:-run-elastic-preflight-first}
  k3d-log-agent-1:
    external: true
    name: ${K3D_LOG_VOL_AGENT_1:-run-elastic-preflight-first}
```

- [ ] **Step 2: `monitoring/elastic/filebeat.yml`**

```yaml
# Filebeat for the optional Elastic demo (REQ-DVP-008).
# Same source as Promtail: CRI log files under /var/log/pods of every k3d node,
# mounted read-only at /k3d/<node>/ by docker-compose.elastic.yml.
# Output: default data stream filebeat-<version>; Kibana data view filebeat-*.

filebeat.inputs:
  - type: filestream
    id: k3d-pod-logs
    paths:
      - /k3d/*/pods/*/*/*.log
    parsers:
      - container:
          stream: all
          format: cri

processors:
  # /k3d/<node>/pods/<namespace>_<pod>_<uid>/<container>/<n>.log -> k8s.* fields
  - dissect:
      field: log.file.path
      tokenizer: "/k3d/%{node}/pods/%{namespace}_%{pod}_%{pod_uid}/%{container}/%{?file}"
      target_prefix: k8s

output.elasticsearch:
  hosts: ["http://elasticsearch:9200"]

logging.level: info
```

- [ ] **Step 3: 语法检查（不需要 daemon）**

```bash
docker compose -f docker-compose.elastic.yml config --quiet && echo CONFIG_OK
docker compose -f docker-compose.elastic.yml config --images
```
Expected: `CONFIG_OK`；3 行 `docker.elastic.co/...:9.5.7`。Filebeat 配置在 Task 9 Step 2 用 `filebeat test config` 校验（需运行中的容器，属路径 B 运行）。

- [ ] **Step 4: CI step**（插入 Unit Tests job，工作目录默认为 `devops-platform-demo`）

```yaml
      - name: Elastic compose syntax check (never started in CI)
        run: docker compose -f docker-compose.elastic.yml config --quiet
```
- [ ] **Step 5:** `actionlint .github/workflows/dvp-pr.yml` 通过
- [ ] **Step 6:** Commit `feat(dvp): add optional single-node Elastic compose stack`

### Task 8: 启动 / 验证 / 清理脚本

**Files:** Create `devops-platform-demo/monitoring/elastic/elastic-up.sh`、`verify-elastic.sh`、`elastic-down.sh`

- [ ] **Step 1: `elastic-up.sh`**

```bash
#!/usr/bin/env bash
# Start the optional Elastic demo after preflight passes (REQ-DVP-008).
# Usage: bash monitoring/elastic/elastic-up.sh [live|snapshot]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
bash "$ROOT/monitoring/elastic/preflight.sh" "${1:-live}"
cd "$ROOT"
start="$(date +%s)"
docker compose --env-file monitoring/elastic/.env -f docker-compose.elastic.yml up -d --wait --wait-timeout 300
echo "Elastic stack healthy after $(( $(date +%s) - start ))s"
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}' \
  qa-elastic-es qa-elastic-kibana qa-elastic-filebeat
```

- [ ] **Step 2: `verify-elastic.sh`**

```bash
#!/usr/bin/env bash
# Verify the Elastic demo indexed the same pod logs as Loki (REQ-DVP-008).
# Usage: EXPECTED_NEWMAN_LINES=<Loki Q1 hits> bash monitoring/elastic/verify-elastic.sh
set -euo pipefail

ES="${ES_URL:-http://127.0.0.1:9200}"
KB="${KIBANA_URL:-http://127.0.0.1:5601}"
fail() { echo "FAIL $*" >&2; exit 1; }

status="$(curl -fsS "$ES/_cluster/health" | jq -r .status)"
case "$status" in
  green|yellow) echo "ok   cluster health $status" ;;
  *) fail "cluster health $status" ;;
esac

query='{"query":{"bool":{"filter":[{"match_phrase":{"k8s.namespace":"qa-portfolio"}},{"match_phrase":{"k8s.container":"newman"}}]}}}'
count=0
for _ in $(seq 1 24); do  # Filebeat may lag; poll up to 120s
  count="$(curl -fsS -H 'Content-Type: application/json' "$ES/filebeat-*/_count" -d "$query" | jq -r .count)"
  [ "$count" -gt 0 ] && break
  sleep 5
done
[ "$count" -gt 0 ] || fail "no newman log lines indexed"
echo "ok   newman lines indexed: $count"
if [ -n "${EXPECTED_NEWMAN_LINES:-}" ]; then
  [ "$count" -eq "$EXPECTED_NEWMAN_LINES" ] || fail "newman lines $count != Loki $EXPECTED_NEWMAN_LINES"
  echo "ok   matches Loki Q1 ($EXPECTED_NEWMAN_LINES)"
fi

curl -fsS -X POST "$KB/api/data_views/data_view" -H 'kbn-xsrf: true' -H 'Content-Type: application/json' \
  -d '{"data_view":{"title":"filebeat-*","name":"k3d pod logs","timeFieldName":"@timestamp"},"override":true}' >/dev/null
echo "ok   Kibana data view filebeat-* -> $KB/app/discover"

echo "--- measurements"
curl -fsS "$ES/_data_stream/filebeat-*/_stats?human" | jq -c '.data_streams[] | {data_stream, store_size}'
curl -fsS -H 'Content-Type: application/json' "$ES/filebeat-*/_count" | jq -c '{all_docs: .count}'
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}' qa-elastic-es qa-elastic-kibana qa-elastic-filebeat
```

- [ ] **Step 3: `elastic-down.sh`**

```bash
#!/usr/bin/env bash
# Stop the Elastic demo and delete its data (REQ-DVP-008).
# k3d node /var/log volumes are external and are not touched.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
env_file=monitoring/elastic/.env
if [ -f "$env_file" ]; then
  docker compose --env-file "$env_file" -f docker-compose.elastic.yml down -v
else
  docker compose -f docker-compose.elastic.yml down -v
fi
for vol in $(docker volume ls -q --filter name=qa-elastic-snap-); do
  docker volume rm "$vol"
done
rm -f "$env_file"
left="$(docker ps -a -q --filter name=qa-elastic-)"
[ -z "$left" ] || { echo "FAIL containers left: $left" >&2; exit 1; }
echo "Elastic demo removed"
```

- [ ] **Step 4:** `shellcheck monitoring/elastic/*.sh` 通过；`/bin/bash -n monitoring/elastic/*.sh`（bash 3.2 语法）通过
- [ ] **Step 5:** Commit `feat(dvp): add start, verify and cleanup scripts for Elastic demo`

### Task 9: 运行演示并回填 `⏳B`

**Files:** Modify `devops-platform-demo/docs/guides/LOGGING-LOKI-VS-ELASTIC.md`

> 前置：Task 1 newman pod 仍在（`kubectl --context k3d-portfolio-dev get pods -n qa-portfolio`，24h TTL 内）；monitoring 命名空间不存在；REQ-009 / REQ-010 e2e 未运行；Task 4 镜像已拉完。

- [ ] **Step 1: 启动**：`bash monitoring/elastic/elastic-up.sh live`（Task 4 决策为 snapshot 时传 `snapshot`）→ `PREFLIGHT PASS`，`Elastic stack healthy after <N>s`，stats 表。preflight 失败 → 按提示处理，**不得**降低 `ELASTIC_MIN_FREE_MIB` 绕过（如需调整阈值先问用户）。
- [ ] **Step 2: Filebeat 自检**：`docker exec qa-elastic-filebeat filebeat test config --strict.perms=false` → `Config OK`；`docker exec qa-elastic-filebeat filebeat test output --strict.perms=false` → 末行 `talk to server... OK`
- [ ] **Step 3: 验证**：`EXPECTED_NEWMAN_LINES=<Task 1 Q1 hits> bash monitoring/elastic/verify-elastic.sh` → 全部 `ok`；若计数不等，记录差值与原因（例如 kubelet 日志轮转），不要放宽断言后提交 —— 先报告用户。
- [ ] **Step 4: 字段与 ILM**

```bash
curl -fsS 'http://127.0.0.1:9200/filebeat-*/_mapping/field/k8s.*' | jq -c '[.[] | .mappings | to_entries[] | {(.key): (.value.mapping | to_entries[0].value.type)}]'
curl -fsS 'http://127.0.0.1:9200/filebeat-*/_mapping' | jq '[.. | objects | select(has("type"))] | length'
curl -fsS 'http://127.0.0.1:9200/_ilm/policy/filebeat' | jq -c '.[].policy.phases'
```
Expected: `k8s.namespace` / `k8s.container` / `k8s.pod` 存在（记录类型 keyword/text）；字段总数；默认 ILM phases。

- [ ] **Step 5: ES|QL 查询（Q1 / Q3 / Q5 计数）**

```bash
esql() { jq -n --arg q "$1" '{query: $q}' | curl -fsS -X POST 'http://127.0.0.1:9200/_query?format=txt' -H 'Content-Type: application/json' -d @-; }
esql 'FROM filebeat-* | WHERE k8s.namespace == "qa-portfolio" AND k8s.container == "newman" | STATS c = COUNT(*)'
esql 'FROM filebeat-* | WHERE k8s.container == "newman" AND message RLIKE ".*(AssertionError|FAILED|Error:).*" | STATS c = COUNT(*) BY b = BUCKET(@timestamp, 5 minutes), k8s.pod'
esql 'FROM filebeat-* | WHERE k8s.container == "newman" | GROK message "%{NUMBER:ms:int}ms" | WHERE ms > 500 | STATS c = COUNT(*)'
```
Expected: 每条返回表格；Q1 与 Step 3 计数一致。语法错误 → 修正后同步到文档 §4。

- [ ] **Step 6: KQL（Kibana Discover，手动）**：打开 `http://127.0.0.1:5601/app/discover`，data view `k3d pod logs`，时间 Last 24 hours，依次执行 §4 的 Q1 / Q2 / Q4 KQL，记录命中数（Q2 记录与 LogQL 的差值并解释大小写 / 分词）。
- [ ] **Step 7: 回填文档**：§0 / §2 / §3 / §4 / §5 / §6 全部 `⏳B`，附录 A 补 spike 结果表（Task 4：卷检查、模式、镜像大小、拉取耗时、`max_map_count`）与实际启动耗时；§7 中"资源约为 Elastic 的 X"填计算值。`grep -c '⏳B' docs/guides/LOGGING-LOKI-VS-ELASTIC.md` → `0`。
- [ ] **Step 8: 清理**

```bash
bash monitoring/elastic/elastic-down.sh
helm --kube-context k3d-portfolio-dev uninstall dvp008-logs -n qa-portfolio
docker volume ls --filter name=qa-portfolio-elastic   # Expected: 无输出
docker inspect -f '{{.State.Running}}' k3d-portfolio-dev-agent-0   # Expected: true（节点卷未受影响）
```
- [ ] **Step 9:** Commit `docs(dvp): record Elastic demo measurements for REQ-DVP-008`

---

## 需求追踪

| 需求 / 验收 | 任务 |
|-------------|------|
| FR-DVP-008-01 架构、索引模型、资源、成本、LogQL vs KQL | Task 1（Loki 实测）、Task 2（§1–§7）、Task 3（登记） |
| FR-DVP-008-02 compose 单节点 ES + Filebeat + Kibana，同源日志，不进 k3d，不与监控栈并行 | Task 4（同源 spike）、Task 5–6（preflight 拒绝并行 / 内存不足）、Task 7（compose）、Task 8（脚本）、Task 9（运行） |
| 验收：对比文档完成 | Task 2 Step 3 自检 + Task 9 Step 7（`⏳B` = 0）或"未执行"替换 |
| 验收（可选）：Kibana 检索到与 Loki 相同的日志 | Task 9 Step 3（`EXPECTED_NEWMAN_LINES` 相等）+ Step 6（Discover） |
| 设计 §6：LogQL 在本项目 Loki 可执行 | Task 1 Step 5 |
| 设计 §6：`_cluster/health` green / yellow | Task 8 `verify-elastic.sh` |
| 设计 §6：CI 只做语法校验 | Task 7 Step 4 |
| 设计 §7：内存不足退出、许可证来源注明日期 | Task 6、Task 2 Step 2 |

## Stage 4 / 5

| 阶段 | 内容 |
|------|------|
| 4 | `bash tests/test-elastic-preflight.sh`（本机 bash 5 + `/bin/bash` 3.2）、shellcheck、actionlint、`docker compose -f docker-compose.elastic.yml config --quiet`；push `feature/dvp-008-logging-comparison`，开 PR，CI 全绿 |
| 5 | REQUIREMENTS 总表与章节状态（设计"待评审"→ 已评审，Done）、勾验收、`docs/REQUIREMENTS-INDEX.md:102` 状态、更新 memory；后续项登记：Promtail → Alloy 迁移、仪表盘 `job_name` 查询修复、Loki retention 配置 |
