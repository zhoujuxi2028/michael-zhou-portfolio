# 字段详细参考

## 设备对象字段完整说明

### 基础标识字段

#### deviceId
- **类型**: String
- **必填**: 是
- **说明**: 设备在AEP平台的唯一标识符
- **格式**: 数字字符串，通常由产品ID+设备序列号组成
- **示例**: `"16857118866877072647385"`
- **用途**:
  - API调用中的设备定位
  - 设备管理操作的主键
  - 数据查询的索引

#### deviceName
- **类型**: String
- **必填**: 是
- **说明**: 设备的显示名称，用于用户界面展示
- **长度限制**: 通常1-100字符
- **示例**: `"866877072647385"`
- **命名规律**:
  - 通常使用IMEI号作为设备名称
  - 部分设备使用自定义名称
  - 建议使用有意义的标识符

#### deviceSn
- **类型**: String
- **必填**: 是
- **说明**: 设备序列号，设备的物理标识
- **格式**:
  - MQTT/T-Link/TCP/HTTP/JT808协议：自定义序列号
  - LWM2M协议：IMEI号（15位数字）
- **示例**: `"866877072647385"`
- **验证**: 如果是IMEI，需要通过Luhn校验算法

#### tenantId
- **类型**: String
- **必填**: 是
- **说明**: 租户标识符，表示设备所属的租户账户
- **示例**: `"10433748"`
- **用途**:
  - 多租户隔离
  - 权限控制
  - 计费统计

#### productId
- **类型**: Integer
- **必填**: 是
- **说明**: 产品标识符，定义设备类型和能力
- **示例**: `16857118`
- **关联**:
  - 产品决定了设备的协议类型
  - 产品定义了设备的数据模型
  - 产品配置影响设备行为

### 状态字段详解

#### deviceStatus
- **类型**: Integer
- **必填**: 是
- **说明**: 设备在平台的生命周期状态
- **可能值**:
  - `0`: 已注册 - 设备已在平台创建，但未激活
  - `1`: 已激活 - 设备已激活，可以正常通信
  - `2`: 已注销 - 设备已停用，无法通信
- **状态转换**:
  ```
  已注册(0) → 已激活(1) → 已注销(2)
      ↑                        ↓
      ←―――――― 重新注册 ←――――――
  ```

#### netStatus
- **类型**: Integer | null
- **必填**: 否
- **说明**: 设备当前的网络连接状态
- **可能值**:
  - `1`: 在线 - 设备当前连接到平台
  - `2`: 离线 - 设备当前未连接
  - `null`: 未知 - 状态未确定或从未连接
- **更新机制**:
  - 实时更新，基于心跳和通信状态
  - 离线判断通常有延迟（心跳超时）

#### productProtocol
- **类型**: Integer
- **必填**: 是
- **说明**: 设备使用的通信协议类型
- **协议映射**:

| 值 | 协议名称 | 说明 | 适用场景 |
|----|----------|------|----------|
| 1 | T-LINK | 中国电信自研协议 | 专用物联网设备 |
| 2 | MQTT | 消息队列遥测传输 | 通用IoT设备 |
| 3 | LWM2M | 轻量级M2M协议 | NB-IoT设备 |
| 4 | TUP | 电信统一协议 | 电信定制设备 |
| 5 | HTTP | 超文本传输协议 | Web设备 |
| 6 | JT/T808 | 交通部车载协议 | 车载终端 |
| 7 | TCP | 传输控制协议 | 自定义TCP设备 |
| 8 | 私有TCP | 网关子设备协议 | 网关下挂设备 |
| 9 | 私有UDP | 网关子设备协议 | 网关下挂设备 |
| 10 | 网关产品MQTT | 网关产品协议 | MQTT网关 |
| 11 | 南向云 | 南向云协议 | 云端接入设备 |

### 时间字段详解

所有时间字段均为Unix时间戳格式（毫秒），表示自1970年1月1日以来的毫秒数。

#### createTime
- **类型**: Long
- **必填**: 是
- **说明**: 设备在平台的创建时间
- **示例**: `1763523187257` (对应 2025-12-28 某时刻)
- **用途**:
  - 设备注册时间跟踪
  - 设备生命周期管理
  - 统计分析基准

#### updateTime
- **类型**: Long | null
- **必填**: 否
- **说明**: 设备信息最后修改时间
- **示例**: `1763525221000`
- **触发条件**:
  - 设备信息更新
  - 设备状态变更
  - 配置修改

#### activeTime
- **类型**: Long | null
- **必填**: 否
- **说明**: 设备首次激活时间
- **示例**: `1763525221000`
- **意义**:
  - 设备开始提供服务的时间
  - 激活率统计基础
  - 设备部署进度跟踪

#### logoutTime
- **类型**: Long | null
- **必填**: 否
- **说明**: 设备注销时间
- **示例**: `null` (大部分设备未注销)
- **用途**:
  - 设备生命周期结束标记
  - 设备回收管理
  - 历史数据保留策略

#### onlineAt
- **类型**: Long | null
- **必填**: 否
- **说明**: 设备最后一次上线时间
- **示例**: `1766754209912`
- **更新**:
  - 设备连接成功时更新
  - 重连时更新
  - 心跳恢复时更新

#### offlineAt
- **类型**: Long | null
- **必填**: 否
- **说明**: 设备最后一次下线时间
- **示例**: `1766753670143`
- **更新**:
  - 设备主动断开时更新
  - 心跳超时时更新
  - 网络异常时更新

### 版本字段

#### firmwareVersion
- **类型**: String | null
- **必填**: 否
- **说明**: 设备当前的固件版本号
- **示例**: `""`, `"v1.2.3"`, `null`
- **格式**:
  - 语义化版本号（推荐）
  - 自定义版本标识
  - 空字符串表示未设置
- **用途**:
  - 固件升级管理
  - 版本兼容性检查
  - 问题排查和统计

## 数据质量检查

### 必填字段检查
```javascript
function validateRequiredFields(device) {
    const required = ['deviceId', 'deviceName', 'tenantId', 'productId', 'deviceStatus', 'createTime', 'productProtocol'];
    const missing = required.filter(field =>
        device[field] === undefined || device[field] === null
    );
    return {
        valid: missing.length === 0,
        missing: missing
    };
}
```

### 数据逻辑检查
```javascript
function validateLogic(device) {
    const issues = [];

    // 时间逻辑检查
    if (device.activeTime && device.createTime && device.activeTime < device.createTime) {
        issues.push('激活时间不能早于创建时间');
    }

    if (device.logoutTime && device.activeTime && device.logoutTime < device.activeTime) {
        issues.push('注销时间不能早于激活时间');
    }

    // 状态逻辑检查
    if (device.deviceStatus === 1 && !device.activeTime) {
        issues.push('已激活设备应该有激活时间');
    }

    if (device.deviceStatus === 2 && !device.logoutTime) {
        issues.push('已注销设备应该有注销时间');
    }

    return {
        valid: issues.length === 0,
        issues: issues
    };
}
```

### IMEI校验
```javascript
function validateIMEI(imei) {
    if (!imei || !/^\d{15}$/.test(imei)) {
        return { valid: false, reason: 'IMEI格式错误' };
    }

    // Luhn校验算法
    let sum = 0;
    for (let i = 0; i < 14; i++) {
        let digit = parseInt(imei.charAt(i));
        if (i % 2 === 1) {
            digit *= 2;
            if (digit > 9) digit = Math.floor(digit / 10) + (digit % 10);
        }
        sum += digit;
    }

    const checkDigit = (10 - (sum % 10)) % 10;
    const valid = checkDigit === parseInt(imei.charAt(14));

    return {
        valid: valid,
        checkDigit: checkDigit,
        provided: parseInt(imei.charAt(14))
    };
}
```

## 字段使用最佳实践

### 1. 设备查询优化
- 使用`deviceId`进行精确查询
- 使用`deviceStatus`过滤有效设备
- 组合`netStatus`判断设备可用性

### 2. 时间字段处理
- 始终检查null值
- 使用适当的时区转换
- 考虑服务器时间与本地时间差异

### 3. 状态判断逻辑
```javascript
function isDeviceUsable(device) {
    return device.deviceStatus === 1 && device.netStatus === 1;
}

function getDeviceHealth(device) {
    if (device.deviceStatus !== 1) return 'inactive';
    if (device.netStatus === 1) return 'healthy';

    const lastActivity = device.onlineAt || device.updateTime;
    const hoursSinceActivity = lastActivity ?
        (Date.now() - lastActivity) / (1000 * 60 * 60) : Infinity;

    if (hoursSinceActivity <= 24) return 'warning';
    return 'critical';
}
```

### 4. 协议特定处理
```javascript
function getProtocolSpecificFields(device) {
    const protocol = device.productProtocol;

    switch (protocol) {
        case 3: // LWM2M
            return {
                requiresIMEI: true,
                supportsRemoteConfig: true,
                batteryOptimized: true
            };
        case 2: // MQTT
            return {
                requiresIMEI: false,
                supportsRemoteConfig: true,
                realTimeCapable: true
            };
        case 6: // JT/T808
            return {
                vehicleSpecific: true,
                locationTracking: true,
                regulatoryCompliance: 'JT/T808'
            };
        default:
            return { standard: true };
    }
}
```

---

*最后更新: 2024-12-29*