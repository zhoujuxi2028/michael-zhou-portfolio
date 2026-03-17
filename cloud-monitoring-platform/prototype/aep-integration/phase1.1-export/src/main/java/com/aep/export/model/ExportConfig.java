package com.aep.export.model;

import java.util.Objects;

/**
 * 导出配置数据模型
 * 对应需求: FR-004-01 - 环境变量配置读取
 * 对应需求: FR-004-02 - 配置参数验证
 * 设计模块: DM-015 - ExportConfig
 * 基于AEP平台认证和导出功能的配置需求
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class ExportConfig {

    // 必需字段 - AEP平台认证 (DM-015-01)
    private final String appKey;        // 应用密钥 (必需)
    private final String appSecret;     // 应用秘钥 (必需)
    private final String apiHost;       // API主机地址 (必需)
    private final String appId;         // 应用ID (必需)

    // 连接配置字段 (DM-015-01)
    private final Integer maxRetries;       // 最大重试次数
    private final Integer timeoutSeconds;  // 超时时间(秒)
    private final Integer pageSize;        // 分页大小
    private final Boolean enableDebugLog;  // 调试日志

    // 导出格式配置字段 (DM-015-01)
    private final String exportFormat;     // 导出格式: JSON, CSV
    private final Boolean jsonIndented;    // JSON是否缩进
    private final String csvSeparator;     // CSV分隔符
    private final Boolean csvWithHeader;   // CSV是否包含标题

    // 文件输出配置字段 (DM-015-01)
    private final String outputDirectory;  // 输出目录
    private final String productFileName;  // 产品文件名
    private final String deviceFileName;   // 设备文件名
    private final Boolean createBackup;    // 是否创建备份

    /**
     * 私有构造函数，强制使用Builder模式
     * 实现: DM-015-02 - Builder模式实现
     */
    private ExportConfig(Builder builder) {
        this.appKey = validateRequired(builder.appKey, "appKey");
        this.appSecret = validateRequired(builder.appSecret, "appSecret");
        this.apiHost = validateRequired(builder.apiHost, "apiHost");
        this.appId = builder.appId;

        this.maxRetries = builder.maxRetries;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.pageSize = builder.pageSize;
        this.enableDebugLog = builder.enableDebugLog;

        this.exportFormat = builder.exportFormat;
        this.jsonIndented = builder.jsonIndented;
        this.csvSeparator = builder.csvSeparator;
        this.csvWithHeader = builder.csvWithHeader;

        this.outputDirectory = builder.outputDirectory;
        this.productFileName = builder.productFileName;
        this.deviceFileName = builder.deviceFileName;
        this.createBackup = builder.createBackup;
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类实现
     * 实现: DM-015-02 - Builder模式实现
     */
    public static class Builder {
        private String appKey;
        private String appSecret;
        private String apiHost;
        private String appId;
        private Integer maxRetries;
        private Integer timeoutSeconds;
        private Integer pageSize;
        private Boolean enableDebugLog;
        private String exportFormat;
        private Boolean jsonIndented;
        private String csvSeparator;
        private Boolean csvWithHeader;
        private String outputDirectory;
        private String productFileName;
        private String deviceFileName;
        private Boolean createBackup;

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

        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder enableDebugLog(Boolean enableDebugLog) {
            this.enableDebugLog = enableDebugLog;
            return this;
        }

        public Builder exportFormat(String exportFormat) {
            this.exportFormat = exportFormat;
            return this;
        }

        public Builder jsonIndented(Boolean jsonIndented) {
            this.jsonIndented = jsonIndented;
            return this;
        }

        public Builder csvSeparator(String csvSeparator) {
            this.csvSeparator = csvSeparator;
            return this;
        }

        public Builder csvWithHeader(Boolean csvWithHeader) {
            this.csvWithHeader = csvWithHeader;
            return this;
        }

        public Builder outputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public Builder productFileName(String productFileName) {
            this.productFileName = productFileName;
            return this;
        }

        public Builder deviceFileName(String deviceFileName) {
            this.deviceFileName = deviceFileName;
            return this;
        }

        public Builder createBackup(Boolean createBackup) {
            this.createBackup = createBackup;
            return this;
        }

        public ExportConfig build() {
            return new ExportConfig(this);
        }
    }

    /**
     * 字段验证逻辑
     * 实现: DM-015-03 - 字段验证逻辑
     */
    private <T> T validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    // Getter方法
    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }
    public String getApiHost() { return apiHost; }
    public String getAppId() { return appId; }
    public Integer getMaxRetries() { return maxRetries; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public Integer getPageSize() { return pageSize; }
    public Boolean getEnableDebugLog() { return enableDebugLog; }
    public String getExportFormat() { return exportFormat; }
    public Boolean getJsonIndented() { return jsonIndented; }
    public String getCsvSeparator() { return csvSeparator; }
    public Boolean getCsvWithHeader() { return csvWithHeader; }
    public String getOutputDirectory() { return outputDirectory; }
    public String getProductFileName() { return productFileName; }
    public String getDeviceFileName() { return deviceFileName; }
    public Boolean getCreateBackup() { return createBackup; }

    /**
     * equals方法实现
     * 实现: DM-015-04 - equals/hashCode实现
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
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(enableDebugLog, that.enableDebugLog) &&
               Objects.equals(exportFormat, that.exportFormat) &&
               Objects.equals(csvSeparator, that.csvSeparator) &&
               Objects.equals(outputDirectory, that.outputDirectory);
    }

    /**
     * hashCode方法实现
     * 实现: DM-015-04 - equals/hashCode实现
     */
    @Override
    public int hashCode() {
        return Objects.hash(appKey, appSecret, apiHost, appId, maxRetries,
                           timeoutSeconds, pageSize, enableDebugLog, exportFormat,
                           csvSeparator, outputDirectory);
    }

    /**
     * toString方法实现，敏感信息脱敏
     * 实现: DM-015-05 - toString安全实现
     * 满足需求: NFR-003-02 - 敏感信息脱敏处理
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
                ", pageSize=" + pageSize +
                ", enableDebugLog=" + enableDebugLog +
                ", exportFormat='" + exportFormat + '\'' +
                ", jsonIndented=" + jsonIndented +
                ", csvSeparator='" + csvSeparator + '\'' +
                ", csvWithHeader=" + csvWithHeader +
                ", outputDirectory='" + outputDirectory + '\'' +
                ", productFileName='" + productFileName + '\'' +
                ", deviceFileName='" + deviceFileName + '\'' +
                ", createBackup=" + createBackup +
                '}';
    }

    /**
     * 敏感信息脱敏处理
     * 实现: DM-015-05 - toString安全实现
     * 对认证密钥进行适当脱敏
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }
}