# 📚 AEP数据导出工具 - 文档中心

欢迎来到AEP数据导出工具的文档中心！这里汇集了项目的所有重要文档，帮助你快速上手、开发、部署和维护系统。

## 🚀 快速开始

### 新用户指南
1. **项目概览**: 查看[项目主README](../README.md)了解功能和特性
2. **环境部署**: 根据你的系统选择[Windows部署指南](deployment/WINDOWS_GUIDE.md)
3. **快速验证**: 运行`./verify_win.bat`检查环境是否就绪

### 开发者指南
1. **开发环境**: 查看[开发环境设置](development/SETUP.md)配置开发工具
2. **测试指南**: 查看[测试完整指南](testing/GUIDE.md)学习测试流程
3. **问题排查**: 遇到问题时查看[故障排查指南](development/TROUBLESHOOTING.md)

## 📂 文档结构

### 📋 根目录文档
- **[README.md](../README.md)** - 项目主文档，功能介绍和使用说明
- **[CHANGELOG.md](CHANGELOG.md)** - 版本更新日志和变更记录

### 🚀 部署文档
📁 `deployment/`
- **[WINDOWS_GUIDE.md](deployment/WINDOWS_GUIDE.md)** - Windows Server部署完整指南
  - 系统要求和环境配置
  - 分步部署流程
  - BUILD-006问题解决方案
  - 生产级脚本使用指南

### 🛠️ 开发文档
📁 `development/`
- **[SETUP.md](development/SETUP.md)** - 开发环境设置指南
  - 环境依赖安装
  - 配置管理架构
  - 代码审查清单
  - 开发工具配置

- **[TROUBLESHOOTING.md](development/TROUBLESHOOTING.md)** - 故障排查指南
  - 构建问题解决
  - API和网络问题
  - 配置和运行时问题
  - 平台特定问题

### 🧪 测试文档
📁 `testing/`
- **[GUIDE.md](testing/GUIDE.md)** - 测试完整指南
  - 测试框架架构
  - 单元/集成/系统测试
  - 性能测试和监控
  - 测试最佳实践

### 📚 档案文档
📁 `archive/`
- **[BUGS_SUMMARY.md](archive/BUGS_SUMMARY.md)** - Bug档案汇总
  - 19个已解决问题记录
  - 按类别的问题分析
  - 经验教训和改进措施

## 🎯 按场景查找文档

### 🔧 我想部署系统
- **Windows服务器**: [Windows部署指南](deployment/WINDOWS_GUIDE.md)
- **遇到BUILD-006错误**: 查看[Windows部署指南 - 故障排查部分](deployment/WINDOWS_GUIDE.md#故障排查)
- **环境验证**: 使用`./verify_win.bat`脚本

### 👨‍💻 我想参与开发
- **环境搭建**: [开发环境设置](development/SETUP.md)
- **代码提交**: 查看[开发环境设置 - 代码审查清单](development/SETUP.md#代码审查清单)
- **运行测试**: [测试指南 - 快速开始](testing/GUIDE.md#快速开始)

### 🐛 我遇到了问题
- **构建失败**: [故障排查 - 构建问题](development/TROUBLESHOOTING.md#构建问题)
- **API错误**: [故障排查 - 网络和API问题](development/TROUBLESHOOTING.md#网络和api问题)
- **配置问题**: [故障排查 - 配置问题](development/TROUBLESHOOTING.md#配置问题)
- **历史问题**: [Bug档案汇总](archive/BUGS_SUMMARY.md)

### 📊 我想了解项目质量
- **测试覆盖率**: [测试指南 - 测试用例映射](testing/GUIDE.md#测试用例映射)
- **Bug统计**: [Bug档案汇总 - 问题分析](archive/BUGS_SUMMARY.md#问题分析)
- **版本历史**: [变更日志](CHANGELOG.md)

## 📋 常用命令参考

### 快速验证
```bash
# 环境验证
./verify_win.bat           # Windows
./scripts/verify.sh        # Unix/Linux

# 快速测试
./test.sh                  # 基础测试
./test.sh --full          # 完整测试
```

### 构建和运行
```bash
# 生产级构建 (推荐)
./build_prod.bat           # Windows

# Fat JAR构建 (单文件部署)
./build_fat_jar.bat        # Windows
./build_fat_jar.sh         # Unix/Linux

# 数据导出
./query.sh --export-all --format json
./run_prod.bat --export-all --format csv
```

### 问题诊断
```bash
# 依赖检查
./check_jars.bat          # Windows

# 详细日志
export DEBUG=true
./query.sh --export-products

# 查看日志
tail -f logs/aep-export-*.log
```

## 🔄 文档维护

### 更新频率
- **实时更新**: README.md, TROUBLESHOOTING.md
- **版本更新**: CHANGELOG.md
- **定期更新**: 部署和开发指南 (月度)
- **归档更新**: Bug档案 (按需)

### 贡献指南
1. 文档使用Markdown格式
2. 遵循现有的结构和风格
3. 添加实际的代码示例
4. 更新相关的导航链接

### 文档质量标准
- **准确性**: 所有命令和路径经过验证
- **完整性**: 包含必要的背景信息和示例
- **可用性**: 新用户能够独立完成操作
- **时效性**: 内容与代码保持同步

## 📞 获取帮助

### 文档问题
如果文档中有不清楚或错误的地方：
1. 查看[故障排查指南](development/TROUBLESHOOTING.md)
2. 检查[Bug档案](archive/BUGS_SUMMARY.md)中的类似问题
3. 创建Issue报告文档问题

### 技术支持
- **开发环境问题**: 参考[开发环境设置](development/SETUP.md)
- **部署问题**: 参考[Windows部署指南](deployment/WINDOWS_GUIDE.md)
- **Bug报告**: 使用[故障排查指南](development/TROUBLESHOOTING.md)中的模板

---

## 📈 文档统计

- **总文档数**: 10个 (从39个优化而来)
- **覆盖领域**: 部署、开发、测试、故障排查
- **减少比例**: 74% (大幅简化)
- **维护成本**: 显著降低

**文档优化效果**: 通过温和整理法，我们将39个分散的markdown文档重组为10个逻辑清晰的文档，显著降低了维护成本，提高了用户体验。

📅 **最后更新**: 2026-01-25
🔄 **版本**: v1.0 (文档重组后首版)