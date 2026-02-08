# 开发环境设置指南 (Development Setup Guide)

## 📋 概述

本文档包含AEP数据导出工具的完整开发环境配置指南，包括环境搭建、配置管理和代码审查流程。

## 🛠️ 环境要求

### 基础依赖
- **Java**: OpenJDK 21 或更高版本
- **Maven**: 3.8.0+ (可选，项目内置构建脚本)
- **Git**: 最新版本
- **IDE**: IntelliJ IDEA 或 VS Code

### 系统要求
- **内存**: 最低4GB，推荐8GB
- **磁盘**: 2GB可用空间
- **网络**: 可访问中国电信AEP平台API

## ⚙️ 配置管理

### 环境变量配置

项目使用`.env`文件管理环境配置，支持多环境：

```ini
# .env - 开发环境配置
AEP_APP_KEY=your_dev_app_key
AEP_APP_SECRET=your_dev_secret
AEP_API_HOST=dev.api.ctwing.cn
AEP_APP_ID=your_dev_app_id

# 可选配置
DEBUG=true
LOG_LEVEL=DEBUG
EXPORT_FORMAT=json
```

### 配置文件层级

1. **`.env`** - 默认配置
2. **`.env.local`** - 本地覆盖配置 (不提交到git)
3. **`.env.test`** - 测试环境配置
4. **`.env.production`** - 生产环境配置

### 自动配置加载

项目脚本自动加载配置：

```bash
# 构建时自动加载
./build.sh

# 运行时自动加载
./query.sh --export-all
```

## 🏗️ 开发流程

### 1. 环境初始化

```bash
# 克隆项目
git clone [repository-url]
cd phase1.1-export

# 复制配置模板
cp .env.template .env
# 编辑 .env 填入真实配置

# 验证环境
./verify_win.bat  # Windows
./scripts/verify.sh  # Unix/Linux
```

### 2. 构建项目

```bash
# 快速构建
./build.sh

# 生产级构建 (推荐)
./build_prod.bat  # Windows
./scripts/build/build.sh  # Unix/Linux

# Fat JAR构建 (包含所有依赖)
./build_fat_jar.bat  # Windows
./build_fat_jar.sh   # Unix/Linux
```

### 3. 运行和测试

```bash
# 快速测试
./test.sh

# 数据导出测试
./query.sh --export-products --format json

# 完整集成测试
./scripts/test/integration_test.sh
```

## 📝 代码审查清单

### 提交前检查

- [ ] **代码质量**
  - [ ] 代码遵循项目编码规范
  - [ ] 删除所有调试代码和注释
  - [ ] 变量和方法命名清晰有意义
  - [ ] 避免硬编码，使用配置文件

- [ ] **安全性**
  - [ ] 无敏感信息硬编码 (密钥、密码等)
  - [ ] 输入验证和错误处理完善
  - [ ] 日志不输出敏感信息

- [ ] **功能性**
  - [ ] 核心功能测试通过
  - [ ] 边界条件处理正确
  - [ ] 错误场景处理适当

- [ ] **文档**
  - [ ] README更新 (如有新功能)
  - [ ] 代码注释完整
  - [ ] CHANGELOG记录变更

### Git提交规范

使用约定式提交格式：

```bash
# 功能添加
git commit -m "feat: 新增设备数据批量导出功能"

# 错误修复
git commit -m "fix: 修复BUILD-006 Fat JAR依赖缺失问题"

# 文档更新
git commit -m "docs: 更新部署指南添加Java 21配置"

# 重构
git commit -m "refactor: 优化配置加载逻辑"
```

## 🧪 开发测试

### 单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AepClientManagerTest

# 带覆盖率报告
mvn test jacoco:report
```

### 集成测试

```bash
# 完整集成测试套件
./scripts/test/run_integration_tests.sh

# 快速功能验证
./test.sh --quick
```

### 调试配置

在IDE中配置调试环境变量：

```
-DAEP_APP_KEY=your_key
-DAEP_APP_SECRET=your_secret
-DAEP_API_HOST=dev.api.ctwing.cn
-DDEBUG=true
```

## 🔧 常用开发工具

### 脚本工具

- **`./verify_win.bat`** - 环境验证工具
- **`./check_jars.bat`** - 依赖检查工具
- **`./query.sh`** - 数据查询工具
- **`./test.sh`** - 快速测试工具

### IDE配置

#### IntelliJ IDEA
1. 导入Maven项目
2. 设置JDK 21
3. 安装.env插件自动加载环境变量
4. 配置代码格式化规则

#### VS Code
1. 安装Java扩展包
2. 安装Environment Variable插件
3. 配置launch.json包含环境变量

## 🚨 故障排查

### 常见开发问题

1. **构建失败**
   ```bash
   # 检查Java版本
   java -version

   # 清理重建
   ./clean_build.sh
   ```

2. **环境变量未加载**
   ```bash
   # 检查.env文件
   cat .env

   # 手动加载测试
   source .env && ./query.sh --version
   ```

3. **依赖问题**
   ```bash
   # 检查JAR完整性
   ./check_jars.bat

   # 重新下载依赖
   rm -rf lib/ && ./download_dependencies.sh
   ```

## 📚 相关文档

- [部署指南](../deployment/WINDOWS_GUIDE.md) - Windows服务器部署
- [测试指南](../testing/GUIDE.md) - 测试框架和用例
- [故障排查](TROUBLESHOOTING.md) - 常见问题解决方案

---

📅 **最后更新**: 2026-01-25
🔄 **版本**: v2.1 (合并配置管理和代码审查文档)