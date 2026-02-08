package com.aep.export.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductInfo单元测试
 * TDD第1轮：数据模型测试
 */
public class ProductInfoTest {

    @Test
    public void shouldCreateProductInfo_WithAllRequiredFields() {
        // Given
        Long productId = 16857118L;
        String productName = "RepeaterLTE";
        String masterKey = "521e0d76d0024539a9718abb3e4f64cc";

        // When
        ProductInfo product = ProductInfo.builder()
            .productId(productId)
            .productName(productName)
            .masterKey(masterKey)
            .build();

        // Then
        assertEquals(productId, product.getProductId());
        assertEquals(productName, product.getProductName());
        assertEquals(masterKey, product.getMasterKey());
    }

    @Test
    public void shouldThrowException_WhenProductIdIsNull() {
        // Given & When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductInfo.builder()
                .productName("Test Product")
                .masterKey("test_key")
                .build();
        });

        assertTrue(exception.getMessage().contains("productId"));
    }

    @Test
    public void shouldThrowException_WhenProductNameIsNull() {
        // Given & When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductInfo.builder()
                .productId(12345L)
                .masterKey("test_key")
                .build();
        });

        assertTrue(exception.getMessage().contains("productName"));
    }

    @Test
    public void shouldCreateProductInfo_WithOptionalFields() {
        // Given
        Long productId = 16857118L;
        String productName = "RepeaterLTE";
        String masterKey = "521e0d76d0024539a9718abb3e4f64cc";
        String tenantId = "10433748";
        Integer deviceCount = 892;
        String createTime = "2023-07-12 19:44:33";
        String deviceModel = "RepeaterLTE_BIN";

        // When
        ProductInfo product = ProductInfo.builder()
            .productId(productId)
            .productName(productName)
            .masterKey(masterKey)
            .tenantId(tenantId)
            .deviceCount(deviceCount)
            .createTime(createTime)
            .deviceModel(deviceModel)
            .build();

        // Then
        assertEquals(tenantId, product.getTenantId());
        assertEquals(deviceCount, product.getDeviceCount());
        assertEquals(createTime, product.getCreateTime());
        assertEquals(deviceModel, product.getDeviceModel());
    }

    @Test
    public void shouldSupportEqualsAndHashCode() {
        // Given
        ProductInfo product1 = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("test_key")
            .build();

        ProductInfo product2 = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("test_key")
            .build();

        ProductInfo product3 = ProductInfo.builder()
            .productId(16980130L)
            .productName("RepeaterLTE01")
            .masterKey("different_key")
            .build();

        // When & Then
        assertEquals(product1, product2);
        assertEquals(product1.hashCode(), product2.hashCode());
        assertNotEquals(product1, product3);
        assertNotEquals(product1.hashCode(), product3.hashCode());
    }

    @Test
    public void shouldSupportToString() {
        // Given
        ProductInfo product = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("521e0d76d0024539a9718abb3e4f64cc")
            .deviceCount(892)
            .build();

        // When
        String toString = product.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("16857118"));
        assertTrue(toString.contains("RepeaterLTE"));
        assertTrue(toString.contains("892"));
        // MasterKey should be masked in toString for security
        assertFalse(toString.contains("521e0d76d0024539a9718abb3e4f64cc"));
    }

    @Test
    public void shouldMaskMasterKey_InToString() {
        // Given
        ProductInfo product = ProductInfo.builder()
            .productId(16857118L)
            .productName("RepeaterLTE")
            .masterKey("521e0d76d0024539a9718abb3e4f64cc")
            .build();

        // When
        String toString = product.toString();

        // Then
        assertTrue(toString.contains("521e****64cc")); // 应该显示masked版本
    }
}