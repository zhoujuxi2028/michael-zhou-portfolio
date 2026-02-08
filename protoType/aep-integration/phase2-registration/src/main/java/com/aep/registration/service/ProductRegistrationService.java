package com.aep.registration.service;

import com.aep.registration.model.ProductRegistrationRequest;
import com.aep.registration.model.RegistrationResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 产品注册服务
 *
 * 核心业务服务，提供产品注册、更新、删除等完整的产品生命周期管理功能
 * 包含数据验证、权限检查、操作审计、错误处理等完整的业务逻辑
 *
 * @author AEP Registration Tool
 * @version 1.0
 */
public class ProductRegistrationService {

    private static final Logger logger = Logger.getLogger(ProductRegistrationService.class.getName());

    // AEP客户端
    private final AepRegistrationClient aepClient;

    // 缓存和状态管理
    private final Map<Long, RegistrationResult> operationCache;
    private final Map<String, Long> productNameIndex;  // 产品名称到ID的索引

    // 配置参数
    private final int maxRetries;
    private final long operationTimeoutMs;
    private final boolean enableAuditLog;

    // 统计信息
    private long totalOperations = 0;
    private long successfulOperations = 0;
    private long failedOperations = 0;

    /**
     * 构造函数
     */
    public ProductRegistrationService(AepRegistrationClient aepClient) {
        this(aepClient, 3, 30000L, true);
    }

    public ProductRegistrationService(AepRegistrationClient aepClient,
                                    int maxRetries,
                                    long operationTimeoutMs,
                                    boolean enableAuditLog) {
        this.aepClient = Objects.requireNonNull(aepClient, "AepRegistrationClient cannot be null");
        this.maxRetries = Math.max(1, maxRetries);
        this.operationTimeoutMs = Math.max(5000L, operationTimeoutMs);
        this.enableAuditLog = enableAuditLog;

        this.operationCache = new ConcurrentHashMap<>();
        this.productNameIndex = new ConcurrentHashMap<>();

        logger.info("ProductRegistrationService初始化完成 - 重试次数: " + maxRetries +
                   ", 超时时间: " + operationTimeoutMs + "ms, 审计日志: " + enableAuditLog);
    }

    /**
     * 注册新产品
     *
     * @param request 产品注册请求
     * @return 注册结果
     */
    public RegistrationResult registerProduct(ProductRegistrationRequest request) {
        String operationId = "REG_" + System.currentTimeMillis();
        long startTime = System.currentTimeMillis();

        try {
            totalOperations++;

            logger.info("[" + operationId + "] 开始注册产品: " + request.getProductName());

            // 1. 预检查验证
            RegistrationResult preCheckResult = performPreRegistrationChecks(request);
            if (!preCheckResult.isSuccess()) {
                auditLog(operationId, "CREATE", "PRE_CHECK_FAILED", request, preCheckResult);
                return preCheckResult;
            }

            // 2. 检查产品名称冲突
            if (isProductNameExists(request.getProductName())) {
                RegistrationResult duplicateResult = RegistrationResult.failure(
                    "CREATE",
                    "DUPLICATE_NAME",
                    "产品名称已存在: " + request.getProductName()
                );
                auditLog(operationId, "CREATE", "DUPLICATE_NAME", request, duplicateResult);
                return duplicateResult;
            }

            // 3. 执行注册操作（带重试机制）
            RegistrationResult result = executeWithRetry(() -> aepClient.createProduct(request),
                                                         "产品注册", operationId);

            // 4. 后处理
            if (result.isSuccess()) {
                successfulOperations++;
                postRegistrationSuccess(result, request);
                logger.info("[" + operationId + "] 产品注册成功: " + request.getProductName() +
                          " (ProductId: " + result.getProductId() + ")");
            } else {
                failedOperations++;
                logger.warning("[" + operationId + "] 产品注册失败: " + result.getErrorMessage());
            }

            // 5. 审计日志
            auditLog(operationId, "CREATE", result.isSuccess() ? "SUCCESS" : "FAILED", request, result);

            // 6. 缓存结果
            if (result.getProductId() != null) {
                operationCache.put(result.getProductId(), result);
            }

            return result;

        } catch (Exception e) {
            failedOperations++;
            logger.log(Level.SEVERE, "[" + operationId + "] 产品注册异常: " + e.getMessage(), e);

            RegistrationResult errorResult = RegistrationResult.failure(
                "CREATE",
                "SYSTEM_ERROR",
                "系统异常: " + e.getMessage()
            );

            auditLog(operationId, "CREATE", "SYSTEM_ERROR", request, errorResult);
            return errorResult;

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[" + operationId + "] 产品注册操作完成，耗时: " + duration + "ms");
        }
    }

    /**
     * 更新产品配置
     */
    public RegistrationResult updateProduct(Long productId, ProductRegistrationRequest request) {
        String operationId = "UPD_" + productId + "_" + System.currentTimeMillis();

        try {
            totalOperations++;

            logger.info("[" + operationId + "] 开始更新产品: " + productId + " -> " + request.getProductName());

            // 1. 验证产品存在
            if (!isProductExists(productId)) {
                RegistrationResult notFoundResult = RegistrationResult.failure(
                    "UPDATE",
                    "PRODUCT_NOT_FOUND",
                    "产品不存在: " + productId
                );
                auditLog(operationId, "UPDATE", "NOT_FOUND", request, notFoundResult);
                return notFoundResult;
            }

            // 2. 执行更新操作
            RegistrationResult result = executeWithRetry(() -> aepClient.updateProduct(productId, request),
                                                         "产品更新", operationId);

            // 3. 后处理
            if (result.isSuccess()) {
                successfulOperations++;
                postUpdateSuccess(result, productId, request);
                logger.info("[" + operationId + "] 产品更新成功: " + productId);
            } else {
                failedOperations++;
                logger.warning("[" + operationId + "] 产品更新失败: " + result.getErrorMessage());
            }

            // 4. 审计和缓存
            auditLog(operationId, "UPDATE", result.isSuccess() ? "SUCCESS" : "FAILED", request, result);
            operationCache.put(productId, result);

            return result;

        } catch (Exception e) {
            failedOperations++;
            logger.log(Level.SEVERE, "[" + operationId + "] 产品更新异常: " + e.getMessage(), e);

            RegistrationResult errorResult = RegistrationResult.failure(
                "UPDATE",
                "SYSTEM_ERROR",
                "系统异常: " + e.getMessage()
            );
            return errorResult;
        }
    }

    /**
     * 删除产品
     */
    public RegistrationResult deleteProduct(Long productId) {
        return deleteProduct(productId, false);
    }

    /**
     * 删除产品（可选强制删除）
     */
    public RegistrationResult deleteProduct(Long productId, boolean forceDelete) {
        String operationId = "DEL_" + productId + "_" + System.currentTimeMillis();

        try {
            totalOperations++;

            logger.info("[" + operationId + "] 开始删除产品: " + productId + " (强制删除: " + forceDelete + ")");

            // 1. 预删除检查
            RegistrationResult preDeleteCheck = performPreDeleteChecks(productId, forceDelete);
            if (!preDeleteCheck.isSuccess()) {
                auditLog(operationId, "DELETE", "PRE_CHECK_FAILED", null, preDeleteCheck);
                return preDeleteCheck;
            }

            // 2. 执行删除操作
            RegistrationResult result = executeWithRetry(() -> aepClient.deleteProduct(productId),
                                                         "产品删除", operationId);

            // 3. 后处理
            if (result.isSuccess()) {
                successfulOperations++;
                postDeleteSuccess(productId);
                logger.info("[" + operationId + "] 产品删除成功: " + productId);
            } else {
                failedOperations++;
                logger.warning("[" + operationId + "] 产品删除失败: " + result.getErrorMessage());
            }

            // 4. 审计和清理
            auditLog(operationId, "DELETE", result.isSuccess() ? "SUCCESS" : "FAILED", null, result);
            if (result.isSuccess()) {
                operationCache.remove(productId);
            }

            return result;

        } catch (Exception e) {
            failedOperations++;
            logger.log(Level.SEVERE, "[" + operationId + "] 产品删除异常: " + e.getMessage(), e);

            return RegistrationResult.failure("DELETE", "SYSTEM_ERROR", "系统异常: " + e.getMessage());
        }
    }

    /**
     * 批量注册产品
     */
    public List<RegistrationResult> registerProductsBatch(List<ProductRegistrationRequest> requests) {
        logger.info("开始批量注册 " + requests.size() + " 个产品");

        List<RegistrationResult> results = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            ProductRegistrationRequest request = requests.get(i);
            logger.info("批量注册进度: " + (i + 1) + "/" + requests.size() + " - " + request.getProductName());

            RegistrationResult result = registerProduct(request);
            results.add(result);

            // 如果连续失败过多，考虑终止批量操作
            if (!result.isSuccess() && shouldTerminateBatchOperation(results)) {
                logger.warning("批量操作失败率过高，终止剩余操作");
                break;
            }

            // 批量操作间隔（避免API频率限制）
            if (i < requests.size() - 1) {
                try {
                    Thread.sleep(1000); // 1秒间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("批量注册完成，总计: " + requests.size() + ", 成功: " +
                   results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum());

        return results;
    }

    // 私有辅助方法

    /**
     * 执行带重试机制的操作
     */
    private RegistrationResult executeWithRetry(OperationSupplier operation, String operationName, String operationId) {
        RegistrationResult lastResult = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("[" + operationId + "] " + operationName + " 尝试 " + attempt + "/" + maxRetries);

                lastResult = operation.get();

                if (lastResult.isSuccess()) {
                    if (attempt > 1) {
                        logger.info("[" + operationId + "] " + operationName + " 在第 " + attempt + " 次尝试成功");
                    }
                    return lastResult;
                }

                // 检查是否应该重试
                if (shouldRetry(lastResult, attempt)) {
                    long delay = calculateRetryDelay(attempt);
                    logger.info("[" + operationId + "] " + operationName + " 第 " + attempt + " 次尝试失败，" +
                              delay + "ms后重试: " + lastResult.getErrorMessage());

                    Thread.sleep(delay);
                } else {
                    logger.warning("[" + operationId + "] " + operationName + " 不适合重试: " + lastResult.getErrorMessage());
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return RegistrationResult.failure("OPERATION", "INTERRUPTED", "操作被中断");
            } catch (Exception e) {
                logger.log(Level.WARNING, "[" + operationId + "] " + operationName + " 第 " + attempt + " 次尝试异常", e);
                lastResult = RegistrationResult.failure("OPERATION", "EXCEPTION", "操作异常: " + e.getMessage());
            }
        }

        return lastResult != null ? lastResult :
               RegistrationResult.failure("OPERATION", "MAX_RETRIES_EXCEEDED", "超过最大重试次数");
    }

    /**
     * 注册前检查
     */
    private RegistrationResult performPreRegistrationChecks(ProductRegistrationRequest request) {
        // 1. 基础验证
        if (!request.isValid()) {
            return RegistrationResult.failure("CREATE", "VALIDATION_ERROR", request.getValidationError());
        }

        // 2. 业务规则验证
        if (request.getProductName().length() > 50) {
            return RegistrationResult.failure("CREATE", "NAME_TOO_LONG", "产品名称不能超过50个字符");
        }

        // 3. 设备类型验证
        if (!isValidDeviceType(request.getDeviceType())) {
            return RegistrationResult.failure("CREATE", "INVALID_DEVICE_TYPE", "不支持的设备类型: " + request.getDeviceType());
        }

        // 4. 网络类型验证
        if (request.getNetworkType() != null && !isValidNetworkType(request.getNetworkType())) {
            return RegistrationResult.failure("CREATE", "INVALID_NETWORK_TYPE", "不支持的网络类型: " + request.getNetworkType());
        }

        return RegistrationResult.success("CREATE", null, request.getProductName(), null);
    }

    /**
     * 删除前检查
     */
    private RegistrationResult performPreDeleteChecks(Long productId, boolean forceDelete) {
        // 1. 产品存在性检查
        if (!isProductExists(productId)) {
            return RegistrationResult.failure("DELETE", "PRODUCT_NOT_FOUND", "产品不存在: " + productId);
        }

        // 2. 依赖检查（如果有关联设备，需要先处理设备）
        if (!forceDelete && hasActiveDevices(productId)) {
            return RegistrationResult.failure("DELETE", "HAS_ACTIVE_DEVICES",
                "产品下还有活跃设备，请先删除设备或使用强制删除");
        }

        return RegistrationResult.success("DELETE", productId, null, null);
    }

    /**
     * 注册成功后处理
     */
    private void postRegistrationSuccess(RegistrationResult result, ProductRegistrationRequest request) {
        // 更新产品名称索引
        if (result.getProductId() != null && request.getProductName() != null) {
            productNameIndex.put(request.getProductName(), result.getProductId());
        }

        // 可以在这里添加其他后处理逻辑，如发送通知等
    }

    /**
     * 更新成功后处理
     */
    private void postUpdateSuccess(RegistrationResult result, Long productId, ProductRegistrationRequest request) {
        // 更新索引
        if (request.getProductName() != null) {
            productNameIndex.put(request.getProductName(), productId);
        }
    }

    /**
     * 删除成功后处理
     */
    private void postDeleteSuccess(Long productId) {
        // 清理索引
        productNameIndex.entrySet().removeIf(entry -> entry.getValue().equals(productId));
    }

    // 验证和检查方法

    private boolean isProductNameExists(String productName) {
        return productNameIndex.containsKey(productName);
    }

    private boolean isProductExists(Long productId) {
        // 简化实现，实际项目中可能需要调用AEP API查询
        return productId != null && productId > 0;
    }

    private boolean hasActiveDevices(Long productId) {
        // 简化实现，实际项目中需要查询设备列表
        return false; // 假设没有活跃设备
    }

    private boolean isValidDeviceType(String deviceType) {
        // 定义支持的设备类型
        Set<String> validTypes = Set.of("SENSOR", "GATEWAY", "DEVICE", "TERMINAL", "MODULE");
        return validTypes.contains(deviceType.toUpperCase());
    }

    private boolean isValidNetworkType(String networkType) {
        // 定义支持的网络类型
        Set<String> validTypes = Set.of("NB-IOT", "2G", "3G", "4G", "5G", "WIFI", "ETHERNET", "LORA");
        return validTypes.contains(networkType.toUpperCase());
    }

    private boolean shouldRetry(RegistrationResult result, int attempt) {
        if (attempt >= maxRetries) return false;

        // 某些错误不适合重试
        String errorCode = result.getErrorCode();
        Set<String> nonRetryableCodes = Set.of(
            "VALIDATION_ERROR", "DUPLICATE_NAME", "INVALID_DEVICE_TYPE",
            "PRODUCT_NOT_FOUND", "PERMISSION_DENIED"
        );

        return !nonRetryableCodes.contains(errorCode);
    }

    private long calculateRetryDelay(int attempt) {
        // 指数退避策略
        return Math.min(1000L * (1L << (attempt - 1)), 10000L); // 最大10秒
    }

    private boolean shouldTerminateBatchOperation(List<RegistrationResult> results) {
        if (results.size() < 5) return false; // 至少尝试5个

        // 如果最近5次操作都失败了，终止批量操作
        long recentFailures = results.stream()
            .skip(Math.max(0, results.size() - 5))
            .mapToLong(r -> r.isSuccess() ? 0 : 1)
            .sum();

        return recentFailures >= 5;
    }

    /**
     * 审计日志记录
     */
    private void auditLog(String operationId, String operationType, String status,
                         ProductRegistrationRequest request, RegistrationResult result) {
        if (!enableAuditLog) return;

        try {
            String auditMessage = String.format(
                "[AUDIT] %s | %s | %s | Product: %s | Result: %s",
                operationId,
                operationType,
                status,
                request != null ? request.getProductName() : "N/A",
                result != null ? (result.isSuccess() ? "SUCCESS" : result.getErrorCode()) : "N/A"
            );

            logger.info(auditMessage);

            // 在实际项目中，这里可以写入专门的审计日志文件或数据库

        } catch (Exception e) {
            logger.log(Level.WARNING, "写入审计日志失败: " + e.getMessage(), e);
        }
    }

    // 统计和状态方法

    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOperations", totalOperations);
        stats.put("successfulOperations", successfulOperations);
        stats.put("failedOperations", failedOperations);
        stats.put("successRate", totalOperations > 0 ? (double) successfulOperations / totalOperations : 0.0);
        stats.put("cacheSize", operationCache.size());
        stats.put("indexSize", productNameIndex.size());
        stats.put("maxRetries", maxRetries);
        stats.put("operationTimeoutMs", operationTimeoutMs);
        return stats;
    }

    public void clearCache() {
        operationCache.clear();
        productNameIndex.clear();
        logger.info("缓存已清理");
    }

    public RegistrationResult getLastResult(Long productId) {
        return operationCache.get(productId);
    }

    // 函数式接口
    @FunctionalInterface
    private interface OperationSupplier {
        RegistrationResult get() throws Exception;
    }
}