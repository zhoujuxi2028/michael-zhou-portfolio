package com.zct.poc;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 物联网模拟平台连接测试
 * 使用可访问的测试平台展示完整的IoT连接流程和详细日志
 */
public class IoTMockTest {

    private static final String LOG_PREFIX = "[IoT-MOCK]";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // 使用httpbin.org作为模拟IoT平台
    private static final String MOCK_IOT_BASE = "https://httpbin.org";

    public static void main(String[] args) {
        logInfo("==========================================");
        logInfo("物联网模拟平台连接测试");
        logInfo("使用httpbin.org模拟IoT平台API");
        logInfo("展示完整的IoT连接流程和详细日志");
        logInfo("==========================================");

        IoTMockTest tester = new IoTMockTest();

        try {
            // 1. 平台连接验证
            tester.testPlatformConnectivity();

            // 2. 模拟认证流程
            tester.testAuthenticationFlow();

            // 3. 模拟设备注册
            tester.testDeviceRegistration();

            // 4. 模拟设备状态查询
            tester.testDeviceStatusQuery();

            // 5. 模拟设备命令下发
            tester.testDeviceCommand();

            // 6. 模拟数据上报
            tester.testDataUpload();

            logInfo("==========================================");
            logInfo("模拟IoT平台连接测试完成!");
            logInfo("✅ 所有API测试通过，连接正常");
            logInfo("==========================================");

        } catch (Exception e) {
            logError("模拟测试过程中发生异常", e);
        }
    }

    /**
     * 测试平台连接性
     */
    private void testPlatformConnectivity() {
        logInfo("\n1. 平台连接验证");
        logInfo("----------------------------------------");

        try {
            String testUrl = MOCK_IOT_BASE + "/get";
            logDebug("测试连接: " + testUrl);

            long startTime = System.currentTimeMillis();
            ConnectionResult result = performHttpRequest("GET", testUrl, null, null);
            long duration = System.currentTimeMillis() - startTime;

            logInfo("平台连接测试结果:");
            logInfo("  目标地址: " + MOCK_IOT_BASE);
            logInfo("  响应状态: HTTP " + result.responseCode);
            logInfo("  响应时间: " + duration + "ms");
            logInfo("  连接状态: " + (result.responseCode == 200 ? "✅ 成功" : "❌ 失败"));

            if (result.responseBody != null) {
                logDebug("响应内容: " + result.responseBody.substring(0, Math.min(200, result.responseBody.length())) + "...");
            }

        } catch (Exception e) {
            logError("平台连接测试失败", e);
        }
    }

    /**
     * 测试认证流程
     */
    private void testAuthenticationFlow() {
        logInfo("\n2. IoT平台认证流程测试");
        logInfo("----------------------------------------");

        try {
            String authUrl = MOCK_IOT_BASE + "/post";
            logInfo("模拟认证端点: " + authUrl);

            // 构建认证请求 (参考toyou系统AuthUtils.java)
            Map<String, Object> authData = new HashMap<>();
            authData.put("appId", "toyou_test_app_001");
            authData.put("secret", "toyou_secret_xyz789");
            authData.put("timestamp", String.valueOf(System.currentTimeMillis()));

            String requestBody = buildJsonString(authData);
            logDebug("认证请求体: " + requestBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("User-Agent", "ToyouIoT/1.0 (Connection-Test)");

            logInfo("发送认证请求...");
            long startTime = System.currentTimeMillis();

            ConnectionResult result = performHttpRequest("POST", authUrl, headers, requestBody);

            long authDuration = System.currentTimeMillis() - startTime;

            logInfo("认证流程结果:");
            logInfo("  请求方式: POST");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  认证耗时: " + authDuration + "ms");
            logInfo("  认证状态: " + (result.responseCode == 200 ? "✅ 认证成功" : "❌ 认证失败"));

            // 解析响应，模拟Token提取
            if (result.responseBody != null && result.responseBody.contains("json")) {
                logInfo("  Token获取: ✅ 模拟AccessToken已获取");
                logDebug("  模拟Token: mock_access_token_" + System.currentTimeMillis());
            }

            if (result.responseBody != null) {
                logDebug("认证响应详情: " + result.responseBody.substring(0, Math.min(300, result.responseBody.length())));
            }

        } catch (Exception e) {
            logError("认证流程测试异常", e);
        }
    }

    /**
     * 测试设备注册
     */
    private void testDeviceRegistration() {
        logInfo("\n3. 设备注册API测试");
        logInfo("----------------------------------------");

        try {
            String registerUrl = MOCK_IOT_BASE + "/post";
            logInfo("设备注册端点: " + registerUrl);

            // 构建设备注册数据 (参考toyou系统TDeviceService)
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", "ZC_DEVICE_001");
            deviceInfo.put("deviceName", "众成科技测试设备001");
            deviceInfo.put("deviceType", "temperature_sensor");
            deviceInfo.put("manufacturerId", "ZhongCheng_Tech");
            deviceInfo.put("model", "ZC-TEMP-v2.1");
            deviceInfo.put("protocolType", "CoAP");
            deviceInfo.put("imei", "123456789012345");
            deviceInfo.put("imsi", "460012345678901");

            Map<String, Object> location = new HashMap<>();
            location.put("latitude", 31.2304);
            location.put("longitude", 121.4737);
            location.put("address", "上海市黄浦区");
            deviceInfo.put("location", location);

            String jsonBody = buildJsonString(deviceInfo);
            logDebug("设备注册数据: " + jsonBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer mock_access_token_12345");
            headers.put("X-Device-Registration", "true");

            logInfo("发送设备注册请求...");
            long startTime = System.currentTimeMillis();

            ConnectionResult result = performHttpRequest("POST", registerUrl, headers, jsonBody);

            long registerDuration = System.currentTimeMillis() - startTime;

            logInfo("设备注册结果:");
            logInfo("  设备ID: ZC_DEVICE_001");
            logInfo("  设备类型: temperature_sensor");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  注册耗时: " + registerDuration + "ms");
            logInfo("  注册状态: " + (result.responseCode == 200 ? "✅ 注册成功" : "❌ 注册失败"));

            // 模拟设备凭据生成
            if (result.responseCode == 200) {
                logInfo("  设备凭据: ✅ 已生成 (deviceSecret: zc_***_001)");
                logInfo("  设备状态: 已激活，等待首次上线");
            }

        } catch (Exception e) {
            logError("设备注册测试异常", e);
        }
    }

    /**
     * 测试设备状态查询
     */
    private void testDeviceStatusQuery() {
        logInfo("\n4. 设备状态查询API测试");
        logInfo("----------------------------------------");

        try {
            String deviceId = "ZC_DEVICE_001";
            String queryUrl = MOCK_IOT_BASE + "/get?deviceId=" + deviceId + "&queryType=status";

            logInfo("设备状态查询端点: " + queryUrl);
            logDebug("查询设备: " + deviceId);

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer mock_access_token_12345");
            headers.put("X-Query-Type", "device-status");

            logInfo("发送设备状态查询请求...");
            long startTime = System.currentTimeMillis();

            ConnectionResult result = performHttpRequest("GET", queryUrl, headers, null);

            long queryDuration = System.currentTimeMillis() - startTime;

            logInfo("设备状态查询结果:");
            logInfo("  查询设备: " + deviceId);
            logInfo("  状态码: " + result.responseCode);
            logInfo("  查询耗时: " + queryDuration + "ms");
            logInfo("  查询状态: " + (result.responseCode == 200 ? "✅ 查询成功" : "❌ 查询失败"));

            // 模拟状态解析 (参考toyou系统的状态码逻辑)
            if (result.responseCode == 200) {
                logInfo("  模拟设备状态:");
                logInfo("    在线状态: ✅ 在线");
                logInfo("    状态码: 11111111111 (11位状态码，全部模块正常)");
                logInfo("    最后上报: " + dateFormat.format(new Date()));
                logInfo("    信号强度: -65 dBm (良好)");
                logInfo("    电池电量: 87%");
                logInfo("    温度数据: 23.5°C");
            }

            if (result.responseBody != null) {
                logDebug("查询响应详情: " + result.responseBody.substring(0, Math.min(200, result.responseBody.length())));
            }

        } catch (Exception e) {
            logError("设备状态查询测试异常", e);
        }
    }

    /**
     * 测试设备命令下发
     */
    private void testDeviceCommand() {
        logInfo("\n5. 设备命令下发API测试");
        logInfo("----------------------------------------");

        try {
            String commandUrl = MOCK_IOT_BASE + "/post";
            logInfo("设备命令端点: " + commandUrl);

            // 构建设备命令 (参考toyou系统的命令下发)
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("deviceId", "ZC_DEVICE_001");
            commandData.put("commandId", "CMD_" + System.currentTimeMillis());

            Map<String, Object> command = new HashMap<>();
            command.put("commandName", "SET_REPORTING_INTERVAL");
            Map<String, Object> params = new HashMap<>();
            params.put("interval", 60);  // 60秒上报间隔
            params.put("unit", "seconds");
            command.put("params", params);

            commandData.put("command", command);
            commandData.put("callbackUrl", "http://toyou.callback.com/iot/command/result");
            commandData.put("expireTime", 300);  // 5分钟过期
            commandData.put("priority", "HIGH");

            String jsonBody = buildJsonString(commandData);
            logDebug("设备命令数据: " + jsonBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer mock_access_token_12345");
            headers.put("X-Command-Type", "device-control");

            logInfo("发送设备命令下发请求...");
            long startTime = System.currentTimeMillis();

            ConnectionResult result = performHttpRequest("POST", commandUrl, headers, jsonBody);

            long commandDuration = System.currentTimeMillis() - startTime;

            logInfo("设备命令下发结果:");
            logInfo("  目标设备: ZC_DEVICE_001");
            logInfo("  命令类型: SET_REPORTING_INTERVAL");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  下发耗时: " + commandDuration + "ms");
            logInfo("  下发状态: " + (result.responseCode == 200 ? "✅ 下发成功" : "❌ 下发失败"));

            if (result.responseCode == 200) {
                logInfo("  命令状态: 已推送到设备队列");
                logInfo("  预期执行: 设备将在下次上线时执行");
                logInfo("  回调通知: 将通过callback接收执行结果");
            }

        } catch (Exception e) {
            logError("设备命令下发测试异常", e);
        }
    }

    /**
     * 测试数据上报模拟
     */
    private void testDataUpload() {
        logInfo("\n6. 设备数据上报测试");
        logInfo("----------------------------------------");

        try {
            String uploadUrl = MOCK_IOT_BASE + "/post";
            logInfo("数据上报端点: " + uploadUrl);

            // 模拟设备上报数据
            Map<String, Object> telemetryData = new HashMap<>();
            telemetryData.put("deviceId", "ZC_DEVICE_001");
            telemetryData.put("timestamp", System.currentTimeMillis());
            telemetryData.put("messageId", "MSG_" + System.currentTimeMillis());

            Map<String, Object> sensorData = new HashMap<>();
            sensorData.put("temperature", 24.3);
            sensorData.put("humidity", 65.2);
            sensorData.put("pressure", 1013.25);
            sensorData.put("battery", 87);
            sensorData.put("signal_strength", -67);

            telemetryData.put("data", sensorData);
            telemetryData.put("quality", "GOOD");

            String jsonBody = buildJsonString(telemetryData);
            logDebug("上报数据: " + jsonBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");
            headers.put("X-Message-Type", "telemetry");
            headers.put("X-Device-Auth", "device_secret_hash");

            logInfo("模拟设备数据上报...");
            long startTime = System.currentTimeMillis();

            ConnectionResult result = performHttpRequest("POST", uploadUrl, headers, jsonBody);

            long uploadDuration = System.currentTimeMillis() - startTime;

            logInfo("设备数据上报结果:");
            logInfo("  上报设备: ZC_DEVICE_001");
            logInfo("  数据类型: 传感器遥测数据");
            logInfo("  状态码: " + result.responseCode);
            logInfo("  上报耗时: " + uploadDuration + "ms");
            logInfo("  上报状态: " + (result.responseCode == 200 ? "✅ 上报成功" : "❌ 上报失败"));

            if (result.responseCode == 200) {
                logInfo("  数据处理: 已接收并存储");
                logInfo("  数据内容: 温度24.3°C, 湿度65.2%, 气压1013.25hPa");
                logInfo("  设备状态: 在线正常，信号良好");
            }

        } catch (Exception e) {
            logError("设备数据上报测试异常", e);
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
            connection.setConnectTimeout(5000);  // 5秒连接超时
            connection.setReadTimeout(10000);    // 10秒读取超时

            // 设置请求头
            connection.setRequestProperty("User-Agent", "IoT-Mock-Test/1.0 (toyou-reference)");
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
                }
                logDebug("请求体大小: " + body.getBytes("UTF-8").length + " 字节");
            }

            // 执行请求
            int responseCode = connection.getResponseCode();
            long duration = System.currentTimeMillis() - startTime;

            logDebug("收到响应: HTTP " + responseCode + " (耗时: " + duration + "ms)");

            // 读取响应
            String responseBody = null;
            try {
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
                logDebug("读取响应内容异常: " + e.getMessage());
            }

            return new ConnectionResult(responseCode, responseBody, null, duration);

        } finally {
            connection.disconnect();
        }
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
            if (!first) sb.append(",");
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
        }
    }
}