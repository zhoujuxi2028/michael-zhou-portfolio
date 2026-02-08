package com.aep.export.model;

import java.util.Objects;

/**
 * 产品信息数据模型
 * 对应需求: FR-001-02 - 提取产品基本信息
 * 设计模块: DM-001 - ProductInfo
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class ProductInfo {

    // 必需字段
    private final Long productId;           // DM-001-01: 产品ID
    private final String productName;       // DM-001-01: 产品名称

    // 核心字段
    private final String masterKey;         // DM-001-01: 主密钥 (从apiKey提取)

    // 可选字段
    private final String tenantId;          // DM-001-01: 租户ID
    private final Integer deviceCount;      // DM-001-01: 设备数量
    private final String createTime;        // DM-001-01: 创建时间
    private final String updateTime;        // DM-001-01: 更新时间
    private final String deviceModel;       // DM-001-01: 设备型号

    /**
     * 私有构造函数，强制使用Builder模式
     * 实现: DM-001-02 - Builder模式实现
     */
    private ProductInfo(Builder builder) {
        this.productId = validateRequired(builder.productId, "productId");
        this.productName = validateRequired(builder.productName, "productName");
        this.masterKey = builder.masterKey;
        this.tenantId = builder.tenantId;
        this.deviceCount = builder.deviceCount;
        this.createTime = builder.createTime;
        this.updateTime = builder.updateTime;
        this.deviceModel = builder.deviceModel;
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类实现
     * 实现: DM-001-02 - Builder模式实现
     */
    public static class Builder {
        private Long productId;
        private String productName;
        private String masterKey;
        private String tenantId;
        private Integer deviceCount;
        private String createTime;
        private String updateTime;
        private String deviceModel;

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder masterKey(String masterKey) {
            this.masterKey = masterKey;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder deviceCount(Integer deviceCount) {
            this.deviceCount = deviceCount;
            return this;
        }

        public Builder createTime(String createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder updateTime(String updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public Builder deviceModel(String deviceModel) {
            this.deviceModel = deviceModel;
            return this;
        }

        public ProductInfo build() {
            return new ProductInfo(this);
        }
    }

    /**
     * 字段验证逻辑
     * 实现: DM-001-03 - 字段验证逻辑
     */
    private <T> T validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    // Getter方法
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getMasterKey() { return masterKey; }
    public String getTenantId() { return tenantId; }
    public Integer getDeviceCount() { return deviceCount; }
    public String getCreateTime() { return createTime; }
    public String getUpdateTime() { return updateTime; }
    public String getDeviceModel() { return deviceModel; }

    /**
     * equals方法实现
     * 实现: DM-001-04 - equals/hashCode实现
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductInfo that = (ProductInfo) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(productName, that.productName) &&
               Objects.equals(masterKey, that.masterKey) &&
               Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(deviceCount, that.deviceCount) &&
               Objects.equals(createTime, that.createTime) &&
               Objects.equals(updateTime, that.updateTime) &&
               Objects.equals(deviceModel, that.deviceModel);
    }

    /**
     * hashCode方法实现
     * 实现: DM-001-04 - equals/hashCode实现
     */
    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, masterKey, tenantId,
                           deviceCount, createTime, updateTime, deviceModel);
    }

    /**
     * toString方法实现，敏感信息脱敏
     * 实现: DM-001-05 - toString安全实现
     * 满足需求: NFR-003-02 - 敏感信息脱敏处理
     */
    @Override
    public String toString() {
        return "ProductInfo{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", masterKey='" + maskSensitiveValue(masterKey) + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", deviceCount=" + deviceCount +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                '}';
    }

    /**
     * 敏感信息脱敏处理
     * 实现: DM-001-05 - toString安全实现
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}