package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigService配置读取优先级测试
 * TDD测试用例 - 验证环境变量应该优先于系统属性
 *
 * 对应缺陷: CONFIG-001
 * TDD阶段: RED - 编写失败测试定义期望行为
 */
public class ConfigServicePriorityTest {

    private ConfigService configService;
    private String originalAppKeyProperty;
    private String originalAppSecretProperty;
    private String originalApiHostProperty;

    @BeforeEach
    void setUp() {
        configService = new ConfigService();
        // 备份原始系统属性
        originalAppKeyProperty = System.getProperty("AEP_APP_KEY");
        originalAppSecretProperty = System.getProperty("AEP_APP_SECRET");
        originalApiHostProperty = System.getProperty("AEP_API_HOST");
    }

    @AfterEach
    void tearDown() {
        // 恢复原始系统属性
        if (originalAppKeyProperty != null) {
            System.setProperty("AEP_APP_KEY", originalAppKeyProperty);
        } else {
            System.clearProperty("AEP_APP_KEY");
        }

        if (originalAppSecretProperty != null) {
            System.setProperty("AEP_APP_SECRET", originalAppSecretProperty);
        } else {
            System.clearProperty("AEP_APP_SECRET");
        }

        if (originalApiHostProperty != null) {
            System.setProperty("AEP_API_HOST", originalApiHostProperty);
        } else {
            System.clearProperty("AEP_API_HOST");
        }
    }

    /**
     * TDD测试用例001: 环境变量应该优先于系统属性 - 通过loadConfig验证
     * 当前状态: 🔴 RED - 此测试应该失败，因为当前实现可能受系统属性污染
     *
     * 这是CONFIG-001缺陷的核心测试：验证测试代码设置的System.setProperty
     * 不应该影响ConfigService.loadConfig()的结果
     */
    @Test
    void shouldPrioritizeEnvironmentVariableOverSystemProperty() {
        // Given: 设置系统属性为测试值 (模拟AepDataExporterTest的污染)
        System.setProperty("AEP_APP_KEY", "mock_test_key");
        System.setProperty("AEP_APP_SECRET", "mock_test_secret");
        System.setProperty("AEP_API_HOST", "mock.api.test");

        // When: 加载配置 (这是ConfigService的主要接口)
        ExportConfig config = configService.loadConfig();

        // Then: 应该返回环境变量值，而不是系统属性值
        assertNotEquals("mock_test_key", config.getAppKey(),
            "配置服务不应该返回系统属性值，应该优先使用环境变量");

        assertNotEquals("mock_test_secret", config.getAppSecret(),
            "配置服务不应该返回系统属性值，应该优先使用环境变量");

        assertNotEquals("mock.api.test", config.getApiHost(),
            "配置服务不应该返回系统属性值，应该优先使用环境变量");

        // 验证返回的是真实环境变量值 (来自.env文件)
        assertEquals("CIj7aTFV1R9", config.getAppKey(),
            "应该返回.env文件中的真实AEP_APP_KEY值");

        assertEquals("zGEm97sb5M", config.getAppSecret(),
            "应该返回.env文件中的真实AEP_APP_SECRET值");

        assertEquals("10433748.api.ctwing.cn", config.getApiHost(),
            "应该返回.env文件中的真实AEP_API_HOST值");
    }

    /**
     * TDD测试用例002: 当环境变量不存在时，应该使用系统属性作为fallback
     * 当前状态: 🔴 RED - 需要添加fallback机制
     */
    @Test
    void shouldUseSystemPropertyAsFallbackWhenEnvMissing() {
        // Given: 清除环境变量，只设置系统属性
        // 注意：这个测试验证fallback机制，而不是污染问题
        System.setProperty("AEP_TIMEOUT_SECONDS", "60");

        // When: 加载配置
        ExportConfig config = configService.loadConfig();

        // Then: 应该使用系统属性作为fallback
        assertEquals(60, config.getTimeoutSeconds(),
            "当环境变量不存在时，应该使用系统属性作为fallback");
    }

    /**
     * TDD测试用例003: 配置污染检测机制
     * 当前状态: 🔴 RED - 当前ConfigService没有污染检测功能
     */
    @Test
    void shouldDetectConfigurationPollution() {
        // Given: 设置多个测试相关的系统属性 (模拟测试污染)
        System.setProperty("AEP_APP_KEY", "mock_test_key");
        System.setProperty("AEP_APP_SECRET", "mock_test_secret");
        System.setProperty("AEP_API_HOST", "mock.api.test");

        // When: 检测配置污染 (需要新增此方法)
        // 这个方法目前不存在，期望TDD GREEN阶段添加

        // Then: 通过间接方式验证污染 - loadConfig不应该返回mock值
        ExportConfig config = configService.loadConfig();

        boolean isPolluted = config.getAppKey().contains("mock") ||
                           config.getAppKey().contains("test") ||
                           config.getAppSecret().contains("mock") ||
                           config.getApiHost().contains("mock");

        assertFalse(isPolluted,
            "配置不应该被测试相关的系统属性污染");
    }

    /**
     * TDD测试用例004: 配置清理功能
     * 当前状态: 🔴 RED - 当前ConfigService没有清理功能
     */
    @Test
    void shouldClearTestPropertiesWhenRequested() {
        // Given: 设置测试系统属性
        System.setProperty("AEP_APP_KEY", "mock_test_key");
        System.setProperty("AEP_APP_SECRET", "mock_test_secret");

        // 验证属性已设置
        assertEquals("mock_test_key", System.getProperty("AEP_APP_KEY"));

        // When: 清理测试属性 (需要新增此方法)
        // ConfigService.clearTestProperties(); // 期望在GREEN阶段添加

        // Then: 测试属性应该被清理 (暂时跳过，因为方法不存在)
        // assertNull(System.getProperty("AEP_APP_KEY"),
        //     "测试属性应该被清理");

        // 现在验证loadConfig不受污染影响
        ExportConfig config = configService.loadConfig();
        assertNotEquals("mock_test_key", config.getAppKey(),
            "即使系统属性存在mock值，loadConfig也不应该使用它");
    }
}