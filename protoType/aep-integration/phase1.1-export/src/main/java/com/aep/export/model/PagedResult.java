package com.aep.export.model;

import java.util.List;
import java.util.Objects;

/**
 * 分页结果数据模型
 * 对应需求: FR-002-02 - 处理设备查询分页逻辑
 * 对应需求: FR-002-05 - 大量设备流式处理
 * 设计模块: DM-014 - PagedResult
 * 用于封装AEP API分页响应数据的通用模型
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 * @param <T> 数据类型泛型
 */
public class PagedResult<T> {

    // 必需字段 - 分页数据 (DM-014-01)
    private final List<T> data;        // 当前页数据 (必需)
    private final Integer pageNum;     // 当前页码 (必需)
    private final Integer pageSize;    // 页大小 (必需)

    // 统计信息字段 (DM-014-01)
    private final Integer total;       // 总记录数
    private final Integer pages;       // 总页数

    // 分页状态字段 (DM-014-01)
    private final Boolean isFirstPage;     // 是否第一页
    private final Boolean isLastPage;      // 是否最后一页
    private final Boolean hasPreviousPage; // 是否有上一页
    private final Boolean hasNextPage;     // 是否有下一页

    // 业务扩展字段 (DM-014-01)
    private final String dataType;     // 数据类型: PRODUCT, DEVICE
    private final Long productId;      // 关联产品ID (设备查询时使用)
    private final String queryTime;    // 查询时间

    /**
     * 私有构造函数，强制使用Builder模式
     * 实现: DM-014-02 - Builder模式实现
     */
    private PagedResult(Builder<T> builder) {
        this.data = validateRequired(builder.data, "data");
        this.pageNum = builder.pageNum;
        this.pageSize = builder.pageSize;

        this.total = builder.total;
        this.pages = builder.pages;

        this.isFirstPage = builder.isFirstPage;
        this.isLastPage = builder.isLastPage;
        this.hasPreviousPage = builder.hasPreviousPage;
        this.hasNextPage = builder.hasNextPage;

        this.dataType = builder.dataType;
        this.productId = builder.productId;
        this.queryTime = builder.queryTime;
    }

    /**
     * 创建Builder实例
     */
    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    /**
     * Builder类实现
     * 实现: DM-014-02 - Builder模式实现
     */
    public static class Builder<T> {
        private List<T> data;
        private Integer pageNum;
        private Integer pageSize;
        private Integer total;
        private Integer pages;
        private Boolean isFirstPage;
        private Boolean isLastPage;
        private Boolean hasPreviousPage;
        private Boolean hasNextPage;
        private String dataType;
        private Long productId;
        private String queryTime;

        public Builder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public Builder<T> pageNum(Integer pageNum) {
            this.pageNum = pageNum;
            return this;
        }

        public Builder<T> pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> total(Integer total) {
            this.total = total;
            return this;
        }

        public Builder<T> pages(Integer pages) {
            this.pages = pages;
            return this;
        }

        public Builder<T> isFirstPage(Boolean isFirstPage) {
            this.isFirstPage = isFirstPage;
            return this;
        }

        public Builder<T> isLastPage(Boolean isLastPage) {
            this.isLastPage = isLastPage;
            return this;
        }

        public Builder<T> hasPreviousPage(Boolean hasPreviousPage) {
            this.hasPreviousPage = hasPreviousPage;
            return this;
        }

        public Builder<T> hasNextPage(Boolean hasNextPage) {
            this.hasNextPage = hasNextPage;
            return this;
        }

        public Builder<T> dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder<T> productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder<T> queryTime(String queryTime) {
            this.queryTime = queryTime;
            return this;
        }

        public PagedResult<T> build() {
            return new PagedResult<>(this);
        }
    }

    /**
     * 字段验证逻辑
     * 实现: DM-014-03 - 字段验证逻辑
     */
    private <U> U validateRequired(U value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    // Getter方法
    public List<T> getData() { return data; }
    public Integer getPageNum() { return pageNum; }
    public Integer getPageSize() { return pageSize; }
    public Integer getTotal() { return total; }
    public Integer getPages() { return pages; }
    public Boolean getIsFirstPage() { return isFirstPage; }
    public Boolean getIsLastPage() { return isLastPage; }
    public Boolean getHasPreviousPage() { return hasPreviousPage; }
    public Boolean getHasNextPage() { return hasNextPage; }
    public String getDataType() { return dataType; }
    public Long getProductId() { return productId; }
    public String getQueryTime() { return queryTime; }

    /**
     * 是否有下一页判断方法
     * 实现: DM-014-06 - 分页计算方法
     */
    public boolean hasNextPage() {
        if (hasNextPage != null) {
            return hasNextPage;
        }
        if (total == null || pageNum == null || pageSize == null) {
            return false;
        }
        return pageNum * pageSize < total;
    }

    /**
     * 计算总页数
     * 实现: DM-014-06 - 分页计算方法
     */
    public Integer calculateTotalPages() {
        if (total == null || pageSize == null || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 是否为空结果
     * 实现: DM-014-06 - 数据状态判断方法
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    /**
     * 获取当前页数据大小
     * 实现: DM-014-06 - 数据统计方法
     */
    public int size() {
        return data == null ? 0 : data.size();
    }

    /**
     * equals方法实现
     * 实现: DM-014-04 - equals/hashCode实现
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagedResult<?> that = (PagedResult<?>) o;
        return Objects.equals(data, that.data) &&
               Objects.equals(pageNum, that.pageNum) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(total, that.total) &&
               Objects.equals(dataType, that.dataType) &&
               Objects.equals(productId, that.productId);
    }

    /**
     * hashCode方法实现
     * 实现: DM-014-04 - equals/hashCode实现
     */
    @Override
    public int hashCode() {
        return Objects.hash(data, pageNum, pageSize, total, dataType, productId);
    }

    /**
     * toString方法实现
     * 实现: DM-014-05 - toString安全实现
     */
    @Override
    public String toString() {
        return "PagedResult{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", pages=" + (pages != null ? pages : calculateTotalPages()) +
                ", dataType='" + dataType + '\'' +
                ", productId=" + productId +
                ", size=" + size() +
                ", isEmpty=" + isEmpty() +
                ", hasNextPage=" + hasNextPage() +
                ", isFirstPage=" + (pageNum != null && pageNum == 1) +
                ", isLastPage=" + (pageNum != null && pages != null && pageNum.equals(pages)) +
                '}';
    }
}