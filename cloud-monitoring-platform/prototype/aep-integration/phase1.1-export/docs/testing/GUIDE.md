# 测试完整指南 (Complete Testing Guide)

## 📋 概述

本文档提供AEP数据导出工具的完整测试指南，包括单元测试、集成测试、性能测试和手动测试流程。

## 🚀 快速开始

### 常用测试命令

```bash
# 快速验证
./test.sh

# 完整测试套件
./test.sh --full

# 生产级测试
./scripts/test/run_tests_enhanced.sh

# 集成测试
./scripts/test/integration_test.sh
```

## 🧪 测试框架架构

### 测试层次结构

1. **单元测试** - 组件级别功能验证
2. **集成测试** - 组件间交互验证
3. **系统测试** - 端到端功能验证
4. **性能测试** - 负载和响应时间验证

### 测试工具栈

- **JUnit 5** - 单元测试框架
- **Mockito** - Mock对象框架
- **Maven Surefire** - 测试执行插件
- **自定义脚本** - 集成和系统测试

## 📊 测试用例映射

### 核心功能测试矩阵

| 功能模块 | 单元测试 | 集成测试 | 系统测试 | 覆盖率 |
|----------|----------|----------|----------|---------|
| AEP客户端管理 | ✅ AepClientManagerTest | ✅ API连接测试 | ✅ 端到端认证 | 95% |
| 产品服务 | ✅ ProductServiceTest | ✅ API集成测试 | ✅ 数据导出验证 | 90% |
| 设备服务 | ✅ DeviceServiceTest | ✅ 分页查询测试 | ✅ 大数据量测试 | 88% |
| 导出服务 | ✅ ExportServiceTest | ✅ 格式转换测试 | ✅ 文件完整性 | 92% |
| 文件管理 | ✅ FileManagerTest | ✅ IO操作测试 | ✅ 权限和路径 | 85% |
| 错误处理 | ✅ ErrorHandlerTest | ✅ 异常场景测试 | ✅ 失败恢复 | 93% |

### 测试用例分类

#### 🔧 单元测试 (Unit Tests)

**AepClientManagerTest**
```bash
mvn test -Dtest=AepClientManagerTest
```
- SDK初始化测试
- 认证流程测试
- 配置加载测试
- 连接管理测试

**ProductServiceTest**
```bash
mvn test -Dtest=ProductServiceTest
```
- 产品查询功能
- 分页处理逻辑
- 数据转换验证
- 错误处理机制

**DeviceServiceTest**
```bash
mvn test -Dtest=DeviceServiceTest
```
- 设备查询功能
- 批量处理逻辑
- 状态过滤测试
- 数据完整性验证

#### 🔗 集成测试 (Integration Tests)

**API集成测试**
```bash
./scripts/test/integration_test.sh
```
- AEP平台API连接
- 认证流程完整性
- 数据查询端到端
- 错误处理验证

**文件系统集成**
```bash
./scripts/test/file_integration_test.sh
```
- 输出文件创建
- CSV/JSON格式验证
- 权限和路径处理
- 大文件处理能力

#### 🌐 系统测试 (System Tests)

**端到端测试场景**

1. **默认导出模式**
```bash
./test.sh --scenario default
# 验证: 默认参数下的完整导出流程
```

2. **指定格式导出**
```bash
./test.sh --scenario formats
# 验证: CSV和JSON格式的正确性
```

3. **大数据量处理**
```bash
./test.sh --scenario large-dataset
# 验证: 大量设备数据的处理能力
```

4. **错误恢复测试**
```bash
./test.sh --scenario error-recovery
# 验证: 网络中断后的恢复机制
```

## 🎯 测试执行指南

### 开发环境测试

#### 预提交测试 (Pre-commit)
```bash
# 1. 快速单元测试
mvn test -Dtest='*Test'

# 2. 代码格式检查
mvn checkstyle:check

# 3. 基本功能验证
./test.sh --quick
```

#### 完整开发测试
```bash
# 1. 完整单元测试套件
mvn clean test

# 2. 集成测试
./scripts/test/integration_test.sh

# 3. 系统测试 (如果有测试环境)
./test.sh --full --env test
```

### 生产部署测试

#### 部署前验证
```bash
# 1. 环境验证
./verify_win.bat  # Windows
./scripts/verify.sh  # Unix

# 2. 构建测试
./build_prod.bat && ./test.sh --production

# 3. 性能基准测试
./scripts/test/performance_test.sh
```

#### 部署后验证
```bash
# 1. 基本功能测试
./query.sh --export-products --format json

# 2. 数据完整性验证
./scripts/test/data_integrity_test.sh

# 3. 性能监控
./scripts/test/monitor_performance.sh
```

## 📈 性能测试

### 性能基准

| 测试场景 | 目标响应时间 | 目标吞吐量 | 内存使用 |
|----------|--------------|------------|----------|
| 产品查询(100个) | < 2秒 | - | < 256MB |
| 设备查询(1000个) | < 10秒 | - | < 512MB |
| CSV导出(10K设备) | < 30秒 | - | < 1GB |
| JSON导出(10K设备) | < 45秒 | - | < 1.5GB |

### 性能测试命令

```bash
# 基础性能测试
./scripts/test/performance_test.sh --basic

# 压力测试
./scripts/test/performance_test.sh --stress

# 内存泄漏检测
./scripts/test/memory_leak_test.sh

# 并发测试
./scripts/test/concurrent_test.sh
```

## 🔍 测试数据管理

### 测试数据准备

```bash
# 生成测试数据
./scripts/test/generate_test_data.sh

# 使用样例数据
cp samples/devices_sample.csv test-data/
cp samples/products_sample.csv test-data/
```

### 测试环境隔离

```bash
# 设置测试环境变量
export TEST_MODE=true
export AEP_API_HOST=test.api.ctwing.cn

# 使用测试配置
cp .env.test .env.local
```

## 📋 测试报告

### 自动测试报告

测试报告自动生成在：
- `target/surefire-reports/` - JUnit报告
- `reports/` - 集成测试报告
- `logs/test-results/` - 详细测试日志

### 报告格式

1. **JUnit XML报告** - CI/CD集成用
2. **HTML报告** - 人工查看用
3. **JSON报告** - 程序解析用

### 查看报告

```bash
# 查看最新测试报告
open target/surefire-reports/index.html  # macOS
start target/surefire-reports/index.html  # Windows

# 查看集成测试报告
ls -la reports/integration_test_*/
```

## 🚨 测试故障排查

### 常见测试失败

#### 环境依赖问题
```bash
# 检查Java版本
java -version  # 需要Java 21+

# 检查网络连接
ping $AEP_API_HOST

# 检查配置文件
cat .env | grep -v "^#"
```

#### Mock数据问题
```bash
# 重置mock数据
./scripts/test/reset_mocks.sh

# 验证mock服务
curl http://localhost:8080/mock/health
```

#### 权限问题
```bash
# 检查文件权限
ls -la output/
ls -la logs/

# 修复权限
chmod 755 output/ logs/
```

### 调试技巧

#### 详细日志调试
```bash
# 启用调试模式
export DEBUG=true
export TEST_LOG_LEVEL=TRACE

# 运行单个失败测试
mvn test -Dtest=FailingTestClass -X
```

#### 断点调试
```bash
# IDE调试配置
-Dmaven.surefire.debug=true
-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005
```

## 📚 测试最佳实践

### 编写测试用例

1. **遵循AAA模式**: Arrange, Act, Assert
2. **一个测试一个断言**: 保持测试简单
3. **有意义的测试名称**: 描述测试场景
4. **独立性**: 测试间不应有依赖

### 测试维护

1. **定期更新**: 随功能变更更新测试
2. **清理过期测试**: 删除不再需要的测试
3. **重构测试代码**: 保持测试代码质量
4. **文档同步**: 更新测试文档

## 🔄 持续集成

### CI/CD集成

```yaml
# GitHub Actions示例
test:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v2
    - uses: actions/setup-java@v2
      with:
        java-version: '21'
    - run: ./test.sh --ci
    - run: ./scripts/test/integration_test.sh --ci
```

### 测试指标监控

- **代码覆盖率**: 目标 > 85%
- **测试通过率**: 目标 > 95%
- **测试执行时间**: 目标 < 5分钟
- **测试稳定性**: 目标 > 99%

---

## 📚 相关文档

- [开发环境设置](../development/SETUP.md) - 测试环境配置
- [故障排查](../development/TROUBLESHOOTING.md) - 测试问题解决
- [部署指南](../deployment/WINDOWS_GUIDE.md) - 生产测试

📅 **最后更新**: 2026-01-25
🔄 **版本**: v2.1 (合并所有测试文档)