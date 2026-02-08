# AEP分组管理
## API列表
|API名称 | 安全认证方式 | 签名认证方式 | 描述 |
|:-------|:------|:--------|:--------|
|CreateDeviceGroup|none|hmac-sha1|新增分组|
|UpdateDeviceGroup|none|hmac-sha1|编辑分组信息|
|DeleteDeviceGroup|none|hmac-sha1|删除分组|
|QueryDeviceGroupList|none|hmac-sha1|查询分组列表信息|
|QueryGroupDeviceList|none|hmac-sha1|查询分组下已关联的设备列表或产品下未关联的设备列表|
|UpdateDeviceGroupRelation|none|hmac-sha1|编辑分组与设备关联关系|
|getGroupDetailByDeviceId|none|hmac-sha1|查询设备所属分组信息|

## API调用
### 请求地址

|环境 | HTTP请求地址  | HTTPS请求地址 |
|:-------|:------|:--------|
|正式环境|10433748.api.ctwing.cn/aep_device_group_management|10433748.api.ctwing.cn/aep_device_group_management|

### 公共入参

公共请求参数是指每个接口都需要使用到的请求参数。

|参数名称 | 含义  | 位置 | 描述|
|:-------|:------|:--------|:--------|
|application|应用标识|HEAD|应用的App Key，如果需要进行签名认证则需要填写，例如：dAaFG7DiPt8|
|signature|签名数据|HEAD|将业务数据连同timestamp、application一起签名后的数据，如果需要进行签名认证则需要填写|
|timestamp|UNIX格式时间戳|HEAD|如果需要进行签名认证则需要填写，例如：1515752817580|
|version|API版本号|HEAD|可以指定API版本号访问历史版本|

## API 文档说明
### API名称：CreateDeviceGroup   版本号: 20190615001622

#### 描述

新增分组

#### 请求信息

请求路径：/deviceGroup

请求方法：POST

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|MasterKey|HEAD|String|false||

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
//单产品群组
{
  "description": "groupDesc",
  "deviceGroupName": "groupName",
  "productId": 10006031
}
//多产品群组
{
  "description": "groupDesc",
  "deviceGroupName": "groupName",
  "groupLevel" :1
}
描述：
   "description": 群组描述，String,
   "deviceGroupName": 群组名称(只支持英文和数字)，String，必填
   "productId": 产品Id，Integer，单产品群组必填
   "groupLevel": 群组类别，0或不填为单产品群组，1为多产品群组

#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "deviceGroupId": 790,
    "tenantId": "300",
    "productId": 10000587,
    "deviceGroupName": "string",
    "description": "string"
  }
}

##### 异常返回示例
{
  "code": 2101,
  "msg": "创建群组失败",
  "result": null
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

### API名称：UpdateDeviceGroup   版本号: 20190615001615

#### 描述

编辑分组信息

#### 请求信息

请求路径：/deviceGroup

请求方法：PUT

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|MasterKey|HEAD|String|false||

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "description": "groupDesc",
  "deviceGroupId": 196,
  "deviceGroupName": "groupName",
  "productId": 10006031
}
描述：
   "description": 分组描述，String,
   "deviceGroupId": 分组Id，Integer,
   "deviceGroupName": 分组名称(只支持英文和数字)，String,
   "productId": 产品Id，Integer，多产品群组不填

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": null
}

##### 异常返回示例
{
  "code": 2102,
  "msg": "编辑群组失败",
  "result": null
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

### API名称：DeleteDeviceGroup   版本号: 20190615001601

#### 描述

删除分组

#### 请求信息

请求路径：/deviceGroup

请求方法：DELETE

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|productId|QUERY|Long|false|产品Id，单产品分组必填|
|deviceGroupId|QUERY|Long|true|分组Id|
|MasterKey|HEAD|String|false||


#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": null
}

##### 异常返回示例
{
  "code": 2104,
  "msg": "删除群组失败",
  "result": null
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

### API名称：QueryDeviceGroupList   版本号: 20230218035819

#### 描述

查询分组列表信息

#### 请求信息

请求路径：/deviceGroups

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|pageNow|QUERY|Long|true|当前页数|
|pageSize|QUERY|Long|true|每页记录数|
|productId|QUERY|Long|false|支持通过产品id查询单产品分组列表|
|deviceGroupId|QUERY|Long|false|支持通过分组ID查询|
|deviceGroupName|QUERY|String|false|支持通过分组名称查询|
|groupLevel|QUERY|Long|false|支持通过分组类别查询，0为单产品分组，1为多产品分组|


#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "list": [
      {
        "deviceGroupId": 21179,
        "tenantId": "990000000001111",
        "productId": 15171217,
        "deviceGroupName": "test",
        "description": "",
        "groupLevel": 0,
        "createTime": 1675342845000,
        "updateTime": 1675342845000
      }
    ]
  }
}

##### 异常返回示例
{
  "code": 0,
  "msg": "ok",
  "result": null
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

### API名称：QueryGroupDeviceList   版本号: 20190615001540

#### 描述

查询分组下已关联的设备列表或产品下未关联的设备列表

#### 请求信息

请求路径：/groupDeviceList

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|MasterKey|HEAD|String|false||
|productId|QUERY|Long|false|产品ID，查询单产品分组下已关联的设备列表或产品下未关联的设备列表时必填|
|searchValue|QUERY|String|false|可查询：设备ID，设备名称，设备编号或者IMEI号；仅支持单产品分组查询|
|pageNow|QUERY|Long|true|当前页数|
|pageSize|QUERY|Long|true|每页条数|
|deviceGroupId|QUERY|Long|false|群组ID：1.有值则查询该群组已关联的设备信息列表。2.为空则查询该产品下未关联的设备信息列表|


#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 1,
    "list": [
      {
        "deviceName": "fe",
        "deviceId": "6d9ff188a6394e63b3760e76188e175a",
        "imei": "",
        "deviceSn": "",
        "deviceStatus": 1,
        "productId":15047763,
        "productProtocol":3,
        "productName":"zsy测试冰箱",
        "nodeType":1
      }
    ]
  }
}

##### 异常返回示例
{
  "code": 0,
  "msg": "ok",
  "result": null
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

### API名称：UpdateDeviceGroupRelation   版本号: 20190615001526

#### 描述

编辑分组与设备关联关系

#### 请求信息

请求路径：/deviceGroupRelation

请求方法：PUT

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|MasterKey|HEAD|String|false||

#### 请求BODY

##### 数据类型：
json

##### 内容描述：
{
  "deviceGroupId": 196,
  "deviceList": [
    "6d9ff188a6394e63b3760e76188e175a"
  ],
  "flag": 0,
  "productId": 10006031
}
 描述：
   deviceGroupId:分组ID，Integer,
   deviceList:设备ID列表，List<String>,
   flag:关联操作标识:0：关联，1：去除关联, Integer,
   productId:产品Id ,Integer

#### 返回信息

##### 返回参数类型
default

##### 返回结果示例
{
  "code": 0,
  "msg": "ok",
  "result": null
}

##### 异常返回示例
{
  "code": 2106,
  "msg": "编辑群组关联失败",
  "result": null
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

### API名称：getGroupDetailByDeviceId   版本号: 20211014095939

#### 描述

查询设备所属分组信息

#### 请求信息

请求路径：/groupOfDeviceId

请求方法：GET

#### 请求参数

|名称 | 位置| 类型| 必填| 描述|
|:-------|:------|:--------|:--------|:--------|
|productId|QUERY|Long|true||
|deviceId|QUERY|String|true||


#### 返回信息

##### 返回参数类型
json

##### 返回结果示例
{
	"code": 0,
	"msg": "ok",
	"result": {
		"deviceGroupId": 15629,   //组ID
		"productId": 15067073,   //跨产品分组返回null
		"deviceGroupName": "组名称",
		"description": "",
		"groupLevel": 0,  //组级别，0:单产品,1:跨产品
		"maxDevNum": 0    //设备数量上限，0不做限制'
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

