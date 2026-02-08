package com.aep.registration.database.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 操作日志实体类
 *
 * 对应数据库表: aep_operation_logs
 *
 * 功能：
 * - 记录所有AEP API操作的详细信息
 * - 支持操作审计和故障排查
 * - 提供性能统计数据
 * - 确保敏感信息安全存储
 *
 * @author AEP Integration Team
 * @version 1.0.0
 * @since 2026-01-25
 */
public class OperationLog {

    // 主键ID
    private Long id;

    // 操作唯一标识
    private String operationId;

    // 操作类型 (CREATE/UPDATE/DELETE/QUERY)
    private String operationType;

    // 资源类型 (PRODUCT/DEVICE/SUBSCRIPTION)
    private String resourceType;

    // 资源ID
    private String resourceId;

    // 资源名称
    private String resourceName;

    // 操作状态 (SUCCESS/FAILED/PENDING)
    private String operationStatus;

    // 错误码
    private String errorCode;

    // 错误信息
    private String errorMessage;

    // 请求参数(JSON格式，脱敏后)
    private String requestParams;

    // 响应数据(JSON格式，脱敏后)
    private String responseData;

    // 执行时长(毫秒)
    private Integer executionTimeMs;

    // 开始时间
    private LocalDateTime startTime;

    // 结束时间
    private LocalDateTime endTime;

    // 操作者
    private String operator;

    // 客户端IP
    private String clientIp;

    // 用户代理
    private String userAgent;

    /**
     * 默认构造函数
     */
    public OperationLog() {
        this.startTime = LocalDateTime.now();
        this.operationStatus = "PENDING";
    }

    /**
     * 构造函数 - 创建操作日志
     */
    public OperationLog(String operationId, String operationType, String resourceType) {
        this();
        this.operationId = operationId;
        this.operationType = operationType;
        this.resourceType = resourceType;
    }

    /**
     * 建造者模式
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OperationLog log = new OperationLog();

        public Builder operationId(String operationId) {
            log.operationId = operationId;
            return this;
        }

        public Builder operationType(String operationType) {
            log.operationType = operationType;
            return this;
        }

        public Builder resourceType(String resourceType) {
            log.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            log.resourceId = resourceId;
            return this;
        }

        public Builder resourceName(String resourceName) {
            log.resourceName = resourceName;
            return this;
        }

        public Builder operator(String operator) {
            log.operator = operator;
            return this;
        }

        public Builder clientIp(String clientIp) {
            log.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            log.userAgent = userAgent;
            return this;
        }

        public Builder requestParams(String requestParams) {
            log.requestParams = requestParams;
            return this;
        }

        public OperationLog build() {
            Objects.requireNonNull(log.operationId, "操作ID不能为空");
            Objects.requireNonNull(log.operationType, "操作类型不能为空");
            Objects.requireNonNull(log.resourceType, "资源类型不能为空");
            return log;
        }
    }

    /**
     * 业务方法 - 标记操作成功
     */
    public void markSuccess() {
        this.operationStatus = "SUCCESS";
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.executionTimeMs = (int) java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    /**
     * 业务方法 - 标记操作失败
     */
    public void markFailure(String errorCode, String errorMessage) {
        this.operationStatus = "FAILED";
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.executionTimeMs = (int) java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    /**
     * 业务方法 - 检查操作是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.operationStatus);
    }

    /**
     * 业务方法 - 检查操作是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(this.operationStatus);
    }

    /**
     * 业务方法 - 检查操作是否仍在进行中
     */
    public boolean isPending() {
        return "PENDING".equals(this.operationStatus);
    }

    /**
     * 业务方法 - 获取操作耗时描述
     */
    public String getExecutionTimeDescription() {
        if (executionTimeMs == null) {
            return "未知";
        }

        if (executionTimeMs < 1000) {
            return executionTimeMs + "ms";
        } else if (executionTimeMs < 60000) {
            return String.format("%.2fs", executionTimeMs / 1000.0);
        } else {
            long minutes = executionTimeMs / 60000;
            long seconds = (executionTimeMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * 业务方法 - 获取操作结果概要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(operationType).append(" ").append(resourceType);
        if (resourceName != null) {
            summary.append(" '").append(resourceName).append("'");
        }
        summary.append(" - ").append(operationStatus);
        if (isFailed() && errorCode != null) {
            summary.append(" (").append(errorCode).append(")");
        }
        if (executionTimeMs != null) {
            summary.append(" [").append(getExecutionTimeDescription()).append("]");
        }
        return summary.toString();
    }

    // ==================== Getter和Setter方法 ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public Integer getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Integer executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    // ==================== Object方法重写 ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationLog that = (OperationLog) o;
        return Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationId);
    }

    @Override
    public String toString() {
        return "OperationLog{" +
                "id=" + id +
                ", operationId='" + operationId + '\'' +
                ", operationType='" + operationType + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", operationStatus='" + operationStatus + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", operator='" + operator + '\'' +
                '}';
    }

    /**
     * 用于安全日志输出的toString（隐藏敏感信息）
     */
    public String toSafeString() {
        return "OperationLog{" +
                "operationId='" + operationId + '\'' +
                ", operation='" + operationType + " " + resourceType + '\'' +
                ", status='" + operationStatus + '\'' +
                ", duration=" + getExecutionTimeDescription() +
                ", operator='" + operator + '\'' +
                '}';
    }
}