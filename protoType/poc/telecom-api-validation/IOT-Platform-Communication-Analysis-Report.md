# 中国电信物联网平台通信规则分析报告

**报告时间**: 2024-12-24
**分析范围**: 6个众成科技项目与中国电信IoT平台的通信模式
**平台地址**: `https://device.api.ct10649.com:8743`

## 1. 执行摘要

本报告分析了众成科技6个项目与中国电信物联网平台的通信规则，发现了统一的通信架构和几个关键差异点。所有项目都采用HTTPS双向认证，但在认证凭据、API版本和具体实现细节上存在差异。

### 1.1 核心发现
- **5个项目**使用生产环境 (`https://device.api.ct10649.com:8743`)
- **1个项目**使用开发环境 (`https://develop.api.ct10649.com:8743`) - 5G兼容需求项目
- **认证凭据分化**: 5个项目共享数据库配置凭据，1个项目(toyou)使用独立凭据
- API版本存在v1.1.0和v1.4.0两个主要版本
- 统一使用双向SSL证书认证
- **开发组织**: 公司A开发5个项目+主项目，公司B开发toyou项目

## 2. 项目概述

### 2.1 分析的6个项目
1. **信号覆盖主机内置式设备监控系统软件开发** (主项目)
2. **toyou项目** (zc_backend)
3. **众成验收-物联网云监控平台**
4. **众成验收-信号覆盖主机内置式设备监控系统**
5. **众成验收-信号覆盖设备安装及租赁系统**
6. **众成验收-5G兼容需求**

### 2.2 技术栈统计
- **后端**: Spring Boot 2.x + Java 8 + MyBatis + MySQL
- **IoT平台**: 中国电信物联网开放平台 (AEP)
- **通信协议**: HTTPS + RESTful API
- **认证方式**: 双向SSL证书 + APP KEY/SECRET

## 3. 服务器环境分析

### 3.1 生产环境配置 (5个项目)
```
服务器地址: https://device.api.ct10649.com:8743
使用项目:
- 主项目 (信号覆盖主机内置式设备监控系统)
- toyou项目
- 众成验收-物联网云监控平台
- 众成验收-信号覆盖主机内置式设备监控系统
- 众成验收-信号覆盖设备安装及租赁系统
```

### 3.2 开发环境配置 (1个项目)
```
服务器地址: https://develop.api.ct10649.com:8743
使用项目:
- 众成验收-5G兼容需求 (公司A开发)

配置文件: 众成验收/5G兼容需求/源码/source/src/main/resources/application.yml
配置内容:
  # iotplat:
  #   base-url: https://device.api.ct10649.com:8743  (生产环境，已注释)
    base-url: https://develop.api.ct10649.com:8743   (开发环境，当前使用)
特殊说明: 该项目专门配置了开发测试环境，用于5G兼容性测试
```

## 4. 认证凭据统计

### 4.1 统一认证配置 (6个项目)
```
APP_KEY: "06B39HtOpSefmdIduE8YvFkxDBQa"
APP_SECRET: "85wGRsmwwn0uCpNBT_8_tntRy8a"

数据来源: 主项目数据库配置表 (t_configure)
配置项:
- iotplat_appid = '06B39HtOpSefmdIduE8YvFkxDBQa'
- iotplat_secret = '85wGRsmwwn0uCpNBT_8_tntRy8a'
```

### 4.2 toyou项目认证配置 (公司B开发) - 修正版
```
toyou项目APP_KEY: "ed5a4f1fcb364575a614f70d52a5a1ac"
toyou项目APP_SECRET: "f8a8df37f85a4b6892a7c058b5bfb655"

项目: toyou/zc_backend (公司B开发)
源码配置: toyou/zc_backend/nmps-impl/src/main/java/cn/com/git/nmps/impl/iot/utils/Constant.java
部署配置: /host-machine/jsty_zhongcheng/config/application-dev.yml
服务器地址: https://device.api.ct10649.com:8743 (生产环境)

重要发现: 虽然源码中硬编码了认证凭据，但实际部署使用了电信官方SDK
SDK文件位置: /host-machine/jsty_zhongcheng/lib/
- ctg-ag-sdk-core-2.8.0-20230508.100604-1.jar
- ag-sdk-biz-267848.tar.gz-20230830.093551-SNAPSHOT.jar
```

## 5. API接口版本分析

### 5.1 设备管理API
| 项目      | API版本  | 接口路径                                |
| ------- | ------ | ----------------------------------- |
| 主项目     | v1.1.0 | /iocm/app/dm/v1.1.0/devices         |
| toyou项目 | v1.1.0 | /iocm/app/dm/v1.1.0/devices         |
| 众成验收项目  | v1.1.0 | /iocm/app/dm/v1.1.0/devices         |
| POC测试项目 | v1.4.0 | /iocm/app/cmd/v1.4.0/deviceCommands |

### 5.2 认证API (统一)
```
接口路径: /iocm/app/sec/v1.1.0/login
使用版本: v1.1.0 (所有项目)
认证方式: Bearer Token + APP KEY Header
```

### 5.3 命令下发API
```
接口路径: /iocm/app/cmd/v1.4.0/deviceCommands
说明: 主要用于设备命令下发和控制
支持的命令类型: 周期设置、电源控制、频段设置等
```

## 6. SSL证书配置

### 6.1 证书文件统一配置
```
客户端证书: cert/outgoing.CertwithKey.pkcs12
CA证书: cert/ca.jks
证书密码:
- 客户端证书密码: "IoM@1234"
- CA证书密码: "Huawei@123"
```

### 6.2 证书路径差异
```
开发环境路径: /nmps-run/src/main/resources/cert/
生产环境路径: cert/ (相对路径)

代码配置: cn.com.git.nmps.impl.iot.utils.Constant.java
```

## 7. 通信协议细节

### 7.1 HTTP请求头配置
```
必需头信息:
- app_key: APP_KEY值
- Authorization: "Bearer " + access_token
- Content-Type: application/json

请求方法:
- 登录: POST /iocm/app/sec/v1.1.0/login
- 设备查询: GET /iocm/app/dm/v1.1.0/devices/{deviceId}
- 设备注册: POST /iocm/app/reg/v1.1.0/devices
- 命令下发: POST /iocm/app/cmd/v1.4.0/deviceCommands
```

### 7.2 错误处理模式
```
常见错误码:
- 200: 请求成功
- 404: Application not found (APP KEY无效)
- 401: 认证失败
- 400: 参数错误

错误响应格式:
{
  "statusCode": 404,
  "message": "Application not found"
}
```

## 8. 设备标识映射规则

### 8.1 设备ID类型
```
内部设备ID (lbs_id):
示例: "868681043922820"
说明: 系统内部使用的设备编号

电信平台设备ID (iot_device_id):
示例: "f8727760-b74f-423b-98e9-aa77f49b0d6e"
说明: 电信IoT平台生成的UUID格式设备ID
```

### 8.2 设备注册流程
```
1. 使用lbs_id调用设备注册API
2. 电信平台返回iot_device_id
3. 系统存储两个ID的映射关系
4. 后续API调用使用iot_device_id

数据库表: t_device (pet_lbs)
关键字段: lbs_id, iot_device_id
```

## 9. 业务功能差异分析

### 9.1 主项目特性
- 完整的设备生命周期管理
- 支持多种设备型号 (ZC_Repeat01-05, LTE系列)
- 集成工单流程和审批机制
- 完整的告警和恢复机制

### 9.2 toyou项目特性
- 简化的设备管理功能
- 更注重数据展示和分析
- 地理位置服务集成
- 批量设备操作功能

### 9.3 5G兼容需求项目特性
- 使用开发服务器环境
- 支持设备固件升级功能
- LTE设备专用API调用
- 文件传输和CRC校验机制

## 10. 配置管理模式

### 10.1 数据库驱动配置 (主流模式)
```java
// Spring Boot配置注入模式
@Value("${iotplat.appid}")
private String appId;

@Value("${iotplat.secret}")
private String secret;

// 数据库配置表查询模式
TDictPO config = tDictMapper.queryByDictCode("iotplat_appid", "0");
String appKey = config.getItemValue();
```

### 10.2 硬编码配置 (遗留模式)
```java
// 直接在代码中定义 (不推荐)
public static final String APPID = "06B39HtOpSefmdIduE8YvFkxDBQa";
public static final String SECRET = "85wGRsmwwn0uCpNBT_8_tntRy8a";
```

## 11. 问题分析与建议

### 11.1 当前存在的问题
1. **认证凭据失效**: 无论新旧APP KEY都返回"Application not found"错误
2. **API版本不一致**: 混用v1.1.0和v1.4.0版本可能导致兼容性问题
3. **配置管理分散**: 硬编码和数据库配置并存
4. **错误处理不完善**: 部分项目对API错误处理不充分

### 11.2 优化建议
1. **统一配置管理**: 全部迁移到数据库配置模式
2. **API版本标准化**: 统一使用最新稳定版本
3. **完善错误处理**: 增加详细的错误码处理逻辑
4. **证书管理**: 建立证书更新和管理流程
5. **环境隔离**: 明确开发、测试、生产环境配置

## 12. 技术架构总结

### 12.1 通信架构图
```
众成科技应用层
    ↓ HTTPS + 双向SSL认证
中国电信IoT平台 (AEP)
    ↓ 设备通信协议
IoT设备层 (信号覆盖设备)
```

### 12.2 核心组件
- **认证模块**: APP KEY/SECRET + JWT Token
- **设备管理模块**: 设备CRUD + 状态监控
- **命令控制模块**: 远程控制 + 参数配置
- **数据上报模块**: 设备状态 + 告警信息

## 13. 结论

6个项目在与中国电信IoT平台的通信上采用了基本一致的技术架构，主要差异体现在：

### 13.1 环境差异
- **5个项目**使用生产环境 (`https://device.api.ct10649.com:8743`)
  - 主项目 + 4个众成验收项目 (公司A开发)
  - toyou项目 (公司B开发)
- **1个项目**使用开发环境 (`https://develop.api.ct10649.com:8743`)
  - 众成验收-5G兼容需求项目 (公司A开发)

### 13.2 认证凭据差异
- **5个项目**使用统一认证凭据 (来自数据库配置)
- **1个项目** (toyou) 使用独立认证凭据 (硬编码配置)

### 13.3 开发组织差异
- **公司A项目**: 众成验收相关的5个项目 + 主项目，技术栈和配置相对统一
- **公司B项目**: toyou项目，独立开发和配置

技术实现层面已经比较成熟和统一，当前的主要挑战是认证凭据的有效性问题，需要与电信方面确认不同APP KEY的状态和权限配置。

---

**报告生成时间**: 2024-12-24
**分析工具**: Claude Code
**数据来源**: 项目源代码静态分析