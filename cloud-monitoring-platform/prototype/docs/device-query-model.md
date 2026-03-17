# 电信IOT设备查询数据模型设计

## 核心设备信息字段

基于vendor-b/zc_backend项目分析，提取查询功能需要的关键字段：

### Device 设备基础信息
```java
public class Device {
    private String id;              // 设备ID
    private String deviceName;      // 设备名称
    private String lbsId;          // 设备编码ID（基站ID）
    private String iotDeviceId;    // IOT平台36位设备ID
    private String status;         // 设备状态（online/offline）
    private Double longitude;      // 经度
    private Double latitude;       // 纬度
    private String regionId;       // 区域ID
    private String companyId;      // 厂商ID
    private Date lastUpdateTime;   // 最后更新时间
}
```

### DeviceQueryRequest 查询请求
```java
public class DeviceQueryRequest {
    private String deviceName;     // 按设备名称查询
    private String lbsId;         // 按基站ID查询
    private String status;        // 按状态查询
    private String regionId;      // 按区域查询
    private Integer page;         // 分页页码
    private Integer size;         // 分页大小
}
```

### DeviceResponse 查询响应
```java
public class DeviceResponse {
    private Boolean success;      // 查询是否成功
    private String message;       // 响应消息
    private List<Device> data;    // 设备列表数据
    private Integer total;        // 总记录数
    private Integer page;         // 当前页码
}
```

## 电信IOT平台API映射

### 认证接口
- **URL**: `/iocm/app/sec/v1.1.0/login`
- **用途**: 获取访问Token

### 设备查询接口
- **URL**: `/iocm/app/dm/v1.4.0/devices`
- **用途**: 查询设备列表
- **参数**: pageNo, pageSize, deviceId等

## 简化查询功能列表

1. **设备列表查询** - 支持分页和基础筛选
2. **设备详情查询** - 根据设备ID查询单个设备
3. **基站设备查询** - 根据lbsId查询设备
4. **状态统计查询** - 统计在线/离线设备数量

不包含：设备注册、设备命令、设备控制等功能。