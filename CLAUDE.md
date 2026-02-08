# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

众成通信物联网监控系统项目集合，包含主要开发项目和验收系统：

1. **信号覆盖主机内置式设备监控系统软件开发** - 主要开发项目
2. **众成验收** - 4个相关子系统的验收版本

**合作方目录**：majiang、toyou

## 系统架构

### 技术栈
- **后端**: Spring Boot 2.1.5 + Java 8 + MyBatis + MySQL + Druid连接池
- **前端Web**: Vue.js 2.6 + iView UI 3.5 + Vue Router + Vuex + ECharts
- **移动端**: uni-app (众成云APP，支持iOS/Android/微信小程序)
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
├── majiang/                           # 合作方项目目录
│   ├── 信号覆盖主机内置式设备监控系统软件开发/源代码/sourcecode/
│   │   ├── app/JSZCAPP/               # uni-app移动端
│   │   ├── front/iview-Iot/           # Vue.js前端
│   │   ├── plat/springboot/           # Spring Boot后端
│   │   ├── sql/                       # 数据库脚本
│   │   └── deploy/                    # 部署文件
│   └── 众成验收/                       # 验收系统集合
│       ├── 物联网云监控平台/
│       ├── 信号覆盖主机内置式设备监控系统软件开发/
│       ├── 信号覆盖设备安装及租赁系统/
│       └── 5G兼容需求/
├── toyou/                            # toyou合作项目
├── protoType/                        # 原型开发
├── reference/                        # 参考文档和AEP SDK
└── docs/                             # 项目文档
```

## 开发命令

### 后端开发 (Spring Boot)
```bash
cd majiang/信号覆盖主机内置式设备监控系统软件开发/源代码/sourcecode/plat/springboot/

./mvnw spring-boot:run    # Linux/macOS
mvnw.cmd spring-boot:run  # Windows

# 其他命令
./mvnw clean package     # 编译打包
./mvnw test              # 运行测试

# API文档: http://localhost:8080/swagger-ui.html
```

### 前端开发 (Vue.js)
```bash
cd majiang/信号覆盖主机内置式设备监控系统软件开发/源代码/sourcecode/front/iview-Iot/

npm install              # 安装依赖
npm run dev             # 开发服务器
npm run build           # 生产构建
npm run lint            # 代码检查

# Windows快捷脚本 (bin/目录下)
bin/build.bat           # 生产构建
bin/run-web.bat         # 开发服务器
```

### 移动端开发 (uni-app)
```bash
cd majiang/信号覆盖主机内置式设备监控系统软件开发/源代码/sourcecode/app/JSZCAPP/
# 通过HBuilderX IDE开发，支持iOS/Android/微信小程序
# 微信小程序AppID: wx678de40d7c844bab
```

### 数据库管理
```bash
mysql -u root -p < sql/iot_plat.sql        # 完整数据库结构
mysql -u root -p < sql/upgrade_from306.sql # v3.0.6升级脚本
deploy/backupdatabase.bat                  # 备份数据库(Windows)
deploy/start.bat                           # 启动服务(Windows)
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

### 百度地图密钥
- Android: `AS43P8lG2yPFZRRn5hKWPEIqU0cq1lgk`
- iOS: `AXzWh3LDYCyrOAy8URBMw5T8sPCBsqFa`

## 部署说明

### 标准部署流程
1. **构建前端**: `cd front/iview-Iot && npm run build`
2. **构建后端**: `cd plat/springboot && mvn clean package`
3. **配置Nginx**: 静态文件指向前端dist，API反向代理到Spring Boot
4. **数据库**: 导入SQL文件，配置连接信息

### 配置文件
- 后端: `plat/springboot/src/main/resources/application.yml`
- 前端: `front/iview-Iot/src/config/`
- 移动端: `app/JSZCAPP/manifest.json`

## 多系统说明

### 众成验收4个子系统
1. **物联网云监控平台**: 通用监控框架
2. **信号覆盖主机内置式设备监控系统**: 信号覆盖设备监控
3. **信号覆盖设备安装及租赁系统**: 设备安装租赁管理
4. **5G兼容需求**: 5G兼容功能

### 目录结构
- **主开发**: `majiang/信号覆盖主机内置式设备监控系统软件开发/源代码/sourcecode/`
- **验收系统**: `majiang/众成验收/[系统名]/源代码/sourcecode/`
- **5G系统**: `majiang/众成验收/5G兼容需求/源码/source/` （特殊目录）

### 注意事项
- 各系统使用不同数据库名称
- 5G系统有独特的升级包管理功能
- API接口根据业务需求有所不同

## 常见开发任务

### 添加设备类型
1. 后端: 添加Entity和Mapper
2. 前端: 更新设备管理页面
3. 移动端: 增加显示组件
4. 数据库: 更新表结构

### 地图功能集成
1. 前端: 扩展Vue-Baidu-Map
2. 移动端: 更新manifest.json地图配置
3. 后端: 添加地理位置处理逻辑

### 工作流程修改
1. BPMN.js编辑器设计流程
2. Flowable部署流程定义
3. 前端更新流程管理界面
4. 测试流程节点和分支

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

### 开发命令
```bash
cd reference/267848_sdk/demo/
source export_env.sh
./run_query.sh
```

### 文档位置
- API文档: `reference/267848_sdk/doc/`
- SDK示例: `reference/267848_sdk/demo/`
- 学习笔记: `reference/AEP_学习笔记.md`