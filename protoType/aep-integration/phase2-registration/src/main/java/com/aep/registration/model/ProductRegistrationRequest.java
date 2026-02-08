package com.aep.registration.model;

import java.util.Objects;

/**
 * 产品注册请求模型
 *
 * 对应AEP API: CreateProductRequest
 * 用于封装产品注册时所需的所有参数
 *
 * @author AEP Registration Tool
 * @version 1.0
 */
public class ProductRegistrationRequest {

    // 基础产品信息
    private String productName;        // 产品名称 (必需)
    private String deviceType;         // 设备类型 (必需)
    private String networkType;        // 网络类型 (可选: NB-IoT, 2G, 3G, 4G, WiFi, Ethernet等)
    private String dataFormat;         // 数据格式 (可选: JSON, XML, Binary等)
    private String description;        // 产品描述

    // 技术参数
    private String deviceModel;        // 设备型号
    private String manufacturer;       // 制造商
    private String protocolType;       // 协议类型 (LwM2M, CoAP, MQTT等)
    private Integer maxDeviceCount;    // 最大设备数量限制

    // 安全配置
    private String encryptionType;     // 加密类型 (可选)
    private Boolean enableSecurity;    // 是否启用安全认证

    // 业务配置
    private String tenantId;           // 租户ID (继承自当前用户)
    private String category;           // 产品类别
    private String[] tags;             // 产品标签

    // 系统配置
    private Boolean autoCreateDevice;  // 是否自动创建设备
    private Integer dataRetentionDays; // 数据保留天数

    // 构造函数
    public ProductRegistrationRequest() {}

    public ProductRegistrationRequest(String productName, String deviceType) {
        this.productName = productName;
        this.deviceType = deviceType;
    }

    // Builder模式支持
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductRegistrationRequest request = new ProductRegistrationRequest();

        public Builder productName(String productName) {
            request.productName = productName;
            return this;
        }

        public Builder deviceType(String deviceType) {
            request.deviceType = deviceType;
            return this;
        }

        public Builder networkType(String networkType) {
            request.networkType = networkType;
            return this;
        }

        public Builder dataFormat(String dataFormat) {
            request.dataFormat = dataFormat;
            return this;
        }

        public Builder description(String description) {
            request.description = description;
            return this;
        }

        public Builder deviceModel(String deviceModel) {
            request.deviceModel = deviceModel;
            return this;
        }

        public Builder manufacturer(String manufacturer) {
            request.manufacturer = manufacturer;
            return this;
        }

        public Builder protocolType(String protocolType) {
            request.protocolType = protocolType;
            return this;
        }

        public Builder maxDeviceCount(Integer maxDeviceCount) {
            request.maxDeviceCount = maxDeviceCount;
            return this;
        }

        public Builder enableSecurity(Boolean enableSecurity) {
            request.enableSecurity = enableSecurity;
            return this;
        }

        public Builder tenantId(String tenantId) {
            request.tenantId = tenantId;
            return this;
        }

        public Builder category(String category) {
            request.category = category;
            return this;
        }

        public Builder tags(String[] tags) {
            request.tags = tags;
            return this;
        }

        public Builder autoCreateDevice(Boolean autoCreateDevice) {
            request.autoCreateDevice = autoCreateDevice;
            return this;
        }

        public Builder dataRetentionDays(Integer dataRetentionDays) {
            request.dataRetentionDays = dataRetentionDays;
            return this;
        }

        public ProductRegistrationRequest build() {
            return request;
        }
    }

    // 数据验证
    public boolean isValid() {
        return getValidationError() == null;
    }

    public String getValidationError() {
        if (productName == null || productName.trim().isEmpty()) {
            return "产品名称不能为空";
        }
        if (deviceType == null || deviceType.trim().isEmpty()) {
            return "设备类型不能为空";
        }
        if (productName.length() > 50) {
            return "产品名称不能超过50个字符";
        }
        return null;
    }

    // Getter和Setter方法
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }

    public String getDataFormat() { return dataFormat; }
    public void setDataFormat(String dataFormat) { this.dataFormat = dataFormat; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getProtocolType() { return protocolType; }
    public void setProtocolType(String protocolType) { this.protocolType = protocolType; }

    public Integer getMaxDeviceCount() { return maxDeviceCount; }
    public void setMaxDeviceCount(Integer maxDeviceCount) { this.maxDeviceCount = maxDeviceCount; }

    public String getEncryptionType() { return encryptionType; }
    public void setEncryptionType(String encryptionType) { this.encryptionType = encryptionType; }

    public Boolean getEnableSecurity() { return enableSecurity; }
    public void setEnableSecurity(Boolean enableSecurity) { this.enableSecurity = enableSecurity; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public Boolean getAutoCreateDevice() { return autoCreateDevice; }
    public void setAutoCreateDevice(Boolean autoCreateDevice) { this.autoCreateDevice = autoCreateDevice; }

    public Integer getDataRetentionDays() { return dataRetentionDays; }
    public void setDataRetentionDays(Integer dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }

    // toString方法
    @Override
    public String toString() {
        return "ProductRegistrationRequest{" +
                "productName='" + productName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", networkType='" + networkType + '\'' +
                ", dataFormat='" + dataFormat + '\'' +
                ", description='" + description + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", protocolType='" + protocolType + '\'' +
                ", maxDeviceCount=" + maxDeviceCount +
                ", enableSecurity=" + enableSecurity +
                ", tenantId='" + tenantId + '\'' +
                ", category='" + category + '\'' +
                ", autoCreateDevice=" + autoCreateDevice +
                ", dataRetentionDays=" + dataRetentionDays +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductRegistrationRequest that = (ProductRegistrationRequest) o;
        return Objects.equals(productName, that.productName) &&
               Objects.equals(deviceType, that.deviceType) &&
               Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName, deviceType, tenantId);
    }
}