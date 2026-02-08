# AEP数据导出工具 🌐

从中国电信AEP平台导出产品和设备数据的跨平台命令行工具，支持JSON/CSV格式。

## 🖥️ 跨平台支持

**完全支持 Linux 🐧 和 Windows 🏢 双平台部署**

| 平台 | 支持状态 | 快速开始 |
|------|----------|----------|
| **Linux/macOS** | ✅ 生产就绪 | `./build.sh && ./query.sh --export-all` |
| **Windows Server** | ✅ 生产就绪 | `build_prod.bat && run_prod.bat --export-all` |
| **开发环境** | ✅ 全平台支持 | 参见下方详细指南 |

## ✅ 系统状态 - 生产就绪

### 📊 数据完整性状态 (API-003修复完成)
- **✅ 产品数据**: 真实来自AEP平台API (100%)
- **✅ 设备数据**: 真实来自AEP平台API (100% - **1447个设备完整导出**)
- **✅ 分页处理**: 15页分页数据完整获取 (修复前仅1页)
- **✅ 数据完整性**: 从14%提升到**100%** (API-003修复成果)

### 🚀 关键改进成果 (API-003分页修复)
| 改进指标 | 修复前 | 修复后 | 提升幅度 |
|----------|--------|--------|----------|
| **设备导出量** | 100个 | **1447个** | 🚀 **1347%增长** |
| **数据完整性** | 14% | **100%** | 🎯 **完全修复** |
| **文件大小** | 62KB | **898KB** | 📈 **14倍增长** |
| **分页覆盖** | 第1页 | **全部15页** | ✅ **完整覆盖** |
| **系统状态** | 不可用 | **生产就绪** | 🏆 **质量达标** |

### 📄 详细技术分析
- **修复文档**: [`docs/bugs/API-003.md`](docs/bugs/API-003.md) - 设备分页查询数据丢失完整修复过程
- **调研报告**: [`docs/INVESTIGATION_REPORT_2026-01-03.md`](docs/INVESTIGATION_REPORT_2026-01-03.md) - 历史技术调研
- **缺陷管理**: [`docs/bugs/README.md`](docs/bugs/README.md) - **19个缺陷100%修复完成**
- **Windows部署**: [`COPY_FILES_LIST.md`](COPY_FILES_LIST.md) - Windows Server部署完整清单

## 🚀 快速使用

### 🔧 环境配置

创建 `.env` 文件：
```bash
AEP_APP_KEY=your_app_key
AEP_APP_SECRET=your_app_secret
AEP_API_HOST=tenant_id.api.ctwing.cn
AEP_APP_ID=your_app_id
```

### 🧪 测试环境配置 (MOCK-001-A)
创建 `.env.test` 文件（基于 `.env.test.template`）：
```bash
# 测试模式控制
AEP_TEST_MODE=mock          # mock|real|skip
ENABLE_MOCK_TESTS=true      # 启用Mock测试
ENABLE_POLLUTION_DETECTION=true
TEST_CLEANUP_ENABLED=true
```

### 🚀 执行方法

#### 🐧 Linux/macOS
```bash
# 构建
./build.sh

# 导出CSV数据 (1447个设备)
./query.sh --export-all --format csv

# 运行测试
./test.sh
```

#### 🏢 Windows
```cmd
REM 构建
build_prod.bat

REM 导出CSV数据 (1447个设备)
run_prod.bat --export-all --format csv

REM 环境验证
verify_win.bat

REM 依赖检查
check_jars.bat
```

## 📊 输出文件

默认输出到 `output/` 目录：
- `devices.csv` - 设备数据 (1447个设备, 292KB)
- `products.csv` - 产品数据 (约2KB)

#### 🧪 测试执行 (MOCK-001-A修复)
```bash
# 生产环境测试（真实API，推荐）
./test.sh

# 开发环境测试（Mock模式）
export AEP_TEST_MODE=mock
./scripts/test/run_tests_enhanced.sh

# CI/CD环境（跳过API测试）
export AEP_TEST_MODE=skip
./scripts/test/run_tests_enhanced.sh
```

## 📊 完整设备导出验证 (1447个设备)

### ✅ API-003修复成功 - 数据完整性100%

经过API-003分页查询修复，系统已实现**1447个设备的完整导出**，数据完整性从14%提升到100%：

#### 🎯 验证结果
```bash
# 运行完整导出
./query.sh --export-all

# 验证输出文件
ls -lh output/
# devices.json: 898K (1447个设备 - 100%完整)
# devices.csv:  292K (1447个设备 - 100%完整)
# products.json: 根据实际产品数
```

#### 📈 修复前后对比
| 指标 | API-003修复前 | API-003修复后 | 改进倍数 |
|------|---------------|---------------|----------|
| 设备导出数量 | 100个 | 1447个 | **14.47倍** |
| 数据完整性 | 14% | 100% | **7倍提升** |
| devices.json大小 | 62KB | 898KB | **14.5倍** |
| 分页处理 | 仅第1页 | 全部15页 | **15页完整** |

#### 🔍 数据构成分析
- **产品16980130** (RepeaterLTE01): 552个设备 (6页)
- **产品16857118** (RepeaterLTE): 895个设备 (9页)
- **分页策略**: 每页100个设备，自动处理所有分页
- **API调用**: 15次分页查询，100%成功获取

#### 📁 生成文件验证
```bash
# 验证设备数量
grep -c '"deviceId":' output/devices.json
# 输出: 1447

# 验证文件大小
ls -lh output/devices.json
# 输出: -rw------- 898K devices.json

# 验证数据样本
head -n 20 output/devices.json
# 显示第一个设备的完整字段信息
```

#### ⚡ 快速导出命令
```bash
# 方法1: 导出所有数据 (推荐)
./query.sh --export-all

# 方法2: 分别导出各产品设备
./query.sh --export-devices --product-id 16980130  # 552个设备
./query.sh --export-devices --product-id 16857118  # 895个设备

# 方法3: 指定格式导出
./query.sh --export-all --format csv  # CSV格式
./query.sh --export-all --format json # JSON格式(默认)
```

## 📁 输出文件 - 生产就绪质量验证

程序默认输出到 `./output/` 目录，经过API-003修复后实现**数据完整性100%**：

### 🎯 标准输出文件
```bash
output/
├── devices.json      # 1447个设备 (898KB) ✅ 主要输出
├── devices.csv       # 1447个设备 (292KB) ✅ 主要输出
├── products.json     # 产品元数据 (JSON格式)
└── products.csv      # 产品元数据 (CSV格式)
```

### 📊 文件规格验证
| 文件名 | 设备数量 | 文件大小 | 数据完整性 | 用途 |
|--------|----------|----------|------------|------|
| devices.json | 1447个 | 898KB | 100% | **主要数据文件** |
| devices.csv  | 1447个 | 292KB | 100% | **Excel兼容格式** |
| products.json | 2个产品 | ~5KB | 100% | 产品元数据 |
| products.csv  | 2个产品 | ~2KB | 100% | 产品清单 |

### 🔍 质量验证命令
```bash
# 验证设备数量
grep -c '"deviceId":' output/devices.json
# 预期输出: 1447

# 验证文件大小
ls -lh output/
# devices.json: 898K ✅
# devices.csv:  292K ✅

# 验证分页完整性
grep -E '"productId": (16980130|16857118)' output/devices.json | wc -l
# 预期: 1447 (552 + 895)

# 验证数据字段完整性
head -n 5 output/devices.json | jq '.[]'
# 显示完整设备信息结构
```

### 自定义输出目录
```bash
java -cp ".:../../../lib/*" com.aep.export.AepDataExporter --export-all --output-dir /your/path
```

## 运行测试

### ⚠️ 重要：正确的执行位置

**必须在项目根目录下执行测试脚本：**
```bash
cd /Users/michael_zhou/Documents/ZCT/github/Cloud-Monitoring-Platform/protoType/aep-integration/phase1.1-export
./test.sh
```

### ⚡ 快速测试命令

```bash
# 🎯 日常开发验证（推荐 - 100%通过，无噪音）
./test.sh                    # 153个生产测试，验证系统就绪状态

# 🛠️ TDD开发验证（查看待实现功能）
./test.sh --tdd-red          # 9个TDD测试，预期失败

# 📋 完整系统验证（调试用）
./test.sh --all-with-tdd     # 162个全部测试，完整视图

# ⚡ 分层快速验证
./test.sh --model-only       # 仅Model层测试（53个用例）
./test.sh --service-only     # 仅Service层测试（70个用例）
./test.sh --main-only        # 仅Main层测试（30个用例）
./test.sh --help            # 显示帮助信息
```

### 📖 详细测试指南

**⚠️ 重要**: 不同测试模式有不同用途，请参考 📘 [测试执行指南](docs/testing/TESTING_GUIDE.md) 了解：
- 🎯 何时使用生产测试 (100%通过，CI/CD友好)
- 🛠️ 何时使用TDD测试 (预期失败，开发验证)
- 🔄 日常开发工作流和最佳实践
- 🚀 发布前检查清单

### 📊 测试结果说明

| 测试模式 | 用例数 | 预期通过率 | 适用场景 |
|----------|--------|------------|----------|
| `./test.sh` | 153个 | 100% | 🎯 日常验证、CI/CD |
| `./test.sh --tdd-red` | 9个 | 0% | 🛠️ TDD开发、查看待办 |
| `./test.sh --all-with-tdd` | 162个 | 94.4% | 📊 完整视图、调试 |

### 📁 目录结构依赖

脚本依赖以下目录结构（必须在正确目录执行）：
```
phase1.1-export/
├── lib/                     # AEP SDK依赖库
├── target/classes           # 编译后的主程序类文件
├── target/test-classes      # 编译后的测试类文件
├── src/                     # 源代码目录
├── logs/                    # 日志输出目录（自动创建）
├── reports/                 # 报告生成目录（自动创建）
├── scripts/                 # 脚本目录（新组织结构）
│   ├── build/               # 构建相关脚本
│   │   └── build.sh         # 标准构建脚本
│   ├── test/                # 测试相关脚本
│   │   └── run_tests_enhanced.sh  # 综合测试执行器
│   └── query/               # 查询相关脚本
│       └── run_query.sh     # AEP数据查询脚本
├── build.sh                 # 便捷构建入口点
├── test.sh                  # 便捷测试入口点
└── query.sh                 # 便捷查询入口点
```

### 📊 测试覆盖

#### 🎯 生产测试 (默认执行)
- **测试文件**: 14个
- **测试用例**: 153个 (生产就绪验证)
- **分层结构**: Model层(53) + Service层(70) + Main层(30)
- **通过率**: 100% (系统正常状态)

#### 🛠️ 开发测试 (TDD RED)
- **测试文件**: 2个
- **测试用例**: 9个 (待实现功能)
- **内容**: ConfigServicePriorityTest(4) + AepDataExporterConfigTest(5)
- **通过率**: 0% (预期失败，驱动开发)

#### 📊 完整测试覆盖
- **总测试文件**: 16个
- **总测试用例**: 162个 (153生产 + 9开发)
- **测试框架**: JUnit 5 + 自定义TDD框架
- **语义化编号**: 支持测试用例追踪和映射

### 📄 生成的文件

**测试报告** (便于阅读和理解):
- `reports/test_execution_report_*.md` - Markdown格式测试报告
- `reports/html/test_report_*.html` - HTML可视化报告
- `reports/json/test_report_*.json` - JSON数据格式
- `reports/csv/test_results_*.csv` - CSV表格格式

**详细日志** (便于缺陷分析和定位):
- `logs/test_execution_*.log` - 主执行日志
- `logs/detailed-test-results/[测试名]_*.log` - 每个测试的详细执行日志
- `logs/test-results/[测试名]_result_*.json` - 结构化测试结果数据

## 命令行选项

```bash
--export-all              # 导出所有数据
--export-products         # 仅导出产品
--export-devices          # 仅导出设备（需要 --product-id）
--format <json|csv>       # 格式（默认json）
--output-dir <path>       # 输出目录（默认./output）
--help                    # 帮助信息
```

## 示例

```bash
# 导出所有数据为CSV
java -cp ".:../../../lib/*" com.aep.export.AepDataExporter --export-all --format csv

# 导出指定产品设备
java -cp ".:../../../lib/*" com.aep.export.AepDataExporter --export-devices --product-id 12345678
```

## 脚本组织结构

### 便捷入口脚本
根目录提供便捷的入口脚本，保持使用习惯：
```bash
./build.sh     # 构建项目
./test.sh      # 运行测试
./query.sh     # 查询AEP数据
```

### 组织化脚本
详细的脚本按功能分类存放在 `scripts/` 目录：
```bash
scripts/build/build.sh               # 完整的构建脚本
scripts/test/run_tests_enhanced.sh   # 综合测试执行器
scripts/query/run_query.sh          # AEP数据查询脚本
```

### 依赖库管理
所有依赖库（包括AEP SDK、JUnit测试库、Apache Commons等）统一存放在 `lib/` 目录中：
```bash
lib/
├── ctg-ag-sdk-core-*.jar           # AEP核心SDK
├── ag-sdk-biz-*.jar                # AEP业务SDK
├── httpclient-*.jar                # HTTP客户端
├── junit-jupiter-*.jar             # JUnit 5 测试框架
├── junit-platform-*.jar            # JUnit平台
└── commons-*.jar                   # Apache Commons工具库
```

**优势**:
- 简化依赖管理，避免多个lib目录的复杂性
- 统一的classpath配置，减少路径错误
- 便于依赖库的版本管理和升级

### 使用建议
- **日常使用**: 优先使用根目录的便捷脚本 (`./build.sh`, `./test.sh`, `./query.sh`)
- **高级配置**: 直接使用 `scripts/` 目录下的具体脚本
- **脚本维护**: 所有实际逻辑在 `scripts/` 目录，便于组织和维护

## 🔧 构建说明

### 🚀 跨平台构建系统 (BUILD-004/BUILD-005修复完成)

**最新优化**: 经过BUILD-004和BUILD-005缺陷修复，构建系统现已完全稳定并支持多种输出模式。

#### 🐧 Linux/macOS 构建
```bash
# 标准构建（默认输出级别）
./build.sh

# 🔕 静默模式 - 适合CI/CD和自动化构建
./build.sh build --quiet

# 🔍 详细模式 - 适合调试构建问题 (BUILD-005修复)
./build.sh build --verbose     # 显示完整Maven输出和测试详情

# 🏭 生产模式 - 简洁输出，适合生产部署
./build.sh build --production

# 其他构建操作
./build.sh clean           # 清理构建目录
./build.sh compile         # 仅编译源代码
./build.sh jar             # 仅构建JAR文件
```

#### 🏢 Windows 构建
```cmd
REM 🎯 生产级构建 (推荐)
build_prod.bat

REM 🛠️ 标准构建
build_win.bat

REM 🔧 诊断构建问题
verify_win.bat          REM 环境验证
check_jars.bat          REM 依赖完整性检查
diagnose_classpath.bat  REM 类路径问题诊断
```

**🎯 输出模式对比** (BUILD-005优化):
- **默认模式**: 显示构建进度和关键状态，适合日常开发
- **静默模式**: 最小输出，仅显示错误和结果
- **详细模式**: 显示完整Maven输出、测试执行详情和应用日志
- **生产模式**: 标准化输出，适合自动化环境

**🔧 BUILD-005修复亮点**:
- ✅ `--verbose` 模式现在真正详细，显示完整构建过程
- ✅ Maven警告过滤优化，避免误导性错误信息
- ✅ 测试执行详情完整显示，包括用例级别的输出

### 正确构建JAR文件

确保只生成正确的JAR文件，避免创建包含源代码的错误构建：

```bash
# ✅ 正确的构建命令（标准Maven风格结构）
jar cvfm target/aep-data-exporter.jar src/main/resources/META-INF/MANIFEST.MF -C target/classes .

# ❌ 错误的构建命令（会包含源代码和其他文件）
jar cvf aep-data-exporter-new.jar ./*
```

**文件位置说明**:
- **MANIFEST.MF**: `src/main/resources/META-INF/MANIFEST.MF` (标准Maven位置)
- **JAR文件**: `target/aep-data-exporter.jar` (构建输出目录)
- **编译后类文件**: `target/classes/` (构建过程生成)

**注意事项**:
- JAR文件应该输出到 `target/` 目录
- `target/` 目录应该被 `.gitignore` 忽略
- 构建的JAR文件大小应该在75-100KB范围内
- MANIFEST.MF作为资源文件受版本控制管理

## 🏢 Windows Server 部署指南

### 📋 完整的Windows支持

项目现已全面支持Windows Server部署，包含15个专用批处理脚本和完整的部署文档。

#### 🔧 核心Windows工具
| 脚本文件 | 功能说明 | 使用场景 |
|----------|----------|----------|
| `build_prod.bat` | 生产级构建 | **推荐主要构建方式** |
| `run_prod.bat` | 生产级执行 | **推荐主要执行方式** |
| `verify_win.bat` | 环境验证 | 部署前检查 |
| `check_jars.bat` | 依赖完整性检查 | 解决依赖问题 |

#### 🚀 Windows快速部署流程
1. **部署文件**: 参考 [`COPY_FILES_LIST.md`](COPY_FILES_LIST.md) 拷贝必要文件
2. **环境配置**: `copy .env.template .env` 并配置实际参数
3. **构建项目**: `build_prod.bat`
4. **验证环境**: `verify_win.bat`
5. **生产运行**: `run_prod.bat --export-all`

#### 📊 可用脚本清单

**Linux/macOS**:
- `build.sh` - 构建项目
- `query.sh` - 导出数据
- `test.sh` - 运行测试

**Windows**:
- `build_prod.bat` - 构建项目
- `run_prod.bat` - 导出数据
- `verify_win.bat` - 环境验证
- `check_jars.bat` - 依赖检查

#### 🎯 Windows部署优势
- ✅ **无需Maven**: 所有依赖已预编译打包
- ✅ **一键部署**: 批处理脚本自动化所有步骤
- ✅ **故障诊断**: 内置环境检查和问题诊断工具
- ✅ **生产就绪**: 经过19个缺陷修复，稳定可靠

#### 📖 详细Windows文档
- **部署清单**: [`COPY_FILES_LIST.md`](COPY_FILES_LIST.md) - 完整的文件拷贝指南
- **缺陷修复**: [`docs/bugs/README.md`](docs/bugs/README.md) - 包含Windows相关修复
- **环境模板**: [`.env.template`](.env.template) - 配置参数模板

---

## 📞 技术支持

- **项目状态**: ✅ 生产就绪
- **缺陷修复**: 19个缺陷100%修复完成
- **平台支持**: Linux/macOS + Windows Server
- **数据完整性**: 1447个设备100%导出成功

🎉 **项目已达到生产级别质量标准，可放心部署使用！**