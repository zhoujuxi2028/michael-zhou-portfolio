# 电信物联网平台SDK客户端详细设计

**文档版本**: v1.0
**创建时间**: 2024-12-24
**适用平台**: 中国电信物联网开放平台 (AEP)
**SDK版本**: ctg-ag-sdk-core-2.8.0 + ag-sdk-biz-267848

---

## 1. SDK使用情况分析

### 1.1 各项目SDK集成现状

| 项目名称 | SDK使用方式 | 实现类型 | 配置状态 |
|---------|------------|----------|----------|
| **POC测试项目** | 电信官方SDK | Maven依赖 | ✅ 已配置 |
| **vendor-b项目** | ~~电信官方SDK~~ | SystemPath依赖 | ❌ 已注释 |
| **主项目** | 自定义HTTP客户端 | HttpsUtil实现 | ✅ 已配置 |
| **Vendor C验收项目1-4** | 自定义HTTP客户端 | 各自实现 | ✅ 已配置 |

### 1.2 SDK架构对比

#### 方案A: 电信官方SDK (推荐)
```java
// 使用电信官方SDK
AepDeviceManagementClient deviceClient = AepDeviceManagementClient.newClient()
    .appKey(APP_KEY)
    .appSecret(APP_SECRET)
    .build();

QueryDeviceRequest request = new QueryDeviceRequest();
request.setParamDeviceId(deviceId);
QueryDeviceResponse response = deviceClient.QueryDevice(request);
```

**优点**:
- 官方维护，API更新及时
- 内置SSL证书处理
- 标准化错误处理
- 自动处理认证和令牌管理

**缺点**:
- 依赖外部JAR包
- 版本更新需要手动替换JAR文件
- 依赖管理复杂

#### 方案B: 自定义HTTP客户端 (当前主流)
```java
// 自定义HTTP客户端
HttpsUtil httpsUtil = new HttpsUtil();
httpsUtil.initSSLConfigForTwoWay();

String accessToken = AuthUtils.login(httpsUtil, appId, secret);
Map<String, String> headers = new HashMap<>();
headers.put("app_key", appId);
headers.put("Authorization", "Bearer " + accessToken);

String response = httpsUtil.doGetWithParas(url, null, headers);
```

**优点**:
- 完全控制HTTP通信细节
- 便于调试和日志记录
- 可定制化异常处理
- 不依赖外部SDK

**缺点**:
- 需要手动处理认证流程
- SSL证书配置复杂
- API变更需要手动适配
- 错误处理需要自己实现

---

## 2. 电信SDK客户端统一设计架构

### 2.1 整体架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    应用业务层                                 │
│  (设备管理、命令控制、数据上报、告警处理)                      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  IoT客户端服务层                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   设备管理服务    │  │   命令控制服务    │  │  数据监控服务  │ │
│  │  DeviceService  │  │ CommandService  │  │ MonitorService│ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                 IoT客户端适配器层                            │
│  ┌─────────────────┐             ┌─────────────────────────┐ │
│  │  官方SDK适配器    │             │   自定义HTTP适配器       │ │
│  │ TelecomSDKClient│  ←-切换-→   │ TelecomHttpClient      │ │
│  └─────────────────┘             └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    配置管理层                                │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │    认证配置     │  │   服务器配置   │  │    SSL证书配置   │  │
│  │ AuthConfig    │  │ ServerConfig │  │  CertConfig    │  │
│  └────────────────┘  └──────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                 中国电信IoT平台 (AEP)                        │
│         https://device.api.ct10649.com:8743                │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心接口定义

#### 2.2.1 统一IoT客户端接口
```java
/**
 * 电信IoT平台统一客户端接口
 */
public interface TelecomIoTClient {

    /**
     * 设备管理操作
     */
    DeviceResult queryDevice(String deviceId);
    DeviceResult registerDevice(DeviceInfo deviceInfo);
    DeviceResult updateDevice(String deviceId, DeviceInfo deviceInfo);
    DeviceResult deleteDevice(String deviceId);

    /**
     * 命令控制操作
     */
    CommandResult sendCommand(String deviceId, CommandRequest command);
    CommandResult queryCommandStatus(String commandId);

    /**
     * 数据监控操作
     */
    DataResult getDeviceData(String deviceId, TimeRange timeRange);
    List<AlarmInfo> getDeviceAlarms(String deviceId);

    /**
     * 连接管理
     */
    boolean testConnection();
    void refreshToken();
    void close();
}
```

#### 2.2.2 配置管理接口
```java
/**
 * IoT平台配置管理接口
 */
public interface TelecomConfig {
    String getBaseUrl();
    String getAppKey();
    String getAppSecret();
    String getCertPath();
    String getCertPassword();
    int getConnectTimeout();
    int getReadTimeout();
    boolean isSSLEnabled();
}

/**
 * 数据库驱动的配置实现
 */
@Component
public class DatabaseTelecomConfig implements TelecomConfig {

    @Autowired
    private TDictMapper dictMapper;

    @Override
    public String getAppKey() {
        TDictPO config = dictMapper.queryByDictCode("iotplat_appid", "0");
        return config != null ? config.getItemValue() : null;
    }

    @Override
    public String getAppSecret() {
        TDictPO config = dictMapper.queryByDictCode("iotplat_secret", "0");
        return config != null ? config.getItemValue() : null;
    }

    // 其他配置方法...
}
```

### 2.3 官方SDK适配器实现

#### 2.3.1 SDK客户端封装
```java
/**
 * 电信官方SDK适配器实现
 */
@Service
public class TelecomSDKClient implements TelecomIoTClient {

    private static final Logger logger = LoggerFactory.getLogger(TelecomSDKClient.class);

    private final TelecomConfig config;
    private AepDeviceManagementClient deviceClient;
    private AepCommandClient commandClient;

    public TelecomSDKClient(TelecomConfig config) {
        this.config = config;
        initializeClients();
    }

    /**
     * 初始化SDK客户端
     */
    private void initializeClients() {
        try {
            // 初始化设备管理客户端
            deviceClient = AepDeviceManagementClient.newClient()
                .appKey(config.getAppKey())
                .appSecret(config.getAppSecret())
                .build();

            // 初始化命令客户端
            commandClient = AepCommandClient.newClient()
                .appKey(config.getAppKey())
                .appSecret(config.getAppSecret())
                .build();

            logger.info("电信SDK客户端初始化成功");

        } catch (Exception e) {
            logger.error("电信SDK客户端初始化失败", e);
            throw new IoTClientException("SDK客户端初始化失败", e);
        }
    }

    @Override
    public DeviceResult queryDevice(String deviceId) {
        try {
            QueryDeviceRequest request = new QueryDeviceRequest();
            request.setParamDeviceId(deviceId);

            QueryDeviceResponse response = deviceClient.QueryDevice(request);

            return convertToDeviceResult(response);

        } catch (Exception e) {
            logger.error("查询设备失败: deviceId={}", deviceId, e);
            throw new IoTClientException("查询设备失败", e);
        }
    }

    @Override
    public DeviceResult registerDevice(DeviceInfo deviceInfo) {
        try {
            CreateDeviceRequest request = new CreateDeviceRequest();
            // 设置设备信息
            request.setDeviceName(deviceInfo.getDeviceName());
            request.setDeviceType(deviceInfo.getDeviceType());
            request.setManufacturerId(deviceInfo.getManufacturerId());
            request.setProtocolType(deviceInfo.getProtocolType());

            CreateDeviceResponse response = deviceClient.CreateDevice(request);

            return convertToDeviceResult(response);

        } catch (Exception e) {
            logger.error("注册设备失败: deviceInfo={}", deviceInfo, e);
            throw new IoTClientException("注册设备失败", e);
        }
    }

    @Override
    public CommandResult sendCommand(String deviceId, CommandRequest command) {
        try {
            CreateCommandRequest request = new CreateCommandRequest();
            request.setDeviceId(deviceId);
            request.setCommand(command.toJsonString());
            request.setCallbackUrl(command.getCallbackUrl());
            request.setExpireTime(command.getExpireTime());

            CreateCommandResponse response = commandClient.CreateCommand(request);

            return convertToCommandResult(response);

        } catch (Exception e) {
            logger.error("发送命令失败: deviceId={}, command={}", deviceId, command, e);
            throw new IoTClientException("发送命令失败", e);
        }
    }

    @Override
    public boolean testConnection() {
        try {
            // 使用一个简单的查询来测试连接
            QueryDeviceRequest request = new QueryDeviceRequest();
            request.setParamDeviceId("test-connection");

            deviceClient.QueryDevice(request);
            return true;

        } catch (Exception e) {
            logger.warn("连接测试失败", e);
            return false;
        }
    }

    // 结果转换方法
    private DeviceResult convertToDeviceResult(QueryDeviceResponse response) {
        DeviceResult result = new DeviceResult();
        result.setSuccess(response.getStatusCode() == 200);
        result.setStatusCode(response.getStatusCode());
        result.setMessage(response.getMessage());

        if (result.isSuccess()) {
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setDeviceId(response.getDeviceId());
            deviceInfo.setDeviceName(response.getDeviceName());
            deviceInfo.setDeviceType(response.getDeviceType());
            deviceInfo.setStatus(response.getStatus());
            result.setDeviceInfo(deviceInfo);
        }

        return result;
    }

    private CommandResult convertToCommandResult(CreateCommandResponse response) {
        CommandResult result = new CommandResult();
        result.setSuccess(response.getStatusCode() == 201);
        result.setStatusCode(response.getStatusCode());
        result.setCommandId(response.getCommandId());
        result.setMessage(response.getMessage());
        return result;
    }
}
```

### 2.4 自定义HTTP适配器实现

#### 2.4.1 HTTP客户端封装
```java
/**
 * 自定义HTTP客户端适配器实现
 */
@Service
public class TelecomHttpClient implements TelecomIoTClient {

    private static final Logger logger = LoggerFactory.getLogger(TelecomHttpClient.class);

    private final TelecomConfig config;
    private final HttpsUtil httpsUtil;
    private volatile String accessToken;
    private volatile long tokenExpireTime;

    public TelecomHttpClient(TelecomConfig config) {
        this.config = config;
        this.httpsUtil = new HttpsUtil();
        initializeSSL();
    }

    /**
     * 初始化SSL配置
     */
    private void initializeSSL() {
        try {
            httpsUtil.initSSLConfigForTwoWay();
            logger.info("SSL双向认证配置成功");
        } catch (Exception e) {
            logger.error("SSL配置失败", e);
            throw new IoTClientException("SSL配置失败", e);
        }
    }

    /**
     * 获取访问令牌
     */
    private String getAccessToken() {
        // 检查令牌是否过期
        if (accessToken == null || System.currentTimeMillis() > tokenExpireTime) {
            refreshToken();
        }
        return accessToken;
    }

    @Override
    public void refreshToken() {
        try {
            String loginUrl = config.getBaseUrl() + "/iocm/app/sec/v1.1.0/login";

            Map<String, Object> authParams = new HashMap<>();
            authParams.put("appId", config.getAppKey());
            authParams.put("secret", config.getAppSecret());

            Map<String, String> headers = new HashMap<>();
            headers.put("app_key", config.getAppKey());
            headers.put("Content-Type", "application/json");

            String jsonRequest = JsonUtil.objectToJson(authParams);
            StreamClosedHttpResponse response = httpsUtil.doPostJsonGetStatusLine(
                loginUrl, headers, jsonRequest);

            if (response.getStatusLine().getStatusCode() == 200) {
                Map<String, Object> result = JsonUtil.jsonToMap(response.getContent());
                this.accessToken = (String) result.get("accessToken");
                Integer expiresIn = (Integer) result.get("expiresIn");
                this.tokenExpireTime = System.currentTimeMillis() + (expiresIn * 1000L);

                logger.info("访问令牌刷新成功");
            } else {
                throw new IoTClientException("获取访问令牌失败: " + response.getContent());
            }

        } catch (Exception e) {
            logger.error("刷新访问令牌失败", e);
            throw new IoTClientException("刷新访问令牌失败", e);
        }
    }

    @Override
    public DeviceResult queryDevice(String deviceId) {
        try {
            String queryUrl = config.getBaseUrl() + "/iocm/app/dm/v1.1.0/devices/" + deviceId;

            Map<String, String> headers = buildHeaders();

            StreamClosedHttpResponse response = httpsUtil.doGetWithParasGetStatusLine(
                queryUrl, null, headers);

            return parseDeviceResponse(response);

        } catch (Exception e) {
            logger.error("查询设备失败: deviceId={}", deviceId, e);
            throw new IoTClientException("查询设备失败", e);
        }
    }

    @Override
    public DeviceResult registerDevice(DeviceInfo deviceInfo) {
        try {
            String registerUrl = config.getBaseUrl() + "/iocm/app/reg/v1.1.0/devices";

            Map<String, Object> deviceParams = new HashMap<>();
            deviceParams.put("verifyCode", deviceInfo.getVerifyCode());
            deviceParams.put("nodeId", deviceInfo.getNodeId());
            deviceParams.put("deviceInfo", buildDeviceInfoMap(deviceInfo));
            deviceParams.put("timeout", 0);

            Map<String, String> headers = buildHeaders();
            String jsonRequest = JsonUtil.objectToJson(deviceParams);

            StreamClosedHttpResponse response = httpsUtil.doPostJsonGetStatusLine(
                registerUrl, headers, jsonRequest);

            return parseDeviceResponse(response);

        } catch (Exception e) {
            logger.error("注册设备失败: deviceInfo={}", deviceInfo, e);
            throw new IoTClientException("注册设备失败", e);
        }
    }

    @Override
    public CommandResult sendCommand(String deviceId, CommandRequest command) {
        try {
            String commandUrl = config.getBaseUrl() + "/iocm/app/cmd/v1.4.0/deviceCommands";

            Map<String, Object> commandParams = new HashMap<>();
            commandParams.put("deviceId", deviceId);
            commandParams.put("command", command.toCommandMap());
            commandParams.put("callbackUrl", command.getCallbackUrl());
            commandParams.put("expireTime", command.getExpireTime());

            Map<String, String> headers = buildHeaders();
            String jsonRequest = JsonUtil.objectToJson(commandParams);

            StreamClosedHttpResponse response = httpsUtil.doPostJsonGetStatusLine(
                commandUrl, headers, jsonRequest);

            return parseCommandResponse(response);

        } catch (Exception e) {
            logger.error("发送命令失败: deviceId={}, command={}", deviceId, command, e);
            throw new IoTClientException("发送命令失败", e);
        }
    }

    @Override
    public boolean testConnection() {
        try {
            String testUrl = config.getBaseUrl() + "/iocm/app/dm/v1.1.0/devices";
            Map<String, String> headers = buildHeaders();

            StreamClosedHttpResponse response = httpsUtil.doGetWithParasGetStatusLine(
                testUrl, null, headers);

            return response.getStatusLine().getStatusCode() < 500;

        } catch (Exception e) {
            logger.warn("连接测试失败", e);
            return false;
        }
    }

    /**
     * 构建HTTP请求头
     */
    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("app_key", config.getAppKey());
        headers.put("Authorization", "Bearer " + getAccessToken());
        headers.put("Content-Type", "application/json");
        return headers;
    }

    /**
     * 解析设备响应
     */
    private DeviceResult parseDeviceResponse(StreamClosedHttpResponse response) {
        DeviceResult result = new DeviceResult();
        result.setStatusCode(response.getStatusLine().getStatusCode());
        result.setSuccess(result.getStatusCode() == 200);

        try {
            Map<String, Object> responseMap = JsonUtil.jsonToMap(response.getContent());

            if (result.isSuccess()) {
                DeviceInfo deviceInfo = new DeviceInfo();
                deviceInfo.setDeviceId((String) responseMap.get("deviceId"));
                deviceInfo.setDeviceName((String) responseMap.get("deviceName"));
                deviceInfo.setDeviceType((String) responseMap.get("deviceType"));
                deviceInfo.setStatus((String) responseMap.get("status"));
                result.setDeviceInfo(deviceInfo);
            } else {
                result.setMessage((String) responseMap.get("error_desc"));
            }

        } catch (Exception e) {
            logger.warn("解析响应失败", e);
            result.setMessage("响应解析失败: " + response.getContent());
        }

        return result;
    }

    private CommandResult parseCommandResponse(StreamClosedHttpResponse response) {
        CommandResult result = new CommandResult();
        result.setStatusCode(response.getStatusLine().getStatusCode());
        result.setSuccess(result.getStatusCode() == 201);

        try {
            Map<String, Object> responseMap = JsonUtil.jsonToMap(response.getContent());
            result.setCommandId((String) responseMap.get("commandId"));

            if (!result.isSuccess()) {
                result.setMessage((String) responseMap.get("error_desc"));
            }

        } catch (Exception e) {
            logger.warn("解析命令响应失败", e);
            result.setMessage("响应解析失败: " + response.getContent());
        }

        return result;
    }

    private Map<String, Object> buildDeviceInfoMap(DeviceInfo deviceInfo) {
        Map<String, Object> deviceInfoMap = new HashMap<>();
        deviceInfoMap.put("deviceType", deviceInfo.getDeviceType());
        deviceInfoMap.put("manufacturerId", deviceInfo.getManufacturerId());
        deviceInfoMap.put("manufacturerName", deviceInfo.getManufacturerName());
        deviceInfoMap.put("model", deviceInfo.getModel());
        deviceInfoMap.put("protocolType", deviceInfo.getProtocolType());
        return deviceInfoMap;
    }
}
```

---

## 3. 客户端工厂和配置

### 3.1 客户端工厂实现
```java
/**
 * 电信IoT客户端工厂
 */
@Component
public class TelecomIoTClientFactory {

    private final TelecomConfig config;
    private final ApplicationContext applicationContext;

    public TelecomIoTClientFactory(TelecomConfig config, ApplicationContext applicationContext) {
        this.config = config;
        this.applicationContext = applicationContext;
    }

    /**
     * 创建IoT客户端
     * @param clientType 客户端类型 (SDK/HTTP)
     * @return IoT客户端实例
     */
    public TelecomIoTClient createClient(ClientType clientType) {
        switch (clientType) {
            case SDK:
                return applicationContext.getBean(TelecomSDKClient.class);
            case HTTP:
                return applicationContext.getBean(TelecomHttpClient.class);
            default:
                throw new IllegalArgumentException("不支持的客户端类型: " + clientType);
        }
    }

    /**
     * 创建默认客户端 (根据配置自动选择)
     */
    public TelecomIoTClient createDefaultClient() {
        // 根据配置或环境变量决定使用哪种实现
        String clientType = System.getProperty("iot.client.type", "HTTP");
        return createClient(ClientType.valueOf(clientType.toUpperCase()));
    }
}

/**
 * 客户端类型枚举
 */
public enum ClientType {
    SDK,    // 使用官方SDK
    HTTP    // 使用自定义HTTP客户端
}
```

### 3.2 Spring Boot配置
```java
/**
 * IoT客户端自动配置类
 */
@Configuration
@EnableConfigurationProperties(TelecomProperties.class)
public class TelecomIoTAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "telecom.iot.client.type", havingValue = "sdk")
    public TelecomSDKClient telecomSDKClient(TelecomConfig config) {
        return new TelecomSDKClient(config);
    }

    @Bean
    @ConditionalOnProperty(value = "telecom.iot.client.type", havingValue = "http", matchIfMissing = true)
    public TelecomHttpClient telecomHttpClient(TelecomConfig config) {
        return new TelecomHttpClient(config);
    }

    @Bean
    @Primary
    public TelecomIoTClient telecomIoTClient(TelecomIoTClientFactory factory) {
        return factory.createDefaultClient();
    }
}

/**
 * 配置属性类
 */
@ConfigurationProperties(prefix = "telecom.iot")
@Data
public class TelecomProperties implements TelecomConfig {

    private String baseUrl = "https://device.api.ct10649.com:8743";
    private String appKey;
    private String appSecret;
    private String certPath = "cert/outgoing.CertwithKey.pkcs12";
    private String certPassword = "IoM@1234";
    private String caCertPath = "cert/ca.jks";
    private String caCertPassword = "Huawei@123";
    private int connectTimeout = 30000;
    private int readTimeout = 60000;
    private boolean sslEnabled = true;
    private ClientType clientType = ClientType.HTTP;

    // 数据库配置优先级更高
    @Autowired
    private TDictMapper dictMapper;

    @Override
    public String getAppKey() {
        if (appKey != null) {
            return appKey;
        }
        // 从数据库读取
        TDictPO config = dictMapper.queryByDictCode("iotplat_appid", "0");
        return config != null ? config.getItemValue() : null;
    }

    @Override
    public String getAppSecret() {
        if (appSecret != null) {
            return appSecret;
        }
        // 从数据库读取
        TDictPO config = dictMapper.queryByDictCode("iotplat_secret", "0");
        return config != null ? config.getItemValue() : null;
    }
}
```

---

## 4. 使用示例

### 4.1 Spring Boot项目集成
```java
/**
 * 设备管理服务示例
 */
@Service
public class IoTDeviceService {

    private final TelecomIoTClient iotClient;

    public IoTDeviceService(TelecomIoTClient iotClient) {
        this.iotClient = iotClient;
    }

    /**
     * 查询设备信息
     */
    public DeviceInfo getDeviceInfo(String deviceId) {
        DeviceResult result = iotClient.queryDevice(deviceId);
        if (result.isSuccess()) {
            return result.getDeviceInfo();
        } else {
            throw new BusinessException("查询设备失败: " + result.getMessage());
        }
    }

    /**
     * 注册新设备
     */
    public String registerNewDevice(DeviceRegistrationRequest request) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceName(request.getDeviceName());
        deviceInfo.setDeviceType(request.getDeviceType());
        deviceInfo.setVerifyCode(request.getVerifyCode());
        deviceInfo.setNodeId(request.getNodeId());

        DeviceResult result = iotClient.registerDevice(deviceInfo);
        if (result.isSuccess()) {
            return result.getDeviceInfo().getDeviceId();
        } else {
            throw new BusinessException("注册设备失败: " + result.getMessage());
        }
    }

    /**
     * 发送设备命令
     */
    public String sendDeviceCommand(String deviceId, String commandType, Map<String, Object> params) {
        CommandRequest command = new CommandRequest();
        command.setCommandName(commandType);
        command.setParameters(params);
        command.setCallbackUrl("/api/iot/command/callback");
        command.setExpireTime(300); // 5分钟过期

        CommandResult result = iotClient.sendCommand(deviceId, command);
        if (result.isSuccess()) {
            return result.getCommandId();
        } else {
            throw new BusinessException("发送命令失败: " + result.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkIoTConnection() {
        if (iotClient.testConnection()) {
            log.info("IoT平台连接正常");
        } else {
            log.error("IoT平台连接异常");
        }
    }
}
```

### 4.2 配置文件示例
```yaml
# application.yml
telecom:
  iot:
    base-url: https://device.api.ct10649.com:8743
    client-type: http  # 可选: sdk, http
    connect-timeout: 30000
    read-timeout: 60000
    ssl-enabled: true
    cert-path: cert/outgoing.CertwithKey.pkcs12
    cert-password: IoM@1234
    ca-cert-path: cert/ca.jks
    ca-cert-password: Huawei@123

# 数据库配置将覆盖这里的app-key和app-secret
logging:
  level:
    com.telecom.iot: DEBUG
```

---

## 5. Maven依赖配置

### 5.1 官方SDK依赖 (推荐)
```xml
<!-- pom.xml -->
<dependencies>
    <!-- 电信官方SDK -->
    <dependency>
        <groupId>com.ctg.ag.sdk</groupId>
        <artifactId>ctg-ag-sdk-core</artifactId>
        <version>2.8.0</version>
    </dependency>
    <dependency>
        <groupId>com.ctg.ag.sdk</groupId>
        <artifactId>ag-sdk-biz</artifactId>
        <version>267848-SNAPSHOT</version>
    </dependency>

    <!-- HTTP客户端依赖 -->
    <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        <version>4.5.14</version>
    </dependency>

    <!-- JSON处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>

    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 5.2 SDK JAR包本地安装
```bash
# 安装电信SDK到本地Maven仓库
mvn install:install-file \
  -Dfile=ctg-ag-sdk-core-2.8.0-20230508.100604-1.jar \
  -DgroupId=com.ctg.ag.sdk \
  -DartifactId=ctg-ag-sdk-core \
  -Dversion=2.8.0 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=ag-sdk-biz-267848.tar.gz-20230830.093551-SNAPSHOT.jar \
  -DgroupId=com.ctg.ag.sdk \
  -DartifactId=ag-sdk-biz \
  -Dversion=267848-SNAPSHOT \
  -Dpackaging=jar
```

---

## 6. 最佳实践和注意事项

### 6.1 性能优化
- **连接池管理**: 复用HTTP连接，避免频繁建立连接
- **令牌缓存**: 缓存访问令牌，减少认证请求
- **异步处理**: 对于耗时操作使用异步调用
- **重试机制**: 实现智能重试，处理网络抖动

### 6.2 安全考虑
- **证书管理**: 定期更新SSL证书，使用安全的存储方式
- **凭据保护**: APP_KEY和APP_SECRET不能硬编码，使用配置中心
- **访问控制**: 限制客户端访问权限，使用最小权限原则
- **日志脱敏**: 记录日志时要脱敏敏感信息

### 6.3 监控和运维
- **健康检查**: 定期检查IoT平台连接状态
- **错误监控**: 监控API调用失败率和响应时间
- **日志记录**: 详细记录API调用日志，便于问题排查
- **指标统计**: 收集调用量、成功率等关键指标

### 6.4 兼容性处理
- **API版本**: 支持多个API版本，便于平滑升级
- **降级策略**: 当主要方式不可用时，提供备用方案
- **配置热更新**: 支持不重启更新配置
- **向后兼容**: 新版本要兼容旧的调用方式

---

**文档结束**

该设计架构提供了一个统一、灵活、可扩展的电信IoT平台客户端解决方案，既支持官方SDK又支持自定义HTTP客户端，能够满足不同项目的需求和约束条件。