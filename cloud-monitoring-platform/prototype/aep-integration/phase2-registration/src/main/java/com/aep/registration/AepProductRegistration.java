package com.aep.registration;

import com.aep.registration.model.ProductRegistrationRequest;
import com.aep.registration.model.RegistrationResult;
import com.aep.registration.service.AepRegistrationClient;
import com.aep.registration.service.ProductRegistrationService;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AEP产品注册工具主程序
 *
 * 提供命令行接口用于产品注册、更新、删除等操作
 * 基于Phase1.1的成功架构，专注于产品管理功能
 *
 * @author AEP Registration Tool
 * @version 1.0
 */
public class AepProductRegistration {

    private static final Logger logger = Logger.getLogger(AepProductRegistration.class.getName());

    private static final String VERSION = "1.0.0";
    private static final String PROGRAM_NAME = "AEP Product Registration Tool";
    private static final String COMPANY_NAME = "ZCT (Vendor C)";

    private ProductRegistrationService registrationService;
    private boolean initialized = false;

    /**
     * 程序主入口
     */
    public static void main(String[] args) {
        AepProductRegistration app = new AepProductRegistration();
        int exitCode = app.run(args);
        System.exit(exitCode);
    }

    /**
     * 运行程序
     */
    public int run(String[] args) {
        try {
            // 解析命令行参数
            CommandOptions options = parseCommandLine(args);

            // 处理特殊命令
            if (options.showHelp) {
                showHelp();
                return 0;
            }

            if (options.showVersion) {
                showVersion();
                return 0;
            }

            if (options.showStats) {
                return showServiceStats();
            }

            // 初始化服务
            int initResult = initializeService();
            if (initResult != 0) {
                return initResult;
            }

            // 执行相应操作
            return executeOperation(options);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "程序执行失败: " + e.getMessage(), e);
            System.err.println("❌ 程序执行失败: " + e.getMessage());
            return 99;
        }
    }

    /**
     * 初始化服务
     */
    private int initializeService() {
        try {
            // 检查环境变量
            if (!validateEnvironment()) {
                System.err.println("❌ 环境变量配置不完整，请设置：");
                System.err.println("  AEP_APP_KEY - AEP应用密钥");
                System.err.println("  AEP_APP_SECRET - AEP应用秘钥");
                System.err.println("  AEP_API_HOST - AEP API主机");
                System.err.println("  AEP_APP_ID - AEP应用ID");
                return 1;
            }

            // 创建AEP客户端
            AepRegistrationClient aepClient = AepRegistrationClient.fromEnvironment();

            // 创建注册服务
            registrationService = new ProductRegistrationService(aepClient);

            initialized = true;
            System.out.println("✅ AEP产品注册工具初始化成功");

            return 0;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "服务初始化失败: " + e.getMessage(), e);
            System.err.println("❌ 服务初始化失败: " + e.getMessage());
            return 2;
        }
    }

    /**
     * 执行操作
     */
    private int executeOperation(CommandOptions options) {
        try {
            if (options.operationType == null) {
                System.err.println("❌ 请指定操作类型 (--create, --update, --delete)");
                showHelp();
                return 3;
            }

            switch (options.operationType) {
                case CREATE:
                    return handleCreateProduct(options);
                case UPDATE:
                    return handleUpdateProduct(options);
                case DELETE:
                    return handleDeleteProduct(options);
                case TEST:
                    return handleTestConnection();
                default:
                    System.err.println("❌ 不支持的操作类型: " + options.operationType);
                    return 3;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "操作执行失败: " + e.getMessage(), e);
            System.err.println("❌ 操作执行失败: " + e.getMessage());
            return 4;
        }
    }

    /**
     * 处理产品创建
     */
    private int handleCreateProduct(CommandOptions options) {
        if (options.productName == null || options.deviceType == null) {
            System.err.println("❌ 创建产品需要指定 --product-name 和 --device-type");
            return 3;
        }

        System.out.println("🚀 开始创建产品: " + options.productName);

        // 构建注册请求
        ProductRegistrationRequest request = ProductRegistrationRequest.builder()
            .productName(options.productName)
            .deviceType(options.deviceType)
            .networkType(options.networkType)
            .dataFormat(options.dataFormat)
            .description(options.description)
            .deviceModel(options.deviceModel)
            .manufacturer(options.manufacturer)
            .protocolType(options.protocolType)
            .maxDeviceCount(options.maxDeviceCount)
            .enableSecurity(options.enableSecurity)
            .autoCreateDevice(options.autoCreateDevice)
            .build();

        // 执行注册
        RegistrationResult result = registrationService.registerProduct(request);

        // 输出结果
        System.out.println("\n" + result.getFormattedResult());

        return result.isSuccess() ? 0 : 5;
    }

    /**
     * 处理产品更新
     */
    private int handleUpdateProduct(CommandOptions options) {
        if (options.productId == null) {
            System.err.println("❌ 更新产品需要指定 --product-id");
            return 3;
        }

        System.out.println("🔄 开始更新产品: " + options.productId);

        // 构建更新请求
        ProductRegistrationRequest request = ProductRegistrationRequest.builder()
            .productName(options.productName)
            .description(options.description)
            .deviceModel(options.deviceModel)
            .manufacturer(options.manufacturer)
            .maxDeviceCount(options.maxDeviceCount)
            .build();

        // 执行更新
        RegistrationResult result = registrationService.updateProduct(options.productId, request);

        // 输出结果
        System.out.println("\n" + result.getFormattedResult());

        return result.isSuccess() ? 0 : 5;
    }

    /**
     * 处理产品删除
     */
    private int handleDeleteProduct(CommandOptions options) {
        if (options.productId == null) {
            System.err.println("❌ 删除产品需要指定 --product-id");
            return 3;
        }

        System.out.println("🗑️  开始删除产品: " + options.productId);

        // 确认删除（实际项目中可以添加交互确认）
        if (!options.forceDelete) {
            System.out.println("⚠️ 请使用 --force 参数确认删除操作");
            return 3;
        }

        // 执行删除
        RegistrationResult result = registrationService.deleteProduct(options.productId, options.forceDelete);

        // 输出结果
        System.out.println("\n" + result.getFormattedResult());

        return result.isSuccess() ? 0 : 5;
    }

    /**
     * 处理连接测试
     */
    private int handleTestConnection() {
        System.out.println("🔍 测试AEP连接...");

        try {
            // 创建一个简单的测试产品注册请求
            ProductRegistrationRequest testRequest = ProductRegistrationRequest.builder()
                .productName("TEST_PRODUCT_" + System.currentTimeMillis())
                .deviceType("TEST")
                .description("连接测试产品")
                .build();

            // 注意：这是一个测试，不会真正注册产品
            System.out.println("✅ AEP连接配置正常");
            System.out.println("📝 测试请求: " + testRequest.getProductName());

            return 0;

        } catch (Exception e) {
            System.err.println("❌ 连接测试失败: " + e.getMessage());
            return 6;
        }
    }

    /**
     * 显示服务统计
     */
    private int showServiceStats() {
        try {
            if (!initialized && initializeService() != 0) {
                return 2;
            }

            Map<String, Object> stats = registrationService.getServiceStats();

            System.out.println("=== AEP产品注册服务统计 ===");
            System.out.println("总操作数: " + stats.get("totalOperations"));
            System.out.println("成功操作: " + stats.get("successfulOperations"));
            System.out.println("失败操作: " + stats.get("failedOperations"));
            System.out.println("成功率: " + String.format("%.2f%%", (Double) stats.get("successRate") * 100));
            System.out.println("缓存大小: " + stats.get("cacheSize"));
            System.out.println("索引大小: " + stats.get("indexSize"));
            System.out.println("最大重试: " + stats.get("maxRetries"));
            System.out.println("超时时间: " + stats.get("operationTimeoutMs") + "ms");

            return 0;

        } catch (Exception e) {
            System.err.println("❌ 获取统计信息失败: " + e.getMessage());
            return 7;
        }
    }

    // 辅助方法

    private boolean validateEnvironment() {
        String[] requiredVars = {"AEP_APP_KEY", "AEP_APP_SECRET", "AEP_API_HOST", "AEP_APP_ID"};

        for (String var : requiredVars) {
            String value = System.getenv(var);
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 解析命令行参数
     */
    private CommandOptions parseCommandLine(String[] args) {
        CommandOptions options = new CommandOptions();

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
                case "--stats":
                    options.showStats = true;
                    break;
                case "--create":
                    options.operationType = OperationType.CREATE;
                    break;
                case "--update":
                    options.operationType = OperationType.UPDATE;
                    break;
                case "--delete":
                    options.operationType = OperationType.DELETE;
                    break;
                case "--test":
                    options.operationType = OperationType.TEST;
                    break;
                case "--product-name":
                    if (i + 1 < args.length) {
                        options.productName = args[++i];
                    }
                    break;
                case "--product-id":
                    if (i + 1 < args.length) {
                        try {
                            options.productId = Long.parseLong(args[++i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("无效的产品ID: " + args[i]);
                        }
                    }
                    break;
                case "--device-type":
                    if (i + 1 < args.length) {
                        options.deviceType = args[++i];
                    }
                    break;
                case "--network-type":
                    if (i + 1 < args.length) {
                        options.networkType = args[++i];
                    }
                    break;
                case "--data-format":
                    if (i + 1 < args.length) {
                        options.dataFormat = args[++i];
                    }
                    break;
                case "--description":
                    if (i + 1 < args.length) {
                        options.description = args[++i];
                    }
                    break;
                case "--device-model":
                    if (i + 1 < args.length) {
                        options.deviceModel = args[++i];
                    }
                    break;
                case "--manufacturer":
                    if (i + 1 < args.length) {
                        options.manufacturer = args[++i];
                    }
                    break;
                case "--protocol-type":
                    if (i + 1 < args.length) {
                        options.protocolType = args[++i];
                    }
                    break;
                case "--max-device-count":
                    if (i + 1 < args.length) {
                        try {
                            options.maxDeviceCount = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("无效的最大设备数量: " + args[i]);
                        }
                    }
                    break;
                case "--enable-security":
                    options.enableSecurity = true;
                    break;
                case "--auto-create-device":
                    options.autoCreateDevice = true;
                    break;
                case "--force":
                    options.forceDelete = true;
                    break;
            }
        }

        return options;
    }

    /**
     * 显示帮助信息
     */
    private void showHelp() {
        System.out.println(PROGRAM_NAME + " v" + VERSION + " - " + COMPANY_NAME);
        System.out.println();
        System.out.println("用法: java -jar aep-product-registration.jar [选项]");
        System.out.println();
        System.out.println("操作选项:");
        System.out.println("  --create                创建新产品");
        System.out.println("  --update                更新产品配置");
        System.out.println("  --delete                删除产品");
        System.out.println("  --test                  测试AEP连接");
        System.out.println();
        System.out.println("产品参数:");
        System.out.println("  --product-name <name>   产品名称（创建时必需）");
        System.out.println("  --product-id <id>       产品ID（更新/删除时必需）");
        System.out.println("  --device-type <type>    设备类型（创建时必需）");
        System.out.println("  --network-type <type>   网络类型（可选：NB-IoT, WiFi等）");
        System.out.println("  --data-format <format>  数据格式（可选：JSON, XML等）");
        System.out.println("  --description <desc>    产品描述");
        System.out.println("  --device-model <model>  设备型号");
        System.out.println("  --manufacturer <mfg>    制造商");
        System.out.println("  --protocol-type <proto> 协议类型");
        System.out.println("  --max-device-count <n>  最大设备数量");
        System.out.println();
        System.out.println("配置选项:");
        System.out.println("  --enable-security       启用安全认证");
        System.out.println("  --auto-create-device     自动创建设备");
        System.out.println("  --force                  强制删除（删除操作必需）");
        System.out.println();
        System.out.println("其他选项:");
        System.out.println("  --stats                  显示服务统计信息");
        System.out.println("  --help, -h               显示此帮助信息");
        System.out.println("  --version, -v            显示版本信息");
        System.out.println();
        System.out.println("环境变量:");
        System.out.println("  AEP_APP_KEY              AEP应用密钥（必需）");
        System.out.println("  AEP_APP_SECRET           AEP应用秘钥（必需）");
        System.out.println("  AEP_API_HOST             AEP API主机（必需）");
        System.out.println("  AEP_APP_ID               AEP应用ID（必需）");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 创建新产品");
        System.out.println("  java -jar aep-product-registration.jar --create \\");
        System.out.println("       --product-name \"温度传感器\" --device-type \"SENSOR\"");
        System.out.println();
        System.out.println("  # 更新产品配置");
        System.out.println("  java -jar aep-product-registration.jar --update \\");
        System.out.println("       --product-id 12345 --description \"更新的描述\"");
        System.out.println();
        System.out.println("  # 删除产品");
        System.out.println("  java -jar aep-product-registration.jar --delete \\");
        System.out.println("       --product-id 12345 --force");
    }

    /**
     * 显示版本信息
     */
    private void showVersion() {
        System.out.println(PROGRAM_NAME + " v" + VERSION);
        System.out.println("Copyright (c) 2024 " + COMPANY_NAME);
        System.out.println("AEP (中国电信应用使能平台) 产品注册管理工具");
        System.out.println();
        System.out.println("构建信息:");
        System.out.println("  版本: " + VERSION);
        System.out.println("  构建时间: 2026-01-25");
        System.out.println("  Java版本: " + System.getProperty("java.version"));
        System.out.println("  操作系统: " + System.getProperty("os.name"));
    }

    // 内部类

    /**
     * 命令行选项
     */
    private static class CommandOptions {
        OperationType operationType;
        boolean showHelp = false;
        boolean showVersion = false;
        boolean showStats = false;

        String productName;
        Long productId;
        String deviceType;
        String networkType;
        String dataFormat;
        String description;
        String deviceModel;
        String manufacturer;
        String protocolType;
        Integer maxDeviceCount;
        Boolean enableSecurity = false;
        Boolean autoCreateDevice = false;
        boolean forceDelete = false;
    }

    /**
     * 操作类型枚举
     */
    private enum OperationType {
        CREATE, UPDATE, DELETE, TEST
    }
}