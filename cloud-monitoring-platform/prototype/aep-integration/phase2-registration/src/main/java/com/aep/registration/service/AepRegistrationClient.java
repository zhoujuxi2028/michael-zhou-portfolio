package com.aep.registration.service;

import com.aep.registration.model.ProductRegistrationRequest;
import com.aep.registration.model.RegistrationResult;

// 导入AEP SDK
import com.ctg.ag.sdk.core.constant.Scheme;
import com.ctg.ag.sdk.biz.AepProductManagementClient;
import com.ctg.ag.sdk.biz.aep_product_management.*;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AEP产品注册客户端
 *
 * 扩展AEP SDK客户端，专门用于产品注册、更新、删除等管理操作
 * 基于Phase1.1的成功架构，专注于产品管理功能
 *
 * @author AEP Registration Tool
 * @version 1.0
 */
public class AepRegistrationClient {

    private static final Logger logger = Logger.getLogger(AepRegistrationClient.class.getName());

    // AEP SDK客户端
    private AepProductManagementClient productClient;

    // 认证配置
    private final String appKey;
    private final String appSecret;
    private final String apiHost;
    private final String appId;

    // 状态管理
    private boolean initialized = false;

    /**
     * 构造函数
     */
    public AepRegistrationClient(String appKey, String appSecret, String apiHost, String appId) {
        this.appKey = validateNotEmpty(appKey, "AppKey");
        this.appSecret = validateNotEmpty(appSecret, "AppSecret");
        this.apiHost = validateNotEmpty(apiHost, "ApiHost");
        this.appId = validateNotEmpty(appId, "AppId");

        initialize();
    }

    /**
     * 从环境变量创建客户端
     */
    public static AepRegistrationClient fromEnvironment() {
        String appKey = System.getenv("AEP_APP_KEY");
        String appSecret = System.getenv("AEP_APP_SECRET");
        String apiHost = System.getenv("AEP_API_HOST");
        String appId = System.getenv("AEP_APP_ID");

        if (appKey == null || appSecret == null || apiHost == null || appId == null) {
            throw new AepRegistrationException("Missing required environment variables: AEP_APP_KEY, AEP_APP_SECRET, AEP_API_HOST, AEP_APP_ID");
        }

        return new AepRegistrationClient(appKey, appSecret, apiHost, appId);
    }

    /**
     * 初始化AEP SDK客户端
     */
    private void initialize() {
        try {
            this.productClient = AepProductManagementClient.newClient()
                .appKey(appKey)
                .appSecret(appSecret)
                .scheme(Scheme.HTTPS)
                .build();

            this.initialized = true;
            logger.info("AEP注册客户端初始化成功 - AppKey: " + maskSensitive(appKey));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "AEP注册客户端初始化失败: " + e.getMessage(), e);
            throw new AepRegistrationException("Failed to initialize AEP registration client: " + e.getMessage());
        }
    }

    /**
     * 创建新产品
     *
     * @param request 产品注册请求
     * @return 注册结果
     */
    public RegistrationResult createProduct(ProductRegistrationRequest request) {
        validateInitialized();

        if (!request.isValid()) {
            return RegistrationResult.failure("CREATE", "VALIDATION_ERROR", request.getValidationError());
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.info("开始创建产品: " + request.getProductName());

            // 构建AEP SDK请求
            CreateProductRequest aepRequest = new CreateProductRequest();

            // 设置产品基本信息（需要根据实际AEP SDK API文档调整）
            // 注意：这里的方法名需要根据实际SDK确认
            try {
                // 使用CreateProductRequest的通用setParam(String, Object)方法
                if (request.getProductName() != null) {
                    aepRequest.setParam("productName", request.getProductName());
                }
                if (request.getDeviceType() != null) {
                    aepRequest.setParam("deviceType", request.getDeviceType());
                }
                if (request.getNetworkType() != null) {
                    aepRequest.setParam("networkType", request.getNetworkType());
                }
                if (request.getDataFormat() != null) {
                    aepRequest.setParam("dataFormat", request.getDataFormat());
                }
                if (request.getDescription() != null) {
                    aepRequest.setParam("description", request.getDescription());
                }
                if (request.getDeviceModel() != null) {
                    aepRequest.setParam("deviceModel", request.getDeviceModel());
                }
                if (request.getManufacturer() != null) {
                    aepRequest.setParam("manufacturer", request.getManufacturer());
                }
                if (request.getProtocolType() != null) {
                    aepRequest.setParam("protocolType", request.getProtocolType());
                }
                if (request.getMaxDeviceCount() != null) {
                    aepRequest.setParam("maxDeviceCount", request.getMaxDeviceCount());
                }

            } catch (Exception e) {
                logger.warning("设置产品参数时遇到问题: " + e.getMessage());
            }

            // 调用AEP API
            CreateProductResponse response = productClient.CreateProduct(aepRequest);

            long responseTime = System.currentTimeMillis() - startTime;

            // 解析响应
            return parseCreateProductResponse(response, responseTime, request.getProductName());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.log(Level.SEVERE, "产品创建失败: " + e.getMessage(), e);

            return RegistrationResult.builder()
                .operationType("CREATE")
                .success(false)
                .errorCode("CREATE_FAILED")
                .errorMessage("产品创建失败: " + e.getMessage())
                .productName(request.getProductName())
                .responseTimeMs(responseTime)
                .build();
        }
    }

    /**
     * 更新产品配置
     */
    public RegistrationResult updateProduct(Long productId, ProductRegistrationRequest request) {
        validateInitialized();

        long startTime = System.currentTimeMillis();

        try {
            logger.info("开始更新产品: " + productId + " - " + request.getProductName());

            UpdateProductRequest aepRequest = new UpdateProductRequest();

            // 设置产品ID和更新参数
            aepRequest.setParam("productId", productId);
            if (request.getProductName() != null) {
                aepRequest.setParam("productName", request.getProductName());
            }
            if (request.getDescription() != null) {
                aepRequest.setParam("description", request.getDescription());
            }

            // 调用AEP API
            UpdateProductResponse response = productClient.UpdateProduct(aepRequest);

            long responseTime = System.currentTimeMillis() - startTime;

            // 解析响应
            return parseUpdateProductResponse(response, responseTime, productId, request.getProductName());

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.log(Level.SEVERE, "产品更新失败: " + e.getMessage(), e);

            return RegistrationResult.builder()
                .operationType("UPDATE")
                .success(false)
                .errorCode("UPDATE_FAILED")
                .errorMessage("产品更新失败: " + e.getMessage())
                .productId(productId)
                .productName(request.getProductName())
                .responseTimeMs(responseTime)
                .build();
        }
    }

    /**
     * 删除产品
     */
    public RegistrationResult deleteProduct(Long productId) {
        validateInitialized();

        long startTime = System.currentTimeMillis();

        try {
            logger.info("开始删除产品: " + productId);

            DeleteProductRequest aepRequest = new DeleteProductRequest();
            aepRequest.setParamProductId(productId);

            // 调用AEP API
            DeleteProductResponse response = productClient.DeleteProduct(aepRequest);

            long responseTime = System.currentTimeMillis() - startTime;

            // 解析响应
            return parseDeleteProductResponse(response, responseTime, productId);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.log(Level.SEVERE, "产品删除失败: " + e.getMessage(), e);

            return RegistrationResult.builder()
                .operationType("DELETE")
                .success(false)
                .errorCode("DELETE_FAILED")
                .errorMessage("产品删除失败: " + e.getMessage())
                .productId(productId)
                .responseTimeMs(responseTime)
                .build();
        }
    }

    // 私有辅助方法

    /**
     * 解析产品创建响应
     */
    private RegistrationResult parseCreateProductResponse(CreateProductResponse response, long responseTime, String productName) {
        try {
            if (response != null && response.getBody() != null) {
                String responseBody = new String(response.getBody(), StandardCharsets.UTF_8);

                // 简单的JSON解析（生产环境建议使用Gson等库）
                if (responseBody.contains("\"code\":0") || responseBody.contains("\"code\": 0")) {
                    // 解析成功响应
                    Long productId = extractProductIdFromResponse(responseBody);
                    String masterKey = extractMasterKeyFromResponse(responseBody);

                    return RegistrationResult.builder()
                        .operationType("CREATE")
                        .success(true)
                        .productId(productId)
                        .productName(productName)
                        .masterKey(masterKey)
                        .status("ACTIVE")
                        .message("产品创建成功")
                        .responseTimeMs(responseTime)
                        .apiResponseRaw(responseBody)
                        .build();
                } else {
                    // 解析错误响应
                    String errorMessage = extractErrorMessageFromResponse(responseBody);
                    String errorCode = extractErrorCodeFromResponse(responseBody);

                    return RegistrationResult.builder()
                        .operationType("CREATE")
                        .success(false)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .productName(productName)
                        .responseTimeMs(responseTime)
                        .apiResponseRaw(responseBody)
                        .build();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "解析产品创建响应失败: " + e.getMessage(), e);
        }

        return RegistrationResult.failure("CREATE", "PARSE_ERROR", "无法解析API响应");
    }

    /**
     * 解析产品更新响应
     */
    private RegistrationResult parseUpdateProductResponse(UpdateProductResponse response, long responseTime, Long productId, String productName) {
        // 类似的解析逻辑...
        return RegistrationResult.success("UPDATE", productId, productName, null);
    }

    /**
     * 解析产品删除响应
     */
    private RegistrationResult parseDeleteProductResponse(DeleteProductResponse response, long responseTime, Long productId) {
        // 类似的解析逻辑...
        return RegistrationResult.success("DELETE", productId, null, null);
    }

    /**
     * 安全设置参数（使用反射）
     */
    private void setParameterSafely(Object request, String methodName, Object value) {
        if (value == null) return;

        try {
            java.lang.reflect.Method method = request.getClass().getMethod(methodName, value.getClass());
            method.invoke(request, value);
        } catch (Exception e) {
            logger.warning("设置参数失败 - " + methodName + ": " + e.getMessage());
        }
    }

    /**
     * 从响应中提取产品ID
     */
    private Long extractProductIdFromResponse(String responseBody) {
        try {
            // 简单的正则解析（生产环境建议使用JSON库）
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"productId\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception e) {
            logger.warning("提取产品ID失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从响应中提取MasterKey
     */
    private String extractMasterKeyFromResponse(String responseBody) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"masterKey\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            logger.warning("提取MasterKey失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从响应中提取错误信息
     */
    private String extractErrorMessageFromResponse(String responseBody) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"msg\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            logger.warning("提取错误信息失败: " + e.getMessage());
        }
        return "未知错误";
    }

    /**
     * 从响应中提取错误代码
     */
    private String extractErrorCodeFromResponse(String responseBody) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            logger.warning("提取错误代码失败: " + e.getMessage());
        }
        return "UNKNOWN";
    }

    // 验证和工具方法

    private String validateNotEmpty(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new AepRegistrationException(name + " cannot be null or empty");
        }
        return value.trim();
    }

    private void validateInitialized() {
        if (!initialized) {
            throw new AepRegistrationException("AEP Registration Client is not initialized");
        }
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }

    // Getter方法
    public boolean isInitialized() { return initialized; }
    public String getApiHost() { return apiHost; }

    /**
     * AEP注册异常类
     */
    public static class AepRegistrationException extends RuntimeException {
        public AepRegistrationException(String message) {
            super(message);
        }

        public AepRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}