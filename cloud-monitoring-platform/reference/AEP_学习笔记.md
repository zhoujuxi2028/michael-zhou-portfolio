# AEP (Application Enablement Platform) 学习笔记

## 📋 基本概念

### 什么是AEP
- **全称**：Application Enablement Platform（应用使能平台）
- **运营商**：中国电信集团 (China Telecom Group, CTG)
- **品牌**：天翼物联 (China Telecom Wing)
- **官网域名**：`*.api.ctwing.cn`

### 平台定位
AEP是中国电信推出的企业级物联网云平台，为物联网应用提供设备管理、数据采集、远程控制等核心服务。

## 🏗️ 平台架构

### 核心服务模块
1. **产品管理** (`aep_product_management`)
   - 产品创建、查询、更新、删除
   - 产品列表管理
   - 产品模型定义

2. **设备管理** (`aep_device_management`)
   - 设备注册、认证
   - 设备状态监控
   - 设备生命周期管理

3. **数据服务** (`aep_device_status`)
   - 实时设备状态查询
   - 历史数据存储与查询
   - 数据分页查询

4. **远程控制** (`aep_device_control`)
   - 设备远程控制指令
   - 指令下发与响应

5. **固件管理** (`aep_firmware_management` + `aep_upgrade_management`)
   - 固件版本管理
   - 远程固件升级
   - 升级进度监控

6. **边缘计算** (`aep_edge_gateway`)
   - 边缘网关接入
   - 边缘数据处理

7. **物模型管理** (`aep_device_model`)
   - 设备数据模型定义
   - 属性、事件、服务模型

8. **消息订阅** (`aep_subscribe_north` + `aep_mq_sub`)
   - 数据推送订阅
   - MQ消息订阅管理

## 🔐 认证机制

### 认证方式
- **签名算法**：HMAC-SHA1
- **认证参数**：
  - `App ID`：应用标识符
  - `App Key`：应用密钥（用于签名）
  - `App Secret`：应用秘钥（用于签名）
  - `API Host`：租户专用域名

### 请求头格式
```http
application: {App_Key}
timestamp: {Unix_Timestamp}
signature: {HMAC_SHA1_Signature}
version: {API_Version}
Content-Type: application/json
```

### 签名算法
```
签名原文 = application:{App_Key}\ntimestamp:{timestamp}\n{业务参数}
签名结果 = HMAC-SHA1(签名原文, App_Secret)
```

## 💻 技术实现

### SDK支持
- **Java SDK**：官方提供完整的Java开发包
- **核心库**：`ctg-ag-sdk-core-2.9.0.jar`
- **业务库**：`ag-sdk-biz-{app_id}.jar`

### API特点
- **协议**：HTTPS REST API
- **数据格式**：JSON
- **认证**：基于HMAC-SHA1签名
- **版本管理**：支持API版本号访问

### 关键类库
```java
// 主要客户端
import com.ctg.ag.sdk.biz.AepProductManagementClient;
import com.ctg.ag.sdk.biz.AepDeviceManagementClient;

// 请求响应类
import com.ctg.ag.sdk.biz.aep_product_management.*;
import com.ctg.ag.sdk.biz.aep_device_management.*;
```

## 🌟 实际应用场景

### 在Vendor C科技项目中的应用
1. **物联网监控系统**
   - 信号覆盖主机内置式设备监控
   - 设备状态实时监控
   - 告警数据推送

2. **设备管理**
   - 产品型号管理
   - 设备注册与认证
   - 设备生命周期管理

3. **数据采集**
   - 传感器数据采集
   - 历史数据存储
   - 数据分析与报表

## 📚 学习资源

### 项目中的参考资料
- **SDK示例**：`/reference/267848_sdk/demo/`
- **API文档**：`/reference/267848_sdk/doc/`
- **实际应用**：`/protoType/aep-integration/`

### 官方文档
- 中国电信天翼物联官网
- AEP开发者平台文档
- SDK下载与示例

## 🔧 开发经验总结

### 常见问题
1. **签名认证失败**
   - 检查时间戳格式
   - 验证签名字符串构造
   - 确认App Key/Secret正确性

2. **网络连接问题**
   - 确认HTTPS连接
   - 检查防火墙设置
   - 验证域名解析

3. **权限问题**
   - 验证应用权限配置
   - 检查App状态
   - 确认API调用权限

### 最佳实践
1. **安全配置**
   - 使用环境变量存储认证信息
   - 避免硬编码敏感数据
   - 定期轮换密钥

2. **错误处理**
   - 完整的异常捕获
   - 详细的错误日志
   - 重试机制

3. **性能优化**
   - 连接池管理
   - 异步调用
   - 数据分页查询

## 📝 实践笔记

### 当前项目状态
- **App ID**: 267848
- **测试域名**: `10433748.api.ctwing.cn`
- **主要功能**: 产品管理SDK开发
- **当前进展**: 基础框架搭建完成，签名认证待优化

### 下一步计划
1. 解决签名认证问题
2. 完善设备管理功能
3. 集成数据查询服务
4. 添加消息订阅功能

---

**更新时间**: 2024-12-27
**学习来源**: Cloud-Monitoring-Platform项目实践
**技术栈**: Java 8+, HMAC-SHA1, REST API, IoT