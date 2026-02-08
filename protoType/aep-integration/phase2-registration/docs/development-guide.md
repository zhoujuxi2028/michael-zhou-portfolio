# Phase2 开发实施指南

## 📋 文档信息
- **版本**: v1.0
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-25
- **负责人**: 云监控平台技术团队
- **状态**: 设计完成，实施指导

## 🎯 开发目标

基于完成的架构设计和集成规范，提供详细的分步实施指南，确保Phase2产品注册功能的正确、高效开发。

## 🚀 开发环境准备

### 1. 技术环境要求

```bash
# 基础环境
Java 17+                    # 与Phase1.1保持一致
Maven 3.6+                  # 依赖管理
Git 2.0+                    # 版本控制

# IDE推荐
IntelliJ IDEA 2023+         # 推荐IDE
VS Code + Java Extension    # 替代方案
```

### 2. 项目依赖确认

**复用Phase1.1的成功配置**:
```bash
# 确认Phase1.1的依赖可用
cd ../phase1.1-export
ls lib/                     # 确认AEP SDK jar文件存在
cat pom.xml                 # 查看Phase1.1的依赖配置

# 复制到Phase2
cp -r ../phase1.1-export/lib/* ./lib/
cp ../phase1.1-export/pom.xml ./pom.xml  # 作为参考
```

### 3. 环境变量配置

```bash
# 复用Phase1.1的成功配置
cp ../phase1.1-export/.env ./.env

# 验证配置
source .env
echo "AEP_APP_KEY: ${AEP_APP_KEY:0:8}***"
echo "AEP_API_HOST: $AEP_API_HOST"
```

## 📁 项目结构创建

### 1. 目录结构设置

```bash
# 创建Phase2标准目录结构
mkdir -p src/main/java/com/aep/registration/{model,service}
mkdir -p src/test/java/com/aep/registration/{model,service}
mkdir -p docs/{testing,bugs}
mkdir -p scripts
mkdir -p config
```

### 2. 文件创建清单

```bash
# Phase2核心文件清单
src/main/java/com/aep/registration/
├── AepProductRegistration.java           # 主程序入口
├── model/
│   ├── ProductRegistrationRequest.java  # 注册请求模型
│   └── RegistrationResult.java          # 操作结果模型
└── service/
    ├── AepClientManager.java            # 扩展版客户端管理器
    ├── ProductRegistrationService.java  # 业务逻辑服务
    └── LogManager.java                   # 日志管理器(复用Phase1.1)
```

## 🔧 分阶段开发计划

### 阶段1: MVP原型 (Day 1-2)

#### 1.1 复制Phase1.1基础代码

```bash
# 复制Phase1.1的成功组件
cp ../phase1.1-export/src/main/java/com/aep/export/service/AepClientManager.java \
   ./src/main/java/com/aep/registration/service/AepClientManager.java

cp ../phase1.1-export/src/main/java/com/aep/export/service/LogManager.java \
   ./src/main/java/com/aep/registration/service/LogManager.java

cp ../phase1.1-export/src/main/java/com/aep/export/model/ExportConfig.java \
   ./src/main/java/com/aep/registration/model/ExportConfig.java
```

#### 1.2 AepClientManager最小扩展

**第一步**: 添加包导入
```java
// 在AepClientManager.java中添加新的导入
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductResponse;
```

**第二步**: 添加createProduct方法
```java
// 在AepClientManager类中添加方法
public String createProduct(Map<String, Object> params) {
    try {
        // 1. 参数验证
        validateCreateParams(params);

        // 2. 构建请求
        CreateProductRequest request = new CreateProductRequest();
        request.setProductName((String) params.get("productName"));
        request.setProductType((Integer) params.getOrDefault("productType", 1));
        request.setDataFormat((Integer) params.getOrDefault("dataFormat", 1));

        // 3. 调用AEP SDK
        CreateProductResponse response = productClient.CreateProduct(request);

        // 4. 解析响应
        return parseCreateResponse(response);

    } catch (Exception e) {
        LogManager.getInstance().error("产品创建", "AepClientManager",
            "❌ Create product failed: " + e.getMessage());
        throw new AepClientException("Failed to create product: " + e.getMessage());
    }
}
```

**第三步**: 添加辅助方法
```java
// 参数验证方法
private void validateCreateParams(Map<String, Object> params) {
    if (params == null || !params.containsKey("productName") ||
        params.get("productName") == null) {
        throw new AepClientException("productName is required");
    }

    String productName = (String) params.get("productName");
    if (productName.trim().isEmpty()) {
        throw new AepClientException("productName cannot be empty");
    }
}

// 响应解析方法
private String parseCreateResponse(CreateProductResponse response) {
    if (response != null) {
        String responseStr = response.toString();
        LogManager.getInstance().info("产品创建", "AepClientManager",
            "✅ Create product success: " + responseStr);
        return responseStr;
    } else {
        throw new AepClientException("Empty response from AEP CreateProduct API");
    }
}
```

#### 1.3 创建简单测试类

```java
// src/test/java/com/aep/registration/service/AepClientManagerTest.java
public class AepClientManagerTest {
    private AepClientManager client;

    @Before
    public void setUp() {
        ExportConfig config = ExportConfig.fromEnvironment();
        client = new AepClientManager(config);
    }

    @Test
    public void testCreateProduct() {
        Map<String, Object> params = new HashMap<>();
        params.put("productName", "MVP测试产品");
        params.put("productType", 1);
        params.put("dataFormat", 1);

        // 这应该不抛出异常
        assertDoesNotThrow(() -> {
            String result = client.createProduct(params);
            assertNotNull(result);
            System.out.println("创建结果: " + result);
        });
    }
}
```

#### 1.4 MVP验证步骤

```bash
# 1. 编译测试
mvn clean compile

# 2. 运行基础测试
mvn test -Dtest=AepClientManagerTest

# 3. 验证Phase1.1功能无影响
mvn test -Dtest=*QueryTest

# 4. 手动功能验证
java -cp "target/classes:lib/*" \
  com.aep.registration.service.AepClientManagerTest
```

### 阶段2: 完整功能实现 (Day 2-3)

#### 2.1 添加UpdateProduct和DeleteProduct

```java
// updateProduct方法实现
public String updateProduct(Map<String, Object> params) {
    try {
        validateUpdateParams(params);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setProductId(((Number) params.get("productId")).longValue());

        if (params.containsKey("productName")) {
            request.setProductName((String) params.get("productName"));
        }
        if (params.containsKey("description")) {
            request.setDescription((String) params.get("description"));
        }

        UpdateProductResponse response = productClient.UpdateProduct(request);
        return parseUpdateResponse(response);

    } catch (Exception e) {
        throw new AepClientException("Failed to update product: " + e.getMessage());
    }
}

// deleteProduct方法实现
public String deleteProduct(Map<String, Object> params) {
    try {
        validateDeleteParams(params);

        DeleteProductRequest request = new DeleteProductRequest();
        request.setProductId(((Number) params.get("productId")).longValue());

        DeleteProductResponse response = productClient.DeleteProduct(request);
        return parseDeleteResponse(response);

    } catch (Exception e) {
        throw new AepClientException("Failed to delete product: " + e.getMessage());
    }
}
```

#### 2.2 创建业务服务层

```java
// src/main/java/com/aep/registration/service/ProductRegistrationService.java
public class ProductRegistrationService {
    private final AepClientManager aepClient;
    private final OperationMetrics metrics;

    public ProductRegistrationService(AepClientManager aepClient) {
        this.aepClient = aepClient;
        this.metrics = new OperationMetrics();
    }

    public RegistrationResult registerProduct(ProductRegistrationRequest request) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;

        try {
            // 转换为AepClientManager参数格式
            Map<String, Object> params = request.toParameterMap();

            // 调用底层API
            String response = aepClient.createProduct(params);

            // 解析结果
            RegistrationResult result = parseRegistrationResponse(response, "create");
            success = result.isSuccess();
            return result;

        } catch (Exception e) {
            errorMessage = e.getMessage();
            return RegistrationResult.builder()
                .operation("create")
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordOperation("create", success, duration);
            auditOperation("create", request.getProductName(), success, errorMessage, duration);
        }
    }
}
```

#### 2.3 创建数据模型类

```java
// src/main/java/com/aep/registration/model/ProductRegistrationRequest.java
public class ProductRegistrationRequest {
    private String productName;
    private Integer productType;
    private Integer dataFormat;
    private String description;
    private Integer industryId;

    // Builder模式实现
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductRegistrationRequest request = new ProductRegistrationRequest();

        public Builder productName(String productName) {
            request.productName = productName;
            return this;
        }

        public Builder productType(Integer productType) {
            request.productType = productType;
            return this;
        }

        // 其他builder方法...

        public ProductRegistrationRequest build() {
            validate();
            return request;
        }

        private void validate() {
            if (request.productName == null || request.productName.trim().isEmpty()) {
                throw new IllegalArgumentException("Product name is required");
            }
        }
    }

    // 转换为API参数
    public Map<String, Object> toParameterMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("productName", productName);
        params.put("productType", productType != null ? productType : 1);
        params.put("dataFormat", dataFormat != null ? dataFormat : 1);

        if (description != null) {
            params.put("description", description);
        }
        if (industryId != null) {
            params.put("industryId", industryId);
        }

        return params;
    }
}
```

### 阶段3: 命令行接口 (Day 3-4)

#### 3.1 主程序入口实现

```java
// src/main/java/com/aep/registration/AepProductRegistration.java
public class AepProductRegistration {
    public static void main(String[] args) {
        try {
            CommandOptions options = parseCommandLine(args);

            // 初始化服务
            ExportConfig config = ExportConfig.fromEnvironment();
            AepClientManager aepClient = new AepClientManager(config);
            ProductRegistrationService service = new ProductRegistrationService(aepClient);

            // 执行操作
            switch (options.getOperation()) {
                case CREATE:
                    handleCreateOperation(service, options);
                    break;
                case UPDATE:
                    handleUpdateOperation(service, options);
                    break;
                case DELETE:
                    handleDeleteOperation(service, options);
                    break;
                case TEST:
                    handleTestOperation(aepClient);
                    break;
                default:
                    showUsage();
            }

        } catch (Exception e) {
            System.err.println("❌ 操作失败: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleCreateOperation(ProductRegistrationService service, CommandOptions options) {
        ProductRegistrationRequest request = ProductRegistrationRequest.builder()
            .productName(options.getProductName())
            .productType(options.getProductType())
            .dataFormat(options.getDataFormat())
            .description(options.getDescription())
            .build();

        RegistrationResult result = service.registerProduct(request);

        if (result.isSuccess()) {
            System.out.println("✅ 产品创建成功!");
            System.out.println("产品ID: " + result.getProductId());
            if (result.getMasterKey() != null) {
                System.out.println("主密钥: " + result.getMasterKey());
            }
        } else {
            System.err.println("❌ 产品创建失败: " + result.getErrorMessage());
            System.exit(1);
        }
    }
}
```

#### 3.2 命令行脚本创建

```bash
# scripts/register_product.sh
#!/bin/bash

set -e

# 检查参数
if [ $# -lt 2 ]; then
    echo "用法: $0 <产品名称> <设备类型> [其他选项...]"
    echo "示例: $0 '温度传感器' SENSOR"
    exit 1
fi

PRODUCT_NAME="$1"
DEVICE_TYPE="$2"
shift 2

# 设置默认值
PRODUCT_TYPE=1
DATA_FORMAT=1
DESCRIPTION=""

# 解析其他参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --description)
            DESCRIPTION="$2"
            shift 2
            ;;
        --product-type)
            PRODUCT_TYPE="$2"
            shift 2
            ;;
        --data-format)
            DATA_FORMAT="$2"
            shift 2
            ;;
        *)
            echo "未知参数: $1"
            exit 1
            ;;
    esac
done

# 加载环境变量
if [ -f .env ]; then
    source .env
fi

# 执行创建命令
echo "正在创建产品: $PRODUCT_NAME"
java -cp "target/classes:lib/*" \
  com.aep.registration.AepProductRegistration \
  --create \
  --product-name "$PRODUCT_NAME" \
  --product-type "$PRODUCT_TYPE" \
  --data-format "$DATA_FORMAT" \
  ${DESCRIPTION:+--description "$DESCRIPTION"}

echo "产品创建完成"
```

### 阶段4: 集成测试与验证 (Day 4-5)

#### 4.1 集成测试套件

```java
// src/test/java/com/aep/registration/integration/FullIntegrationTest.java
public class FullIntegrationTest {

    @Test
    public void testProductLifecycle() {
        // 确保有真实的AEP环境配置
        assumeTrue("需要真实AEP配置", hasRealAepConfig());

        ExportConfig config = ExportConfig.fromEnvironment();
        AepClientManager client = new AepClientManager(config);
        ProductRegistrationService service = new ProductRegistrationService(client);

        String testProductName = "集成测试产品_" + System.currentTimeMillis();
        Long productId = null;

        try {
            // 1. 创建产品
            ProductRegistrationRequest createRequest = ProductRegistrationRequest.builder()
                .productName(testProductName)
                .productType(1)
                .dataFormat(1)
                .description("自动化集成测试产品")
                .build();

            RegistrationResult createResult = service.registerProduct(createRequest);
            assertTrue("产品创建应该成功", createResult.isSuccess());
            assertNotNull("应该返回产品ID", createResult.getProductId());

            productId = createResult.getProductId();
            System.out.println("✅ 产品创建成功 - ID: " + productId);

            // 2. 更新产品
            RegistrationResult updateResult = service.updateProduct(productId,
                "更新的集成测试产品", "更新的描述");
            assertTrue("产品更新应该成功", updateResult.isSuccess());
            System.out.println("✅ 产品更新成功");

            // 3. 验证与Phase1.1的兼容性
            String queryResult = client.queryProducts(new HashMap<>());
            assertNotNull("查询功能应该正常工作", queryResult);
            assertTrue("查询结果应该包含新创建的产品",
                queryResult.contains(testProductName) || queryResult.contains(String.valueOf(productId)));
            System.out.println("✅ 与Phase1.1兼容性验证通过");

        } finally {
            // 清理测试数据
            if (productId != null) {
                try {
                    RegistrationResult deleteResult = service.deleteProduct(productId, true);
                    if (deleteResult.isSuccess()) {
                        System.out.println("✅ 测试数据清理完成");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ 测试数据清理失败: " + e.getMessage());
                }
            }
        }
    }

    private boolean hasRealAepConfig() {
        return System.getenv("AEP_APP_KEY") != null &&
               !System.getenv("AEP_APP_KEY").contains("YOUR_");
    }
}
```

#### 4.2 性能测试

```java
// src/test/java/com/aep/registration/performance/PerformanceTest.java
public class PerformanceTest {

    @Test
    public void testCreateProductPerformance() {
        ExportConfig config = ExportConfig.fromEnvironment();
        ProductRegistrationService service = new ProductRegistrationService(
            new AepClientManager(config));

        int testCount = 10;
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < testCount; i++) {
            long startTime = System.currentTimeMillis();

            ProductRegistrationRequest request = ProductRegistrationRequest.builder()
                .productName("性能测试产品_" + i + "_" + System.currentTimeMillis())
                .productType(1)
                .dataFormat(1)
                .build();

            RegistrationResult result = service.registerProduct(request);
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimes.add(responseTime);

            if (result.isSuccess()) {
                System.out.printf("测试 %d: 成功, 耗时 %dms%n", i + 1, responseTime);
            } else {
                System.printf("测试 %d: 失败, 耗时 %dms, 错误: %s%n",
                    i + 1, responseTime, result.getErrorMessage());
            }

            // 避免过快请求
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 性能分析
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0L);

        System.out.printf("%n性能统计:%n");
        System.out.printf("测试次数: %d%n", testCount);
        System.out.printf("平均响应时间: %.2fms%n", avgResponseTime);
        System.out.printf("最大响应时间: %dms%n", maxResponseTime);
        System.out.printf("最小响应时间: %dms%n", minResponseTime);

        // 性能断言
        assertTrue("平均响应时间应该小于5秒", avgResponseTime < 5000);
        assertTrue("最大响应时间应该小于10秒", maxResponseTime < 10000);
    }
}
```

## 🧪 测试策略

### 1. 分层测试结构

```
测试层次                    测试内容                    执行频率
单元测试                    类和方法级别功能             每次提交
集成测试                    AEP API真实调用             每日构建
性能测试                    响应时间和并发               每周执行
兼容性测试                  与Phase1.1协同工作          每次发布前
```

### 2. 测试环境配置

```bash
# 测试环境变量 (.env.test)
AEP_APP_KEY=test_key_here
AEP_APP_SECRET=test_secret_here
AEP_API_HOST=test_tenant.api.ctwing.cn
AEP_APP_ID=test_app_id

# 测试专用配置
AEP_TEST_MODE=true
AEP_CLEANUP_TEST_DATA=true
AEP_TEST_PRODUCT_PREFIX=AutoTest_
```

### 3. 自动化测试脚本

```bash
# scripts/test_all.sh
#!/bin/bash

echo "=== Phase2 产品注册功能全面测试 ==="

# 1. 单元测试
echo "运行单元测试..."
mvn test -Dtest=*Test || exit 1

# 2. 编译检查
echo "编译检查..."
mvn clean compile || exit 1

# 3. 集成测试(需要真实环境)
if [ "$RUN_INTEGRATION_TEST" = "true" ]; then
    echo "运行集成测试..."
    mvn test -Dtest=*IntegrationTest || exit 1
fi

# 4. 兼容性测试
echo "测试Phase1.1兼容性..."
cd ../phase1.1-export
./scripts/test.sh || exit 1
cd ../phase2-registration

# 5. 命令行功能测试
echo "测试命令行接口..."
./scripts/register_product.sh "CLI测试产品" SENSOR --description "命令行测试" || exit 1

echo "✅ 所有测试通过!"
```

## 📊 质量保证

### 1. 代码质量检查

```bash
# 代码格式检查
mvn checkstyle:check

# 静态代码分析
mvn spotbugs:check

# 测试覆盖率
mvn jacoco:report
```

### 2. 性能监控

```java
// 性能监控配置
public class PerformanceMonitor {
    private static final Logger PERF_LOGGER = LoggerFactory.getLogger("PERFORMANCE");

    public static void logOperationTime(String operation, long durationMs) {
        PERF_LOGGER.info("Operation: {}, Duration: {}ms", operation, durationMs);

        if (durationMs > 5000) {
            PERF_LOGGER.warn("Slow operation detected: {} took {}ms", operation, durationMs);
        }
    }
}
```

### 3. 错误跟踪

```java
// 错误统计和报告
public class ErrorTracker {
    private static final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();

    public static void recordError(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public static Map<String, Integer> getErrorReport() {
        return errorCounts.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().get()
            ));
    }
}
```

## 🔒 安全实施

### 1. 输入验证

```java
// 严格的输入验证
public class InputValidator {

    public static void validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("产品名称不能为空");
        }

        if (productName.length() > 64) {
            throw new IllegalArgumentException("产品名称长度不能超过64字符");
        }

        // 检查特殊字符
        if (!productName.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5_\\-\\s]+$")) {
            throw new IllegalArgumentException("产品名称包含不允许的字符");
        }
    }

    public static void sanitizeDescription(String description) {
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("描述长度不能超过255字符");
        }
    }
}
```

### 2. 敏感信息保护

```java
// 敏感信息脱敏
public class SecurityUtils {

    public static String maskSensitiveInfo(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    public static void logSafely(String operation, Map<String, Object> params) {
        Map<String, Object> safeParams = new HashMap<>(params);

        // 移除敏感字段
        safeParams.remove("masterKey");
        safeParams.remove("appSecret");

        // 脱敏关键字段
        if (safeParams.containsKey("appKey")) {
            safeParams.put("appKey", maskSensitiveInfo((String) safeParams.get("appKey")));
        }

        LogManager.getInstance().info("操作日志", "SecurityUtils",
            String.format("操作: %s, 参数: %s", operation, safeParams));
    }
}
```

## 📝 发布准备

### 1. 发布检查清单

```bash
# 发布前检查脚本
# scripts/release_check.sh

#!/bin/bash

echo "=== Phase2 发布检查清单 ==="

# 代码质量检查
echo "□ 代码格式检查"
mvn checkstyle:check || exit 1

echo "□ 静态代码分析"
mvn spotbugs:check || exit 1

echo "□ 测试覆盖率检查"
mvn jacoco:report
coverage=$(mvn jacoco:report | grep "Total.*%" | tail -1 | grep -o '[0-9]*%')
echo "测试覆盖率: $coverage"

# 功能完整性检查
echo "□ 核心功能测试"
mvn test || exit 1

echo "□ 集成测试"
if [ "$RUN_INTEGRATION_TEST" = "true" ]; then
    mvn test -Dtest=*IntegrationTest || exit 1
fi

# 文档完整性检查
echo "□ 文档检查"
[ -f README.md ] || { echo "❌ 缺少README.md"; exit 1; }
[ -f docs/architecture-design.md ] || { echo "❌ 缺少架构文档"; exit 1; }
[ -f docs/integration-specification.md ] || { echo "❌ 缺少集成规范"; exit 1; }

# 配置检查
echo "□ 配置文件检查"
[ -f .env.template ] || { echo "❌ 缺少环境变量模板"; exit 1; }

echo "✅ 发布检查通过! Phase2准备就绪"
```

### 2. 用户文档更新

```markdown
# 更新README.md的安装说明
## 快速开始

### 1. 环境配置
```bash
# 复制Phase1.1的工作配置
cp ../phase1.1-export/.env .env

# 或创建新配置
cp .env.template .env
# 编辑.env文件，填入AEP认证信息
```

### 2. 验证安装
```bash
# 测试AEP连接
java -jar aep-product-registration.jar --test

# 创建测试产品
java -jar aep-product-registration.jar --create \
  --product-name "安装测试产品" \
  --product-type 1 \
  --data-format 1
```

## 📋 实施时间安排

| 阶段 | 任务 | 预估时间 | 关键交付物 |
|-----|------|----------|-----------|
| 1 | MVP原型开发 | 1.5天 | 基础createProduct功能 |
| 2 | 完整CRUD功能 | 1天 | update/delete方法 |
| 3 | 命令行接口 | 1天 | CLI和脚本 |
| 4 | 集成测试 | 1天 | 测试套件和验证 |
| 5 | 文档完善 | 0.5天 | 用户手册和API文档 |
| **总计** | **完整实现** | **5天** | **生产就绪版本** |

## 📞 开发支持

### 1. 问题解决流程

```
遇到问题
    ↓
1. 查看相关日志文件
    ↓
2. 检查Phase1.1对应功能是否正常
    ↓
3. 验证AEP SDK方法调用
    ↓
4. 查阅集成规范文档
    ↓
5. 创建最小重现示例
    ↓
6. 寻求技术支持
```

### 2. 调试技巧

```bash
# 启用详细日志
export JAVA_OPTS="-Djava.util.logging.level=FINE"

# AEP连接测试
java -cp "target/classes:lib/*" \
  com.aep.registration.AepProductRegistration --test

# 方法调试
javap -cp lib/ag-sdk-biz-*.jar \
  com.ctg.ag.sdk.biz.aep_product_management.CreateProductRequest
```

---

**指南状态**: ✅ 完成，可开始实施
**下一步**: 开始MVP原型开发
**预期完成**: 5个工作日内交付生产就绪版本