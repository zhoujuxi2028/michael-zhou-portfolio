# 电信IOT平台连接测试指南

## 🔍 测试结果总结

### 当前状态
- ❌ **连接超时**：无法连接到 `device.api.ct10649.com:8743`
- ⚠️ **网络问题**：可能需要特定网络环境或访问权限

### 认证信息来源
```java
// 来源：toyou/zc_backend项目 Constant.java 文件
// 注释：please replace the appId and secret, when you use the demo.
APPID: "ed5a4f1fcb364575a614f70d52a5a1ac"
SECRET: "f8a8df37f85a4b6892a7c058b5bfb655"
```

## 🏠 回家测试步骤

### 方法1：使用curl测试
```bash
# 测试登录接口
curl -X POST "https://device.api.ct10649.com:8743/iocm/app/sec/v1.1.0/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "appId=ed5a4f1fcb364575a614f70d52a5a1ac&secret=f8a8df37f85a4b6892a7c058b5bfb655" \
  --insecure \
  --connect-timeout 30 \
  -v
```

### 方法2：使用Python脚本
```bash
# 使用项目中的测试脚本
cd protoType/poc/iot-query-validation
python3 test-connection.py
```

### 方法3：使用Postman测试
```
URL: https://device.api.ct10649.com:8743/iocm/app/sec/v1.1.0/login
Method: POST
Headers: Content-Type: application/x-www-form-urlencoded
Body:
  appId=ed5a4f1fcb364575a614f70d52a5a1ac
  secret=f8a8df37f85a4b6892a7c058b5bfb655
```

## 📊 预期测试结果

### 成功情况
```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "refreshToken": "...",
  "tokenType": "Bearer"
}
```

### 失败情况可能原因
1. **认证信息过期**：演示账号可能已失效
2. **网络限制**：需要特定IP白名单或VPN
3. **平台变更**：API地址或认证方式已更改
4. **SSL证书问题**：需要双向认证证书

## 🔄 测试后续步骤

### 如果连接成功
1. 记录返回的accessToken
2. 测试设备查询接口
3. 继续完成POC开发

### 如果连接失败
1. 记录具体错误信息
2. 考虑申请正式的电信IOT平台账号
3. 或者创建模拟接口进行POC演示

## 📞 获取正式认证信息

### 官方渠道
- **中国电信物联网开放平台**
- **企业客户经理**
- **技术支持热线**

### 申请流程
1. 企业资质认证
2. 项目需求说明
3. 技术对接
4. 获得正式appId和secret
5. 下载SSL证书

## 🎯 无论测试结果如何

我们已经完成了：
- ✅ 电信IOT平台API接口分析
- ✅ 技术集成方案设计
- ✅ POC项目框架搭建
- ✅ 详细的技术文档

这为后续的正式开发奠定了坚实的技术基础。