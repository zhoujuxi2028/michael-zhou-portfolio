package com.aep.export.service;

import com.aep.export.model.ExportConfig;
import com.aep.export.model.ProductInfo;
import com.aep.export.model.PagedResult;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 产品管理服务
 * 对应需求: FR-001-02 - 提取产品基本信息
 * 对应需求: FR-001-03 - 产品数据分页查询
 * 对应需求: NFR-001-03 - 产品数据导出性能
 * 设计模块: DM-012 - ProductService
 * 负责产品数据的查询、解析、验证和缓存管理
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class ProductService {

    private final ExportConfig config;
    private final AepClientManager clientManager;
    private final ErrorHandler errorHandler;
    private final Map<Long, ProductInfo> productCache;
    private final int maxRetries;

    /**
     * 构造函数
     * 实现: DM-012-01 - 产品服务初始化
     */
    public ProductService(ExportConfig config) {
        this.config = config;
        this.clientManager = new AepClientManager(config);
        this.errorHandler = new ErrorHandler(config);
        this.productCache = new ConcurrentHashMap<>();
        this.maxRetries = config.getMaxRetries() != null ? config.getMaxRetries() : 3;
    }

    /**
     * 查询产品列表（分页）
     * 实现: DM-012-02 - 产品列表分页查询
     */
    public PagedResult<ProductInfo> queryProductList(int pageNum, int pageSize) {
        try {
            // 构建查询参数
            Map<String, Object> params = new HashMap<>();
            params.put("pageNum", pageNum);
            params.put("pageSize", pageSize);

            // 调用AEP API
            String response = clientManager.queryProducts(params);

            // 解析响应
            return parseProductListFromResponse(response, pageNum, pageSize);

        } catch (Exception e) {
            // 错误处理
            errorHandler.handleException(e, "QUERY_PRODUCT_LIST");

            // 返回空结果
            return createEmptyPagedResult(pageNum, pageSize);
        }
    }

    /**
     * 根据ID查询产品
     * 实现: DM-012-03 - 产品单个查询
     */
    public ProductInfo queryProductById(Long productId) {
        if (productId == null) {
            return null;
        }

        // 检查缓存
        if (productCache.containsKey(productId)) {
            return productCache.get(productId);
        }

        try {
            // 构建查询参数
            Map<String, Object> params = new HashMap<>();
            params.put("productId", productId);

            // 调用AEP API
            String response = clientManager.queryProductById(params);

            // 解析响应
            ProductInfo productInfo = parseProductFromResponse(response);

            // 更新缓存
            if (productInfo != null) {
                productCache.put(productId, productInfo);
            }

            return productInfo;

        } catch (Exception e) {
            // 错误处理
            errorHandler.handleException(e, "QUERY_PRODUCT_BY_ID");
            return null;
        }
    }

    /**
     * 从API响应解析产品信息
     * 实现: DM-012-04 - 产品数据解析
     */
    public ProductInfo parseProductFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }

        try {
            // 简化的JSON解析（实际项目中应该使用JSON库）
            String productIdStr = extractJsonValue(response, "productId");
            String productName = extractJsonValue(response, "productName");
            String tenantId = extractJsonValue(response, "tenantId");
            String deviceCountStr = extractJsonValue(response, "deviceCount");
            String createTime = extractJsonValue(response, "createTime");
            String updateTime = extractJsonValue(response, "updateTime");
            String deviceModel = extractJsonValue(response, "deviceModel");
            String apiKey = extractJsonValue(response, "apiKey");

            if (productIdStr == null || productName == null) {
                return null;
            }

            return ProductInfo.builder()
                .productId(Long.parseLong(productIdStr))
                .productName(productName)
                .masterKey(apiKey)
                .tenantId(tenantId)
                .deviceCount(deviceCountStr != null ? Integer.parseInt(deviceCountStr) : null)
                .createTime(createTime)
                .updateTime(updateTime)
                .deviceModel(deviceModel)
                .build();

        } catch (Exception e) {
            errorHandler.handleException(e, "PARSE_PRODUCT_RESPONSE");
            return null;
        }
    }

    /**
     * 验证产品数据
     * 实现: DM-012-05 - 产品数据验证
     */
    public boolean validateProductData(Long productId, String productName, String masterKey) {
        if (productId == null) {
            return false;
        }

        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }

        return true; // masterKey可以为空
    }

    /**
     * 获取重试次数配置
     * 实现: DM-012-06 - 重试配置
     */
    public int getRetryCount() {
        return maxRetries;
    }

    /**
     * 判断是否应该重试查询
     * 实现: DM-012-06 - 重试判断
     */
    public boolean shouldRetryQuery(int currentAttempt) {
        return currentAttempt <= maxRetries;
    }

    /**
     * 清理产品缓存
     * 实现: DM-012-07 - 缓存管理
     */
    public void clearCache() {
        productCache.clear();
    }

    /**
     * 获取缓存大小
     * 实现: DM-012-07 - 缓存状态
     */
    public int getCacheSize() {
        return productCache.size();
    }

    // 私有辅助方法

    /**
     * 从AEP SDK响应解析产品列表
     */
    private PagedResult<ProductInfo> parseProductListFromResponse(String response, int pageNum, int pageSize) {
        try {
            List<ProductInfo> productList = new ArrayList<>();

            // 解析AEP SDK响应数据 - 处理response.toString()格式
            if (response != null && response.contains("body=")) {
                // 提取body部分的JSON数据
                int bodyStart = response.indexOf("body=") + 5;
                String jsonBody = response.substring(bodyStart);

                // 检查响应是否成功
                String code = extractJsonValue(jsonBody, "code");
                if (!"0".equals(code)) {
                    LogManager.getInstance().error("产品查询", "ProductService", "AEP API返回错误: code=" + code);
                    System.err.println("AEP API返回错误: code=" + code); // 保持控制台输出
                    return createEmptyPagedResult(pageNum, pageSize);
                }

                // 提取result中的list数组
                int resultStart = jsonBody.indexOf("\"result\":");
                if (resultStart != -1) {
                    String resultContent = jsonBody.substring(resultStart + 9);

                    // 查找list数组
                    int listStart = resultContent.indexOf("\"list\":[");
                    if (listStart != -1) {
                        String listContent = resultContent.substring(listStart + 8);
                        int listEnd = listContent.indexOf("]}}");
                        if (listEnd != -1) {
                            String listData = listContent.substring(0, listEnd);

                            // 按产品对象分割
                            String[] productBlocks = listData.split("\\},\\{");
                            for (int i = 0; i < productBlocks.length; i++) {
                                String productData = productBlocks[i];

                                // 确保数据格式正确
                                if (!productData.startsWith("{")) productData = "{" + productData;
                                if (!productData.endsWith("}")) productData = productData + "}";

                                // 提取产品信息
                                String productId = extractJsonValue(productData, "productId");
                                String productName = extractJsonValue(productData, "productName");
                                String tenantId = extractJsonValue(productData, "tenantId");
                                String deviceCount = extractJsonValue(productData, "deviceCount");
                                String apiKey = extractJsonValue(productData, "apiKey");
                                String createTime = extractJsonValue(productData, "createTime");

                                if (!productId.isEmpty() && !productName.isEmpty()) {
                                    ProductInfo product = ProductInfo.builder()
                                        .productId(Long.valueOf(productId))
                                        .productName(productName)
                                        .masterKey(apiKey)
                                        .tenantId(tenantId)
                                        .deviceCount(deviceCount.isEmpty() ? 0 : Integer.parseInt(deviceCount))
                                        .createTime(formatTimestamp(createTime))
                                        .build();
                                    productList.add(product);

                                    // 更新缓存
                                    productCache.put(product.getProductId(), product);
                                }
                            }
                        }
                    }
                }
            }

            return PagedResult.<ProductInfo>builder()
                .data(productList)
                .total(productList.size())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .hasNextPage(false)
                .build();

        } catch (Exception e) {
            errorHandler.handleException(e, "PARSE_PRODUCT_LIST_RESPONSE");
            return createEmptyPagedResult(pageNum, pageSize);
        }
    }

    /**
     * 从JSON字符串中提取指定字段的值
     */
    private String extractJsonValue(String jsonData, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int start = jsonData.indexOf(pattern);
            if (start == -1) return "";

            start += pattern.length();
            if (jsonData.charAt(start) == '"') {
                // 字符串值
                start++;
                int end = jsonData.indexOf('"', start);
                return end > start ? jsonData.substring(start, end) : "";
            } else {
                // 数值
                int end = start;
                while (end < jsonData.length() &&
                       (Character.isDigit(jsonData.charAt(end)) || jsonData.charAt(end) == '.')) {
                    end++;
                }
                return end > start ? jsonData.substring(start, end) : "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(String timestamp) {
        try {
            if (timestamp.isEmpty()) return "";
            long ts = Long.parseLong(timestamp);
            java.util.Date date = new java.util.Date(ts);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }

    /**
     * 创建空的分页结果
     */
    private PagedResult<ProductInfo> createEmptyPagedResult(int pageNum, int pageSize) {
        return PagedResult.<ProductInfo>builder()
            .data(new ArrayList<>())
            .total(0)
            .pageNum(pageNum)
            .pageSize(pageSize)
            .hasNextPage(false)
            .build();
    }

}