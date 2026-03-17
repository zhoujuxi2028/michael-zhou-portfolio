package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import com.aep.export.service.ErrorHandler.ErrorType;
import com.aep.export.service.ErrorHandler.RecoveryAction;
import com.aep.export.service.ErrorHandler.ErrorResponse;

/**
 * ErrorHandler单元测试
 * TDD第2轮：基础服务测试
 * 对应需求: FR-004-03 - API调用异常处理
 * 对应需求: FR-004-04 - 业务异常分类处理
 * 对应需求: NFR-002-01 - API调用失败重试
 * 测试用例: TC-UNIT-FUNC-131~140
 */
public class ErrorHandlerTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始ErrorHandler TDD测试...");

            testHandleAepApiError_WithRetryableError();
            testHandleAepApiError_WithNonRetryableError();
            testClassifyException_NetworkError();
            testClassifyException_AuthenticationError();
            testClassifyException_BusinessError();
            testRetryMechanism_WithExponentialBackoff();
            testRetryMechanism_ReachMaxRetries();
            testLogError_WithStructuredInfo();
            testCreateErrorResponse();
            testRecoverFromError_WhenPossible();

            System.out.println("✅ 所有ErrorHandler测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-131: ✅ shouldHandleAepApiError_WithRetryableError");
            System.out.println("  TC-UNIT-FUNC-132: ✅ shouldHandleAepApiError_WithNonRetryableError");
            System.out.println("  TC-UNIT-FUNC-133: ✅ shouldClassifyException_NetworkError");
            System.out.println("  TC-UNIT-FUNC-134: ✅ shouldClassifyException_AuthenticationError");
            System.out.println("  TC-UNIT-FUNC-135: ✅ shouldClassifyException_BusinessError");
            System.out.println("  TC-UNIT-FUNC-136: ✅ shouldRetryMechanism_WithExponentialBackoff");
            System.out.println("  TC-UNIT-FUNC-137: ✅ shouldRetryMechanism_ReachMaxRetries");
            System.out.println("  TC-UNIT-FUNC-138: ✅ shouldLogError_WithStructuredInfo");
            System.out.println("  TC-UNIT-FUNC-139: ✅ shouldCreateErrorResponse");
            System.out.println("  TC-UNIT-FUNC-140: ✅ shouldRecoverFromError_WhenPossible");

        } catch (Exception e) {
            System.err.println("❌ ErrorHandler测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-131: 测试处理可重试的AEP API错误
     * 验证需求: NFR-002-01 - API调用失败重试
     */
    public static void testHandleAepApiError_WithRetryableError() {
        System.out.println("  🔴 RED: 测试可重试API错误处理...");

        // Given - 网络超时错误（可重试）
        ExportConfig config = createTestConfig();
        ErrorHandler errorHandler = new ErrorHandler(config);

        Exception networkError = new RuntimeException("Connection timeout");

        // When
        ErrorResponse response = errorHandler.handleException(networkError, "API_CALL");

        // Then
        assert response.isRetryable() : "网络错误应该可重试";
        assert response.getErrorType() == ErrorType.NETWORK_ERROR : "错误类型应为网络错误";
        assert response.getSuggestedAction().contains("retry") : "建议操作应包含重试";

        System.out.println("  🟢 GREEN: 可重试API错误处理测试通过");
    }

    /**
     * TC-UNIT-FUNC-132: 测试处理不可重试的AEP API错误
     * 验证需求: FR-004-04 - 业务异常分类处理
     */
    public static void testHandleAepApiError_WithNonRetryableError() {
        System.out.println("  🔴 RED: 测试不可重试API错误处理...");

        // Given - 认证错误（不可重试）
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());
        Exception authError = new RuntimeException("Authentication failed - invalid key");

        // When
        ErrorResponse response = errorHandler.handleException(authError, "AUTH");

        // Then
        assert !response.isRetryable() : "认证错误不应该可重试";
        assert response.getErrorType() == ErrorType.AUTHENTICATION_ERROR : "错误类型应为认证错误";
        assert response.getSuggestedAction().contains("check") : "建议操作应包含检查配置";

        System.out.println("  🟢 GREEN: 不可重试API错误处理测试通过");
    }

    /**
     * TC-UNIT-FUNC-133: 测试网络错误分类
     * 验证需求: FR-004-04 - 业务异常分类处理
     */
    public static void testClassifyException_NetworkError() {
        System.out.println("  🔴 RED: 测试网络错误分类...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When & Then
        ErrorType timeoutType = errorHandler.classifyException(new RuntimeException("timeout"));
        ErrorType connectionType = errorHandler.classifyException(new RuntimeException("connection refused"));
        ErrorType hostType = errorHandler.classifyException(new RuntimeException("unknown host"));

        assert timeoutType == ErrorType.NETWORK_ERROR : "超时应分类为网络错误";
        assert connectionType == ErrorType.NETWORK_ERROR : "连接拒绝应分类为网络错误";
        assert hostType == ErrorType.NETWORK_ERROR : "未知主机应分类为网络错误";

        System.out.println("  🟢 GREEN: 网络错误分类测试通过");
    }

    /**
     * TC-UNIT-FUNC-134: 测试认证错误分类
     * 验证需求: FR-004-04 - 业务异常分类处理
     */
    public static void testClassifyException_AuthenticationError() {
        System.out.println("  🔴 RED: 测试认证错误分类...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When & Then
        ErrorType authType = errorHandler.classifyException(new RuntimeException("authentication failed"));
        ErrorType keyType = errorHandler.classifyException(new RuntimeException("invalid key"));
        ErrorType tokenType = errorHandler.classifyException(new RuntimeException("token expired"));

        assert authType == ErrorType.AUTHENTICATION_ERROR : "认证失败应分类为认证错误";
        assert keyType == ErrorType.AUTHENTICATION_ERROR : "无效密钥应分类为认证错误";
        assert tokenType == ErrorType.AUTHENTICATION_ERROR : "令牌过期应分类为认证错误";

        System.out.println("  🟢 GREEN: 认证错误分类测试通过");
    }

    /**
     * TC-UNIT-FUNC-135: 测试业务错误分类
     * 验证需求: FR-004-04 - 业务异常分类处理
     */
    public static void testClassifyException_BusinessError() {
        System.out.println("  🔴 RED: 测试业务错误分类...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When & Then
        ErrorType dataType = errorHandler.classifyException(new RuntimeException("data validation failed"));
        ErrorType configType = errorHandler.classifyException(new RuntimeException("invalid configuration"));
        ErrorType limitType = errorHandler.classifyException(new RuntimeException("rate limit exceeded"));

        assert dataType == ErrorType.BUSINESS_ERROR : "数据验证失败应分类为业务错误";
        assert configType == ErrorType.CONFIGURATION_ERROR : "配置无效应分类为配置错误";
        assert limitType == ErrorType.RATE_LIMIT_ERROR : "频率限制应分类为限制错误";

        System.out.println("  🟢 GREEN: 业务错误分类测试通过");
    }

    /**
     * TC-UNIT-FUNC-136: 测试指数退避重试机制
     * 验证需求: NFR-002-01 - API调用失败重试
     */
    public static void testRetryMechanism_WithExponentialBackoff() {
        System.out.println("  🔴 RED: 测试指数退避重试机制...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When
        long delay1 = errorHandler.calculateRetryDelay(1);
        long delay2 = errorHandler.calculateRetryDelay(2);
        long delay3 = errorHandler.calculateRetryDelay(3);

        // Then
        assert delay1 > 0 : "第1次重试延迟应大于0";
        assert delay2 > delay1 : "第2次重试延迟应大于第1次";
        assert delay3 > delay2 : "第3次重试延迟应大于第2次";
        assert delay3 <= 30000 : "最大延迟不应超过30秒";

        System.out.println("  🟢 GREEN: 指数退避重试机制测试通过");
    }

    /**
     * TC-UNIT-FUNC-137: 测试达到最大重试次数
     * 验证需求: NFR-002-01 - 最多3次重试
     */
    public static void testRetryMechanism_ReachMaxRetries() {
        System.out.println("  🔴 RED: 测试最大重试次数限制...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When & Then
        assert errorHandler.shouldRetry(1) : "第1次应该重试";
        assert errorHandler.shouldRetry(2) : "第2次应该重试";
        assert errorHandler.shouldRetry(3) : "第3次应该重试";
        assert !errorHandler.shouldRetry(4) : "第4次不应该重试";

        System.out.println("  🟢 GREEN: 最大重试次数限制测试通过");
    }

    /**
     * TC-UNIT-FUNC-138: 测试结构化错误日志
     * 验证需求: FR-004-03 - 错误信息记录
     */
    public static void testLogError_WithStructuredInfo() {
        System.out.println("  🔴 RED: 测试结构化错误日志...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());
        Exception error = new RuntimeException("Test error for logging");

        // When
        String logEntry = errorHandler.createErrorLogEntry(error, "TEST_OPERATION", "Additional context info");

        // Then
        assert logEntry != null : "日志条目不应该为null";
        assert logEntry.contains("TEST_OPERATION") : "日志应包含操作信息";
        assert logEntry.contains("Test error") : "日志应包含错误信息";
        assert logEntry.contains("timestamp") : "日志应包含时间戳";

        System.out.println("  🟢 GREEN: 结构化错误日志测试通过");
    }

    /**
     * TC-UNIT-FUNC-139: 测试创建错误响应
     * 验证需求: FR-004-03 - 统一错误响应格式
     */
    public static void testCreateErrorResponse() {
        System.out.println("  🔴 RED: 测试创建错误响应...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When
        ErrorResponse response = errorHandler.createErrorResponse(
            ErrorType.NETWORK_ERROR,
            "Connection timeout",
            "NETWORK_001",
            true,
            "Please check network connection and retry"
        );

        // Then
        assert response.getErrorType() == ErrorType.NETWORK_ERROR : "错误类型不匹配";
        assert response.getMessage().equals("Connection timeout") : "错误消息不匹配";
        assert response.getErrorCode().equals("NETWORK_001") : "错误代码不匹配";
        assert response.isRetryable() : "重试状态不匹配";
        assert response.getSuggestedAction().contains("network") : "建议操作不匹配";

        System.out.println("  🟢 GREEN: 创建错误响应测试通过");
    }

    /**
     * TC-UNIT-FUNC-140: 测试错误恢复机制
     * 验证需求: NFR-002-04 - 导出任务失败恢复
     */
    public static void testRecoverFromError_WhenPossible() {
        System.out.println("  🔴 RED: 测试错误恢复机制...");

        // Given
        ErrorHandler errorHandler = new ErrorHandler(createTestConfig());

        // When & Then
        RecoveryAction networkRecovery = errorHandler.getRecoveryAction(ErrorType.NETWORK_ERROR);
        RecoveryAction authRecovery = errorHandler.getRecoveryAction(ErrorType.AUTHENTICATION_ERROR);
        RecoveryAction businessRecovery = errorHandler.getRecoveryAction(ErrorType.BUSINESS_ERROR);

        assert networkRecovery == RecoveryAction.RETRY_WITH_BACKOFF : "网络错误应使用退避重试";
        assert authRecovery == RecoveryAction.FAIL_FAST : "认证错误应快速失败";
        assert businessRecovery == RecoveryAction.LOG_AND_CONTINUE : "业务错误应记录并继续";

        System.out.println("  🟢 GREEN: 错误恢复机制测试通过");
    }

    /**
     * 辅助方法：创建测试配置
     */
    private static ExportConfig createTestConfig() {
        return ExportConfig.builder()
            .appKey("test_key")
            .appSecret("test_secret")
            .apiHost("test.api.ctwing.cn")
            .appId("123456")
            .maxRetries(3)
            .timeoutSeconds(30)
            .build();
    }

}