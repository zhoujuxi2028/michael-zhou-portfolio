# AEP产品注册工具 (Phase2-Registration)

## 📋 项目概述

AEP产品注册工具是云监控平台与中国电信AEP物联网平台集成项目的第二阶段，专门用于产品的注册、更新、删除等管理操作。基于Phase1.1-export的成功架构，提供完整的产品生命周期管理功能。

## 🎯 功能特性

### 核心功能
- ✅ **产品注册** - 创建新的IoT产品，支持多种设备类型和网络协议
- ✅ **产品更新** - 修改现有产品的配置信息
- ✅ **产品删除** - 安全删除产品（支持强制删除）
- ✅ **连接测试** - 验证AEP平台连接和权限配置

### 高级特性
- 🔄 **重试机制** - 自动重试失败的操作，支持指数退避策略
- 📝 **操作审计** - 完整记录所有产品管理操作
- ⚡ **批量操作** - 支持批量产品注册
- 🛡️ **安全验证** - 多层数据验证和权限检查
- 📊 **统计监控** - 实时操作统计和成功率监控

### 支持的设备类型
- `SENSOR` - 传感器设备
- `GATEWAY` - 网关设备
- `DEVICE` - 通用设备
- `TERMINAL` - 终端设备
- `MODULE` - 模块设备

### 支持的网络类型
- `NB-IOT` - NB-IoT网络
- `2G/3G/4G/5G` - 移动网络
- `WIFI` - WiFi网络
- `ETHERNET` - 以太网
- `LORA` - LoRa网络

## 🚀 快速开始

### 环境要求
- **Java**: JDK 17+
- **Maven**: 3.6+
- **AEP平台**: 有效的应用密钥和权限

### 1. 安装配置

```bash
# 克隆项目（如果需要）
git clone <repository-url>
cd phase2-registration

# 配置环境变量
cp .env.template .env
# 编辑 .env 文件，填入真实的AEP认证信息

# 运行测试验证
./scripts/test.sh
```

### 2. 环境变量配置

编辑 `.env` 文件：

```bash
# AEP平台认证信息
AEP_APP_KEY=your_app_key_here
AEP_APP_SECRET=your_app_secret_here
AEP_API_HOST=your_tenant_id.api.ctwing.cn
AEP_APP_ID=your_app_id_here

# 可选配置
AEP_MAX_RETRIES=3
AEP_OPERATION_TIMEOUT=30000
AEP_ENABLE_AUDIT_LOG=true
```

### 3. 验证安装

```bash
# 运行完整测试套件
./scripts/test.sh

# 测试AEP连接
java -jar target/aep-product-registration-1.0.0.jar --test
```

## 📖 使用指南

### 命令行方式

#### 创建产品
```bash
# 基础产品创建
java -jar aep-product-registration.jar --create \
  --product-name "温度传感器" \
  --device-type "SENSOR"

# 完整配置的产品创建
java -jar aep-product-registration.jar --create \
  --product-name "智能网关" \
  --device-type "GATEWAY" \
  --network-type "NB-IOT" \
  --data-format "JSON" \
  --description "室外环境监测网关" \
  --device-model "ZCT-GW-001" \
  --manufacturer "Vendor C" \
  --protocol-type "CoAP" \
  --max-device-count 1000 \
  --enable-security \
  --auto-create-device
```

#### 更新产品
```bash
java -jar aep-product-registration.jar --update \
  --product-id 12345 \
  --description "更新的产品描述" \
  --max-device-count 2000
```

#### 删除产品
```bash
java -jar aep-product-registration.jar --delete \
  --product-id 12345 \
  --force
```

### 脚本方式

```bash
# 使用便捷脚本创建产品
./scripts/register_product.sh "智能水表" SENSOR

# 带参数的产品创建
./scripts/register_product.sh \
  --network-type NB-IOT \
  --data-format JSON \
  --description "小区智能水表" \
  --enable-security \
  "智能水表" SENSOR
```

### Java API方式

```java
// 创建AEP客户端
AepRegistrationClient client = AepRegistrationClient.fromEnvironment();

// 创建注册服务
ProductRegistrationService service = new ProductRegistrationService(client);

// 构建注册请求
ProductRegistrationRequest request = ProductRegistrationRequest.builder()
    .productName("API测试产品")
    .deviceType("SENSOR")
    .networkType("NB-IOT")
    .dataFormat("JSON")
    .description("通过API创建的产品")
    .enableSecurity(true)
    .build();

// 执行注册
RegistrationResult result = service.registerProduct(request);

// 处理结果
if (result.isSuccess()) {
    System.out.println("产品创建成功: " + result.getProductId());
    System.out.println("主密钥: " + result.getMasterKey());
} else {
    System.err.println("产品创建失败: " + result.getErrorMessage());
}
```

## 🏗️ 项目架构

```
phase2-registration/
├── src/main/java/com/aep/registration/
│   ├── AepProductRegistration.java          # 主程序入口
│   ├── model/                               # 数据模型
│   │   ├── ProductRegistrationRequest.java
│   │   └── RegistrationResult.java
│   └── service/                             # 服务层
│       ├── ProductRegistrationService.java  # 核心业务逻辑
│       └── AepRegistrationClient.java       # AEP API客户端
├── src/test/java/                          # 单元测试
├── scripts/                                # 运行脚本
│   ├── register_product.sh                 # 产品注册脚本
│   └── test.sh                             # 测试套件
├── lib/                                    # AEP SDK依赖
├── .env.template                           # 环境变量模板
├── pom.xml                                 # Maven配置
└── README.md                               # 项目文档
```

### 核心组件说明

#### 1. ProductRegistrationService
- **职责**: 核心业务逻辑，产品生命周期管理
- **特性**: 重试机制、数据验证、操作审计、批量操作
- **设计模式**: 建造者模式、策略模式

#### 2. AepRegistrationClient
- **职责**: AEP SDK封装，API调用管理
- **特性**: 认证管理、响应解析、错误处理
- **安全**: 参数验证、敏感信息脱敏

#### 3. 数据模型
- **ProductRegistrationRequest**: 产品注册请求，支持Builder模式
- **RegistrationResult**: 操作结果封装，包含详细状态信息

## 🔧 高级配置

### 重试策略配置
```java
// 自定义重试策略
ProductRegistrationService service = new ProductRegistrationService(
    client,
    5,      // 最大重试次数
    60000L, // 操作超时时间(ms)
    true    // 启用审计日志
);
```

### 批量操作
```java
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
```

### 操作统计
```java
Map<String, Object> stats = service.getServiceStats();
System.out.println("总操作数: " + stats.get("totalOperations"));
System.out.println("成功率: " + stats.get("successRate"));
```

## 🧪 测试

### 运行测试
```bash
# 运行完整测试套件
./scripts/test.sh

# 运行单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=ProductRegistrationRequestTest

# 运行集成测试（需要真实环境）
mvn test -Dtest=*IntegrationTest
```

### 测试覆盖率
```bash
# 生成测试覆盖率报告
mvn jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

## 🛡️ 安全注意事项

### 1. 凭据管理
- ❌ **不要在代码中硬编码认证信息**
- ✅ **使用环境变量管理敏感配置**
- ✅ **确保 `.env` 文件在 `.gitignore` 中**

### 2. 权限控制
- **产品管理权限**: 确保AEP应用具有产品管理权限
- **操作审计**: 启用操作日志记录
- **最小权限原则**: 只申请必需的API权限

### 3. 数据验证
- **输入验证**: 严格验证所有输入参数
- **业务规则**: 检查产品名称唯一性等业务规则
- **依赖检查**: 删除前检查关联设备

## 📊 监控和故障排查

### 查看操作统计
```bash
java -jar aep-product-registration.jar --stats
```

### 日志级别配置
```bash
# 启用详细日志
export JAVA_OPTS="-Djava.util.logging.level=FINE"

# 或在运行时指定
java -Djava.util.logging.level=FINE -jar aep-product-registration.jar --create ...
```

### 常见问题排查

#### 1. 认证失败
```bash
# 检查环境变量
echo $AEP_APP_KEY
echo $AEP_API_HOST

# 测试连接
java -jar aep-product-registration.jar --test
```

#### 2. 权限不足
- 检查AEP应用是否有产品管理权限
- 确认API Key对应的租户ID正确

#### 3. 网络超时
- 增加超时配置: `AEP_OPERATION_TIMEOUT=60000`
- 检查网络连接到AEP平台

## 🔄 与其他Phase集成

### Phase1.1集成
```java
// 复用Phase1.1的查询功能
AepDataExporter exporter = new AepDataExporter();
ExportResult queryResult = exporter.exportAll();

// 使用查询结果进行产品注册
ProductRegistrationRequest request = ProductRegistrationRequest.builder()
    .productName("基于查询的新产品")
    .deviceType("SENSOR")
    .build();
```

### 为Phase3准备
```java
// 注册产品后为消息订阅准备
RegistrationResult result = service.registerProduct(request);
if (result.isSuccess()) {
    // 产品ID和MasterKey将用于Phase3的消息订阅
    Long productId = result.getProductId();
    String masterKey = result.getMasterKey();
}
```

## 📝 更新日志

### v1.0.0 (2026-01-25)
- ✅ 完成产品注册、更新、删除核心功能
- ✅ 实现重试机制和错误处理
- ✅ 添加操作审计和统计功能
- ✅ 提供命令行和API两种使用方式
- ✅ 完善的单元测试覆盖
- ✅ 详细的文档和示例

### 计划功能 (v1.1.0)
- 🔲 产品权限管理功能
- 🔲 产品配置备份和恢复
- 🔲 Web管理界面集成
- 🔲 更多设备类型和协议支持

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

Copyright (c) 2024 Vendor C (ZCT)

## 📞 技术支持

- **项目团队**: 云监控平台技术团队
- **文档**: [项目Wiki](项目链接)
- **问题反馈**: [GitHub Issues](项目链接)

---

**项目状态**: ✅ 可用于生产环境
**维护状态**: 🔄 积极维护
**最后更新**: 2026-01-25