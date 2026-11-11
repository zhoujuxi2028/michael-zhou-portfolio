# Michael Zhou | DevOps / SRE Portfolio

求职方向：**DevOps Engineer / Site Reliability Engineer (SRE)**

本仓库以可运行、可验证的工程演示为主，重点展示 CI/CD、Kubernetes、Infrastructure as Code、GitOps、DevSecOps 与可观测性实践。

## 旗舰项目：DevOps Infrastructure Platform

[进入项目](./devops-platform-demo/) · [架构说明](./devops-platform-demo/docs/architecture/ARCHITECTURE.md) · [快速开始](./devops-platform-demo/docs/guides/QUICKSTART.md) · [CI/CD 工作流](./.github/workflows/)

通过一个可本地运行的演示环境，串联代码校验、容器化测试、基础设施配置、Kubernetes 部署、GitOps 与监控。项目重点是展示工程流程和技术决策，不代表生产环境服务。

| 工程实践 | 项目中的实现 |
|---|---|
| CI/CD | GitHub Actions PR 校验、部署流水线及 Terraform 校验 |
| 测试自动化 | Cypress E2E、Newman API 测试、ESLint |
| 容器与编排 | Docker Compose、Kubernetes、Helm |
| IaC 与 GitOps | Terraform（LocalStack 演示配置）、ArgoCD 应用配置 |
| DevSecOps | GitHub Actions 中的 Trivy 与 `npm audit` 检查 |
| 可观测性 | Prometheus、Grafana 仪表盘与告警规则 |

**部署边界：** GitHub Actions 部署工作流当前使用 `helm template` 和 `kubectl --dry-run` 演示发布门禁；它不是连接真实云端集群的生产部署。Kubernetes、ArgoCD 和监控组件提供本地演示配置。

### 快速验证

```bash
cd devops-platform-demo
npm ci
npm run lint
npm test
```

完整本地环境与集群步骤见[快速开始指南](./devops-platform-demo/docs/guides/QUICKSTART.md)。Docker、k3d 等环境依赖按指南准备。

## 其他项目

| 项目 | 展示重点 |
|---|---|
| [Cloud Monitoring Platform](./cloud-monitoring-platform/) | IoT 云监控与平台集成原型 |
| [EDI & Reporting Demo](./edi-reporting-demo/) | Python EDI 解析、schema 驱动校验与自动化测试 |
| [Android Compose Testing Demo](./android-testing-demo-compose/) | Kotlin、Jetpack Compose、Room 与 UI 自动化测试 |
| [Android View Testing Demo](./android-testing-demo-view/) | Kotlin、XML UI、Room 与 Espresso 测试 |
| [Copilot CLI 学习中心](./docs/copilot-cli-journey/) | AI 辅助开发的学习笔记与实践示例 |

## 仓库结构

- `devops-platform-demo/` — 求职主项目：CI/CD、容器、Kubernetes、IaC、GitOps、DevSecOps、监控
- `cloud-monitoring-platform/`、`edi-reporting-demo/`、`android-*` — 补充项目
- `docs/` — 项目文档与学习资料
