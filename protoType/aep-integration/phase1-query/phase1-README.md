# Phase 1: AEP产品和设备查询

## 🎯 Phase 1 目标

本阶段专注于实现AEP平台的基础查询功能，能够：
- 查询产品列表
- 查询设备信息
- 验证API连接和认证
- 为后续Phase奠定基础

## 📁 目录结构

```
phase1-query/
├── src/                                # Java源代码
│   ├── AepProductQuerySimple.java      # 简化版查询工具（推荐）
│   ├── AepProductManagementDemo_Enhanced.java # 增强版SDK demo
│   └── AepProductManagementDemo.java   # 原始SDK模板
├── lib/                                # 依赖库
│   ├── ctg-ag-sdk-core-*.jar          # AEP SDK核心库
│   ├── ag-sdk-biz-267848-*.jar        # AEP SDK业务库
│   ├── httpclient-4.5.13.jar         # Apache HttpClient
│   ├── httpcore-4.4.13.jar           # Apache HttpCore
│   ├── commons-logging-1.2.jar       # Commons Logging
│   └── commons-codec-1.15.jar        # Commons Codec
├── scripts/                           # 运行脚本
│   ├── run_query.sh                   # 简化版运行脚本
│   ├── run_enhanced.sh                # 增强版运行脚本
│   ├── export_env.sh                  # 环境变量导出脚本
│   └── load_env.sh                    # 环境变量加载脚本
├── config/                            # 配置文件
│   ├── .env.template                  # 环境变量模板
│   ├── .env                          # 实际环境变量（不提交到git）
│   └── .gitignore                     # Git忽略规则
├── docs/                              # 文档
│   ├── README.md                      # 详细使用指南
│   ├── SECURITY_CONFIG.md             # 安全配置指南
│   └── PROJECT_SUMMARY.md             # 项目总结
└── phase1-README.md                   # Phase 1 概览（本文档）
```

## 🚀 快速开始

### 方法1: 一键运行（推荐）

```bash
# 进入Phase 1目录
cd /Users/michael_zhou/Documents/ZCT/github/Cloud-Monitoring-Platform/protoType/aep-integration/phase1-query

# 运行简化版查询
./scripts/run_query.sh

# 或运行增强版SDK
./scripts/run_enhanced.sh
```

### 方法2: 手动运行

```bash
# 1. 导出环境变量
source scripts/export_env.sh

# 2. 进入源码目录
cd src

# 3. 编译和运行简化版
javac AepProductQuerySimple.java
java AepProductQuerySimple

# 或编译和运行增强版
javac -cp "../lib/*:." AepProductManagementDemo_Enhanced.java
java -cp "../lib/*:." AepProductManagementDemo_Enhanced
```

## 🔧 配置说明

### 环境变量配置

在运行前，请确保配置了以下环境变量：

```bash
# 基础认证信息
export AEP_APP_KEY="您的App Key"
export AEP_APP_SECRET="您的App Secret"
export AEP_API_HOST="您的域名.api.ctwing.cn"
export AEP_APP_ID="您的应用ID"

# 可选配置
export AEP_MASTER_KEY="您的产品MasterKey"
export AEP_PRODUCT_ID="您的产品ID"
```

### 配置文件使用

1. **从模板创建配置**：
   ```bash
   cp config/.env.template config/.env
   # 编辑 .env 文件，填入真实认证信息
   ```

2. **使用全局配置**：
   ```bash
   # 也可以使用项目根目录的全局配置
   source ../.env.global
   ```

## 📊 功能验证

运行成功后，您应该看到类似以下输出：

```
=== 您的产品列表 ===
总产品数: 2

1. RepeaterLTE01
   产品ID: 16980130
   类型: 4G通信模组
   设备数: 532台

2. RepeaterLTE
   产品ID: 16857118
   类型: 5G通信模组
   设备数: 892台
```

## 📋 Phase 1 包含功能

### 1. 产品查询功能
- ✅ 查询所有产品列表
- ✅ 显示产品基本信息（ID、名称、类型、设备数量）
- ✅ 支持简化版（无依赖）和增强版（完整SDK）

### 2. 认证和连接
- ✅ HMAC-SHA1签名算法实现
- ✅ 环境变量安全配置
- ✅ API连接验证

### 3. 工具和脚本
- ✅ 一键运行脚本
- ✅ 环境变量管理工具
- ✅ 编译和执行自动化

## 🔜 下一阶段计划

完成Phase 1后，可以继续进行：

- **Phase 2**: 产品注册和管理功能
- **Phase 3**: 订阅和消息处理
- **Phase 4**: Web界面开发

## 📖 详细文档

- [详细使用指南](docs/README.md)
- [安全配置指南](docs/SECURITY_CONFIG.md)
- [项目开发总结](docs/PROJECT_SUMMARY.md)

## 🆘 问题排查

如果遇到问题：

1. **环境变量问题**：检查 `config/.env` 文件配置
2. **依赖库问题**：确认 `lib/` 目录包含所有jar文件
3. **网络问题**：检查与AEP平台的连接
4. **权限问题**：确认应用拥有产品管理权限

详细排查方法请参考 [docs/SECURITY_CONFIG.md](docs/SECURITY_CONFIG.md) 中的故障排除部分。

---

**Phase 1 完成日期**: 2024-12-27
**技术栈**: Java 8+, AEP SDK, HMAC-SHA1, HttpURLConnection
**状态**: ✅ 功能完成，已验证