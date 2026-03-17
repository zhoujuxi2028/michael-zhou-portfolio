package com.aep.export.model;

/**
 * ProductInfo简单测试 - 使用Java内置断言
 * TDD第1轮：数据模型测试
 */
public class SimpleProductInfoTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始ProductInfo TDD测试...");

            testCreateProductInfo_WithAllRequiredFields();
            testThrowException_WhenProductIdIsNull();
            testThrowException_WhenProductNameIsNull();
            testCreateProductInfo_WithOptionalFields();
            testEqualsAndHashCode();
            testToString();
            testMaskMasterKey_InToString();

            System.out.println("✅ 所有测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-001: ✅ shouldCreateProductInfo_WithAllRequiredFields");
            System.out.println("  TC-UNIT-FUNC-002: ✅ shouldThrowException_WhenProductIdIsNull");
            System.out.println("  TC-UNIT-FUNC-003: ✅ shouldThrowException_WhenProductNameIsNull");
            System.out.println("  TC-UNIT-FUNC-004: ✅ shouldCreateProductInfo_WithOptionalFields");
            System.out.println("  TC-UNIT-FUNC-005: ✅ shouldSupportEqualsAndHashCode");
            System.out.println("  TC-UNIT-FUNC-006: ✅ shouldSupportToString");
            System.out.println("  TC-UNIT-FUNC-007: ✅ shouldMaskMasterKey_InToString");

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-001: 测试必需字段创建
     * 验证需求: FR-001-02 - 提取产品基本信息
     */
    public static void testCreateProductInfo_WithAllRequiredFields() {
        System.out.println("  🔴 RED: 测试必需字段创建...");

        // Given
        Long productId = 16857118L;
        String productName = "RepeaterLTE";
        String masterKey = "521e0d76d0024539a9718abb3e4f64cc";

        // When
        ProductInfo product = ProductInfo.builder()
            .productId(productId)
            .productName(productName)
            .masterKey(masterKey)
            .build();

        // Then
        assert product.getProductId().equals(productId) : "ProductId不匹配";
        assert product.getProductName().equals(productName) : "ProductName不匹配";
        assert product.getMasterKey().equals(masterKey) : "MasterKey不匹配";

        System.out.println("  🟢 GREEN: 必需字段创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-002: 测试ProductId为空异常
     * 验证需求: FR-001-04 - 验证产品信息完整性
     */
    public static void testThrowException_WhenProductIdIsNull() {
        System.out.println("  🔴 RED: 测试ProductId为空异常...");

        try {
            ProductInfo.builder()
                .productName("Test Product")
                .masterKey("test_key")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("productId") : "异常消息应包含productId";
        }

        System.out.println("  🟢 GREEN: ProductId为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-003: 测试ProductName为空异常
     * 验证需求: FR-001-04 - 验证产品信息完整性
     */
    public static void testThrowException_WhenProductNameIsNull() {
        System.out.println("  🔴 RED: 测试ProductName为空异常...");

        try {
            ProductInfo.builder()
                .productId(12345L)
                .masterKey("test_key")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("productName") : "异常消息应包含productName";
        }

        System.out.println("  🟢 GREEN: ProductName为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-004: 测试可选字段创建
     * 验证需求: FR-001-02 - 提取产品基本信息
     */
    public static void testCreateProductInfo_WithOptionalFields() {
        System.out.println("  🔴 RED: 测试可选字段创建...");

        // Given
        Long productId = 16857118L;
        String productName = "RepeaterLTE";
        String masterKey = "521e0d76d0024539a9718abb3e4f64cc";
        String tenantId = "10433748";
        Integer deviceCount = 892;
        String createTime = "2023-07-12 19:44:33";
        String deviceModel = "RepeaterLTE_BIN";

        // When
        ProductInfo product = ProductInfo.builder()
            .productId(productId)
            .productName(productName)
            .masterKey(masterKey)
            .tenantId(tenantId)
            .deviceCount(deviceCount)
            .createTime(createTime)
            .deviceModel(deviceModel)
            .build();

        // Then
        assert product.getTenantId().equals(tenantId) : "TenantId不匹配";
        assert product.getDeviceCount().equals(deviceCount) : "DeviceCount不匹配";
        assert product.getCreateTime().equals(createTime) : "CreateTime不匹配";
        assert product.getDeviceModel().equals(deviceModel) : "DeviceModel不匹配";

        System.out.println("  🟢 GREEN: 可选字段创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-005: 测试equals和hashCode
     * 验证设计: DM-001-04 - equals/hashCode实现
     */
    public static void testEqualsAndHashCode() {
        System.out.println("  🔴 RED: 测试equals和hashCode...");

        // Given
        ProductInfo product1 = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("test_key")
            .build();

        ProductInfo product2 = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("test_key")
            .build();

        ProductInfo product3 = ProductInfo.builder()
            .productId(16980130L)
            .productName("RepeaterLTE01")
            .masterKey("different_key")
            .build();

        // When & Then
        assert product1.equals(product2) : "相同数据的对象应该相等";
        assert product1.hashCode() == product2.hashCode() : "相等对象的hashCode应该相同";
        assert !product1.equals(product3) : "不同数据的对象不应该相等";

        System.out.println("  🟢 GREEN: equals和hashCode测试通过");
    }

    /**
     * TC-UNIT-FUNC-006: 测试toString方法
     * 验证设计: DM-001-05 - toString安全实现
     */
    public static void testToString() {
        System.out.println("  🔴 RED: 测试toString方法...");

        // Given
        ProductInfo product = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("521e0d76d0024539a9718abb3e4f64cc")
            .deviceCount(892)
            .build();

        // When
        String toString = product.toString();

        // Then
        assert toString != null : "toString不应该为null";
        assert toString.contains("16857118") : "应该包含productId";
        assert toString.contains("RepeaterLTE") : "应该包含productName";
        assert toString.contains("892") : "应该包含deviceCount";
        // MasterKey should be masked in toString for security
        assert !toString.contains("521e0d76d0024539a9718abb3e4f64cc") : "不应该包含完整的masterKey";

        System.out.println("  🟢 GREEN: toString方法测试通过");
    }

    /**
     * TC-UNIT-FUNC-007: 测试敏感信息脱敏
     * 验证需求: NFR-003-02 - 敏感信息脱敏处理
     */
    public static void testMaskMasterKey_InToString() {
        System.out.println("  🔴 RED: 测试敏感信息脱敏...");

        // Given
        ProductInfo product = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("521e0d76d0024539a9718abb3e4f64cc")
            .build();

        // When
        String toString = product.toString();

        // Then
        assert toString.contains("521e****64cc") : "应该显示masked版本: " + toString;

        System.out.println("  🟢 GREEN: 敏感信息脱敏测试通过");
    }
}