package com.zct.poc.service;

import com.zct.poc.config.TelecomIotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中国电信物联网认证服务
 * 实现标准的电信物联网平台认证机制
 */
@Service
@Slf4j
public class TelecomIotAuthService {

    @Autowired
    private TelecomIotConfig config;

    /**
     * Token缓存 (简化实现，生产环境建议使用Redis)
     */
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    /**
     * 获取访问Token
     * 参考电信物联网平台的认证流程
     *
     * @return AccessToken
     */
    public String getAccessToken() {
        log.debug("开始获取电信物联网平台访问Token");

        if (!config.isEnabled()) {
            log.warn("电信物联网认证未启用，返回模拟Token");
            return "mock_token_" + System.currentTimeMillis();
        }

        String cacheKey = config.getAppKey();
        TokenInfo tokenInfo = tokenCache.get(cacheKey);

        // 检查Token是否有效
        if (tokenInfo != null && !isTokenExpired(tokenInfo)) {
            log.debug("使用缓存的Token");
            return tokenInfo.getAccessToken();
        }

        // 获取新Token
        log.info("Token已过期或不存在，开始获取新Token");
        return requestNewToken(cacheKey);
    }

    /**
     * 验证设备认证信息
     * 包括IMEI、IMSI等设备标识
     *
     * @param deviceId 设备ID
     * @param imei     设备IMEI
     * @param imsi     设备IMSI
     * @return 认证结果
     */
    public boolean validateDeviceAuth(String deviceId, String imei, String imsi) {
        log.debug("开始验证设备认证信息，设备ID: {}, IMEI: {}, IMSI: {}", deviceId, imei, imsi);

        try {
            // 参数校验
            if (deviceId == null || deviceId.trim().isEmpty()) {
                log.error("设备ID不能为空");
                return false;
            }

            if (!config.isEnabled()) {
                log.warn("电信物联网认证未启用，返回模拟认证成功");
                return true;
            }

            // IMEI格式验证 (15位数字)
            if (imei != null && !validateIMEI(imei)) {
                log.error("IMEI格式无效: {}", imei);
                return false;
            }

            // IMSI格式验证 (15位数字，以460开头表示中国)
            if (imsi != null && !validateIMSI(imsi)) {
                log.error("IMSI格式无效: {}", imsi);
                return false;
            }

            // 模拟与电信平台的认证交互
            boolean authResult = simulateDeviceAuthentication(deviceId, imei, imsi);

            log.info("设备认证完成，设备ID: {}, 认证结果: {}", deviceId, authResult);
            return authResult;

        } catch (Exception e) {
            log.error("设备认证过程中发生异常，设备ID: {}", deviceId, e);
            return false;
        }
    }

    /**
     * 生成API请求签名
     * 参考电信物联网平台的签名算法
     *
     * @param timestamp 时间戳
     * @param params    请求参数
     * @return 签名字符串
     */
    public String generateSignature(long timestamp, String params) {
        log.debug("生成API请求签名，时间戳: {}", timestamp);

        try {
            // 构造签名原文：AppKey + timestamp + params + AppSecret
            String signText = config.getAppKey() + timestamp + params + config.getAppSecret();

            log.debug("签名原文: {}", maskSensitiveInfo(signText));

            // 使用SHA-256生成签名
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signText.getBytes(StandardCharsets.UTF_8));

            // Base64编码
            String signature = Base64.getEncoder().encodeToString(hash);

            log.debug("生成的签名: {}", signature.substring(0, Math.min(10, signature.length())) + "...");
            return signature;

        } catch (Exception e) {
            log.error("生成签名时发生异常", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }

    /**
     * 请求新的访问Token
     */
    private String requestNewToken(String cacheKey) {
        try {
            log.info("开始向电信物联网平台请求新Token");

            // 生成时间戳
            long timestamp = System.currentTimeMillis();

            // 生成签名
            String signature = generateSignature(timestamp, "");

            // 模拟HTTP请求到电信平台
            // 实际实现中需要使用HttpClient发送真实的HTTP请求
            String newToken = simulateTokenRequest(timestamp, signature);

            // 缓存新Token
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setAccessToken(newToken);
            tokenInfo.setExpireTime(System.currentTimeMillis() + config.getTokenExpireSeconds() * 1000L);
            tokenInfo.setCreateTime(System.currentTimeMillis());

            tokenCache.put(cacheKey, tokenInfo);

            log.info("新Token获取成功，有效期: {}秒", config.getTokenExpireSeconds());
            return newToken;

        } catch (Exception e) {
            log.error("获取新Token失败", e);
            throw new RuntimeException("Token获取失败", e);
        }
    }

    /**
     * 模拟Token请求（生产环境中替换为真实的HTTP请求）
     */
    private String simulateTokenRequest(long timestamp, String signature) {
        log.debug("模拟Token请求，时间戳: {}, 签名前缀: {}",
                timestamp, signature.substring(0, Math.min(10, signature.length())));

        // 模拟网络延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 生成模拟Token
        return "telecom_token_" + System.currentTimeMillis() + "_" + config.getAppKey().hashCode();
    }

    /**
     * 模拟设备认证（生产环境中替换为真实的设备认证调用）
     */
    private boolean simulateDeviceAuthentication(String deviceId, String imei, String imsi) {
        log.debug("模拟设备认证过程");

        // 模拟网络延迟
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 基于设备ID生成认证结果（模拟逻辑）
        int hash = Math.abs(deviceId.hashCode()) % 10;
        boolean result = hash < 8; // 80%成功率

        log.debug("设备认证模拟结果: {}", result);
        return result;
    }

    /**
     * 验证IMEI格式
     */
    private boolean validateIMEI(String imei) {
        return imei != null && imei.matches("\\d{15}");
    }

    /**
     * 验证IMSI格式
     */
    private boolean validateIMSI(String imsi) {
        return imsi != null && imsi.matches("460\\d{12}"); // 中国电信IMSI以460开头
    }

    /**
     * 检查Token是否过期
     */
    private boolean isTokenExpired(TokenInfo tokenInfo) {
        return System.currentTimeMillis() > tokenInfo.getExpireTime();
    }

    /**
     * 遮蔽敏感信息用于日志输出
     */
    private String maskSensitiveInfo(String text) {
        if (text == null || text.length() < 10) {
            return "***";
        }
        return text.substring(0, 5) + "***" + text.substring(text.length() - 5);
    }

    /**
     * Token信息内部类
     */
    private static class TokenInfo {
        private String accessToken;
        private long expireTime;
        private long createTime;

        // Getters and Setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }

        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }
    }
}