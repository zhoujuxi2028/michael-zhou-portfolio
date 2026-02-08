package com.aep.registration.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * RegistrationResult 测试类
 */
class RegistrationResultTest {

    private RegistrationResult result;

    @BeforeEach
    void setUp() {
        result = new RegistrationResult();
    }

    @Test
    void testSuccessResult() {
        RegistrationResult success = RegistrationResult.success("CREATE", 12345L, "测试产品", "test-master-key");

        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertEquals("CREATE", success.getOperationType());
        assertEquals(Long.valueOf(12345L), success.getProductId());
        assertEquals("测试产品", success.getProductName());
        assertEquals("test-master-key", success.getMasterKey());
        assertEquals("ACTIVE", success.getStatus());
        assertEquals("操作成功完成", success.getMessage());
        assertTrue(success.hasProductInfo());
        assertFalse(success.hasWarnings());
    }

    @Test
    void testFailureResult() {
        RegistrationResult failure = RegistrationResult.failure("CREATE", "VALIDATION_ERROR", "产品名称不能为空");

        assertFalse(failure.isSuccess());
        assertTrue(failure.isFailure());
        assertEquals("CREATE", failure.getOperationType());
        assertEquals("VALIDATION_ERROR", failure.getErrorCode());
        assertEquals("产品名称不能为空", failure.getErrorMessage());
        assertEquals("操作失败", failure.getMessage());
        assertFalse(failure.hasProductInfo());
    }

    @Test
    void testBuilderPattern() {
        List<String> warnings = Arrays.asList("警告1", "警告2");

        RegistrationResult built = RegistrationResult.builder()
            .operationType("UPDATE")
            .success(true)
            .productId(67890L)
            .productName("Builder产品")
            .masterKey("builder-master-key")
            .apiKey("builder-api-key")
            .status("ACTIVE")
            .message("更新成功")
            .deviceCount(15)
            .maxDeviceCount(100)
            .warnings(warnings)
            .tenantId("tenant-123")
            .apiResponseRaw("{\"code\":0,\"msg\":\"ok\"}")
            .responseTimeMs(1500L)
            .build();

        assertTrue(built.isSuccess());
        assertEquals("UPDATE", built.getOperationType());
        assertEquals(Long.valueOf(67890L), built.getProductId());
        assertEquals("Builder产品", built.getProductName());
        assertEquals("builder-master-key", built.getMasterKey());
        assertEquals("builder-api-key", built.getApiKey());
        assertEquals("ACTIVE", built.getStatus());
        assertEquals("更新成功", built.getMessage());
        assertEquals(Integer.valueOf(15), built.getDeviceCount());
        assertEquals(Integer.valueOf(100), built.getMaxDeviceCount());
        assertEquals(warnings, built.getWarnings());
        assertEquals("tenant-123", built.getTenantId());
        assertEquals("{\"code\":0,\"msg\":\"ok\"}", built.getApiResponseRaw());
        assertEquals(Long.valueOf(1500L), built.getResponseTimeMs());
        assertTrue(built.hasWarnings());
    }

    @Test
    void testOperationIdGeneration() {
        RegistrationResult createResult = new RegistrationResult("CREATE");
        RegistrationResult updateResult = new RegistrationResult("UPDATE");

        assertNotNull(createResult.getOperationId());
        assertNotNull(updateResult.getOperationId());
        assertTrue(createResult.getOperationId().startsWith("CREATE_"));
        assertTrue(updateResult.getOperationId().startsWith("UPDATE_"));
        assertNotEquals(createResult.getOperationId(), updateResult.getOperationId());
    }

    @Test
    void testTimestamp() {
        RegistrationResult result1 = new RegistrationResult();

        // 等待一小段时间
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RegistrationResult result2 = new RegistrationResult();

        assertNotNull(result1.getTimestamp());
        assertNotNull(result2.getTimestamp());
        assertTrue(result2.getTimestamp().isAfter(result1.getTimestamp()) ||
                   result2.getTimestamp().isEqual(result1.getTimestamp()));
    }

    @Test
    void testHasProductInfo() {
        RegistrationResult withProductInfo = RegistrationResult.builder()
            .productId(123L)
            .productName("有产品信息")
            .build();

        RegistrationResult withoutProductInfo = RegistrationResult.builder()
            .productId(123L)
            .build();

        RegistrationResult noProductInfo = new RegistrationResult();

        assertTrue(withProductInfo.hasProductInfo());
        assertFalse(withoutProductInfo.hasProductInfo());
        assertFalse(noProductInfo.hasProductInfo());
    }

    @Test
    void testHasWarnings() {
        List<String> warnings = Arrays.asList("警告信息");
        List<String> emptyWarnings = Arrays.asList();

        RegistrationResult withWarnings = RegistrationResult.builder()
            .warnings(warnings)
            .build();

        RegistrationResult withEmptyWarnings = RegistrationResult.builder()
            .warnings(emptyWarnings)
            .build();

        RegistrationResult noWarnings = new RegistrationResult();

        assertTrue(withWarnings.hasWarnings());
        assertFalse(withEmptyWarnings.hasWarnings());
        assertFalse(noWarnings.hasWarnings());
    }

    @Test
    void testFormattedResult() {
        // 测试成功结果格式化
        RegistrationResult success = RegistrationResult.builder()
            .operationType("CREATE")
            .success(true)
            .productId(12345L)
            .productName("格式化测试产品")
            .masterKey("test-master-key-123456789")
            .build();

        String formatted = success.getFormattedResult();
        assertTrue(formatted.contains("产品注册结果"));
        assertTrue(formatted.contains("CREATE"));
        assertTrue(formatted.contains("✅ 成功"));
        assertTrue(formatted.contains("12345"));
        assertTrue(formatted.contains("格式化测试产品"));
        assertTrue(formatted.contains("test****789")); // 脱敏后的密钥

        // 测试失败结果格式化
        RegistrationResult failure = RegistrationResult.builder()
            .operationType("UPDATE")
            .success(false)
            .errorCode("UPDATE_ERROR")
            .errorMessage("更新失败详细信息")
            .warnings(Arrays.asList("警告1", "警告2"))
            .build();

        String formattedFailure = failure.getFormattedResult();
        assertTrue(formattedFailure.contains("❌ 失败"));
        assertTrue(formattedFailure.contains("UPDATE_ERROR"));
        assertTrue(formattedFailure.contains("更新失败详细信息"));
        assertTrue(formattedFailure.contains("警告信息"));
        assertTrue(formattedFailure.contains("警告1"));
        assertTrue(formattedFailure.contains("警告2"));
    }

    @Test
    void testToString() {
        RegistrationResult result = RegistrationResult.builder()
            .operationType("DELETE")
            .success(true)
            .productId(99999L)
            .productName("ToString测试")
            .status("DELETED")
            .message("删除成功")
            .build();

        String toString = result.toString();
        assertTrue(toString.contains("DELETE"));
        assertTrue(toString.contains("true"));
        assertTrue(toString.contains("99999"));
        assertTrue(toString.contains("ToString测试"));
        assertTrue(toString.contains("DELETED"));
        assertTrue(toString.contains("删除成功"));
    }

    @Test
    void testDefaultValues() {
        RegistrationResult defaultResult = new RegistrationResult();

        assertFalse(defaultResult.isSuccess()); // 默认为失败
        assertTrue(defaultResult.isFailure());
        assertNotNull(defaultResult.getTimestamp());
        assertNull(defaultResult.getOperationId());
        assertNull(defaultResult.getOperationType());
        assertNull(defaultResult.getProductId());
        assertNull(defaultResult.getProductName());
        assertFalse(defaultResult.hasProductInfo());
        assertFalse(defaultResult.hasWarnings());
    }

    @Test
    void testParameterizedConstructor() {
        RegistrationResult paramResult = new RegistrationResult("TEST");

        assertFalse(paramResult.isSuccess());
        assertEquals("TEST", paramResult.getOperationType());
        assertTrue(paramResult.getOperationId().startsWith("TEST_"));
        assertNotNull(paramResult.getTimestamp());
    }
}