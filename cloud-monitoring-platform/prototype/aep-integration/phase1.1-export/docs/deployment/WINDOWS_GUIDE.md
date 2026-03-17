# AEP数据导出工具 - Windows部署指南

## 快速部署

### 环境要求
- Windows Server 2019+ 或 Windows 10+
- Java 21+ (推荐OpenJDK 21 LTS)
- 2GB可用磁盘空间

### 安装Java
1. 下载并安装 [Eclipse Temurin 21](https://adoptium.net/)
2. 验证安装：`java -version`

### 部署步骤

#### 1. 获取项目文件
复制整个项目目录到Windows服务器，确保包含：
- `lib/` 目录下的所有JAR文件
- `src/` 目录下的源代码
- 批处理脚本（.bat文件）

#### 2. 配置AEP认证
创建 `.env` 文件：
```
AEP_APP_KEY=your_app_key
AEP_APP_SECRET=your_app_secret
AEP_API_HOST=your_tenant.api.ctwing.cn
AEP_APP_ID=your_app_id
```

#### 3. 构建和运行
```cmd
# 构建项目
build_prod.bat

# 导出CSV文件
run_prod.bat --export-all --format csv
```

#### 4. 验证结果
检查 `output/` 目录：
- `devices.csv` - 设备数据 (1447个设备)
- `products.csv` - 产品数据

## 可用脚本

| 脚本 | 用途 |
|------|------|
| `build_prod.bat` | 构建项目 |
| `run_prod.bat` | 运行导出 |
| `verify_win.bat` | 环境验证 |
| `check_jars.bat` | 依赖检查 |

## CSV导出命令

```cmd
# 导出所有数据为CSV格式
run_prod.bat --export-all --format csv

# 仅导出设备数据
run_prod.bat --export-devices --format csv

# 仅导出产品数据
run_prod.bat --export-products --format csv
```

## 常见问题

**Q: 构建失败？**
A: 运行 `verify_win.bat` 检查环境

**Q: JAR依赖错误？**
A: 运行 `check_jars.bat` 检查依赖

**Q: 网络连接问题？**
A: 确认可访问AEP平台API地址

## 自动化部署

### 定时导出
使用Windows任务计划程序：
1. 打开任务计划程序
2. 创建基本任务
3. 程序：`run_prod.bat`
4. 参数：`--export-all --format csv`

### 输出文件
默认输出到 `output/` 目录：
- `devices.csv` - 约292KB，包含1447个设备
- `products.csv` - 约2KB，包含产品清单

---

**项目状态**: ✅ 生产就绪，所有缺陷已修复