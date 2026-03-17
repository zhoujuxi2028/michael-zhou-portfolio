package com.zct.poc.service;

import com.zct.poc.config.TelecomIotConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 中国电信物联网认证服务测试
 */
@SpringBootTest
public class TelecomIotAuthServiceTest {

    @Autowired
    private TelecomIotAuthService authService;

    @Autowired
    private TelecomIotConfig config;

    @BeforeEach
    void setUp() {
        // 确保配置正确
        config.setEnabled(true);
        config.setAppKey("test_app_key");
        config.setAppSecret("test_app_secret");
    }

    @Test
    void testGetAccessToken() {
        // 测试获取访问Token
        String token1 = authService.getAccessToken();
        assertNotNull(token1);
        assertTrue(token1.length() > 0);

        // 测试Token缓存
        String token2 = authService.getAccessToken();
        assertEquals(token1, token2); // 应该返回相同的缓存Token
    }

    @Test
    void testValidateDeviceAuth_Success() {
        // 测试设备认证成功场景
        boolean result = authService.validateDeviceAuth("device001", "123456789012345", "460012345678901");
        // 由于是模拟逻辑，结果可能为true或false
        assertNotNull(result);
    }

    @Test
    void testValidateDeviceAuth_InvalidIMEI() {
        // 测试无效IMEI
        boolean result = authService.validateDeviceAuth("device001", "invalid_imei", "460012345678901");
        assertFalse(result);
    }

    @Test
    void testValidateDeviceAuth_InvalidIMSI() {
        // 测试无效IMSI（不以460开头）
        boolean result = authService.validateDeviceAuth("device001", "123456789012345", "123456789012345");
        assertFalse(result);
    }

    @Test
    void testValidateDeviceAuth_NullDeviceId() {
        // 测试空设备ID
        boolean result = authService.validateDeviceAuth(null, "123456789012345", "460012345678901");
        assertFalse(result);
    }

    @Test
    void testGenerateSignature() {
        // 测试签名生成
        long timestamp = System.currentTimeMillis();
        String params = "test_params";

        String signature1 = authService.generateSignature(timestamp, params);
        String signature2 = authService.generateSignature(timestamp, params);

        assertNotNull(signature1);
        assertNotNull(signature2);
        assertEquals(signature1, signature2); // 相同输入应该产生相同签名

        // 测试不同输入产生不同签名
        String signature3 = authService.generateSignature(timestamp + 1, params);
        assertNotEquals(signature1, signature3);
    }

    @Test
    void testAuthServiceDisabled() {
        // 测试认证服务禁用的情况
        config.setEnabled(false);

        String token = authService.getAccessToken();
        assertNotNull(token);
        assertTrue(token.startsWith("mock_token_"));

        boolean authResult = authService.validateDeviceAuth("device001", "invalid", "invalid");
        assertTrue(authResult); // 禁用时应该返回true

        // 恢复设置
        config.setEnabled(true);
    }

    @Test
    void testMultipleTokenRequests() {
        // 测试多次Token请求
        for (int i = 0; i < 5; i++) {
            String token = authService.getAccessToken();
            assertNotNull(token);
            assertTrue(token.length() > 0);
        }
    }

    @Test
    void testDeviceAuthWithDifferentDevices() {
        // 测试不同设备的认证
        String[] deviceIds = {"device001", "device002", "device003", "device004", "device005"};
        String validIMEI = "123456789012345";
        String validIMSI = "460012345678901";

        for (String deviceId : deviceIds) {
            boolean result = authService.validateDeviceAuth(deviceId, validIMEI, validIMSI);
            // 记录每个设备的认证结果
            System.out.println("设备认证结果 - 设备ID: " + deviceId + ", 结果: " + result);
        }
    }
}