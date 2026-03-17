# Phase2 产品注册工具用户手册

## 📋 文档信息
- **版本**: v1.0
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-25
- **适用对象**: 系统管理员、开发人员、运维人员
- **状态**: 用户指南，待评审

## 🎯 产品概述

Phase2产品注册工具是云监控平台与中国电信AEP物联网平台集成项目的第二阶段，在Phase1.1-export成功架构基础上，提供完整的IoT产品生命周期管理功能。

### 核心功能

- ✅ **产品注册** - 创建新的IoT产品
- ✅ **产品更新** - 修改现有产品配置
- ✅ **产品删除** - 安全删除产品
- ✅ **兼容查询** - 继承Phase1.1的产品/设备查询功能

### 技术特性

- 🔄 **自动重试** - 失败操作自动重试
- 📝 **操作审计** - 完整的操作日志记录
- 🛡️ **数据验证** - 多层输入验证机制
- 📊 **性能监控** - 实时操作统计

## 🚀 快速开始

### 1. 环境要求

**系统要求**：
- Java 17+ (推荐)
- 至少512MB可用内存
- 网络连接到AEP平台

**AEP平台要求**：
- 有效的AEP应用账户
- 产品管理权限
- API调用配额

### 2. 安装配置

#### 步骤1: 获取认证信息

从AEP平台管理控制台获取以下信息：
```bash
应用ID (APP_ID): 您的应用ID
应用密钥 (APP_KEY): 应用密钥
应用秘钥 (APP_SECRET): 应用秘钥
API域名 (API_HOST): 租户ID.api.ctwing.cn
```

#### 步骤2: 配置环境变量

创建 `.env` 文件：
```bash
# 复制模板文件
cp .env.template .env

# 编辑配置文件
nano .env
```

填入您的AEP认证信息：
```bash
# AEP平台认证信息
AEP_APP_KEY=您的应用密钥
AEP_APP_SECRET=您的应用秘钥
AEP_API_HOST=您的租户ID.api.ctwing.cn
AEP_APP_ID=您的应用ID

# 可选配置
AEP_MAX_RETRIES=3
AEP_OPERATION_TIMEOUT=30000
AEP_ENABLE_DEBUG_LOG=true
```

#### 步骤3: 验证配置

```bash
# 测试连接
java -jar aep-product-registration.jar --test

# 预期输出
✅ AEP连接测试成功
✅ 应用认证验证通过
✅ 产品管理权限确认
```

## 📖 使用指南

### 1. 命令行使用方式

#### 1.1 创建产品

**基础产品创建**：
```bash
java -jar aep-product-registration.jar --create \
  --product-name "智能温度传感器" \
  --device-type "SENSOR"
```

**完整参数创建**：
```bash
java -jar aep-product-registration.jar --create \
  --product-name "工业网关设备" \
  --device-type "GATEWAY" \
  --network-type "NB-IOT" \
  --data-format "JSON" \
  --description "工业环境监测网关" \
  --device-model "ZCT-GW-2024" \
  --manufacturer "Vendor C" \
  --protocol-type "CoAP" \
  --enable-security \
  --auto-create-device
```

**成功输出示例**：
```json
✅ 产品创建成功!
{
  "operation": "createProduct",
  "success": true,
  "productId": 16980145,
  "productName": "智能温度传感器",
  "masterKey": "a1b2c3d4e5f6...",
  "timestamp": 1737789600000,
  "duration": 2340
}
```

#### 1.2 更新产品

```bash
java -jar aep-product-registration.jar --update \
  --product-id 16980145 \
  --description "升级版工业网关设备" \
  --device-model "ZCT-GW-2024-Pro"
```

#### 1.3 删除产品

**安全删除** (检查依赖)：
```bash
java -jar aep-product-registration.jar --delete \
  --product-id 16980145
```

**强制删除** (忽略依赖)：
```bash
java -jar aep-product-registration.jar --delete \
  --product-id 16980145 \
  --force
```

#### 1.4 查询功能 (继承Phase1.1)

```bash
# 查询所有产品 (Phase1.1兼容)
java -jar aep-product-registration.jar --query-products

# 查询指定产品的设备
java -jar aep-product-registration.jar --query-devices \
  --product-id 16980145 \
  --master-key "a1b2c3d4e5f6..."
```

### 2. 脚本使用方式

#### 2.1 便捷创建脚本

```bash
# 基础使用
./scripts/register_product.sh "智能水表" SENSOR

# 带参数使用
./scripts/register_product.sh \
  --network-type NB-IOT \
  --data-format JSON \
  --description "小区智能水表监测" \
  --enable-security \
  "智能水表" SENSOR
```

#### 2.2 批量操作脚本

```bash
# 批量创建产品
./scripts/batch_create_products.sh products.txt

# products.txt格式示例:
# 产品名称,设备类型,网络类型,描述
# 温度传感器01,SENSOR,NB-IOT,室内温度监测
# 湿度传感器01,SENSOR,NB-IOT,室内湿度监测
# 智能网关01,GATEWAY,4G,数据汇聚网关
```

### 3. Java API使用方式

#### 3.1 基础API调用

```java
import com.aep.registration.service.*;
import com.aep.registration.model.*;

// 创建客户端
ExportConfig config = ExportConfig.fromEnvironment();
AepClientManager client = new AepClientManager(config);
ProductRegistrationService service = new ProductRegistrationService(client);

// 创建产品
ProductRegistrationRequest request = ProductRegistrationRequest.builder()
    .productName("API测试产品")
    .deviceType("SENSOR")
    .networkType("NB-IOT")
    .dataFormat("JSON")
    .description("通过API创建的测试产品")
    .enableSecurity(true)
    .build();

RegistrationResult result = service.registerProduct(request);

// 处理结果
if (result.isSuccess()) {
    System.out.println("产品创建成功:");
    System.out.println("产品ID: " + result.getProductId());
    System.out.println("主密钥: " + result.getMasterKey());
} else {
    System.err.println("创建失败: " + result.getErrorMessage());
}
```

#### 3.2 批量操作API

```java
// 批量创建产品
List<ProductRegistrationRequest> requests = Arrays.asList(
    ProductRegistrationRequest.builder()
        .productName("批量产品1")
        .deviceType("SENSOR")
        .build(),
    ProductRegistrationRequest.builder()
        .productName("批量产品2")
        .deviceType("GATEWAY")
        .build()
);

List<RegistrationResult> results = service.registerProductsBatch(requests);

// 处理批量结果
for (int i = 0; i < results.size(); i++) {
    RegistrationResult result = results.get(i);
    if (result.isSuccess()) {
        System.out.printf("产品%d创建成功: ID=%d%n",
            i + 1, result.getProductId());
    } else {
        System.err.printf("产品%d创建失败: %s%n",
            i + 1, result.getErrorMessage());
    }
}
```

## 📊 参数参考

### 1. 命令行参数

| 参数 | 类型 | 必需 | 描述 | 示例 |
|------|------|------|------|------|
| `--create` | 操作 | - | 创建产品操作 | `--create` |
| `--update` | 操作 | - | 更新产品操作 | `--update` |
| `--delete` | 操作 | - | 删除产品操作 | `--delete` |
| `--test` | 操作 | - | 连接测试操作 | `--test` |
| `--product-name` | 字符串 | 是* | 产品名称 (1-64字符) | `"智能传感器"` |
| `--product-id` | 数字 | 是** | 产品ID | `16980145` |
| `--device-type` | 枚举 | 是* | 设备类型 | `SENSOR/GATEWAY/DEVICE` |
| `--network-type` | 枚举 | 否 | 网络类型 | `NB-IOT/4G/WIFI` |
| `--data-format` | 枚举 | 否 | 数据格式 | `JSON/BINARY` |
| `--description` | 字符串 | 否 | 产品描述 (最大255字符) | `"环境监测传感器"` |
| `--force` | 标志 | 否 | 强制执行 | `--force` |

> *创建操作必需
> **更新/删除操作必需

### 2. 设备类型枚举

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `SENSOR` | 传感器设备 | 温度、湿度、压力等传感器 |
| `GATEWAY` | 网关设备 | 数据汇聚、协议转换网关 |
| `DEVICE` | 通用设备 | 通用IoT设备 |
| `TERMINAL` | 终端设备 | 用户交互终端 |
| `MODULE` | 模块设备 | 嵌入式功能模块 |

### 3. 网络类型枚举

| 类型 | 说明 | 特点 |
|------|------|------|
| `NB-IOT` | 窄带物联网 | 低功耗、广覆盖 |
| `2G/3G/4G/5G` | 移动网络 | 高带宽、实时性好 |
| `WIFI` | WiFi网络 | 局域网、高速率 |
| `ETHERNET` | 以太网 | 有线连接、稳定 |
| `LORA` | LoRa网络 | 长距离、低功耗 |

### 4. 配置参数

| 参数名 | 默认值 | 说明 |
|--------|--------|------|
| `AEP_MAX_RETRIES` | 3 | 最大重试次数 |
| `AEP_OPERATION_TIMEOUT` | 30000 | 操作超时(毫秒) |
| `AEP_ENABLE_DEBUG_LOG` | false | 启用调试日志 |
| `AEP_DEFAULT_PRODUCT_TYPE` | 1 | 默认产品类型 |
| `AEP_DEFAULT_DATA_FORMAT` | 1 | 默认数据格式 |
| `AEP_MAX_PRODUCT_NAME_LENGTH` | 64 | 产品名称最大长度 |

## 🔧 高级配置

### 1. 自定义重试策略

```java
// 配置重试参数
ProductRegistrationService service = new ProductRegistrationService(
    client,
    5,      // 最大重试次数
    60000L, // 操作超时时间(ms)
    true    // 启用审计日志
);
```

### 2. 批量操作优化

```java
// 配置批量操作参数
service.setBatchSize(10);           // 批量大小
service.setBatchInterval(1000);     // 批量间隔(ms)
service.setParallelProcessing(true); // 并行处理
```

### 3. 日志配置

```bash
# 启用详细日志
export JAVA_OPTS="-Djava.util.logging.level=FINE"

# 指定日志文件路径
export LOG_FILE_PATH="/var/log/aep-registration/"

# 设置日志轮转策略
export LOG_MAX_FILES=5
export LOG_MAX_SIZE="100MB"
```

## 📊 监控与故障排查

### 1. 操作统计查看

```bash
# 查看操作统计
java -jar aep-product-registration.jar --stats

# 输出示例
操作统计报告:
├── 总操作数: 156
├── 成功操作: 149 (95.5%)
├── 失败操作: 7 (4.5%)
├── 平均响应时间: 2.3秒
└── 最后操作时间: 2026-01-25 22:15:30
```

### 2. 日志文件位置

```bash
# 主日志文件
./logs/aep-registration-YYYYMMDD-HHMMSS.log

# 审计日志文件 (包含所有操作记录)
./logs/audit-YYYYMMDD.log

# 性能日志文件
./logs/performance-YYYYMMDD.log
```

### 3. 常见问题诊断

#### 3.1 认证失败

**现象**: 返回401认证错误

**排查步骤**:
```bash
# 1. 检查环境变量
echo "APP_KEY: ${AEP_APP_KEY:0:8}***"
echo "API_HOST: $AEP_API_HOST"

# 2. 测试连接
java -jar aep-product-registration.jar --test

# 3. 检查应用权限
curl -H "Authorization: your_auth_header" \
  https://$AEP_API_HOST/aep_product_management/products
```

**解决方法**:
- 验证APP_KEY和APP_SECRET正确性
- 确认应用具有产品管理权限
- 检查API调用配额是否用尽

#### 3.2 网络超时

**现象**: 操作超时错误

**排查步骤**:
```bash
# 1. 测试网络连通性
ping $AEP_API_HOST

# 2. 检查防火墙设置
telnet $AEP_API_HOST 443

# 3. 增加超时时间
export AEP_OPERATION_TIMEOUT=60000
```

**解决方法**:
- 检查网络连接
- 增加超时配置
- 使用重试机制

#### 3.3 参数验证失败

**现象**: 返回参数错误

**排查步骤**:
```bash
# 1. 检查产品名称
# 确保: 1-64字符, 仅包含字母数字中文下划线连字符

# 2. 检查必填参数
# productName, productType, dataFormat必填

# 3. 查看详细错误信息
java -jar aep-product-registration.jar --create \
  --product-name "测试产品" \
  --device-type "SENSOR" \
  --verbose
```

## 🛡️ 安全最佳实践

### 1. 认证信息安全

```bash
# ✅ 正确做法
# 使用环境变量文件
echo "AEP_APP_KEY=your_key" > .env
source .env

# ❌ 错误做法
# 不要在命令行直接传递
java -jar tool.jar --app-key your_key_here  # 会被记录在历史
```

### 2. 权限最小化

```bash
# AEP应用权限建议
├── 产品管理: ✅ 必需
├── 设备管理: ✅ 推荐 (用于查询)
├── 数据管理: ❌ 不必需
└── 用户管理: ❌ 不必需
```

### 3. 操作审计

```bash
# 启用完整审计日志
export AEP_ENABLE_AUDIT_LOG=true
export AEP_AUDIT_LOG_LEVEL=FULL

# 审计日志将记录:
# - 所有API调用详情
# - 操作用户和时间
# - 操作结果和错误信息
# - 性能指标
```

## 📞 技术支持

### 1. 获取帮助

```bash
# 查看帮助信息
java -jar aep-product-registration.jar --help

# 查看版本信息
java -jar aep-product-registration.jar --version

# 生成诊断报告
java -jar aep-product-registration.jar --diagnose
```

### 2. 问题反馈

**反馈渠道**:
- GitHub Issues: [项目链接]
- 内部支持: 云监控平台技术团队
- 邮件支持: tech-support@example.com

**反馈信息包含**:
- 错误信息和日志
- 操作系统和Java版本
- 配置文件内容 (脱敏后)
- 复现步骤

### 3. 更新升级

```bash
# 检查更新
java -jar aep-product-registration.jar --check-updates

# 下载最新版本
wget https://releases.example.com/aep-registration-latest.jar

# 备份配置
cp .env .env.backup

# 升级
mv aep-registration-latest.jar aep-product-registration.jar
```

## 📝 更新日志

### v1.0.0 (2026-01-25)

**新增功能**:
- ✅ 产品创建、更新、删除核心功能
- ✅ 完整的命令行接口
- ✅ Java API支持
- ✅ Phase1.1兼容性保持
- ✅ 操作审计和性能监控

**技术特性**:
- ✅ 自动重试机制
- ✅ 详细的错误处理
- ✅ 配置验证
- ✅ 日志轮转

**已知限制**:
- 批量操作功能基础版本
- 暂不支持产品权限管理
- Web界面待下一版本

---

**用户手册状态**: ✅ 编写完成，待评审
**适用版本**: Phase2 v1.0.0
**最后更新**: 2026-01-25