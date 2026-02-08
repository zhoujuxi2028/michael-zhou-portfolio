package com.zct.poc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 中国电信物联网认证配置
 * 参考中国电信物联网平台的标准认证机制
 */
@Configuration
@ConfigurationProperties(prefix = "app.telecom-iot")
@Data
public class TelecomIotConfig {

    /**
     * 是否启用电信物联网认证
     */
    private boolean enabled = false;

    /**
     * 应用密钥
     */
    private String appKey;

    /**
     * 应用秘钥
     */
    private String appSecret;

    /**
     * 平台URL
     */
    private String platformUrl;

    /**
     * 超时时间（毫秒）
     */
    private int timeout = 5000;

    /**
     * Token有效期（秒）
     */
    private int tokenExpireSeconds = 3600;

    /**
     * 重试次数
     */
    private int retryTimes = 3;
}