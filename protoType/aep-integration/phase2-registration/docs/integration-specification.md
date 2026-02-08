# Phase2 AEP SDK设备管理集成规范

## 📋 文档信息
- **版本**: v2.0 (重新定位为设备管理)
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-26
- **负责人**: 云监控平台技术团队
- **状态**: ✅ 基于真实测试验证完成

## 🎯 集成目标

基于Phase1.1-export成功的AEP SDK集成模式，扩展**设备管理功能**的集成规范，确保API调用的正确性、稳定性和可维护性。本规范基于真实AEP环境的测试验证结果。

## 🔧 AEP SDK集成分析

### 1. SDK版本与依赖

**当前使用的SDK版本** (继承自Phase1.1):
```xml
<!-- Phase2复用Phase1.1的成功配置 -->
<dependency>
    <groupId>com.ctg.ag</groupId>
    <artifactId>ag-sdk-biz</artifactId>
    <version>267848.tar.gz-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.ctg.ag</groupId>
    <artifactId>ctg-ag-sdk-core</artifactId>
    <version>2.9.0-SNAPSHOT</version>
</dependency>
```

**已验证可用的SDK类** (基于真实测试验证):
```java
// Phase1.1已成功使用的导入 (保持不变)
import com.ctg.ag.sdk.biz.AepProductManagementClient; // ✅ 用于产品查询
import com.ctg.ag.sdk.biz.AepDeviceManagementClient;  // ✅ 设备管理主客户端

// Phase2设备管理所需导入 (已验证)
import com.ctg.ag.sdk.biz.aep_device_management.CreateDeviceRequest;   // ✅ 已测试成功
import com.ctg.ag.sdk.biz.aep_device_management.CreateDeviceResponse;  // ✅ 已测试成功
import com.ctg.ag.sdk.biz.aep_device_management.UpdateDeviceRequest;   // ✅ 已测试成功
import com.ctg.ag.sdk.biz.aep_device_management.UpdateDeviceResponse;  // ✅ 已测试成功
import com.ctg.ag.sdk.biz.aep_device_management.DeleteDeviceRequest;   // ✅ 接口已确认
import com.ctg.ag.sdk.biz.aep_device_management.DeleteDeviceResponse;  // ✅ 接口已确认
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceRequest;    // ✅ Phase1.1继承
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceResponse;   // ✅ Phase1.1继承
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListRequest;  // ✅ Phase1.1继承
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListResponse; // ✅ Phase1.1继承
```

### 2. 客户端初始化模式 (基于真实测试验证)

**Phase1.1成功模式** (保持不变):
```java
// 产品管理客户端 (Phase1.1已验证的工作模式)
this.productClient = AepProductManagementClient.newClient()
    .appKey(config.getAppKey())
    .appSecret(config.getAppSecret())
    .scheme(Scheme.HTTPS)
    .build();
```

**Phase2设备管理初始化** ✅ (已验证):
```java
// 设备管理客户端 (DeviceManagementTest.java验证成功)
this.deviceClient = AepDeviceManagementClient.newClient()
    .appKey(appKey)           // ✅ 从环境变量获取
    .appSecret(appSecret)     // ✅ 从环境变量获取
    .scheme(Scheme.HTTPS)     // ✅ 必须使用HTTPS
    .build();
```

**关键验证结果**:
- ✅ **连接成功**: 可正常连接到真实AEP环境 (10433748.api.ctwing.cn)
- ✅ **认证通过**: 使用真实APP_KEY和APP_SECRET认证成功
- ✅ **API调用**: 成功执行设备创建、更新、查询操作
- ✅ **响应解析**: 正确解析AEP标准响应格式

**重要注意事项**:
- ✅ **不使用** `.host()` 方法 (已确认在SDK中不可用)
- ✅ **使用** 标准的 `appKey` 和 `appSecret` 认证
- ✅ **使用** `Scheme.HTTPS` 确保安全连接
- ✅ 复用Phase1.1的客户端实例避免重复初始化

## 📖 设备管理API集成规范 (基于真实测试验证)

### 1. 核心参数设置模式 ✅

**关键发现**: AEP SDK严格区分三种参数类型，必须使用正确的设置方法:

#### 1.1 MasterKey认证 (头部参数)
```java
// ✅ 正确方式 - 使用专用方法 (DeviceManagementTest.java已验证)
request.setParamMasterKey("7f1417fbecad4934bdcfe301c302fa3f");

// ❌ 错误方式 - 通用setParam不适用于头部参数
// request.setParam("MasterKey", masterKey); // 会导致认证失败
```

#### 1.2 URL参数设置
```java
// ✅ 正确方式 - 查询参数 (已验证)
request.setParam("deviceId", "16980130866877072647500");
request.setParam("productId", 16980130L);
request.setParam("pageNow", 1);
request.setParam("pageSize", 100);
```

#### 1.3 请求体设置 (JSON格式)
```java
// ✅ 正确方式 - 构建JSON并设置为字节数组 (已验证)
String requestBodyJson = String.format(
    "{\"deviceName\":\"%s\",\"deviceSn\":\"%s\",\"operator\":\"%s\",\"productId\":%d}",
    deviceName, deviceSn, operator, productId
);
request.setBody(requestBodyJson.getBytes("UTF-8"));

// ❌ 错误方式 - setParam不适用于请求体字段
// request.setParam("deviceName", deviceName); // 报错: Param deviceName not available
```

### 2. CreateDevice API集成规范 ✅

**真实测试验证的调用模式**:
```java
// DeviceManagementTest.java 验证成功的实现
CreateDeviceRequest request = new CreateDeviceRequest();

// 1. 设置MasterKey认证
request.setParamMasterKey(masterKey);

// 2. 构建JSON请求体 (必填字段)
String requestBodyJson = String.format(
    "{" +
    "\"deviceName\":\"%s\"," +
    "\"deviceSn\":\"%s\"," +
    "\"operator\":\"%s\"," +
    "\"productId\":%d" +
    "}",
    deviceName, deviceSn, operator, productId
);

// 3. 设置请求体
request.setBody(requestBodyJson.getBytes("UTF-8"));

// 4. 调用SDK API
CreateDeviceResponse response = deviceClient.CreateDevice(request);
```

**验证成功的参数**:
```java
// 基于真实测试的参数规范
"deviceName": "TestDevice_1769392253866",    // ✅ 必填 - 设备名称
"deviceSn": "TEST_SN_1769392253868",         // ✅ 必填 - MQTT协议设备编号
"operator": "system_test",                    // ✅ 必填 - 操作者
"productId": 16980130                         // ✅ 必填 - 所属产品ID
```

**真实测试响应** (已验证):
```json
{
  "code": 0,
  "msg": "ok",
  "result": {
    "deviceSn": "TEST_SN_1769392253868",
    "deviceId": "16980130TEST_SN_1769392253868",
    "deviceName": "TestDevice_1769392253866",
    "tenantId": "10433748",
    "productId": 16980130,
    "token": "sBJMjBkssG1SPUpdfwCLgJCkwlMCeKLH1hTDLFdxWE8"
  }
}
```

### 3. UpdateDevice API集成规范 ✅

**真实测试验证的调用模式**:
```java
// DeviceManagementTest.java 验证成功的实现
UpdateDeviceRequest request = new UpdateDeviceRequest();

// 1. 设置MasterKey认证
request.setParamMasterKey(masterKey);

// 2. 设置URL参数
request.setParam("deviceId", "16980130866877072647500");

// 3. 构建JSON请求体
String requestBodyJson = String.format(
    "{\"deviceName\":\"%s\",\"operator\":\"%s\",\"productId\":%d}",
    newDeviceName, operator, productId
);
request.setBody(requestBodyJson.getBytes("UTF-8"));

// 4. 调用SDK API
UpdateDeviceResponse response = deviceClient.UpdateDevice(request);
```

**验证成功的更新字段**:
```java
// 可更新的字段 (已验证)
"deviceName": "Updated_Device_1769392253934", // ✅ 设备名称可更新
"operator": "system_test",                     // ✅ 操作者必填
"productId": 16980130                          // ✅ 产品ID必填
```

### 4. DeleteDevice API集成规范 ✅

**基于AEP官方文档的调用模式** (接口已确认):
```java
DeleteDeviceRequest request = new DeleteDeviceRequest();

// 1. 设置MasterKey认证
request.setParamMasterKey(masterKey);

// 2. 设置URL参数
request.setParam("productId", 16980130L);
request.setParam("deviceIds", "id1,id2,id3"); // 批量删除，最多200个

// 3. 调用SDK API
DeleteDeviceResponse response = deviceClient.DeleteDevice(request);
```

**批量删除支持** (已确认):
```java
// 批量删除示例
request.setParam("deviceIds",
    "16980130TEST_SN_1769354052974,16980130TEST_SN_1769392253868");
```

## 🔍 错误处理与最佳实践 (基于真实测试经验)

### 1. 常见错误及解决方案

#### 1.1 MasterKey认证错误 ❌
```json
{"error_code":"400","error_desc":"Required head parameter 'MasterKey' can not be null"}
```
**原因**: 未正确设置MasterKey头部参数
**解决**: 使用 `request.setParamMasterKey(masterKey)`

#### 1.2 参数方法错误 ❌
```
Exception: Param deviceName not available
```
**原因**: 对请求体字段错误使用了`setParam()`方法
**解决**: 请求体字段使用 `request.setBody(json.getBytes())`

#### 1.3 JSON格式错误 ❌
```json
{"code": 8802, "msg": "参数解析失败", "result": null}
```
**原因**: 请求体JSON格式不正确
**解决**: 确保JSON语法正确，字段名用双引号包围

### 2. 响应处理标准 ✅

**AEP标准响应格式** (已验证):
```java
// 成功响应检查
if (response != null && response.getBody() != null) {
    String result = new String(response.getBody(), "UTF-8");

    // AEP成功响应特征
    if (result.contains("\"code\":0")) {
        // 处理成功结果
        return parseSuccessResult(result);
    } else {
        // 处理AEP业务错误
        return parseErrorResult(result);
    }
} else {
    throw new AepClientException("Empty response from AEP API");
}
```

### 3. 最佳实践总结

#### 3.1 参数设置最佳实践
```java
// 推荐的标准操作流程
public void executeDeviceOperation() {
    SomeDeviceRequest request = new SomeDeviceRequest();

    // 1. 设置认证头部 (必须首先设置)
    request.setParamMasterKey(masterKey);

    // 2. 设置URL参数 (查询参数)
    if (deviceId != null) {
        request.setParam("deviceId", deviceId);
    }
    request.setParam("productId", productId);

    // 3. 设置请求体 (JSON数据)
    if (needsRequestBody) {
        String json = buildRequestJson(parameters);
        request.setBody(json.getBytes("UTF-8"));
    }

    // 4. 执行请求
    SomeDeviceResponse response = deviceClient.SomeOperation(request);
}
```

#### 3.2 配置管理最佳实践
```java
// 环境变量配置 (推荐)
public static AepDeviceManagementClient createClient() {
    String appKey = System.getenv("AEP_APP_KEY");
    String appSecret = System.getenv("AEP_APP_SECRET");

    if (appKey == null || appSecret == null) {
        throw new RuntimeException("Missing AEP credentials in environment");
    }

    return AepDeviceManagementClient.newClient()
        .appKey(appKey)
        .appSecret(appSecret)
        .scheme(Scheme.HTTPS)
        .build();
}
```

## 📊 验证状态总结

### ✅ 已验证功能
1. **设备查询** - QueryDevice/QueryDeviceList (Phase1.1继承)
2. **设备创建** - CreateDevice接口调用成功
3. **设备更新** - UpdateDevice接口调用成功
4. **MasterKey认证** - 头部参数设置方法正确
5. **JSON请求体** - 请求体设置方法正确
6. **错误处理** - 常见错误识别和解决

### 📋 接口已确认
1. **设备删除** - DeleteDevice接口规范已确认
2. **批量删除** - 最多支持200个设备ID
3. **参数验证** - AEP官方参数要求已确认

### 🎯 Phase2开发就绪
- ✅ 技术架构验证完成
- ✅ API调用模式确认
- ✅ 错误处理经验总结
- ✅ 最佳实践制定完成

---

**规范状态**: ✅ 基于真实环境测试验证完成
**适用范围**: Phase2设备管理模块开发、DeviceCleanupTool开发
**最后更新**: 2026-01-26

**响应解析模式**:
```java
// 基于Phase1.1的响应处理模式
CreateProductResponse response = productClient.CreateProduct(request);

if (response != null) {
    // 获取响应内容 (复用Phase1.1的处理方式)
    String responseBody = response.toString();

    // 解析关键字段
    Long productId = extractProductId(response);
    String masterKey = extractMasterKey(response);

    // 构建标准返回格式
    return buildSuccessResponse("createProduct", productId, masterKey, responseBody);
} else {
    throw new AepClientException("Empty response from AEP CreateProduct API");
}
```

**关键字段提取方法**:
```java
// 基于AEP响应格式的字段提取
private Long extractProductId(CreateProductResponse response) {
    try {
        String responseStr = response.toString();
        // 使用JSON解析或正则表达式提取productId
        // 参考Phase1.1的extractJsonIntField方法
        return extractJsonLongField(responseStr, "productId");
    } catch (Exception e) {
        LogManager.getInstance().warning("产品创建", "AepClientManager",
            "无法提取productId: " + e.getMessage());
        return null;
    }
}
```

## 📝 UpdateProduct API集成规范

### 1. 更新请求构建

```java
// UpdateProduct调用模式
UpdateProductRequest request = new UpdateProductRequest();

// 必填参数
request.setProductId(productId);           // 必填，要更新的产品ID

// 可更新的字段
request.setProductName(newProductName);    // 可选，新的产品名称
request.setDescription(newDescription);    // 可选，新的产品描述
// 其他可更新字段待SDK文档确认

UpdateProductResponse response = productClient.UpdateProduct(request);
```

### 2. 更新字段验证

```java
// 更新参数验证规则
private void validateUpdateParams(Map<String, Object> params) {
    // productId必填验证
    requireNonNull(params, "productId", "产品ID不能为空");

    // 至少有一个可更新字段
    boolean hasUpdateField = params.containsKey("productName") ||
                           params.containsKey("description") ||
                           params.containsKey("industryId");

    if (!hasUpdateField) {
        throw new AepClientException("至少需要一个可更新的字段");
    }

    // 字段格式验证
    if (params.containsKey("productName")) {
        validateProductName((String) params.get("productName"));
    }
}
```

## 🗑️ DeleteProduct API集成规范

### 1. 删除请求构建

```java
// DeleteProduct调用模式
DeleteProductRequest request = new DeleteProductRequest();

// 必填参数
request.setProductId(productId);           // 必填，要删除的产品ID

// 可选参数
boolean forceDelete = (Boolean) params.getOrDefault("force", false);
if (forceDelete) {
    request.setForceDelete(true);          // 强制删除（忽略设备依赖）
}

DeleteProductResponse response = productClient.DeleteProduct(request);
```

### 2. 删除前置检查

```java
// 删除操作的安全检查
private void validateDeleteParams(Map<String, Object> params) {
    // productId必填验证
    requireNonNull(params, "productId", "产品ID不能为空");

    Long productId = ((Number) params.get("productId")).longValue();
    boolean force = (Boolean) params.getOrDefault("force", false);

    if (!force) {
        // 非强制删除时，检查是否存在关联设备
        checkProductDependencies(productId);
    }
}

private void checkProductDependencies(Long productId) {
    try {
        // 复用Phase1.1的设备查询功能检查依赖
        Map<String, Object> deviceParams = new HashMap<>();
        deviceParams.put("productId", productId);
        deviceParams.put("pageSize", 1);  // 只检查是否存在设备

        String deviceResult = queryDevices(deviceParams);

        if (hasDevices(deviceResult)) {
            throw new AepClientException(DELETE_HAS_DEVICES,
                "产品下存在设备，无法删除。使用--force参数强制删除。");
        }
    } catch (Exception e) {
        LogManager.getInstance().warning("删除检查", "AepClientManager",
            "检查产品依赖时出错: " + e.getMessage());
        // 检查失败时允许继续删除，但记录警告
    }
}
```

## 🔐 认证与安全集成

### 1. 认证信息管理

**复用Phase1.1的认证体系**:
```java
// 直接继承ExportConfig的认证管理
public class AepClientManager {
    private final ExportConfig config;

    public AepClientManager(ExportConfig config) {
        this.config = validateConfig(config);  // 复用Phase1.1验证
    }

    // 复用Phase1.1的配置验证方法
    private ExportConfig validateConfig(ExportConfig config) {
        // 完全复用Phase1.1的验证逻辑 (第78-92行)
        if (config == null) {
            throw new AepClientException("Config cannot be null");
        }
        if (config.getAppKey() == null || config.getAppKey().trim().isEmpty()) {
            throw new AepClientException("AppKey is missing or invalid");
        }
        // ... 其他验证逻辑保持相同
        return config;
    }
}
```

### 2. 敏感信息保护

```java
// 日志中的敏感信息脱敏 (继承Phase1.1模式)
private void logOperationStart(String operation, Map<String, Object> params) {
    Map<String, Object> safeParams = new HashMap<>(params);

    // 移除或脱敏敏感字段
    if (safeParams.containsKey("masterKey")) {
        safeParams.put("masterKey", "***");
    }

    LogManager.getInstance().info("产品注册", "AepClientManager",
        String.format("开始%s操作 - 参数: %s", operation, safeParams.toString()));
}
```

## ⚡ 性能与可靠性

### 1. 重试机制设计

```java
// 重试机制 (扩展Phase1.1的错误处理)
private <T> T executeWithRetry(String operation, Supplier<T> apiCall) {
    int maxRetries = 3;
    long baseDelayMs = 1000;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            return apiCall.get();
        } catch (Exception e) {
            if (attempt == maxRetries || !isRetryableException(e)) {
                throw e;
            }

            long delay = baseDelayMs * (1L << (attempt - 1)); // 指数退避
            LogManager.getInstance().warning("产品注册", "AepClientManager",
                String.format("%s操作失败，第%d次重试，等待%dms", operation, attempt, delay));

            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new AepClientException("操作被中断");
            }
        }
    }

    return null; // 不会到达此处
}

private boolean isRetryableException(Exception e) {
    // 判断是否为可重试的异常（网络超时、临时服务不可用等）
    String message = e.getMessage().toLowerCase();
    return message.contains("timeout") ||
           message.contains("connection") ||
           message.contains("503") ||
           message.contains("502");
}
```

### 2. 操作超时控制

```java
// 基于环境变量的超时配置
private static final long DEFAULT_TIMEOUT_MS = 30000; // 30秒

private long getOperationTimeout() {
    String timeoutStr = System.getenv("AEP_OPERATION_TIMEOUT");
    if (timeoutStr != null && !timeoutStr.trim().isEmpty()) {
        try {
            return Long.parseLong(timeoutStr);
        } catch (NumberFormatException e) {
            LogManager.getInstance().warning("配置解析", "AepClientManager",
                "无效的超时配置: " + timeoutStr + ", 使用默认值: " + DEFAULT_TIMEOUT_MS);
        }
    }
    return DEFAULT_TIMEOUT_MS;
}
```

## 📊 监控与审计

### 1. 操作审计日志

```java
// 操作审计规范 (扩展Phase1.1的日志体系)
private void auditOperation(String operation, Map<String, Object> params,
                          boolean success, String errorMessage, long durationMs) {

    StringBuilder auditLog = new StringBuilder();
    auditLog.append("操作审计 - ");
    auditLog.append("类型: ").append(operation);
    auditLog.append(", 结果: ").append(success ? "成功" : "失败");
    auditLog.append(", 耗时: ").append(durationMs).append("ms");

    if (params.containsKey("productName")) {
        auditLog.append(", 产品: ").append(params.get("productName"));
    }
    if (params.containsKey("productId")) {
        auditLog.append(", ID: ").append(params.get("productId"));
    }

    if (!success && errorMessage != null) {
        auditLog.append(", 错误: ").append(errorMessage);
    }

    LogManager.getInstance().audit("产品注册", "AepClientManager", auditLog.toString());
}
```

### 2. 性能监控

```java
// 操作性能监控
public class OperationMetrics {
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong successOperations = new AtomicLong(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);

    public void recordOperation(String operation, boolean success, long durationMs) {
        totalOperations.incrementAndGet();
        if (success) {
            successOperations.incrementAndGet();
        }
        totalDurationMs.addAndGet(durationMs);

        // 记录到日志用于监控
        LogManager.getInstance().debug("性能监控", "AepClientManager",
            String.format("操作: %s, 成功: %s, 耗时: %dms", operation, success, durationMs));
    }

    public Map<String, Object> getMetrics() {
        long total = totalOperations.get();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalOperations", total);
        metrics.put("successOperations", successOperations.get());
        metrics.put("successRate", total > 0 ? (double) successOperations.get() / total : 0.0);
        metrics.put("avgDurationMs", total > 0 ? totalDurationMs.get() / total : 0);
        return metrics;
    }
}
```

## 🧪 集成测试规范

### 1. SDK方法验证测试

```java
@Test
public void testCreateProductRequestMethods() {
    // 验证CreateProductRequest的setter方法存在且可用
    CreateProductRequest request = new CreateProductRequest();

    // 测试必需方法
    assertDoesNotThrow(() -> request.setProductName("测试产品"));
    assertDoesNotThrow(() -> request.setProductType(1));
    assertDoesNotThrow(() -> request.setDataFormat(1));

    // 测试可选方法
    assertDoesNotThrow(() -> request.setDescription("测试描述"));
    assertDoesNotThrow(() -> request.setIndustryId(1));
}

@Test
public void testProductClientInitialization() {
    // 验证客户端初始化方法与Phase1.1一致
    ExportConfig config = ExportConfig.fromEnvironment();
    AepClientManager client = new AepClientManager(config);

    assertNotNull(client);
    assertTrue(client.isInitialized());
}
```

### 2. 参数验证测试

```java
@Test
public void testParameterValidation() {
    AepClientManager client = createTestClient();

    // 测试必需参数缺失
    Map<String, Object> emptyParams = new HashMap<>();
    assertThrows(AepClientException.class, () -> client.createProduct(emptyParams));

    // 测试有效参数
    Map<String, Object> validParams = createValidCreateParams();
    assertDoesNotThrow(() -> client.createProduct(validParams));
}
```

## 📚 错误处理集成

### 1. 统一错误响应格式

```java
// 错误响应构建器
private String buildErrorResponse(String operation, String errorCode,
                                String errorMessage, Exception originalException) {
    StringBuilder errorResponse = new StringBuilder();
    errorResponse.append("{\n");
    errorResponse.append("  \"operation\": \"").append(operation).append("\",\n");
    errorResponse.append("  \"success\": false,\n");
    errorResponse.append("  \"errorCode\": \"").append(errorCode).append("\",\n");
    errorResponse.append("  \"errorMessage\": \"").append(errorMessage).append("\",\n");
    errorResponse.append("  \"timestamp\": ").append(System.currentTimeMillis()).append("\n");

    if (originalException != null && LogManager.getInstance().isDebugEnabled()) {
        errorResponse.append("  \"debugInfo\": \"").append(originalException.getMessage()).append("\"\n");
    }

    errorResponse.append("}");

    return errorResponse.toString();
}
```

### 2. AEP API错误映射

```java
// AEP API错误码到内部错误码的映射
private String mapAepErrorToInternalCode(String aepErrorMessage) {
    if (aepErrorMessage == null) return "REG-UNKNOWN-001";

    String lowerMsg = aepErrorMessage.toLowerCase();

    if (lowerMsg.contains("duplicate") || lowerMsg.contains("already exists")) {
        return RegistrationErrorCodes.CREATE_DUPLICATE_NAME;
    } else if (lowerMsg.contains("quota") || lowerMsg.contains("limit")) {
        return RegistrationErrorCodes.CREATE_QUOTA_EXCEEDED;
    } else if (lowerMsg.contains("not found")) {
        return RegistrationErrorCodes.UPDATE_PRODUCT_NOT_FOUND;
    } else if (lowerMsg.contains("permission") || lowerMsg.contains("unauthorized")) {
        return RegistrationErrorCodes.DELETE_PERMISSION_DENIED;
    }

    return "REG-UNKNOWN-002";
}
```

## 🔄 版本兼容性

### 1. Phase1.1兼容性保证

```java
// 确保Phase2的扩展不影响Phase1.1的功能
public class AepClientManager {
    // === Phase1.1原有方法保持完全不变 ===
    public String queryProducts(Map<String, Object> params) {
        // 完全复用Phase1.1的实现，不做任何修改
    }

    public String queryDevices(Map<String, Object> params) {
        // 完全复用Phase1.1的实现，不做任何修改
    }

    // === Phase2新增方法 ===
    public String createProduct(Map<String, Object> params) {
        // 新增的产品创建功能
    }

    // 版本兼容性检查
    public String getVersion() {
        return "Phase2.0 (兼容Phase1.1)";
    }
}
```

### 2. 配置向后兼容

```java
// 确保Phase1.1的配置文件在Phase2中完全可用
@Test
public void testPhase1ConfigCompatibility() {
    // 使用Phase1.1的.env配置
    ExportConfig phase1Config = ExportConfig.fromEnvironment();

    // 在Phase2中应该完全可用
    AepClientManager phase2Client = new AepClientManager(phase1Config);

    // 查询功能应该与Phase1.1完全一致
    String products = phase2Client.queryProducts(new HashMap<>());
    assertNotNull(products);
}
```

## 📋 实施检查清单

### 1. 集成准备检查

- [ ] Phase1.1的AEP SDK依赖已确认可用
- [ ] CreateProduct/UpdateProduct/DeleteProduct类导入成功
- [ ] Phase1.1的AepClientManager代码已分析理解
- [ ] 环境变量配置与Phase1.1保持一致
- [ ] 日志和错误处理体系已理解

### 2. 实施验证检查

- [ ] SDK方法调用参数正确设置
- [ ] 响应解析与Phase1.1格式一致
- [ ] 错误处理继承Phase1.1体系
- [ ] 配置管理完全复用
- [ ] 性能监控和审计日志正常

### 3. 集成测试检查

- [ ] 与Phase1.1查询功能并行工作正常
- [ ] 配置文件共享无冲突
- [ ] 客户端连接复用成功
- [ ] 操作审计日志格式一致
- [ ] 异常处理体系统一

---

**集成规范状态**: ✅ 设计完成
**下一步**: 开始MVP原型实现
**参考文档**: Phase1.1 AepClientManager.java, AepProductManagementDemo_Enhanced.java