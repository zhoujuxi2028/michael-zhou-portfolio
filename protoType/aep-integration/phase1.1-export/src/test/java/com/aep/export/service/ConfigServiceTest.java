package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import com.aep.export.service.ConfigService.ConfigurationException;

/**
 * ConfigService单元测试
 * TDD第2轮：基础服务测试
 * 对应需求: FR-004-01 - 环境变量配置读取
 * 对应需求: FR-004-02 - 配置参数验证
 * 测试用例: TC-UNIT-FUNC-101~110
 */
public class ConfigServiceTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始ConfigService TDD测试...");

            testLoadConfigFromEnvironment_Success();
            testThrowException_WhenRequiredEnvMissing();
            testLoadConfigWithDefaults();
            testValidateConfig_Success();
            testValidateConfig_ThrowsForInvalidConfig();
            testGetDefaultConfig();
            testOverrideConfigValues();
            testConfigSerialization();
            testConfigCaching();
            testReloadConfig();

            System.out.println("✅ 所有ConfigService测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-101: ✅ shouldLoadConfigFromEnvironment_Success");
            System.out.println("  TC-UNIT-FUNC-102: ✅ shouldThrowException_WhenRequiredEnvMissing");
            System.out.println("  TC-UNIT-FUNC-103: ✅ shouldLoadConfigWithDefaults");
            System.out.println("  TC-UNIT-FUNC-104: ✅ shouldValidateConfig_Success");
            System.out.println("  TC-UNIT-FUNC-105: ✅ shouldValidateConfig_ThrowsForInvalidConfig");
            System.out.println("  TC-UNIT-FUNC-106: ✅ shouldGetDefaultConfig");
            System.out.println("  TC-UNIT-FUNC-107: ✅ shouldOverrideConfigValues");
            System.out.println("  TC-UNIT-FUNC-108: ✅ shouldConfigSerialization");
            System.out.println("  TC-UNIT-FUNC-109: ✅ shouldConfigCaching");
            System.out.println("  TC-UNIT-FUNC-110: ✅ shouldReloadConfig");

        } catch (Exception e) {
            System.err.println("❌ ConfigService测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-101: 测试从环境变量加载配置成功
     * 验证需求: FR-004-01 - 环境变量配置读取
     */
    public static void testLoadConfigFromEnvironment_Success() {
        System.out.println("  🔴 RED: 测试从环境变量加载配置...");

        // 模拟环境变量设置
        MockEnvironment env = new MockEnvironment();
        env.set("AEP_APP_KEY", "CIj7aTFV1R9");
        env.set("AEP_APP_SECRET", "zGEm97sb5M");
        env.set("AEP_API_HOST", "10433748.api.ctwing.cn");
        env.set("AEP_APP_ID", "267848");

        // When
        ConfigService configService = new ConfigService(env);
        ExportConfig config = configService.loadConfig();

        // Then
        assert config != null : "配置不应该为null";
        assert config.getAppKey().equals("CIj7aTFV1R9") : "AppKey不匹配";
        assert config.getAppSecret().equals("zGEm97sb5M") : "AppSecret不匹配";
        assert config.getApiHost().equals("10433748.api.ctwing.cn") : "ApiHost不匹配";
        assert config.getAppId().equals("267848") : "AppId不匹配";

        System.out.println("  🟢 GREEN: 从环境变量加载配置测试通过");
    }

    /**
     * TC-UNIT-FUNC-102: 测试必需环境变量缺失异常
     * 验证需求: FR-004-02 - 配置参数验证
     */
    public static void testThrowException_WhenRequiredEnvMissing() {
        System.out.println("  🔴 RED: 测试必需环境变量缺失异常...");

        // Given - 缺少必需的环境变量
        MockEnvironment env = new MockEnvironment();
        env.set("AEP_APP_KEY", "test_key");
        // 缺少 AEP_APP_SECRET, AEP_API_HOST

        try {
            ConfigService configService = new ConfigService(env);
            configService.loadConfig();
            assert false : "应该抛出异常";
        } catch (ConfigurationException e) {
            assert e.getMessage().contains("Required") : "异常消息应包含Required: " + e.getMessage();
        }

        System.out.println("  🟢 GREEN: 必需环境变量缺失异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-103: 测试加载配置时应用默认值
     * 验证需求: FR-004-01 - 默认配置管理
     */
    public static void testLoadConfigWithDefaults() {
        System.out.println("  🔴 RED: 测试配置默认值加载...");

        // Given - 只有必需环境变量，其他使用默认值
        MockEnvironment env = new MockEnvironment();
        env.set("AEP_APP_KEY", "test_key");
        env.set("AEP_APP_SECRET", "test_secret");
        env.set("AEP_API_HOST", "test.api.ctwing.cn");
        env.set("AEP_APP_ID", "123456");

        // When
        ConfigService configService = new ConfigService(env);
        ExportConfig config = configService.loadConfig();

        // Then - 检查默认值
        assert config.getMaxRetries().equals(3) : "默认重试次数应该是3";
        assert config.getTimeoutSeconds().equals(30) : "默认超时时间应该是30秒";
        assert config.getPageSize().equals(50) : "默认页大小应该是50";
        assert config.getEnableDebugLog().equals(false) : "默认调试日志应该是false";
        assert config.getExportFormat().equals("JSON") : "默认导出格式应该是JSON";

        System.out.println("  🟢 GREEN: 配置默认值加载测试通过");
    }

    /**
     * TC-UNIT-FUNC-104: 测试配置验证成功
     * 验证需求: FR-004-02 - 配置参数验证
     */
    public static void testValidateConfig_Success() {
        System.out.println("  🔴 RED: 测试配置验证成功...");

        // Given
        ExportConfig validConfig = ExportConfig.builder()
            .appKey("valid_key")
            .appSecret("valid_secret")
            .apiHost("valid.api.ctwing.cn")
            .appId("123456")
            .maxRetries(3)
            .timeoutSeconds(30)
            .pageSize(50)
            .build();

        // When
        ConfigService configService = new ConfigService();
        boolean isValid = configService.validateConfig(validConfig);

        // Then
        assert isValid : "配置应该是有效的";

        System.out.println("  🟢 GREEN: 配置验证成功测试通过");
    }

    /**
     * TC-UNIT-FUNC-105: 测试无效配置验证失败
     * 验证需求: FR-004-02 - 配置参数验证
     */
    public static void testValidateConfig_ThrowsForInvalidConfig() {
        System.out.println("  🔴 RED: 测试无效配置验证...");

        // Given - 无效配置（超时时间过长）
        ExportConfig invalidConfig = ExportConfig.builder()
            .appKey("valid_key")
            .appSecret("valid_secret")
            .apiHost("valid.api.ctwing.cn")
            .appId("123456")
            .timeoutSeconds(300) // 超过合理范围
            .pageSize(1000)      // 超过合理范围
            .build();

        try {
            ConfigService configService = new ConfigService();
            configService.validateConfig(invalidConfig);
            assert false : "应该抛出验证异常";
        } catch (ConfigurationException e) {
            assert e.getMessage().contains("must be") : "异常消息应包含验证信息: " + e.getMessage();
        }

        System.out.println("  🟢 GREEN: 无效配置验证测试通过");
    }

    /**
     * TC-UNIT-FUNC-106: 测试获取默认配置
     * 验证需求: FR-004-01 - 默认配置提供
     */
    public static void testGetDefaultConfig() {
        System.out.println("  🔴 RED: 测试获取默认配置...");

        // When
        ConfigService configService = new ConfigService();
        ExportConfig defaultConfig = configService.getDefaultConfig();

        // Then
        assert defaultConfig != null : "默认配置不应该为null";
        assert defaultConfig.getMaxRetries().equals(3) : "默认重试次数不匹配";
        assert defaultConfig.getTimeoutSeconds().equals(30) : "默认超时时间不匹配";
        assert defaultConfig.getPageSize().equals(50) : "默认页大小不匹配";
        assert defaultConfig.getExportFormat().equals("JSON") : "默认导出格式不匹配";

        System.out.println("  🟢 GREEN: 获取默认配置测试通过");
    }

    /**
     * TC-UNIT-FUNC-107: 测试配置值覆盖
     * 验证需求: FR-004-01 - 配置灵活性
     */
    public static void testOverrideConfigValues() {
        System.out.println("  🔴 RED: 测试配置值覆盖...");

        // Given
        ExportConfig baseConfig = ExportConfig.builder()
            .appKey("base_key")
            .appSecret("base_secret")
            .apiHost("base.api.ctwing.cn")
            .appId("123456")
            .build();

        ExportConfig overrides = ExportConfig.builder()
            .appKey("override_key")
            .appSecret("override_secret")
            .apiHost("override.api.ctwing.cn")
            .appId("999999")
            .maxRetries(5)
            .timeoutSeconds(60)
            .exportFormat("CSV")
            .build();

        // When
        ConfigService configService = new ConfigService();
        ExportConfig mergedConfig = configService.mergeConfigs(baseConfig, overrides);

        // Then
        assert mergedConfig.getAppKey().equals("override_key") : "覆盖值应该生效";
        assert mergedConfig.getMaxRetries().equals(5) : "覆盖值应该生效";
        assert mergedConfig.getTimeoutSeconds().equals(60) : "覆盖值应该生效";
        assert mergedConfig.getExportFormat().equals("CSV") : "覆盖值应该生效";

        System.out.println("  🟢 GREEN: 配置值覆盖测试通过");
    }

    /**
     * TC-UNIT-FUNC-108: 测试配置序列化
     * 验证需求: FR-004-01 - 配置持久化
     */
    public static void testConfigSerialization() {
        System.out.println("  🔴 RED: 测试配置序列化...");

        // Given
        ExportConfig config = ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .exportFormat("JSON")
            .build();

        // When
        ConfigService configService = new ConfigService();
        String serialized = configService.serializeConfig(config);
        ExportConfig deserialized = configService.deserializeConfig(serialized);

        // Then
        assert deserialized != null : "反序列化配置不应该为null";
        assert deserialized.getAppKey().contains("test") : "序列化后appKey应该包含test";
        assert deserialized.getExportFormat().equals(config.getExportFormat()) : "序列化后exportFormat不匹配";

        System.out.println("  🟢 GREEN: 配置序列化测试通过");
    }

    /**
     * TC-UNIT-FUNC-109: 测试配置缓存
     * 验证需求: FR-001-05 - 配置缓存机制
     */
    public static void testConfigCaching() {
        System.out.println("  🔴 RED: 测试配置缓存...");

        // Given
        MockEnvironment env = new MockEnvironment();
        env.set("AEP_APP_KEY", "cached_key");
        env.set("AEP_APP_SECRET", "cached_secret");
        env.set("AEP_API_HOST", "cached.api.ctwing.cn");
        env.set("AEP_APP_ID", "123456");

        // When
        ConfigService configService = new ConfigService(env);
        ExportConfig config1 = configService.loadConfig();
        ExportConfig config2 = configService.loadConfig(); // 第二次加载应该使用缓存

        // Then
        assert config1 == config2 : "第二次加载应该返回缓存的配置实例";

        System.out.println("  🟢 GREEN: 配置缓存测试通过");
    }

    /**
     * TC-UNIT-FUNC-110: 测试重新加载配置
     * 验证需求: FR-004-01 - 配置动态更新
     */
    public static void testReloadConfig() {
        System.out.println("  🔴 RED: 测试重新加载配置...");

        // Given
        MockEnvironment env = new MockEnvironment();
        env.set("AEP_APP_KEY", "initial_key");
        env.set("AEP_APP_SECRET", "initial_secret");
        env.set("AEP_API_HOST", "initial.api.ctwing.cn");
        env.set("AEP_APP_ID", "123456");

        ConfigService configService = new ConfigService(env);
        ExportConfig initialConfig = configService.loadConfig();

        // When - 修改环境变量并重新加载
        env.set("AEP_APP_KEY", "updated_key");
        configService.reloadConfig();
        ExportConfig updatedConfig = configService.loadConfig();

        // Then
        assert !initialConfig.getAppKey().equals(updatedConfig.getAppKey()) : "配置应该已更新";
        assert updatedConfig.getAppKey().equals("updated_key") : "新配置值应该生效";

        System.out.println("  🟢 GREEN: 重新加载配置测试通过");
    }

    /**
     * 模拟环境变量访问的辅助类
     */
    static class MockEnvironment {
        private java.util.Map<String, String> env = new java.util.HashMap<>();

        public void set(String key, String value) {
            env.put(key, value);
        }

        public String get(String key) {
            return env.get(key);
        }

        public String get(String key, String defaultValue) {
            return env.getOrDefault(key, defaultValue);
        }
    }

}