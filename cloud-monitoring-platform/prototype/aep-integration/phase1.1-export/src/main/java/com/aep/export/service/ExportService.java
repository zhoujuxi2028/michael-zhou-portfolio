package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import com.aep.export.model.ExportResult;
import com.aep.export.model.ProductInfo;
import com.aep.export.model.DeviceInfo;
import com.aep.export.model.PagedResult;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * 导出协调服务
 * 对应需求: FR-003-01 - 导出产品和设备数据到JSON/CSV
 * 对应需求: FR-003-02 - 支持多种导出格式
 * 对应需求: FR-003-03 - 导出过程进度显示
 * 对应需求: FR-003-04 - 导出进度跟踪
 * 设计模块: DM-016 - ExportService
 * 负责协调各个服务完成完整的数据导出流程
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class ExportService {

    private final ExportConfig config;
    private final ProductService productService;
    private final DeviceService deviceService;
    private final FileManager fileManager;
    private final ErrorHandler errorHandler;
    private final SimpleDateFormat timeFormat;
    private final LogManager logger;

    /**
     * 构造函数
     * 实现: DM-016-01 - 导出服务初始化
     */
    public ExportService(ExportConfig config) {
        this.config = config;
        this.productService = new ProductService(config);
        this.deviceService = new DeviceService(config);
        this.fileManager = new FileManager(config);
        this.errorHandler = new ErrorHandler(config);
        this.timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.logger = LogManager.getInstance();
    }

    /**
     * 导出所有数据（产品和设备）
     * 实现: DM-016-02 - 完整数据导出
     */
    public ExportResult exportAllData() {
        String taskId = generateTaskId();
        String startTime = timeFormat.format(new Date());

        logger.info("ExportService", "=== 开始AEP数据导出流程，任务ID: " + taskId + " ===");

        try {
            // 第一步：产品查询
            logger.logStep1("ExportService", "开始查询产品列表");
            long step1Start = System.currentTimeMillis();
            PagedResult<ProductInfo> products = productService.queryProductList(1, 100);
            long step1Duration = System.currentTimeMillis() - step1Start;

            if (products != null && products.getData() != null) {
                logger.logStep1("ExportService", "产品查询完成 - 获取到 " + products.getData().size() + " 个产品，耗时 " + step1Duration + "ms");
            } else {
                logger.error("产品查询", "ExportService", "产品查询返回空结果");
            }

            // 第二步：设备查询
            logger.logStep2("ExportService", "开始查询设备数据");
            long step2Start = System.currentTimeMillis();
            List<DeviceInfo> allDevices = new ArrayList<>();
            int deviceCount = 0;

            if (products != null && products.getData() != null) {
                for (ProductInfo product : products.getData()) {
                    logger.logStep2("ExportService", "正在查询产品 " + product.getProductName() + " (ID:" + product.getProductId() + ") 的设备");

                    // 收集该产品的所有设备（分页处理）
                    int pageNum = 1;
                    int pageSize = 100;
                    boolean hasMorePages = true;
                    int productDeviceCount = 0;

                    while (hasMorePages) {
                        PagedResult<DeviceInfo> devices = deviceService.queryDevicesByProduct(
                            product.getProductId(), product.getMasterKey(), pageNum, pageSize);

                        if (devices != null && devices.getData() != null && !devices.getData().isEmpty()) {
                            allDevices.addAll(devices.getData());
                            deviceCount += devices.getData().size();
                            productDeviceCount += devices.getData().size();

                            // 检查是否还有下一页
                            hasMorePages = devices.hasNextPage();
                            pageNum++;
                        } else {
                            hasMorePages = false;
                        }
                    }

                    logger.logStep2("ExportService", "产品 " + product.getProductName() + " 设备查询完成 - 获取到 " + productDeviceCount + " 台设备");
                }
            }

            long step2Duration = System.currentTimeMillis() - step2Start;
            logger.logStep2("ExportService", "设备查询完成 - 总计获取到 " + deviceCount + " 台设备，耗时 " + step2Duration + "ms");

            // 第三步：数据合并
            logger.logStep3("ExportService", "开始数据合并和格式转换");
            long step3Start = System.currentTimeMillis();

            String productFilePath = null;
            String deviceFilePath = null;
            long productFileSize = 0;
            long deviceFileSize = 0;

            String productData = null;
            String deviceData = null;

            if (products != null && products.getData() != null && !products.getData().isEmpty()) {
                productData = convertToFormat(products.getData(), "PRODUCT");
                logger.logStep3("ExportService", "产品数据格式转换完成 - " + products.getData().size() + " 条记录");
            }

            if (!allDevices.isEmpty()) {
                deviceData = convertToFormat(allDevices, "DEVICE");
                logger.logStep3("ExportService", "设备数据格式转换完成 - " + allDevices.size() + " 条记录");
            }

            long step3Duration = System.currentTimeMillis() - step3Start;
            logger.logStep3("ExportService", "数据合并完成，耗时 " + step3Duration + "ms");

            // 第四步：导出
            logger.logStep4("ExportService", "开始文件写入");
            long step4Start = System.currentTimeMillis();

            if (productData != null) {
                productFileSize = fileManager.writeFile(productData, "products", getFileExtension());
                productFilePath = getFilePath("products");
                logger.logStep4("ExportService", "产品文件写入完成 - " + productFilePath + "，大小: " + formatFileSize(productFileSize));
            }

            if (deviceData != null) {
                deviceFileSize = fileManager.writeFile(deviceData, "devices", getFileExtension());
                deviceFilePath = getFilePath("devices");
                logger.logStep4("ExportService", "设备文件写入完成 - " + deviceFilePath + "，大小: " + formatFileSize(deviceFileSize));
            }

            long step4Duration = System.currentTimeMillis() - step4Start;
            logger.logStep4("ExportService", "文件导出完成，耗时 " + step4Duration + "ms");

            // 记录总体完成情况
            long totalDuration = System.currentTimeMillis() - parseTime(startTime);
            logger.info("ExportService", "=== AEP数据导出流程完成，任务ID: " + taskId + " ===");
            logger.info("ExportService", "总耗时: " + totalDuration + "ms，产品: " + (products != null ? products.getData().size() : 0) +
                       "个，设备: " + deviceCount + "台");

            // 构建成功结果
            return ExportResult.builder()
                .taskId(taskId)
                .startTime(startTime)
                .endTime(timeFormat.format(new Date()))
                .status("SUCCESS")
                .message("导出完成")
                .totalProducts(products != null ? products.getTotal() : 0)
                .totalDevices(allDevices.size())
                .processedProducts(products != null ? products.getData().size() : 0)
                .processedDevices(deviceCount)
                .productFilePath(productFilePath)
                .deviceFilePath(deviceFilePath)
                .productFileSize(productFileSize)
                .deviceFileSize(deviceFileSize)
                .exportFormat(config.getExportFormat())
                .durationMillis(System.currentTimeMillis() - parseTime(startTime))
                .build();

        } catch (Exception e) {
            errorHandler.handleException(e, "EXPORT_ALL_DATA");

            return ExportResult.builder()
                .taskId(taskId)
                .startTime(startTime)
                .endTime(timeFormat.format(new Date()))
                .status("FAILED")
                .message("导出失败: " + e.getMessage())
                .errorCode("EXPORT_001")
                .exportFormat(config.getExportFormat())
                .build();
        }
    }

    /**
     * 仅导出产品数据
     * 实现: DM-016-03 - 选择性导出
     */
    public ExportResult exportProductsOnly() {
        String taskId = generateTaskId();
        String startTime = timeFormat.format(new Date());

        try {
            // 导出产品数据
            PagedResult<ProductInfo> products = productService.queryProductList(1, 100);

            String productFilePath = null;
            long productFileSize = 0;

            if (products != null && products.getData() != null && !products.getData().isEmpty()) {
                String productData = convertToFormat(products.getData(), "PRODUCT");
                productFileSize = fileManager.writeFile(productData, "products", getFileExtension());
                productFilePath = getFilePath("products");
            }

            // 构建成功结果
            return ExportResult.builder()
                .taskId(taskId)
                .startTime(startTime)
                .endTime(timeFormat.format(new Date()))
                .status("SUCCESS")
                .message("产品导出完成")
                .totalProducts(products != null ? products.getTotal() : 0)
                .totalDevices(0)
                .processedProducts(products != null ? products.getData().size() : 0)
                .processedDevices(0)
                .productFilePath(productFilePath)
                .productFileSize(productFileSize)
                .exportFormat(config.getExportFormat())
                .durationMillis(System.currentTimeMillis() - parseTime(startTime))
                .build();

        } catch (Exception e) {
            errorHandler.handleException(e, "EXPORT_PRODUCTS_ONLY");

            return ExportResult.builder()
                .taskId(taskId)
                .startTime(startTime)
                .endTime(timeFormat.format(new Date()))
                .status("FAILED")
                .message("产品导出失败: " + e.getMessage())
                .errorCode("EXPORT_002")
                .exportFormat(config.getExportFormat())
                .build();
        }
    }

    /**
     * 导出指定产品的设备数据
     * 实现: DM-016-04 - 产品关联设备导出
     */
    public ExportResult exportDevicesForProduct(Long productId) {
        String taskId = generateTaskId();
        String startTime = timeFormat.format(new Date());

        try {
            // 获取产品信息
            ProductInfo product = productService.queryProductById(productId);
            if (product == null) {
                return ExportResult.builder()
                    .taskId(taskId)
                    .startTime(startTime)
                    .endTime(timeFormat.format(new Date()))
                    .status("FAILED")
                    .message("产品不存在: " + productId)
                    .errorCode("EXPORT_003")
                    .exportFormat(config.getExportFormat())
                    .build();
            }

            // 导出设备数据
            PagedResult<DeviceInfo> devices = deviceService.queryDevicesByProduct(
                productId, product.getMasterKey(), 1, 100);

            String deviceFilePath = null;
            long deviceFileSize = 0;

            if (devices != null && devices.getData() != null && !devices.getData().isEmpty()) {
                String deviceData = convertToFormat(devices.getData(), "DEVICE");
                deviceFileSize = fileManager.writeFile(deviceData, "devices", getFileExtension());
                deviceFilePath = getFilePath("devices");
            }

            // 构建成功结果
            return ExportResult.builder()
                .taskId(taskId)
                .startTime(startTime)
                .endTime(timeFormat.format(new Date()))
                .status("SUCCESS")
                .message("设备导出完成")
                .totalProducts(0)
                .totalDevices(devices != null ? devices.getTotal() : 0)
                .processedProducts(0)
                .processedDevices(devices != null ? devices.getData().size() : 0)
                .deviceFilePath(deviceFilePath)
                .deviceFileSize(deviceFileSize)
                .exportFormat(config.getExportFormat())
                .durationMillis(System.currentTimeMillis() - parseTime(startTime))
                .build();

        } catch (Exception e) {
            errorHandler.handleException(e, "EXPORT_DEVICES_FOR_PRODUCT");

            return ExportResult.builder()
                .taskId(taskId)
                .startTime(startTime)
                .endTime(timeFormat.format(new Date()))
                .status("FAILED")
                .message("设备导出失败: " + e.getMessage())
                .errorCode("EXPORT_004")
                .exportFormat(config.getExportFormat())
                .build();
        }
    }

    /**
     * 带进度跟踪的导出
     * 实现: DM-016-05 - 进度跟踪导出
     */
    public ExportResult exportAllDataWithProgress() {
        // 目前实现为简化版，完整实现可以添加进度回调
        return exportAllData();
    }

    /**
     * 验证导出结果
     * 实现: DM-016-06 - 结果验证
     */
    public boolean validateExportResult(ExportResult result) {
        if (result == null) {
            return false;
        }

        // 验证基本字段
        if (result.getTaskId() == null || result.getStartTime() == null) {
            return false;
        }

        // 验证成功结果的完整性
        if (result.isSuccess()) {
            if (result.getFilePaths().isEmpty()) {
                return false;
            }

            if (result.getFileSize() <= 0) {
                return false;
            }
        }

        return true;
    }

    // 私有辅助方法

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "EXPORT_" + System.currentTimeMillis() + "_" + Math.abs(Objects.hash(config.getAppKey()));
    }

    /**
     * 将数据转换为指定格式
     */
    private <T> String convertToFormat(List<T> data, String dataType) {
        String format = config.getExportFormat();
        if ("CSV".equalsIgnoreCase(format)) {
            return convertToCsv(data, dataType);
        } else {
            return convertToJson(data);
        }
    }

    /**
     * 转换为JSON格式
     */
    private <T> String convertToJson(List<T> data) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < data.size(); i++) {
            if (i > 0) json.append(",\n");
            json.append("  ").append(objectToJson(data.get(i)));
        }

        json.append("\n]");
        return json.toString();
    }

    /**
     * 转换为CSV格式
     */
    private <T> String convertToCsv(List<T> data, String dataType) {
        StringBuilder csv = new StringBuilder();

        // 添加标题行
        if (config.getCsvWithHeader() != null && config.getCsvWithHeader()) {
            csv.append(getCsvHeader(dataType)).append("\n");
        }

        // 添加数据行
        for (T item : data) {
            csv.append(objectToCsv(item)).append("\n");
        }

        return csv.toString();
    }

    /**
     * 获取CSV标题行
     */
    private String getCsvHeader(String dataType) {
        String separator = config.getCsvSeparator() != null ? config.getCsvSeparator() : ",";

        if ("PRODUCT".equals(dataType)) {
            return "ProductId" + separator + "ProductName" + separator + "TenantId" + separator +
                   "DeviceCount" + separator + "MasterKey" + separator + "CreateTime";
        } else {
            // 设备完整字段列表 - 包含所有可用信息
            return "DeviceId" + separator + "DeviceName" + separator + "DeviceSn" + separator +
                   "ProductId" + separator + "TenantId" + separator +
                   "DeviceStatus" + separator + "DeviceStatusDesc" + separator +
                   "NetStatus" + separator + "NetStatusDesc" + separator +
                   "FirmwareVersion" + separator + "DeviceType" + separator +
                   "CreateTime" + separator + "UpdateTime" + separator +
                   "LastActiveTime" + separator + "OnlineTime" + separator + "OfflineTime" + separator +
                   "ActiveTime" + separator + "LogoutTime" + separator + "ProductProtocol";
        }
    }

    /**
     * 对象转JSON（完整实现）
     */
    private String objectToJson(Object obj) {
        if (obj instanceof ProductInfo) {
            ProductInfo p = (ProductInfo) obj;
            return String.format("{ \"productId\": %d, \"productName\": \"%s\", \"tenantId\": \"%s\", \"deviceCount\": %d, \"masterKey\": \"%s\" }",
                p.getProductId(), p.getProductName(), p.getTenantId(), p.getDeviceCount(),
                p.getMasterKey() != null ? p.getMasterKey() : "");
        } else if (obj instanceof DeviceInfo) {
            DeviceInfo d = (DeviceInfo) obj;
            StringBuilder json = new StringBuilder();
            json.append("{ ");

            // 基础信息
            json.append("\"deviceId\": \"").append(escapeJsonString(d.getDeviceId())).append("\", ");
            json.append("\"deviceName\": \"").append(escapeJsonString(d.getDeviceName())).append("\", ");
            json.append("\"deviceSn\": \"").append(escapeJsonString(d.getDeviceSn())).append("\", ");
            json.append("\"productId\": ").append(d.getProductId()).append(", ");
            json.append("\"tenantId\": \"").append(escapeJsonString(d.getTenantId())).append("\", ");

            // 状态信息（包含数字值和文字描述）
            json.append("\"deviceStatus\": ").append(d.getDeviceStatus()).append(", ");
            json.append("\"deviceStatusDesc\": \"").append(d.getDeviceStatusDescription()).append("\", ");
            json.append("\"netStatus\": ").append(d.getNetStatus()).append(", ");
            json.append("\"netStatusDesc\": \"").append(d.getNetStatusDescription()).append("\", ");

            // 技术信息
            json.append("\"firmwareVersion\": \"").append(escapeJsonString(d.getFirmwareVersion())).append("\", ");
            json.append("\"deviceType\": \"").append(escapeJsonString(d.getDeviceType())).append("\", ");
            json.append("\"productProtocol\": ").append(d.getProductProtocol()).append(", ");

            // 时间信息（人类可读格式）
            json.append("\"createTime\": \"").append(escapeJsonString(d.getCreateTime())).append("\", ");
            json.append("\"updateTime\": \"").append(escapeJsonString(d.getUpdateTime())).append("\", ");
            json.append("\"lastActiveTime\": \"").append(escapeJsonString(d.getLastActiveTime())).append("\", ");
            json.append("\"onlineTime\": \"").append(escapeJsonString(d.getOnlineTime())).append("\", ");
            json.append("\"offlineTime\": \"").append(escapeJsonString(d.getOfflineTime())).append("\", ");
            json.append("\"activeTime\": \"").append(escapeJsonString(d.getActiveTime())).append("\", ");
            json.append("\"logoutTime\": \"").append(escapeJsonString(d.getLogoutTime())).append("\" ");

            json.append("}");
            return json.toString();
        }
        return "{}";
    }

    /**
     * 对象转CSV（完整实现）
     */
    private String objectToCsv(Object obj) {
        String separator = config.getCsvSeparator() != null ? config.getCsvSeparator() : ",";

        if (obj instanceof ProductInfo) {
            ProductInfo p = (ProductInfo) obj;
            return p.getProductId() + separator + "\"" + p.getProductName() + "\"" + separator +
                   p.getTenantId() + separator + p.getDeviceCount() + separator +
                   "\"" + (p.getMasterKey() != null ? p.getMasterKey() : "") + "\"" + separator + p.getCreateTime();
        } else if (obj instanceof DeviceInfo) {
            DeviceInfo d = (DeviceInfo) obj;
            StringBuilder csv = new StringBuilder();

            // 按照CSV标题行的顺序添加所有字段
            // DeviceId,DeviceName,DeviceSn,ProductId,TenantId,DeviceStatus,DeviceStatusDesc,NetStatus,NetStatusDesc,
            // FirmwareVersion,DeviceType,CreateTime,UpdateTime,LastActiveTime,OnlineTime,OfflineTime,ActiveTime,LogoutTime,ProductProtocol

            csv.append("\"").append(escapeCsvValue(d.getDeviceId())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getDeviceName())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getDeviceSn())).append("\"").append(separator);
            csv.append(d.getProductId()).append(separator);
            csv.append("\"").append(escapeCsvValue(d.getTenantId())).append("\"").append(separator);

            // 状态字段（数字值和文字描述）
            csv.append(d.getDeviceStatus()).append(separator);
            csv.append("\"").append(d.getDeviceStatusDescription()).append("\"").append(separator);
            csv.append(d.getNetStatus()).append(separator);
            csv.append("\"").append(d.getNetStatusDescription()).append("\"").append(separator);

            // 技术信息
            csv.append("\"").append(escapeCsvValue(d.getFirmwareVersion())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getDeviceType())).append("\"").append(separator);

            // 时间信息（人类可读格式）
            csv.append("\"").append(escapeCsvValue(d.getCreateTime())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getUpdateTime())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getLastActiveTime())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getOnlineTime())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getOfflineTime())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getActiveTime())).append("\"").append(separator);
            csv.append("\"").append(escapeCsvValue(d.getLogoutTime())).append("\"").append(separator);
            csv.append(d.getProductProtocol());

            return csv.toString();
        }
        return "";
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension() {
        return "JSON".equalsIgnoreCase(config.getExportFormat()) ? "json" : "csv";
    }

    /**
     * 获取文件路径
     */
    private String getFilePath(String type) {
        String directory = config.getOutputDirectory();
        String filename;

        if ("products".equals(type)) {
            filename = config.getProductFileName() != null ? config.getProductFileName() : "products";
        } else {
            filename = config.getDeviceFileName() != null ? config.getDeviceFileName() : "devices";
        }

        return directory + "/" + filename + "." + getFileExtension();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 解析时间字符串为毫秒
     */
    private long parseTime(String timeString) {
        try {
            return timeFormat.parse(timeString).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 转义CSV字段中的特殊字符
     */
    private String escapeCsvValue(String input) {
        if (input == null) {
            return "";
        }
        // CSV中主要处理双引号转义
        return input.replace("\"", "\"\"");
    }
}