# 电信IOT平台设备查询POC项目

## 项目说明
本项目是中国电信物联网平台设备查询功能的概念验证(POC)项目，演示如何调用电信IOT平台API获取设备信息。

⚠️ **注意**: 本项目使用的认证信息为学习示例，实际使用时需要向中国电信物联网平台申请正式的appId和secret。

## 电信IOT平台信息

### 平台地址
- **生产环境**: https://device.api.ct10649.com:8743
- **开发环境**: https://develop.api.ct10649.com:8743

### 示例认证信息
```
APPID: ed5a4f1fcb364575a614f70d52a5a1ac
SECRET: f8a8df37f85a4b6892a7c058b5bfb655
```

## 主要功能

### 支持的查询操作
- ✅ 设备列表查询 (支持分页)
- ✅ 设备详情查询
- ✅ 设备状态查询
- ✅ 基站设备查询

### 不包含的功能
- ❌ 设备注册
- ❌ 设备命令控制
- ❌ 设备配置修改

## 技术栈
- Spring Boot 2.7.x
- Java 8
- HTTPS/SSL双向认证
- RESTful API

## 快速开始

### 1. 环境要求
- Java 8+
- Maven 3.6+

### 2. 运行项目
```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run
```

### 3. 访问接口
```bash
# 设备列表查询
GET http://localhost:8080/api/devices

# 设备详情查询
GET http://localhost:8080/api/devices/{deviceId}

# API文档
http://localhost:8080/swagger-ui.html
```

## 获取正式认证信息

如需正式使用，请按以下步骤获取认证：

1. 访问中国电信物联网开发者平台
2. 企业账号注册和实名认证
3. 创建应用项目获得appId和secret
4. 下载SSL客户端证书
5. 配置回调地址

## 项目结构
```
iot-query-validation/
├── src/main/java/com/zct/iot/
│   ├── config/          # 电信平台配置
│   ├── controller/      # REST API控制器
│   ├── service/         # 业务服务层
│   ├── client/          # 电信平台客户端
│   └── dto/             # 数据传输对象
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── cert/            # SSL证书目录
└── README.md
```