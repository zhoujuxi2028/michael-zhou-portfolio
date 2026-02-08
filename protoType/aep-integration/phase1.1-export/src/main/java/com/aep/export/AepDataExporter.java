package com.aep.export;

import com.aep.export.model.ExportConfig;
import com.aep.export.model.ExportResult;
import com.aep.export.service.ConfigService;
import com.aep.export.service.ExportService;
import com.aep.export.service.ErrorHandler;
import com.aep.export.service.LogManager;
import java.util.*;
import java.io.File;
import java.text.SimpleDateFormat;

/**
 * AEP数据导出工具主程序
 * 对应需求: FR-005-01 - 命令行程序入口
 * 对应需求: FR-005-02 - 环境变量配置加载
 * 对应需求: FR-005-03 - 完整导出流程执行
 * 对应需求: NFR-004-01 - 程序稳定性和容错
 * 设计模块: DM-017 - AepDataExporter
 * 负责协调所有服务组件，提供命令行接口和程序入口
 *
 * @author ZCT AEP Export Tool
 * @version 1.0
 */
public class AepDataExporter {

    private static final String VERSION = "1.0.0";
    private static final String PROGRAM_NAME = "AEP Data Exporter";
    private static final String COMPANY_NAME = "ZCT (众成通信)";

    // LOG-001修复: 防止重复初始化日志输出
    private static volatile boolean initLogPrinted = false;

    private ConfigService configService;
    private ExportService exportService;
    private ErrorHandler errorHandler;
    private ExportResult lastExportResult;
    private final SimpleDateFormat logTimeFormat;
    private final LogManager logger;

    /**
     * 构造函数
     * 实现: DM-017-01 - 主程序初始化
     * 修复: MOCK-001-A-1.2 - 添加测试模式检测
     */
    public AepDataExporter() {
        this.logTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.logger = LogManager.getInstance();

        // LOG-001修复: 只在第一次初始化时打印日志
        if (!initLogPrinted) {
            synchronized (AepDataExporter.class) {
                if (!initLogPrinted) {
                    logger.info("AepDataExporter", "AEP Data Exporter " + VERSION + " 初始化完成");
                    // MOCK-001-A: 检测并记录测试模式状态
                    detectAndLogTestMode();
                    initLogPrinted = true;
                }
            }
        }
    }

    /**
     * 程序主入口
     * 实现: DM-017-02 - 命令行入口
     */
    public static void main(String[] args) {
        AepDataExporter exporter = new AepDataExporter();
        int exitCode = exporter.run(args);
        System.exit(exitCode);
    }

    /**
     * 运行程序的核心方法
     * 实现: DM-017-03 - 程序运行逻辑
     */
    public int run(String[] args) {
        try {
            // 解析命令行参数
            CommandLineOptions options = parseCommandLine(args);

            // 处理特殊命令
            if (options.showHelp) {
                showHelp();
                return 0;
            }

            if (options.showVersion) {
                showVersion();
                return 0;
            }

            // 初始化配置
            int configResult = initializeConfiguration(options);
            if (configResult != 0) {
                return configResult;
            }

            // 执行导出操作
            return executeExport(options);

        } catch (Exception e) {
            logError("程序执行失败", e);
            return 99; // 未知错误
        }
    }

    /**
     * 导出所有数据（API方法）
     * 实现: DM-017-04 - 完整导出API
     */
    public ExportResult exportAll() {
        try {
            if (exportService == null) {
                ExportConfig defaultConfig = loadDefaultConfiguration();
                if (defaultConfig == null) {
                    return createFailureResult("配置加载失败");
                }
                exportService = new ExportService(defaultConfig);
            }

            lastExportResult = exportService.exportAllData();
            return lastExportResult;

        } catch (Exception e) {
            logError("导出所有数据失败", e);
            return createFailureResult("导出失败: " + e.getMessage());
        }
    }

    /**
     * 使用自定义配置导出所有数据
     * 实现: DM-017-05 - 自定义配置导出
     */
    public ExportResult exportAllWithConfig(ExportConfig config) {
        try {
            exportService = new ExportService(config);
            lastExportResult = exportService.exportAllData();
            return lastExportResult;

        } catch (Exception e) {
            logError("自定义配置导出失败", e);
            return createFailureResult("导出失败: " + e.getMessage());
        }
    }

    /**
     * 验证配置有效性
     * 实现: DM-017-06 - 配置验证
     */
    public boolean validateConfiguration(ExportConfig config) {
        try {
            if (config == null) {
                return false;
            }

            // 验证必需字段
            if (config.getAppKey() == null || config.getAppKey().trim().isEmpty()) {
                return false;
            }

            if (config.getAppSecret() == null || config.getAppSecret().trim().isEmpty()) {
                return false;
            }

            if (config.getApiHost() == null || config.getApiHost().trim().isEmpty()) {
                return false;
            }

            if (config.getAppId() == null || config.getAppId().trim().isEmpty()) {
                return false;
            }

            // 验证格式参数
            String format = config.getExportFormat();
            if (format != null && !format.equalsIgnoreCase("JSON") && !format.equalsIgnoreCase("CSV")) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logError("配置验证失败", e);
            return false;
        }
    }

    /**
     * 验证输出文件是否存在
     * 实现: DM-017-07 - 文件验证
     */
    public boolean verifyOutputFiles(List<String> filePaths) {
        try {
            if (filePaths == null || filePaths.isEmpty()) {
                return false;
            }

            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (!file.exists() || file.length() == 0) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logError("文件验证失败", e);
            return false;
        }
    }

    /**
     * 获取最后一次导出结果
     * 实现: DM-017-08 - 结果获取
     */
    public ExportResult getLastExportResult() {
        return lastExportResult;
    }

    // 私有辅助方法

    /**
     * 解析命令行参数
     */
    private CommandLineOptions parseCommandLine(String[] args) {
        CommandLineOptions options = new CommandLineOptions();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help":
                case "-h":
                    options.showHelp = true;
                    break;
                case "--version":
                case "-v":
                    options.showVersion = true;
                    break;
                case "--export-all":
                    options.exportAll = true;
                    break;
                case "--export-products":
                    options.exportProductsOnly = true;
                    break;
                case "--export-devices":
                    options.exportDevicesOnly = true;
                    break;
                case "--product-id":
                    if (i + 1 < args.length) {
                        options.productId = Long.parseLong(args[++i]);
                    }
                    break;
                case "--format":
                    if (i + 1 < args.length) {
                        options.format = args[++i];
                    }
                    break;
                case "--output-dir":
                    if (i + 1 < args.length) {
                        options.outputDirectory = args[++i];
                    }
                    break;
                case "--enable-backup":
                    options.enableBackup = true;
                    break;
                case "--debug":
                    options.enableDebug = true;
                    break;
                default:
                    if (arg.startsWith("--")) {
                        options.hasInvalidOptions = true;
                    }
                    break;
            }
        }

        return options;
    }

    /**
     * 初始化配置
     */
    private int initializeConfiguration(CommandLineOptions options) {
        try {
            // 加载配置服务
            configService = new ConfigService();

            // 构建配置
            ExportConfig config = buildConfiguration(options);

            if (config == null) {
                logError("环境变量配置不完整", null);
                System.err.println("错误：缺少必需的环境变量，请设置：");
                System.err.println("  AEP_APP_KEY - AEP应用密钥");
                System.err.println("  AEP_APP_SECRET - AEP应用秘钥");
                System.err.println("  AEP_API_HOST - AEP API主机");
                System.err.println("  AEP_APP_ID - AEP应用ID");
                return 1; // 配置错误
            }

            // 验证配置
            if (!validateConfiguration(config)) {
                logError("配置验证失败", null);
                System.err.println("错误：配置参数无效");
                return 2; // 配置无效
            }

            // 初始化服务
            exportService = new ExportService(config);
            errorHandler = new ErrorHandler(config);

            if (options.enableDebug) {
                logInfo("配置初始化完成");
                logInfo("API主机: " + maskSensitive(config.getApiHost()));
                logInfo("导出格式: " + config.getExportFormat());
            }

            return 0;

        } catch (Exception e) {
            logError("配置初始化失败", e);
            return 2; // 配置错误
        }
    }

    /**
     * 构建配置对象
     */
    private ExportConfig buildConfiguration(CommandLineOptions options) {
        try {
            // 从环境变量获取基本配置 - 环境变量优先于系统属性
            // 修复 CONFIG-001: 确保环境变量优先级高于系统属性，避免测试污染
            String appKey = getConfigValue("AEP_APP_KEY");
            String appSecret = getConfigValue("AEP_APP_SECRET");
            String apiHost = getConfigValue("AEP_API_HOST");
            String appId = getConfigValue("AEP_APP_ID");

            // 检查必需参数
            if (appKey == null || appSecret == null || apiHost == null || appId == null) {
                return null;
            }

            // 构建配置
            ExportConfig.Builder configBuilder = ExportConfig.builder()
                .appKey(appKey)
                .appSecret(appSecret)
                .apiHost(apiHost)
                .appId(appId)
                .maxRetries(3)
                .timeoutSeconds(30)
                .pageSize(20);

            // 应用命令行选项
            if (options.format != null) {
                configBuilder.exportFormat(options.format.toUpperCase());
            } else {
                configBuilder.exportFormat("JSON");
            }

            if (options.outputDirectory != null) {
                configBuilder.outputDirectory(options.outputDirectory);
            } else {
                configBuilder.outputDirectory("./output");
            }

            configBuilder.createBackup(options.enableBackup);
            configBuilder.enableDebugLog(options.enableDebug);

            // 设置格式相关参数
            if ("CSV".equalsIgnoreCase(options.format)) {
                configBuilder.csvWithHeader(true);
                configBuilder.csvSeparator(",");
            } else {
                configBuilder.jsonIndented(true);
            }

            return configBuilder.build();

        } catch (Exception e) {
            logError("配置构建失败", e);
            return null;
        }
    }

    /**
     * 加载默认配置
     */
    private ExportConfig loadDefaultConfiguration() {
        CommandLineOptions defaultOptions = new CommandLineOptions();
        defaultOptions.format = "JSON";
        defaultOptions.outputDirectory = "./output";
        return buildConfiguration(defaultOptions);
    }

    /**
     * 执行导出操作
     */
    private int executeExport(CommandLineOptions options) {
        try {
            // 检查无效选项
            if (options.hasInvalidOptions) {
                System.err.println("错误：无效的命令行选项");
                showHelp();
                return 3; // 参数错误
            }

            ExportResult result = null;

            // 执行相应的导出操作
            if (options.exportAll) {
                logInfo("开始导出所有数据...");
                result = exportService.exportAllData();
            } else if (options.exportProductsOnly) {
                logInfo("开始导出产品数据...");
                result = exportService.exportProductsOnly();
            } else if (options.exportDevicesOnly) {
                if (options.productId != null) {
                    logInfo("开始导出产品 " + options.productId + " 的设备数据...");
                    result = exportService.exportDevicesForProduct(options.productId);
                } else {
                    System.err.println("错误：导出设备时必须指定 --product-id");
                    return 3; // 参数错误
                }
            } else {
                System.err.println("错误：必须指定导出操作 (--export-all, --export-products, --export-devices)");
                showHelp();
                return 3; // 参数错误
            }

            // 处理导出结果
            lastExportResult = result;
            return handleExportResult(result, options);

        } catch (Exception e) {
            logError("导出执行失败", e);
            return 4; // 执行错误
        }
    }

    /**
     * 处理导出结果
     */
    private int handleExportResult(ExportResult result, CommandLineOptions options) {
        if (result == null) {
            logError("导出结果为空", null);
            return 4;
        }

        if (result.isSuccess()) {
            logInfo("✅ 导出成功完成！");
            logInfo("导出格式: " + result.getExportFormat());
            logInfo("产品数量: " + result.getProductCount());
            logInfo("设备数量: " + result.getDeviceCount());
            logInfo("文件大小: " + formatFileSize(result.getFileSize()));

            if (!result.getFilePaths().isEmpty()) {
                logInfo("输出文件:");
                for (String filePath : result.getFilePaths()) {
                    logInfo("  - " + filePath);
                }
            }

            if (result.getBackupCreated()) {
                logInfo("备份文件: " + result.getBackupPath());
            }

            return 0; // 成功

        } else {
            logError("❌ 导出失败", null);
            logError("错误信息: " + result.getErrorMessage(), null);
            logError("错误代码: " + result.getErrorCode(), null);
            return 5; // 导出失败
        }
    }

    /**
     * 创建失败结果
     */
    private ExportResult createFailureResult(String message) {
        return ExportResult.builder()
            .taskId("FAILED_" + System.currentTimeMillis())
            .startTime(logTimeFormat.format(new Date()))
            .endTime(logTimeFormat.format(new Date()))
            .status("FAILED")
            .message(message)
            .errorCode("MAIN_001")
            .exportFormat("JSON") // 默认格式
            .build();
    }

    /**
     * 显示帮助信息
     */
    private void showHelp() {
        System.out.println(PROGRAM_NAME + " v" + VERSION + " - " + COMPANY_NAME);
        System.out.println();
        System.out.println("用法: java -jar aep-data-exporter.jar [选项]");
        System.out.println();
        System.out.println("导出选项:");
        System.out.println("  --export-all              导出所有产品和设备数据");
        System.out.println("  --export-products         仅导出产品数据");
        System.out.println("  --export-devices          仅导出设备数据（需要 --product-id）");
        System.out.println();
        System.out.println("配置选项:");
        System.out.println("  --product-id <id>         指定产品ID（导出设备时必需）");
        System.out.println("  --format <json|csv>       导出格式（默认：json）");
        System.out.println("  --output-dir <path>       输出目录（默认：./output）");
        System.out.println("  --enable-backup           创建备份文件");
        System.out.println("  --debug                   启用调试输出");
        System.out.println();
        System.out.println("其他选项:");
        System.out.println("  --help, -h                显示此帮助信息");
        System.out.println("  --version, -v             显示版本信息");
        System.out.println();
        System.out.println("环境变量:");
        System.out.println("  AEP_APP_KEY               AEP应用密钥（必需）");
        System.out.println("  AEP_APP_SECRET            AEP应用秘钥（必需）");
        System.out.println("  AEP_API_HOST              AEP API主机（必需）");
        System.out.println("  AEP_APP_ID                AEP应用ID（必需）");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 导出所有数据为JSON格式");
        System.out.println("  java -jar aep-data-exporter.jar --export-all");
        System.out.println();
        System.out.println("  # 导出所有数据为CSV格式");
        System.out.println("  java -jar aep-data-exporter.jar --export-all --format csv");
        System.out.println();
        System.out.println("  # 仅导出产品数据");
        System.out.println("  java -jar aep-data-exporter.jar --export-products");
        System.out.println();
        System.out.println("  # 导出指定产品的设备");
        System.out.println("  java -jar aep-data-exporter.jar --export-devices --product-id 12345678");
    }

    /**
     * 显示版本信息
     */
    private void showVersion() {
        System.out.println(PROGRAM_NAME + " v" + VERSION);
        System.out.println("Copyright (c) 2024 " + COMPANY_NAME);
        System.out.println("AEP (中国电信应用使能平台) 数据导出工具");
        System.out.println();
        System.out.println("构建信息:");
        System.out.println("  版本: " + VERSION);
        System.out.println("  构建时间: 2026-01-05");
        System.out.println("  Java版本: " + System.getProperty("java.version"));
        System.out.println("  操作系统: " + System.getProperty("os.name"));
    }

    /**
     * 记录信息日志
     */
    private void logInfo(String message) {
        logger.info("AepDataExporter", message);
    }

    /**
     * 记录错误日志
     */
    private void logError(String message, Exception e) {
        if (e != null) {
            logger.error("AepDataExporter", message, e);
        } else {
            logger.error("AepDataExporter", message);
        }
    }

    /**
     * 获取配置值 - 环境变量优先于系统属性
     * 修复 CONFIG-001: 实现正确的配置优先级，避免测试污染
     *
     * @param key 配置键名
     * @return 配置值，优先级：环境变量 > 系统属性 > null
     */
    private String getConfigValue(String key) {
        // 1. 优先使用环境变量
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.trim().isEmpty()) {
            logger.debug("AepDataExporter", "使用环境变量: " + key + "=" + maskSensitive(envValue));
            return envValue;
        }

        // 2. fallback到系统属性 (用于测试或特殊情况)
        String propValue = System.getProperty(key);
        if (propValue != null && !propValue.trim().isEmpty()) {
            // CONFIG-002修复: 检测构建环境，避免Maven测试阶段的误报
            boolean isMavenTest = System.getProperty("basedir") != null ||
                                 System.getProperty("maven.home") != null ||
                                 System.getProperty("surefire.real.class.path") != null;

            // MOCK-001-A: 检测并警告可能的测试污染（但排除Maven构建环境和开关控制）
            if (!isMavenTest && isPollutionDetectionEnabled() &&
                (propValue.contains("mock") || propValue.contains("test"))) {
                logger.warn("AepDataExporter", "⚠️ 检测到可能的测试配置污染: " + key + "=" + propValue);
                logger.warn("AepDataExporter", "建议检查测试代码是否正确清理了系统属性");
                logger.warn("AepDataExporter", "可通过设置 ENABLE_POLLUTION_DETECTION=false 暂时禁用此检测");
            }
            logger.debug("AepDataExporter", "使用系统属性: " + key + "=" + maskSensitive(propValue));
            return propValue;
        }

        // 3. 都没有则返回null
        logger.debug("AepDataExporter", "配置项未找到: " + key);
        return null;
    }

    /**
     * MOCK-001-A: 检测并记录测试模式状态
     * 实现: MOCK-001-A-1.2 - 测试模式检测机制
     */
    private void detectAndLogTestMode() {
        String testMode = System.getenv("AEP_TEST_MODE");
        String mockEnabled = System.getenv("ENABLE_MOCK_TESTS");
        boolean pollutionDetection = !"false".equals(System.getenv("ENABLE_POLLUTION_DETECTION"));

        if (testMode != null) {
            switch (testMode.toLowerCase()) {
                case "real":
                    logger.info("AepDataExporter", "🔧 测试模式: REAL - 使用真实AEP API");
                    break;
                case "mock":
                    logger.info("AepDataExporter", "🧪 测试模式: MOCK - 使用Mock配置");
                    break;
                case "skip":
                    logger.info("AepDataExporter", "⏭️  测试模式: SKIP - 跳过AEP API测试");
                    break;
                default:
                    logger.warn("AepDataExporter", "⚠️ 未知的测试模式: " + testMode + "，使用默认模式");
            }
        }

        if ("false".equals(mockEnabled)) {
            logger.info("AepDataExporter", "🚫 Mock测试已禁用");
        }

        if (!pollutionDetection) {
            logger.info("AepDataExporter", "🔓 配置污染检测已关闭");
        }
    }

    /**
     * MOCK-001-A: 检查是否应该跳过Mock相关测试
     * 实现: MOCK-001-A-1.3 - 测试模式判断
     */
    public static boolean shouldSkipMockTests() {
        String testMode = System.getenv("AEP_TEST_MODE");
        String mockEnabled = System.getenv("ENABLE_MOCK_TESTS");

        // 如果明确禁用Mock测试
        if ("false".equals(mockEnabled)) {
            return true;
        }

        // 如果测试模式不是mock
        if (testMode != null && !"mock".equals(testMode.toLowerCase())) {
            return true;
        }

        return false;
    }

    /**
     * MOCK-001-A: 检查是否启用配置污染检测
     * 实现: MOCK-001-A-1.4 - 污染检测控制
     */
    public static boolean isPollutionDetectionEnabled() {
        return !"false".equals(System.getenv("ENABLE_POLLUTION_DETECTION"));
    }

    /**
     * 脱敏敏感信息
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 3);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * 命令行选项类
     */
    private static class CommandLineOptions {
        boolean showHelp = false;
        boolean showVersion = false;
        boolean exportAll = false;
        boolean exportProductsOnly = false;
        boolean exportDevicesOnly = false;
        Long productId = null;
        String format = null;
        String outputDirectory = null;
        boolean enableBackup = false;
        boolean enableDebug = false;
        boolean hasInvalidOptions = false;
    }
}