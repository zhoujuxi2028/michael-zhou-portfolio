package com.aep.export.model;

/**
 * ExportConfig单元测试
 * TDD第1轮：配置管理模型测试
 * 对应需求: FR-004-01 - 环境变量配置读取
 * 对应需求: FR-004-02 - 配置参数验证
 * 测试用例: TC-UNIT-FUNC-061~070
 */
public class ExportConfigTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始ExportConfig TDD测试...");

            testCreateExportConfig_WithAllRequiredFields();
            testThrowException_WhenAppKeyIsNull();
            testThrowException_WhenAppSecretIsNull();
            testThrowException_WhenApiHostIsNull();
            testCreateExportConfig_WithOptionalFields();
            testCreateExportConfig_WithExportFormats();
            testCreateExportConfig_WithOutputPaths();
            testEqualsAndHashCode();
            testToString();
            testMaskSensitiveInfo_InToString();

            System.out.println("✅ 所有ExportConfig测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-061: ✅ shouldCreateExportConfig_WithAllRequiredFields");
            System.out.println("  TC-UNIT-FUNC-062: ✅ shouldThrowException_WhenAppKeyIsNull");
            System.out.println("  TC-UNIT-FUNC-063: ✅ shouldThrowException_WhenAppSecretIsNull");
            System.out.println("  TC-UNIT-FUNC-064: ✅ shouldThrowException_WhenApiHostIsNull");
            System.out.println("  TC-UNIT-FUNC-065: ✅ shouldCreateExportConfig_WithOptionalFields");
            System.out.println("  TC-UNIT-FUNC-066: ✅ shouldCreateExportConfig_WithExportFormats");
            System.out.println("  TC-UNIT-FUNC-067: ✅ shouldCreateExportConfig_WithOutputPaths");
            System.out.println("  TC-UNIT-FUNC-068: ✅ shouldSupportEqualsAndHashCode");
            System.out.println("  TC-UNIT-FUNC-069: ✅ shouldSupportToString");
            System.out.println("  TC-UNIT-FUNC-070: ✅ shouldMaskSensitiveInfo_InToString");

        } catch (Exception e) {
            System.err.println("❌ ExportConfig测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-061: 测试必需字段创建
     * 验证需求: FR-004-01 - 环境变量配置读取
     */
    public static void testCreateExportConfig_WithAllRequiredFields() {
        System.out.println("  🔴 RED: 测试配置必需字段创建...");

        // Given - 基于.env配置的核心字段
        String appKey = "CIj7aTFV1R9";
        String appSecret = "zGEm97sb5M";
        String apiHost = "10433748.api.ctwing.cn";
        String appId = "267848";

        // When
        ExportConfig config = ExportConfig.builder()
            .appKey(appKey)
            .appSecret(appSecret)
            .apiHost(apiHost)
            .appId(appId)
            .build();

        // Then
        assert config.getAppKey().equals(appKey) : "AppKey不匹配";
        assert config.getAppSecret().equals(appSecret) : "AppSecret不匹配";
        assert config.getApiHost().equals(apiHost) : "ApiHost不匹配";
        assert config.getAppId().equals(appId) : "AppId不匹配";

        System.out.println("  🟢 GREEN: 配置必需字段创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-062: 测试AppKey为空异常
     * 验证需求: FR-004-02 - 配置参数验证
     */
    public static void testThrowException_WhenAppKeyIsNull() {
        System.out.println("  🔴 RED: 测试AppKey为空异常...");

        try {
            ExportConfig.builder()
                .appSecret("test_secret")
                .apiHost("test.api.ctwing.cn")
                .appId("123456")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("appKey") : "异常消息应包含appKey";
        }

        System.out.println("  🟢 GREEN: AppKey为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-063: 测试AppSecret为空异常
     * 验证需求: FR-004-02 - 配置参数验证
     */
    public static void testThrowException_WhenAppSecretIsNull() {
        System.out.println("  🔴 RED: 测试AppSecret为空异常...");

        try {
            ExportConfig.builder()
                .appKey("test_key")
                .apiHost("test.api.ctwing.cn")
                .appId("123456")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("appSecret") : "异常消息应包含appSecret";
        }

        System.out.println("  🟢 GREEN: AppSecret为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-064: 测试ApiHost为空异常
     * 验证需求: FR-004-02 - 配置参数验证
     */
    public static void testThrowException_WhenApiHostIsNull() {
        System.out.println("  🔴 RED: 测试ApiHost为空异常...");

        try {
            ExportConfig.builder()
                .appKey("test_key")
                .appSecret("test_secret")
                .appId("123456")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("apiHost") : "异常消息应包含apiHost";
        }

        System.out.println("  🟢 GREEN: ApiHost为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-065: 测试可选字段创建
     * 验证需求: FR-004-01 - 完整配置管理
     */
    public static void testCreateExportConfig_WithOptionalFields() {
        System.out.println("  🔴 RED: 测试配置可选字段创建...");

        // Given
        ExportConfig config = ExportConfig.builder()
            .appKey("CIj7aTFV1R9")
            .appSecret("zGEm97sb5M")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .maxRetries(3)
            .timeoutSeconds(30)
            .pageSize(50)
            .enableDebugLog(true)
            .build();

        // Then
        assert config.getMaxRetries().equals(3) : "MaxRetries不匹配";
        assert config.getTimeoutSeconds().equals(30) : "TimeoutSeconds不匹配";
        assert config.getPageSize().equals(50) : "PageSize不匹配";
        assert config.getEnableDebugLog().equals(true) : "EnableDebugLog不匹配";

        System.out.println("  🟢 GREEN: 配置可选字段创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-066: 测试导出格式配置
     * 验证需求: FR-003-01, FR-003-02 - 导出格式支持
     */
    public static void testCreateExportConfig_WithExportFormats() {
        System.out.println("  🔴 RED: 测试导出格式配置...");

        // 测试JSON格式配置
        ExportConfig jsonConfig = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .exportFormat("JSON")
            .jsonIndented(true)
            .build();

        // 测试CSV格式配置
        ExportConfig csvConfig = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .exportFormat("CSV")
            .csvSeparator(",")
            .csvWithHeader(true)
            .build();

        // Then
        assert jsonConfig.getExportFormat().equals("JSON") : "JSON格式不匹配";
        assert jsonConfig.getJsonIndented().equals(true) : "JSON缩进配置不匹配";
        assert csvConfig.getExportFormat().equals("CSV") : "CSV格式不匹配";
        assert csvConfig.getCsvSeparator().equals(",") : "CSV分隔符不匹配";
        assert csvConfig.getCsvWithHeader().equals(true) : "CSV标题配置不匹配";

        System.out.println("  🟢 GREEN: 导出格式配置测试通过");
    }

    /**
     * TC-UNIT-FUNC-067: 测试输出路径配置
     * 验证需求: FR-003-04 - 导出文件管理
     */
    public static void testCreateExportConfig_WithOutputPaths() {
        System.out.println("  🔴 RED: 测试输出路径配置...");

        ExportConfig config = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .outputDirectory("./output")
            .productFileName("products.json")
            .deviceFileName("devices.csv")
            .createBackup(true)
            .build();

        // Then
        assert config.getOutputDirectory().equals("./output") : "输出目录不匹配";
        assert config.getProductFileName().equals("products.json") : "产品文件名不匹配";
        assert config.getDeviceFileName().equals("devices.csv") : "设备文件名不匹配";
        assert config.getCreateBackup().equals(true) : "备份配置不匹配";

        System.out.println("  🟢 GREEN: 输出路径配置测试通过");
    }

    /**
     * TC-UNIT-FUNC-068: 测试equals和hashCode
     * 验证设计: DM-015-04 - equals/hashCode实现
     */
    public static void testEqualsAndHashCode() {
        System.out.println("  🔴 RED: 测试配置equals和hashCode...");

        ExportConfig config1 = ExportConfig.builder()
            .appKey("CIj7aTFV1R9")
            .appSecret("zGEm97sb5M")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .build();

        ExportConfig config2 = ExportConfig.builder()
            .appKey("CIj7aTFV1R9")
            .appSecret("zGEm97sb5M")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .build();

        ExportConfig config3 = ExportConfig.builder()
            .appKey("different_key")
            .appSecret("different_secret")
            .apiHost("different.api.ctwing.cn")
            .appId("999999")
            .build();

        // When & Then
        assert config1.equals(config2) : "相同数据的配置对象应该相等";
        assert config1.hashCode() == config2.hashCode() : "相等配置对象的hashCode应该相同";
        assert !config1.equals(config3) : "不同数据的配置对象不应该相等";

        System.out.println("  🟢 GREEN: 配置equals和hashCode测试通过");
    }

    /**
     * TC-UNIT-FUNC-069: 测试toString方法
     * 验证设计: DM-015-05 - toString安全实现
     */
    public static void testToString() {
        System.out.println("  🔴 RED: 测试配置toString方法...");

        ExportConfig config = ExportConfig.builder()
            .appKey("CIj7aTFV1R9")
            .appSecret("zGEm97sb5M")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .exportFormat("JSON")
            .build();

        String toString = config.toString();

        // Then
        assert toString != null : "toString不应该为null";
        assert toString.contains("267848") : "应该包含appId";
        assert toString.contains("10433748.api.ctwing.cn") : "应该包含apiHost";
        assert toString.contains("JSON") : "应该包含exportFormat";
        // 敏感信息应该被脱敏
        assert !toString.contains("CIj7aTFV1R9") : "不应该包含完整的appKey";
        assert !toString.contains("zGEm97sb5M") : "不应该包含完整的appSecret";

        System.out.println("  🟢 GREEN: 配置toString方法测试通过");
    }

    /**
     * TC-UNIT-FUNC-070: 测试敏感信息脱敏
     * 验证需求: NFR-003-02 - 敏感信息脱敏处理
     */
    public static void testMaskSensitiveInfo_InToString() {
        System.out.println("  🔴 RED: 测试配置敏感信息脱敏...");

        ExportConfig config = ExportConfig.builder()
            .appKey("CIj7aTFV1R9")
            .appSecret("zGEm97sb5M")
            .apiHost("10433748.api.ctwing.cn")
            .appId("267848")
            .build();

        String toString = config.toString();

        // AppKey和AppSecret应该进行适当脱敏显示
        assert toString.contains("CIj7****1R9") : "应该显示脱敏后的appKey: " + toString;
        assert toString.contains("zGEm****b5M") : "应该显示脱敏后的appSecret: " + toString;

        System.out.println("    配置信息toString: " + toString);
        System.out.println("  🟢 GREEN: 配置敏感信息脱敏测试通过");
    }
}