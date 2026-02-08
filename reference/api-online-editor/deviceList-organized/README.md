# AEP设备列表API响应数据说明

## 概述

本文档描述了AEP (Application Enablement Platform) 设备管理API返回的设备列表数据结构，基于中国电信IoT平台的设备管理服务。

## 数据源

- **API端点**: `/aep_device_management/devices`
- **认证方式**: hmac-sha1
- **协议支持**: MQTT, T-Link, HTTP, TCP, LWM2M, JT/T808等
- **租户ID**: 10433748
- **产品ID**: 16857118

## 文件结构

```
deviceList-organized/
├── README.md                 # 本说明文档
├── raw-response.json        # 原始API响应数据
├── schema.json              # 数据结构定义
├── examples/                # 使用示例
│   ├── parser.js           # JavaScript解析示例
│   ├── filter.js           # 数据过滤示例
│   └── stats.js            # 统计分析示例
└── docs/                    # 详细文档
    ├── field-reference.md   # 字段参考
    ├── protocol-guide.md    # 协议指南
    └── status-codes.md      # 状态码说明
```

## 响应结构

### 基本格式

```json
{
  "code": "0",
  "msg": "ok",
  "result": {
    "pageNum": 1,
    "pageSize": 100,
    "total": 892,
    "list": [设备对象数组]
  }
}
```

### 分页参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| `pageNum` | Integer | 当前页码，从1开始 |
| `pageSize` | Integer | 每页记录数，最大100 |
| `total` | Integer | 设备总数 |

## 设备对象字段说明

### 基础标识字段

| 字段名 | 类型 | 必填 | 说明 | 示例值 |
|--------|------|------|------|--------|
| `deviceId` | String | 是 | 设备唯一标识符 | "16857118866877072647385" |
| `deviceName` | String | 是 | 设备名称/显示名称 | "866877072647385" |
| `deviceSn` | String | 是 | 设备序列号/编号 | "866877072647385" |
| `tenantId` | String | 是 | 租户标识符 | "10433748" |
| `productId` | Integer | 是 | 产品标识符 | 16857118 |

### 状态字段

| 字段名 | 类型 | 必填 | 说明 | 可能值 |
|--------|------|------|------|--------|
| `deviceStatus` | Integer | 是 | 设备状态 | 0:已注册, 1:已激活, 2:已注销 |
| `netStatus` | Integer | 否 | 网络在线状态 | 1:在线, 2:离线, null:未知 |
| `productProtocol` | Integer | 是 | 产品协议类型 | 见协议类型说明 |

### 时间字段

| 字段名 | 类型 | 必填 | 说明 | 格式 |
|--------|------|------|------|------|
| `createTime` | Long | 是 | 设备创建时间戳 | Unix时间戳(毫秒) |
| `updateTime` | Long | 否 | 最后更新时间戳 | Unix时间戳(毫秒) |
| `activeTime` | Long | 否 | 设备激活时间戳 | Unix时间戳(毫秒) |
| `logoutTime` | Long | 否 | 设备注销时间戳 | Unix时间戳(毫秒) |
| `onlineAt` | Long | 否 | 最后上线时间戳 | Unix时间戳(毫秒) |
| `offlineAt` | Long | 否 | 最后下线时间戳 | Unix时间戳(毫秒) |

### 版本信息

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `firmwareVersion` | String | 否 | 固件版本号 |

## 协议类型说明

| 值 | 协议名称 | 说明 |
|----|----------|------|
| 1 | T-LINK | T-Link协议 |
| 2 | MQTT | MQTT协议 |
| 3 | LWM2M | LWM2M协议 |
| 4 | TUP | TUP协议 |
| 5 | HTTP | HTTP协议 |
| 6 | JT/T808 | JT/T808协议 |
| 7 | TCP | TCP协议 |
| 8 | 私有TCP | 网关子设备协议 |
| 9 | 私有UDP | 网关子设备协议 |
| 10 | 网关产品MQTT | 网关产品协议 |
| 11 | 南向云 | 南向云协议 |

## 设备状态详解

### deviceStatus (设备状态)

- **0 (已注册)**: 设备已在平台注册，但尚未激活
- **1 (已激活)**: 设备已激活，可以正常通信
- **2 (已注销)**: 设备已注销，停止服务

### netStatus (网络状态)

- **1 (在线)**: 设备当前在线，可以接收指令
- **2 (离线)**: 设备当前离线，无法通信
- **null**: 网络状态未知或未初始化

## 数据分析摘要

基于当前数据集的分析结果：

### 设备总览
- **总设备数**: 892台
- **已激活设备**: ~85% (大部分设备状态为1)
- **在线设备**: ~60% (netStatus=1的设备)
- **主要协议**: MQTT (productProtocol=2)

### IMEI模式分析
- **主要前缀**: 866877072 (中国移动IMEI段)
- **次要前缀**: 866207075 (中国移动IMEI段)
- **设备命名**: 通常使用完整IMEI号作为设备名称

### 时间范围
- **最早创建**: ~2023年底
- **最新创建**: ~2024年12月
- **活跃周期**: 大部分设备在近期有上线记录

## 快速开始

### 1. 查看原始数据
```bash
cat raw-response.json | jq '.'
```

### 2. 统计设备状态
```bash
cat raw-response.json | jq '.result.list[] | .deviceStatus' | sort | uniq -c
```

### 3. 查找在线设备
```bash
cat raw-response.json | jq '.result.list[] | select(.netStatus == 1)'
```

## 相关文档

- [字段详细参考](docs/field-reference.md)
- [协议类型指南](docs/protocol-guide.md)
- [状态码说明](docs/status-codes.md)
- [API使用示例](examples/)

## 相关API接口

1. **QueryDeviceList** - 批量获取设备信息
2. **QueryDevice** - 获取单个设备详情
3. **CreateDevice** - 创建新设备
4. **UpdateDevice** - 更新设备信息
5. **DeleteDevice** - 删除设备

## 注意事项

1. **分页处理**: 单次查询最多返回100条记录
2. **时间戳格式**: 所有时间字段均为Unix时间戳(毫秒)
3. **空值处理**: 部分字段可能为null，需要做空值检查
4. **设备状态**: 只有已激活设备才能正常通信
5. **协议差异**: 不同协议的设备字段可能略有差异

## 错误处理

常见错误码及处理方式:

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查请求参数格式 |
| 401 | 认证失败 | 检查API Key和签名 |
| 404 | 设备不存在 | 确认设备ID是否正确 |
| 500 | 服务器异常 | 稍后重试或联系技术支持 |

---

*最后更新时间: 2024-12-29*
*文档版本: v1.0*
*数据源: AEP平台 - 租户10433748 - 产品16857118*