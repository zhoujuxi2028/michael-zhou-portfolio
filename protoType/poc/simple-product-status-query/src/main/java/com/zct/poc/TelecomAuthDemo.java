package com.zct.poc;

/**
 * 中国电信物联网认证演示
 * 演示如何集成中国电信物联网认证机制
 */
public class TelecomAuthDemo {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("中国电信物联网认证演示");
        System.out.println("==========================================");

        TelecomAuthDemo demo = new TelecomAuthDemo();

        // 1. 测试Token生成
        demo.testTokenGeneration();

        // 2. 测试设备认证
        demo.testDeviceAuthentication();

        // 3. 结合产品状态查询进行认证
        demo.testAuthenticatedQuery();

        System.out.println("\n==========================================");
        System.out.println("认证演示完成!");
        System.out.println("==========================================");
    }

    /**
     * 测试Token生成
     */
    private void testTokenGeneration() {
        System.out.println("\n1. 测试Token生成");
        System.out.println("----------------------------------------");

        String appKey = "test_app_key_12345";
        String appSecret = "test_app_secret_67890";

        String token = SimpleQueryExample.TelecomAuthExample.generateAccessToken(appKey, appSecret);
        System.out.println("生成的Token: " + token);

        // 验证Token格式
        if (token != null && token.startsWith("telecom_token_")) {
            System.out.println("✅ Token格式正确");
        } else {
            System.out.println("❌ Token格式错误");
        }
    }

    /**
     * 测试设备认证
     */
    private void testDeviceAuthentication() {
        System.out.println("\n2. 测试设备认证");
        System.out.println("----------------------------------------");

        // 测试用例：有效的认证信息
        String[][] testCases = {
            {"device_001", "123456789012345", "460012345678901", "有效认证信息"},
            {"device_002", "987654321098765", "460098765432109", "有效认证信息2"},
            {"device_003", "invalid_imei", "460012345678901", "无效IMEI"},
            {"device_004", "123456789012345", "123456789012345", "无效IMSI(非460开头)"},
            {"device_005", "123456789012345", "46001234567890", "无效IMSI(长度错误)"}
        };

        System.out.println("| 设备ID      | IMEI            | IMSI            | 认证结果 | 说明           |");
        System.out.println("|-------------|-----------------|-----------------|----------|----------------|");

        for (String[] testCase : testCases) {
            String deviceId = testCase[0];
            String imei = testCase[1];
            String imsi = testCase[2];
            String description = testCase[3];

            boolean authResult = SimpleQueryExample.TelecomAuthExample.authenticateDevice(deviceId, imei, imsi);
            String result = authResult ? "✅ 成功" : "❌ 失败";

            System.out.printf("| %-11s | %-15s | %-15s | %-8s | %-14s |\n",
                    deviceId, imei, imsi, result, description);
        }
    }

    /**
     * 测试集成认证的产品状态查询
     */
    private void testAuthenticatedQuery() {
        System.out.println("\n3. 集成认证的产品状态查询");
        System.out.println("----------------------------------------");

        SimpleQueryExample example = new SimpleQueryExample();

        // 模拟认证成功的设备查询
        String deviceId = "authenticated_device_001";
        String imei = "123456789012345";
        String imsi = "460012345678901";

        System.out.println("步骤1: 设备认证");
        boolean authResult = SimpleQueryExample.TelecomAuthExample.authenticateDevice(deviceId, imei, imsi);

        if (authResult) {
            System.out.println("步骤2: 认证成功，开始查询产品状态");
            SimpleQueryExample.ProductStatus status = example.queryProductStatus("station_auth_001");

            System.out.println("\n认证查询结果:");
            System.out.println("  认证状态: ✅ 已认证");
            System.out.println("  设备名称: " + status.getDeviceName());
            System.out.println("  在线状态: " + (status.getOnlineStatus() == 1 ? "在线" : "离线"));
            System.out.println("  状态描述: " + status.getStatusDescription());
        } else {
            System.out.println("❌ 设备认证失败，拒绝查询");
        }

        System.out.println("\n模拟认证失败的场景:");
        boolean failedAuth = SimpleQueryExample.TelecomAuthExample.authenticateDevice("invalid_device", "invalid", "invalid");
        if (!failedAuth) {
            System.out.println("❌ 认证失败，已拒绝访问");
        }
    }

    /**
     * 演示完整的认证流程
     */
    public static class AuthFlow {

        /**
         * 完整的认证查询流程
         */
        public static void performAuthenticatedQuery(String deviceId, String imei, String imsi, String lbsId) {
            System.out.println("\n执行认证查询流程:");
            System.out.println("设备ID: " + deviceId);
            System.out.println("LbsId: " + lbsId);

            // Step 1: 生成Token
            System.out.println("\n[STEP 1] 生成访问Token...");
            String token = SimpleQueryExample.TelecomAuthExample.generateAccessToken("app_key", "app_secret");

            // Step 2: 设备认证
            System.out.println("\n[STEP 2] 设备认证...");
            boolean authResult = SimpleQueryExample.TelecomAuthExample.authenticateDevice(deviceId, imei, imsi);

            if (!authResult) {
                System.out.println("❌ 认证失败，流程终止");
                return;
            }

            // Step 3: 查询产品状态
            System.out.println("\n[STEP 3] 查询产品状态...");
            SimpleQueryExample example = new SimpleQueryExample();
            SimpleQueryExample.ProductStatus status = example.queryProductStatus(lbsId);

            // Step 4: 返回结果
            System.out.println("\n[STEP 4] 查询结果:");
            System.out.println("✅ 认证成功，状态查询完成");
            System.out.println("设备状态: " + status.getStatusDescription());
            System.out.println("在线状态: " + (status.getOnlineStatus() == 1 ? "在线" : "离线"));
        }
    }
}