package com.zct.poc;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/**
 * 参考vendor-b系统实现的中国电信物联网认证示例
 * 模拟vendor-b系统与中国电信IoT平台的认证交互
 */
public class Vendor-BAuthExample {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("vendor-b系统电信物联网认证演示");
        System.out.println("参考vendor-b系统的认证实现");
        System.out.println("==========================================");

        Vendor-BAuthExample example = new Vendor-BAuthExample();

        // 1. 模拟vendor-b系统的认证配置
        example.demoIotPlatformConfig();

        // 2. 演示认证流程
        example.demoAuthenticationFlow();

        // 3. 演示设备注册与状态查询
        example.demoDeviceRegistrationAndQuery();

        System.out.println("\n==========================================");
        System.out.println("vendor-b认证演示完成!");
        System.out.println("==========================================");
    }

    /**
     * 模拟vendor-b系统的IoT平台配置
     * 参考IotPlatConfig.java
     */
    private void demoIotPlatformConfig() {
        System.out.println("\n1. IoT平台配置 (参考IotPlatConfig.java)");
        System.out.println("----------------------------------------");

        String baseUrl = "https://iot.platform.chinatelecom.com";
        IotPlatformConfig config = new IotPlatformConfig(baseUrl);

        System.out.println("基础URL: " + baseUrl);
        System.out.println("认证端点: " + config.getAppAuth());
        System.out.println("设备注册端点: " + config.getRegisterDevice());
        System.out.println("设备查询端点: " + config.getQueryDeviceStatus());
        System.out.println("Token刷新端点: " + config.getRefreshToken());
    }

    /**
     * 演示认证流程
     * 参考AuthUtils.java
     */
    private void demoAuthenticationFlow() {
        System.out.println("\n2. 认证流程演示 (参考AuthUtils.java)");
        System.out.println("----------------------------------------");

        // 模拟vendor-b系统的认证参数
        String appId = "vendor-b_app_12345";
        String secret = "vendor-b_secret_67890";

        Vendor-BAuthUtils authUtils = new Vendor-BAuthUtils();

        try {
            // 执行认证
            String accessToken = authUtils.login(appId, secret);
            System.out.println("✅ 认证成功!");
            System.out.println("Access Token: " + accessToken.substring(0, 20) + "...");

            // 验证Token
            boolean isValid = authUtils.validateToken(accessToken);
            System.out.println("Token验证: " + (isValid ? "有效" : "无效"));

        } catch (Exception e) {
            System.out.println("❌ 认证失败: " + e.getMessage());
        }
    }

    /**
     * 演示设备注册与状态查询
     * 参考TDeviceService.java
     */
    private void demoDeviceRegistrationAndQuery() {
        System.out.println("\n3. 设备注册与状态查询 (参考TDeviceService.java)");
        System.out.println("----------------------------------------");

        Vendor-BDeviceService deviceService = new Vendor-BDeviceService();

        // 设备信息
        String deviceId = "vendor-b_device_001";
        String lbsId = "station001";
        String imei = "123456789012345";
        String imsi = "460012345678901";

        try {
            // 1. 设备认证
            System.out.println("步骤1: 设备认证");
            boolean authResult = deviceService.authenticateDevice(deviceId, imei, imsi);
            System.out.println("设备认证结果: " + (authResult ? "✅ 成功" : "❌ 失败"));

            if (authResult) {
                // 2. 注册设备到电信IoT平台
                System.out.println("\n步骤2: 注册设备到电信IoT平台");
                boolean registerResult = deviceService.registerDeviceToIotPlatform(deviceId, lbsId);
                System.out.println("设备注册结果: " + (registerResult ? "✅ 成功" : "❌ 失败"));

                // 3. 查询设备状态
                System.out.println("\n步骤3: 查询设备状态");
                DeviceStatus status = deviceService.queryDeviceStatus(lbsId);
                if (status != null) {
                    System.out.println("设备状态查询成功:");
                    System.out.println("  设备ID: " + status.getDeviceId());
                    System.out.println("  在线状态: " + status.getOnlineStatus());
                    System.out.println("  状态码: " + status.getStatusCode());
                    System.out.println("  最后上报: " + status.getLastReportTime());
                    System.out.println("  电信平台状态: " + status.getIotPlatformStatus());
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 设备操作失败: " + e.getMessage());
        }
    }

    /**
     * IoT平台配置类 (参考IotPlatConfig.java)
     */
    static class IotPlatformConfig {
        private String baseUrl;
        private String appAuth;
        private String registerDevice;
        private String queryDeviceStatus;
        private String refreshToken;

        public IotPlatformConfig(String baseUrl) {
            this.baseUrl = baseUrl;
            initializeEndpoints();
        }

        private void initializeEndpoints() {
            // 参考vendor-b系统的端点配置
            this.appAuth = baseUrl + "/iocm/app/sec/v1.1.0/login";
            this.refreshToken = baseUrl + "/iocm/app/sec/v1.1.0/refreshToken";
            this.registerDevice = baseUrl + "/iocm/app/reg/v1.1.0/deviceCredentials";
            this.queryDeviceStatus = baseUrl + "/iocm/app/dm/v1.4.0/devices";
        }

        // Getters
        public String getAppAuth() { return appAuth; }
        public String getRegisterDevice() { return registerDevice; }
        public String getQueryDeviceStatus() { return queryDeviceStatus; }
        public String getRefreshToken() { return refreshToken; }
    }

    /**
     * 认证工具类 (参考AuthUtils.java)
     */
    static class Vendor-BAuthUtils {

        public String login(String appId, String secret) throws Exception {
            System.out.println("[AUTH] 开始认证到中国电信IoT平台...");
            System.out.println("[AUTH] AppID: " + appId);
            System.out.println("[AUTH] Secret: " + maskSecret(secret));

            // 模拟HTTPS请求到电信IoT平台
            // 参考vendor-b系统的实现逻辑
            Map<String, String> params = new HashMap<>();
            params.put("appId", appId);
            params.put("secret", secret);

            // 模拟HTTP请求
            SimulatedHttpResponse response = simulateHttpsRequest("/iocm/app/sec/v1.1.0/login", params);

            if (response.getStatusCode() != 200) {
                throw new Exception("认证失败: HTTP " + response.getStatusCode());
            }

            // 解析响应获取accessToken
            Map<String, Object> responseData = parseJsonResponse(response.getBody());
            String accessToken = (String) responseData.get("accessToken");

            System.out.println("[AUTH] 认证成功，获得Token");
            return accessToken;
        }

        public boolean validateToken(String token) {
            // 简单的Token验证逻辑
            return token != null && token.startsWith("telecom_token_");
        }

        private String maskSecret(String secret) {
            if (secret == null || secret.length() < 8) return "***";
            return secret.substring(0, 4) + "***" + secret.substring(secret.length() - 4);
        }

        private SimulatedHttpResponse simulateHttpsRequest(String endpoint, Map<String, String> params) {
            // 模拟网络延迟
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 模拟认证成功响应
            String responseBody = "{\"accessToken\":\"telecom_token_" + System.currentTimeMillis() + "\",\"expiresIn\":3600}";
            return new SimulatedHttpResponse(200, responseBody);
        }

        private Map<String, Object> parseJsonResponse(String json) {
            // 简单的JSON解析模拟
            Map<String, Object> result = new HashMap<>();
            if (json.contains("accessToken")) {
                String token = json.substring(json.indexOf("accessToken\":\"") + 15);
                token = token.substring(0, token.indexOf("\""));
                result.put("accessToken", token);
            }
            return result;
        }
    }

    /**
     * 设备服务类 (参考TDeviceService.java)
     */
    static class Vendor-BDeviceService {

        public boolean authenticateDevice(String deviceId, String imei, String imsi) {
            System.out.println("[DEVICE] 验证设备认证信息...");
            System.out.println("[DEVICE] 设备ID: " + deviceId);
            System.out.println("[DEVICE] IMEI: " + imei);
            System.out.println("[DEVICE] IMSI: " + imsi);

            // IMEI格式验证 (15位数字)
            if (!imei.matches("\\d{15}")) {
                System.out.println("[DEVICE] IMEI格式无效");
                return false;
            }

            // IMSI格式验证 (中国电信以460开头)
            if (!imsi.matches("460\\d{12}")) {
                System.out.println("[DEVICE] IMSI格式无效或不属于中国电信");
                return false;
            }

            // 模拟认证逻辑
            boolean result = Math.abs(deviceId.hashCode()) % 10 < 8; // 80%成功率
            System.out.println("[DEVICE] 设备认证" + (result ? "成功" : "失败"));
            return result;
        }

        public boolean registerDeviceToIotPlatform(String deviceId, String lbsId) throws Exception {
            System.out.println("[DEVICE] 向电信IoT平台注册设备...");

            // 1. 获取认证Token
            Vendor-BAuthUtils authUtils = new Vendor-BAuthUtils();
            String token = authUtils.login("vendor-b_app", "vendor-b_secret");

            // 2. 构建设备注册请求
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", deviceId);
            deviceInfo.put("lbsId", lbsId);
            deviceInfo.put("deviceType", "4G_DEVICE");
            deviceInfo.put("manufacturer", "ZhongCheng");

            // 3. 发送注册请求到电信IoT平台
            SimulatedHttpResponse response = simulateIotPlatformRequest(
                "/iocm/app/reg/v1.1.0/deviceCredentials", deviceInfo, token);

            boolean success = response.getStatusCode() == 200;
            System.out.println("[DEVICE] 设备注册" + (success ? "成功" : "失败"));
            return success;
        }

        public DeviceStatus queryDeviceStatus(String lbsId) {
            System.out.println("[DEVICE] 查询设备状态: " + lbsId);

            // 模拟从vendor-b本地数据库查询
            DeviceStatus localStatus = queryLocalDeviceStatus(lbsId);

            // 模拟从电信IoT平台查询
            DeviceStatus iotStatus = queryIotPlatformStatus(lbsId);

            // 合并状态信息
            DeviceStatus mergedStatus = mergeDeviceStatus(localStatus, iotStatus);

            return mergedStatus;
        }

        private DeviceStatus queryLocalDeviceStatus(String lbsId) {
            // 模拟本地数据库查询 (参考vendor-b的TDeviceService.listByLbsId)
            DeviceStatus status = new DeviceStatus();
            status.setDeviceId("device_" + lbsId.hashCode());
            status.setLbsId(lbsId);
            status.setOnlineStatus(Math.random() > 0.3 ? "在线" : "离线");
            status.setStatusCode(generateStatusCode(lbsId));
            status.setLastReportTime(new Date());
            return status;
        }

        private DeviceStatus queryIotPlatformStatus(String lbsId) {
            // 模拟从电信IoT平台查询设备状态
            DeviceStatus status = new DeviceStatus();
            status.setIotPlatformStatus(Math.random() > 0.2 ? "正常" : "异常");
            return status;
        }

        private DeviceStatus mergeDeviceStatus(DeviceStatus local, DeviceStatus iot) {
            local.setIotPlatformStatus(iot.getIotPlatformStatus());
            return local;
        }

        private String generateStatusCode(String lbsId) {
            // 参考vendor-b系统的状态码生成逻辑
            int hash = Math.abs(lbsId.hashCode()) % 3;
            switch (hash) {
                case 0: return "11111111111"; // 全部正常
                case 1: return "11110111111"; // 部分异常
                default: return "00000000000"; // 离线
            }
        }

        private SimulatedHttpResponse simulateIotPlatformRequest(String endpoint, Map<String, Object> data, String token) {
            // 模拟IoT平台API调用
            try {
                Thread.sleep(200); // 模拟网络延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return new SimulatedHttpResponse(200, "{\"result\":\"success\",\"deviceId\":\"" + data.get("deviceId") + "\"}");
        }
    }

    /**
     * 设备状态类
     */
    static class DeviceStatus {
        private String deviceId;
        private String lbsId;
        private String onlineStatus;
        private String statusCode;
        private Date lastReportTime;
        private String iotPlatformStatus;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getLbsId() { return lbsId; }
        public void setLbsId(String lbsId) { this.lbsId = lbsId; }

        public String getOnlineStatus() { return onlineStatus; }
        public void setOnlineStatus(String onlineStatus) { this.onlineStatus = onlineStatus; }

        public String getStatusCode() { return statusCode; }
        public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

        public Date getLastReportTime() { return lastReportTime; }
        public void setLastReportTime(Date lastReportTime) { this.lastReportTime = lastReportTime; }

        public String getIotPlatformStatus() { return iotPlatformStatus; }
        public void setIotPlatformStatus(String iotPlatformStatus) { this.iotPlatformStatus = iotPlatformStatus; }
    }

    /**
     * 模拟HTTP响应
     */
    static class SimulatedHttpResponse {
        private int statusCode;
        private String body;

        public SimulatedHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
    }
}