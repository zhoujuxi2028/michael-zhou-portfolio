package com.zct.poc;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.net.ssl.*;

/**
 * 物联网连接测试程序
 * 参考toyou系统的IoT连接实现，提供详细的连接测试和日志记录
 */
public class IoTConnectionTest {

    private static final String LOG_PREFIX = "[IoT-TEST]";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // 测试配置
    private static final Map<String, String> TEST_CONFIGS = new HashMap<String, String>() {{
        put("TELECOM_IOT_BASE_URL", "https://117.78.47.187:8743");  // 中国电信IoT平台测试地址
        put("TELECOM_IOT_PROD_URL", "https://iot.platform.chinatelecom.com");  // 生产环境地址
        put("HUAWEI_IOT_URL", "https://iot.huawei.com");  // 华为IoT平台
        put("MOCK_IOT_URL", "https://httpbin.org");  // 模拟测试平台
    }};

    public static void main(String[] args) {
        logInfo("==========================================");
        logInfo("物联网连接测试开始");
        logInfo("参考toyou系统IoT连接实现");
        logInfo("==========================================");

        IoTConnectionTest tester = new IoTConnectionTest();

        try {
            // 1. 系统环境检查
            tester.performSystemCheck();

            // 2. 网络连接测试
            tester.performNetworkConnectivityTest();

            // 3. IoT平台连接测试
            tester.performIoTPlatformConnectionTests();

            // 4. 认证流程测试
            tester.performAuthenticationTests();

            // 5. 设备管理API测试
            tester.performDeviceManagementTests();

            logInfo("==========================================");
            logInfo("物联网连接测试完成!");
            logInfo("==========================================");

        } catch (Exception e) {
            logError("测试过程中发生异常", e);
        }
    }

    /**
     * 系统环境检查
     */
    private void performSystemCheck() {
        logInfo("\n1. 系统环境检查");
        logInfo("----------------------------------------");

        // Java版本检查
        String javaVersion = System.getProperty("java.version");
        logInfo("Java版本: " + javaVersion);

        // 网络配置检查
        String httpProxy = System.getProperty("http.proxyHost");
        String httpsProxy = System.getProperty("https.proxyHost");
        logInfo("HTTP代理: " + (httpProxy != null ? httpProxy : "未设置"));
        logInfo("HTTPS代理: " + (httpsProxy != null ? httpsProxy : "未设置"));

        // SSL配置检查
        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        logInfo("信任库: " + (trustStore != null ? trustStore : "使用默认"));

        // 系统时间
        logInfo("系统时间: " + dateFormat.format(new Date()));

        logInfo("✅ 系统环境检查完成");
    }

    /**
     * 网络连接测试
     */
    private void performNetworkConnectivityTest() {
        logInfo("\n2. 网络连接测试");
        logInfo("----------------------------------------");

        // 基础网络连接测试
        String[] testUrls = {
            "https://www.baidu.com",
            "https://httpbin.org/get",
            "https://jsonplaceholder.typicode.com/posts/1"
        };

        for (String testUrl : testUrls) {
            testBasicHttpConnection(testUrl);
        }

        logInfo("✅ 网络连接测试完成");
    }

    /**
     * IoT平台连接测试
     */
    private void performIoTPlatformConnectionTests() {
        logInfo("\n3. IoT平台连接测试");
        logInfo("----------------------------------------");

        for (Map.Entry<String, String> config : TEST_CONFIGS.entrySet()) {
            String platformName = config.getKey();
            String baseUrl = config.getValue();

            logInfo("测试平台: " + platformName);
            logInfo("平台地址: " + baseUrl);

            testIoTPlatformConnection(platformName, baseUrl);
            logInfo("");
        }

        logInfo("✅ IoT平台连接测试完成");
    }

    /**
     * 认证流程测试
     */
    private void performAuthenticationTests() {
        logInfo("\n4. 认证流程测试");
        logInfo("----------------------------------------");

        // 模拟中国电信IoT平台认证
        testTelecomAuthFlow();

        // 模拟华为IoT平台认证
        testHuaweiAuthFlow();

        logInfo("✅ 认证流程测试完成");
    }

    /**
     * 设备管理API测试
     */
    private void performDeviceManagementTests() {
        logInfo("\n5. 设备管理API测试");
        logInfo("----------------------------------------");

        // 设备注册测试
        testDeviceRegistration();

        // 设备状态查询测试
        testDeviceStatusQuery();

        // 设备命令下发测试
        testDeviceCommand();

        logInfo("✅ 设备管理API测试完成");
    }

    /**
     * 基础HTTP连接测试
     */
    private void testBasicHttpConnection(String testUrl) {
        logDebug("开始测试HTTP连接: " + testUrl);

        long startTime = System.currentTimeMillis();

        try {
            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置连接参数
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "IoT-Connection-Test/1.0");

            logDebug("发送HTTP请求...");

            // 执行连接
            int responseCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logInfo("连接结果: " + testUrl);
            logInfo("  响应码: " + responseCode);
            logInfo("  耗时: " + duration + "ms");
            logInfo("  状态: " + (responseCode == 200 ? "✅ 成功" : "❌ 失败"));

            connection.disconnect();

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logError("连接失败: " + testUrl + " (耗时: " + duration + "ms)", e);
        }
    }

    /**
     * IoT平台连接测试
     */
    private void testIoTPlatformConnection(String platformName, String baseUrl) {
        logDebug("开始测试IoT平台连接: " + platformName);

        long startTime = System.currentTimeMillis();

        try {
            // 测试不同的API端点
            String[] endpoints = {
                "/",
                "/health",
                "/api/v1/status",
                "/iocm/app/sec/v1.1.0/login"  // 中国电信IoT平台认证端点
            };

            boolean anySuccess = false;

            for (String endpoint : endpoints) {
                String fullUrl = baseUrl + endpoint;
                logDebug("测试端点: " + fullUrl);

                try {
                    ConnectionResult result = performHttpRequest("GET", fullUrl, null, null);

                    logDebug("  端点响应: " + result.responseCode + " (" + result.duration + "ms)");

                    if (result.responseCode < 500) {  // 非服务器错误
                        anySuccess = true;
                        logInfo("  ✅ 端点可达: " + endpoint + " (HTTP " + result.responseCode + ")");
                        break;
                    }

                } catch (Exception e) {
                    logDebug("  端点不可达: " + endpoint + " - " + e.getMessage());
                }
            }

            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            if (anySuccess) {
                logInfo("平台连接状态: ✅ 可达 (总耗时: " + totalDuration + "ms)");
            } else {
                logWarn("平台连接状态: ❌ 不可达 (总耗时: " + totalDuration + "ms)");
            }

        } catch (Exception e) {
            logError("IoT平台连接测试异常: " + platformName, e);
        }
    }

    /**
     * 中国电信IoT平台认证流程测试
     */
    private void testTelecomAuthFlow() {
        logInfo("测试中国电信IoT平台认证流程");
        logInfo("参考toyou系统AuthUtils.java实现");

        try {
            // 1. 准备认证参数
            String appId = "test_app_12345";
            String secret = "test_secret_67890";
            String authUrl = TEST_CONFIGS.get("TELECOM_IOT_BASE_URL") + "/iocm/app/sec/v1.1.0/login";

            logDebug("认证URL: " + authUrl);
            logDebug("AppID: " + appId);
            logDebug("Secret: " + maskSensitive(secret));

            // 2. 构建认证请求体
            Map<String, String> authParams = new HashMap<>();
            authParams.put("appId", appId);
            authParams.put("secret", secret);

            String requestBody = buildFormUrlEncoded(authParams);
            logDebug("请求体: " + maskSensitive(requestBody));

            // 3. 发送认证请求
            logInfo("发送认证请求到中国电信IoT平台...");

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "application/json");

            ConnectionResult result = performHttpRequest("POST", authUrl, headers, requestBody);

            logInfo("认证响应:");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  耗时: " + result.duration + "ms");
            logInfo("  响应头: " + result.responseHeaders);

            if (result.responseBody != null && !result.responseBody.trim().isEmpty()) {
                logInfo("  响应体: " + result.responseBody);

                // 尝试解析Token
                if (result.responseBody.contains("accessToken")) {
                    logInfo("  ✅ 响应包含accessToken字段");
                } else {
                    logWarn("  ❌ 响应不包含accessToken字段");
                }
            }

            // 4. 评估认证结果
            if (result.responseCode == 200) {
                logInfo("✅ 认证请求格式正确 (HTTP 200)");
            } else if (result.responseCode == 401) {
                logWarn("❌ 认证失败 - 凭据无效 (HTTP 401)");
            } else if (result.responseCode == 404) {
                logWarn("❌ 认证端点不存在 (HTTP 404)");
            } else {
                logWarn("❌ 认证异常 (HTTP " + result.responseCode + ")");
            }

        } catch (Exception e) {
            logError("中国电信IoT平台认证测试异常", e);
        }
    }

    /**
     * 华为IoT平台认证流程测试
     */
    private void testHuaweiAuthFlow() {
        logInfo("测试华为IoT平台认证流程");

        try {
            String authUrl = TEST_CONFIGS.get("HUAWEI_IOT_URL") + "/v1/auth/token";

            logDebug("华为IoT认证URL: " + authUrl);

            // 华为IoT平台通常使用JSON格式认证
            String jsonBody = "{\"username\":\"test_user\",\"password\":\"test_pass\"}";

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");

            logInfo("发送华为IoT平台认证请求...");

            ConnectionResult result = performHttpRequest("POST", authUrl, headers, jsonBody);

            logInfo("华为IoT认证响应:");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  耗时: " + result.duration + "ms");

            if (result.responseCode < 500) {
                logInfo("✅ 华为IoT平台可达");
            } else {
                logWarn("❌ 华为IoT平台不可达");
            }

        } catch (Exception e) {
            logError("华为IoT平台认证测试异常", e);
        }
    }

    /**
     * 设备注册测试
     */
    private void testDeviceRegistration() {
        logInfo("测试设备注册API");
        logInfo("参考toyou系统TDeviceService设备注册实现");

        try {
            String registerUrl = TEST_CONFIGS.get("TELECOM_IOT_BASE_URL") + "/iocm/app/reg/v1.1.0/deviceCredentials";

            // 构建设备注册请求
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", "test_device_001");
            deviceInfo.put("deviceName", "测试设备001");
            deviceInfo.put("deviceType", "sensor");
            deviceInfo.put("manufacturerId", "ZhongCheng");
            deviceInfo.put("model", "ZC-IOT-001");
            deviceInfo.put("protocolType", "CoAP");

            String jsonBody = buildJsonString(deviceInfo);
            logDebug("设备注册请求体: " + jsonBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer mock_access_token_12345");  // 模拟Token

            logInfo("发送设备注册请求...");

            ConnectionResult result = performHttpRequest("POST", registerUrl, headers, jsonBody);

            logInfo("设备注册响应:");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  耗时: " + result.duration + "ms");
            logInfo("  响应体: " + result.responseBody);

            // 评估注册结果
            if (result.responseCode == 201 || result.responseCode == 200) {
                logInfo("✅ 设备注册请求格式正确");
            } else if (result.responseCode == 401) {
                logWarn("❌ 认证Token无效");
            } else {
                logInfo("ℹ️ 设备注册API响应: HTTP " + result.responseCode);
            }

        } catch (Exception e) {
            logError("设备注册测试异常", e);
        }
    }

    /**
     * 设备状态查询测试
     */
    private void testDeviceStatusQuery() {
        logInfo("测试设备状态查询API");

        try {
            String deviceId = "test_device_001";
            String queryUrl = TEST_CONFIGS.get("TELECOM_IOT_BASE_URL") + "/iocm/app/dm/v1.4.0/devices/" + deviceId;

            logDebug("状态查询URL: " + queryUrl);

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer mock_access_token_12345");

            logInfo("发送设备状态查询请求...");

            ConnectionResult result = performHttpRequest("GET", queryUrl, headers, null);

            logInfo("设备状态查询响应:");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  耗时: " + result.duration + "ms");

            if (result.responseBody != null && !result.responseBody.trim().isEmpty()) {
                logInfo("  响应体: " + result.responseBody);
            }

            if (result.responseCode == 200) {
                logInfo("✅ 设备状态查询请求格式正确");
            } else if (result.responseCode == 404) {
                logInfo("ℹ️ 设备不存在 (预期结果)");
            } else {
                logInfo("ℹ️ 设备状态查询API响应: HTTP " + result.responseCode);
            }

        } catch (Exception e) {
            logError("设备状态查询测试异常", e);
        }
    }

    /**
     * 设备命令下发测试
     */
    private void testDeviceCommand() {
        logInfo("测试设备命令下发API");

        try {
            String commandUrl = TEST_CONFIGS.get("TELECOM_IOT_BASE_URL") + "/iocm/app/cmd/v1.4.0/deviceCommands";

            // 构建设备命令
            Map<String, Object> command = new HashMap<>();
            command.put("deviceId", "test_device_001");
            command.put("command", Map.of(
                "commandName", "LED_CONTROL",
                "params", Map.of(
                    "action", "ON",
                    "brightness", 80
                )
            ));
            command.put("callbackUrl", "http://callback.example.com/iot");
            command.put("expireTime", 300);  // 5分钟过期

            String jsonBody = buildJsonString(command);
            logDebug("设备命令请求体: " + jsonBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer mock_access_token_12345");

            logInfo("发送设备命令下发请求...");

            ConnectionResult result = performHttpRequest("POST", commandUrl, headers, jsonBody);

            logInfo("设备命令下发响应:");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  耗时: " + result.duration + "ms");
            logInfo("  响应体: " + result.responseBody);

            if (result.responseCode == 200 || result.responseCode == 202) {
                logInfo("✅ 设备命令下发请求格式正确");
            } else {
                logInfo("ℹ️ 设备命令下发API响应: HTTP " + result.responseCode);
            }

        } catch (Exception e) {
            logError("设备命令下发测试异常", e);
        }
    }

    /**
     * 执行HTTP请求
     */
    private ConnectionResult performHttpRequest(String method, String urlString, Map<String, String> headers, String body) throws Exception {
        logDebug("执行HTTP请求: " + method + " " + urlString);

        long startTime = System.currentTimeMillis();

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // 设置基本参数
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);  // 10秒连接超时
            connection.setReadTimeout(15000);     // 15秒读取超时
            connection.setInstanceFollowRedirects(true);

            // 设置请求头
            connection.setRequestProperty("User-Agent", "IoT-Connection-Test/1.0 (toyou-reference)");
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // 设置请求体
            if (body != null && !body.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                    os.flush();
                }
                logDebug("已发送请求体 (" + body.getBytes("UTF-8").length + " 字节)");
            }

            // 执行请求
            logDebug("正在连接...");
            int responseCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logDebug("收到响应: HTTP " + responseCode + " (耗时: " + duration + "ms)");

            // 读取响应
            String responseBody = null;
            Map<String, String> responseHeaders = new HashMap<>();

            try {
                // 读取响应头
                for (Map.Entry<String, java.util.List<String>> header : connection.getHeaderFields().entrySet()) {
                    if (header.getKey() != null && header.getValue() != null && !header.getValue().isEmpty()) {
                        responseHeaders.put(header.getKey(), String.join(", ", header.getValue()));
                    }
                }

                // 读取响应体
                InputStream inputStream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append("\n");
                        }
                        responseBody = response.toString().trim();
                    }
                }

            } catch (Exception e) {
                logDebug("读取响应内容时发生异常: " + e.getMessage());
            }

            return new ConnectionResult(responseCode, responseBody, responseHeaders, duration);

        } finally {
            connection.disconnect();
        }
    }

    /**
     * 构建表单编码字符串
     */
    private String buildFormUrlEncoded(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 构建JSON字符串
     */
    private String buildJsonString(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Map) {
                sb.append(buildJsonString((Map<String, Object>) value));
            } else {
                sb.append("\"").append(String.valueOf(value)).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 遮蔽敏感信息
     */
    private String maskSensitive(String text) {
        if (text == null || text.length() <= 8) {
            return "***";
        }
        return text.substring(0, 4) + "***" + text.substring(text.length() - 4);
    }

    /**
     * 连接结果类
     */
    static class ConnectionResult {
        int responseCode;
        String responseBody;
        Map<String, String> responseHeaders;
        long duration;

        ConnectionResult(int responseCode, String responseBody, Map<String, String> responseHeaders, long duration) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
            this.responseHeaders = responseHeaders;
            this.duration = duration;
        }
    }

    /**
     * 日志方法
     */
    private static void logInfo(String message) {
        System.out.println(LOG_PREFIX + " [INFO]  " + dateFormat.format(new Date()) + " " + message);
    }

    private static void logDebug(String message) {
        System.out.println(LOG_PREFIX + " [DEBUG] " + dateFormat.format(new Date()) + " " + message);
    }

    private static void logWarn(String message) {
        System.out.println(LOG_PREFIX + " [WARN]  " + dateFormat.format(new Date()) + " " + message);
    }

    private static void logError(String message, Exception e) {
        System.err.println(LOG_PREFIX + " [ERROR] " + dateFormat.format(new Date()) + " " + message);
        if (e != null) {
            System.err.println(LOG_PREFIX + " [ERROR] 异常详情: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println(LOG_PREFIX + " [ERROR] 根本原因: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
        }
    }
}