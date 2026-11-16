# Cloud Monitoring Platform

IoT cloud monitoring system for signal coverage device monitoring, management, and operations.

## Tech Stack

- **Backend**: Spring Boot 2.1.5 + Java 8 + MyBatis + MySQL
- **Frontend**: Vue.js 2.6 + iView UI 3.5 + ECharts
- **Mobile**: uni-app (iOS/Android/WeChat Mini Program)
- **Workflow**: Flowable 6.4.1
- **Map**: Baidu Map API

## Project Structure

```
├── prototype/          # Prototypes and POC
│   ├── poc/            # Proof of concept projects
│   ├── aep-integration/# AEP platform integration phases
│   └── docs/           # Prototype documentation
└── docs/               # Project documentation
```

> Note: `reference/`（AEP SDK、真实生产数据）不在公开仓库中，见 `.gitignore`。

## Core Features

- Real-time device monitoring and alert management
- GIS map visualization
- Workflow management (BPMN 2.0)
- WeChat integration
- WebSocket real-time communication
- AEP platform integration (China Telecom)

## Requirements

- **Java**: JDK 8+
- **Node.js**: 14.0+
- **MySQL**: 5.7+
