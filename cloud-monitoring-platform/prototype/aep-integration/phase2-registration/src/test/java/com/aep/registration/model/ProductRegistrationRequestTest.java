package com.aep.registration.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductRegistrationRequest 测试类
 */
class ProductRegistrationRequestTest {

    private ProductRegistrationRequest request;

    @BeforeEach
    void setUp() {
        request = new ProductRegistrationRequest();
    }

    @Test
    void testValidRequest() {
        // 构建有效的注册请求
        request.setProductName("测试产品");
        request.setDeviceType("SENSOR");

        assertTrue(request.isValid());
        assertNull(request.getValidationError());
    }

    @Test
    void testInvalidRequest_MissingProductName() {
        // 缺少产品名称
        request.setDeviceType("SENSOR");

        assertFalse(request.isValid());
        assertEquals("产品名称不能为空", request.getValidationError());
    }

    @Test
    void testInvalidRequest_MissingDeviceType() {
        // 缺少设备类型
        request.setProductName("测试产品");

        assertFalse(request.isValid());
        assertEquals("设备类型不能为空", request.getValidationError());
    }

    @Test
    void testInvalidRequest_ProductNameTooLong() {
        // 产品名称过长
        request.setProductName("a".repeat(51)); // 超过50个字符
        request.setDeviceType("SENSOR");

        assertFalse(request.isValid());
        assertEquals("产品名称不能超过50个字符", request.getValidationError());
    }

    @Test
    void testBuilderPattern() {
        // 测试Builder模式
        ProductRegistrationRequest built = ProductRegistrationRequest.builder()
            .productName("Builder测试产品")
            .deviceType("GATEWAY")
            .networkType("NB-IOT")
            .dataFormat("JSON")
            .description("通过Builder创建的产品")
            .deviceModel("TEST-MODEL-001")
            .manufacturer("测试厂商")
            .protocolType("CoAP")
            .maxDeviceCount(100)
            .enableSecurity(true)
            .autoCreateDevice(true)
            .dataRetentionDays(30)
            .build();

        assertTrue(built.isValid());
        assertEquals("Builder测试产品", built.getProductName());
        assertEquals("GATEWAY", built.getDeviceType());
        assertEquals("NB-IOT", built.getNetworkType());
        assertEquals("JSON", built.getDataFormat());
        assertEquals("通过Builder创建的产品", built.getDescription());
        assertEquals("TEST-MODEL-001", built.getDeviceModel());
        assertEquals("测试厂商", built.getManufacturer());
        assertEquals("CoAP", built.getProtocolType());
        assertEquals(100, built.getMaxDeviceCount());
        assertTrue(built.getEnableSecurity());
        assertTrue(built.getAutoCreateDevice());
        assertEquals(30, built.getDataRetentionDays());
    }

    @Test
    void testEqualsAndHashCode() {
        // 测试equals和hashCode
        ProductRegistrationRequest request1 = ProductRegistrationRequest.builder()
            .productName("产品A")
            .deviceType("SENSOR")
            .tenantId("tenant1")
            .build();

        ProductRegistrationRequest request2 = ProductRegistrationRequest.builder()
            .productName("产品A")
            .deviceType("SENSOR")
            .tenantId("tenant1")
            .build();

        ProductRegistrationRequest request3 = ProductRegistrationRequest.builder()
            .productName("产品B")
            .deviceType("SENSOR")
            .tenantId("tenant1")
            .build();

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        request.setProductName("ToString测试产品");
        request.setDeviceType("MODULE");
        request.setDescription("测试toString方法");

        String toString = request.toString();
        assertTrue(toString.contains("ToString测试产品"));
        assertTrue(toString.contains("MODULE"));
        assertTrue(toString.contains("测试toString方法"));
    }

    @Test
    void testDefaultConstructor() {
        ProductRegistrationRequest defaultRequest = new ProductRegistrationRequest();

        assertFalse(defaultRequest.isValid());
        assertNull(defaultRequest.getProductName());
        assertNull(defaultRequest.getDeviceType());
    }

    @Test
    void testParameterizedConstructor() {
        ProductRegistrationRequest paramRequest = new ProductRegistrationRequest("参数化产品", "TERMINAL");

        assertTrue(paramRequest.isValid());
        assertEquals("参数化产品", paramRequest.getProductName());
        assertEquals("TERMINAL", paramRequest.getDeviceType());
    }

    @Test
    void testValidationEdgeCases() {
        // 测试边界情况

        // 空白字符串
        request.setProductName("  ");
        request.setDeviceType("SENSOR");
        assertFalse(request.isValid());

        request.setProductName("正常产品");
        request.setDeviceType("  ");
        assertFalse(request.isValid());

        // 正好50个字符
        request.setProductName("a".repeat(50));
        request.setDeviceType("SENSOR");
        assertTrue(request.isValid());

        // null值
        request.setProductName(null);
        request.setDeviceType("SENSOR");
        assertFalse(request.isValid());
    }
}