# M12: 性能/稳定性测试集成

## 概览

性能测试和稳定性测试是保障系统上线后稳定运行的关键，但相关脚本的编写门槛较高。本模块专注于**用 Copilot CLI 快速生成 k6 和 JMeter 测试脚本**，以及如何分析性能测试结果并提出优化建议。通过本模块，你将学会为已有系统快速补充性能测试覆盖。

---

## 核心概念 (理论 ~ 30-40%)

### 概念 1: 性能测试的核心指标

在编写性能测试脚本之前，需要明确目标指标：

| 指标 | 含义 | 典型基准 |
|------|------|---------|
| **TPS / RPS** | 每秒事务数 / 请求数 | 根据业务峰值 × 2 设计 |
| **P95 响应时间** | 95% 请求的响应时间上限 | 通常 < 500ms |
| **P99 响应时间** | 99% 请求的响应时间上限 | 通常 < 1000ms |
| **错误率** | 失败请求比例 | 通常 < 1% |
| **并发用户数** | 同时在线用户数 | 根据 DAU 估算 |

### 概念 2: 性能测试工具选型

| 工具 | 语言 | 特点 | 适用场景 |
|------|------|------|---------|
| **k6** | JavaScript | 现代化，代码即配置，CI 友好 | 新项目，DevOps 团队 |
| **JMeter** | Java/GUI | 成熟稳定，生态丰富 | 企业环境，已有 JMeter 基础 |
| **Locust** | Python | Python 原生，易于扩展 | Python 团队，复杂场景 |
| **Artillery** | JavaScript | 简单易用，YAML 配置 | 快速验证，轻量场景 |

**Copilot 生成质量**：k6（⭐⭐⭐⭐⭐）> Locust（⭐⭐⭐⭐）> Artillery（⭐⭐⭐⭐）> JMeter（⭐⭐⭐）

### 概念 3: 性能测试场景类型

```
负载测试（Load Test）
  → 验证系统在预期负载下的表现
  → 典型：持续 10 分钟，100 并发用户

压力测试（Stress Test）
  → 找到系统的临界点
  → 典型：逐步增加用户数到系统崩溃

稳定性测试（Soak/Endurance Test）
  → 验证系统长期运行的稳定性
  → 典型：持续 24 小时，正常负载

峰值测试（Spike Test）
  → 验证系统对突然流量激增的处理
  → 典型：瞬间从 10 用户升到 1000 用户
```

---

## 实战应用 (70% 以上)

### 场景 1: 用 Copilot 生成 k6 负载测试脚本

**问题描述**

你需要为一个订单创建 API 编写 k6 负载测试，要求在 100 个并发用户下，P95 响应时间 < 500ms，错误率 < 1%。

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
请生成一个 k6 负载测试脚本，测试以下 API：

POST http://api.example.com/orders
Headers: Authorization: Bearer ${TOKEN}
Body: { "product_id": 1, "quantity": 2 }
预期响应: 201, { "order_id": ..., "status": "pending" }

测试场景：
- 逐渐增加到 100 个并发用户（爬坡期 30 秒）
- 持续 5 分钟稳定压测
- 逐渐减少到 0（降坡期 30 秒）

性能目标（threshold）：
- http_req_duration P95 < 500ms
- http_req_failed < 1%
- http_reqs > 50 req/s

要求：
- TOKEN 从环境变量读取：__ENV.API_TOKEN
- 每次请求使用随机 product_id（1-100）
- 记录失败请求的详情
- 添加合理的注释说明每部分的作用
EOF
```

**生成的 k6 脚本示例**

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 100 }, // 爬坡
    { duration: '5m', target: 100 },  // 稳定
    { duration: '30s', target: 0 },   // 降坡
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.01'],
    http_reqs: ['rate>50'],
  },
};

export default function () {
  const productId = Math.floor(Math.random() * 100) + 1;
  const payload = JSON.stringify({ product_id: productId, quantity: 2 });
  
  const res = http.post('http://api.example.com/orders', payload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${__ENV.API_TOKEN}`,
    },
  });
  
  const success = check(res, {
    'status is 201': (r) => r.status === 201,
    'has order_id': (r) => r.json('order_id') !== undefined,
  });
  
  errorRate.add(!success);
  sleep(1);
}
```

**运行验证**

```bash
k6 run --env API_TOKEN=your-token scripts/load-test.js
```

**常见陷阱与对策**

- ❌ 陷阱 1：生成的脚本所有请求使用相同的测试数据，导致数据库缓存影响结果
  - ✅ 对策：要求"每次请求使用随机生成的测试数据"

---

### 场景 2: 分析性能测试结果

**问题描述**

k6 测试完成后，输出了大量指标数据，你需要快速判断哪里是瓶颈，并给出优化建议。

**Copilot CLI 解决方案**

```bash
# 把 k6 的 JSON 输出传给 Copilot 分析
k6 run --out json=/tmp/k6-results.json scripts/load-test.js

cat /tmp/k6-results.json | jq '.metrics | {
  p95: .http_req_duration.values["p(95)"],
  p99: .http_req_duration.values["p(99)"],
  rps: .http_reqs.values.rate,
  errors: .http_req_failed.values.rate
}' | gh copilot suggest "以上是 k6 性能测试结果，分析：
1. 是否达到了性能目标（P95 < 500ms, 错误率 < 1%）
2. 瓶颈在哪里（响应时间分布、错误类型）
3. 给出 3 个具体的优化建议（从最可能有效到最复杂排序）"
```

---

### 场景 3: 生成稳定性测试（Soak Test）脚本

**问题描述**

系统上线前，需要运行 24 小时稳定性测试，验证不存在内存泄漏或性能退化。

**Copilot CLI 解决方案**

```bash
cat << 'EOF' | gh copilot suggest
请生成一个 k6 稳定性测试脚本（Soak Test），要求：

测试场景：24 小时，30 个稳定并发用户
API：GET /api/products?page={1-10}&size=20

监控项：
- 每小时记录一次 P95 响应时间（检测性能退化）
- 记录错误率变化趋势
- 内存相关的指标（如果可以获取）

告警条件：
- P95 > 1000ms 时打印警告日志
- 错误率 > 5% 时中止测试

在注释中说明为什么 soak test 要用较低的并发数（30 而非 100）
EOF
```

---

## 最佳实践速查表

| 任务 | 关键配置 | 注意事项 |
|------|---------|---------|
| 负载测试 | stages + thresholds | 设置合理的爬坡时间 |
| 压力测试 | 持续增加并发直到失败 | 注意系统恢复时间 |
| 稳定性测试 | 低并发 + 长时间 | 监控内存和错误率趋势 |
| 峰值测试 | 瞬间增加到最大值 | 观察系统是否能自动恢复 |

---

## 常见错误与调试

| 问题 | 症状 | 原因 | 解决方案 |
|------|------|------|---------|
| 测试环境影响结果 | 本地和 CI 结果差异大 | 环境配置不同 | 在 CI 中使用固定规格的运行环境 |
| 共享数据污染 | 并发写入导致错误 | 测试数据没有隔离 | 每个 VU 使用独立的测试用户 |
| 爬坡太快系统崩溃 | 测试开始后立刻报错 | 爬坡时间太短 | 增加爬坡期至少 60 秒 |

---

## 与其他模块的关系

- **前置模块**：M10（API 测试 — 性能测试是 API 测试的延伸）、M8（工作流集成 — 性能测试 CI 集成）
- **相关模块**：M9（调试 — 分析性能测试失败和异常）
- **后续模块**：M13（Copilot Workspace 探索）

---

## 进阶延伸

- **Grafana + InfluxDB 可视化**：把 k6 结果输出到 InfluxDB，用 Grafana 实时监控（让 Copilot 帮你写配置）
- **分布式性能测试**：使用 k6 Cloud 或 k6 Operator（Kubernetes）分布式运行，让 Copilot 生成配置
- **混沌工程**：结合 Chaos Monkey，在性能测试时注入故障，用 Copilot 生成混沌场景脚本

---

## 参考资源

- [k6 官方文档](https://k6.io/docs/)
- [k6 Cloud 分布式测试](https://k6.io/cloud/)
- [Apache JMeter 文档](https://jmeter.apache.org/usermanual/index.html)
- [Locust 官方文档](https://locust.io/)

---

## 反思与迭代

完成本模块学习后，请记录：

- ✅ **学到的最有用的技巧**：Copilot 生成的性能测试脚本是否节省了大量时间？
- 🤔 **遇到的主要困难**：测试数据隔离和环境一致性是否是瓶颈？
- 💡 **改进的空间**：如何把性能测试集成到 release 流程中，让每次上线前自动验证？

---

**下一步**：[Phase 5 - 进阶与扩展 — M13: Copilot Workspace 探索](../phase5/M13-copilot-workspace.md)

*最后更新：2026-04-28*
