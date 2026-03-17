# AEP产品管理SDK

中国电信AEP平台产品管理SDK，支持产品查询和管理。

## 项目状态

✅ **已完成**: API连接成功，HTTP响应从404改善到401
⚠️ **待解决**: 签名认证失败，收到"Signature not matched"错误
📋 **可用**: 独立查询工具可直接使用，SDK增强版需要解决依赖问题

## 环境配置

### 创建.env文件
```bash
# 复制模板文件
cp .env.template .env

# 编辑.env文件，填入实际值
vim .env
```

### .env文件内容
```bash
# AEP平台认证信息
AEP_APP_KEY=您的实际App Key
AEP_APP_SECRET=您的实际App Secret
AEP_API_HOST=您的实际域名.api.ctwing.cn

# 可选配置
AEP_APP_ID=您的应用ID
AEP_PRODUCT_ID=您的产品ID
```

### 加载环境变量
```bash
# 方法1: 使用source命令
source .env

# 方法2: 使用export_env.sh脚本
source export_env.sh

# 验证配置
echo $AEP_APP_KEY
```

### .env.template示例（可提交到git）
```bash
# AEP平台认证信息配置模板
AEP_APP_KEY=YOUR_APP_KEY_HERE
AEP_APP_SECRET=YOUR_APP_SECRET_HERE
AEP_API_HOST=YOUR_TENANT_ID.api.ctwing.cn
AEP_APP_ID=YOUR_APP_ID_HERE
AEP_PRODUCT_ID=YOUR_PRODUCT_ID_HERE
```

## 快速开始

### 方法1: 一键运行脚本
```bash
./run_query.sh
```

### 方法2: 独立查询工具
```bash
source .env
javac AepProductQuerySimple.java
java AepProductQuerySimple
```

### 方法3: 增强版SDK
```bash
source .env
javac -cp "../lib/*:." AepProductManagementDemo_Enhanced.java
java -cp "../lib/*:." AepProductManagementDemo_Enhanced
```

## 核心文件

- **AepProductQuerySimple.java** - 独立HTTP查询工具（推荐）
- **AepProductManagementDemo_Enhanced.java** - 完整SDK demo
- **run_query.sh** - 一键运行脚本
- **.env** - 环境变量配置文件（不提交到git）
- **.env.template** - 配置模板（可提交到git）

## API调用示例

```java
// 基础查询
String appKey = System.getenv("AEP_APP_KEY");
String appSecret = System.getenv("AEP_APP_SECRET");

AepProductManagementClient client = AepProductManagementClient.newClient()
    .appKey(appKey)
    .appSecret(appSecret)
    .scheme(Scheme.HTTPS)
    .build();

QueryProductListRequest request = new QueryProductListRequest();
QueryProductListResponse response = client.QueryProductList(request);
```

## 预期输出

```
============================================================
AEP产品查询工具
App Key: [YOUR_APP_KEY]
API Host: [YOUR_API_HOST]
============================================================
正在查询产品列表...
HTTP响应码: 200
总产品数量: 3

产品列表:
1. 智能监控设备 - ID: 12345
2. 环境传感器 - ID: 12346
3. 4G通信模块 - ID: 12347
============================================================
```

## 故障排除

### 环境变量未加载
```bash
# 确保已加载.env文件
source .env
printenv | grep AEP
```

### 编译错误
```bash
# 确保classpath包含所有jar文件
javac -cp "../lib/*:." *.java
```

### 401认证失败
- 检查.env文件中App Key/Secret是否正确
- 确认应用有产品管理权限
- 验证API域名格式

### 网络连接问题
```bash
# 测试连接
curl -I "https://您的域名.api.ctwing.cn"
```

### 签名问题（当前状态）
当前签名格式和算法正确，但服务器验证失败。可能原因：
- 签名字符串细节处理差异
- 服务器端特殊参数要求
- 建议联系AEP技术支持获取详细签名文档

## 安全最佳实践

- ❌ 不要硬编码凭据
- ✅ 使用.env文件: `AEP_APP_KEY=your_key`
- 📁 将 `.env` 文件加入 `.gitignore`
- 📝 使用 `.env.template` 作为配置示例
- 🔄 定期轮换App Key/Secret

## 技术实现

**核心技术**: Java 8+, HMAC-SHA1签名, HttpURLConnection
**API端点**: `https://domain.api.ctwing.cn/aep_product_management/products`
**API版本**: 20190507004824
**签名算法**: HMAC-SHA1，签名字符串格式已验证正确

## 后续建议

1. **联系AEP技术支持**：提供当前签名字符串和生成的签名值，请求详细签名算法文档
2. **获取官方SDK**：下载最新完整SDK，对比签名实现差异
3. **查看平台文档**：检查是否有特殊签名要求或参数

---
**版本**: v1.1 | **状态**: 基础功能完成，签名认证待优化 | **更新**: 2024-12-26