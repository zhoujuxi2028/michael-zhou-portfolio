# Phase2 测试设计文档

## 📋 文档信息
- **版本**: v1.0
- **创建日期**: 2026-01-25
- **更新日期**: 2026-01-25
- **负责人**: 云监控平台技术团队
- **状态**: 设计完成，待评审

## 🎯 测试目标

基于Phase1.1-export的成功测试模式，设计Phase2产品注册功能的全面测试策略，确保功能正确性、性能表现和与Phase1.1的兼容性。

## 📊 测试分层策略

### 1. 测试金字塔架构

```
    🔺 E2E测试 (10%)
      ├── 完整工作流验证
      └── 真实AEP环境测试

  🔺 集成测试 (30%)
    ├── AEP SDK集成
    ├── Phase1.1兼容性
    └── API响应处理

🔺 单元测试 (60%)
  ├── 参数验证
  ├── 错误处理
  ├── 数据模型
  └── 业务逻辑
```

### 2. 测试环境策略

| 环境类型 | 用途 | AEP配置 | 数据策略 |
|---------|------|---------|----------|
| **Mock环境** | 单元测试 | 模拟配置 | 测试数据 |
| **Dev环境** | 集成测试 | 开发AEP账户 | 测试产品 |
| **Staging环境** | E2E测试 | 预生产AEP | 仿真数据 |

## 🧪 详细测试计划

### 1. 单元测试设计 (Level 1)

#### 1.1 配置管理测试
```java
// src/test/java/com/aep/registration/model/ExportConfigTest.java
public class ExportConfigTest {

    @Test
    public void testFromEnvironmentWithValidConfig() {
        // 测试环境变量正确配置
    }

    @Test
    public void testFromEnvironmentWithMissingRequired() {
        // 测试缺少必需环境变量
    }

    @Test
    public void testBuilderValidation() {
        // 测试Builder模式验证
    }

    @Test
    public void testDefaultValueApplication() {
        // 测试默认值应用
    }
}
```

#### 1.2 AepClientManager核心测试
```java
// src/test/java/com/aep/registration/service/AepClientManagerTest.java
public class AepClientManagerTest {

    // === Phase1.1兼容性测试 ===
    @Test
    public void testPhase1QueryProductsCompatibility() {
        // 验证queryProducts方法与Phase1.1完全一致
    }

    @Test
    public void testPhase1QueryDevicesCompatibility() {
        // 验证queryDevices方法与Phase1.1完全一致
    }

    // === Phase2新功能测试 ===
    @Test
    public void testCreateProductParameterValidation() {
        // 测试创建产品参数验证逻辑
    }

    @Test
    public void testCreateProductRequestBuilding() {
        // 测试CreateProductRequest构建
    }

    @Test
    public void testUpdateProductParameterValidation() {
        // 测试更新产品参数验证
    }

    @Test
    public void testDeleteProductParameterValidation() {
        // 测试删除产品参数验证
    }

    // === 错误处理测试 ===
    @Test
    public void testAepClientExceptionHandling() {
        // 测试AEP SDK异常处理
    }

    @Test
    public void testNetworkErrorHandling() {
        // 测试网络错误处理
    }

    @Test
    public void testInvalidResponseHandling() {
        // 测试无效响应处理
    }
}
```

#### 1.3 数据模型测试
```java
// src/test/java/com/aep/registration/model/ProductRegistrationRequestTest.java
public class ProductRegistrationRequestTest {

    @Test
    public void testBuilderPattern() {
        // 测试Builder模式正确性
    }

    @Test
    public void testValidationRules() {
        // 测试验证规则
    }

    @Test
    public void testParameterMapping() {
        // 测试参数映射到AEP格式
    }
}

// src/test/java/com/aep/registration/model/RegistrationResultTest.java
public class RegistrationResultTest {

    @Test
    public void testSuccessResultCreation() {
        // 测试成功结果创建
    }

    @Test
    public void testErrorResultCreation() {
        // 测试错误结果创建
    }

    @Test
    public void testResultSerialization() {
        // 测试结果序列化
    }
}
```

#### 1.4 日志管理测试
```java
// src/test/java/com/aep/registration/service/LogManagerTest.java
public class LogManagerTest {

    @Test
    public void testLogLevelFiltering() {
        // 测试日志级别过滤
    }

    @Test
    public void testAuditLogging() {
        // 测试审计日志功能
    }

    @Test
    public void testPerformanceLogging() {
        // 测试性能日志
    }

    @Test
    public void testLogRotation() {
        // 测试日志轮转
    }
}
```

### 2. 集成测试设计 (Level 2)

#### 2.1 AEP SDK集成测试
```java
// src/test/java/com/aep/registration/integration/AepSdkIntegrationTest.java
@Category(IntegrationTest.class)
public class AepSdkIntegrationTest {

    @Test
    public void testCreateProductWithRealAep() {
        // 使用真实AEP环境测试产品创建
        assumeTrue(hasRealAepConfig());
        // 创建测试产品
        // 验证响应
        // 清理测试数据
    }

    @Test
    public void testUpdateProductWithRealAep() {
        // 使用真实AEP环境测试产品更新
    }

    @Test
    public void testDeleteProductWithRealAep() {
        // 使用真实AEP环境测试产品删除
    }

    @Test
    public void testApiErrorHandling() {
        // 测试AEP API错误响应处理
    }
}
```

#### 2.2 Phase1.1兼容性集成测试
```java
// src/test/java/com/aep/registration/integration/Phase1CompatibilityTest.java
@Category(IntegrationTest.class)
public class Phase1CompatibilityTest {

    @Test
    public void testPhase1ConfigCompatibility() {
        // 测试Phase1.1配置文件在Phase2中完全可用
    }

    @Test
    public void testPhase1QueryFunctionsUnchanged() {
        // 测试Phase1.1查询功能保持不变
    }

    @Test
    public void testMixedOperations() {
        // 测试混合使用Phase1.1查询和Phase2注册功能
    }

    @Test
    public void testClientReuse() {
        // 测试客户端连接复用
    }
}
```

#### 2.3 性能集成测试
```java
// src/test/java/com/aep/registration/integration/PerformanceIntegrationTest.java
@Category(IntegrationTest.class)
public class PerformanceIntegrationTest {

    @Test
    @Timeout(5)
    public void testCreateProductResponseTime() {
        // 测试创建产品响应时间 < 5秒
    }

    @Test
    public void testConcurrentOperations() {
        // 测试并发操作性能
    }

    @Test
    public void testBatchOperations() {
        // 测试批量操作性能
    }

    @Test
    public void testMemoryUsage() {
        // 测试内存使用情况
    }
}
```

### 3. 端到端测试设计 (Level 3)

#### 3.1 完整工作流测试
```java
// src/test/java/com/aep/registration/e2e/ProductLifecycleE2ETest.java
@Category(E2ETest.class)
public class ProductLifecycleE2ETest {

    @Test
    public void testCompleteProductLifecycle() {
        // 完整的产品生命周期测试
        // 1. 创建产品
        // 2. 查询验证 (使用Phase1.1功能)
        // 3. 更新产品
        // 4. 再次查询验证
        // 5. 删除产品
        // 6. 最终验证删除成功
    }

    @Test
    public void testMultiProductScenario() {
        // 多产品场景测试
    }

    @Test
    public void testErrorRecoveryScenario() {
        // 错误恢复场景测试
    }
}
```

#### 3.2 命令行接口E2E测试
```bash
# src/test/scripts/e2e_cli_test.sh
#!/bin/bash

test_cli_create_product() {
    # 测试命令行创建产品
    local result=$(java -jar aep-registration.jar --create \
        --product-name "CLI测试产品" \
        --device-type "SENSOR")

    # 验证创建成功
    assert_contains "$result" "产品创建成功"

    # 提取产品ID用于后续测试
    PRODUCT_ID=$(echo "$result" | grep -o 'productId":[0-9]*' | cut -d: -f2)
}

test_cli_update_product() {
    # 测试命令行更新产品
    local result=$(java -jar aep-registration.jar --update \
        --product-id $PRODUCT_ID \
        --description "更新的描述")

    assert_contains "$result" "产品更新成功"
}

test_cli_delete_product() {
    # 测试命令行删除产品
    local result=$(java -jar aep-registration.jar --delete \
        --product-id $PRODUCT_ID \
        --force)

    assert_contains "$result" "产品删除成功"
}
```

## 📋 测试用例矩阵

### 1. 功能测试矩阵

| 功能模块 | 测试场景 | 预期结果 | 测试级别 |
|---------|---------|----------|----------|
| **创建产品** | 有效参数 | 创建成功 | Unit/Integration |
| | 无效产品名称 | 参数错误 | Unit |
| | 重复产品名称 | 业务错误 | Integration |
| | 超长产品名称 | 验证错误 | Unit |
| | 特殊字符 | 验证错误 | Unit |
| **更新产品** | 有效更新 | 更新成功 | Unit/Integration |
| | 产品不存在 | 未找到错误 | Integration |
| | 无更新字段 | 参数错误 | Unit |
| **删除产品** | 正常删除 | 删除成功 | Integration |
| | 强制删除 | 删除成功 | Integration |
| | 产品不存在 | 未找到错误 | Integration |
| | 有设备依赖 | 业务错误 | Integration |

### 2. 兼容性测试矩阵

| 兼容项目 | Phase1.1功能 | Phase2期望 | 测试方法 |
|---------|-------------|-----------|----------|
| **配置复用** | .env文件 | 完全兼容 | 配置测试 |
| **查询功能** | queryProducts | 不变 | 功能测试 |
| | queryDevices | 不变 | 功能测试 |
| **客户端管理** | 初始化逻辑 | 兼容扩展 | 单元测试 |
| **错误处理** | 异常体系 | 向下兼容 | 异常测试 |
| **日志格式** | 日志输出 | 格式一致 | 集成测试 |

### 3. 性能测试矩阵

| 性能指标 | 目标值 | 测试方法 | 失败阈值 |
|---------|--------|----------|----------|
| **响应时间** | < 5秒 | 实际API调用 | > 10秒 |
| **并发处理** | 10个请求/秒 | 并发测试 | < 5个/秒 |
| **内存使用** | < 256MB | JVM监控 | > 512MB |
| **错误率** | < 1% | 压力测试 | > 5% |

## 🛠️ 测试环境配置

### 1. 测试环境变量

```bash
# 测试专用环境变量
export TEST_MODE=true
export TEST_AEP_APP_KEY="test_key_here"
export TEST_AEP_APP_SECRET="test_secret_here"
export TEST_AEP_API_HOST="test.api.ctwing.cn"
export TEST_AEP_APP_ID="test_app_id"

# 测试控制变量
export RUN_INTEGRATION_TEST=false  # 是否运行集成测试
export RUN_E2E_TEST=false         # 是否运行E2E测试
export CLEANUP_TEST_DATA=true     # 是否自动清理测试数据
export TEST_TIMEOUT=30000         # 测试超时时间(ms)
```

### 2. 测试数据管理

```java
// 测试数据工厂
public class TestDataFactory {

    public static ProductRegistrationRequest createValidProductRequest() {
        return ProductRegistrationRequest.builder()
            .productName("测试产品_" + System.currentTimeMillis())
            .productType(1)
            .dataFormat(1)
            .description("自动化测试产品")
            .build();
    }

    public static ExportConfig createMockConfig() {
        return ExportConfig.builder()
            .appKey("mock_app_key")
            .appSecret("mock_app_secret")
            .apiHost("mock.api.ctwing.cn")
            .appId("mock_app_id")
            .enableProductValidation(true)
            .maxProductNameLength(64)
            .build();
    }
}
```

### 3. 测试清理策略

```java
// 测试清理管理器
public class TestCleanupManager {
    private final List<Long> createdProductIds = new ArrayList<>();

    public void registerProductForCleanup(Long productId) {
        createdProductIds.add(productId);
    }

    @After
    public void cleanup() {
        if (Boolean.parseBoolean(System.getenv("CLEANUP_TEST_DATA"))) {
            for (Long productId : createdProductIds) {
                try {
                    // 清理测试创建的产品
                    clientManager.deleteProduct(Collections.singletonMap("productId", productId));
                } catch (Exception e) {
                    System.out.println("清理产品失败: " + productId + ", 错误: " + e.getMessage());
                }
            }
        }
    }
}
```

## 📊 测试执行策略

### 1. 测试执行顺序

```bash
# 完整测试执行流程
1. 环境检查和配置验证
2. 单元测试执行 (快速反馈)
3. 集成测试执行 (需要AEP环境)
4. E2E测试执行 (完整场景)
5. 性能测试执行 (最后执行)
6. 测试报告生成
```

### 2. 持续集成策略

```yaml
# CI/CD测试配置示例
test_stages:
  unit_tests:
    - mvn test -Dtest=*Test
    - 时间限制: 5分钟
    - 覆盖率要求: > 80%

  integration_tests:
    - mvn test -Dtest=*IntegrationTest
    - 时间限制: 15分钟
    - 需要: AEP测试环境

  e2e_tests:
    - ./scripts/e2e_test_suite.sh
    - 时间限制: 30分钟
    - 需要: 完整测试环境
```

### 3. 测试报告策略

```bash
# 测试报告生成
测试覆盖率报告: mvn jacoco:report
性能测试报告: jmeter -n -t performance_test.jmx -l results.jtl
集成测试报告: mvn surefire-report:report
E2E测试报告: ./scripts/generate_e2e_report.sh
```

## 🔍 测试验收标准

### 1. 功能验收标准

- [ ] 所有单元测试通过 (100%)
- [ ] 集成测试通过率 > 95%
- [ ] E2E测试关键场景100%通过
- [ ] Phase1.1兼容性测试100%通过
- [ ] 错误处理覆盖率100%

### 2. 性能验收标准

- [ ] 产品创建响应时间 < 5秒
- [ ] 产品更新响应时间 < 3秒
- [ ] 产品删除响应时间 < 3秒
- [ ] 并发10个请求时成功率 > 95%
- [ ] 内存使用稳定在256MB以下

### 3. 质量验收标准

- [ ] 代码覆盖率 > 80%
- [ ] 静态代码分析0严重问题
- [ ] 无内存泄漏
- [ ] 日志输出格式正确
- [ ] 异常处理完整

## 📋 测试风险评估

### 1. 技术风险

| 风险项目 | 影响级别 | 缓解措施 |
|---------|---------|----------|
| AEP SDK API变更 | 高 | Mock测试+版本控制 |
| 网络连接不稳定 | 中 | 重试机制+超时设置 |
| 测试数据污染 | 中 | 自动清理+隔离环境 |

### 2. 业务风险

| 风险项目 | 影响级别 | 缓解措施 |
|---------|---------|----------|
| 测试环境配额限制 | 中 | 配额监控+轮转账户 |
| 真实数据误删除 | 高 | 严格的测试标识 |
| 性能测试影响生产 | 高 | 独立测试环境 |

## 🎯 测试成功标准

### 1. 短期目标 (MVP验证)

- ✅ 基础功能测试通过
- ✅ Phase1.1兼容性验证
- ✅ 核心错误处理覆盖

### 2. 中期目标 (完整功能)

- 🎯 完整测试套件建立
- 🎯 性能基准测试通过
- 🎯 集成测试自动化

### 3. 长期目标 (生产就绪)

- 🎯 CI/CD集成完成
- 🎯 监控和告警就位
- 🎯 文档和培训完成

---

**测试设计状态**: ✅ 设计完成，待评审
**实施优先级**: P0 (与开发并行)
**预估工作量**: 测试开发3-4天 + 执行验证2-3天