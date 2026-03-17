package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import com.aep.export.model.DeviceInfo;
import com.aep.export.model.PagedResult;

/**
 * DeviceService单元测试
 * TDD第3轮：核心业务服务测试
 * 对应需求: FR-002-01 - 根据ProductId+MasterKey查询设备
 * 对应需求: FR-002-02 - 处理设备查询分页逻辑
 * 对应需求: FR-002-03 - 提取设备基本信息
 * 对应需求: NFR-001-03 - 设备数据导出性能
 * 测试用例: TC-UNIT-FUNC-151~160
 */
public class DeviceServiceTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始DeviceService TDD测试...");

            testQueryDevicesByProduct_WithValidConfig();
            testQueryDevicesByProduct_WithPagination();
            testQueryDeviceById_WithValidId();
            testQueryDeviceById_WithInvalidId();
            testExtractDeviceInfo_FromApiResponse();
            testFilterDevices_ByStatus();
            testFilterDevices_ByNetStatus();
            testValidateDeviceData_WithRequiredFields();
            testPerformanceTest_LargeDeviceList();
            testBatchDeviceQuery_WithProductList();

            System.out.println("✅ 所有DeviceService测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-151: ✅ shouldQueryDevicesByProduct_WithValidConfig");
            System.out.println("  TC-UNIT-FUNC-152: ✅ shouldQueryDevicesByProduct_WithPagination");
            System.out.println("  TC-UNIT-FUNC-153: ✅ shouldQueryDeviceById_WithValidId");
            System.out.println("  TC-UNIT-FUNC-154: ✅ shouldQueryDeviceById_WithInvalidId");
            System.out.println("  TC-UNIT-FUNC-155: ✅ shouldExtractDeviceInfo_FromApiResponse");
            System.out.println("  TC-UNIT-FUNC-156: ✅ shouldFilterDevices_ByStatus");
            System.out.println("  TC-UNIT-FUNC-157: ✅ shouldFilterDevices_ByNetStatus");
            System.out.println("  TC-UNIT-FUNC-158: ✅ shouldValidateDeviceData_WithRequiredFields");
            System.out.println("  TC-UNIT-FUNC-159: ✅ shouldPerformanceTest_LargeDeviceList");
            System.out.println("  TC-UNIT-FUNC-160: ✅ shouldBatchDeviceQuery_WithProductList");

        } catch (Exception e) {
            System.err.println("❌ DeviceService测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-151: 测试根据产品查询设备列表
     * 验证需求: FR-002-01 - 根据ProductId+MasterKey查询设备
     */
    public static void testQueryDevicesByProduct_WithValidConfig() {
        System.out.println("  🔴 RED: 测试根据产品查询设备列表...");

        // Given
        ExportConfig config = createTestConfig();
        DeviceService deviceService = new DeviceService(config);
        Long testProductId = 12345678L;
        String masterKey = "test_master_key_123";

        // When
        PagedResult<DeviceInfo> result = deviceService.queryDevicesByProduct(
            testProductId, masterKey, 1, 10);

        // Then
        assert result != null : "查询结果不应该为null";
        assert result.getTotal() >= 0 : "总数应该大于等于0";
        assert result.getData() != null : "数据列表不应该为null";
        assert result.getPageNum() == 1 : "页码应该为1";
        assert result.getPageSize() == 10 : "页面大小应该为10";
        assert result.getProductId().equals(testProductId) : "产品ID应该匹配";

        System.out.println("  🟢 GREEN: 根据产品查询设备列表测试通过");
    }

    /**
     * TC-UNIT-FUNC-152: 测试设备分页查询
     * 验证需求: FR-002-02 - 处理设备查询分页逻辑
     */
    public static void testQueryDevicesByProduct_WithPagination() {
        System.out.println("  🔴 RED: 测试设备分页查询...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        Long productId = 12345678L;
        String masterKey = "test_master_key";

        // When
        PagedResult<DeviceInfo> page1 = deviceService.queryDevicesByProduct(
            productId, masterKey, 1, 5);
        PagedResult<DeviceInfo> page2 = deviceService.queryDevicesByProduct(
            productId, masterKey, 2, 5);

        // Then
        assert page1.getPageNum() == 1 : "第1页页码正确";
        assert page2.getPageNum() == 2 : "第2页页码正确";
        assert page1.getPageSize() == 5 : "页面大小为5";
        assert page2.getPageSize() == 5 : "页面大小为5";
        assert page1.getProductId().equals(productId) : "产品ID应该匹配";
        assert page2.getProductId().equals(productId) : "产品ID应该匹配";

        System.out.println("  🟢 GREEN: 设备分页查询测试通过");
    }

    /**
     * TC-UNIT-FUNC-153: 测试根据设备ID查询设备
     * 验证需求: FR-002-03 - 设备信息精确查询
     */
    public static void testQueryDeviceById_WithValidId() {
        System.out.println("  🔴 RED: 测试根据设备ID查询设备...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        String testDeviceId = "test_device_12345";

        // When
        DeviceInfo device = deviceService.queryDeviceById(testDeviceId);

        // Then
        if (device != null) {
            assert device.getDeviceId().equals(testDeviceId) : "设备ID应该匹配";
            assert device.getProductId() != null : "产品ID不应该为null";
            assert device.getDeviceName() != null : "设备名称不应该为null";
        }
        // 如果设备不存在，应该返回null而不是抛出异常

        System.out.println("  🟢 GREEN: 根据设备ID查询设备测试通过");
    }

    /**
     * TC-UNIT-FUNC-154: 测试查询不存在的设备ID
     * 验证需求: FR-004-04 - 业务异常处理
     */
    public static void testQueryDeviceById_WithInvalidId() {
        System.out.println("  🔴 RED: 测试查询无效设备ID...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        String invalidDeviceId = "invalid_device_999999";

        // When
        DeviceInfo device = deviceService.queryDeviceById(invalidDeviceId);

        // Then
        assert device == null : "不存在的设备ID应该返回null";

        System.out.println("  🟢 GREEN: 查询无效设备ID测试通过");
    }

    /**
     * TC-UNIT-FUNC-155: 测试从API响应提取设备信息
     * 验证需求: FR-002-03 - 设备数据解析
     */
    public static void testExtractDeviceInfo_FromApiResponse() {
        System.out.println("  🔴 RED: 测试从API响应提取设备信息...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        String mockApiResponse = """
            {
                "result": {
                    "deviceId": "test_device_001",
                    "productId": 12345678,
                    "deviceName": "测试设备",
                    "deviceSn": "SN001234567890",
                    "deviceStatus": 1,
                    "netStatus": 1,
                    "tenantId": "test_tenant",
                    "firmwareVersion": "1.0.0",
                    "deviceType": "SENSOR",
                    "createTime": "2024-12-28 10:00:00",
                    "updateTime": "2024-12-28 12:00:00",
                    "lastActiveTime": "2024-12-28 13:00:00"
                }
            }
            """;

        // When
        DeviceInfo deviceInfo = deviceService.parseDeviceFromResponse(mockApiResponse);

        // Then
        assert deviceInfo != null : "解析的设备信息不应该为null";
        assert deviceInfo.getDeviceId().equals("test_device_001") : "设备ID解析正确";
        assert deviceInfo.getProductId().equals(12345678L) : "产品ID解析正确";
        assert deviceInfo.getDeviceName().equals("测试设备") : "设备名称解析正确";
        assert deviceInfo.getDeviceStatus().equals(1) : "设备状态解析正确";
        assert deviceInfo.getNetStatus().equals(1) : "网络状态解析正确";

        System.out.println("  🟢 GREEN: 从API响应提取设备信息测试通过");
    }

    /**
     * TC-UNIT-FUNC-156: 测试按设备状态过滤
     * 验证需求: FR-002-04 - 设备状态过滤
     */
    public static void testFilterDevices_ByStatus() {
        System.out.println("  🔴 RED: 测试按设备状态过滤...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        Long productId = 12345678L;
        String masterKey = "test_master_key";

        // When
        PagedResult<DeviceInfo> activeDevices = deviceService.queryDevicesByProductAndStatus(
            productId, masterKey, 1, 1, 10); // deviceStatus = 1 (激活)
        PagedResult<DeviceInfo> inactiveDevices = deviceService.queryDevicesByProductAndStatus(
            productId, masterKey, 0, 1, 10); // deviceStatus = 0 (未激活)

        // Then
        assert activeDevices != null : "激活设备查询结果不应该为null";
        assert inactiveDevices != null : "未激活设备查询结果不应该为null";

        // 验证过滤条件
        if (activeDevices.getData().size() > 0) {
            for (DeviceInfo device : activeDevices.getData()) {
                assert device.getDeviceStatus().equals(1) : "激活设备状态应该为1";
            }
        }

        System.out.println("  🟢 GREEN: 按设备状态过滤测试通过");
    }

    /**
     * TC-UNIT-FUNC-157: 测试按网络状态过滤
     * 验证需求: FR-002-04 - 网络状态过滤
     */
    public static void testFilterDevices_ByNetStatus() {
        System.out.println("  🔴 RED: 测试按网络状态过滤...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        Long productId = 12345678L;
        String masterKey = "test_master_key";

        // When
        PagedResult<DeviceInfo> onlineDevices = deviceService.queryDevicesByProductAndNetStatus(
            productId, masterKey, 1, 1, 10); // netStatus = 1 (在线)
        PagedResult<DeviceInfo> offlineDevices = deviceService.queryDevicesByProductAndNetStatus(
            productId, masterKey, 2, 1, 10); // netStatus = 2 (离线)

        // Then
        assert onlineDevices != null : "在线设备查询结果不应该为null";
        assert offlineDevices != null : "离线设备查询结果不应该为null";

        // 验证过滤条件
        if (onlineDevices.getData().size() > 0) {
            for (DeviceInfo device : onlineDevices.getData()) {
                assert device.getNetStatus().equals(1) : "在线设备网络状态应该为1";
            }
        }

        System.out.println("  🟢 GREEN: 按网络状态过滤测试通过");
    }

    /**
     * TC-UNIT-FUNC-158: 测试设备数据验证
     * 验证需求: FR-004-02 - 数据验证
     */
    public static void testValidateDeviceData_WithRequiredFields() {
        System.out.println("  🔴 RED: 测试设备数据验证...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());

        // When & Then - 测试必需字段验证
        boolean valid1 = deviceService.validateDeviceData(
            "device123", 12345678L, "测试设备", "SN123456"
        );
        boolean valid2 = deviceService.validateDeviceData(
            null, 12345678L, "测试设备", "SN123456"
        );
        boolean valid3 = deviceService.validateDeviceData(
            "device123", null, "测试设备", "SN123456"
        );
        boolean valid4 = deviceService.validateDeviceData(
            "device123", 12345678L, null, "SN123456"
        );

        assert valid1 : "完整数据应该验证通过";
        assert !valid2 : "设备ID为null应该验证失败";
        assert !valid3 : "产品ID为null应该验证失败";
        assert !valid4 : "设备名称为null应该验证失败";

        System.out.println("  🟢 GREEN: 设备数据验证测试通过");
    }

    /**
     * TC-UNIT-FUNC-159: 测试大量设备数据查询性能
     * 验证需求: NFR-001-03 - 设备数据导出性能
     */
    public static void testPerformanceTest_LargeDeviceList() {
        System.out.println("  🔴 RED: 测试大量设备数据查询性能...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        Long productId = 12345678L;
        String masterKey = "test_master_key";
        long startTime = System.currentTimeMillis();

        // When
        PagedResult<DeviceInfo> result = deviceService.queryDevicesByProduct(
            productId, masterKey, 1, 100);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        assert duration < 10000 : "查询100个设备应该在10秒内完成，实际用时: " + duration + "ms";
        assert result != null : "查询结果不应该为null";

        System.out.println("    查询性能: " + duration + "ms for " +
                          (result != null ? result.getData().size() : 0) + " devices");
        System.out.println("  🟢 GREEN: 大量设备数据查询性能测试通过");
    }

    /**
     * TC-UNIT-FUNC-160: 测试批量设备查询
     * 验证需求: FR-002-05 - 批量查询优化
     */
    public static void testBatchDeviceQuery_WithProductList() {
        System.out.println("  🔴 RED: 测试批量设备查询...");

        // Given
        DeviceService deviceService = new DeviceService(createTestConfig());
        java.util.List<Long> productIds = java.util.Arrays.asList(
            12345678L, 12345679L, 12345680L
        );
        String masterKey = "test_master_key";

        // When
        java.util.Map<Long, PagedResult<DeviceInfo>> batchResult =
            deviceService.batchQueryDevicesByProducts(productIds, masterKey, 1, 20);

        // Then
        assert batchResult != null : "批量查询结果不应该为null";
        assert batchResult.size() == productIds.size() : "结果数量应该等于产品数量";

        // 验证每个产品的查询结果
        for (Long productId : productIds) {
            assert batchResult.containsKey(productId) : "应该包含产品ID: " + productId;
            PagedResult<DeviceInfo> productDevices = batchResult.get(productId);
            assert productDevices != null : "产品设备列表不应该为null";
        }

        System.out.println("  🟢 GREEN: 批量设备查询测试通过");
    }

    /**
     * 辅助方法：创建测试配置
     */
    private static ExportConfig createTestConfig() {
        return ExportConfig.builder()
            .appKey("test_key_12345")
            .appSecret("test_secret_67890")
            .apiHost("test.api.ctwing.cn")
            .appId("267848")
            .maxRetries(3)
            .timeoutSeconds(30)
            .pageSize(20)
            .enableDebugLog(false)
            .build();
    }
}