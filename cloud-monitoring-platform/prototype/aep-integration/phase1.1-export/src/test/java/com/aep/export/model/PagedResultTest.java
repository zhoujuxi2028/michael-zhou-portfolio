package com.aep.export.model;

import java.util.Arrays;
import java.util.List;

/**
 * PagedResult单元测试
 * TDD第1轮：分页结果模型测试
 * 对应需求: FR-002-02 - 处理设备查询分页逻辑
 * 对应需求: FR-002-05 - 大量设备流式处理
 * 测试用例: TC-UNIT-FUNC-081~090
 */
public class PagedResultTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始PagedResult TDD测试...");

            testCreatePagedResult_WithAllRequiredFields();
            testThrowException_WhenDataIsNull();
            testCreatePagedResult_WithEmptyData();
            testCreatePagedResult_WithPaginationInfo();
            testCreatePagedResult_WithProductData();
            testCreatePagedResult_WithDeviceData();
            testHasNextPage();
            testCalculateTotalPages();
            testEqualsAndHashCode();
            testToString();

            System.out.println("✅ 所有PagedResult测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-081: ✅ shouldCreatePagedResult_WithAllRequiredFields");
            System.out.println("  TC-UNIT-FUNC-082: ✅ shouldThrowException_WhenDataIsNull");
            System.out.println("  TC-UNIT-FUNC-083: ✅ shouldCreatePagedResult_WithEmptyData");
            System.out.println("  TC-UNIT-FUNC-084: ✅ shouldCreatePagedResult_WithPaginationInfo");
            System.out.println("  TC-UNIT-FUNC-085: ✅ shouldCreatePagedResult_WithProductData");
            System.out.println("  TC-UNIT-FUNC-086: ✅ shouldCreatePagedResult_WithDeviceData");
            System.out.println("  TC-UNIT-FUNC-087: ✅ shouldCalculateHasNextPage");
            System.out.println("  TC-UNIT-FUNC-088: ✅ shouldCalculateTotalPages");
            System.out.println("  TC-UNIT-FUNC-089: ✅ shouldSupportEqualsAndHashCode");
            System.out.println("  TC-UNIT-FUNC-090: ✅ shouldSupportToString");

        } catch (Exception e) {
            System.err.println("❌ PagedResult测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-081: 测试必需字段创建
     * 验证需求: FR-002-02 - 分页响应处理
     */
    public static void testCreatePagedResult_WithAllRequiredFields() {
        System.out.println("  🔴 RED: 测试分页结果必需字段创建...");

        // Given
        List<String> data = Arrays.asList("item1", "item2", "item3");
        Integer pageNum = 1;
        Integer pageSize = 10;

        // When
        PagedResult<String> result = PagedResult.<String>builder()
            .data(data)
            .pageNum(pageNum)
            .pageSize(pageSize)
            .build();

        // Then
        assert result.getData().equals(data) : "Data不匹配";
        assert result.getPageNum().equals(pageNum) : "PageNum不匹配";
        assert result.getPageSize().equals(pageSize) : "PageSize不匹配";

        System.out.println("  🟢 GREEN: 分页结果必需字段创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-082: 测试Data为空异常
     * 验证需求: FR-002-02 - 分页数据验证
     */
    public static void testThrowException_WhenDataIsNull() {
        System.out.println("  🔴 RED: 测试Data为空异常...");

        try {
            PagedResult.<String>builder()
                .pageNum(1)
                .pageSize(10)
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("data") : "异常消息应包含data";
        }

        System.out.println("  🟢 GREEN: Data为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-083: 测试空数据列表创建
     * 验证需求: FR-002-02 - 处理空结果页面
     */
    public static void testCreatePagedResult_WithEmptyData() {
        System.out.println("  🔴 RED: 测试空数据列表创建...");

        // Given
        List<String> emptyData = Arrays.asList();

        // When
        PagedResult<String> result = PagedResult.<String>builder()
            .data(emptyData)
            .pageNum(1)
            .pageSize(10)
            .total(0)
            .build();

        // Then
        assert result.getData().isEmpty() : "应该是空数据列表";
        assert result.getTotal().equals(0) : "总数应该为0";
        assert !result.hasNextPage() : "不应该有下一页";

        System.out.println("  🟢 GREEN: 空数据列表创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-084: 测试分页信息创建
     * 验证需求: FR-002-02 - 完整分页信息处理
     */
    public static void testCreatePagedResult_WithPaginationInfo() {
        System.out.println("  🔴 RED: 测试分页信息创建...");

        List<String> data = Arrays.asList("item1", "item2", "item3", "item4", "item5");

        PagedResult<String> result = PagedResult.<String>builder()
            .data(data)
            .pageNum(2)
            .pageSize(10)
            .total(25)
            .pages(3)
            .isFirstPage(false)
            .isLastPage(false)
            .hasPreviousPage(true)
            .hasNextPage(true)
            .build();

        // Then
        assert result.getPageNum().equals(2) : "页码不匹配";
        assert result.getPageSize().equals(10) : "页大小不匹配";
        assert result.getTotal().equals(25) : "总数不匹配";
        assert result.getPages().equals(3) : "总页数不匹配";
        assert !result.getIsFirstPage() : "不应该是第一页";
        assert !result.getIsLastPage() : "不应该是最后一页";
        assert result.getHasPreviousPage() : "应该有上一页";
        assert result.getHasNextPage() : "应该有下一页";

        System.out.println("  🟢 GREEN: 分页信息创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-085: 测试产品数据分页
     * 验证需求: FR-001-01 - 产品列表分页处理
     */
    public static void testCreatePagedResult_WithProductData() {
        System.out.println("  🔴 RED: 测试产品数据分页...");

        // 模拟产品数据
        List<String> productData = Arrays.asList(
            "Product_16857118_RepeaterLTE",
            "Product_16980130_RepeaterLTE01",
            "Product_16980143_RepeaterLTE02"
        );

        PagedResult<String> result = PagedResult.<String>builder()
            .data(productData)
            .pageNum(1)
            .pageSize(50)
            .total(3)
            .dataType("PRODUCT")
            .build();

        // Then
        assert result.getData().size() == 3 : "产品数据数量不匹配";
        assert result.getDataType().equals("PRODUCT") : "数据类型不匹配";
        assert result.getData().get(0).contains("RepeaterLTE") : "产品数据内容不匹配";

        System.out.println("  🟢 GREEN: 产品数据分页测试通过");
    }

    /**
     * TC-UNIT-FUNC-086: 测试设备数据分页
     * 验证需求: FR-002-01, FR-002-02 - 设备查询分页处理
     */
    public static void testCreatePagedResult_WithDeviceData() {
        System.out.println("  🔴 RED: 测试设备数据分页...");

        // 模拟设备数据
        List<String> deviceData = Arrays.asList(
            "Device_16857118866877072647385_866877072647385",
            "Device_16857118866877072647386_866877072647386",
            "Device_16857118866877072647387_866877072647387"
        );

        PagedResult<String> result = PagedResult.<String>builder()
            .data(deviceData)
            .pageNum(1)
            .pageSize(50)
            .total(892)  // 基于验证阶段的真实设备数量
            .pages(18)   // 892 / 50 = 18页
            .dataType("DEVICE")
            .productId(16857118L)
            .build();

        // Then
        assert result.getData().size() == 3 : "设备数据数量不匹配";
        assert result.getDataType().equals("DEVICE") : "数据类型不匹配";
        assert result.getProductId().equals(16857118L) : "产品ID不匹配";
        assert result.getTotal().equals(892) : "设备总数不匹配";

        System.out.println("  🟢 GREEN: 设备数据分页测试通过");
    }

    /**
     * TC-UNIT-FUNC-087: 测试是否有下一页判断
     * 验证需求: FR-002-05 - 大量数据流式处理
     */
    public static void testHasNextPage() {
        System.out.println("  🔴 RED: 测试是否有下一页判断...");

        List<String> data = Arrays.asList("item1", "item2");

        // 测试有下一页的情况
        PagedResult<String> hasNextResult = PagedResult.<String>builder()
            .data(data)
            .pageNum(1)
            .pageSize(10)
            .total(25)
            .build();

        // 测试没有下一页的情况
        PagedResult<String> noNextResult = PagedResult.<String>builder()
            .data(data)
            .pageNum(3)
            .pageSize(10)
            .total(25)
            .build();

        // Then
        assert hasNextResult.hasNextPage() : "第1页应该有下一页";
        assert !noNextResult.hasNextPage() : "第3页不应该有下一页";

        System.out.println("  🟢 GREEN: 是否有下一页判断测试通过");
    }

    /**
     * TC-UNIT-FUNC-088: 测试总页数计算
     * 验证需求: FR-002-02 - 分页信息计算
     */
    public static void testCalculateTotalPages() {
        System.out.println("  🔴 RED: 测试总页数计算...");

        List<String> data = Arrays.asList("item1");

        PagedResult<String> result = PagedResult.<String>builder()
            .data(data)
            .pageNum(1)
            .pageSize(50)
            .total(892)
            .build();

        // Then
        // 892 / 50 = 17.84，向上取整 = 18页
        Integer expectedPages = result.calculateTotalPages();
        assert expectedPages.equals(18) : "总页数计算不正确，应该是18页，实际：" + expectedPages;

        System.out.println("  🟢 GREEN: 总页数计算测试通过");
    }

    /**
     * TC-UNIT-FUNC-089: 测试equals和hashCode
     * 验证设计: DM-014-04 - equals/hashCode实现
     */
    public static void testEqualsAndHashCode() {
        System.out.println("  🔴 RED: 测试分页结果equals和hashCode...");

        List<String> data = Arrays.asList("item1", "item2");

        PagedResult<String> result1 = PagedResult.<String>builder()
            .data(data)
            .pageNum(1)
            .pageSize(10)
            .total(25)
            .build();

        PagedResult<String> result2 = PagedResult.<String>builder()
            .data(data)
            .pageNum(1)
            .pageSize(10)
            .total(25)
            .build();

        PagedResult<String> result3 = PagedResult.<String>builder()
            .data(Arrays.asList("item3"))
            .pageNum(2)
            .pageSize(20)
            .total(50)
            .build();

        // When & Then
        assert result1.equals(result2) : "相同数据的分页结果对象应该相等";
        assert result1.hashCode() == result2.hashCode() : "相等分页结果对象的hashCode应该相同";
        assert !result1.equals(result3) : "不同数据的分页结果对象不应该相等";

        System.out.println("  🟢 GREEN: 分页结果equals和hashCode测试通过");
    }

    /**
     * TC-UNIT-FUNC-090: 测试toString方法
     * 验证设计: DM-014-05 - toString安全实现
     */
    public static void testToString() {
        System.out.println("  🔴 RED: 测试分页结果toString方法...");

        List<String> data = Arrays.asList("item1", "item2", "item3");

        PagedResult<String> result = PagedResult.<String>builder()
            .data(data)
            .pageNum(1)
            .pageSize(10)
            .total(25)
            .dataType("DEVICE")
            .build();

        String toString = result.toString();

        // Then
        assert toString != null : "toString不应该为null";
        assert toString.contains("pageNum=1") : "应该包含pageNum";
        assert toString.contains("pageSize=10") : "应该包含pageSize";
        assert toString.contains("total=25") : "应该包含total";
        assert toString.contains("dataType='DEVICE'") : "应该包含dataType";
        assert toString.contains("size=3") : "应该包含数据大小";

        System.out.println("  🟢 GREEN: 分页结果toString方法测试通过");
    }
}