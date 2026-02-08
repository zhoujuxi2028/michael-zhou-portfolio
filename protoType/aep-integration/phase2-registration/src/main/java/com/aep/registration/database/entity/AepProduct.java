package com.aep.registration.database.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * AEP产品实体类
 *
 * 对应数据库表: aep_products
 *
 * 功能：
 * - 封装AEP产品的完整信息
 * - 支持产品的CRUD操作
 * - 提供业务逻辑验证
 * - 支持安全的敏感数据处理
 *
 * @author AEP Integration Team
 * @version 1.0.0
 * @since 2026-01-25
 */
public class AepProduct {

    // 主键ID
    private Long id;

    // AEP平台产品ID
    private Long productId;

    // 产品名称
    private String productName;

    // 设备类型
    private String deviceType;

    // 网络类型
    private String networkType;

    // 数据格式 (1=JSON, 2=二进制)
    private Integer dataFormat;

    // 行业ID
    private Integer industryId;

    // 产品描述
    private String description;

    // 设备型号
    private String deviceModel;

    // 制造商
    private String manufacturer;

    // 协议类型
    private String protocolType;

    // 最大设备数量
    private Integer maxDeviceCount;

    // 是否启用安全认证
    private Boolean enableSecurity;

    // 是否自动创建设备
    private Boolean autoCreateDevice;

    // AEP产品主密钥(加密存储)
    private String masterKey;

    // 状态 (ACTIVE/INACTIVE/DELETED)
    private String status;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    // 创建者
    private String createdBy;

    /**
     * 默认构造函数
     */
    public AepProduct() {
        this.dataFormat = 1; // 默认JSON格式
        this.status = "ACTIVE"; // 默认激活状态
        this.enableSecurity = false; // 默认不启用安全认证
        this.autoCreateDevice = false; // 默认不自动创建设备
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 构造函数 - 用于创建新产品
     */
    public AepProduct(String productName, String deviceType, Integer dataFormat) {
        this();
        this.productName = productName;
        this.deviceType = deviceType;
        this.dataFormat = dataFormat;
    }

    /**
     * 建造者模式 - 用于构建复杂的产品对象
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AepProduct product = new AepProduct();

        public Builder productName(String productName) {
            product.productName = productName;
            return this;
        }

        public Builder deviceType(String deviceType) {
            product.deviceType = deviceType;
            return this;
        }

        public Builder networkType(String networkType) {
            product.networkType = networkType;
            return this;
        }

        public Builder dataFormat(Integer dataFormat) {
            product.dataFormat = dataFormat;
            return this;
        }

        public Builder industryId(Integer industryId) {
            product.industryId = industryId;
            return this;
        }

        public Builder description(String description) {
            product.description = description;
            return this;
        }

        public Builder deviceModel(String deviceModel) {
            product.deviceModel = deviceModel;
            return this;
        }

        public Builder manufacturer(String manufacturer) {
            product.manufacturer = manufacturer;
            return this;
        }

        public Builder protocolType(String protocolType) {
            product.protocolType = protocolType;
            return this;
        }

        public Builder maxDeviceCount(Integer maxDeviceCount) {
            product.maxDeviceCount = maxDeviceCount;
            return this;
        }

        public Builder enableSecurity(Boolean enableSecurity) {
            product.enableSecurity = enableSecurity;
            return this;
        }

        public Builder autoCreateDevice(Boolean autoCreateDevice) {
            product.autoCreateDevice = autoCreateDevice;
            return this;
        }

        public Builder createdBy(String createdBy) {
            product.createdBy = createdBy;
            return this;
        }

        public AepProduct build() {
            // 基本验证
            Objects.requireNonNull(product.productName, "产品名称不能为空");
            Objects.requireNonNull(product.deviceType, "设备类型不能为空");
            Objects.requireNonNull(product.dataFormat, "数据格式不能为空");

            return product;
        }
    }

    /**
     * 业务方法 - 检查产品是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * 业务方法 - 激活产品
     */
    public void activate() {
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 停用产品
     */
    public void deactivate() {
        this.status = "INACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 删除产品（软删除）
     */
    public void markDeleted() {
        this.status = "DELETED";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 更新产品信息时自动更新时间戳
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 检查设备类型是否有效
     */
    public boolean isValidDeviceType() {
        return deviceType != null &&
               (deviceType.equals("SENSOR") ||
                deviceType.equals("GATEWAY") ||
                deviceType.equals("DEVICE") ||
                deviceType.equals("TERMINAL") ||
                deviceType.equals("MODULE"));
    }

    /**
     * 业务方法 - 获取脱敏后的主密钥（用于日志输出）
     */
    public String getMaskedMasterKey() {
        if (masterKey == null || masterKey.length() <= 8) {
            return "****";
        }
        return masterKey.substring(0, 4) + "****" + masterKey.substring(masterKey.length() - 4);
    }

    // ==================== Getter和Setter方法 ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
        touch();
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
        touch();
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
        touch();
    }

    public Integer getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(Integer dataFormat) {
        this.dataFormat = dataFormat;
        touch();
    }

    public Integer getIndustryId() {
        return industryId;
    }

    public void setIndustryId(Integer industryId) {
        this.industryId = industryId;
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
        touch();
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        touch();
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
        touch();
    }

    public Integer getMaxDeviceCount() {
        return maxDeviceCount;
    }

    public void setMaxDeviceCount(Integer maxDeviceCount) {
        this.maxDeviceCount = maxDeviceCount;
        touch();
    }

    public Boolean getEnableSecurity() {
        return enableSecurity;
    }

    public void setEnableSecurity(Boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
        touch();
    }

    public Boolean getAutoCreateDevice() {
        return autoCreateDevice;
    }

    public void setAutoCreateDevice(Boolean autoCreateDevice) {
        this.autoCreateDevice = autoCreateDevice;
        touch();
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
        touch();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // ==================== Object方法重写 ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AepProduct product = (AepProduct) o;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "AepProduct{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", networkType='" + networkType + '\'' +
                ", dataFormat=" + dataFormat +
                ", industryId=" + industryId +
                ", description='" + description + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", protocolType='" + protocolType + '\'' +
                ", maxDeviceCount=" + maxDeviceCount +
                ", enableSecurity=" + enableSecurity +
                ", autoCreateDevice=" + autoCreateDevice +
                ", masterKey='" + getMaskedMasterKey() + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }

    /**
     * 用于JSON序列化的安全toString（隐藏敏感信息）
     */
    public String toSafeString() {
        return "AepProduct{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", networkType='" + networkType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}