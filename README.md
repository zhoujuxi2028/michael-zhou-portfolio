# Cloud-Monitoring-Platform

众成通信云监控系统平台

## 项目概述

物联网监控系统项目集合，服务于信号覆盖设备监控、管理和运维。

**合作方**: majiang、toyou

## 技术栈

- **后端**: Spring Boot 2.1.5 + Java 8 + MyBatis + MySQL
- **前端**: Vue.js 2.6 + iView UI 3.5 + ECharts
- **移动端**: uni-app (众成云APP)
- **工作流**: Flowable 6.4.1
- **地图**: 百度地图API

## 项目结构

```
├── majiang/                           # 核心项目目录
│   ├── 信号覆盖主机内置式设备监控系统软件开发/  # 主要开发项目
│   │   └── 源代码/sourcecode/
│   │       ├── app/JSZCAPP/              # uni-app移动端
│   │       ├── front/iview-Iot/          # Vue.js前端
│   │       ├── plat/springboot/          # Spring Boot后端
│   │       ├── sql/                     # 数据库脚本
│   │       └── deploy/                  # 部署文件
│   └── 众成验收/                         # 验收系统集合(4个子系统)
├── toyou/                            # toyou合作项目
├── protoType/                        # 原型开发
├── reference/                        # 参考文档和AEP SDK
└── docs/                             # 项目文档
```

## 核心功能

- 设备实时监控与告警管理
- GIS地图可视化展示
- 工作流程管理(BPMN 2.0)
- 微信公众号集成
- WebSocket实时通信
- AEP平台集成(中国电信)

## 快速开始

```bash
# 进入主开发项目
cd majiang/信号覆盖主机内置式设备监控系统软件开发/源代码/sourcecode/

# 后端开发
cd plat/springboot/
./mvnw spring-boot:run

# 前端开发
cd front/iview-Iot/
npm install && npm run dev

# 移动端开发 (HBuilderX IDE)
cd app/JSZCAPP/
```

## 验收系统

位于 `majiang/众成验收/` 目录，包含4个子系统：
- 物联网云监控平台
- 信号覆盖设备监控系统
- 设备安装租赁管理系统
- 5G兼容需求

## AEP平台集成

中国电信应用使能平台集成：
- **SDK位置**: `reference/267848_sdk/`
- **域名**: `*.api.ctwing.cn`

## 环境要求

- **Java**: JDK 8+
- **Node.js**: 14.0+
- **MySQL**: 5.7+
