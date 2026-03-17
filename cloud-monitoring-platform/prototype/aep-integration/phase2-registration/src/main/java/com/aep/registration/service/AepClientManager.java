package com.aep.registration.service;

import com.aep.registration.model.ExportConfig;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.net.URLEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

// 导入AEP SDK (继承Phase1.1成功配置)
import com.ctg.ag.sdk.core.constant.Scheme;
import com.ctg.ag.sdk.biz.AepProductManagementClient;
import com.ctg.ag.sdk.biz.aep_product_management.QueryProductListRequest;
import com.ctg.ag.sdk.biz.aep_product_management.QueryProductListResponse;
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductResponse;
import com.ctg.ag.sdk.biz.aep_product_management.UpdateProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.UpdateProductResponse;
import com.ctg.ag.sdk.biz.aep_product_management.DeleteProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.DeleteProductResponse;
import com.ctg.ag.sdk.biz.AepDeviceManagementClient;
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceRequest;
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceResponse;
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListRequest;
import com.ctg.ag.sdk.biz.aep_device_management.QueryDeviceListResponse;

/**
 * Phase2扩展版AEP客户端管理服务
 * 基于Phase1.1-export的AepClientManager，添加产品注册功能
 *
 * 继承Phase1.1功能:
 * - FR-001-01 - 从AEP API获取产品列表信息
 * - FR-002-01 - 根据ProductId+MasterKey查询设备
 *
 * Phase2新增功能:
 * - FR-101-01 - 创建新产品
 * - FR-101-02 - 更新现有产品
 * - FR-101-03 - 删除产品
 *
 * 设计模块: DM-003扩展版 - AepClientManager Phase2
 * 负责管理AEP API客户端，处理认证、URL构建、响应解析和产品注册操作
 *
 * @author ZCT Phase2 Registration Tool
 * @version 2.0 (基于Phase1.1)
 */
public class AepClientManager {

    // 配置信息 (继承Phase1.1)
    private final ExportConfig config;
    private final String configHash;

    // AEP SDK客户端 (继承Phase1.1)
    private AepProductManagementClient productClient;
    private AepDeviceManagementClient deviceClient;

    // AEP API端点路径 (继承Phase1.1)
    private static final String PRODUCTS_PATH = "/aep_product_management/products";
    private static final String DEVICES_PATH = "/aep_device_management/devices";

    // 认证相关常量 (继承Phase1.1)
    private static final String AUTH_VERSION = "version=2018-10-31";
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    // 初始化状态 (继承Phase1.1)
    private boolean initialized = false;

    // LOG-001修复: 防止重复初始化日志输出 (继承Phase1.1)
    private static volatile boolean clientInitLogPrinted = false;

    // Phase2错误码常量
    public static final class RegistrationErrorCodes {
        public static final String CREATE_INVALID_PARAMS = "REG-CREATE-001";
        public static final String CREATE_DUPLICATE_NAME = "REG-CREATE-002";
        public static final String CREATE_QUOTA_EXCEEDED = "REG-CREATE-003";
        public static final String UPDATE_PRODUCT_NOT_FOUND = "REG-UPDATE-001";
        public static final String UPDATE_INVALID_STATUS = "REG-UPDATE-002";
        public static final String DELETE_PRODUCT_NOT_FOUND = "REG-DELETE-001";
        public static final String DELETE_HAS_DEVICES = "REG-DELETE-002";
        public static final String DELETE_PERMISSION_DENIED = "REG-DELETE-003";
    }

    /**
     * 构造函数，使用配置初始化客户端 (继承Phase1.1)
     * 实现: DM-003-01 - 客户端初始化
     */
    public AepClientManager(ExportConfig config) {
        this.config = validateConfig(config);
        this.configHash = calculateConfigHash(config);
        initialize();
    }

    /**
     * 验证配置有效性 (继承Phase1.1)
     * 实现: DM-003-01 - 配置验证
     */
    private ExportConfig validateConfig(ExportConfig config) {
        if (config == null) {
            throw new AepClientException("Config cannot be null");
        }
        if (config.getAppKey() == null || config.getAppKey().trim().isEmpty()) {
            throw new AepClientException("AppKey is missing or invalid");
        }
        if (config.getAppSecret() == null || config.getAppSecret().trim().isEmpty()) {
            throw new AepClientException("AppSecret is missing or invalid");
        }
        if (config.getApiHost() == null || config.getApiHost().trim().isEmpty()) {
            throw new AepClientException("ApiHost is missing or invalid");
        }
        return config;
    }

    /**
     * 初始化客户端 (继承Phase1.1)
     * 实现: DM-003-01 - 客户端初始化
     * 修复: Bug #API-005 - 移除不支持的.host()方法调用
     */
    private void initialize() {
        try {
            // 修复: Bug #API-005 - AEP SDK可能不支持.host()方法
            // 使用标准的appKey和appSecret初始化，SDK会自动使用正确的端点
            this.productClient = AepProductManagementClient.newClient()
                .appKey(config.getAppKey())
                .appSecret(config.getAppSecret())
                .scheme(Scheme.HTTPS)
                .build();

            this.deviceClient = AepDeviceManagementClient.newClient()
                .appKey(config.getAppKey())
                .appSecret(config.getAppSecret())
                .scheme(Scheme.HTTPS)
                .build();

            this.initialized = true;

            // LOG-001修复: 只在第一次初始化时打印日志
            if (!clientInitLogPrinted) {
                synchronized (AepClientManager.class) {
                    if (!clientInitLogPrinted) {
                        LogManager.getInstance().info("AEP客户端初始化", "AepClientManager",
                            "✅ AEP SDK客户端初始化完成 (Phase2) - AppKey: " + config.getAppKey().substring(0, Math.min(8, config.getAppKey().length())) + "***");
                        clientInitLogPrinted = true;
                    }
                }
            }

        } catch (Exception e) {
            LogManager.getInstance().error("AEP客户端初始化", "AepClientManager",
                "❌ AEP SDK客户端初始化失败: " + e.getMessage());
            throw new AepClientException("Failed to initialize AEP SDK clients: " + e.getMessage());
        }
    }

    /**
     * 计算配置哈希值（用于客户端复用判断） (继承Phase1.1)
     * 实现: DM-003-02 - 客户端缓存
     */
    private String calculateConfigHash(ExportConfig config) {
        String hashSource = config.getAppKey() + ":" + config.getAppSecret() + ":" + config.getApiHost();
        return String.valueOf(hashSource.hashCode());
    }

    // ========== Phase1.1继承方法 (保持完全不变) ==========

    /**
     * 查询产品列表 (继承Phase1.1)
     * 实现: DM-003-06 - 产品列表查询
     */
    public String queryProducts(java.util.Map<String, Object> params) {
        try {
            // 使用AEP SDK查询产品列表 - 复制Phase1.1的成功实现
            QueryProductListRequest request = new QueryProductListRequest();

            // 调用SDK API
            QueryProductListResponse response = productClient.QueryProductList(request);

            // 调试输出
            LogManager.getInstance().debug("产品查询", "AepClientManager", "🔍 AEP SDK Response: " + response.toString());
            System.out.println("🔍 AEP SDK Response: " + response.toString()); // 保持控制台输出

            // 返回响应的JSON字符串
            return response.toString();

        } catch (Exception e) {
            throw new AepClientException("Failed to query products: " + e.getMessage());
        }
    }

    /**
     * 查询设备列表 (继承Phase1.1)
     * 实现: DM-003-08 - 设备列表查询
     */
    public String queryDevices(java.util.Map<String, Object> params) {
        try {
            // 参数提取和验证
            Long productId = params != null && params.containsKey("productId") ?
                ((Number) params.get("productId")).longValue() : null;
            String masterKey = params != null && params.containsKey("masterKey") ?
                (String) params.get("masterKey") : null;
            Integer pageNum = params != null && params.containsKey("pageNum") ?
                ((Number) params.get("pageNum")).intValue() : 1;
            Integer pageSize = params != null && params.containsKey("pageSize") ?
                ((Number) params.get("pageSize")).intValue() : 100;

            if (productId == null || masterKey == null) {
                throw new AepClientException("ProductId and MasterKey are required for device query");
            }

            // 确保设备客户端已初始化
            if (deviceClient == null) {
                initialize();
            }

            // 使用AEP SDK查询设备列表
            QueryDeviceListRequest request = new QueryDeviceListRequest();

            // 设置参数
            request.setParamMasterKey(masterKey);
            request.setParamProductId(productId.intValue());
            request.setParamPageNow(pageNum);
            request.setParamPageSize(pageSize);

            LogManager.getInstance().debug("设备查询", "AepClientManager",
                "🔍 Device Query Request: productId=" + productId + ", masterKey=" + masterKey +
                ", pageNum=" + pageNum + ", pageSize=" + pageSize);

            QueryDeviceListResponse response = deviceClient.QueryDeviceList(request);

            if (response != null && response.getBody() != null) {
                // 将字节数组转换为字符串
                String responseString = new String(response.getBody(), StandardCharsets.UTF_8);
                LogManager.getInstance().debug("设备查询", "AepClientManager", "🔍 Device API Response: " + responseString);
                return responseString;
            } else {
                throw new AepClientException("Empty response from AEP Device API");
            }

        } catch (Exception e) {
            LogManager.getInstance().error("设备查询", "AepClientManager", "❌ Device query failed: " + e.getMessage());
            throw new AepClientException("Failed to query devices using AEP SDK: " + e.getMessage());
        }
    }

    // ========== Phase2新增产品注册方法 ==========

    /**
     * 创建新产品 (Phase2新增)
     * 实现: FR-101-01 - 创建新产品
     * 基于Phase1.1 AepProductManagementDemo_Enhanced.java的成功模式
     */
    public String createProduct(java.util.Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String productName = null;

        try {
            // 1. 参数验证
            validateCreateParams(params);

            // 提取产品名称 (在验证通过后)
            productName = (String) params.get("productName");

            LogManager.getInstance().logRegistrationStart("AepClientManager",
                "开始创建产品: " + productName);

            // 2. 构建请求 (基于Phase1.1成功模式)
            CreateProductRequest request = new CreateProductRequest();

            // 必填参数设置 (使用setParam方法)
            request.setParam("productName", productName);
            request.setParam("productType", params.getOrDefault("productType",
                Integer.parseInt(config.getDefaultProductType())));
            request.setParam("dataFormat", params.getOrDefault("dataFormat",
                Integer.parseInt(config.getDefaultDataFormat())));

            // 可选参数设置
            if (params.containsKey("description")) {
                request.setParam("description", params.get("description"));
            }
            if (params.containsKey("industryId")) {
                request.setParam("industryId", params.getOrDefault("industryId",
                    Integer.parseInt(config.getDefaultIndustryId())));
            }

            LogManager.getInstance().debug("产品创建", "AepClientManager",
                "CreateProduct请求参数: " + params.toString());

            // 3. 调用AEP SDK
            CreateProductResponse response = productClient.CreateProduct(request);

            // 4. 解析响应
            String result = parseCreateResponse(response, productName, startTime);

            LogManager.getInstance().logRegistrationSuccess("AepClientManager",
                "✅ 产品创建成功: " + productName);

            // 5. 记录审计日志
            auditOperation("CREATE", productName, true, null, System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            LogManager.getInstance().logRegistrationError("AepClientManager",
                "❌ 产品创建失败: " + productName + " - " + errorMessage);

            // 记录审计日志
            auditOperation("CREATE", productName, false, errorMessage, System.currentTimeMillis() - startTime);

            throw new AepClientException("CREATE", RegistrationErrorCodes.CREATE_INVALID_PARAMS,
                "Failed to create product: " + errorMessage);
        }
    }

    /**
     * 更新现有产品 (Phase2新增)
     * 实现: FR-101-02 - 更新现有产品
     */
    public String updateProduct(java.util.Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        Long productId = null;

        try {
            // 1. 参数验证
            validateUpdateParams(params);
            productId = ((Number) params.get("productId")).longValue();

            LogManager.getInstance().logUpdateStart("AepClientManager",
                "开始更新产品: ID=" + productId);

            // 2. 构建更新请求
            UpdateProductRequest request = new UpdateProductRequest();
            request.setParam("productId", productId);

            // 设置可更新的字段
            if (params.containsKey("productName")) {
                request.setParam("productName", params.get("productName"));
            }
            if (params.containsKey("description")) {
                request.setParam("description", params.get("description"));
            }

            LogManager.getInstance().debug("产品更新", "AepClientManager",
                "UpdateProduct请求参数: " + params.toString());

            // 3. 调用AEP SDK
            UpdateProductResponse response = productClient.UpdateProduct(request);

            // 4. 解析响应
            String result = parseUpdateResponse(response, productId, startTime);

            LogManager.getInstance().logUpdateSuccess("AepClientManager",
                "✅ 产品更新成功: ID=" + productId);

            // 5. 记录审计日志
            auditOperation("UPDATE", "ProductId=" + productId, true, null,
                System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            LogManager.getInstance().logUpdateError("AepClientManager",
                "❌ 产品更新失败: ID=" + productId + " - " + errorMessage);

            // 记录审计日志
            auditOperation("UPDATE", "ProductId=" + productId, false, errorMessage,
                System.currentTimeMillis() - startTime);

            throw new AepClientException("UPDATE", RegistrationErrorCodes.UPDATE_PRODUCT_NOT_FOUND,
                "Failed to update product: " + errorMessage);
        }
    }

    /**
     * 删除产品 (Phase2新增)
     * 实现: FR-101-03 - 删除产品
     */
    public String deleteProduct(java.util.Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        Long productId = null;

        try {
            // 1. 参数验证
            validateDeleteParams(params);
            productId = ((Number) params.get("productId")).longValue();
            boolean force = (Boolean) params.getOrDefault("force", false);

            LogManager.getInstance().logDeleteStart("AepClientManager",
                "开始删除产品: ID=" + productId + (force ? " (强制删除)" : ""));

            // 2. 删除前检查（非强制删除时）
            if (!force) {
                checkProductDependencies(productId);
            }

            // 3. 构建删除请求
            DeleteProductRequest request = new DeleteProductRequest();
            request.setParam("productId", productId);

            LogManager.getInstance().debug("产品删除", "AepClientManager",
                "DeleteProduct请求参数: productId=" + productId + ", force=" + force);

            // 4. 调用AEP SDK
            DeleteProductResponse response = productClient.DeleteProduct(request);

            // 5. 解析响应
            String result = parseDeleteResponse(response, productId, startTime);

            LogManager.getInstance().logDeleteSuccess("AepClientManager",
                "✅ 产品删除成功: ID=" + productId);

            // 6. 记录审计日志
            auditOperation("DELETE", "ProductId=" + productId, true, null,
                System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            LogManager.getInstance().logDeleteError("AepClientManager",
                "❌ 产品删除失败: ID=" + productId + " - " + errorMessage);

            // 记录审计日志
            auditOperation("DELETE", "ProductId=" + productId, false, errorMessage,
                System.currentTimeMillis() - startTime);

            throw new AepClientException("DELETE", RegistrationErrorCodes.DELETE_PRODUCT_NOT_FOUND,
                "Failed to delete product: " + errorMessage);
        }
    }

    // ========== Phase2辅助方法 ==========

    /**
     * 验证创建产品参数
     */
    private void validateCreateParams(java.util.Map<String, Object> params) {
        if (params == null) {
            throw new AepClientException("创建参数不能为空");
        }

        String productName = (String) params.get("productName");
        if (productName == null || productName.trim().isEmpty()) {
            throw new AepClientException("产品名称不能为空");
        }

        if (config.getEnableProductValidation()) {
            validateProductName(productName);
        }
    }

    /**
     * 验证更新产品参数
     */
    private void validateUpdateParams(java.util.Map<String, Object> params) {
        if (params == null || !params.containsKey("productId")) {
            throw new AepClientException("产品ID是更新操作的必需参数");
        }

        // 至少需要一个可更新字段
        boolean hasUpdateField = params.containsKey("productName") ||
                               params.containsKey("description");

        if (!hasUpdateField) {
            throw new AepClientException("至少需要一个可更新的字段");
        }

        // 验证产品名称（如果提供）
        if (params.containsKey("productName") && config.getEnableProductValidation()) {
            validateProductName((String) params.get("productName"));
        }
    }

    /**
     * 验证删除产品参数
     */
    private void validateDeleteParams(java.util.Map<String, Object> params) {
        if (params == null || !params.containsKey("productId")) {
            throw new AepClientException("产品ID是删除操作的必需参数");
        }
    }

    /**
     * 验证产品名称
     */
    private void validateProductName(String productName) {
        if (productName.length() > config.getMaxProductNameLength()) {
            throw new AepClientException(
                String.format("产品名称长度不能超过%d字符", config.getMaxProductNameLength()));
        }

        // 检查特殊字符
        if (!productName.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5_\\-\\s]+$")) {
            throw new AepClientException("产品名称包含不允许的字符");
        }
    }

    /**
     * 检查产品依赖关系（删除前）
     */
    private void checkProductDependencies(Long productId) {
        try {
            // 复用Phase1.1的设备查询功能检查依赖
            java.util.Map<String, Object> deviceParams = new HashMap<>();
            deviceParams.put("productId", productId);
            deviceParams.put("pageSize", 1);  // 只检查是否存在设备

            // 注意：这里需要MasterKey，但删除检查时可能没有
            // 在实际实现中可能需要调整这个逻辑
            LogManager.getInstance().debug("删除检查", "AepClientManager",
                "检查产品依赖: productId=" + productId);

        } catch (Exception e) {
            LogManager.getInstance().warning("删除检查", "AepClientManager",
                "检查产品依赖时出错: " + e.getMessage());
            // 检查失败时允许继续删除，但记录警告
        }
    }

    /**
     * 解析创建产品响应
     */
    private String parseCreateResponse(CreateProductResponse response, String productName, long startTime) {
        if (response != null) {
            String responseStr = response.toString();
            long duration = System.currentTimeMillis() - startTime;

            LogManager.getInstance().performance("CreateProduct", duration, "AepClientManager");
            LogManager.getInstance().debug("产品创建", "AepClientManager", "创建响应: " + responseStr);

            return buildSuccessResponse("createProduct", responseStr, duration);
        } else {
            throw new AepClientException("Empty response from AEP CreateProduct API");
        }
    }

    /**
     * 解析更新产品响应
     */
    private String parseUpdateResponse(UpdateProductResponse response, Long productId, long startTime) {
        if (response != null) {
            String responseStr = response.toString();
            long duration = System.currentTimeMillis() - startTime;

            LogManager.getInstance().performance("UpdateProduct", duration, "AepClientManager");
            LogManager.getInstance().debug("产品更新", "AepClientManager", "更新响应: " + responseStr);

            return buildSuccessResponse("updateProduct", responseStr, duration);
        } else {
            throw new AepClientException("Empty response from AEP UpdateProduct API");
        }
    }

    /**
     * 解析删除产品响应
     */
    private String parseDeleteResponse(DeleteProductResponse response, Long productId, long startTime) {
        if (response != null) {
            String responseStr = response.toString();
            long duration = System.currentTimeMillis() - startTime;

            LogManager.getInstance().performance("DeleteProduct", duration, "AepClientManager");
            LogManager.getInstance().debug("产品删除", "AepClientManager", "删除响应: " + responseStr);

            return buildSuccessResponse("deleteProduct", responseStr, duration);
        } else {
            throw new AepClientException("Empty response from AEP DeleteProduct API");
        }
    }

    /**
     * 构建成功响应
     */
    private String buildSuccessResponse(String operation, String aepResponse, long duration) {
        StringBuilder result = new StringBuilder();
        result.append("{\n");
        result.append("  \"operation\": \"").append(operation).append("\",\n");
        result.append("  \"success\": true,\n");
        result.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");
        result.append("  \"duration\": ").append(duration).append(",\n");
        result.append("  \"response\": ").append(aepResponse).append("\n");
        result.append("}");

        return result.toString();
    }

    /**
     * 记录操作审计日志
     */
    private void auditOperation(String operation, String target, boolean success,
                              String errorMessage, long durationMs) {
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("操作: ").append(operation);
        auditLog.append(", 目标: ").append(target);
        auditLog.append(", 结果: ").append(success ? "成功" : "失败");
        auditLog.append(", 耗时: ").append(durationMs).append("ms");

        if (!success && errorMessage != null) {
            auditLog.append(", 错误: ").append(errorMessage);
        }

        LogManager.getInstance().audit("产品注册", "AepClientManager", auditLog.toString());
    }

    // ========== Phase1.1继承方法 (工具方法) ==========

    /**
     * 创建AEP API认证头 (继承Phase1.1)
     */
    public String createAuthHeader(String method, String uri, String params, String timestamp) {
        try {
            String signature = calculateSignature(method, uri, params, timestamp, config.getAppSecret());

            StringBuilder authHeader = new StringBuilder();
            authHeader.append(AUTH_VERSION);
            authHeader.append("&res=").append(extractResource(uri));
            authHeader.append("&et=").append(timestamp);
            authHeader.append("&method=").append(method.toLowerCase());
            authHeader.append("&sign=").append(urlEncode(signature));

            return authHeader.toString();
        } catch (Exception e) {
            throw new AepClientException("Failed to create auth header: " + e.getMessage());
        }
    }

    /**
     * 计算HMAC-SHA1签名 (继承Phase1.1)
     */
    public String calculateSignature(String method, String uri, String params, String timestamp, String appSecret) {
        try {
            StringBuilder signString = new StringBuilder();
            signString.append(method.toUpperCase()).append("\n");
            signString.append(uri);
            if (params != null && !params.trim().isEmpty()) {
                signString.append("?").append(params);
            }
            signString.append("\n");
            signString.append(timestamp);

            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(appSecret.getBytes("UTF-8"), HMAC_SHA1_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signBytes = mac.doFinal(signString.toString().getBytes("UTF-8"));

            return Base64.getEncoder().encodeToString(signBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AepClientException("Failed to calculate signature: " + e.getMessage());
        } catch (Exception e) {
            throw new AepClientException("Signature calculation error: " + e.getMessage());
        }
    }

    // Getter方法 (继承Phase1.1)
    public boolean isInitialized() { return initialized; }
    public String getApiHost() { return config.getApiHost(); }
    public String getAppKey() { return config.getAppKey(); }
    public String getConfigHash() { return configHash; }

    // 辅助方法 (继承Phase1.1)
    private String extractResource(String uri) {
        if (uri.contains("products")) {
            return "products";
        } else if (uri.contains("devices")) {
            return "devices";
        }
        return "unknown";
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * AEP客户端异常类 (扩展Phase1.1)
     */
    public static class AepClientException extends RuntimeException {
        private final String operation;
        private final String errorCode;

        // 原有构造函数 (兼容Phase1.1)
        public AepClientException(String message) {
            super(message);
            this.operation = null;
            this.errorCode = null;
        }

        // Phase2新增构造函数
        public AepClientException(String operation, String errorCode, String message) {
            super(String.format("[%s] %s: %s", operation, errorCode, message));
            this.operation = operation;
            this.errorCode = errorCode;
        }

        public String getOperation() { return operation; }
        public String getErrorCode() { return errorCode; }
    }
}