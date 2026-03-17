# 中国电信物联网平台API接口详细文档

## 🏢 **平台基础信息**

### 平台地址
```java
// 从 Constant.java:15 获取
public static final String BASE_URL = "https://device.api.ct10649.com:8743";
```

### 应用认证信息
```java
// 从 Constant.java:20-21 获取
public static final String APPID = "ed5a4f1fcb364575a614f70d52a5a1ac";
public static final String SECRET = "f8a8df37f85a4b6892a7c058b5bfb655";
```

## 🔐 **SSL双向认证证书**

### 证书文件路径
```java
// 从 Constant.java:34-42 获取
public static String SELFCERTPATH = "cert/outgoing.CertwithKey.pkcs12";
public static String TRUSTCAPATH = "cert/ca.jks";

// 证书密码
public static String SELFCERTPWD = "IoM@1234";
public static String TRUSTCAPWD = "Huawei@123";
```

## 📡 **具体API接口列表**

### 1. 认证相关接口

#### 登录认证
```http
POST /iocm/app/sec/v1.1.0/login
Host: device.api.ct10649.com:8743
Content-Type: application/x-www-form-urlencoded

appId={appId}&secret={secret}
```
**响应**:
```json
{
  "accessToken": "..."
}
```

#### Token刷新
```http
POST /iocm/app/sec/v1.1.0/refreshToken
Host: device.api.ct10649.com:8743
Authorization: Bearer {accessToken}
```

### 2. 设备查询接口

#### 设备信息查询/修改
```http
GET /iocm/app/dm/v1.4.0/devices
Host: device.api.ct10649.com:8743
Authorization: Bearer {accessToken}
app_key: {appId}

# 查询参数
?pageNo=1&pageSize=20&deviceId={deviceId}
```

#### 设备激活状态查询
```http
GET /iocm/app/reg/v1.1.0/deviceCredentials
Host: device.api.ct10649.com:8743
Authorization: Bearer {accessToken}
app_key: {appId}

# 查询参数
?deviceId={deviceId}
```

## 🛠️ **接口调用实现方式**

### HTTP请求头配置
```java
// 从 Constant.java:47-48 获取
public static final String HEADER_APP_KEY = "app_key";        // 值为appId
public static final String HEADER_APP_AUTH = "Authorization"; // 值为"Bearer " + accessToken
```

### 调用流程
1. **SSL双向认证初始化**
   ```java
   // 从 AuthUtils.java:43
   httpsUtil.initSSLConfigForTwoWay();
   ```

2. **获取访问Token**
   ```java
   // 从 AuthUtils.java:21-40
   String accessToken = AuthUtils.login(httpsUtil, appId, secret);
   ```

3. **调用业务接口**
   ```java
   // 设置请求头
   headers.put("Authorization", "Bearer " + accessToken);
   headers.put("app_key", appId);
   ```

## 📋 **回调接口配置**

### 设备状态回调
```java
// 从 Constant.java:9-10
public static final String DEVICE_ADDED_CALLBACK_URL = "/na/iocm/devNotify/v1.1.0/addDevice";
public static final String SW_UPGRADE_CALLBACK_URL = "/na/iocm/devNotify/v1.1.0/upgradeSW";
```

### 本地回调地址
```java
// 从 Constant.java:24
public static final String CALLBACK_BASE_URL = "http://49.79.111.151:8082";
```

## ⚡ **关键技术要点**

1. **这是第三方API集成**，不是本地数据库查询
2. **需要SSL双向认证**，必须配置客户端证书
3. **Token机制**，需要先认证获取accessToken
4. **请求头要求**，必须传递app_key和Authorization
5. **华为技术支持**，证书密码显示是华为平台

## 🎯 **可实现的查询功能**

基于这些接口，我们可以实现：

- ✅ **设备列表查询**: 调用 `/iocm/app/dm/v1.4.0/devices`
- ✅ **设备详情查询**: 传递deviceId参数
- ✅ **设备状态查询**: 调用 `/iocm/app/reg/v1.1.0/deviceCredentials`
- ✅ **分页查询**: 支持pageNo和pageSize参数

**注意**: 这些都是中国电信物联网云平台的设备数据，不是本地存储的数据。