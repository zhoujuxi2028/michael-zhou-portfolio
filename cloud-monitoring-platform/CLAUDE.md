# CLAUDE.md - Cloud Monitoring Platform

This file provides guidance to Claude Code when working within the `cloud-monitoring-platform/` project.

## 项目概述

Vendor C物联网监控系统项目集合，包含主要开发项目和验收系统：

1. **信号覆盖主机内置式设备监控系统软件开发** - 主要开发项目
2. **Vendor C验收** - 4个相关子系统的验收版本

**合作方**：Vendor A、Vendor B

## 系统架构

### 技术栈
- **后端**: Spring Boot 2.1.5 + Java 8 + MyBatis + MySQL + Druid连接池
- **前端Web**: Vue.js 2.6 + iView UI 3.5 + Vue Router + Vuex + ECharts
- **移动端**: uni-app (Vendor C云APP，支持iOS/Android/微信小程序)
- **Web服务器**: Nginx 1.18.0
- **工作流引擎**: Flowable 6.4.1
- **地图服务**: 百度地图API

### 主要功能模块
- 设备实时监控与告警管理
- GIS地图可视化展示
- 工作流程管理(BPMN 2.0)
- 微信公众号集成
- WebSocket实时通信
- 多租户权限管理

## 项目结构

```
cloud-monitoring-platform/
├── prototype/                         # 原型开发
│   ├── poc/                           # 概念验证
│   │   ├── simple-product-status-query/
│   │   ├── telecom-api-validation/
│   │   ├── iot-query-validation/
│   │   └── backend-validation/
│   ├── aep-integration/               # AEP平台集成
│   │   ├── phase1-query/
│   │   ├── phase1.1-export/
│   │   └── phase2-registration/
│   └── docs/                          # 原型文档
├── reference/                         # 参考文档和AEP SDK
│   ├── 267848_sdk/                    # AEP SDK
│   ├── analysis/                      # 分析报告
│   ├── data/                          # 数据文件
│   └── reports/                       # 报告
└── docs/                              # 项目文档
```

## 开发命令

### 原型 POC
```bash
cd cloud-monitoring-platform/prototype/poc/simple-product-status-query/
mvn spring-boot:run

cd cloud-monitoring-platform/prototype/poc/telecom-api-validation/
./run.sh
```

### AEP集成开发
```bash
cd cloud-monitoring-platform/reference/267848_sdk/demo/
source export_env.sh
./run_query.sh
```

## 核心技术配置

### 后端关键依赖
- **Flowable** 6.4.1: 工作流引擎
- **JWT** 3.4.0: 身份认证
- **HuTool** 5.2.5: Java工具库
- **FastJSON** 1.2.5: JSON处理
- **Apache POI** 4.1.0: Excel处理
- **微信公众号SDK** 3.7.0: weixin-java-mp

### 前端关键组件
- **iView UI** 3.5: 主要UI组件库
- **ECharts** 4.7.0: 数据可视化
- **Vue-Baidu-Map** 0.21.22: 地图集成
- **BPMN.js** 6.4.2: 工作流编辑器
- **XLSX** 0.16.1: Excel处理

## AEP平台集成

**AEP (Application Enablement Platform)** - 中国电信应用使能平台
- **域名**: `*.api.ctwing.cn`
- **SDK位置**: `reference/267848_sdk/`

### 核心服务
- 产品管理、设备管理、数据查询、远程控制
- 固件管理、边缘网关、物模型管理、消息订阅

### 认证配置
```bash
export AEP_APP_ID="267848"
export AEP_APP_KEY="Your_App_Key"
export AEP_APP_SECRET="Your_App_Secret"
export AEP_API_HOST="10433748.api.ctwing.cn"
```

### 文档位置
- API文档: `reference/267848_sdk/doc/`
- SDK示例: `reference/267848_sdk/demo/`
- 学习笔记: `reference/AEP_学习笔记.md`

## Vendor C验收4个子系统
1. **物联网云监控平台**: 通用监控框架
2. **信号覆盖主机内置式设备监控系统**: 信号覆盖设备监控
3. **信号覆盖设备安装及租赁系统**: 设备安装租赁管理
4. **5G兼容需求**: 5G兼容功能
