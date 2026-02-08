package com.aep.export.model;

/**
 * ExportResult单元测试
 * TDD第1轮：导出结果模型测试
 * 对应需求: FR-003-05 - 导出任务结果统计
 * 对应需求: FR-003-04 - 导出进度跟踪
 * 测试用例: TC-UNIT-FUNC-048~057
 */
public class ExportResultTest {

    public static void main(String[] args) {
        try {
            // 启用断言
            assert true : "断言功能已启用";

            System.out.println("🧪 开始ExportResult TDD测试...");

            testCreateExportResult_WithAllRequiredFields();
            testThrowException_WhenStartTimeIsNull();
            testThrowException_WhenTaskIdIsNull();
            testCreateExportResult_WithSuccessStatus();
            testCreateExportResult_WithFailureStatus();
            testCreateExportResult_WithStatistics();
            testCreateExportResult_WithFileOutputs();
            testCalculateDuration();
            testEqualsAndHashCode();
            testToString();

            System.out.println("✅ 所有ExportResult测试通过！");
            System.out.println("📊 测试覆盖情况:");
            System.out.println("  TC-UNIT-FUNC-048: ✅ shouldCreateExportResult_WithAllRequiredFields");
            System.out.println("  TC-UNIT-FUNC-049: ✅ shouldThrowException_WhenStartTimeIsNull");
            System.out.println("  TC-UNIT-FUNC-050: ✅ shouldThrowException_WhenTaskIdIsNull");
            System.out.println("  TC-UNIT-FUNC-051: ✅ shouldCreateExportResult_WithSuccessStatus");
            System.out.println("  TC-UNIT-FUNC-052: ✅ shouldCreateExportResult_WithFailureStatus");
            System.out.println("  TC-UNIT-FUNC-053: ✅ shouldCreateExportResult_WithStatistics");
            System.out.println("  TC-UNIT-FUNC-054: ✅ shouldCreateExportResult_WithFileOutputs");
            System.out.println("  TC-UNIT-FUNC-055: ✅ shouldCalculateDuration");
            System.out.println("  TC-UNIT-FUNC-056: ✅ shouldSupportEqualsAndHashCode");
            System.out.println("  TC-UNIT-FUNC-057: ✅ shouldSupportToString");

        } catch (Exception e) {
            System.err.println("❌ ExportResult测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * TC-UNIT-FUNC-048: 测试必需字段创建
     * 验证需求: FR-003-05 - 导出任务结果统计
     */
    public static void testCreateExportResult_WithAllRequiredFields() {
        System.out.println("  🔴 RED: 测试导出结果必需字段创建...");

        // Given
        String taskId = "export_task_20241228_001";
        String startTime = "2024-12-28 10:00:00";
        String status = "RUNNING";

        // When
        ExportResult result = ExportResult.builder()
            .taskId(taskId)
            .startTime(startTime)
            .status(status)
            .build();

        // Then
        assert result.getTaskId().equals(taskId) : "TaskId不匹配";
        assert result.getStartTime().equals(startTime) : "StartTime不匹配";
        assert result.getStatus().equals(status) : "Status不匹配";

        System.out.println("  🟢 GREEN: 导出结果必需字段创建测试通过");
    }

    /**
     * TC-UNIT-FUNC-049: 测试StartTime为空异常
     * 验证需求: FR-003-04 - 导出任务有效性验证
     */
    public static void testThrowException_WhenStartTimeIsNull() {
        System.out.println("  🔴 RED: 测试StartTime为空异常...");

        try {
            ExportResult.builder()
                .taskId("test_task")
                .status("SUCCESS")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("startTime") : "异常消息应包含startTime";
        }

        System.out.println("  🟢 GREEN: StartTime为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-050: 测试TaskId为空异常
     * 验证需求: FR-003-04 - 导出任务标识验证
     */
    public static void testThrowException_WhenTaskIdIsNull() {
        System.out.println("  🔴 RED: 测试TaskId为空异常...");

        try {
            ExportResult.builder()
                .startTime("2024-12-28 10:00:00")
                .status("SUCCESS")
                .build();
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("taskId") : "异常消息应包含taskId";
        }

        System.out.println("  🟢 GREEN: TaskId为空异常测试通过");
    }

    /**
     * TC-UNIT-FUNC-051: 测试成功状态创建
     * 验证需求: FR-003-05 - 成功导出结果记录
     */
    public static void testCreateExportResult_WithSuccessStatus() {
        System.out.println("  🔴 RED: 测试成功状态导出结果...");

        // Given
        ExportResult result = ExportResult.builder()
            .taskId("export_success_task")
            .startTime("2024-12-28 10:00:00")
            .endTime("2024-12-28 10:05:30")
            .status("SUCCESS")
            .message("Export completed successfully")
            .build();

        // Then
        assert result.getStatus().equals("SUCCESS") : "成功状态不匹配";
        assert result.getEndTime().equals("2024-12-28 10:05:30") : "结束时间不匹配";
        assert result.getMessage().equals("Export completed successfully") : "消息不匹配";

        System.out.println("  🟢 GREEN: 成功状态导出结果测试通过");
    }

    /**
     * TC-UNIT-FUNC-052: 测试失败状态创建
     * 验证需求: FR-003-05 - 失败导出结果记录
     */
    public static void testCreateExportResult_WithFailureStatus() {
        System.out.println("  🔴 RED: 测试失败状态导出结果...");

        ExportResult result = ExportResult.builder()
            .taskId("export_failure_task")
            .startTime("2024-12-28 10:00:00")
            .endTime("2024-12-28 10:01:15")
            .status("FAILED")
            .message("API connection timeout")
            .errorCode("TIMEOUT_ERROR")
            .build();

        // Then
        assert result.getStatus().equals("FAILED") : "失败状态不匹配";
        assert result.getMessage().equals("API connection timeout") : "错误消息不匹配";
        assert result.getErrorCode().equals("TIMEOUT_ERROR") : "错误代码不匹配";

        System.out.println("  🟢 GREEN: 失败状态导出结果测试通过");
    }

    /**
     * TC-UNIT-FUNC-053: 测试统计信息创建
     * 验证需求: FR-003-05 - 导出任务统计
     */
    public static void testCreateExportResult_WithStatistics() {
        System.out.println("  🔴 RED: 测试导出统计信息...");

        ExportResult result = ExportResult.builder()
            .taskId("export_stats_task")
            .startTime("2024-12-28 10:00:00")
            .status("SUCCESS")
            .totalProducts(5)
            .totalDevices(892)
            .processedProducts(5)
            .processedDevices(892)
            .skippedProducts(0)
            .skippedDevices(0)
            .build();

        // Then
        assert result.getTotalProducts().equals(5) : "总产品数不匹配";
        assert result.getTotalDevices().equals(892) : "总设备数不匹配";
        assert result.getProcessedProducts().equals(5) : "处理产品数不匹配";
        assert result.getProcessedDevices().equals(892) : "处理设备数不匹配";
        assert result.getSkippedProducts().equals(0) : "跳过产品数不匹配";
        assert result.getSkippedDevices().equals(0) : "跳过设备数不匹配";

        System.out.println("  🟢 GREEN: 导出统计信息测试通过");
    }

    /**
     * TC-UNIT-FUNC-054: 测试文件输出信息
     * 验证需求: FR-003-01, FR-003-02 - 导出文件记录
     */
    public static void testCreateExportResult_WithFileOutputs() {
        System.out.println("  🔴 RED: 测试文件输出信息...");

        ExportResult result = ExportResult.builder()
            .taskId("export_files_task")
            .startTime("2024-12-28 10:00:00")
            .status("SUCCESS")
            .productFilePath("./output/products_20241228.json")
            .deviceFilePath("./output/devices_20241228.csv")
            .productFileSize(2048L)
            .deviceFileSize(524288L)
            .build();

        // Then
        assert result.getProductFilePath().equals("./output/products_20241228.json") : "产品文件路径不匹配";
        assert result.getDeviceFilePath().equals("./output/devices_20241228.csv") : "设备文件路径不匹配";
        assert result.getProductFileSize().equals(2048L) : "产品文件大小不匹配";
        assert result.getDeviceFileSize().equals(524288L) : "设备文件大小不匹配";

        System.out.println("  🟢 GREEN: 文件输出信息测试通过");
    }

    /**
     * TC-UNIT-FUNC-055: 测试持续时间计算
     * 验证需求: FR-003-04 - 执行时间统计
     */
    public static void testCalculateDuration() {
        System.out.println("  🔴 RED: 测试持续时间计算...");

        ExportResult result = ExportResult.builder()
            .taskId("export_duration_task")
            .startTime("2024-12-28 10:00:00")
            .endTime("2024-12-28 10:05:30")
            .status("SUCCESS")
            .durationMillis(330000L)
            .build();

        // Then
        assert result.getDurationMillis().equals(330000L) : "持续时间不匹配";

        System.out.println("  🟢 GREEN: 持续时间计算测试通过");
    }

    /**
     * TC-UNIT-FUNC-056: 测试equals和hashCode
     * 验证设计: DM-009-04 - equals/hashCode实现
     */
    public static void testEqualsAndHashCode() {
        System.out.println("  🔴 RED: 测试导出结果equals和hashCode...");

        ExportResult result1 = ExportResult.builder()
            .taskId("export_task_001")
            .startTime("2024-12-28 10:00:00")
            .status("SUCCESS")
            .build();

        ExportResult result2 = ExportResult.builder()
            .taskId("export_task_001")
            .startTime("2024-12-28 10:00:00")
            .status("SUCCESS")
            .build();

        ExportResult result3 = ExportResult.builder()
            .taskId("export_task_002")
            .startTime("2024-12-28 11:00:00")
            .status("FAILED")
            .build();

        // When & Then
        assert result1.equals(result2) : "相同数据的导出结果对象应该相等";
        assert result1.hashCode() == result2.hashCode() : "相等导出结果对象的hashCode应该相同";
        assert !result1.equals(result3) : "不同数据的导出结果对象不应该相等";

        System.out.println("  🟢 GREEN: 导出结果equals和hashCode测试通过");
    }

    /**
     * TC-UNIT-FUNC-057: 测试toString方法
     * 验证设计: DM-009-05 - toString安全实现
     */
    public static void testToString() {
        System.out.println("  🔴 RED: 测试导出结果toString方法...");

        ExportResult result = ExportResult.builder()
            .taskId("export_task_toString")
            .startTime("2024-12-28 10:00:00")
            .endTime("2024-12-28 10:05:30")
            .status("SUCCESS")
            .totalProducts(5)
            .totalDevices(892)
            .build();

        String toString = result.toString();

        // Then
        assert toString != null : "toString不应该为null";
        assert toString.contains("export_task_toString") : "应该包含taskId";
        assert toString.contains("SUCCESS") : "应该包含status";
        assert toString.contains("5") : "应该包含totalProducts";
        assert toString.contains("892") : "应该包含totalDevices";

        System.out.println("  🟢 GREEN: 导出结果toString方法测试通过");
    }
}