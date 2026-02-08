# AEP集成项目

## 🎯 项目概述

本项目是云监控平台与中国电信AEP（Application Enablement Platform）物联网平台的集成开发项目，采用分阶段开发方式，逐步实现完整的AEP平台集成功能。

## 📋 开发阶段规划

### Phase 1: 产品和设备查询 ✅
**目标**: 实现基础的产品和设备信息查询功能
- ✅ 产品列表查询
- ✅ 设备信息查询
- ✅ API认证和连接验证
- ✅ 安全配置管理

**状态**: 已完成
**位置**: [phase1-query/](phase1-query/)

### Phase 2: 产品注册和管理 🔄
**目标**: 实现产品的创建、更新、删除等管理功能
- 🔲 创建新产品
- 🔲 更新产品配置
- 🔲 删除产品
- 🔲 产品权限管理

**状态**: 规划中
**位置**: [phase2-registration/](phase2-registration/)

### Phase 3: 订阅和消息处理 🔄
**目标**: 实现AEP平台消息订阅和事件处理
- 🔲 订阅设备消息
- 🔲 处理设备事件
- 🔲 消息队列管理
- 🔲 实时数据推送

**状态**: 规划中
**位置**: [phase3-subscription/](phase3-subscription/)

### Phase 4: Web用户界面 🔄
**目标**: 开发Web管理界面
- 🔲 产品管理界面
- 🔲 设备状态监控
- 🔲 数据可视化
- 🔲 系统配置管理

**状态**: 规划中
**位置**: [phase4-ui/](phase4-ui/)

## 📁 项目结构

```
aep-integration/
├── phase1-query/                      # Phase 1: 查询功能
│   ├── src/                          # Java源代码
│   ├── lib/                          # 依赖库
│   ├── scripts/                      # 运行脚本
│   ├── config/                       # 配置文件
│   └── docs/                         # 文档
├── phase2-registration/              # Phase 2: 产品注册（预留）
├── phase3-subscription/              # Phase 3: 订阅处理（预留）
├── phase4-ui/                        # Phase 4: Web界面（预留）
├── shared/                           # 共享资源
│   ├── lib/                         # 通用依赖库
│   ├── config/                      # 通用配置
│   └── utils/                       # 通用工具类
├── .env.global                      # 全局环境变量（不提交）
├── .gitignore                       # Git忽略规则
└── README.md                        # 项目总体说明（本文档）
```

## 🚀 快速开始

### 1. 环境准备

确保您有以下环境：
- Java 8+
- AEP平台认证信息（App Key, App Secret等）

### 2. 配置认证信息

```bash
# 方法1: 配置全局环境变量
cp .env.global.template .env.global
# 编辑 .env.global，填入真实认证信息

# 方法2: 配置各Phase独立环境变量
cd phase1-query
cp config/.env.template config/.env
# 编辑 .env，填入认证信息
```

### 3. 运行Phase 1功能

```bash
# 进入Phase 1目录
cd phase1-query

# 一键运行产品查询
./scripts/run_query.sh

# 或运行增强版功能
./scripts/run_enhanced.sh
```

## 🔧 技术架构

### 核心技术栈
- **Java 8+**: 主要开发语言
- **AEP SDK**: 中国电信官方SDK
- **Apache HttpClient**: HTTP客户端库
- **HMAC-SHA1**: API签名算法

### 依赖管理
- **Phase独立性**: 每个Phase有独立的依赖库
- **共享资源**: 通用库和工具放在shared目录
- **版本控制**: 明确的依赖版本记录

### 安全设计
- **环境变量配置**: 所有敏感信息通过环境变量管理
- **分层配置**: 全局配置 + Phase特定配置
- **Git保护**: 完善的.gitignore规则

## 📊 认证信息配置

### 必需的环境变量

```bash
# AEP平台基础认证信息
AEP_APP_KEY=您的应用Key
AEP_APP_SECRET=您的应用Secret
AEP_API_HOST=您的域名.api.ctwing.cn
AEP_APP_ID=您的应用ID

# 可选配置
AEP_MASTER_KEY=您的产品MasterKey
AEP_PRODUCT_ID=您的产品ID
```

### 配置文件层次

1. **全局配置**: `.env.global` - 所有Phase通用
2. **Phase配置**: `phase*/config/.env` - Phase特定
3. **运行时配置**: 命令行环境变量 - 临时覆盖

## 🔄 开发流程

### 当前Phase开发
1. 进入对应Phase目录
2. 配置环境变量
3. 开发和测试功能
4. 更新文档

### 新Phase开发
1. 从模板创建新Phase目录结构
2. 复制必要的共享依赖
3. 开发Phase特定功能
4. 集成测试

### 版本发布
1. 完成Phase功能验证
2. 更新文档和README
3. 提交代码到版本控制
4. 标记版本里程碑

## 📖 文档和帮助

### Phase 1 相关文档
- [Phase 1 概览](phase1-query/phase1-README.md)
- [详细使用指南](phase1-query/docs/README.md)
- [安全配置指南](phase1-query/docs/SECURITY_CONFIG.md)
- [开发总结报告](phase1-query/docs/PROJECT_SUMMARY.md)

### 通用文档
- [AEP API官方文档](../reference/267848_sdk/doc/)
- [项目架构设计](../docs/architecture/)
- [需求分析文档](../docs/requirements/)

## 🛡️ 安全注意事项

1. **敏感信息保护**
   - 不要在代码中硬编码认证信息
   - 使用环境变量管理敏感配置
   - 确保 `.env*` 文件在 `.gitignore` 中

2. **访问权限控制**
   - 定期轮换API密钥
   - 最小权限原则
   - 监控API调用日志

3. **代码安全**
   - 定期安全扫描
   - 依赖库漏洞检查
   - 代码审查流程

## 📞 技术支持

### 问题排查
1. 查看对应Phase的README文档
2. 检查环境变量配置
3. 验证网络连接和权限
4. 查看API调用日志

### 联系方式
- 项目内部技术支持
- AEP平台官方技术支持
- [GitHub Issues](链接待补充)

## 📝 更新日志

### v1.0.0 - 2024-12-27
- ✅ 完成Phase 1: 产品和设备查询功能
- ✅ 建立项目基础架构
- ✅ 实现安全配置管理
- ✅ 完成文档体系

### v1.1.0 - 待规划
- 🔲 Phase 2: 产品注册功能
- 🔲 改进错误处理机制
- 🔲 添加单元测试

---

**项目创建**: 2024-12-27
**当前版本**: v1.0.0
**维护团队**: 众成科技云监控平台团队