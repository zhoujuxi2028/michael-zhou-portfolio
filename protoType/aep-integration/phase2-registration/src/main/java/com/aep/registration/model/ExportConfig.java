package com.aep.registration.model;

import java.util.Objects;

/**
 * Phase2产品注册配置数据模型
 * 基于Phase1.1-export的ExportConfig，针对产品注册功能优化
 *
 * @author ZCT Phase2 Registration Tool
 * @version 2.0 (基于Phase1.1)
 */
public class ExportConfig {

    // 必需字段 - AEP平台认证 (继承Phase1.1)
    private final String appKey;        // 应用密钥 (必需)
    private final String appSecret;     // 应用秘钥 (必需)
    private final String apiHost;       // API主机地址 (必需)
    private final String appId;         // 应用ID (必需)

    // 连接配置字段 (继承Phase1.1)
    private final Integer maxRetries;       // 最大重试次数
    private final Integer timeoutSeconds;  // 超时时间(秒)
    private final Boolean enableDebugLog;  // 调试日志

    // Phase2特定配置字段
    private final String defaultProductType;   // 默认产品类型
    private final String defaultDataFormat;    // 默认数据格式
    private final String defaultIndustryId;    // 默认行业ID
    private final Boolean enableProductValidation; // 启用产品验证
    private final Integer maxProductNameLength;    // 产品名称最大长度

    /**
     * 私有构造函数，强制使用Builder模式或fromEnvironment方法
     */
    private ExportConfig(Builder builder) {
        this.appKey = validateRequired(builder.appKey, "appKey");
        this.appSecret = validateRequired(builder.appSecret, "appSecret");
        this.apiHost = validateRequired(builder.apiHost, "apiHost");
        this.appId = builder.appId;

        this.maxRetries = builder.maxRetries;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.enableDebugLog = builder.enableDebugLog;

        this.defaultProductType = builder.defaultProductType;
        this.defaultDataFormat = builder.defaultDataFormat;
        this.defaultIndustryId = builder.defaultIndustryId;
        this.enableProductValidation = builder.enableProductValidation;
        this.maxProductNameLength = builder.maxProductNameLength;
    }

    /**
     * 从环境变量创建配置 (Phase2新增方法)
     * 继承Phase1.1的环境变量配置模式
     */
    public static ExportConfig fromEnvironment() {
        // 读取环境变量 - 与Phase1.1保持一致
        String appKey = getEnvOrProperty("AEP_APP_KEY");
        String appSecret = getEnvOrProperty("AEP_APP_SECRET");
        String apiHost = getEnvOrProperty("AEP_API_HOST");
        String appId = getEnvOrProperty("AEP_APP_ID");

        if (appKey == null || appSecret == null || apiHost == null) {
            throw new IllegalArgumentException("Required AEP configuration missing. " +
                "Please set AEP_APP_KEY, AEP_APP_SECRET, and AEP_API_HOST environment variables.");
        }

        // 可选配置参数
        String maxRetriesStr = getEnvOrProperty("AEP_MAX_RETRIES");
        String timeoutStr = getEnvOrProperty("AEP_TIMEOUT_SECONDS");
        String debugStr = getEnvOrProperty("AEP_ENABLE_DEBUG_LOG");

        // Phase2特定配置
        String defaultProductType = getEnvOrProperty("AEP_DEFAULT_PRODUCT_TYPE", "1");
        String defaultDataFormat = getEnvOrProperty("AEP_DEFAULT_DATA_FORMAT", "1");
        String defaultIndustryId = getEnvOrProperty("AEP_DEFAULT_INDUSTRY_ID", "1");
        String enableValidationStr = getEnvOrProperty("AEP_ENABLE_PRODUCT_VALIDATION", "true");
        String maxNameLengthStr = getEnvOrProperty("AEP_MAX_PRODUCT_NAME_LENGTH", "64");

        return builder()
            .appKey(appKey)
            .appSecret(appSecret)
            .apiHost(apiHost)
            .appId(appId)
            .maxRetries(parseIntegerOrDefault(maxRetriesStr, 3))
            .timeoutSeconds(parseIntegerOrDefault(timeoutStr, 30))
            .enableDebugLog(Boolean.parseBoolean(debugStr))
            .defaultProductType(defaultProductType)
            .defaultDataFormat(defaultDataFormat)
            .defaultIndustryId(defaultIndustryId)
            .enableProductValidation(Boolean.parseBoolean(enableValidationStr))
            .maxProductNameLength(parseIntegerOrDefault(maxNameLengthStr, 64))
            .build();
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类实现
     */
    public static class Builder {
        private String appKey;
        private String appSecret;
        private String apiHost;
        private String appId;
        private Integer maxRetries;
        private Integer timeoutSeconds;
        private Boolean enableDebugLog;
        private String defaultProductType;
        private String defaultDataFormat;
        private String defaultIndustryId;
        private Boolean enableProductValidation;
        private Integer maxProductNameLength;

        public Builder appKey(String appKey) {
            this.appKey = appKey;
            return this;
        }

        public Builder appSecret(String appSecret) {
            this.appSecret = appSecret;
            return this;
        }

        public Builder apiHost(String apiHost) {
            this.apiHost = apiHost;
            return this;
        }

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder enableDebugLog(Boolean enableDebugLog) {
            this.enableDebugLog = enableDebugLog;
            return this;
        }

        public Builder defaultProductType(String defaultProductType) {
            this.defaultProductType = defaultProductType;
            return this;
        }

        public Builder defaultDataFormat(String defaultDataFormat) {
            this.defaultDataFormat = defaultDataFormat;
            return this;
        }

        public Builder defaultIndustryId(String defaultIndustryId) {
            this.defaultIndustryId = defaultIndustryId;
            return this;
        }

        public Builder enableProductValidation(Boolean enableProductValidation) {
            this.enableProductValidation = enableProductValidation;
            return this;
        }

        public Builder maxProductNameLength(Integer maxProductNameLength) {
            this.maxProductNameLength = maxProductNameLength;
            return this;
        }

        public ExportConfig build() {
            return new ExportConfig(this);
        }
    }

    // 辅助方法 (继承Phase1.1模式)
    private static String getEnvOrProperty(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }

    private static String getEnvOrProperty(String key, String defaultValue) {
        String value = getEnvOrProperty(key);
        return value != null ? value : defaultValue;
    }

    private static Integer parseIntegerOrDefault(String value, Integer defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 字段验证逻辑 (继承Phase1.1)
     */
    private <T> T validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    // Getter方法 (继承Phase1.1 + Phase2新增)
    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }
    public String getApiHost() { return apiHost; }
    public String getAppId() { return appId; }
    public Integer getMaxRetries() { return maxRetries; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public Boolean getEnableDebugLog() { return enableDebugLog; }

    // Phase2特定Getter
    public String getDefaultProductType() { return defaultProductType; }
    public String getDefaultDataFormat() { return defaultDataFormat; }
    public String getDefaultIndustryId() { return defaultIndustryId; }
    public Boolean getEnableProductValidation() { return enableProductValidation; }
    public Integer getMaxProductNameLength() { return maxProductNameLength; }

    /**
     * equals方法实现 (继承Phase1.1模式)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportConfig that = (ExportConfig) o;
        return Objects.equals(appKey, that.appKey) &&
               Objects.equals(appSecret, that.appSecret) &&
               Objects.equals(apiHost, that.apiHost) &&
               Objects.equals(appId, that.appId) &&
               Objects.equals(maxRetries, that.maxRetries) &&
               Objects.equals(timeoutSeconds, that.timeoutSeconds) &&
               Objects.equals(enableDebugLog, that.enableDebugLog);
    }

    /**
     * hashCode方法实现 (继承Phase1.1模式)
     */
    @Override
    public int hashCode() {
        return Objects.hash(appKey, appSecret, apiHost, appId, maxRetries,
                           timeoutSeconds, enableDebugLog);
    }

    /**
     * toString方法实现，敏感信息脱敏 (继承Phase1.1模式)
     */
    @Override
    public String toString() {
        return "ExportConfig{" +
                "appKey='" + maskSensitiveValue(appKey) + '\'' +
                ", appSecret='" + maskSensitiveValue(appSecret) + '\'' +
                ", apiHost='" + apiHost + '\'' +
                ", appId='" + appId + '\'' +
                ", maxRetries=" + maxRetries +
                ", timeoutSeconds=" + timeoutSeconds +
                ", enableDebugLog=" + enableDebugLog +
                ", defaultProductType='" + defaultProductType + '\'' +
                ", defaultDataFormat='" + defaultDataFormat + '\'' +
                ", defaultIndustryId='" + defaultIndustryId + '\'' +
                ", enableProductValidation=" + enableProductValidation +
                ", maxProductNameLength=" + maxProductNameLength +
                '}';
    }

    /**
     * 敏感信息脱敏处理 (继承Phase1.1模式)
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }
}