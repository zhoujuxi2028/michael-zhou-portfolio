package com.zct.poc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的产品状态查询示例
 * 参考vendor-b系统实现最简单的查询功能
 *
 * 使用方法：
 * java -cp target/classes com.zct.poc.SimpleQueryExample
 */
public class SimpleQueryExample {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("简单产品状态查询示例");
        System.out.println("参考vendor-b系统实现");
        System.out.println("==========================================");

        // 创建查询实例
        SimpleQueryExample example = new SimpleQueryExample();

        // 测试不同的lbsId
        String[] testLbsIds = {
            "station001",
            "station002",
            "station003",
            "test_device_001",
            "base_station_999"
        };

        for (String lbsId : testLbsIds) {
            System.out.println("\n查询设备: " + lbsId);
            ProductStatus status = example.queryProductStatus(lbsId);
            example.printStatus(status);
        }

        System.out.println("\n==========================================");
        System.out.println("测试完成!");
        System.out.println("==========================================");
    }

    /**
     * 查询产品状态
     * 参考vendor-b系统的TDeviceService.listByLbsId()方法
     */
    public ProductStatus queryProductStatus(String lbsId) {
        System.out.println("[DEBUG] 开始查询产品状态，lbsId: " + lbsId);

        // 1. 参数校验
        if (lbsId == null || lbsId.trim().isEmpty()) {
            System.out.println("[ERROR] lbsId不能为空");
            return null;
        }

        // 2. 模拟数据库查询
        System.out.println("[DEBUG] 模拟查询设备基础信息...");
        ProductStatus status = new ProductStatus();
        status.setId("device_" + lbsId.hashCode());
        status.setLbsId(lbsId);
        status.setDeviceName("测试设备_" + lbsId);
        status.setModel("ZC-001");
        status.setDeviceType("4G设备");
        status.setProjectId("project_001");
        status.setCompanyId("company_001");

        // 3. 计算设备状态 - 参考vendor-b系统的状态计算逻辑
        System.out.println("[DEBUG] 计算设备状态码...");
        String statusCode = calculateDeviceStatus(lbsId);
        status.setStatus(statusCode);
        status.setStatusDescription(parseStatusDescription(statusCode));

        // 4. 判断在线状态 - 参考vendor-b系统的在线判断逻辑
        boolean online = isDeviceOnline(statusCode);
        status.setOnlineStatus(online ? 1 : 0);

        // 5. 设置时间信息
        status.setLastReportTime(new Date(System.currentTimeMillis() - 30000)); // 30秒前
        status.setCreateTime(new Date());

        // 6. 补充项目和公司信息
        System.out.println("[DEBUG] 补充项目和公司信息...");
        status.setProjectName("测试项目_" + status.getProjectId());
        status.setCompanyName("测试公司_" + status.getCompanyId());

        // 7. 设置升级状态
        int upgradeStatus = Math.abs(lbsId.hashCode()) % 4;
        switch (upgradeStatus) {
            case 0: status.setUpgradeStatus("0"); status.setUpgradeStatusName("未升级"); break;
            case 1: status.setUpgradeStatus("1"); status.setUpgradeStatusName("升级中"); break;
            case 2: status.setUpgradeStatus("2"); status.setUpgradeStatusName("升级成功"); break;
            default: status.setUpgradeStatus("3"); status.setUpgradeStatusName("升级失败"); break;
        }

        System.out.println("[INFO] 产品状态查询完成");
        return status;
    }

    /**
     * 计算设备状态码
     * 参考vendor-b系统的状态码计算逻辑
     */
    private String calculateDeviceStatus(String lbsId) {
        // 模拟vendor-b系统中的11位状态码计算
        int hash = Math.abs(lbsId.hashCode()) % 3;

        switch (hash) {
            case 0: return "11111111111"; // 全部正常
            case 1: return "11110111111"; // 部分模块异常
            default: return "00000000000"; // 设备离线
        }
    }

    /**
     * 解析状态描述
     */
    private String parseStatusDescription(String statusCode) {
        if ("11111111111".equals(statusCode)) {
            return "设备运行正常";
        } else if ("00000000000".equals(statusCode)) {
            return "设备离线";
        } else {
            return "设备部分功能异常";
        }
    }

    /**
     * 判断设备是否在线
     * 参考vendor-b系统的在线判断逻辑
     */
    private boolean isDeviceOnline(String statusCode) {
        // 如果第一位或第二位是0，则认为离线
        return !(statusCode.substring(0, 1).equals("0") || statusCode.substring(1, 2).equals("0"));
    }

    /**
     * 打印状态信息
     */
    private void printStatus(ProductStatus status) {
        if (status == null) {
            System.out.println("  查询结果: 设备不存在或查询失败");
            return;
        }

        System.out.println("  设备ID: " + status.getId());
        System.out.println("  设备名称: " + status.getDeviceName());
        System.out.println("  设备型号: " + status.getModel());
        System.out.println("  设备类型: " + status.getDeviceType());
        System.out.println("  状态码: " + status.getStatus());
        System.out.println("  状态描述: " + status.getStatusDescription());
        System.out.println("  在线状态: " + (status.getOnlineStatus() == 1 ? "在线" : "离线"));
        System.out.println("  升级状态: " + status.getUpgradeStatusName());
        System.out.println("  项目名称: " + status.getProjectName());
        System.out.println("  公司名称: " + status.getCompanyName());
        System.out.println("  最后上报: " + status.getLastReportTime());
    }

    /**
     * 简单的产品状态类
     */
    public static class ProductStatus {
        private String id;
        private String lbsId;
        private String deviceName;
        private String model;
        private String deviceType;
        private String projectId;
        private String projectName;
        private String companyId;
        private String companyName;
        private String status;
        private String statusDescription;
        private Integer onlineStatus;
        private String upgradeStatus;
        private String upgradeStatusName;
        private Date lastReportTime;
        private Date createTime;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getLbsId() { return lbsId; }
        public void setLbsId(String lbsId) { this.lbsId = lbsId; }

        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStatusDescription() { return statusDescription; }
        public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

        public Integer getOnlineStatus() { return onlineStatus; }
        public void setOnlineStatus(Integer onlineStatus) { this.onlineStatus = onlineStatus; }

        public String getUpgradeStatus() { return upgradeStatus; }
        public void setUpgradeStatus(String upgradeStatus) { this.upgradeStatus = upgradeStatus; }

        public String getUpgradeStatusName() { return upgradeStatusName; }
        public void setUpgradeStatusName(String upgradeStatusName) { this.upgradeStatusName = upgradeStatusName; }

        public Date getLastReportTime() { return lastReportTime; }
        public void setLastReportTime(Date lastReportTime) { this.lastReportTime = lastReportTime; }

        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
    }

    /**
     * 中国电信物联网认证示例
     */
    public static class TelecomAuthExample {

        /**
         * 模拟中国电信物联网认证
         */
        public static boolean authenticateDevice(String deviceId, String imei, String imsi) {
            System.out.println("[AUTH] 开始设备认证...");
            System.out.println("[AUTH] 设备ID: " + deviceId);
            System.out.println("[AUTH] IMEI: " + imei);
            System.out.println("[AUTH] IMSI: " + imsi);

            // IMEI格式验证 (15位数字)
            if (imei != null && !imei.matches("\\d{15}")) {
                System.out.println("[AUTH] IMEI格式无效");
                return false;
            }

            // IMSI格式验证 (以460开头的15位数字，中国电信)
            if (imsi != null && !imsi.matches("460\\d{12}")) {
                System.out.println("[AUTH] IMSI格式无效或不属于中国电信");
                return false;
            }

            // 模拟认证逻辑
            boolean authResult = Math.abs(deviceId.hashCode()) % 10 < 8; // 80%成功率

            System.out.println("[AUTH] 设备认证结果: " + (authResult ? "成功" : "失败"));
            return authResult;
        }

        /**
         * 生成访问Token
         */
        public static String generateAccessToken(String appKey, String appSecret) {
            System.out.println("[AUTH] 生成访问Token...");

            long timestamp = System.currentTimeMillis();
            String tokenData = appKey + timestamp + appSecret;
            int tokenHash = Math.abs(tokenData.hashCode());

            String token = "telecom_token_" + timestamp + "_" + tokenHash;
            System.out.println("[AUTH] Token生成成功: " + token.substring(0, 20) + "...");

            return token;
        }
    }
}