package com.aep.registration.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品注册结果模型
 *
 * 封装产品注册操作的结果信息，包括成功/失败状态、产品信息、错误信息等
 *
 * @author AEP Registration Tool
 * @version 1.0
 */
public class RegistrationResult {

    // 基础结果信息
    private String operationId;        // 操作ID（用于追踪）
    private String operationType;      // 操作类型：CREATE, UPDATE, DELETE
    private boolean success;           // 操作是否成功
    private LocalDateTime timestamp;   // 操作时间戳

    // 产品信息
    private Long productId;           // 注册成功后的产品ID
    private String productName;       // 产品名称
    private String masterKey;         // 产品主密钥（注册成功后生成）
    private String apiKey;            // API访问密钥

    // 状态信息
    private String status;            // 产品状态：ACTIVE, INACTIVE, PENDING
    private String message;           // 操作结果消息
    private String errorCode;         // 错误代码（失败时）
    private String errorMessage;      // 详细错误信息

    // 详细信息
    private Integer deviceCount;      // 当前设备数量
    private Integer maxDeviceCount;   // 最大设备数量限制
    private List<String> warnings;   // 警告信息列表
    private String tenantId;          // 租户ID

    // 元数据
    private String apiResponseRaw;    // 原始API响应（调试用）
    private Long responseTimeMs;      // API响应时间（毫秒）

    // 构造函数
    public RegistrationResult() {
        this.timestamp = LocalDateTime.now();
        this.success = false;
    }

    public RegistrationResult(String operationType) {
        this();
        this.operationType = operationType;
        this.operationId = generateOperationId(operationType);
    }

    // Builder模式支持
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RegistrationResult result = new RegistrationResult();

        public Builder operationType(String operationType) {
            result.operationType = operationType;
            result.operationId = result.generateOperationId(operationType);
            return this;
        }

        public Builder success(boolean success) {
            result.success = success;
            return this;
        }

        public Builder productId(Long productId) {
            result.productId = productId;
            return this;
        }

        public Builder productName(String productName) {
            result.productName = productName;
            return this;
        }

        public Builder masterKey(String masterKey) {
            result.masterKey = masterKey;
            return this;
        }

        public Builder apiKey(String apiKey) {
            result.apiKey = apiKey;
            return this;
        }

        public Builder status(String status) {
            result.status = status;
            return this;
        }

        public Builder message(String message) {
            result.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            result.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }

        public Builder deviceCount(Integer deviceCount) {
            result.deviceCount = deviceCount;
            return this;
        }

        public Builder maxDeviceCount(Integer maxDeviceCount) {
            result.maxDeviceCount = maxDeviceCount;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            result.warnings = warnings;
            return this;
        }

        public Builder tenantId(String tenantId) {
            result.tenantId = tenantId;
            return this;
        }

        public Builder apiResponseRaw(String apiResponseRaw) {
            result.apiResponseRaw = apiResponseRaw;
            return this;
        }

        public Builder responseTimeMs(Long responseTimeMs) {
            result.responseTimeMs = responseTimeMs;
            return this;
        }

        public RegistrationResult build() {
            return result;
        }
    }

    // 便捷创建方法
    public static RegistrationResult success(String operationType, Long productId, String productName, String masterKey) {
        return builder()
            .operationType(operationType)
            .success(true)
            .productId(productId)
            .productName(productName)
            .masterKey(masterKey)
            .status("ACTIVE")
            .message("操作成功完成")
            .build();
    }

    public static RegistrationResult failure(String operationType, String errorCode, String errorMessage) {
        return builder()
            .operationType(operationType)
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .message("操作失败")
            .build();
    }

    // 工具方法
    private String generateOperationId(String operationType) {
        return (operationType != null ? operationType : "UNKNOWN") + "_" +
               System.currentTimeMillis() + "_" +
               Integer.toHexString(hashCode());
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public boolean hasProductInfo() {
        return productId != null && productName != null;
    }

    // 格式化输出方法
    public String getFormattedResult() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 产品注册结果 ===\n");
        sb.append("操作类型: ").append(operationType).append("\n");
        sb.append("操作状态: ").append(success ? "✅ 成功" : "❌ 失败").append("\n");
        sb.append("操作时间: ").append(timestamp).append("\n");

        if (success && hasProductInfo()) {
            sb.append("产品ID: ").append(productId).append("\n");
            sb.append("产品名称: ").append(productName).append("\n");
            if (masterKey != null) {
                sb.append("主密钥: ").append(maskSensitive(masterKey)).append("\n");
            }
        }

        if (!success && errorMessage != null) {
            sb.append("错误信息: ").append(errorMessage).append("\n");
            if (errorCode != null) {
                sb.append("错误代码: ").append(errorCode).append("\n");
            }
        }

        if (hasWarnings()) {
            sb.append("警告信息:\n");
            for (String warning : warnings) {
                sb.append("  - ").append(warning).append("\n");
            }
        }

        return sb.toString();
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }

    // Getter和Setter方法
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public void setSuccess(boolean success) { this.success = success; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getMasterKey() { return masterKey; }
    public void setMasterKey(String masterKey) { this.masterKey = masterKey; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getDeviceCount() { return deviceCount; }
    public void setDeviceCount(Integer deviceCount) { this.deviceCount = deviceCount; }

    public Integer getMaxDeviceCount() { return maxDeviceCount; }
    public void setMaxDeviceCount(Integer maxDeviceCount) { this.maxDeviceCount = maxDeviceCount; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getApiResponseRaw() { return apiResponseRaw; }
    public void setApiResponseRaw(String apiResponseRaw) { this.apiResponseRaw = apiResponseRaw; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    @Override
    public String toString() {
        return "RegistrationResult{" +
                "operationId='" + operationId + '\'' +
                ", operationType='" + operationType + '\'' +
                ", success=" + success +
                ", timestamp=" + timestamp +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}