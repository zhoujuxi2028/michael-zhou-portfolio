# 简单产品状态查询服务

## 项目概述

本项目是参考toyou系统实现的简化版产品状态查询服务，支持中国电信物联网认证机制，提供详细的日志记录便于调试。

## 技术特性

### 核心功能
- **产品状态查询**: 根据LbsId查询设备的详细状态信息
- **批量查询**: 支持同时查询多个设备状态
- **中国电信物联网认证**: 集成标准的电信物联网平台认证机制
- **详细日志记录**: 全链路日志记录，便于问题定位

### 技术栈
- **Spring Boot 2.7.14**: 基础框架
- **Java 8**: 开发语言
- **H2 Database**: 内存数据库（测试用）
- **Swagger/OpenAPI**: API文档
- **Lombok**: 简化开发
- **JUnit 5**: 单元测试

## 快速开始

### 1. 环境要求
- JDK 8+
- Maven 3.6+

### 2. 编译和运行
```bash
# 进入项目目录
cd simple-product-status-query

# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动服务
mvn spring-boot:run
```

### 3. 服务访问
启动成功后，可以通过以下地址访问：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API文档**: http://localhost:8080/v2/api-docs
- **健康检查**: http://localhost:8080/api/product-status/health
- **H2控制台**: http://localhost:8080/h2-console

## API使用示例

### 1. 查询单个产品状态

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/product-status/queryByLbsId \
  -H "Content-Type: application/json" \
  -d '{
    "lbsId": "station001",
    "projectId": "project_001",
    "companyId": "company_001"
  }'
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "success": true,
  "data": {
    "id": "device_-1350924170",
    "lbsId": "station001",
    "deviceName": "测试设备_station001",
    "model": "ZC-001",
    "deviceType": "4G设备",
    "projectId": "project_001",
    "projectName": "测试项目_project_001",
    "companyId": "company_001",
    "companyName": "测试公司_company_001",
    "status": "11111111111",
    "statusDescription": "设备运行正常",
    "onlineStatus": 1,
    "lastReportTime": "2024-12-25T14:30:00.000+00:00",
    "processStatus": "3",
    "upgradeStatus": "2",
    "upgradeStatusName": "升级成功"
  },
  "timestamp": 1703518200000
}
```

### 2. 批量查询产品状态

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/product-status/batchQuery?lbsIds=station001,station002,station003"
```

### 3. 健康检查

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/product-status/health
```

## 状态码说明

### 设备状态码格式
设备状态使用11位状态码表示各模块的运行状态：
- `11111111111`: 设备运行正常
- `00000000000`: 设备离线
- 其他组合: 设备部分功能异常

### 在线状态判断
- 如果状态码第1位或第2位为`0`，则设备离线
- 否则设备在线

### 升级状态
- `0`: 未升级
- `1`: 升级中
- `2`: 升级成功
- `3`: 升级失败

### 流程状态
- `1`: 待采购
- `2`: 已采购
- `3`: 调试成功
- `4`: 调试失败

## 中国电信物联网认证

### 认证配置
在`application.yml`中配置电信物联网认证信息：

```yaml
app:
  telecom-iot:
    enabled: true
    app-key: "your_app_key"
    app-secret: "your_app_secret"
    platform-url: "https://api.iot.chinatelecom.com"
    timeout: 5000
```

### 认证机制
1. **设备认证**: 支持IMEI、IMSI验证
2. **Token认证**: 自动获取和缓存访问Token
3. **请求签名**: 基于SHA-256的请求签名验证

### 使用示例
```java
// 验证设备认证
boolean authResult = telecomIotAuthService.validateDeviceAuth(
    "device001",           // 设备ID
    "123456789012345",     // IMEI (15位数字)
    "460012345678901"      // IMSI (以460开头的15位数字)
);

// 获取访问Token
String token = telecomIotAuthService.getAccessToken();

// 生成API签名
String signature = telecomIotAuthService.generateSignature(timestamp, params);
```

## 日志配置

### 日志级别
- **DEBUG**: 详细的调试信息
- **INFO**: 关键业务流程信息
- **WARN**: 警告信息
- **ERROR**: 错误信息

### 日志文件
日志文件位置: `logs/simple-product-status-query.log`

### 关键日志点
1. **API调用**: 记录所有API请求和响应
2. **业务流程**: 记录状态查询的各个步骤
3. **认证过程**: 记录电信物联网认证流程
4. **异常处理**: 记录所有异常和错误处理

## 测试

### 单元测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ProductStatusServiceTest

# 运行特定测试方法
mvn test -Dtest=ProductStatusServiceTest#testQueryProductStatusByLbsId_Success
```

### 集成测试
```bash
# 启动服务后运行集成测试
curl -X POST http://localhost:8080/api/product-status/queryByLbsId \
  -H "Content-Type: application/json" \
  -d '{"lbsId": "test_station"}'
```

## 性能监控

### 执行时间监控
每个API调用都会记录执行时间：
```
产品状态查询完成 - Controller层, lbsId: station001, 耗时: 156ms
```

### 健康检查端点
- **基础健康检查**: `/actuator/health`
- **详细信息**: `/actuator/info`
- **指标监控**: `/actuator/metrics`

## 故障排除

### 常见问题

1. **lbsId参数为空**
   - 错误：`lbsId不能为空`
   - 解决：确保请求体中包含有效的lbsId

2. **电信物联网认证失败**
   - 检查app-key和app-secret配置
   - 验证IMEI和IMSI格式是否正确

3. **服务启动失败**
   - 检查端口8080是否被占用
   - 验证Java版本是否为8+

### 日志分析
查看详细日志以定位问题：
```bash
tail -f logs/simple-product-status-query.log
```

## 扩展开发

### 添加新的认证方式
1. 实现新的认证服务接口
2. 在配置文件中添加相关配置
3. 在Controller中集成新的认证逻辑

### 集成真实数据库
1. 替换H2配置为MySQL/PostgreSQL
2. 添加相应的数据库驱动依赖
3. 创建对应的数据表结构

### 添加缓存支持
1. 引入Redis依赖
2. 配置缓存管理器
3. 在Service层添加缓存注解

## 参考资料

- [toyou系统源码](../../../toyou/)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [中国电信物联网开发平台](https://www.ctwing.cn/)
- [Swagger文档规范](https://swagger.io/specification/)

## 版本历史

- **v1.0.0**: 基础功能实现，支持产品状态查询和电信物联网认证