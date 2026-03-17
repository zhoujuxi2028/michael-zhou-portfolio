# 故障排查指南 (Troubleshooting Guide)

## 📋 概述

本文档汇总了AEP数据导出工具开发和部署过程中的常见问题及解决方案。

## 🚨 构建问题

### BUILD-006: Fat JAR构建脚本语法错误 ✅ **已修复**

**症状**:
```
Exception in thread "main" java.lang.NoClassDefFoundError: com/ctg/ag/sdk/biz/AepProductManagementClient
```

**原因**: `build_fat_jar.bat`中jar命令语法错误，导致AEP SDK依赖缺失

**解决方案**:
```bash
# 1. 确认使用修复后的脚本
git pull origin phase1.1-export

# 2. 重新构建Fat JAR
del target\aep-data-exporter-fat.jar  # Windows
rm target/aep-data-exporter-fat.jar   # Unix

.\build_fat_jar.bat  # Windows
./build_fat_jar.sh   # Unix

# 3. 验证修复
jar tf target/aep-data-exporter-fat.jar | grep AepProductManagementClient
```

### BUILD-005: Maven构建依赖冲突

**症状**: Maven构建时出现依赖版本冲突
**解决方案**:
```bash
# 清理依赖缓存
mvn clean

# 强制更新依赖
mvn clean compile -U

# 使用内置构建脚本 (推荐)
./build.sh
```

## 🌐 网络和API问题

### API-001: 设备查询使用方式说明 ✅ **已解决**

**症状**: 提示"导出设备时必须指定 --product-id"错误
**原因**: 设备查询需要指定产品ID，工具设计要求
**解决方案**:
```bash
# 1. 查询特定产品的设备 (推荐)
./query.sh --export-devices --product-id 16980130 --format json

# 2. 先查询产品列表，获取产品ID
./query.sh --export-products --format json

# 3. 查询所有产品和设备 (完整数据)
./query.sh --export-all --format json

# 4. 验证API权限正常
curl -H "Authorization: Bearer $AEP_APP_KEY" \
     https://$AEP_API_HOST/aep_device_management
```

### API-002: 网络连接超时

**症状**: API请求超时或连接失败
**解决方案**:
```bash
# 1. 检查网络连通性
ping $AEP_API_HOST

# 2. 检查代理设置
export http_proxy=your_proxy
export https_proxy=your_proxy

# 3. 增加超时时间
./query.sh --timeout 60 --export-all
```

## ⚙️ 配置问题

### CONFIG-001: 环境变量加载失败

**症状**: "环境变量未设置"错误
**解决方案**:
```bash
# 1. 检查.env文件存在
ls -la .env

# 2. 检查文件格式 (无BOM，UTF-8编码)
file .env

# 3. 手动加载测试
source .env  # Unix
# 或编辑脚本确保正确加载

# 4. 使用脚本自动加载
./query.sh  # 自动加载.env配置
```

### CONFIG-002: 密钥格式错误

**症状**: AEP认证失败
**解决方案**:
```bash
# 1. 验证密钥格式
echo $AEP_APP_KEY | wc -c  # 检查长度

# 2. 检查特殊字符
grep -o '[^a-zA-Z0-9]' .env

# 3. 重新获取密钥
# 从AEP平台重新生成应用密钥
```

## 🏃‍♂️ 运行时问题

### RUNTIME-001: Java版本不兼容

**症状**: "Unsupported class file major version"错误
**解决方案**:
```bash
# 1. 检查Java版本
java -version  # 需要Java 21+

# 2. 设置JAVA_HOME
export JAVA_HOME=/path/to/java21

# 3. 验证版本匹配
javac -version
```

### RUNTIME-002: 内存不足

**症状**: "OutOfMemoryError"错误
**解决方案**:
```bash
# 1. 增加JVM内存 (临时)
export JAVA_OPTS="-Xmx4G -Xms2G"

# 2. 使用生产级脚本 (已优化)
./run_prod.bat  # Windows
./scripts/run_prod.sh  # Unix

# 3. 分批导出大数据
./query.sh --export-products --format csv  # 仅导出产品
```

## 📊 数据问题

### DATA-001: 导出数据为空

**症状**: 导出的JSON/CSV文件为空或很小
**解决方案**:
```bash
# 1. 检查API响应
./query.sh --export-products --format json --verbose

# 2. 验证权限和数据范围
# 确认当前租户下有数据

# 3. 使用调试模式
export DEBUG=true
./query.sh --export-all
```

### DATA-002: CSV格式问题

**症状**: CSV文件格式不正确或乱码
**解决方案**:
```bash
# 1. 检查文件编码
file -i output/devices.csv

# 2. 强制UTF-8编码
./query.sh --export-all --format csv --encoding utf8

# 3. 验证CSV内容
head -5 output/devices.csv
```

## 🧪 测试问题

### TEST-001: 单元测试失败

**症状**: Maven测试阶段失败
**解决方案**:
```bash
# 1. 跳过测试快速构建
mvn clean compile -DskipTests

# 2. 运行特定测试
mvn test -Dtest=AepClientManagerTest

# 3. 使用集成测试
./test.sh --integration
```

### TEST-002: 环境依赖问题

**症状**: 测试需要真实API访问
**解决方案**:
```bash
# 1. 使用mock测试
mvn test -Dtest.profile=mock

# 2. 配置测试环境
cp .env.test.template .env.test

# 3. 运行离线测试
./test.sh --offline
```

## 📱 平台特定问题

### Windows问题

**问题**: 批处理脚本执行权限
```cmd
# 设置执行策略
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser

# 使用PowerShell替代
powershell -File .\scripts\run.ps1
```

### Unix/Linux问题

**问题**: 脚本执行权限
```bash
# 添加执行权限
chmod +x *.sh scripts/**/*.sh

# 检查shell兼容性
bash --version  # 需要bash 4.0+
```

## 🔧 调试技巧

### 详细日志调试

```bash
# 1. 启用详细日志
export DEBUG=true
export LOG_LEVEL=TRACE

# 2. 查看实时日志
tail -f logs/aep-export-$(date +%Y%m%d).log

# 3. 分析错误堆栈
grep -A 10 "Exception" logs/*.log
```

### 网络调试

```bash
# 1. 抓包分析
tcpdump -i any host $AEP_API_HOST

# 2. API调试
curl -v -H "Authorization: Bearer $AEP_APP_KEY" \
     https://$AEP_API_HOST/aep_product_management

# 3. 代理调试
export HTTPS_PROXY=http://proxy.company.com:8080
```

## 📞 获取支持

### 自助解决

1. **查看日志**: `logs/` 目录下的详细错误信息
2. **运行诊断**: `./verify_win.bat` 或 `./scripts/verify.sh`
3. **检查依赖**: `./check_jars.bat`

### 联系支持

提供以下信息：
- 错误信息和堆栈跟踪
- 系统环境 (`java -version`, OS版本)
- 配置文件 (隐藏敏感信息)
- 重现步骤

---

## 📚 相关文档

- [开发环境设置](SETUP.md) - 环境配置指南
- [部署指南](../deployment/WINDOWS_GUIDE.md) - 生产环境部署
- [测试指南](../testing/GUIDE.md) - 测试框架说明

📅 **最后更新**: 2026-01-26
🔄 **版本**: v2.2 (更新API-001解决方案)