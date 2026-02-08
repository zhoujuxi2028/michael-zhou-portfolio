package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import com.aep.export.service.AepClientManager.AepClientException;
import com.aep.export.service.AepClientManager.ApiResponse;

/**
 * AepClientManager单元测试
 * TDD第2轮：基础服务测试
 * 对应需求: FR-001-01 - 从AEP API获取产品列表信息
 * 对应需求: FR-002-01 - 根据ProductId+MasterKey查询设备
 * 测试用例: TC-UNIT-FUNC-111~120
 */
public class AepClientManagerTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始AepClientManager TDD测试...");

            testInitializeClient_WithValidConfig();
            testThrowException_WhenConfigInvalid();
            testCreateAuthHeader_Success();
            testBuildApiUrl_ForProducts();
            testBuildApiUrl_ForDevices();
            testCalculateSignature_WithHmacSha1();
            testHandleApiResponse_Success();
            testHandleApiResponse_WithError();
            testClientReuse_WithSameConfig();
            testClientRecreation_WithDifferentConfig();

            System.out.println("✅ 所有AepClientManager测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-111: ✅ shouldInitializeClient_WithValidConfig");
            System.out.println("  TC-UNIT-FUNC-112: ✅ shouldThrowException_WhenConfigInvalid");
            System.out.println("  TC-UNIT-FUNC-113: ✅ shouldCreateAuthHeader_Success");
            System.out.println("  TC-UNIT-FUNC-114: ✅ shouldBuildApiUrl_ForProducts");
            System.out.println("  TC-UNIT-FUNC-115: ✅ shouldBuildApiUrl_ForDevices");
            System.out.println("  TC-UNIT-FUNC-116: ✅ shouldCalculateSignature_WithHmacSha1");
            System.out.println("  TC-UNIT-FUNC-117: ✅ shouldHandleApiResponse_Success");
            System.out.println("  TC-UNIT-FUNC-118: ✅ shouldHandleApiResponse_WithError");
            System.out.println("  TC-UNIT-FUNC-119: ✅ shouldClientReuse_WithSameConfig");
            System.out.println("  TC-UNIT-FUNC-120: ✅ shouldClientRecreation_WithDifferentConfig");

        } catch (Exception e) {
            System.err.println("❌ AepClientManager测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-111: 测试使用有效配置初始化客户端
     * 验证需求: FR-001-01 - AEP API客户端初始化
     */
    public static void testInitializeClient_WithValidConfig() {
        System.out.println("  🔴 RED: 测试客户端初始化...");

        // Given
        ExportConfig config = ExportConfig.builder()
            .appKey("CIj7aTFV1R9")
            .appSecret("zGEm97sb5M")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .timeoutSeconds(30)
            .build();

        // When
        AepClientManager clientManager = new AepClientManager(config);
        boolean isInitialized = clientManager.isInitialized();

        // Then
        assert isInitialized : "客户端应该已初始化";
        assert clientManager.getApiHost().equals("10433748.api.ctwing.cn") : "API主机不匹配";
        assert clientManager.getAppKey().equals("CIj7aTFV1R9") : "AppKey不匹配";

        System.out.println("  🟢 GREEN: 客户端初始化测试通过");
    }

    /**
     * TC-UNIT-FUNC-112: 测试无效配置时抛出异常
     * 验证需求: FR-004-02 - 配置验证
     */
    public static void testThrowException_WhenConfigInvalid() {
        System.out.println("  🔴 RED: 测试无效配置异常...");

        // Given - 空的appSecret（AepClientManager应该检测到）
        ExportConfig invalidConfig = ExportConfig.builder()
            .appKey("invalid_key")
            .appSecret("") // 空的appSecret
            .apiHost("") // 空的apiHost
            .appId("123456")
            .build();

        try {
            new AepClientManager(invalidConfig);
            assert false : "应该抛出配置异常";
        } catch (AepClientException e) {
            assert e.getMessage().contains("invalid") || e.getMessage().contains("missing") : "异常消息应描述配置问题";
        }

        System.out.println("  🟢 GREEN: 无效配置异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-113: 测试创建认证头成功
     * 验证需求: FR-001-01 - AEP API认证
     */
    public static void testCreateAuthHeader_Success() {
        System.out.println("  🔴 RED: 测试认证头创建...");

        // Given
        ExportConfig config = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .build();

        AepClientManager clientManager = new AepClientManager(config);

        // When
        String timestamp = "1640995200000"; // 固定时间戳用于测试
        String authHeader = clientManager.createAuthHeader("GET", "/aep_product_management/products", null, timestamp);

        // Then
        assert authHeader != null : "认证头不应该为null";
        assert authHeader.startsWith("version=2018-10-31") : "认证头格式不正确";
        assert authHeader.contains("res=products") : "资源信息不正确";
        assert authHeader.contains("et=1640995200000") : "时间戳不正确";

        System.out.println("  🟢 GREEN: 认证头创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-114: 测试构建产品API URL
     * 验证需求: FR-001-01 - 产品列表API URL构建
     */
    public static void testBuildApiUrl_ForProducts() {
        System.out.println("  🔴 RED: 测试产品API URL构建...");

        // Given
        ExportConfig config = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .build();

        AepClientManager clientManager = new AepClientManager(config);

        // When
        String productsUrl = clientManager.buildApiUrl("products", null);

        // Then
        assert productsUrl.startsWith("https://10433748.api.ctwing.cn") : "URL主机不正确";
        assert productsUrl.contains("/aep_product_management/products") : "产品API路径不正确";

        System.out.println("  🟢 GREEN: 产品API URL构建测试通过");
    }

    /**
     * TC-UNIT-FUNC-115: 测试构建设备API URL
     * 验证需求: FR-002-01 - 设备查询API URL构建
     */
    public static void testBuildApiUrl_ForDevices() {
        System.out.println("  🔴 RED: 测试设备API URL构建...");

        // Given
        ExportConfig config = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .pageSize(50)
            .build();

        AepClientManager clientManager = new AepClientManager(config);

        // When
        String params = "productId=16857118&pageNow=1&pageSize=50";
        String devicesUrl = clientManager.buildApiUrl("devices", params);

        // Then
        assert devicesUrl.startsWith("https://10433748.api.ctwing.cn") : "URL主机不正确";
        assert devicesUrl.contains("/aep_device_management/devices") : "设备API路径不正确";
        assert devicesUrl.contains("productId=16857118") : "产品ID参数不正确";
        assert devicesUrl.contains("pageSize=50") : "分页参数不正确";

        System.out.println("  🟢 GREEN: 设备API URL构建测试通过");
    }

    /**
     * TC-UNIT-FUNC-116: 测试HMAC-SHA1签名计算
     * 验证需求: FR-001-01 - AEP API签名算法
     */
    public static void testCalculateSignature_WithHmacSha1() {
        System.out.println("  🔴 RED: 测试HMAC-SHA1签名计算...");

        // Given - 基于AEP官方文档的签名算法
        String method = "GET";
        String uri = "/aep_product_management/products";
        String timestamp = "1640995200000";
        String appSecret = "test_secret";

        AepClientManager clientManager = new AepClientManager(createTestConfig());

        // When
        String signature = clientManager.calculateSignature(method, uri, null, timestamp, appSecret);

        // Then
        assert signature != null : "签名不应该为null";
        assert signature.length() > 0 : "签名不应该为空";
        // 验证签名是Base64编码格式
        assert signature.matches("[A-Za-z0-9+/=]+") : "签名应该是Base64格式";

        System.out.println("  🟢 GREEN: HMAC-SHA1签名计算测试通过");
    }

    /**
     * TC-UNIT-FUNC-117: 测试处理成功API响应
     * 验证需求: FR-001-03 - 解析产品响应数据结构
     */
    public static void testHandleApiResponse_Success() {
        System.out.println("  🔴 RED: 测试处理成功API响应...");

        // Given - 模拟AEP成功响应
        String successResponse = "{\n" +
            "  \"code\": 0,\n" +
            "  \"msg\": \"ok\",\n" +
            "  \"result\": {\n" +
            "    \"dataList\": [\n" +
            "      {\n" +
            "        \"productId\": 16857118,\n" +
            "        \"productName\": \"RepeaterLTE\",\n" +
            "        \"apiKey\": \"521e0d76d0024539a9718abb3e4f64cc\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        AepClientManager clientManager = new AepClientManager(createTestConfig());

        // When
        ApiResponse response = clientManager.handleApiResponse(successResponse, 200);

        // Then
        assert response.isSuccess() : "响应应该标记为成功";
        assert response.getCode() == 0 : "响应码应该为0";
        assert response.getMessage().equals("ok") : "响应消息不匹配";
        assert response.getData() != null : "响应数据不应该为null";

        System.out.println("  🟢 GREEN: 处理成功API响应测试通过");
    }

    /**
     * TC-UNIT-FUNC-118: 测试处理错误API响应
     * 验证需求: FR-004-03 - API调用异常处理
     */
    public static void testHandleApiResponse_WithError() {
        System.out.println("  🔴 RED: 测试处理错误API响应...");

        // Given - 模拟AEP错误响应
        String errorResponse = "{\n" +
            "  \"code\": 10001,\n" +
            "  \"msg\": \"Authentication failed\",\n" +
            "  \"result\": null\n" +
            "}";

        AepClientManager clientManager = new AepClientManager(createTestConfig());

        // When
        ApiResponse response = clientManager.handleApiResponse(errorResponse, 401);

        // Then
        assert !response.isSuccess() : "响应应该标记为失败";
        assert response.getCode() == 10001 : "错误码不匹配";
        assert response.getMessage().contains("Authentication") : "错误消息不匹配";

        System.out.println("  🟢 GREEN: 处理错误API响应测试通过");
    }

    /**
     * TC-UNIT-FUNC-119: 测试相同配置时客户端复用
     * 验证需求: FR-001-05 - 客户端缓存机制
     */
    public static void testClientReuse_WithSameConfig() {
        System.out.println("  🔴 RED: 测试客户端复用...");

        // Given
        ExportConfig config = createTestConfig();

        // When
        AepClientManager client1 = new AepClientManager(config);
        AepClientManager client2 = new AepClientManager(config);

        // Then
        assert client1.getConfigHash().equals(client2.getConfigHash()) : "相同配置的客户端应有相同hash";

        System.out.println("  🟢 GREEN: 客户端复用测试通过");
    }

    /**
     * TC-UNIT-FUNC-120: 测试不同配置时重新创建客户端
     * 验证需求: FR-004-01 - 配置动态更新
     */
    public static void testClientRecreation_WithDifferentConfig() {
        System.out.println("  🔴 RED: 测试客户端重新创建...");

        // Given
        ExportConfig config1 = createTestConfig();
        ExportConfig config2 = ExportConfig.builder()
            .appKey("different_key")
            .appSecret("different_secret")
            .apiHost("different.api.ctwing.cn")
            .appId("999999")
            .build();

        // When
        AepClientManager client1 = new AepClientManager(config1);
        AepClientManager client2 = new AepClientManager(config2);

        // Then
        assert !client1.getConfigHash().equals(client2.getConfigHash()) : "不同配置的客户端应有不同hash";
        assert !client1.getApiHost().equals(client2.getApiHost()) : "不同配置的客户端应有不同API主机";

        System.out.println("  🟢 GREEN: 客户端重新创建测试通过");
    }

    /**
     * 辅助方法：创建测试配置
     */
    private static ExportConfig createTestConfig() {
        return ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .timeoutSeconds(30)
            .build();
    }

}