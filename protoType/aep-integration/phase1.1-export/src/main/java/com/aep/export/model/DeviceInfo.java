package com.aep.export.model;

import java.util.Objects;

/**
 * 设备信息数据模型
 * 对应需求: FR-002-03 - 提取设备基本信息
 * 设计模块: DM-005 - DeviceInfo
 * 基于验证阶段的真实AEP设备数据结构
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class DeviceInfo {

    // 必需字段 (DM-005-01)
    private final String deviceId;      // 设备ID (必需)
    private final Long productId;       // 所属产品ID (必需)
    private final String deviceName;    // 设备名称 (通常等于deviceSn)
    private final String deviceSn;      // 设备序列号 (必需)

    // 核心状态字段 (DM-005-01)
    private final Integer deviceStatus; // 设备状态: 0=未激活, 1=已激活
    private final Integer netStatus;    // 网络状态: 1=在线, 2=离线

    // 系统字段 (DM-005-01)
    private final String tenantId;      // 租户ID
    private final Integer productProtocol; // 产品协议

    // 技术字段 (DM-005-01)
    private final String firmwareVersion; // 固件版本
    private final String deviceType;      // 设备类型

    // 时间字段 - 支持多种时间格式 (DM-005-01)
    private final String createTime;      // 创建时间
    private final String updateTime;      // 更新时间
    private final String activeTime;      // 激活时间
    private final String lastActiveTime;  // 最后活跃时间
    private final String logoutTime;      // 注销时间
    private final String onlineTime;      // 上线时间 (时间戳)
    private final String offlineTime;     // 离线时间 (时间戳)

    /**
     * 私有构造函数，强制使用Builder模式
     * 实现: DM-005-02 - Builder模式实现
     */
    private DeviceInfo(Builder builder) {
        this.deviceId = validateRequired(builder.deviceId, "deviceId");
        this.productId = validateRequired(builder.productId, "productId");
        this.deviceName = builder.deviceName;
        this.deviceSn = builder.deviceSn;

        this.deviceStatus = builder.deviceStatus;
        this.netStatus = builder.netStatus;

        this.tenantId = builder.tenantId;
        this.productProtocol = builder.productProtocol;

        this.firmwareVersion = builder.firmwareVersion;
        this.deviceType = builder.deviceType;

        this.createTime = builder.createTime;
        this.updateTime = builder.updateTime;
        this.activeTime = builder.activeTime;
        this.lastActiveTime = builder.lastActiveTime;
        this.logoutTime = builder.logoutTime;
        this.onlineTime = builder.onlineTime;
        this.offlineTime = builder.offlineTime;
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类实现
     * 实现: DM-005-02 - Builder模式实现
     */
    public static class Builder {
        private String deviceId;
        private Long productId;
        private String deviceName;
        private String deviceSn;
        private Integer deviceStatus;
        private Integer netStatus;
        private String tenantId;
        private Integer productProtocol;
        private String firmwareVersion;
        private String deviceType;
        private String createTime;
        private String updateTime;
        private String activeTime;
        private String lastActiveTime;
        private String logoutTime;
        private String onlineTime;
        private String offlineTime;

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder deviceSn(String deviceSn) {
            this.deviceSn = deviceSn;
            return this;
        }

        public Builder deviceStatus(Integer deviceStatus) {
            this.deviceStatus = deviceStatus;
            return this;
        }

        public Builder netStatus(Integer netStatus) {
            this.netStatus = netStatus;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder productProtocol(Integer productProtocol) {
            this.productProtocol = productProtocol;
            return this;
        }

        public Builder firmwareVersion(String firmwareVersion) {
            this.firmwareVersion = firmwareVersion;
            return this;
        }

        public Builder deviceType(String deviceType) {
            this.deviceType = deviceType;
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

        public Builder activeTime(String activeTime) {
            this.activeTime = activeTime;
            return this;
        }

        public Builder lastActiveTime(String lastActiveTime) {
            this.lastActiveTime = lastActiveTime;
            return this;
        }

        public Builder logoutTime(String logoutTime) {
            this.logoutTime = logoutTime;
            return this;
        }

        public Builder onlineTime(String onlineTime) {
            this.onlineTime = onlineTime;
            return this;
        }

        public Builder offlineTime(String offlineTime) {
            this.offlineTime = offlineTime;
            return this;
        }

        public DeviceInfo build() {
            return new DeviceInfo(this);
        }
    }

    /**
     * 字段验证逻辑
     * 实现: DM-005-03 - 字段验证逻辑
     */
    private <T> T validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    // Getter方法
    public String getDeviceId() { return deviceId; }
    public Long getProductId() { return productId; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceSn() { return deviceSn; }
    public Integer getDeviceStatus() { return deviceStatus; }
    public Integer getNetStatus() { return netStatus; }
    public String getTenantId() { return tenantId; }
    public Integer getProductProtocol() { return productProtocol; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public String getDeviceType() { return deviceType; }
    public String getCreateTime() { return createTime; }
    public String getUpdateTime() { return updateTime; }
    public String getActiveTime() { return activeTime; }
    public String getLastActiveTime() { return lastActiveTime; }
    public String getLogoutTime() { return logoutTime; }
    public String getOnlineTime() { return onlineTime; }
    public String getOfflineTime() { return offlineTime; }

    /**
     * 设备状态描述方法
     * 实现: DM-005-06 - 状态解析方法
     */
    public String getDeviceStatusDescription() {
        if (deviceStatus == null) return "Unknown";
        switch (deviceStatus) {
            case 0: return "Inactive";
            case 1: return "Active";
            default: return "Unknown(" + deviceStatus + ")";
        }
    }

    /**
     * 网络状态描述方法
     * 实现: DM-005-06 - 状态解析方法
     */
    public String getNetStatusDescription() {
        if (netStatus == null) return "Unknown";
        switch (netStatus) {
            case 1: return "Online";
            case 2: return "Offline";
            default: return "Unknown(" + netStatus + ")";
        }
    }

    /**
     * equals方法实现
     * 实现: DM-005-04 - equals/hashCode实现
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceInfo that = (DeviceInfo) o;
        return Objects.equals(deviceId, that.deviceId) &&
               Objects.equals(productId, that.productId) &&
               Objects.equals(deviceName, that.deviceName) &&
               Objects.equals(deviceSn, that.deviceSn) &&
               Objects.equals(deviceStatus, that.deviceStatus) &&
               Objects.equals(netStatus, that.netStatus) &&
               Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(firmwareVersion, that.firmwareVersion) &&
               Objects.equals(deviceType, that.deviceType) &&
               Objects.equals(createTime, that.createTime) &&
               Objects.equals(updateTime, that.updateTime);
    }

    /**
     * hashCode方法实现
     * 实现: DM-005-04 - equals/hashCode实现
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceId, productId, deviceName, deviceSn,
                           deviceStatus, netStatus, tenantId, firmwareVersion,
                           deviceType, createTime, updateTime);
    }

    /**
     * toString方法实现
     * 实现: DM-005-05 - toString安全实现
     * 满足需求: NFR-003-02 - 敏感信息脱敏处理
     */
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + maskDeviceId(deviceId) + '\'' +
                ", productId=" + productId +
                ", deviceName='" + deviceName + '\'' +
                ", deviceSn='" + deviceSn + '\'' +
                ", deviceStatus=" + deviceStatus + "(" + getDeviceStatusDescription() + ")" +
                ", netStatus=" + netStatus + "(" + getNetStatusDescription() + ")" +
                ", tenantId='" + tenantId + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", createTime='" + createTime + '\'' +
                ", lastActiveTime='" + lastActiveTime + '\'' +
                '}';
    }

    /**
     * 设备ID脱敏处理
     * 实现: DM-005-05 - toString安全实现
     * 对长设备ID进行适当脱敏
     */
    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() <= 8) {
            return deviceId;
        }
        // 对于长设备ID，保留前4位和后4位
        return deviceId.substring(0, 4) + "****" + deviceId.substring(deviceId.length() - 4);
    }
}