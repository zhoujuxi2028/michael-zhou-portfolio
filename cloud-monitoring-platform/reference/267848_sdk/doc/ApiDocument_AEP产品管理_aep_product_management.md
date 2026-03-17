# AEP产品管理
## API列表
|API名称 | 安全认证方式 | 签名认证方式 | 描述 |
|:-------|:------|:--------|:--------|
|QueryProduct|none|hmac-sha1|支持第三方应用根据产品ID查询产品数据，注意本接口只能查询单个产品数据|
|QueryProductList|none|hmac-sha1|批量查询产品信息|
|DeleteProduct|none|hmac-sha1|支持第三方应用删除产品数据，如果产品下有设备数据，则无法删除产品|
|CreateProduct|none|hmac-sha1|添加产品(产品为设备直连+非NB网关协议,支持创建多种协议产品)|
|UpdateProduct|none|hmac-sha1|更新产品(产品为设备直连+非NB网关协议)|

## API调用
### 请求地址

|环境 | HTTP请求地址  | HTTPS请求地址 |
|:-------|:------|:--------|
|正式环境|10433748.api.ctwing.cn/aep_product_management|10433748.api.ctwing.cn/aep_product_management|

### 公共入参

公共请求参数是指每个接口都需要使用到的请求参数。

|参数名称 | 含义  | 位置 | 描述|
|:-------|:------|:--------|:--------|
|application|应用标识|HEAD|应用的App Key，如果需要进行签名认证则需要填写，例如：dAaFG7DiPt8|
|signature|签名数据|HEAD|将业务数据连同timestamp、application一起签名后的数据，如果需要进行签名认证则需要填写|
|timestamp|UNIX格式时间戳|HEAD|如果需要进行签名认证则需要填写，例如：1515752817580|
|version|API版本号|HEAD|可以指定API版本号访问历史版本|

## API 文档说明
### API名称：QueryProduct   版本号: 20181031202055

#### 描述

支持第三方应用根据产品ID查询产品数据，注意本接口只能查询单个产品数据

#### 请求信息

请求路径：/product

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|productId|QUERY|Long|true||


#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "productId": 10012761,
    "productName": "测试创建产品",
    "tenantId": "300",
    "productDesc": "创建产品",
    "productType": 10024,
    "secondaryType": 10025,
    "thirdType": 10026,
    "productProtocol": 1,
    "authType": 1,
    "payloadFormat": 1,
    "createTime": 1571402267678,
    "updateTime": 1571402268678,
    "networkType": 4,
    "endpointFormat": 1,
    "powerModel": 1,
    "apiKey": "67141cddbf2e4f62a2ff458f1dd9ba8e",
    "deviceCount": 3,
    "productTypeValue": "家电",
    "secondaryTypeValue": "测试",
    "thirdTypeValue": "测试tanglv",
    "rootCert": 1,
    "createBy": "user",
    "updateBy": "user",
    "accessType": 2,
    "nodeType": 1,
    "tupIsThrough": 1,
    "dataEncryption": 5,
    "lwm2mEdrxTime": 25.2,
    "onlineDeviceCount":0,
    "encryptionType":0,
    "tupDeviceModel":"332",
    "tupDeviceType":"",
    "categoryId":1,
    "categoryName":"分类",
    "msgParserMode": 1
  }
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

### API名称：QueryProductList   版本号: 20190507004824

#### 描述

批量查询产品信息

#### 请求信息

请求路径：/products

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|searchValue|QUERY|String|false|产品id或者产品名称|
|pageNow|QUERY|Long|false|当前页数|
|pageSize|QUERY|Long|false|每页记录数|


#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
    "code": 0,
    "msg": "ok",
    "result": {
        "list": [
            {
                "accessType": 2,
                "apiKey": "b0910dc269db472fb45823292e706f3e",
                "authType": 1,
                "createBy": "user",
                "createTime": 1535942487000,
                "dataEncryption": 5,
                "deviceCount": 4,
                "endpointFormat": 1,
                "lwm2mEdrxTime": 25.2,
                "networkType": 3,
                "nodeType": 1,
                "payloadFormat": 2,
                "powerModel": 1,
                "productDesc": "",
                "productId": 307,
                "productName": "tup-test-zcj",
                "productProtocol": 4,
                "productType": 1,
                "productTypeValue": "家电",
                "rootCert": "",
                "secondaryType": 2,
                "secondaryTypeValue": "测试",
                "tenantId": "10007905",
                "thirdType": 13,
                "thirdTypeValue": "测试tanglv",
                "tupIsThrough": 1,
                "updateBy": "user",
                "updateTime": 1535943389000,
                "encryptionType":0,
                "tupDeviceModel":"332",
                "tupDeviceType":"",
                "categoryId": 1,
                "categoryName": "门禁设备",
                "msgParserMode": 1
            }
        ],
        "pageNum": 1,
        "pageSize": 30,
        "total": 8
    }
}

##### 异常返回示例
{
 "code":8800,
 "msg":"内部错误，请联系管理员",
 "result":null
}
(8803, "参数验证失败")报错原因：可能是产品id为空或者未能获取到租户id或者删除操作失败,
(8800, "内部错误，请联系管理员")报错原因：可能缺少必要参数,
(1002, "此产品不存在")报错原因：不存在此产品id对应的产品信息,

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

### API名称：DeleteProduct   版本号: 20181031202029

#### 描述

支持第三方应用删除产品数据，如果产品下有设备数据，则无法删除产品

#### 请求信息

请求路径：/product

请求方法：DELETE

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|MasterKey|HEAD|String|true|MasterKey在该设备所属产品的概况中可以查看|
|productId|QUERY|Long|true||


#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "string",
  "result": {}
}

##### 异常返回示例
{
 "code":8800,
 "msg":"内部错误，请联系管理员",
 "result":null
}

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

### API名称：CreateProduct   版本号: 20220924042921

#### 描述

添加产品(产品为设备直连+非NB网关协议,支持创建多种协议产品)

#### 请求信息

请求路径：/product

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  //产品基本信息
  "productName":"String",   //必填，产品名称
  "productDesc":"String",   //必填，产品描述
  "productType":"智慧城市",  //必填，一级分类名
  "secondaryType":"智慧医疗",//必填，二级分类名
  "thirdType":"智慧医疗耗材",//必填，三级分类名
  "createBy":"String",      //必填，创建者,可填写API调用方、用户标识等
  "blockChainTag":"String", //可选，默认不填，区块链标识 ct-chain\wx-chain
  "locatingTag":1,          //可选，默认不填，定位平台标识 
  
  "nodeType":1,             //必填，节点类型，1.设备，2.网关
  "accessType":1,           //必填，接入方式，1.设备直连，2.网关接入，3.南向云接入（只支持1），5.5G定制网
  "networkType":3,          //必填，网络类型，1.WIFI，2.移动蜂窝数据，3.NB-IoT，4.以太网，5.蓝牙，6.ZigBee
  "productProtocol":3,      //必填，协议类型，1:tlink; 2:MQTT; 3:LWM2M; 5:HTTP; 6:JT/T808; 7:TCP; 8:TCP私有; 9:UDP私有;
                                    //10:MQTT(网关设备); 11:南向云对接; 12:MQTT(网关子设备协议); 13:蓝牙(网关子设备协议);
                                    //14:ZigBee(网关子设备协议); 15:其他(网关子设备协议); 16:Modbus; 17:gb28181; 
                                    //18:GA/T1400; 19:ehome; 20:UDP; 21:TR069; 22:GBT_26875_2011;23:银通TCP;
                                    //24:COAP协议; 25:FACP人脸门禁协议；26：ACCP协议；27：FIIA协议；28：SnapEx协议；29：CameraV协议；
                                    //30：AIVP协议
  "authType":4,             //必填，认证方式 1:特征串认证,2:SM9认证,3:证书认证,4:IMEI认证，6:SM2认证，
                                           //7:IPV6标识认证，8:HTTP基本认证，9:HTTP摘要认证
  "dataEncryption":5,       //必填，数据加密方式1:sm1,2:sm2,3:sm4,4:dtls,5:明文（只支持MQTT/LWM2M）6.量子加密
  "encryptionMode":null,    //数据加密方式为量子加密必填，其他方式不填，
  "encryptionType":0,       //必填，0：一机一密，1：一型一密
  "issueCertType":null,     //认证方式为证书认证且加密方式为SM2必填，其他不填，sm2证书分发方式：1:离线,2:在线

  "tupIsThrough":1,   //是否透传 0:透传，1:非透传
  "payloadFormat":1,  //必填，消息格式，消息格式 1:json，2:紧凑二进制

  "powerModel":2,     //LwM2M\COAP协议必填，其他不填 省电模式（LWM2M协议必填）：1.PSM 2.DRX 3.eDRX
  "lwm2mEdrxTime":20.0,    //省电模式为EDRX必填，其他不填，eDRX模式时间窗(LWM2M协议,当省电模式为3时,必填):20 ～ 10485.76 间的值,精确到小数点后两位
  "endpointFormat":1, //LwM2M\COAP协议必填 Endpoint格式:1.IMEI2.URN:IMEI:###############
                                           //3.URN:IMEI-IMSI: ###############-############### 4.URN:IMEI+SM9

  "tupVendorId":"String",   //TR069、FACP必填，TR069协议填写OUI，FACP协议填写厂商编号
  "tupDeviceModel":"String",//必填，设备型号
  "tupDeviceType":"String", //TR069协议必填，TR069协议填写ProductClass，其他不填，

  "deviceModel":"String",   //JT/T808协议必填,其他不填 制造商ID
  "manufacturerId":"String",//JT/T808协议必填, 设备型号

  "accessArea":110101,   //GA1400\GB28181必填，其他协议不填，国家行政区划代码，
                                      //参考https://www.mca.gov.cn/article/sj/xzqh/1980/2019/202002281436.html
  "accessAreaName":"北京市-东城区",//GA1400\GB28181必填，其他协议不填，国家行政区划代码名称

  "commandUrl":"String",    //南向云协议选填，其他不填，指令下发地址
  "platformId":"String" ,   //南向云协议必填，其他不填，平台标识
  "categoryId":1,           //社区类协议必填，其他不填，设备分类id
  "categoryName":"String",   //社区类协议必填，其他不填，设备分类名称
  "msgParserMode":1         //消息解析格式为脚本解析时填写，1：JS脚本解析
}

#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
    "code": 0,
    "msg": "ok",
    "result": {
        "productId": 1415156014,
        "productName": "测试产品234",
        "tenantId": "300",
        "deptCode": "0",
        "deptName": "postman",
        "productDesc": "string",
        "productType": 10310,
        "secondaryType": 10311,
        "thirdType": 10312,
        "productProtocol": 21,
        "authType": 8,
        "payloadFormat": 1,
        "createTime": 1657692056754,
        "updateTime": null,
        "networkType": 2,
        "endpointFormat": null,
        "powerModel": null,
        "apiKey": "c96906cf909340c997de4410d04aff33",
        "createBy": null,
        "updateBy": null,
        "isDelete": null,
        "onlineDeviceCount": 0,
        "deviceCount": null,
        "activeDeviceCount": null,
        "productTypeValue": null,
        "secondaryTypeValue": null,
        "thirdTypeValue": null,
        "devicdeIpPort": null,
        "serviceCount": null,
        "todayServiceCount": null,
        "rootCert": null,
        "tupVendorId": "AAAAAA",
        "tupDeviceModel": "string",
        "tupDeviceType": "ssssss",
        "tupIsProfile": 1,
        "tupIsThrough": 1,
        "isDtls": null,
        "lwm2mEdrxTime": null,
        "itemInstId": null,
        "saasId": null,
        "publicProductId": null,
        "publicTenantId": null,
        "token": null,
        "deviceModel": null,
        "manufacturerId": null,
        "nodeType": 1,
        "accessType": 1,
        "isFollowTeleNorm": 1,
        "standardVersion": null,
        "saasStatus": null,
        "dataEncryption": 5,
        "encryptionMode": null,
        "encryptionType": 0,
        "issueCertType": null,
        "accessArea": null,
        "accessAreaName": null,
        "commandUrl": null,
        "platformId": null,
        "needHeartbeat": null,
        "withProfile": null,
        "profileFileName": null,
        "quantity": null,
        "isOrder": 0,
        "source": null,
        "blockChainTag": null,
        "locatingTag": null,
        "appId": null,
        "dtls": false,
        "categoryId": null,
        "categoryName": null,
        "msgParserMode": null
    }
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

### API名称：UpdateProduct   版本号: 20220924043504

#### 描述

更新产品(产品为设备直连+非NB网关协议)

#### 请求信息

请求路径：/product

请求方法：PUT

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "commandUrl": "",
  "endpointFormat": 1,
  "lwm2mEdrxTime": 20.0,
  "powerModel": 1,
  "productDesc": "这里是需要更新的产品描述",
  "productId": 91858370,
  "productName": "测试产品名称",
  "updateBy": "更新操作者"
}

#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
    "code": 0,
    "msg": "ok",
    "result":"Success to update product info."
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

