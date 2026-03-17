# AEP公共产品设备管理
## API列表
|API名称 | 安全认证方式 | 签名认证方式 | 描述 |
|:-------|:------|:--------|:--------|
|QueryDeviceToken|none|hmac-sha1|查询公共产品设备密码|

## API调用
### 请求地址

|环境 | HTTP请求地址  | HTTPS请求地址 |
|:-------|:------|:--------|
|正式环境|10433748.api.ctwing.cn/aep_public_product_device|10433748.api.ctwing.cn/aep_public_product_device|

### 公共入参

公共请求参数是指每个接口都需要使用到的请求参数。

|参数名称 | 含义  | 位置 | 描述|
|:-------|:------|:--------|:--------|
|application|应用标识|HEAD|应用的App Key，如果需要进行签名认证则需要填写，例如：dAaFG7DiPt8|
|signature|签名数据|HEAD|将业务数据连同timestamp、application一起签名后的数据，如果需要进行签名认证则需要填写|
|timestamp|UNIX格式时间戳|HEAD|如果需要进行签名认证则需要填写，例如：1515752817580|
|version|API版本号|HEAD|可以指定API版本号访问历史版本|

## API 文档说明
### API名称：QueryDeviceToken   版本号: 20230330172346

#### 描述

查询公共产品设备密码

#### 请求信息

请求路径：/queryDeviceToken

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|MasterKey|HEAD|String|true|公共产品的MasterKey|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
    "publicProductId": 111,      //公共产品ID，必填
    "deviceSn": "string"       //设备编号，一机一密时必填
}

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
一机一密：
{
    "code": 0,
    "msg": "ok",
    "result": 
    {
            "deviceId": "string",
            "token": "string"
        }
    ]
}
一型一密：
{
    "code": 0,
    "msg": "ok",
    "result": 
{
            "token": "string"
        }
    ]
}

##### 异常返回示例


#### 错误码

|错误码 | 错误信息| 描述|
|:-------|:------|:--------|
|200|OK|请求正常|
|400|Bad request|请求数据缺失或格式错误|
|401|Unauthorized|请求缺少权限|
|403|Forbidden|请求禁止|
|404|Not found|请求资源不存在|
|500|Internal Error|服务器异常|
|503|Service Unavailable|服务不可用|
|504|Async Service|异步通讯|

