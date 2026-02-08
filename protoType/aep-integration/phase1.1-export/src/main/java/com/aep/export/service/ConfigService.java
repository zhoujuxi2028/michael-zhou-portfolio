package com.aep.export.service;

import com.aep.export.model.ExportConfig;

/**
 * 配置管理服务
 * 对应需求: FR-004-01 - 环境变量配置读取
 * 对应需求: FR-004-02 - 配置参数验证
 * 设计模块: DM-010 - ConfigService
 * 负责从环境变量加载、验证和管理导出工具的配置信息
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class ConfigService {

    // 环境变量访问接口
    private final EnvironmentAccessor environmentAccessor;

    // 配置缓存
    private ExportConfig cachedConfig;
    private boolean configLoaded = false;

    /**
     * 默认构造函数，使用系统环境变量
     */
    public ConfigService() {
        this.environmentAccessor = new SystemEnvironmentAccessor();
    }

    /**
     * 测试构造函数，使用模拟环境变量
     */
    public ConfigService(Object mockEnvironment) {
        this.environmentAccessor = new MockEnvironmentAccessor(mockEnvironment);
    }

    /**
     * 从环境变量加载配置
     * 实现: DM-010-01 - 环境变量读取
     */
    public ExportConfig loadConfig() {
        if (configLoaded && cachedConfig != null) {
            return cachedConfig;
        }

        String appKey = getRequiredEnv("AEP_APP_KEY");
        String appSecret = getRequiredEnv("AEP_APP_SECRET");
        String apiHost = getRequiredEnv("AEP_API_HOST");
        String appId = getOptionalEnv("AEP_APP_ID", "267848");

        // 可选配置项使用默认值
        Integer maxRetries = getIntEnv("AEP_MAX_RETRIES", 3);
        Integer timeoutSeconds = getIntEnv("AEP_TIMEOUT_SECONDS", 30);
        Integer pageSize = getIntEnv("AEP_PAGE_SIZE", 50);
        Boolean enableDebugLog = getBooleanEnv("AEP_DEBUG_LOG", false);
        String exportFormat = getOptionalEnv("AEP_EXPORT_FORMAT", "JSON");

        // JSON格式配置
        Boolean jsonIndented = getBooleanEnv("AEP_JSON_INDENTED", true);

        // CSV格式配置
        String csvSeparator = getOptionalEnv("AEP_CSV_SEPARATOR", ",");
        Boolean csvWithHeader = getBooleanEnv("AEP_CSV_WITH_HEADER", true);

        // 文件输出配置
        String outputDirectory = getOptionalEnv("AEP_OUTPUT_DIR", "./output");
        String productFileName = getOptionalEnv("AEP_PRODUCT_FILE", "products.json");
        String deviceFileName = getOptionalEnv("AEP_DEVICE_FILE", "devices.csv");
        Boolean createBackup = getBooleanEnv("AEP_CREATE_BACKUP", false);

        cachedConfig = ExportConfig.builder()
            .appKey(appKey)
            .appSecret(appSecret)
            .apiHost(apiHost)
            .appId(appId)
            .maxRetries(maxRetries)
            .timeoutSeconds(timeoutSeconds)
            .pageSize(pageSize)
            .enableDebugLog(enableDebugLog)
            .exportFormat(exportFormat)
            .jsonIndented(jsonIndented)
            .csvSeparator(csvSeparator)
            .csvWithHeader(csvWithHeader)
            .outputDirectory(outputDirectory)
            .productFileName(productFileName)
            .deviceFileName(deviceFileName)
            .createBackup(createBackup)
            .build();

        configLoaded = true;
        return cachedConfig;
    }

    /**
     * 验证配置有效性
     * 实现: DM-010-02 - 配置验证逻辑
     */
    public boolean validateConfig(ExportConfig config) {
        try {
            // 验证必需字段
            if (config.getAppKey() == null || config.getAppKey().trim().isEmpty()) {
                throw new ConfigurationException("AppKey is required");
            }
            if (config.getAppSecret() == null || config.getAppSecret().trim().isEmpty()) {
                throw new ConfigurationException("AppSecret is required");
            }
            if (config.getApiHost() == null || config.getApiHost().trim().isEmpty()) {
                throw new ConfigurationException("ApiHost is required");
            }

            // 验证数值范围
            if (config.getMaxRetries() != null && (config.getMaxRetries() < 0 || config.getMaxRetries() > 10)) {
                throw new ConfigurationException("MaxRetries must be between 0 and 10");
            }
            if (config.getTimeoutSeconds() != null && (config.getTimeoutSeconds() < 5 || config.getTimeoutSeconds() > 120)) {
                throw new ConfigurationException("TimeoutSeconds must be between 5 and 120");
            }
            if (config.getPageSize() != null && (config.getPageSize() < 1 || config.getPageSize() > 500)) {
                throw new ConfigurationException("PageSize must be between 1 and 500");
            }

            // 验证格式
            if (config.getExportFormat() != null) {
                String format = config.getExportFormat().toUpperCase();
                if (!format.equals("JSON") && !format.equals("CSV")) {
                    throw new ConfigurationException("ExportFormat must be JSON or CSV");
                }
            }

            // 验证主机格式
            if (config.getApiHost() != null && !config.getApiHost().contains(".api.ctwing.cn")) {
                throw new ConfigurationException("ApiHost must be a valid AEP endpoint");
            }

            return true;
        } catch (ConfigurationException e) {
            throw e; // 重新抛出验证异常
        }
    }

    /**
     * 获取默认配置
     * 实现: DM-010-03 - 默认配置提供
     */
    public ExportConfig getDefaultConfig() {
        return ExportConfig.builder()
            .appKey("") // 必需在环境变量中设置
            .appSecret("") // 必需在环境变量中设置
            .apiHost("") // 必需在环境变量中设置
            .appId("267848")
            .maxRetries(3)
            .timeoutSeconds(30)
            .pageSize(50)
            .enableDebugLog(false)
            .exportFormat("JSON")
            .jsonIndented(true)
            .csvSeparator(",")
            .csvWithHeader(true)
            .outputDirectory("./output")
            .productFileName("products.json")
            .deviceFileName("devices.csv")
            .createBackup(false)
            .build();
    }

    /**
     * 合并配置
     * 实现: DM-010-04 - 配置合并逻辑
     */
    public ExportConfig mergeConfigs(ExportConfig baseConfig, ExportConfig overrides) {
        return ExportConfig.builder()
            .appKey(overrides.getAppKey() != null ? overrides.getAppKey() : baseConfig.getAppKey())
            .appSecret(overrides.getAppSecret() != null ? overrides.getAppSecret() : baseConfig.getAppSecret())
            .apiHost(overrides.getApiHost() != null ? overrides.getApiHost() : baseConfig.getApiHost())
            .appId(overrides.getAppId() != null ? overrides.getAppId() : baseConfig.getAppId())
            .maxRetries(overrides.getMaxRetries() != null ? overrides.getMaxRetries() : baseConfig.getMaxRetries())
            .timeoutSeconds(overrides.getTimeoutSeconds() != null ? overrides.getTimeoutSeconds() : baseConfig.getTimeoutSeconds())
            .pageSize(overrides.getPageSize() != null ? overrides.getPageSize() : baseConfig.getPageSize())
            .enableDebugLog(overrides.getEnableDebugLog() != null ? overrides.getEnableDebugLog() : baseConfig.getEnableDebugLog())
            .exportFormat(overrides.getExportFormat() != null ? overrides.getExportFormat() : baseConfig.getExportFormat())
            .jsonIndented(overrides.getJsonIndented() != null ? overrides.getJsonIndented() : baseConfig.getJsonIndented())
            .csvSeparator(overrides.getCsvSeparator() != null ? overrides.getCsvSeparator() : baseConfig.getCsvSeparator())
            .csvWithHeader(overrides.getCsvWithHeader() != null ? overrides.getCsvWithHeader() : baseConfig.getCsvWithHeader())
            .outputDirectory(overrides.getOutputDirectory() != null ? overrides.getOutputDirectory() : baseConfig.getOutputDirectory())
            .productFileName(overrides.getProductFileName() != null ? overrides.getProductFileName() : baseConfig.getProductFileName())
            .deviceFileName(overrides.getDeviceFileName() != null ? overrides.getDeviceFileName() : baseConfig.getDeviceFileName())
            .createBackup(overrides.getCreateBackup() != null ? overrides.getCreateBackup() : baseConfig.getCreateBackup())
            .build();
    }

    /**
     * 配置序列化（简单实现）
     * 实现: DM-010-05 - 配置持久化
     */
    public String serializeConfig(ExportConfig config) {
        // 简单的JSON格式序列化
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"appKey\": \"").append(maskSensitive(config.getAppKey())).append("\",\n");
        sb.append("  \"appSecret\": \"").append(maskSensitive(config.getAppSecret())).append("\",\n");
        sb.append("  \"apiHost\": \"").append(config.getApiHost()).append("\",\n");
        sb.append("  \"appId\": \"").append(config.getAppId()).append("\",\n");
        sb.append("  \"exportFormat\": \"").append(config.getExportFormat()).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 配置反序列化（简单实现）
     * 实现: DM-010-05 - 配置持久化
     */
    public ExportConfig deserializeConfig(String serializedConfig) {
        // 简单解析（生产环境应使用JSON库）
        String appKey = extractValue(serializedConfig, "appKey");
        String appSecret = extractValue(serializedConfig, "appSecret");
        String apiHost = extractValue(serializedConfig, "apiHost");
        String appId = extractValue(serializedConfig, "appId");
        String exportFormat = extractValue(serializedConfig, "exportFormat");

        return ExportConfig.builder()
            .appKey(appKey)
            .appSecret(appSecret)
            .apiHost(apiHost)
            .appId(appId)
            .exportFormat(exportFormat)
            .build();
    }

    /**
     * 重新加载配置
     * 实现: DM-010-06 - 配置动态更新
     */
    public void reloadConfig() {
        configLoaded = false;
        cachedConfig = null;
    }

    // 辅助方法
    private String getRequiredEnv(String key) {
        String value = environmentAccessor.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigurationException("Required environment variable " + key + " is missing");
        }
        return value.trim();
    }

    private String getOptionalEnv(String key, String defaultValue) {
        return environmentAccessor.get(key, defaultValue);
    }

    private Integer getIntEnv(String key, Integer defaultValue) {
        String value = environmentAccessor.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean getBooleanEnv(String key, Boolean defaultValue) {
        String value = environmentAccessor.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }

    private String extractValue(String json, String key) {
        // 简单的JSON值提取（生产环境应使用JSON库）
        String pattern = "\"" + key + "\": \"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    // 环境变量访问接口
    interface EnvironmentAccessor {
        String get(String key);
        String get(String key, String defaultValue);
    }

    // 系统环境变量访问实现
    static class SystemEnvironmentAccessor implements EnvironmentAccessor {
        @Override
        public String get(String key) {
            // 1. 优先检查环境变量
            String envValue = System.getenv(key);
            if (envValue != null && !envValue.trim().isEmpty()) {
                return envValue;
            }

            // 2. Fallback到系统属性
            return System.getProperty(key);
        }

        @Override
        public String get(String key, String defaultValue) {
            // 1. 优先检查环境变量
            String envValue = System.getenv(key);
            if (envValue != null && !envValue.trim().isEmpty()) {
                return envValue;
            }

            // 2. Fallback到系统属性
            String propValue = System.getProperty(key);
            if (propValue != null && !propValue.trim().isEmpty()) {
                return propValue;
            }

            // 3. 返回默认值
            return defaultValue;
        }
    }

    // 模拟环境变量访问实现（测试用）
    static class MockEnvironmentAccessor implements EnvironmentAccessor {
        private final Object mockEnvironment;

        public MockEnvironmentAccessor(Object mockEnvironment) {
            this.mockEnvironment = mockEnvironment;
        }

        @Override
        public String get(String key) {
            try {
                return (String) mockEnvironment.getClass().getMethod("get", String.class).invoke(mockEnvironment, key);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String get(String key, String defaultValue) {
            try {
                return (String) mockEnvironment.getClass().getMethod("get", String.class, String.class).invoke(mockEnvironment, key, defaultValue);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    // 配置异常类
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }
    }
}