package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 异常处理服务
 * 对应需求: FR-004-03 - API调用异常处理
 * 对应需求: FR-004-04 - 业务异常分类处理
 * 对应需求: NFR-002-01 - API调用失败重试
 * 设计模块: DM-011 - ErrorHandler
 * 负责统一的异常分类、处理和恢复机制
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class ErrorHandler {

    private final ExportConfig config;
    private final SimpleDateFormat logTimestampFormat;
    private final int maxRetries;

    /**
     * 构造函数
     * 实现: DM-011-01 - 异常处理器初始化
     */
    public ErrorHandler(ExportConfig config) {
        this.config = config;
        this.maxRetries = config.getMaxRetries() != null ? config.getMaxRetries() : 3;
        this.logTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    /**
     * 处理异常的主入口方法
     * 实现: DM-011-02 - 异常统一处理
     */
    public ErrorResponse handleException(Exception exception, String operation) {
        // 分类异常类型
        ErrorType errorType = classifyException(exception);

        // 确定是否可重试
        boolean retryable = isRetryable(errorType);

        // 生成错误代码
        String errorCode = generateErrorCode(errorType);

        // 生成建议操作
        String suggestedAction = generateSuggestedAction(errorType, exception);

        // 记录错误日志
        String logEntry = createErrorLogEntry(exception, operation, null);
        logError(logEntry);

        return createErrorResponse(errorType, exception.getMessage(), errorCode, retryable, suggestedAction);
    }

    /**
     * 异常分类
     * 实现: DM-011-03 - 异常分类逻辑
     */
    public ErrorType classifyException(Exception exception) {
        String message = exception.getMessage().toLowerCase();

        // 网络错误
        if (message.contains("timeout") ||
            message.contains("connection") ||
            message.contains("host") ||
            message.contains("network") ||
            message.contains("socket")) {
            return ErrorType.NETWORK_ERROR;
        }

        // 认证错误
        if (message.contains("authentication") ||
            message.contains("auth") ||
            message.contains("invalid key") ||
            message.contains("token") ||
            message.contains("unauthorized")) {
            return ErrorType.AUTHENTICATION_ERROR;
        }

        // 授权错误
        if (message.contains("authorization") ||
            message.contains("forbidden") ||
            message.contains("permission")) {
            return ErrorType.AUTHORIZATION_ERROR;
        }

        // 配置错误
        if (message.contains("configuration") ||
            message.contains("config") ||
            message.contains("invalid") ||
            message.contains("missing")) {
            return ErrorType.CONFIGURATION_ERROR;
        }

        // 限流错误
        if (message.contains("rate limit") ||
            message.contains("too many requests") ||
            message.contains("quota") ||
            message.contains("throttle")) {
            return ErrorType.RATE_LIMIT_ERROR;
        }

        // 数据验证错误
        if (message.contains("validation") ||
            message.contains("invalid data") ||
            message.contains("format") ||
            message.contains("parse")) {
            return ErrorType.BUSINESS_ERROR;
        }

        // 其他未知错误
        return ErrorType.UNKNOWN_ERROR;
    }

    /**
     * 判断错误是否可重试
     * 实现: DM-011-04 - 重试判断逻辑
     */
    private boolean isRetryable(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
            case RATE_LIMIT_ERROR:
            case UNKNOWN_ERROR:
                return true;
            case AUTHENTICATION_ERROR:
            case AUTHORIZATION_ERROR:
            case CONFIGURATION_ERROR:
            case BUSINESS_ERROR:
                return false;
            default:
                return false;
        }
    }

    /**
     * 计算重试延迟（指数退避）
     * 实现: DM-011-05 - 指数退避算法
     */
    public long calculateRetryDelay(int retryAttempt) {
        // 基础延迟1秒，每次重试时间翻倍，最大不超过30秒
        long baseDelay = 1000; // 1秒
        long delay = baseDelay * (1L << (retryAttempt - 1)); // 2^(n-1) 秒
        long maxDelay = 30000; // 30秒

        return Math.min(delay, maxDelay);
    }

    /**
     * 判断是否应该重试
     * 实现: DM-011-05 - 重试控制
     */
    public boolean shouldRetry(int currentAttempt) {
        return currentAttempt <= maxRetries;
    }

    /**
     * 获取恢复操作策略
     * 实现: DM-011-06 - 恢复策略
     */
    public RecoveryAction getRecoveryAction(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
            case RATE_LIMIT_ERROR:
                return RecoveryAction.RETRY_WITH_BACKOFF;
            case AUTHENTICATION_ERROR:
            case AUTHORIZATION_ERROR:
            case CONFIGURATION_ERROR:
                return RecoveryAction.FAIL_FAST;
            case BUSINESS_ERROR:
                return RecoveryAction.LOG_AND_CONTINUE;
            case UNKNOWN_ERROR:
                return RecoveryAction.RETRY_WITH_BACKOFF;
            default:
                return RecoveryAction.FAIL_FAST;
        }
    }

    /**
     * 创建错误日志条目
     * 实现: DM-011-07 - 结构化日志
     */
    public String createErrorLogEntry(Exception exception, String operation, String additionalContext) {
        StringBuilder logEntry = new StringBuilder();

        logEntry.append("{\n");
        logEntry.append("  \"timestamp\": \"").append(logTimestampFormat.format(new Date())).append("\",\n");
        logEntry.append("  \"level\": \"ERROR\",\n");
        logEntry.append("  \"operation\": \"").append(operation).append("\",\n");
        logEntry.append("  \"errorType\": \"").append(classifyException(exception)).append("\",\n");
        logEntry.append("  \"message\": \"").append(escapeJsonString(exception.getMessage())).append("\",\n");
        logEntry.append("  \"exceptionClass\": \"").append(exception.getClass().getSimpleName()).append("\"");

        if (additionalContext != null && !additionalContext.trim().isEmpty()) {
            logEntry.append(",\n  \"context\": \"").append(escapeJsonString(additionalContext)).append("\"");
        }

        if (exception.getCause() != null) {
            logEntry.append(",\n  \"cause\": \"").append(escapeJsonString(exception.getCause().getMessage())).append("\"");
        }

        logEntry.append("\n}");

        return logEntry.toString();
    }

    /**
     * 创建错误响应
     * 实现: DM-011-08 - 错误响应构建
     */
    public ErrorResponse createErrorResponse(ErrorType errorType, String message, String errorCode,
                                           boolean retryable, String suggestedAction) {
        return new ErrorResponse(errorType, message, errorCode, retryable, suggestedAction);
    }

    // 辅助方法

    /**
     * 生成错误代码
     */
    private String generateErrorCode(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "NETWORK_001";
            case AUTHENTICATION_ERROR:
                return "AUTH_001";
            case AUTHORIZATION_ERROR:
                return "AUTH_002";
            case CONFIGURATION_ERROR:
                return "CONFIG_001";
            case RATE_LIMIT_ERROR:
                return "LIMIT_001";
            case BUSINESS_ERROR:
                return "BUSINESS_001";
            case UNKNOWN_ERROR:
            default:
                return "UNKNOWN_001";
        }
    }

    /**
     * 生成建议操作
     */
    private String generateSuggestedAction(ErrorType errorType, Exception exception) {
        switch (errorType) {
            case NETWORK_ERROR:
                return "Please check network connection and retry the operation.";
            case AUTHENTICATION_ERROR:
                return "Please check your API key and secret configuration.";
            case AUTHORIZATION_ERROR:
                return "Please verify your account permissions for this operation.";
            case CONFIGURATION_ERROR:
                return "Please check your configuration settings and ensure all required parameters are provided.";
            case RATE_LIMIT_ERROR:
                return "Please wait and retry later. Consider implementing request throttling.";
            case BUSINESS_ERROR:
                return "Please check your input data and ensure it meets the required format.";
            case UNKNOWN_ERROR:
            default:
                return "Please retry the operation. If the problem persists, contact technical support.";
        }
    }

    /**
     * 记录错误日志
     */
    private void logError(String logEntry) {
        // 统一使用LogManager记录错误日志
        LogManager.getInstance().error("ErrorHandler", "结构化错误日志: " + logEntry);

        // 保持原有的调试输出（兼容性）
        if (config.getEnableDebugLog() != null && config.getEnableDebugLog()) {
            System.err.println("ERROR LOG: " + logEntry);
        }
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        AUTHORIZATION_ERROR,
        BUSINESS_ERROR,
        CONFIGURATION_ERROR,
        RATE_LIMIT_ERROR,
        UNKNOWN_ERROR
    }

    /**
     * 恢复操作枚举
     */
    public enum RecoveryAction {
        RETRY_WITH_BACKOFF,
        RETRY_IMMEDIATE,
        FAIL_FAST,
        LOG_AND_CONTINUE,
        FALLBACK_OPERATION
    }

    /**
     * 错误响应类
     */
    public static class ErrorResponse {
        private final ErrorType errorType;
        private final String message;
        private final String errorCode;
        private final boolean retryable;
        private final String suggestedAction;

        public ErrorResponse(ErrorType errorType, String message, String errorCode, boolean retryable, String suggestedAction) {
            this.errorType = errorType;
            this.message = message;
            this.errorCode = errorCode;
            this.retryable = retryable;
            this.suggestedAction = suggestedAction;
        }

        public ErrorType getErrorType() { return errorType; }
        public String getMessage() { return message; }
        public String getErrorCode() { return errorCode; }
        public boolean isRetryable() { return retryable; }
        public String getSuggestedAction() { return suggestedAction; }

        @Override
        public String toString() {
            return "ErrorResponse{" +
                    "errorType=" + errorType +
                    ", message='" + message + '\'' +
                    ", errorCode='" + errorCode + '\'' +
                    ", retryable=" + retryable +
                    ", suggestedAction='" + suggestedAction + '\'' +
                    '}';
        }
    }
}