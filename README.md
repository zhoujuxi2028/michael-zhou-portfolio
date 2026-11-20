# Michael Zhou | Developer Portfolio

**有测试架构背景的开发者：写工具链、做交付、懂质量。**

本仓库以可运行、可验证的工程演示为主，不代表生产环境服务。QA / 测试类项目见 [michael-zhou-qa-portfolio](https://github.com/zhoujuxi2028/michael-zhou-qa-portfolio)。

## 招聘方速查（按岗位）

| 目标岗位 | 先看这个 | 关键证据 |
|---|---|---|
| 数据 / 工具链开发 | [EDI & Reporting Demo](./edi-reporting-demo/) | Python 分层解析器（parser / schema / validator）、配置驱动校验、CLI、pytest 契约测试、Jasper 报表 |
| DevOps / SRE / 平台工程 | [DevOps Platform Demo](./devops-platform-demo/) | GitHub Actions PR 门禁、Helm + SBOM、Kubernetes、Terraform、ArgoCD、Trivy、Prometheus / Grafana |
| 全栈 / Web | [Cloud Monitoring Platform](./cloud-monitoring-platform/) | Spring Boot + MyBatis、Vue + ECharts、uni-app 移动端 |
| Android / 移动端 | [Android Compose Demo](./android-testing-demo-compose/) | Kotlin、Jetpack Compose、MVVM + StateFlow、Room、UI 测试 |

## 项目（按方向）

### 数据与工具链

| 项目 | 技术栈 | 展示重点 |
|---|---|---|
| [EDI & Reporting Demo](./edi-reporting-demo/) | Python（标准库）、pytest、Jasper Reports、SQLite | EDI 报文解析分层架构、schema 驱动校验与错误分级、黄金样本 / 坏数据样本、报表分组聚合 |

### DevOps / 平台工程

[DevOps Platform Demo](./devops-platform-demo/) · [架构说明](./devops-platform-demo/docs/architecture/ARCHITECTURE.md) · [快速开始](./devops-platform-demo/docs/guides/QUICKSTART.md) · [CI/CD 工作流](./.github/workflows/)

通过一个可本地运行的演示环境，串联代码校验、容器化测试、基础设施配置、Kubernetes 部署、GitOps 与监控。

| 工程实践 | 项目中的实现 |
|---|---|
| CI/CD | GitHub Actions PR 校验、部署流水线及 Terraform 校验 |
| 测试自动化 | Cypress E2E、Newman API 测试、ESLint |
| 容器与编排 | Docker Compose、Kubernetes、Helm |
| IaC 与 GitOps | Terraform（LocalStack 演示配置）、ArgoCD 应用配置 |
| DevSecOps | GitHub Actions 中的 Trivy 与 `npm audit` 检查 |
| 可观测性 | Prometheus、Grafana 仪表盘与告警规则 |

**部署边界：** GitHub Actions 部署工作流当前使用 `helm template` 和 `kubectl --dry-run` 演示发布门禁；它不是连接真实云端集群的生产部署。Kubernetes、ArgoCD 和监控组件提供本地演示配置。

```bash
cd devops-platform-demo
npm ci
npm run lint
npm test
```

完整本地环境与集群步骤见[快速开始指南](./devops-platform-demo/docs/guides/QUICKSTART.md)。Docker、k3d 等环境依赖按指南准备。

### 全栈 / 移动端

| 项目 | 技术栈 | 展示重点 |
|---|---|---|
| [Cloud Monitoring Platform](./cloud-monitoring-platform/) | Spring Boot、MyBatis、MySQL、Vue.js、ECharts、uni-app、Flowable | IoT 设备监控、管理与运维平台原型 |
| [Android Compose Demo](./android-testing-demo-compose/) | Kotlin、Jetpack Compose、MVVM + StateFlow、Room | Login + Todo 应用与 Compose UI 测试 |
| [Android View Demo](./android-testing-demo-view/) | Kotlin、XML Layout、MVVM + LiveData、Room | Login + Todo 应用与 Espresso 测试 |

### AI 辅助开发

| 项目 | 展示重点 |
|---|---|
| [Copilot CLI 学习中心](./docs/copilot-cli-journey/) | GitHub Copilot CLI 辅助开发的学习笔记与实践示例 |
| [Claude API 学习](./docs/claude-api-journey/) | Claude API 学习计划与示例代码 |

## 仓库结构

- `edi-reporting-demo/` — 数据与工具链
- `devops-platform-demo/`、`.github/workflows/` — DevOps / 平台工程
- `cloud-monitoring-platform/`、`android-testing-demo-*/` — 全栈 / 移动端
- `docs/` — 需求索引、最佳实践与学习资料
